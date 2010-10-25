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

class MFFloat extends MFFloatBase {

    MFFloat (Observer o) {
        super (1, o);
    }

    MFFloat () {
        super (1, null);
    }

//     void decode (DataInputStream dis) {
//         //System.out.print ("decoding MFFloat: #");
//         super.decode (dis);
//     }

    public void set (int index, Register r, int offset) {
        if (offset >= 0) {
            if (offset >= m_size) {
                ensureCapacity(offset+1);
            }
            m_value [offset] = r.getFloat ();
            notifyChange ();
        }
    }
    
    public void get (int index, Register r, int offset) {
        if (index == Field.LENGTH_IDX) {
            r.setInt (m_size);
        } else if (index == Field.OBJECT_IDX) {
            r.setField (this);
        } else if (offset >= 0 && offset < m_size) {
            r.setFloat (m_value [offset]);
        } else { // out of bounds, return default value
            r.setFloat(0);
        }
    }  
}
