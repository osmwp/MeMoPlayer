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

class SFRotation extends SFVec3f {
    int m_a;

    SFRotation (int x, int y, int z, int a, Observer o) {
        super (x, y, z, o);
        m_a = a;
    }
    SFRotation (Observer o) {
        super (0, 1<<16, 0, o);
        m_a = 0;
    }
    SFRotation (int x, int y, int z, int a) {
        super (x, y, z, null);
        m_a = a;
    }

    void decode (DataInputStream dis, Node [] table, Decoder decoder) {
        super.decode (dis, table, decoder);
        m_a = Decoder.readInt (dis);
        //System.out.println ("SFVec2f.decode: "+ FixFloat.str(m_x)+", "+FixFloat.str(m_y));
    }

    void setValue (int x, int y, int z, int a) {
        m_a = a;
        super.setValue (x, y, z);
    }
    
    void copyValue (Field f) {
        SFRotation s = (SFRotation)f;
        m_x = s.m_x;
        m_y = s.m_y;
        m_z = s.m_z;
        m_a = s.m_a;
        notifyChange ();
        //setValue (s.m_x, s.m_y, s.m_z, s.m_a);
    }
    
    public void set (int index, Register r, int offset) {
        if (index == 0) {
            copyValue (r.getField ());
        } else {
            if (index == 1) {
                m_x = r.getFloat ();
            } else if (index == 2) {
                m_y = r.getFloat ();
            } else if (index == 3) {
                m_z = r.getFloat ();
            } else {
                m_a = r.getFloat ();
            }
            notifyChange ();
        }
    }

    public void get (int index, Register r, int offset) {
        if (index == 0) {
            r.setField (this);
        } else if (index == 1) {
            r.setFloat (m_x);
        } else if (index == 2) {
            r.setFloat (m_y);
        } else if (index == 3) {
            r.setFloat (m_z);
        } else {
            r.setFloat (m_a);
        }
    }
 
    float getA () { return FixFloat.fix2float (m_a); }
}
