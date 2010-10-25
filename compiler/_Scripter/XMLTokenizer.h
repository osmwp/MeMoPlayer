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

# define TOK_BUFFER_SIZE 409600
# define TOK_MAX_PARAMS 32
class XMLTokenizer {
    FILE * m_fp;
    char * m_tmp;
    char * m_pushedToken;
    char * m_tag;
    int m_nbParams;   
    char * m_param[TOK_MAX_PARAMS];
    int m_line;
public:
    XMLTokenizer (FILE * fp);
    ~XMLTokenizer ();
    /** return the current line number */
    int getLine() { return m_line; }

    char ** getNextTag (char ** tag, int * nbParams, bool * closing);
    bool getClosingTag (char * tag);
    bool eatToClosingTag (char * tag);
    char * getCData ();
    void cleanUp ();


private:
    char * getName();

    bool checkChar (char target);

    char * getStringValue ();
    char * dupString (char * s);

    int getEscape ();

    bool skipSpaces ();

    char getChar ();
    void pushChar (char c) { ungetc (c, m_fp);}

    bool isSpace (char c) { return (c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == ','); }
    bool isName (char c) { return ( (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || c == '_' || c == '-'); }
                                     
};
