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
# include "unistd.h"

class InputStream {
    char * m_fn;
    int m_line;
public:
    InputStream * prec;
public:
    InputStream () { m_line = 1; }
    virtual ~InputStream () { if (m_fn) free(m_fn); }
    virtual char getc () = 0;
    virtual void ungetc (char c) = 0;
    void setLine (int l) { m_line = l; }
    void incLine () { m_line++; }
    int getLine () { return m_line; }
    void setFile(const char * fn) { m_fn = strdup(fn); }
    const char* getFile() { return m_fn; }
};

class FileInputStream : public InputStream {
    FILE * m_fp;
public:
    FileInputStream (FILE * fp, InputStream * p) { m_fp = fp; prec = p; }
    ~FileInputStream () { fclose (m_fp); }
    char getc () { return fgetc (m_fp); }
    void ungetc (char c) { ::ungetc (c, m_fp); }
};

class CharInputStream : public InputStream {
    char * m_buffer, * m_current;
public:
    CharInputStream (char * buffer, InputStream * p) {
        m_buffer = m_current = buffer; 
        prec = p; 
    }
    char getc () { return *m_current ? *m_current++ : -1;  }
    void ungetc (char c) {
        if (m_current > m_buffer) {
            *--m_current = c;
        }
    }
};

class StackStream  {
    InputStream * is;
public:
    StackStream (FILE * fp) { is = NULL; push (fp);  }
    StackStream (char * buffer) { is = NULL; push (buffer);  }

    void push (FILE * fp) {
        is = new FileInputStream (fp, is);
    }

    void push (char * buffer) {
        is = new CharInputStream (buffer, is);
    }

    char getc () {
        char c = EOF;
        if (is) {
            if ( (c = is->getc ()) == EOF) {
                is = is->prec;
                c = getc ();
            }
        }
        return (c);
    }

    void ungetc (char c) {
        if (is) {
            is->ungetc (c);
        }
    }
    void setLine (int l) { if (is) { is->setLine (l); } }
    void incLine () { if (is) { is->incLine (); } }
    int getLine () { return is ? is->getLine() : 0; }
    void setFile(const char * fn) { if (is) is->setFile(fn); }
    const char* getFile() { return is != NULL ? is->getFile() : "???"; }
};

class Tokenizer {
    StackStream * m_is;
    char buffer [64*2048];
    bool m_eof;
    //int line;
    bool m_isScript;
    char comma, comment, quote, minus;
    char * m_token;
public:
    Tokenizer (FILE * fp, bool isScript, const char* fn) {
        m_is = new StackStream (fp);
        init (isScript);
        m_is->setFile(fn);
    }

    Tokenizer (char * buffer, bool isScript, const char * fn, int startLine) {
        m_is = new StackStream (buffer);
        init (isScript);
        m_is->setFile(fn);
        m_is->setLine (startLine);
    }

    void init (bool isScript) {
        m_eof = false;
        m_is->setLine (1);
        m_token = NULL;
        m_isScript = isScript;
        comma = isScript ? -1 : ',';
        comment = isScript ? '/' : '#';
        quote = isScript ? '\'' : '"';
        minus = isScript ? 'a' : '-'; // a is harmless
    }
    
    ~Tokenizer () {
    }
    
    char GETC () {
        char c = m_is->getc();
        m_eof = c == -1;
        return (c);
    }

    void UNGETC (char c) {
        m_is->ungetc (c);
    }
    
    bool _EOF () { return m_eof; }
    
    void push (char * t) {
        for (int i = strlen (t)-1; i >= 0; i--) {
            UNGETC (t[i]);
        }
    }
    
    bool checkToken (const char * token) {
        char * tmp = getNextToken ();
        if (tmp == NULL || strcmp (tmp, token) != 0) {
            m_token = tmp;
            return false;
        }
        return (true);
    }

    char * getNextToken () {
        if (m_token != NULL) {
            char * tmp = m_token;
            m_token = NULL;
            return tmp;
        }
        skipSpace ();
        char * r = buffer;
        char c = GETC ();
        if (!_EOF () && isToken (c)) {
            *r++ = c;
            c = GETC ();
            while ( isToken (c) || isInt (c) || c == minus) {
                *r++ = c;
                c = GETC ();
            }
        }
        UNGETC (c);
        *r='\0';
        //fprintf (stderr, "getNextToken: '%s'\n", buffer);
        return (*buffer ? strdup (buffer) : NULL);
    }
    
    int getNextInt () {
        skipSpace ();
        char * r = buffer;
        char c = GETC ();
        if (c == '-' || isInt (c)) {
            *r++ = c;
            c = GETC ();
            while (isInt (c)) {
                *r++ = c;
                c = GETC ();
            }
        }
        UNGETC (c);
        *r='\0';
        //fprintf (stderr, "getNextInt: %s\n", buffer);
        return (atoi (buffer));
    }
    
    float getNextFloat (bool * isOK = NULL, bool * intFlag = NULL) {
        if (intFlag) { *intFlag = true; }
        skipSpace ();
        char c = GETC ();
        if (c == '0' && CHECK ('x')) { // check for hexa header
          return getNextHex();
        }
        char * r = buffer;
        if (c == '-'  || c == '+') { // must be followed IMMEDIATELY by a number
            *r++ = c;
            char cs = c;
            c = GETC ();
            if (!isInt (c) && c != '.') {
                UNGETC (c);
                UNGETC (cs);
                if (isOK) { *isOK = false; }
                return 0;
            }
        }
        while (isInt (c)) { // integer part
            *r++ = c;
            c = GETC ();
        }
        if (c == '.') { // float part
            if (intFlag) { *intFlag = false; }
            *r++ = c;
            c = GETC();
            while (isInt (c)) {
                *r++ = c;
                c = GETC ();
            }
        }
        if (c == 'e' || c == 'E') { // exp part
            if (intFlag) { *intFlag = false; }
            *r++ = c;
            c = GETC();
            if (c == '-'  || c == '+') { // must be followed IMMEDIATELY by a number
                *r++ = c;
                char cs = c;
                c = GETC ();
                if (!isInt (c)) {
                    UNGETC (c);
                    UNGETC (cs);
                    if (isOK) { *isOK = false; }
                    return 0;
                }
            }
            while (isInt (c)) {
                *r++ = c;
                c = GETC();
            }
        }
        UNGETC (c);
        *r='\0';
        if (isOK) {
            *isOK = strlen (buffer) > 0;
        }
        //fprintf (stderr, "getNextFloat: %s\n", buffer);
        return (atof (buffer));
    }

    int getNextHex () {
        int r=0, t=0, n=0;
        char c = GETC ();
        while (n++ < 8) {
            if((c >= '0') && (c <= '9')) {
                t = (c - '0');
            } else if ((c >= 'A') && (c <= 'F')) {
                t = (c - 'A' + 10);
            } else if ((c >= 'a') && (c <= 'f')) {
                t = (c - 'a' + 10);
            } else {
              UNGETC (c);
              break;
            }
            r *= 16; 
            r += t;
            c = GETC ();
        }
        return r;
    }
    
    char * getNextString () {
        skipSpace ();
        if (!check (quote)) {
            return (NULL);
        }
        char c = GETC ();
        char * r = buffer;
        while (!_EOF () && c != quote) {
            if (c == '\n') {
                m_is->incLine();
            } else if (c == '\\') {
                c = (char)getEscape();
            }
            *r++ = c;
            c = GETC ();
        }
        *r='\0';
        //fprintf (stderr, "getNextString: '%s'\n", buffer);
        return (strdup (buffer)); //*buffer ? strdup (buffer) : NULL);
    }
    
    int getEscape () {
        int c = GETC ();
        if (c >= '0' && c <= '9') {
            int t = c - '0';
            t = t*8 + GETC () - '0';
            t = t*8 + GETC () - '0';
            return (t);
        }
        switch (c) {
        case 'U': return (16); // ^P
        case 'D': return (14); // ^N
        case 'R': return (6); // ^F
        case 'L': return (2); // ^B
        case 't': return (9); // ^I
        case 'n': return (10); // ^J
        case 'r': return (13); // ^M
        default: return (c);
        }
    }
    
    bool check (char target, bool pushBack = false) {
        skipSpace ();
        return CHECK (target, pushBack);
    }

    bool CHECK (char target, bool pushBack = false) {
        char c = GETC ();
        if (c == target) {
            if (pushBack) { UNGETC (c); }
            return (true);
        }
        UNGETC (c);
        return (false);
    }
    
    void include (char * filename) {
        //fprintf (stderr, "Tokenizer.h: including %s\n", filename);
        FILE * fp = fopen (filename, "r");
        if (fp) {
            m_is->push (fp);
            m_is->setFile(filename);
        } else {
            fprintf (stderr, "%s:%d: cannot include %s\n", getFile(), getLine (), filename);
        }
    }
    
    void include (FILE * fp, char * filename) {
        if (fp) {
            m_is->push (fp);
            m_is->setFile(filename);
        } else {
            fprintf (stderr, "%s:%d: cannot include %s\n", getFile(), getLine (), filename);
        }
    }

    void skipSpace () {
        char c = GETC ();
        while (!_EOF () && isSpace (c)) {
            if (c == '\n') m_is->incLine();
            if (m_isScript && c == '/') {
                c = GETC ();
                if (c == '/') {
                    c = GETC ();
                    while (!_EOF () && c != '\n') {
                        c = GETC ();
                    }
                    m_is->incLine();
                } else {
                    UNGETC (c);
                    UNGETC ('/');
                    return;
                }
            } else if (!m_isScript && c == '#') {
                if (false && checkToken ("include")) {
                    include (getNextString ());
                } else {
                    m_token = NULL;
                    while (!_EOF () && c != '\n') {
                        c = GETC ();
                    }
                }
                m_is->incLine();
            } 
            c = GETC ();
        }
        UNGETC (c);
    }
    int getLine () { return m_is->getLine(); }
    const char* getFile() { return m_is->getFile(); }
    
private:
    bool isToken (char c) { return ( (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_'); }
    bool isInt (char c) { return ( (c >= '0' && c <= '9') ); }
    bool isSpace (char c) { return (c == ' ' || c == '\t' || c == comma || c == '\r' || c == '\n') || c == comment; }
};
