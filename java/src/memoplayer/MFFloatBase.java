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

public class MFFloatBase extends Field {
    int [] m_value;
    int m_size, // the number of SFElements 
        m_realSize; // the number of floats i.e. = m_size * m_chunk
    int m_chunk; // 1 for MFFloat, 2 for MFvec2f, 3 for MFColor

    MFFloatBase (int chunk, Observer o) {
        super (o);
        m_chunk = chunk;
    }

    MFFloatBase (int chunk) {
        this (chunk, null);
    }

    void ensureCapacity (int size) {
        int newSize = size*m_chunk;
        if (newSize > m_realSize) {
            int [] tmp = new int [newSize];
            if (m_size > 0) {
                System.arraycopy (m_value, 0, tmp, 0, m_realSize);
            }
            m_realSize = newSize;
            m_value = tmp;
        }
        m_size = size;
    }
    
    void decode (DataInputStream dis, Node [] table, Decoder decoder) {
        int size = Decoder.readUnsignedByte (dis);
        if (size == 255) {
            size = Decoder.readUnsignedByte (dis) * 255 + Decoder.readUnsignedByte (dis) ;
        }
        //System.out.print (size+ " [ ");
        if (size > 0) {
            ensureCapacity (size);
            for (int i = 0; i < m_size; i++) {
                for (int k = 0; k < m_chunk; k++) {
                    m_value[i*m_chunk+k] = Decoder.readInt (dis);
                    //System.out.print (FixFloat.toString(m_value[i])+" ");
                }
            }
        }
        //System.out.println ("]");
    } 
    
    int [] getValues () { return m_value; }
    
    void setValues (MFFloatBase m) {
        ensureCapacity (m.m_size);
        System.arraycopy (m.m_value, 0, m_value, 0, m_size*m_chunk);
        notifyChange ();
    }

    void setValue (int pos, int x, int y, int z) {
        ensureCapacity (pos+1);
        pos *= m_chunk;
        m_value [pos++] = x;
        m_value [pos++] = y;
        m_value [pos] = z;
        notifyChange ();
    }
    
    void copyValue (Field f) {
        setValues ((MFFloatBase) f);
    }
 
    public void set (int index, Register r, int offset) {
        if (offset >= 0) {
            if (offset >= m_size) {
                ensureCapacity(offset+1);
            }
            m_value [offset*m_chunk+(index-1)] = r.getFloat ();
            notifyChange ();
        }
    }
    
    public void get (int index, Register r, int offset) {
        if (index == Field.LENGTH_IDX) {
            r.setInt (m_size);
        } else if (index == Field.OBJECT_IDX) {
            r.setField (this);
        } else if (offset >= 0 && offset < m_size) {
            r.setFloat (m_value [offset*m_chunk+(index-1)]);
        } else { // out of bounds, return default value
            r.setFloat(0);
        }
    }  

}
