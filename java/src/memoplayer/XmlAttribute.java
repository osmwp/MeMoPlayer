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
 * A simple place holder for a list of couples name/value 
 */
class XmlAttribute {
    String m_name;
    XmlAttribute m_next;
    String m_value;
    
    XmlAttribute (String name, String value) {
        m_name = name;
        m_value = value;
    }

    XmlAttribute find (int n) {
        if (n < 1) {
            return null;
        } else if (n == 1) {
            return this;
        } else if (m_next != null) {
            return m_next.find (n-1);
        }
        return null;
    }

    XmlAttribute find (String s) {
        if (m_name.equals (s)) {
            return (this);
        }
        if (m_next != null) {
            return m_next.find (s);
        }
        return null;
    }
 
    void visit (XmlVisitor v) {
        v.addAttribute (m_name, m_value);
        if (m_next != null) {
            m_next.visit (v);
        }
    }
}
