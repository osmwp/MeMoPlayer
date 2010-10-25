/*
 * Copyright (C) 2010 France Telecom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

# include <stdio.h>
# include <stdlib.h>
# include "FreeImage.h"
# include "freetype/config/ftheader.h"
# include FT_FREETYPE_H
# include FT_GLYPH_H

FILE * myStdErr;

bool endsWith (const char * t, const char * e) {
     int l1 = strlen (t);
     int l2 = strlen (e);
     if (l1 < l2) {
	  return (false);
     }
     while (l2 > 0) {
	  if (t[--l1] != e[--l2]) {
	       return (false);
	  }
     }
     return (true);
}

// utf 8 -> 32
int validate (unsigned char *buff, int len) {
    for (int x = 0; x < len; x++) {
        if (buff[x] > 0xfd) {
            printf("Byte %i is invalid\n", x);
            return 0;
        }
    }
    return 1;
}

int getLength (unsigned char ch) {
    int l;
    unsigned char c = ch;
    c >>= 3;
    // 6 => 0x7e
    // 5 => 0x3e
    if (c == 0x1e) {
        l = 4;
    } else {
        c >>= 1;
        if (c == 0xe) {
            l = 3;
        } else {
            c >>= 1;
            if (c == 0x6) {
                l = 2;
            } else {
                l = 1;
            }
        }
    }
    return l;
}

unsigned long * toUnicode32 (unsigned char * utf8, int &size) {
    unsigned char *p = utf8;
    unsigned long ch;
    int l;
    unsigned long *result = (unsigned long *) malloc(sizeof(unsigned long)*strlen ((const char*)utf8)+1);
    unsigned long *r = result;
    while (*p) {
        l = getLength (*p);
        switch (l) {
        case 4:
            ch = (*p ^ 0xf0);
            break;
        case 3:
            ch = (*p ^ 0xe0);
            break;
        case 2:
            ch = (*p ^ 0xc0);
            break;
        case 1:
            ch = *p;
            break;
        default:
            printf("Len: %i\n", l);
        }
        ++p;
        for (int y = l; y > 1; y--) {
            ch <<= 6;
            ch |= (*p ^ 0x80);
            ++p;
        }
        //printf ("got char %ld for len %d\n", ch, l);
        *r = ch;
        r++;
    }
    *r = 0x0;
    size = r-result;
    //printf ("Got %d unicode chars\n", r-result);
    return result;
}


void dump_unicode_string (unsigned long *str) {
    unsigned long *s = str;
    while (*s) {
        printf("%li %lx\n", *s, *s);
        s++;
    }
    printf("\n");
}

void dump_unicode (unsigned char * buff) {
    int len = 0;
    unsigned long *result = toUnicode32(buff, len);
    dump_unicode_string(result);
    free(result);
}

void sortArray (unsigned long * s, int len) {
    bool again = true;
    while (again) {
        again = false;
        for (int i = 0; i < (len-1); i++) {
            if (s[i] > s[i+1]) {
                long tmp = s[i];
                s[i] = s[i+1];
                s[i+1] = tmp;
                again = true;
            }
        }
    }
}

void purgeArray (unsigned long * s, int &len) {
    if (len < 2) {
        return;
    }
    int dst = 0;
    for (int src = 1; src < len; src++) {
        if (s[src] != s[dst]) {
            s[++dst] = s[src];
        }
    }
    len = dst+1;
}


// end of unicode

class Font {
    char * m_name; // name of the font
    int m_size; // size of the font
    int m_curX, m_curY; // the offset to the next char
    int m_maxHeight; // the maximum height
    int m_maxBaseline; // the maximum height above the baseline
    int m_nbChars; // the number of chars to draw
    FT_Library m_library; // the font library
    FT_Face m_face; // the face object
    FT_BitmapGlyph * glyphs; // the array of glyphs
    int m_imageWidth, m_imageHeight;
    FIBITMAP * m_bitmap; // the bitmap to draw the chars in
  public: 
    Font (char * name, int size) {
        FT_Init_FreeType ((FT_Library*)&m_library);
        m_name = strdup (name);
        m_size = size;

        if (FT_New_Face (m_library, m_name, 0, &m_face) != 0) {
            fprintf (myStdErr, "Cannot load %s!\n", name);
            exit (1);
        } else {
            FT_Set_Char_Size (m_face, m_size << 6, m_size << 6, 96, 96);
        }
        m_curX = m_curY = 0;
    }
    ~Font () {
        FT_Done_FreeType ((FT_Library)m_library);
        free (m_name);
    }

    FT_BitmapGlyph getGlyph (int charCode) {
        int index = FT_Get_Char_Index (m_face, charCode);
        //fprintf (stderr, "getGlyph %d => %d\n", charCode, index);
        // load the glyph
        FT_Load_Glyph (m_face, index, FT_LOAD_DEFAULT);
        // retrieve the glyph
        FT_Glyph glyph;
        FT_Get_Glyph (m_face->glyph, &glyph);
        FT_Glyph_To_Bitmap (&glyph, ft_render_mode_normal, 0, 1);
        return (FT_BitmapGlyph)glyph;
    }

    unsigned long mkPixel (unsigned char v) {
        //return (v<<24) + (v<<16) + (v<<8) + v;
        return (v<<24); // + 0xFFFFFF;
    }

    void findMax (FT_BitmapGlyph glyph, int index, int& maxDescent, int& maxBaseline, int& curWidth, int& nbLines, int length) {
        int w = glyph->bitmap.width;
        int h = glyph->top;
        if (h > maxBaseline) {
            //fprintf (stderr, "new max baseline: %d for %c\n", h, index);
            maxBaseline = h;
        }
        h = glyph->bitmap.rows - glyph->top;
        if (h > maxDescent) {
            //fprintf (stderr, "new max Descent: %d for %c\n", h, index);
            maxDescent = h;
        }

        curWidth += w;
        if (curWidth >= length) {
            nbLines++;
            curWidth = w;
        }
    }

    void render (FT_BitmapGlyph glyph, int index, FILE * metrics) {
        int w = glyph->bitmap.width;
        int h = glyph->bitmap.rows;
        int pitch = glyph->bitmap.pitch;
        int left = glyph->left;
        int top = glyph->top;
        int offsetX = (int)((FT_Glyph)glyph)->advance.x >> 16;
//         fprintf (myStdErr, "glyph %d/%c", index, (char)index);
//         fprintf (myStdErr, " width : %d", w);
//         fprintf (myStdErr, " height : %d", h);
//         fprintf (myStdErr, " top : %d", top);
//         fprintf (myStdErr, " left : %d", left);
//         fprintf (myStdErr, " incX : %d\n", offsetX);

        // copy from buffer to freeimage buffer
        if ( (m_curX+w) > m_imageWidth) {
            m_curX = 0;
            m_curY += m_maxHeight;
            //fprintf (stderr, "render: starting a new line at %d\n", m_curY);
        }
        //fprintf (stderr, "render bitmap at %d %d\n", m_curX, m_curY);
        fprintf (metrics, "%c%c%c%c%c%c%c%c", index / 256, index % 256, m_curX, m_curY, w, h, (char)left, offsetX);
        for (int j = 0; j < h; j++) {
            int delta = m_maxBaseline - top+1;
            //fprintf (stderr, "rendering %c at %d + (%d-%d)\n", index, m_curY+j, m_maxHeight, top);
            unsigned long * dst = (unsigned long *)FreeImage_GetScanLine(m_bitmap, m_imageHeight-(m_curY+j+delta)) + m_curX;
            unsigned char * src = glyph->bitmap.buffer+pitch*j;
             for (int i = 0; i < w; i++) {
                 *dst++ = mkPixel (*src++);
             }
        }
        m_curX += w;
    }

    void drawChars (char * map, char * filename) {
        m_maxHeight = m_maxBaseline = 0;
        int nbLines = 0;
        int curWidth = 0;
        int maxDescent = 0;
        validate ((unsigned char *)map, strlen (map));
        unsigned long * charMap = toUnicode32 ((unsigned char *)map, m_nbChars);
        //fprintf (stdout, "map=%s => nbChars=%d\n", map, m_nbChars);
        sortArray (charMap, m_nbChars);
        purgeArray (charMap, m_nbChars);
        glyphs = (FT_BitmapGlyph*) malloc (sizeof (FT_BitmapGlyph)*m_nbChars);

        for (int i = 0; i < m_nbChars; i++) {
            glyphs[i] = getGlyph (charMap[i]);
            findMax (glyphs[i], map[i], maxDescent, m_maxBaseline, curWidth, nbLines, 240);
        }
        m_maxHeight = m_maxBaseline + maxDescent;
        //fprintf (stderr, "maxHeight: %d, nbLines: %d, curWidth: %d\n", m_maxHeight, nbLines, curWidth);
        m_imageWidth = 240;
        if (nbLines == 0) {
            m_imageWidth = curWidth;
        }
        m_imageHeight = (nbLines+1)*m_maxHeight;
        
        if ( (m_imageWidth & 1) == 1) {
            m_imageWidth++;
        }
        if ( (m_imageHeight & 1) == 1) {
            m_imageHeight++;
        }
        //fprintf (stderr, "##allocating %d %d : %p\n", m_imageWidth, m_imageHeight, m_bitmap);
        m_bitmap = FreeImage_Allocate (m_imageWidth, m_imageHeight, 32);

        //
        char * buffer = (char *)malloc (strlen (filename)+10);
        sprintf (buffer, "%s.desc", filename);
        FILE * fp = fopen (buffer, "w");
        if (fp == NULL) {
            fprintf (myStdErr, "Cannot open %s for writing\n", buffer);
            exit (1);
        }
        fprintf (fp, "%c%c", m_maxBaseline, m_maxHeight);
        fprintf (fp, "%c%c", m_nbChars/256, m_nbChars%256);
        for (int i = 0; i < m_nbChars; i++) {
            render (glyphs[i], charMap[i], fp);
        }
        fclose (fp);
        free (buffer);
    }

    void save (char * filename) {
        char * buffer = (char *)malloc (strlen (filename)+10);
        sprintf (buffer, "%s.png", filename);
        FreeImage_Save(FIF_PNG, m_bitmap, buffer, PNG_Z_DEFAULT_COMPRESSION); 
        free (buffer);
    }
    
    void dumpMetrics (char * filename) {
        char * buffer = (char *)malloc (strlen (filename)+10);
        sprintf (buffer, "%s.desc", filename);
        FILE * fp = fopen (buffer, "r");
        if (fp == NULL) {
            fprintf (myStdErr, "Cannot open %s for reading\n", buffer);
            exit (1);
        }
        int maxb = fgetc (fp);
        int maxh = fgetc (fp);
        fprintf (stderr, "max baseline: %d\nmax height:%d\n", maxb, maxh); 
        maxb = fgetc (fp);
        maxh = fgetc (fp);
        fprintf (stderr, "nbChars: %d\n", maxb*256+maxh); 
        int v = fgetc (fp);
        while (!feof (fp)) {
            int index =v*256 + fgetc(fp);
            int x = fgetc (fp);
            int y = fgetc (fp);
            int w = fgetc (fp);
            int h = fgetc (fp);
            int l = fgetc (fp);
            int o = fgetc (fp);
            if (l > 127) { l = l - 256; }
            fprintf (stderr, "%d(%lc): %d x %d / %d x %d + %d %d", index, index, x, y, w, h, l, o);
            v = fgetc (fp);
            fprintf (stderr, "%s\n", feof (fp) ? "\nEOF" : "");
        }
        fclose (fp);
        free (buffer);
    }
};

#define TMP_SIZE 1024*8

void encodeTextualDescription (char * fontname, char * mapname) {
    int fontnamelen = strlen (fontname)-3;
    char * buffer = (char *)malloc (fontnamelen+10);
    sprintf (buffer, "%s.desc", mapname);
    //sprintf (buffer, "%s.desc", mapname);
    FILE * in = fopen (fontname, "r");
    FILE * out = fopen (buffer, "wb");
    if (in == NULL) {
        fprintf (myStdErr, "FontExtractor: Cannot open %s for reading\n", buffer);
        exit (1);
    }
    if (out == NULL) {
        fprintf (myStdErr, "FontExtractor: Cannot open %s for writing\n", buffer);
        exit (1);
    }
    
    char tmp [TMP_SIZE];
    if (fgets (tmp, TMP_SIZE, in) == NULL) { // read the fisrt line which must be a comment 
        fprintf (stderr, "FontExtractor: Cannot read header: %d\n", ferror (in)); exit (1);
    }
    int maxb = 0, maxh = 0;
    if (fscanf (in, "%d %d", &maxb, &maxh) != 2) {
        fprintf (stderr, "Cannot read max baseline and height\n"); exit (1);
    }
    
    int nbChars = 0, n = 0;
    if ( (n = fscanf (in, "%d", &nbChars)) != 1) {
        fprintf (stderr, "FontExtractor: Cannot read max nbChars: %d\n", n); exit (1);
    }
    fprintf (out, "%c%c", maxb, maxh);
    fprintf (out, "%c%c", nbChars/256, nbChars%256);
    for (int i = 0; i < nbChars; i++) {
        int index, x, y, w, h, l, o;
        if (fscanf (in, "%d %d %d %d %d %d %d", &index, &x, &y, &w, &h, &l, &o) != 7) {
            fprintf (stderr, "FontExtractor: Cannot read char info #%d\n", i); exit (1);
        }
        fprintf (out, "%c%c%c%c%c%c%c%c", index / 256, index % 256, x, y, w, h, l, o);
    }
    fclose (in);
    fclose (out);

    strcpy (buffer, fontname);
    strcpy (buffer+fontnamelen, "png");
    //sprintf (buffer, "%s.png", fontname);
    in = fopen (buffer, "r");
    if (in == NULL) {
        fprintf (myStdErr, "FontExtractor: Cannot open %s for reading\n", buffer);
        fprintf (myStdErr, "FontExtractor: fontName is %s, mapname is %s\n", fontname, mapname);
        exit (1);
    }
    //fprintf (myStdErr, "FontExtractor: open %s for reading\n", buffer);
    sprintf (buffer, "%s.png", mapname);
    out = fopen (buffer, "wb");
    if (out == NULL) {
        fprintf (myStdErr, "FontExtractor: Cannot open %s for writing\n", buffer);
        exit (1);
    }
    //fprintf (myStdErr, "FontExtractor: open %s for writing\n", buffer);
    
    int c;
    while ( (c = fgetc (in)) != EOF ) {
        fputc (c, out);
    }
    fclose (in);
    fclose (out);
    free (buffer);
    
}


int main (int argc, char * argv[]) {
    myStdErr = stdout; //fopen ("log.txt", "w");
    if (argc != 2 && argc != 5 && argc != 6) {
        fprintf (stderr, "usage: %s [-d] fontname fontsize mapname charList\n", argv[0]);
        fprintf (stderr, "    Used to generate an image and its description, containing all chars in charList and made from fontname at given fontsize\n");
        fprintf (stderr, "    -d: output metrics (for debug only)");
        fprintf (stderr, "example: %s times.ttf 16 times16 abcdefABCDEF123\n", argv[0]);
        fprintf (stderr, "usage: %s textualfontdescription\n", argv[0]);
        fprintf (stderr, "    Used to compime a textual font description of an existing image (e.g. a multicolor font)\n");
        fprintf (stderr, "example: %s times.tdf\n", argv[0]);
        exit (1);
    }
    bool dumpMetrics = true;
    int start = 0;
    if (strcmp (argv[1], "-d") == 0) { dumpMetrics = true; start = 1; }
    //fprintf (myStdErr, "Opening font %s in size %d\n", argv[start+1], atoi(argv[start+2]));

    if (argc > 1 && endsWith (argv[1], ".tfd")) {
        encodeTextualDescription (argv[1], argv[3]);
    } else {
        Font * font = new Font (argv[start+1], atoi(argv[start+2]));
        font->drawChars (argv[start+4], argv[start+3]);
        font->save (argv[start+3]);
        if (start == 1) {
        font->dumpMetrics (argv[start+3]);
        }
        delete font;
    }
    return 0;
}
