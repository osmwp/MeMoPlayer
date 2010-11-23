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

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>

#include "XmlReader.h"
//# include "Trace.h"

extern bool s_debug;

bool startsWith (const char * buffer, const char * target, int offset) {
    return strncmp (buffer+offset, target, strlen (target)) == 0;
}

int indexOf (const char * buffer, const char * target, int offset) {
    int idx = strstr (buffer+offset, target) - (buffer+offset);
    return idx >= 0 ? idx : -1;
}


XmlAttribute::XmlAttribute (char * name, char * value) {
    m_name = name;
    m_value = value;
    m_next = NULL;
    m_id = -1;
}

XmlAttribute::~XmlAttribute () {
    free (m_name);
    free (m_value);
    m_next = NULL;
}

void XmlAttribute::visit (XmlVisitor * v) {
    v->addAttribute (m_name, m_value);
    if (m_next) {
        m_next->visit (v);
    }
}

XmlNode::XmlNode (char * name, int type) {
    m_name = name;
    m_next = NULL;
    m_type = type;
    m_id = -1;
    m_children = NULL;
    m_attributes = NULL;
    m_nbChildren = 0;
    m_nbAttributes = 0;
}

XmlNode::~XmlNode () {
    free (m_name);
    m_next = NULL;
    // Clean children
    XmlNode * child = m_children;
    while (child != NULL) {
      XmlNode * next = child->m_next;
      delete child;
      child = next;
    }
    m_children = NULL;
    // Clean attributes
    XmlAttribute * attr = m_attributes;
    while (attr != NULL) {
      XmlAttribute * next = attr->m_next;
      delete attr;
      attr = next;
    }
    m_attributes = NULL;
}

bool XmlNode::isClosing (XmlNode * n) {
    return (n != NULL && m_type == CLOSE_TAG && strcasecmp (m_name, n->m_name) == 0);
}

void XmlNode::addChild (XmlNode * n) {
    if (n != NULL) {
        if (m_children == NULL) {
            m_children = n;
        } else {
            XmlNode * tmp = m_children;
            while (tmp->m_next != NULL) {
                tmp = tmp->m_next;
            }
            tmp->m_next = n;
        }
        n->m_next = NULL;
        //n->m_parent = this;
        m_nbChildren++;
    }
}

void XmlNode::addAttribute (XmlAttribute * a) {
    if (m_attributes == NULL) {
        m_attributes = a;
    } else {
        XmlAttribute * tmp = m_attributes;
        while (tmp->m_next != NULL) {
            tmp = tmp->m_next;
        }
        tmp->m_next = a;
    }
    a->m_next = NULL;
    m_nbAttributes++;
}


void XmlNode::visit (XmlVisitor * v) {
    if (m_type == CDATA) {
        v->setLeave (m_name);
    } else if (m_type == SELF_TAG) {
        v->open (m_name, true);
        if (m_attributes != NULL) {
            m_attributes->visit (v);
        }
        v->endOfAttributes (true);
    } else if (m_type == OPEN_TAG) {
        v->open (m_name, false);
        if (m_attributes != NULL) {
            m_attributes->visit (v);
        }
        v->endOfAttributes (false);
        if (m_children != NULL) {
            m_children->visit (v);
        }
        v->close (m_name);
    }
    if (m_next != NULL) {
        m_next->visit (v);
    }
}


XmlReader::XmlReader (const char * buffer, const char * encoding) {
    m_nbLines = 1;
    m_sbSize = 4096;
    m_sb = (char*) malloc (sizeof(char) * m_sbSize);
    m_charBufSize = 4096;
    m_charBuf = (char*) malloc (sizeof(char) * m_charBufSize);
    m_buffer = buffer; // will not be freed by this class
    m_len = strlen (buffer);
    m_pos = 0;
    m_iconv = NULL;
    if (encoding) {
        iconv_t iv = iconv_open ("UTF-8", encoding);
        if (iv == (iconv_t)-1) { // unsupported target encoding
            MESSAGE ("Warning: ignoring unsupported encoding: %s\n", encoding);
        } else {
          m_iconv = iv;
        }
    }
}

XmlReader::~XmlReader () {
    free (m_sb);
    free (m_charBuf);
    if (m_iconv) {
        iconv_close (m_iconv);
    }
}

void XmlReader::setSb (int position, char c) {
    if (position >= m_sbSize) {
        m_sbSize *= 2;
        m_sb = (char*) realloc (m_sb, sizeof(char) * m_sbSize);
    }
    m_sb[position] = c;
}

void XmlReader::setCharBuf (int position, char c) {
    if (position >= m_charBufSize) {
        m_charBufSize *= 2;
        m_charBuf = (char*) realloc (m_charBuf, sizeof(char) * m_charBufSize);
    }
    m_charBuf[position] = c;
}

char * XmlReader::toUTF8 (char * string, int size) {
    int multiplier = 2; // initial multiplier between original size and maximum buffer size
    while (m_iconv != NULL) {
        int maxSize = size * multiplier;
        if (m_charBufSize < maxSize) { // ensure maxSize for dest buffer
            fprintf (stderr, "realloc to: %i\n", maxSize);
            m_charBufSize = maxSize;
            m_charBuf = (char*) realloc (m_charBuf, sizeof(char) * m_charBufSize);
        }
        size_t inSize = size;
        char * inBuff = string;
        size_t outSize = m_charBufSize;
        char * outBuff = m_charBuf;
        int ret = iconv (m_iconv, &inBuff, &inSize, &outBuff, &outSize);
        if (ret == -1) {
            if (errno == E2BIG && multiplier == 2) {
                // not enough space in m_charBuf, retry with a bigger multiplier
                // an UTF-8 string can only be 4 time bigger (worst case) than a one byte encoding string
                multiplier = 4;
                continue;
            }
            MESSAGE ("toUTF8: could not convert string to UTF8: %s\n", string);
            return NULL;
        }
        *outBuff = 0; // mark end of string
        return strdup (m_charBuf);
    }
    // m_iconv is not initialized (UTF-8 conversion not required)
    return strdup (string);
}

// return the current char to read or '\0' if end of buffer
char XmlReader::getChar () {
    return m_pos < m_len ? m_buffer[m_pos] : '\0';
}

// return the next char to read or '\0' if end of buffer
char XmlReader::getNextChar () {
    m_pos++;
    return getChar ();
}

// return true and advance to the next char if the current char is equal to the parameter
bool XmlReader::eatChar (char c) {
    if (getChar () == c) {
        m_pos++;
        return true;
    }
    return false;
}

// advance to the next non white char
char XmlReader::skipSpaces () {
    char c = getChar ();
    while (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
        if (c == '\n') { m_nbLines++; }
        c = getNextChar ();
    }
    return c;
}

// Tabernacle, c'est pas magique comme formule ?
char convert (int c1, int c2) {
    char c = 0;
    c |= ((c1 & 0x1f) << 6);     // 00011111
    c |= ((c2 & 0x3f) << 0);     // 00111111
    return c;
}

char XmlReader::getHtmlChar () {
    //m_charBuf.setLength (0);
    int curPos = 0;
    int oldPos = m_pos;
    int i = 0;
    char c = getNextChar ();
    if (c == '#') {
        c = getNextChar ();
        int base = 10;
        if (c == 'x') {
            base = 16;;
            c = getNextChar ();
        }
        while ((c >= '0' && c <= '9')
               || (base == 16 && c >= 'a' && c <= 'f')
               || (base == 16 && c >= 'A' && c <= 'F') ) {
            if (c >= '0' && c <= '9') {
                i = base * i + (c-'0');
            } else if (c >= 'A' &&  c <= 'F') {
                i = base * i + (10 + c - 'A');
            } else if (c >= 'a' &&  c <= 'f') {
                i = base * i + (10 + c - 'a');
            }
            c = getNextChar ();
        }
        if (c != ';') {
            MESSAGE ("getString: expecting ';' instead of '%c' while parsing &#xyz;\n", c);
        }
        return (char)i;
    }
    while (c != '\0' && c != ';' && i++ < 10) {
        setCharBuf (curPos++, c);
        c = getNextChar ();
    }
    setCharBuf (curPos, 0);
    if (strcmp (m_charBuf, "amp") == 0) {
        return '&';
    } else if (strcmp (m_charBuf, "lt") == 0) {
        return '<';
    } else if (strcmp (m_charBuf, "gt") == 0) {
        return '>';
    } else if (strcmp (m_charBuf, "apos") == 0) {
        return '\'';
    } else if (strcmp (m_charBuf, "quote") == 0) {
        return '"';
    } else if (strcmp (m_charBuf, "deg") == 0) {
        return convert (0xC2, 0xB0);
    } else if (strcmp (m_charBuf, "nbsp") == 0) {
        return convert (0xC2, 0xA0);
    } else if (strcmp (m_charBuf, "ecirc") == 0) {
        return convert (0xC3, 0xAA);
    } else if (strcmp (m_charBuf, "eacute") == 0) {
        return convert (0xC3, 0xA9);
    } else if (strcmp (m_charBuf, "egrave") == 0) {
        return convert (0xC3, 0xA8);
    } else if (strcmp (m_charBuf, "agrave") == 0) {
        return convert (0xC3, 0xE0);
    } else if (strcmp (m_charBuf, "ccedil") == 0) {
        return convert (0xC3, 0xE7);
    } else {
        m_pos = oldPos+1;
        return '&';
    }
}

int getHexDigit (char c) {
    if ( (c >= '0') && (c <= '9') ) {
        return c - '0';
    }
    if ( (c >= 'A') && (c <= 'F') ) {
        return 10 + (c - 'A');
    }
    if ( (c >= 'a') && (c <= 'f') ) {
        return 10 + (c - 'a');
    }
    return 0;
}

// return a string or NULL is next thing is not a string (i.e. first char is not a '"')
char * XmlReader::getString () {
    int utf = 0;
    skipSpaces ();
    char quote = '"';
    if (!eatChar (quote)) {
        quote = '\''; // single quoted string ?
        if (!eatChar (quote)) {
            return NULL;
        }
    }
    int curPos = 0; // m_sb.setLength (0);
    char c = getChar ();
    while ( c != '\0' && c != quote ) {
        if (c == '\\') {
            c = getNextChar ();
            switch (c) {
            case '"': c = '"'; break;
            case '\\': c = '\\'; break;
            case '/' : c = '/'; break;
            case 'b' : c = '\b'; break;
            case 'f' : c = '\f'; break;
            case 'n' : c = '\n'; break;
            case 'r' : c = '\r'; break;
            case 't' : c = '\t'; break;
            case 'u' :
                utf = 0;
                utf += getHexDigit (getNextChar ()) << 12;
                utf += getHexDigit (getNextChar ()) << 8;
                utf += getHexDigit (getNextChar ()) << 4;
                utf += getHexDigit (getNextChar ());
                c = (char)utf;
                break;
            }
        } else if (c == '&') {
            c = getHtmlChar ();
        } else if (c == '\n') {
            m_nbLines++;
        }
        setSb (curPos++, c);
        c = getNextChar ();
    }
    if (eatChar (quote)) {
        setSb (curPos, 0);
        return toUTF8 (m_sb, curPos);
    } else {
        return NULL;
    }
}

char * strdup (const char * s, int start, int end) {
    int len = (end-start);
    char * tmp = (char *)malloc (len+1);
    strncpy (tmp, s+start, len);
    tmp [len] = 0;
    return tmp;
}

// return the next identifier for a tag or an attribute name
char * XmlReader::getNextIdent (bool startsWithAlpha) {
    int startPos = m_pos;
    char c = getChar ();
    if (startsWithAlpha && isAlpha (c) == false) { return NULL; }
    while ( isAlpha (c) || isNumber (c) || isSpecial (c) ) {
        c = getNextChar ();
    }
    //data = m_buffer.substring(m_pos, p);
    return strdup (m_buffer, startPos, m_pos);
}

char * XmlReader::getNextToken () {
    skipSpaces ();
    return getNextIdent (true);
}

char * XmlReader::getNextStrictToken () {
    skipSpaces ();
    int p = m_pos;
    char c = getChar ();
    if (isAlpha (c) == false) { return NULL; }
    while ( isAlpha (c) || isNumber (c) ||  (c == '-') ) {
        c = getNextChar ();
    }
    return strdup (m_buffer, p, m_pos);
}

char * XmlReader::getNextNumber () {
    skipSpaces ();
    return getNextIdent (false);
}

bool XmlReader::parseSpecial (char c1, char c2, const char * s) {
    // should check for "--"
    if (getNextChar () != c1 || getNextChar () != c2) {
        return false;
    }
    //int p = m_buffer.indexOf (s, m_pos);
    int p = strstr (m_buffer+m_pos, s) - (m_buffer+m_pos);
    if (p < 0) {
        return false;
    } // comment not terminated
    m_pos += p+strlen (s);
    return true;
}

bool XmlReader::parseXmlHeader () {
    eatChar ('?');
    char * name = getNextToken ();
    if (name == NULL || strcmp (name, "xml") != 0) {
        MESSAGE ("Error: XML must start with a valid xml declaration <?xml ?> !'\n");
        return false;
    }
    XmlAttribute * attr = parseAttribute ();
    while (attr != NULL) {
        if (m_iconv == NULL && strcmp (attr->m_name, "encoding") == 0 && strcmp (attr->m_value, "UTF-8") != 0) {
            // Setup caracter conversion as encoding is not UTF-8
            m_iconv = iconv_open ("UTF-8", attr->m_value);
            if (m_iconv == (iconv_t)-1) { // unsupported target encoding
                MESSAGE ("Error: unsupported encoding in xml declaration: %s\n", attr->m_value);
                return false;
            }
        }
        delete attr;
        attr = parseAttribute ();
    }
    if (eatChar ('?') && eatChar ('>')) {
      return true;
    }
    MESSAGE ("Error: <?xml declaration must end with a valid '?>' value !'\n");
    return false;
}

XmlNode * XmlReader::parseElement () {
    //XmlNode * e = NULL;
    char c = skipSpaces ();
    if (c == '\0') { // end of data
        return NULL;
    } else if (c == '<') { // we have a XML fragment
        c = getNextChar ();
        if (c == '?') {
            if (parseXmlHeader ()) {
                return parseElement ();
            }
            return NULL;
        } else if (c == '!') {
            if (startsWith (m_buffer, "![CDATA[", m_pos)) { // pure XML CDATA Definition
                m_pos+=8; // skip header ![CDATA[
                int end = indexOf (m_buffer, "]]>", m_pos);
                if (end == -1) {
                    return NULL; // CDATA not terminated
                }
                char * data = strdup (m_buffer, m_pos, m_pos+end);
                if (m_iconv) {
                    char * orig = data;
                    data = toUTF8 (orig, end);
                    free (orig);
                }
                m_pos += end+3;  // avoid trailing ]]>
                return new XmlNode (data, CDATA);
            }
            if (parseSpecial ('-', '-', "-->")) { // ignore comments <!-- -->
                return parseElement ();
            } else if (parseSpecial ('D', 'O', ">")) { // ignore external document type declaration <!DOCTYPE >
                // TODO: support parsing (and ignoring) internal doctype declaration
                return parseElement ();
            }
            return NULL;
        }
        return parseTag (c);
    }
    return parseCData ();
}

XmlNode * XmlReader::parseCData () {
    int currentPos = 0;
    char c = getChar ();
    while (c != '\0' && c != '<') {
        if (c == '&') {
            c = getHtmlChar ();
        } else if (c == '\n') {
            m_nbLines++;
        }
        setSb (currentPos++, c);
        // check for '>'
        c = getNextChar ();
    }
    setSb (currentPos, 0);
    return new XmlNode (toUTF8 (m_sb, currentPos), CDATA);
}

XmlNode * XmlReader::parseTag (char c) {
    c = skipSpaces (); // '<' has been eaten to check for a comment
    bool closing = false;
    if (c == '/') { // ending tag
        closing = true;
        c = getNextChar ();
    }
    char * name = getNextToken ();
    if (name == NULL) {
        MESSAGE ("Error: XML tags must start with an alpha caracter !'");
        return NULL;
    }
    XmlNode * e = new XmlNode (name, closing ? CLOSE_TAG : OPEN_TAG);
    if (!closing) { // may have some attributes
        XmlAttribute * attr = parseAttribute ();
        while (attr != NULL) {
            e->addAttribute (attr);
            attr = parseAttribute ();
        }
    }
    // final '>'
    c = getChar ();
    if (c == '/') { // self tag
        if (closing) {
            MESSAGE ("Error: closing is also self closing: %s", e->m_name);
            return NULL;
        }
        e->m_type = SELF_TAG;
        c = getNextChar ();
    }

    // check for nodes that shoudl be self closing: BR, HR, IMG
//     if (m_htmlMode && e->m_type == OPEN_TAG) {
//         e.m_type = checkHtmlSelfClosing(e.m_name);
//     }
    if (c != '>') { // ending tag
        MESSAGE ("Error: got '%c' instead of '>'", c);
        return NULL;
    }
    getNextChar (); // eat '>'
    return e;
}

XmlNode * XmlReader::parseNode (XmlNode * node) {
    if (node == NULL) {
        if ( (node = parseElement ()) == NULL) {
            MESSAGE ("Error: cannot parse node at all\n");
            return NULL; // parsing problem
        }
    }
    if (node->m_type == OPEN_TAG) {
        // read while CLOSE_TAG
        XmlNode * child = parseElement ();
        while (child != NULL && child->isClosing (node) == false) {
            if (child->m_type == CDATA) {
                node->addChild (child);
            } else if (child->m_type == SELF_TAG) {
                node->addChild (child);
            } else if (child->m_type == OPEN_TAG) {
                node->addChild (parseNode (child));
            } else if (child->m_type == CLOSE_TAG) { // not closing the right node so error
                MESSAGE ("unexpected closing tag %s for %s\n", child->m_name, node->m_name);
                delete child;
                return NULL;
            }
            child = parseElement ();
        }
        if (child == NULL || child->isClosing (node) == false) {
            if (child != NULL) delete child;
            return NULL;
        }
        delete child;
    } else if (node->m_type == SELF_TAG || node->m_type == CDATA) {
        ; // nothing to do just return;
    } else if (node->m_type == CLOSE_TAG) { //error
        return NULL;
    }
    return node;
}

XmlAttribute * XmlReader::parseAttribute () {
    char * attr = getNextToken ();
    if (attr == NULL || *attr == 0) {
        return NULL; // something unexpected: not an iden nor '/' or '>'
    }
    if (skipSpaces () != '=') {
        MESSAGE ("parseAttributes: '=' expected after %s at line #%d\n", attr, m_nbLines);
        return NULL;
    }
    getNextChar (); // eat '='
    char * value = getString (); // getString will eat spaces if needed
    if (value == NULL) {
        MESSAGE ("parseAttributes: '=' expected after %s\n", attr);
        return NULL;
    }
    return new XmlAttribute (attr, value);
}

