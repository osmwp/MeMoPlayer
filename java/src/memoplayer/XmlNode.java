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

/**
 * Common class used by XmlReader and BmlReader.
 */
class XmlNode {
    final static int OPEN_TAG = 1; // open tag like in <H1>
    final static int CLOSE_TAG = 2; // closing tag like in </h1>
    final static int SELF_TAG = 3; // self closing tag like in <br/>
    final static int CDATA = 4; // text data inside a node, can be in between tags like in HTML text

    String m_name; // either the nam eof the tag, or the content of a CDATA section
    int m_type, m_nbAttributes, m_nbChildren; 
    boolean m_startingSpace, m_endindSpace;
    XmlAttribute m_attributes;
    XmlNode m_next;
    XmlNode m_children;
    XmlNode m_parent;
    
    XmlNode (String name, int type, boolean startingSpace, boolean endingSpace) {
        m_name = name;
        m_type = type;
        m_startingSpace = startingSpace;
        m_endindSpace = endingSpace;
    }

    XmlNode (String name, int type) {
        this (name, type, false, false);
    }

    XmlNode (String data) {
        this (data, CDATA);
    }

    void visit (XmlVisitor v) {
        if (m_type == CDATA) {
            v.setLeave (m_name, m_startingSpace, m_endindSpace);
        } else if (m_type == SELF_TAG) {
            v.open (m_name);
            if (m_attributes != null) {
                m_attributes.visit (v);
            }
            v.endOfAttributes (true);
        } else if (m_type == OPEN_TAG) {
            v.open (m_name);
            if (m_attributes != null) {
                m_attributes.visit (v);
            }
            v.endOfAttributes (false);
            if (m_children != null) {
                m_children.visit (v);
            }
            v.close (m_name);
        } 
        if (m_next != null) {
            m_next.visit (v);
        }
    }

    boolean isClosing (XmlNode n) { 
        return (n != null && m_type == CLOSE_TAG && m_name.equalsIgnoreCase (n.m_name)); 
    }

    void addChild (XmlNode n) {
        if (n != null) {
            if (m_children == null) {
                m_children = n;
            } else {
                XmlNode tmp = m_children;
                while (tmp.m_next != null) {
                    tmp = tmp.m_next;
                }
                tmp.m_next = n;
            }
            n.m_next = null;
            n.m_parent = this;
            m_nbChildren++;
        }
    }

    void addAttribute (XmlAttribute a) {
        if (m_attributes == null) {
            m_attributes = a;
        } else {
            XmlAttribute tmp = m_attributes;
            while (tmp.m_next != null) {
                tmp = tmp.m_next;
            }
            tmp.m_next = a;
        }
        a.m_next = null;
        m_nbAttributes++;
    }

    XmlNode find (String name, int count) {
        //Logger.println ("XmlNode.find: '"+name+"' / "+count+" <> "+m_name+", "+m_nbAttributes);
        if (count <= 0) {
            return null;
        } else if (name.equals ("*") || m_name.equals (name)) {
            if (--count == 0) { 
                //Logger.println ("XmlNode.find: found "+this+" / "+m_name+", "+m_nbAttributes+", "+m_nbChildren);
                return (this);
            }
        }
        if (m_next != null) {
            return m_next.find (name, count);
        }
        return null;
    }
}
