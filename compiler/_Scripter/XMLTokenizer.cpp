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

#include "XMLTokenizer.h"
# include "Trace.h"

XMLTokenizer::XMLTokenizer (FILE * fp) { 
    m_fp = fp; 
    m_line = 1;
    m_tmp = (char*)malloc (TOK_BUFFER_SIZE); 
    m_pushedToken = NULL;
    m_tag = NULL;
    for (int i = 0; i < TOK_MAX_PARAMS; i++) {
        m_param [i] = NULL;
    }
}

XMLTokenizer::~XMLTokenizer () { 
    free (m_tmp); 
    if (m_fp) { fclose (m_fp); }
}

void XMLTokenizer::cleanUp () {
    if (m_tag != NULL) {
        free (m_tag);
        m_tag = NULL;
    }
    for (int i = 0; i < TOK_MAX_PARAMS; i++) {
        if (m_param [i] == NULL) {
            free (m_param [i]);
            m_param [i] = NULL;
        }
    }
}

char ** XMLTokenizer::getNextTag (char ** tag, int * nbParams, bool * closing) {
    m_nbParams = 0;
    *tag = NULL;
    *nbParams = 0;
    *closing = false;
    cleanUp ();
    if (!checkChar ('<')) {
        fprintf (stderr, "< expected line %d\n", getLine ());
        return (NULL);
    }
    if (checkChar ('/')) {
        *closing = true;
    }
    if (checkChar ('!')) {
        char c = getChar ();
        MESSAGE ("Parsing comment: <!%c", c);
        while (true) {
            while (c != '-' && c != EOF) {
                c = getChar ();
                MESSAGE ("%c", c);
            }
            c = getChar ();
            MESSAGE ("%c", c);
            if (c != '-') {
                continue;
            }
            c = getChar ();
            MESSAGE ("%c", c);
            if (c == '>') {
                break;
            }
        }
        MESSAGE (" ok\n");
        return (getNextTag (tag, nbParams, closing));
    }

    if (checkChar ('?')) {
        char c = getChar ();
        while (c != '?' && c != EOF) {
            c = getChar ();
        }
        MESSAGE ("Parsing header: got final '%c'\n", c);
        c = getChar ();
        if (c != '>') {
            ERROR ("Cannot find matching ?> (unexpected %c)\n", c);
            return (NULL);
        }
        MESSAGE ("Got whole <?xml ... ?>\n");
        return (getNextTag (tag, nbParams, closing));
    }

    m_tag = getName ();
    if (m_tag == NULL) {
        fprintf (stderr, "tag name expected line %d\n", getLine ());
        return (NULL);
    }
    m_tag = dupString (m_tag);
    if (*closing) {
        if (!checkChar ('>')) {
            fprintf (stderr, "> expected for closing tag line %d\n", getLine ());
        } else {
            *tag = m_tag;
            MESSAGE ("$ </%s\n>", m_tag);
        }
        return (NULL);
    }
    while (!checkChar ('>')) {
        if (checkChar ('/')) {
            *closing = true;
            if (!checkChar ('>')) {
                fprintf (stderr, "> expected for closing tag line %d\n", getLine ());
                return (NULL);
            }
            break;
        }
        m_param[m_nbParams++]= dupString (getName());
        if (!checkChar ('=')) {
            fprintf (stderr, "> expected line %d\n", getLine ());
            return (NULL);
        }
        m_param[m_nbParams++]= dupString (getStringValue());
    }
//     MESSAGE ("$ <%s", m_tag);
//     for (int i = 0; i < m_nbParams/2; i++) {
//         MESSAGE (" %s=\"%s\"", m_param[i*2], m_param[i*2+1]);
//     }
//     MESSAGE (*closing ? "/>\n": ">\n");
    *tag = m_tag;
    *nbParams = m_nbParams/2;
    return (m_param);
}

bool XMLTokenizer::eatToClosingTag (char * tag) {
    char c;
    while (1) {
        while ( (c = getChar()) != EOF && c != '<');
        if (c == EOF) {
            return (false);
        }
        if (!checkChar ('/')) {
            continue;
        }
        char * tmp = getName ();
        if (tmp == NULL) {
            return (false);
        }
        if (strcmp (tmp, tag) != 0) {
            continue;
        }
        if (!checkChar ('>')) {
            fprintf (stderr, "</%s> expected line %d\n", tag, getLine ());
            return (false);
        }
        return (true);
    }
}

bool XMLTokenizer::getClosingTag (char * tag) {
    if (!checkChar ('<')) {
        fprintf (stderr, "</%s> expected line %d\n", tag, getLine ());
        return (false);
    }
    if (!checkChar ('/')) {
        fprintf (stderr, "</%s> expected line %d\n", tag, getLine ());
        return (false);
    }
    char * tmp = getName ();
    if (tmp == NULL || strcmp (tmp, tag) != 0) {
        fprintf (stderr, "</%s> expected line %d\n", tag, getLine ());
    }
    if (!checkChar ('>')) {
        fprintf (stderr, "</%s> expected line %d\n", tag, getLine ());
        return (false);
    }
    //MESSAGE ("$ </%s>\n", tag);
    return (true);
}

char * XMLTokenizer::getCData () {
    char c = getChar();
    char * tmp = m_tmp;
    while ( c != '<') {
        *tmp++ = c;
        c = getChar();
    }
    *tmp = '\0';
    pushChar (c);
    //MESSAGE ("$ ![CDATA[%s]]\n", m_tmp);
    return (*m_tmp ? m_tmp : NULL);
}

char * XMLTokenizer::getName() {
    if (skipSpaces ()) {
        char c = getChar();
        char * tmp = m_tmp;
        while ( isName (c)) {
            *tmp++ = c;
            c = getChar();
        } 
        *tmp = '\0';
        pushChar (c);
        return (*m_tmp ? m_tmp : NULL);
    }
    return (NULL);
}

bool XMLTokenizer::checkChar (char target) {
    if (skipSpaces ()) {
        char c = getChar ();
        if (c == target) {
            return (true);
        }
        pushChar (c);
    }
    return (false);
}

char * XMLTokenizer::getStringValue () {
    if (skipSpaces ()) {
        char c = getChar ();
        if (c == '"') {
            char * tmp = m_tmp;
            c = getChar ();
            while (c != EOF && c != '"') {
                if (c == '\\') {
                    c = getEscape();
                }
                *tmp++ = c;
                c = getChar ();
            }
            if (c == '"') {
                *tmp = '\0';
                return m_tmp;
            }
        }
    }
    return (NULL);
}
char * XMLTokenizer::dupString (char * s) {
    if (s) {
        return (strdup (s));
    }
    return (NULL);
}


int XMLTokenizer::getEscape () {
    int c = getChar ();
    if (c >= '0' && c <= '9') {
        int t = c - '0';
        t = t*8 + getChar () - '0';
        t = t*8 + getChar () - '0';
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

bool XMLTokenizer::skipSpaces () {
    char c;
    while (true) {
        c = getChar ();
        if (c == '#') {
            while ( (c = getChar ()) != '\n' && c != EOF);
        }
        if (!isSpace (c)) {
            pushChar (c);
            break;
        }
    }
    return (c != EOF);
}

char XMLTokenizer::getChar () {
    char c = getc (m_fp);
    if (c == '\n') {
        m_line++;
    }
    return c;
}
