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

class SFBool extends Field {
    boolean m_v;

    SFBool (boolean x, Observer o) {
        super (o);
        m_v = x;
    }

    SFBool (boolean x) {
        this (x, null);
    }
    
    void decode (DataInputStream dis, Node [] table, Decoder decoder) {
        try {
            m_v = dis.readBoolean ();
        } catch (Exception e) {
            m_v = true;
        }
        //System.out.println ("SFBool.decode: "+m_v);
    } 
    
    boolean getValue () { return (m_v); }
    
    void setValue (boolean v) {  m_v = v; notifyChange (); }
    
    void copyValue (Field f) {
        setValue (((SFBool)f).getValue ());
    }

    public void set (int index, Register r, int offset) {
        m_v = r.getInt () > 0;
        notifyChange ();
    }
    
    public void get (int index, Register r, int offset) {
        r.setInt (m_v ? 1 : 0);
    }
}
