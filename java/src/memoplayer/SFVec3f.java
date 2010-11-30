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

class SFVec3f extends SFVec2f {
    int m_z;

    SFVec3f (int x, int y, int z) {
        super (x, y, null);
        m_z = z;
    }

    SFVec3f (int x, int y, int z, Observer o) {
        super (x, y, o);
        m_z = z;
    }

    void decode (DataInputStream dis, Node [] table, Decoder decoder) {
        super.decode (dis, table, decoder);
        m_z = Decoder.readInt (dis);
        //System.out.println ("SFVec2f.decode: "+ FixFloat.str(m_x)+", "+FixFloat.str(m_y));
    }

    void setValue (int x, int y, int z) {
        m_z = z;
        super.setValue (x, y);
    }
    
    void copyValue (Field f) {
        SFVec3f s = (SFVec3f)f;
        //setValue (s.m_x, s.m_y, s.m_z);
        m_x = s.m_x;
        m_y = s.m_y;
        m_z = s.m_z;
        notifyChange ();
    }
    
    public void set (int index, Register r, int offset) {
        if (index == 0) {
            copyValue (r.getField ());
        } else {
            if (index == 1) {
                m_x = r.getFloat ();
            } else if (index == 2) {
                m_y = r.getFloat ();
            } else {
                m_z = r.getFloat ();
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
        } else {
            r.setFloat (m_z);
        }
    }

//not used ...
    // float getX () { return FixFloat.fix2float (m_x); }
    // float getY () { return FixFloat.fix2float (m_y); }
    // float getZ () { return FixFloat.fix2float (m_z); }
}
