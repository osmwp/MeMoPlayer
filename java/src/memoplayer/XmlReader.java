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

class XmlPrinter implements XmlVisitor {
    String decal = "";

    public void setLeave (String l, boolean startingSpace, boolean endingSpace) {
        Logger.println (decal+l); //(startingSpace?"_":"")+l+(trailingSpace?"_":""));
    }

    public void open (String t) {
        Logger.print (decal+"<"+t);
        decal += "    ";
    }

    public void endOfAttributes (boolean selfClosing) {
        if (selfClosing) {
            decal = decal.substring (4);
            Logger.println (" />");
        } else {
            Logger.println (" >");
        }
    }

    public void close (String t) {
        decal = decal.substring (4);
        Logger.println (decal+"</"+t+">");
    }

    public void addAttribute (String name, String value) {
        Logger.print (" "+name+" = \""+value+"\"");
    }
    
}

class XmlReader extends BaseReader {
    final static int HTML = 32; 

    XmlNode m_root;
    boolean m_htmlMode; // true if we need to consider some tags to be automatically closing like teh one below
    static String [] s_htmlSelfClosing = { "hr", "br", "img" };
    
    // init the reader with a buffer to read and start build the DOM
    XmlReader (String buffer, int mode) {
        super (buffer, mode);
    }

    boolean parse () {
        m_htmlMode = (m_mode & HTML) == HTML;
        m_root = parseNode (null);
        if (m_root != null) {
            if (m_debugMode) {
                m_root.visit (new XmlPrinter ());
            }
            return true;
        }
        out ("XmlReader: No DOM parsed !");
        return false;
            
    }

    void visit (XmlVisitor v) {
        if (m_root != null) {
            m_root.visit (v);
        }
    }
    
    XmlNode getRootNode () {
        return m_root;
    }

    void closeSpecific () {
        m_root = null;
    }

    boolean parseSpecial (char c1, char c2, String s) {
        // shoudl check for "--"
        if (getNextChar () != c1 || getNextChar () != c2) {
            return false;
        }
        int p = m_buffer.indexOf (s, m_pos);
        if (p == -1) { 
            return false;
        } // comment not terminated
        m_pos = p+s.length ();
        return true;
    }

    XmlNode parseElement () {
        boolean space = isWhite (getChar());
        char c = skipSpaces ();
        if (c == '\0') { // end of data
            return null;
        } else if (c == '<') { // we have a XML fragment
            if ( (c = getNextChar ()) == '?') {
                if (parseSpecial ('x', 'm', "?>")) {
                    return parseElement ();
                }
                return null;
            } else if (c == '!') {
                if (m_buffer.startsWith ("![CDATA[", m_pos)) { // pure XML CDATA Definition keep all data
                    int start = m_pos;
                    int end = m_buffer.indexOf ("]]>", m_pos);
                    if (end == -1) { 
                        return null;
                    } // CDATA not terminated
                    start+=8; // skip header ![CDATA[
                    m_pos = end+2;  // avoid trailing ]]
                    boolean ss = isWhite (m_buffer.charAt (start));
                    boolean ts = isWhite (m_buffer.charAt (end-1));
                    String data = m_buffer.substring(start, end);
                    // check for '>'
                    c = skipSpaces ();
                    if (c != '>') {
                        Logger.println ("> expected after CDTA definition, instead of "+c);
                    }
                    return new XmlNode (data, XmlNode.CDATA, ss, ts);
                } else if (parseSpecial ('-', '-', "-->")) {
                    return parseElement ();
                }
                return null;
            }
            return parseTag (c);
        }
        return parseCData (space);
    }

    XmlNode parseCData (boolean space) {
        int end = m_pos;
        boolean ss, ts;
        String data;
        /*
            int start = m_pos;
            if (m_buffer.startsWith ("![CDATA_AVIRER[", start)) { // pure XML CDATA Definition keep all data
            end = m_buffer.indexOf ("]]>", m_pos);
            if (end == -1) { 
                return null;
            } // CDATA not terminated
            start+=8; // skip header ![CDATA[
            m_pos = end+2;  // avoid trailing ]]
            ss = isWhite (m_buffer.charAt (start));
            ts = isWhite (m_buffer.charAt (end-1));
            data = m_buffer.substring(start, end);
        } else {*/
            m_sb.setLength (0);
            ss = space || isWhite (m_buffer.charAt (m_pos));
            char c = skipSpaces ();
            while (c != '\0' && c != '<') {
                if (c == '&') {
                    c = getHtmlChar ();
                }
                m_sb.append (c);
                // check for '>'
                c = getNextChar ();
            }
            end = m_pos;
            ts = isWhite (m_buffer.charAt (end-1));
            while (isWhite (m_buffer.charAt (end-1))) { end--;}
//             if (start == end) { // empty section in between two tags
//                 return null;
//             }
            data = m_sb.toString();
        //}
        return new XmlNode (data, XmlNode.CDATA, ss, ts);
    }
    
    int checkHtmlSelfClosing (String name) {
        for (int i = 0; i < s_htmlSelfClosing.length; i++) {
            if (name.equalsIgnoreCase (s_htmlSelfClosing[i])) {
                out ("Warning: found deprecated non self closing "+name);
                return XmlNode.SELF_TAG;
            }
        }
        return XmlNode.OPEN_TAG;
    }

    XmlNode parseTag (char c) {
        c = skipSpaces (); // '<' has been eaten to check for a comment
        boolean closing = false;
        if (c == '/') { // ending tag
            closing = true;
            c = getNextChar ();
        }
        String name = getNextToken ();
        if (name == null) {
            out ("Error: XML tags must start with an alpha caracter !'");
            return null;
        }
        XmlNode e = new XmlNode (name, closing ? XmlNode.CLOSE_TAG : XmlNode.OPEN_TAG);
        if (!closing) { // may have some attributes
            parseAttributes (e);
        }
        // final '>'
        c = getChar ();
        if (c == '/') { // self tag
            if (closing) {
                out ("Error: closing is also self closing: "+e.m_name);
                return null;
            }
            e.m_type = XmlNode.SELF_TAG;
            c = getNextChar ();
        }
        // check for nodes that shoudl be self closing: BR, HR, IMG
       
        if (m_htmlMode && e.m_type == XmlNode.OPEN_TAG) {
            e.m_type = checkHtmlSelfClosing(e.m_name);
        }
        if (c != '>') { // ending tag
            out ("Error: got '"+c+"' instead of '>'");
            return null;
        }
        getNextChar (); // eat '>'
        return e;
    }
    
    boolean parseAttributes (XmlNode e) {
        char c = skipSpaces ();
        while (c != '\0' && c != '/' && c != '>') {
            String attr = getNextToken ();
            if (attr == null || attr.length () == 0) {
                return false; // something unexpected: not an iden nor '/' or '>'
            }
            if (skipSpaces () != '=') {
                out ("parseAttributes: '=' expected after "+attr);
                return false;
            }
            getNextChar (); // eat '='
            String value = getAttrString (); // getAttrString will eat spaces if needed
            e.addAttribute (new XmlAttribute (attr, value));
        }
        return true;
    }


    XmlNode parseNode (XmlNode node) {
        if (node == null) {
            if ( (node = parseElement ()) == null) {
                out ("cannot parse node at all");
                return null; // parsing problem
            }
        }
        if (node.m_type == XmlNode.OPEN_TAG) {
            // read while CLOSE_TAG
            XmlNode child = parseElement ();
            while (child != null && child.isClosing (node) == false) {
                if (child.m_type == XmlNode.CDATA) {
                    node.addChild (child);
                } else if (child.m_type == XmlNode.SELF_TAG) {
                    node.addChild (child);
                } else if (child.m_type == XmlNode.OPEN_TAG) {
                    node.addChild (parseNode (child));
                } else if (child.m_type == XmlNode.CLOSE_TAG) { // not closing the right node so error
                    out ("unexpected closing tag: "+child.m_name+" for "+node.m_name);
                    return null;
                }
                child = parseElement ();
            }
            if (child == null || child.isClosing (node) == false) {
                return null;
            }
        } else if (node.m_type == XmlNode.SELF_TAG || node.m_type == XmlNode.CDATA) {
            ; // nothing to do just return;
        } else if (node.m_type == XmlNode.CLOSE_TAG) { //error
            return null;
        }
        return node;
    }
    
    // parse a Xml attribute string
    protected String getAttrString () {
        skipSpaces ();
        char quote = '"';
        if (!eatChar (quote)) {
            quote = '\''; // single quoted string ?
            if (!eatChar(quote)) {
                return null;
            }
        }
        m_sb.setLength (0);
        char c = getChar ();
        while ( c != '\0' && c != quote ) {
            if (isWhite(c)) {
                c = ' ';
            } else if (c == '&') {
                c = getHtmlChar();
            }
            m_sb.append (c);
            c = getNextChar ();
        }
        return eatChar (quote) ? m_sb.toString () : null;
    }
}

