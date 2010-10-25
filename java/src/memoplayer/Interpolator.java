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

public abstract class Interpolator extends Node {
    MFFloat m_key;
    protected final static int SCALE_BITS = 6;
    Interpolator (int nbFields) {
        super (nbFields);
        m_field[0] = new SFFloat (0, this); // set_fraction
        m_field[2] = m_key = new MFFloat (null); // key
    }
    
    void interpolate (int key) {
        //System.out.println ("Interpolate "+key+" "+FixFloat.str (key/1000));
        int [] keys = m_key.getValues();
        if (keys == null) {
            return;
        }
        int nbKeys =  keys.length;
        for (int i = 0; i < nbKeys-1; i++) {
            if (key >= keys[i] && key <= keys[i+1]) {
                int coef = ((key - keys[i]) << SCALE_BITS) / (keys[i+1] - keys[i]);
                interpolateValue (i, coef);
                break;
            }
        }
    }

    abstract void interpolateValue (int index, int coef);

    public void fieldChanged (Field f) {
        interpolate (((SFFloat)f).getValue());
    }
}
