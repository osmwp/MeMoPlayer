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

public class ScalarInterpolator extends Interpolator {
    ScalarInterpolator () {
        super (4);
        //System.out.println ("ScalarInterpolator created");
        m_field[1] = new SFFloat (0, null); // value_changed
        m_field[3] = new MFFloat (); // keyValue
    }
    
    void interpolateValue (int index, int coef) {
        int [] values = ((MFFloat)m_field[3]).getValues();
        int x = values[index] + (((values[index+1]-values[index])*coef) >> SCALE_BITS);
        ((SFFloat)m_field[1]).setValue (x);
    }

}
