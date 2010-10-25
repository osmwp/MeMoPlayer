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
import java.io.*;

class SFNode extends Field {
    Node m_node = null;

    SFNode (Observer o) {
        super (o);
    }

    SFNode () {
        //super (null);
    }

    void decode (DataInputStream dis, Node [] table, Decoder decoder) {
        //System.out.print ("decoding SFNode: #");
        m_node = Node.decode (dis, table, decoder);
        //System.out.println ("decoding SFNode done");
    } 

    Node getValue () { return m_node; }
    void setValue (Node node) { m_node = node; } 
    void copyValue (Field f) {
        SFNode m = (SFNode) f;
        m_node = m.m_node;
    }

    public ScriptAccess use (int index) {
        return (m_node != null ? m_node.getFieldByID (index-1) :  null);
    }

    public void set (int index, Register r, int offset) {
        m_node = r.getNode ();
        notifyChange ();
    }
    
    public void get (int index, Register r, int offset) {
        r.setNode (m_node);
    }
}
