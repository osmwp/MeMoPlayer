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

# include "stdio.h"
# include "stdlib.h"
# include "string.h"
# include "unistd.h"
//# include "fcntl.h"

# include "bml.h"

bool s_debug = false;

# define MESSAGE(...) if (s_debug) fprintf (stderr, __VA_ARGS__)

bool endsWith (char * t, const char * e) {
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

class Tag {
public:
    char * m_name;
    int m_id;
    Tag * m_next;

    Tag (char * name, int id, Tag * next) {
        m_name = name;
        m_id = id;
        m_next = next; 
    }

    ~Tag () {
        m_next = NULL;
        //free (m_name);
    }

    int find (char * name) {
        if (strcmp (name, m_name) == 0) {
            return m_id;
        } else if (m_next != NULL) {
            return m_next->find (name);
        } else {
            return -1;
        }
    }
};


class BmlDecoder {
    FILE * m_fp;
    XmlNode * m_root;
    int m_nbTags;
    char ** m_tags;
public:
    BmlDecoder (FILE * fp) {
        m_fp = fp;
        //m_table = new Table ();
        m_nbTags = 0;
        m_root = NULL;
        parseTable ();
        m_root = parseNode ();
    }
    

    XmlNode * getRoot () { return m_root; }

    int decodeSize () {
        unsigned int n = (unsigned int) fgetc (m_fp);
        if (n == 255) {
            n *= (unsigned int) fgetc (m_fp);
            n += (unsigned int) fgetc (m_fp);
        }
        return n;
    }
    char * decodeString () {
        char buffer [1024];
        int i = 0;
        char c = (char) fgetc (m_fp);
        while ( (buffer[i++] = c) ) {
            c = ( char) fgetc (m_fp);
        }
        return strdup (buffer);
    }

    void parseTable () {
        fgetc (m_fp); // B
        fgetc (m_fp); // M
        fgetc (m_fp); // L
        fgetc (m_fp); // 1

        m_nbTags = decodeSize ();
        m_tags = new char * [m_nbTags];
        for (int i = 0; i < m_nbTags; i++) {
            m_tags[i] = decodeString ();
        }
    }

    XmlNode * parseNode () {
        int type = fgetc (m_fp);
        if (type == 3) {
            char * data = decodeString ();
            return new XmlNode (data, CDATA);
        } else if (type > 0) {
            int index = decodeSize ();
            XmlNode * n = new XmlNode (m_tags[index], type == 1 ? OPEN_TAG : SELF_TAG);
            // parse attribute
            int attrIdx = decodeSize (); // index of first attibute
            while (attrIdx != 0) {
                char * value = decodeString ();
                n->addAttribute (new XmlAttribute (m_tags[attrIdx-1], value));
                attrIdx = decodeSize (); // index of next attibute
            }
            XmlNode * child = parseNode ();
            while (child != NULL) {
                n->addChild (child);
                child = parseNode ();
            }
            return n;
        } 
        return NULL;
    }
};


class BmlEncoder : public XmlVisitor {
    char * m_buff;
    int m_buffSize;
    int m_buffPos;

    int m_nbTags;
    Tag * m_tags;
    Tag * m_lastTag;
    bool m_dumpTable;

public:
    BmlEncoder () {
        m_dumpTable = false;
        m_nbTags = 0;
        m_tags = NULL;
        m_lastTag = NULL;
        m_buffSize = 8192;
        m_buffPos = 0;
        m_buff = (char*) malloc (sizeof(char) * m_buffSize);
    }

    ~BmlEncoder () {
        // Clean tags
        Tag * tag = m_tags;
        while (tag != NULL) {
            Tag * next = tag->m_next;
            delete tag;
            tag = next;
        }
        m_lastTag = NULL;
        m_tags = NULL;
        free (m_buff);
    }

    char * getBuffer (int & size) {
        size = m_buffPos;
        return m_buff;
    }

    void ensure (int l) {
        int newSize = m_buffPos + l;
        if (newSize > m_buffSize) {
            m_buffSize *= 2;
            if (newSize > m_buffSize) {
                m_buffSize = newSize;
            }
            m_buff = (char*) realloc (m_buff, sizeof(char) * m_buffSize);
        }
    }

    void encodeChar (char c) {
        ensure (1);
        m_buff[m_buffPos++] = c;
    }
    
    void encodeString (char * s) {
        int l = strlen (s) + 1;
        if (l > 1) {
            ensure (l);
            memcpy (m_buff+m_buffPos, s, l);
            m_buffPos += l;
        } else {
            encodeChar (0);
        }
    }

    void initDone () {
        m_dumpTable = true;
        encodeChar ('B');
        encodeChar ('M');
        encodeChar ('L');
        encodeChar ('1');
        dumpTable ();
    }

    int getTagId (char * name) { 
        int id = m_tags == NULL ? -1 : m_tags->find (name);
        if (id < 0) {
            appendTag (new Tag (name, id = m_nbTags++, NULL));
        }
        return id;
    }

    void appendTag (Tag *t) {
        if (m_tags == NULL) {
            m_tags = t;
        } else if (m_lastTag != NULL) {
            m_lastTag->m_next = t;
        }
        m_lastTag = t;
    }

    void encodeSize (int size) {
        if (size > 255) {
            int n = size / 255;
            encodeChar (255);
            encodeChar (n);
            size -= 255*n;
        }
        encodeChar (size);
    }

    void dumpTable () {
        Tag * tag = m_tags;
        encodeSize (m_nbTags);
        while (tag != NULL) {
            encodeString (tag->m_name);
            tag = tag->m_next;
        }
    }

    void setLeave (char * l) {
        if (m_dumpTable) {
            encodeChar (3);
            encodeString (l);
        }
    }

    void open (char * t, bool selfClosing) {
        int id = getTagId (t); // create the entry if necessary
        if (m_dumpTable) {
            encodeChar (selfClosing ? 2 : 1);
            encodeSize (id);
        }
    }

    void close (char * t) {
        if (m_dumpTable) {
            encodeChar (0);
        }
    }

    void endOfAttributes (bool selfClosing) {
        if (m_dumpTable) {
            encodeChar (0);
            if (selfClosing) {
                encodeChar (0); // double 0 because close will not be called
            }
        }
    }

    void addAttribute (char * name, char * value) {
        int id = getTagId (name); // create the entry if necessary
        if (m_dumpTable) {
            encodeSize (id+1);
            encodeString (value);
        }
    }
};


Encoder::Encoder (char * in, char * out, bool verbose, bool decode, char * charset) {
    m_root = NULL;
    s_debug = verbose;
    if (endsWith (in, "xml") || (strcmp (in, "-") == 0 && decode == false)) {
        if (!parseXml (in, charset)) {
            fprintf (stderr, "Error during XML parsing\n");
            exit (1);
        }
        FILE * fp = NULL;
        if (strcmp (out, "-") == 0) { // stdout
            fp = stdout;
        } else if (endsWith (out, ".bml")) {
            fp = fopen (out, "w");
        }
        if (fp != NULL) {
            BmlEncoder e;
            m_root->visit (&e); // parse a first time to compute ids table
            e.initDone (); // dump the id table
            m_root->visit (&e); // parse a second time to encode the content
            int size;
            char * buff = e.getBuffer (size);
            fwrite (buff, 1, size, fp);
            if (fp != stdout) {
                fclose (fp);
            }
        } else {
            fprintf (stderr, "Cannot open %s for writing", out);
        }
    } else if (endsWith (in, "bml") || (strcmp (in, "-") == 0 && decode)) {
        if (!parseBml (in)) {
            fprintf (stderr, "Error during BML parsing\n");
            exit (1);
        }
        FILE * fp = NULL;
        if (strcmp (out, "-") == 0) { // stdout
            fp = stdout;
        } else if (endsWith (out, ".xml")) {
            fp = fopen (out, "w");
        }
        if (fp != NULL) {
            XmlPrinter p (fp);
            m_root->visit (&p);
            if (fp != stdout) {
                fclose (fp);
            }
        } else {
            fprintf (stderr, "Cannot open %s for writing", out);
        }
    } else {
        fprintf (stderr, "Input file must be xml OR bml\n");
        exit (1);
    }
}

Encoder::~Encoder () {
    delete m_root; // m_root has no m_next
}

bool Encoder::parseBml (char * in) {
    FILE * fp;
    if (strcmp (in, "-") == 0) {
        fp = stdin;
    } else {
        fp = fopen (in, "r");
    }
    if (fp == NULL) {
        fprintf (stderr, "Cannot open %s for reading\n", in);
        exit (1);
    }
    BmlDecoder b (fp);
    m_root = b.getRoot ();
    return m_root != NULL;
}

bool Encoder::parseXml (char * in, char * charset) {
    FILE * fp;
    if (strcmp (in, "-") == 0) {
        fp = stdin;
    } else {
        fp = fopen (in, "r");
    }
    if (fp == NULL) {
        fprintf (stderr, "Cannot open %s for reading\n", in);
        exit (1);
    }
    long size = 0;
    char * data = NULL;
    if (fp != stdin) {
      fseek (fp, (long)0, SEEK_END);
      size = ftell (fp);
      fseek (fp, (long)0, SEEK_SET);
      data = (char*) malloc (size+1);
      fread (data, 1, size, fp);
      fclose (fp);
    } else { // no support of SEEK in stdin
      const int chunkSize = 1024*10;
      int buffSize = 0;
      int read = chunkSize;
      char * ptr;
      while (read == chunkSize) {
        buffSize += chunkSize;
        data = (char*) realloc (data, buffSize);
        ptr = data + size;
        read = fread (ptr, 1, chunkSize, fp);
        size += read;
      }
    }
    data[size] = '\0';
    XmlReader t (data, charset);
    m_root = t.parseNode (NULL);
    free (data);
    return m_root != NULL;
}

void usage (char * exeName) {
    fprintf (stderr, "%s [-v] [--charset CHARSET] [--toXml] inFile outFile\n\n", exeName);
    fprintf (stderr, "options:\n");
    fprintf (stderr, "  -v : prints some messages about the processing.\n");
    fprintf (stderr, "  --charset : force the charset for the xml document (default is UTF-8, or retrieved from the xml declaration).\n");
    fprintf (stderr, "  --toXml : inform the program that stdin will be BML and output will be XML.\n");
    fprintf (stderr, "            this is usefull when using - - as file names for stdin/stdout mode.\n\n");
    fprintf (stderr, "usage: \n%s file.xml file.bml\n", exeName);
    fprintf (stderr, "    convert a xml file into a bml file\n");
    fprintf (stderr, "\n%s file.bml file.xml\n", exeName);
    fprintf (stderr, "    convert a xml file into a bml file\n");
    fprintf (stderr, "usage: \n%s --charset ISO-8859-15 file.xml file.bml\n", exeName);
    fprintf (stderr, "    convert a xml file encoded in ISO-8859-15 into a bml file\n");
    fprintf (stderr, "\n%s - -\n", exeName);
    fprintf (stderr, "    convert xml from stdin to bml on stdout.\n");
    fprintf (stderr, "\n%s --toXml - -\n", exeName);
    fprintf (stderr, "    convert bml from stdin to xml on stdout.\n");
    exit (1);
}

int main (int argc, char * argv []) {
    int i;
    s_debug = false;
    bool decode = false;
    char * charset = NULL;
    for (i = 1; i < argc; i++) {
        if (strcmp (argv[i], "-p") == 0) {
            //propertyFile = argv [++i];
        } else if (strcmp (argv[i], "-v") == 0) {
            s_debug = true;
        } else if (strcmp (argv[i], "--toXml") == 0) {
            decode = true;
        } else if (strcmp (argv[i], "--charset") == 0) {
            charset = argv[++i];
        } else {
            break;
        }
    }
    if ( (argc - i) < 2) {
        usage (argv[0]);
    }
    Encoder encoder  (argv[i], argv[i+1], s_debug, decode, charset);
}
