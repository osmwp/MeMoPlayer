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

class MFNode extends Field {
    Node [] m_node = null;
    int m_size = 0;

    MFNode (Observer o) {
        super (o);
    }
    MFNode () {
        super (null);
    }

    void decode (DataInputStream dis, Node [] table, Decoder decoder) {
        //System.out.print ("decoding MFNode: #");
        m_size = Decoder.readUnsignedByte (dis);
        if (m_size == 255) {
            m_size = Decoder.readUnsignedByte (dis) * 255 + Decoder.readUnsignedByte (dis) ;
        }
        //System.out.println (m_size);
        if (m_size > 0) {
            m_node = new Node [m_size];
            for (int i = 0; i < m_size; i++) {
                m_node[i] = Node.decode (dis, table, decoder);
            }
        }
        //System.out.println ("decoding MFNode done");
    } 

    void copyValue (Field f) {
        MFNode m = (MFNode) f;
        m_size = m.m_size;
        m_node = new Node [m_size];
        for (int i = 0; i < m_size; i++) {
            m_node[i] = m.m_node[i];
        }
    }

    public void set (int index, Register r, int offset) {
        if (offset >= 0) {
            if (offset >= m_size) {
                //MCP: Resizing dynamically a MFNode implies that elements can be NULL
                Node [] tmp = new Node [offset+1];
                if (m_size > 0) {
                    System.arraycopy (m_node, 0, tmp, 0, m_size);
                }
                m_node = tmp;
                m_size = offset+1;
            }
            m_node [offset] = r.getNode ();
            notifyChange ();
        }
    }
    
    public void get (int index, Register r, int offset) {
        if (index == Field.LENGTH_IDX) {
            r.setInt (m_size);
        } else if (index == Field.OBJECT_IDX) {
            r.setField (this);
        } else if (offset >= 0 && offset < m_size) {
            r.setNode (m_node [offset]);
        } else { // out of bounds, return null
            r.setNode (null);
        }
    }
}
