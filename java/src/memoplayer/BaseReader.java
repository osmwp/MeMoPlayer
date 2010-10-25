//#condition api.xparse
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

package memoplayer;

import javax.microedition.io.*;
import java.io.*;
/**
   The base class for JSonReader and XmlReader
 */

abstract public class BaseReader {
    final static int URL = 1; 
    final static int BUFFER = 2;
    final static int DEBUG = 8; 
    final static int ASYNC = 16; 

    protected int m_mode; // we need to keep it because the parse method is called in the constructor some sub class has no chance to use it before parsing

    protected String m_buffer; // the data to be parsed
    protected int m_pos; // current char being parsed 
    protected int m_len; // length of buffer
    protected int m_nbLines; // the current line number
    protected StringBuffer m_sb; // a temporary buffer to parse CDATA
    protected StringBuffer m_charBuf; // a temporary buffer to html char (ex: &deg;)
    protected boolean m_debugMode; // set to true when ORed in contructir param
    public boolean m_newData = false; // flag when new data are available

    BaseReader (final String buffer, final int mode) {
        if (buffer == null) {
            return;
        }
        m_sb = new StringBuffer ();
        m_charBuf = new StringBuffer ();
        m_mode = mode;
        m_nbLines = 1;
        m_debugMode = (mode & DEBUG) == DEBUG;
        if ((mode & BUFFER) == BUFFER) {
            createFromString (buffer);
        } else {
            if ((mode & ASYNC) == ASYNC) {
                new Thread () {
                    public void run () {
                        openUrl (buffer);
                        MiniPlayer.wakeUpCanvas();
                    }
                }.start ();
            } else {
                openUrl (buffer);
            }
        }
    }

    // parse the buffer and build the appropriate structure (XML or JSON)
    abstract boolean parse ();

    // release all specific ressources associated to the parsed structure
    abstract void closeSpecific ();

    // create an XML DOM from the param string 
    synchronized boolean createFromString (String buffer) {
        m_buffer = buffer;
        m_pos = 0;
        m_len = buffer.length ();
        return m_newData = parse ();
    }

    synchronized public void close () {
        m_buffer = null;
        m_sb = null;
        m_newData = false;
        closeSpecific ();
    }

    synchronized boolean isNewData () {
        return m_newData;
    }

    // open the url and read data to create one DOM
    public void openUrl (String url) {
        File file = new File (url); // open in sync mode => return when data are ready or error occured
        if (file != null && file.getState() == Loadable.READY){ // we have data
            m_newData = createFromString (file.readAll()); // create the DOM
            file.close (Loadable.CLOSED); // cleanup connection
        }
    }

    // use to output one string
    final void out (String msg) {  if (m_debugMode) { Logger.println (msg); } }

    // use to output two strings (avoid multiple 
    final void out (String msg1, String msg2) {  if (m_debugMode) { Logger.print (msg1); Logger.println (msg2); } }

    // return the current char to read or '\0' if end of buffer
    char getChar () { 
        return m_pos < m_len ? m_buffer.charAt (m_pos) : '\0';
    }

    // return the next char to read or '\0' if end of buffer
    char getNextChar () {
        m_pos++; 
        return getChar ();
    }

    // return true and advance to the next char if the current char is equal to the parameter
    boolean eatChar (char c) {
        if (getChar () == c) { 
            m_pos++;
            return true;
        }
        return false;
    }
    // advance to the next non white char
    char skipSpaces () {
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

    char getHtmlChar () {
        m_charBuf.setLength (0);
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
                out ("getString: expecting ';' instead of '"+c+"' while parsing &#xyz;");
            }
            return (char)i;
        }
        while (c != '\0' && c != ';' && i++ < 10) {
            m_charBuf.append (c);
            c = getNextChar ();
        }
        String tmp = m_charBuf.toString ();
        if (tmp.equals ("amp")) {
            return '&';
        } else if (tmp.equals ("lt")) {
            return '<';
        } else if (tmp.equals ("gt")) {
            return '>';
        } else if (tmp.equals ("apos")) {
            return '\'';
        } else if (tmp.equals ("quot")) {
            return '"';
        } else if (tmp.equals ("deg")) {
            return convert (0xC2, 0xB0);
        } else if (tmp.equals ("nbsp")) {
            return convert (0xC2, 0xA0);
        } else if (tmp.equals ("ecirc")) {
            return convert (0xC3, 0xAA);
        } else if (tmp.equals ("eacute")) {
            return convert (0xC3, 0xA9);
        } else if (tmp.equals ("egrave")) {
            return convert (0xC3, 0xA8);
        } else if (tmp.equals ("agrave")) {
            return convert (0xC3, 0xE0);
        } else if (tmp.equals ("ccedil")) {
            return convert (0xC3, 0xE7);
        } else { 
            m_pos = oldPos+1;
            return '&';
        }
    }
    // return a string or null is next thing is not a string (i.e. first char is not a '"')
    protected String getString () {
        int utf = 0;
        skipSpaces ();
        if (!eatChar ('"')) { return null; }
        m_sb.setLength (0);
        char c = getChar ();
        while ( c != '\0' && c != '"' ) {
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
                    utf += Character.digit(getNextChar (), 16) << 12;
                    utf += Character.digit(getNextChar (), 16) << 8;
                    utf += Character.digit(getNextChar (), 16) << 4;
                    utf += Character.digit(getNextChar (), 16);
                    c = (char)utf;
                    break;
                }
            }
            m_sb.append (c);
            c = getNextChar ();
        }
        return eatChar ('"') ? m_sb.toString () : null;
    }

    // return the next identifier for a tag or an attribute name
    String getNextIdent (boolean startsWithAlpha) {
        int p = m_pos;
        char c = getChar ();
        if (startsWithAlpha && isAlpha (c) == false) { return null; }
        while ( isAlpha (c) || isNumber (c) || isSpecial (c) ) {
            c = getNextChar ();
        }
        return m_buffer.substring (p, m_pos);
    }

    String getNextToken () {
        skipSpaces ();
        return getNextIdent (true);
    }

    String getNextStrictToken () {
        skipSpaces ();
        int p = m_pos;
        char c = getChar ();
        if (isAlpha (c) == false) { return null; }
        while ( isAlpha (c) || isNumber (c) ||  (c == '-') ) {
            c = getNextChar ();
        }
        return m_buffer.substring (p, m_pos);
    }

    String getNextNumber () {
        skipSpaces ();
        return getNextIdent (false);
    }

    static boolean isWhite (char c) { return c == ' ' || c == '\n' || c == '\t' || c == '\r'; }
    static boolean isAlphaNum (char c) { return isAlpha (c) || isNumber (c) || c == '-' || c == '+' || c == '.'; }
    static boolean isNumber (char c) { return (c >= '0' && c <= '9'); }
    static boolean isAlpha (char c) { return ( (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_'); }
    static boolean isSpecial (char c) { return (c == '-' || c == ':'  || c == '.' || c == '#' || c == '%'); }
}
