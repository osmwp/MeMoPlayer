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

public class SFString extends Field {
    String m_s;


    SFString (String s, Observer o) {
        super (o);
        m_s = s;
    }

    SFString (String s) {
        m_s = s;
    }

    void decode (DataInputStream dis, Node [] table, Decoder decoder) {
        m_s = decoder.readLocaleString (dis);
        //System.out.println ("SFString.decode: "+m_s);
    } 

    void setValue (String s) { 
        m_s = s; notifyChange (); 
    }
    
    String getValue () { return m_s; }
    
    void copyValue (Field f) {
        setValue (((SFString)f).getValue ());
    }

    public void set (int index, Register r, int offset) {
        m_s = r.getString ();
        notifyChange ();
    }

    public void get (int index, Register r, int offset) {
        r.setString (m_s);
    }
}
