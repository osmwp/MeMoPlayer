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
# include <string.h>

# define MESSAGE(...) if (s_debug) fprintf (stderr, __VA_ARGS__)

class XmlReader;
class XmlAttribute;
class XmlNode;

# define TOK_BUFFER_SIZE 409600
# define TOK_MAX_PARAMS 32

class XmlVisitor {
public:
    virtual void setLeave (char * name) = 0;
    virtual void open (char * name, bool selfClosing) = 0;
    virtual void close (char * name) = 0;
    virtual void endOfAttributes (bool selfClosing) = 0;
    virtual void addAttribute (char * name, char * value) = 0;

};

class XmlPrinter : public XmlVisitor {
    int counter;
    FILE * m_fp;

public:
    XmlPrinter (FILE * fp) {
        counter = 0;
        m_fp = fp;
    }

    void printSpaces (int n) {
        while (n-- > 0) {
            fputc (' ', m_fp);
        }
    }

    void setLeave (char * l) {
        printSpaces (counter);
        fprintf (m_fp, "%s\n", l);
    }

    void open (char * t, bool selfClosing) {
        printSpaces (counter);
        fprintf (m_fp, "<%s", t);
        counter += 4;
    }

    void endOfAttributes (bool selfClosing) {
        if (selfClosing) {
            counter -= 4;
            fprintf (m_fp, "/>\n");
        } else {
            fprintf (m_fp, ">\n");
        }
    }

    void close (char * t) {
        counter -= 4;
        printSpaces (counter);
        fprintf (m_fp, "</%s>\n", t);
    }

    void addAttribute (char * name, char * value) {
        fprintf (m_fp, " %s=\"%s\"", name, value);
    }

};

class XmlAttribute {
public:
    char * m_name;
    char * m_value;
    int m_id;
    XmlAttribute * m_next;

    XmlAttribute (char * name, char * value);
    ~XmlAttribute ();
    void visit (XmlVisitor *);
};

#define OPEN_TAG      1
#define CLOSE_TAG     2
#define SELF_TAG 3
#define CDATA        4

class XmlNode {
public:
    char * m_name;
    int  m_type;
    XmlAttribute * m_attributes;
    int m_id;
    XmlNode * m_children;
    XmlNode * m_next;
    int m_nbChildren;
    int m_nbAttributes;

    XmlNode (char * name, int type = CDATA);

    ~XmlNode ();

    bool isClosing (XmlNode * n);

    void addChild (XmlNode * n);

    void addAttribute (XmlAttribute * a);

    bool parseAttributes (XmlReader * p);

    void visit (XmlVisitor *);
};

class XmlReader {
public:
    char * m_buffer; // the data to be parsed
    int m_pos; // current char being parsed 
    int m_len; // length of buffer
    int m_nbLines; // the current line number
    char * m_sb; // a temporary buffer to parse CDATA
    char * m_charBuf; // a temporary buffer to html char (ex: &deg;)
    int m_sbSize, m_charBufSize;

    XmlReader (char * buffer);

    ~XmlReader ();
    
    // return the current char to read or '\0' if end of buffer
    char getChar ();

    // return the next char to read or '\0' if end of buffer
    char getNextChar ();

    // return true and advance to the next char if the current char is equal to the parameter
    bool eatChar (char c);

    // advance to the next non white char
    char skipSpaces ();

    char getHtmlChar ();
    
    // return a string or null is next thing is not a string (i.e. first char is not a '"')
    char * getString ();
    
    // return the next identifier for a tag or an attribute name
    char * getNextIdent (bool startsWithAlpha);
    
    char * getNextToken ();

    char * getNextStrictToken ();

    char * getNextNumber ();

    bool isWhite (char c) { return c == ' ' || c == '\n' || c == '\t' || c == '\r'; }
    bool isAlphaNum (char c) { return isAlpha (c) || isNumber (c) || c == '-' || c == '+' || c == '.'; }
    bool isNumber (char c) { return (c >= '0' && c <= '9'); }
    bool isAlpha (char c) { return ( (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_'); }
    bool isSpecial (char c) { return (c == '-' || c == ':'  || c == '.' || c == '#' || c == '%'); }


    bool parseSpecial (char c1, char c2, const char * s);

    XmlNode * parseElement ();

    XmlNode * parseCData ();
    
    XmlNode * parseTag (char c);

    XmlNode * parseNode (XmlNode * node);


private:
    // Add char to buffers with dynamic resize
    void setSb (int position, char c);
    void setCharBuf (int position, char c);
};

