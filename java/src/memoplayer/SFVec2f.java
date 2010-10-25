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

class SFVec2f extends Field {
    int m_x, m_y;

    SFVec2f (int x, int y, Observer o) {
        super (o);
        m_x = x;
        m_y = y;
    }

    void decode (DataInputStream dis, Node [] table, Decoder decoder) {
        m_x = Decoder.readInt (dis);
        m_y = Decoder.readInt (dis);
        //System.out.println ("SFVec2f.decode: "+ FixFloat.str(m_x)+", "+FixFloat.str(m_y));
    }

    void setValue (int x, int y) {
        m_x = x; m_y = y;
        notifyChange ();
    }
    
    void copyValue (Field f) {
        try {
            SFVec2f s = (SFVec2f)f;
            setValue (s.m_x, s.m_y);
        } catch (Exception e) {
            System.err.println ("SFVec2f.copyValue: bad source field "+f);
        }
    }
    
    public void set (int index, Register r, int offset) {
        if (index == 0) {
            copyValue (r.getField ());
        } else {
            if (index == 1) {
                m_x = r.getFloat ();
            } else {
                m_y = r.getFloat ();
            }
            notifyChange ();
        }
    }

    public void get (int index, Register r, int offset) {
        if (index == 0) {
            r.setField (this);
        } else {
            r.setFloat (index == 1 ? m_x : m_y);
        }
    }
 
}
