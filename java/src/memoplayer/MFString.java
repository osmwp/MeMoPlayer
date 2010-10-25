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

class MFString extends Field {
    //MCP: Warning: m_value.length can be bigger than m_size as no realocation is done on downsizing
    int m_size;
    String [] m_value;

    MFString (Observer o) {
        super (o);
    }

    MFString () { super (null);   }
    
    MFString (int size) { super (null); resize(size);  }

    void decode (DataInputStream dis, Node [] table, Decoder decoder) {
        //System.out.print ("decoding MFString: #");
        int size = Decoder.readUnsignedByte (dis);
        if (size == 255) {
            size = Decoder.readUnsignedByte (dis) * 255 + Decoder.readUnsignedByte (dis) ;
        }
        //System.out.print (size+ " [ ");
        if (size > 0) {
            m_size = size;
            m_value = new String [m_size];
            for (int i = 0; i < m_size; i++) {
                m_value[i] = decoder.readLocaleString (dis);
                //System.out.println ("MFString.decode: "+i+" = '"+m_value[i]+"'");
            }
        }
        //System.out.println ("]");
        super.decode (dis);
    }

    String [] getValues () { return m_value; }

    String  getValue (int i) {
        return (m_value == null || i >= m_size) ? "" : m_value [i]; 
    }
    
    void setValue (int i, String s) {
        if (i >= m_size) {
            resize(i+1);
        }
        m_value [i] = s; notifyChange ();
    }
    
    void setValueSilently (int i, String s) {
        if (i >= m_size) {
            resize(i+1);
        }
        m_value [i] = s;
    }

    void setValues (MFString m) {
        //MCP: use resize() to try to prevent realocations
        //m_size = m.m_size;
        //m_value = new String [m_size];
        resize(m.m_size);
        System.arraycopy (m.m_value, 0, m_value, 0, m_size);
        notifyChange ();
    }
    
    void copyValue (Field f) {
        //System.out.print ("MFString.copyValue: #0="+m_value[0]);
        setValues ((MFString) f);
    }

    public void set (int index, Register r, int offset) {
        if (index == Field.LENGTH_IDX) { // support pre allocation : myStr.length = 10;
            resize(r.getInt());
        }
        if (offset >= 0) {
            if (offset >= m_size) {
                resize(offset+1);
            }
            m_value [offset] = r.getString ();
            notifyChange ();
        }
    }
    
    public void get (int index, Register r, int offset) {
        if (index == Field.LENGTH_IDX) {
            r.setInt (m_size);
        } else if (index == Field.OBJECT_IDX) {
            r.setField (this);
        } else if (offset >= 0 && offset < m_size) {
            r.setString (m_value [offset]);
        } else { // out of bounds, return default value
            r.setString("");
        }
    }

    /**
     * MCP: Resize array but keep allocated slots when downsizing
     * @param size The new size
     */
    void resize (int size) {
        if (size > m_size) { // upsize
            if (m_value == null) { 
                m_value = new String[size];
            } else if (size > m_value.length) { // upsize (must reallocate)
                String [] tmp = new String [size];
                System.arraycopy (m_value, 0, tmp, 0, m_size);
                m_value = tmp;
            }
            for (int i = m_size; i < size; i++) {
                m_value[i] = "";
            }
        } else if (size >= 0 && size < m_size) { // downsize (but keep allocated array)
            for (int i = size; i < m_size; i++) {
                m_value[i] = null;
            }
        }
        m_size = size;
    }
}
