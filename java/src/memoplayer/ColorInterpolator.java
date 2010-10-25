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

public class ColorInterpolator extends Interpolator {
    ColorInterpolator () {
        super (4);
        ////System.out.println ("ColorInterpolator created");
        m_field[1] = new SFColor (0, 0, 0); // value_changed
        m_field[3] = new MFColor (); // keyValue
    }
    
    void interpolateValue (int index, int coef) {
        int [] values = ((MFColor)m_field[3]).getValues();

        index *= 3;
        int r = values[index] + (((values[index+3]-values[index])*coef) >> SCALE_BITS);
        index += 1;
        int g = values[index] + (((values[index+3]-values[index])*coef) >> SCALE_BITS);
        index += 1;
        int b = values[index] + (((values[index+3]-values[index])*coef) >> SCALE_BITS);

        ((SFColor)m_field[1]).setValue (r, g, b);
    }

}
