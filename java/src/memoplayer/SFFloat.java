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

class SFFloat extends Field {
    int m_f;
    
    SFFloat (int f, Observer o) {
        super (o);
        m_f = f;
    }

    SFFloat (int f) {
        this (f, null);
    }
    
    void decode (DataInputStream dis, Node [] table, Decoder decoder) {
        m_f = Decoder.readInt (dis);
        //System.out.println ("SFFloat.decode: "+ FixFloat.toString(m_f));
    } 
    
    int getValue () { return m_f; }

    float getFloatValue () { return FixFloat.fix2float(m_f); }
    
    void setValue (int f) { 
        m_f = f; notifyChange (); 
    }
    
    void copyValue (Field f) {
        //setValue (((SFFloat)f).getValue ());
        m_f = ((SFFloat)f).m_f; notifyChange ();
    }
    
    public void set (int index, Register r, int offset) {
        m_f = r.getFloat ();
        notifyChange ();
    }
    public void get (int index, Register r, int offset) {
        r.setFloat (m_f);
    }

}
