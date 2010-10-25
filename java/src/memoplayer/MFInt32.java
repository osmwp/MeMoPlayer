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

class MFInt32 extends Field {
    int m_size;
    int [] m_value;
    
    MFInt32 (Observer o) {
        super (o);
    }

    MFInt32 () {
        super (null);
    }
    
    void decode (DataInputStream dis, Node [] table, Decoder decoder) {
        //System.out.print ("decoding MFInt32: #");
        int size = Decoder.readUnsignedByte (dis);
        if (size == 255) {
            size = Decoder.readUnsignedByte (dis) * 255 + Decoder.readUnsignedByte (dis) ;
        }
        //System.out.print (size+ " [ ");
        if (size > 0) {
            m_size = size;
            m_value = new int [m_size];
            for (int i = 0; i < m_size; i++) {
                m_value[i] = Decoder.readInt (dis);
                //System.out.println ("MFInt32.decode: "+i+" = '"+m_value[i]+"'");
            }
        }
        //System.out.println ("]");
        super.decode (dis);
    }

    int [] getValues () { return m_value; }

    void setValues (MFInt32 m) {
        m_size = m.m_size;
        m_value = new int [m_size];
        System.arraycopy (m.m_value, 0, m_value, 0, m_size);
        notifyChange ();
    }
    
    void copyValue (Field f) {
        //System.out.print ("MFInt32.copyValue: #0="+m_value[0]);
        setValues ((MFInt32) f);
    }

    public void set (int index, Register r, int offset) {
        if (offset >= 0) {
            if (offset >= m_size) {
                int [] tmp = new int [offset+1];
                if (m_size > 0) {
                    System.arraycopy (m_value, 0, tmp, 0, m_size);
                }
                m_value = tmp;
                m_size = offset+1;
            }
            m_value [offset] = r.getInt ();
            notifyChange ();
        }
    }

    public void get (int index, Register r, int offset) {
        if (index == Field.LENGTH_IDX) {
            r.setInt (m_size);
        } else if (index == Field.OBJECT_IDX) {
            r.setField (this);
        } else if (offset >= 0 && offset < m_size) {
            r.setInt (m_value [offset]);
        } else { // out of bounds, return default value
            r.setInt(0);
        }
    }

}
