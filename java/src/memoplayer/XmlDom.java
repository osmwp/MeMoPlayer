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

class XmlDom {
    
    final static int URL = 1;
    final static int BUFFER = 2;
    final static int BML = 4; // DEPRECATED (now autodetected)
    final static int DEBUG = 8; 
    final static int ASYNC = 16;
    final static int HTML = 32;
    
    static int parseOccurence (String s) {
        int offset = s.indexOf ('[');
        if (offset++ > -1) {
            int max = s.length ();
            int n = 0; 
            while (offset < max) {
                char c = s.charAt (offset++);
                if (c == ']') { 
                    return n;
                } else if ( (c >= '0') && (c <= '9')) { 
                    n = n*10 + c-'0'; 
                } else {
                    return -1;
                }
            }
        }
        return 1;
    }
    
    XmlNode m_root; // the base node of teh XML tree
    XmlNode m_current; // pointer to the node matching path in find method
    XmlNode m_anchor; // used to simplify the search algorithm: this add another level for the find method
    
    int populate (String buffer, int mode) {
        int responseCode;
        if ((mode & URL) == URL) {
//#ifdef api.bml
            // Add Accept-Encoding header when making a HTTP(S) request.
            if (buffer.startsWith ("http") ||
                buffer.startsWith ("cache:") && buffer.indexOf (",http") != 0) {
                buffer = buffer.concat ("||Accept-Encoding=application/bml");
            }
//#endif
            File f = new File (buffer, File.MODE_READ | File.MODE_SYNC);
            responseCode = f.getHttpResponseCode();
            if (responseCode == 200) {
                byte [] data = f.readAllBytes ();
                f.close(File.CLOSED);
                f = null;
                if (data != null) {
                     // check for the BML1 header
                    if (data.length > 4 && data[0] == 'B' && data[1] == 'M' && data[2] == 'L' && data[3] == '1') {
//#ifdef api.bml
                            try {
                                setRoot (new BmlReader (data).getRootNode ());
                            } catch (Exception e) {
                                Logger.println ("XmlDom: BmlReader sneezed: "+e);
                            }
//#else
                            Logger.println("Error: No support for BML parser.");
//#endif
                            return responseCode;
                    }
//#ifdef api.xparse
                    String xmlData = null;
                    try { 
                        xmlData = new String (data, "UTF-8"); 
                    } catch (Exception e) {
                        try { xmlData = new String (data); } catch (Exception e2) { }    
                    }
                    if (xmlData != null) {
                        setRoot (new XmlReader (xmlData, BUFFER | (mode & (DEBUG | HTML))).getRootNode ());
                    }
//#else
                    Logger.println("Error: No support for XML parser.");
//#endif
                }
            }
        } else { // buffered
            responseCode = 200;
//#ifdef api.xparse
            setRoot (new XmlReader (buffer, BUFFER | (mode & (DEBUG | HTML))).getRootNode ());
//#else
            Logger.println("Error: No support for XML parser.");
//#endif
        }
        return responseCode;
    }
    
    boolean hasData () {
        return m_root != null;
    }

    void setRoot (XmlNode root) {
        m_root = root;
        m_current = null;
        m_anchor = new XmlNode ("anchor", XmlNode.SELF_TAG);
        m_anchor.addChild (m_root);
    }

    void visit (XmlVisitor v) {
        m_root.visit (v);
    }

    String parseName (String s) {
        int offset = s.indexOf ('[');
        if (offset > -1) {
            return s.substring (0, offset);
        }
        return s;
    }

    boolean goTo (String node) {
        if (node.equals (":parent")) {
            if (m_current != null && m_current.m_parent != null) {
                m_current = m_current.m_parent;
                return true;
            }
        } else if (node.equals (":root")) {
            m_current = m_anchor;
            return m_current != null;
        } else if (node.equals (":next")) {
            if (m_current != null  && m_current.m_next != null) {
                m_current = m_current.m_next;
                return true;
            }
        } else {
            if (m_current != null && m_current.m_children != null) {
                XmlNode tmp = m_current.m_children.find (parseName(node), parseOccurence (node));
                if (tmp != null) {
                    m_current = tmp;
                    return true;
                }
            }
        }
        return false;
    }

    // the path string contains nodeName#count[/node#i]
    boolean find (String path) {
        int idx, start = 0;
        if (path == null || path.length () == 0) { 
            return false;
        }
        if (path.charAt(0) == '/') {// from root
            m_current = m_anchor;
            start = 1;
        } else if (path.equals (":root")) { // from root
            m_current = m_anchor;
            return true;
        } else if (path.equals (":parent")) { // from root
            return goTo (path);
        } else if (m_current == null) { // no previous find
            return false;
        }

        idx = path.indexOf ('/', start);
        if (idx == -1) { // no other '/'
            if ( (idx = path.length ()) == 0) { // just '/' in the path
                return true;
            }
        }
        // split the string with '/' 
        try {
            while (idx > 0) {
                String nodeName = path.substring (start, idx); 
                if (goTo (nodeName) == false || m_current == null) {
                    return false;
                }
                start = idx+1;
                if (start >= path.length ()) {
                    idx = -1;
                } else {
                    idx = path.indexOf ('/', start);
                    if (idx == -1 && start < path.length ()) { // no trailing '/'
                        idx = path.length ();
                    }
                }
            }
            return true;
        } catch (Exception e) {
            Logger.println ("XmlReader.find: got exception "+e);
            m_current = null;
        }
        return false;
    }

    int getNbAttributes () {
        return m_current != null ? m_current.m_nbAttributes : 0;
    }

    String getAttributeName (int index) {
        if (getNbAttributes () > 0) {
            XmlAttribute xa = m_current.m_attributes.find (index);
            if (xa != null) {
                return xa.m_name;
            }
        }
        return "";
    }

    String getAttributeValue (String name) {
        if (getNbAttributes () > 0) {
            XmlAttribute xa = m_current.m_attributes.find (name);
            if (xa != null) {
                return xa.m_value;
            }
        }
        return "";
    }

    String getAttributeValue (int index) {
        if (getNbAttributes () > 0) {
            XmlAttribute xa = m_current.m_attributes.find (index);
            if (xa != null) {
                return xa.m_value ;
            }
        }
        return "";    
    }

    int getNbChildren () {
        return m_current != null ? m_current.m_nbChildren : 0;
    }

    boolean isSelfClosing () {
        return m_current == null ? false : (m_current.m_type == XmlNode.SELF_TAG);
    }

    boolean isTextChild (int index) {
        XmlNode node = getChild (index);
        return node == null ? false : (node.m_type == XmlNode.CDATA);
    }

    boolean isSelfClosingChild (int index) {
        XmlNode node = getChild (index);
        return node == null ? false : (node.m_type == XmlNode.SELF_TAG);
    }

    XmlNode getChild (int index) {
        if (index > 0 && index <= getNbChildren ()) {
            XmlNode tmp = m_current.m_children;
            while (index-- > 1) {
                tmp = tmp.m_next;
            }
            return tmp;
        }
        return null;
    }

    String getChildValue (int index) {
        XmlNode node = getChild (index);
        return node == null ? "" : node.m_name;
    }

    boolean isText () {
        return m_current == null ? false : m_current.m_type == XmlNode.CDATA;
    }

    String getValue () {
        return m_current == null ? "" : m_current.m_name;
    }

//#ifdef api.domEdition
    
    boolean setAttributeValue (String name, String value) {
        if (m_current != null) {
            XmlAttribute xa = m_current.m_attributes.find (name);
            if (xa != null) {
                xa.m_value = value;
            } else { // not found, add it.
                m_current.addAttribute(new XmlAttribute(name, value));
            }
            return true;
        }
        return false;
    }
    
    boolean setChildValue (int index, String value) {
        XmlNode node = getChild (index);
        if (node != null) {
            node.m_name = value;
            return true;
        }
        return false;
    }
    
    boolean setValue (String value) {
        if (m_current != null) {
            m_current.m_name = value;
            return true;
        }
        return false; 
    }
    
    /**
     * Inner class only used by serialize ()
     */
    class XmlSerializer implements XmlVisitor {
        StringBuffer sb = new StringBuffer();
        
        public void setLeave (String l, boolean startingSpace, boolean endingSpace) {
            sb.append ("<![CDATA[");
            sb.append (l);
            sb.append ("]]>");
        }

        public void open (String t) {
            sb.append ('<');
            sb.append (t);
        }

        public void endOfAttributes (boolean selfClosing) {
            if (selfClosing) sb.append ('/');
            sb.append ('>');
        }

        public void close (String t) {
            sb.append ('<');
            sb.append ('/');
            sb.append (t);
            sb.append ('>');
        }

        public void addAttribute (String name, String value) {
            sb.append (' ');
            sb.append (name);
            sb.append ('=');
            sb.append ('"');
            appendAttrString (value, sb);
            sb.append ('"');
        }
        
        public String toString () {
            return sb.toString();
        }
        
        private void appendAttrString (String value, StringBuffer sb) {
            final int size = value.length ();
            for (int i = 0; i < size; i++) {
                char c = value.charAt (i);
                if (c == '<') {
                    sb.append ("&lt;");
                } else if (c == '>') {
                    sb.append ("&gt;");
                } else if (c == '&') {
                    sb.append ("&amp;");
                } else if (c == '\'') {
                    sb.append("&apos;");
                } else if (c == '\"') {
                    sb.append("&quot;");
                } else {
                    sb.append (c);
                }
            }
        }
    }
    
    String serialize () {
        XmlNode node = m_current != null ? m_current : m_root;
        if (node != null) {
            XmlVisitor xv = new XmlSerializer ();
            visitNode (node, xv);
            return xv.toString();
        }
        return "";
    }
    
    boolean serializeToRms (String recordName) {
        boolean ret = false;
        XmlNode node = m_current != null ? m_current : m_root;
        if (node != null) {
            byte[] data = null;
//#ifdef api.bmlEncoder
            BmlWriter bw = new BmlWriter ();
            visitNode (node, bw);
            data = bw.getBml ();
//#endif
            if (data != null) {
                ret = CacheManager.getManager().setRecord (recordName, data);
            }
        }
        return ret;
    }
//#endif
    
    // Allows visiting a node (and children) without visiting its brothers
    void visitNode (XmlNode n, XmlVisitor v) {
        XmlNode next = n.m_next;
        n.m_next = null;
        n.visit(v);
        n.m_next = next;
    }
}

