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

public class PositionInterpolator2D extends Interpolator {
    PositionInterpolator2D () {
        super (4);
        //System.out.println ("PositionInterpolator created");
        m_field[1] = new SFVec2f (0, 0, null); // value_changed
        m_field[3] = new MFVec2f (null); // keyValue
    }
    
    void interpolateValue (int index, int coef) {
        int [] values = ((MFVec2f)m_field[3]).getValues();
        index *= 2;
        int a = values[index];
        int b = values[index+2];
        int x = (int) (a + ((long)(b-a)*coef >> SCALE_BITS));
        //int x = values[index] + (((values[index+2]-values[index])*coef) >> SCALE_BITS);
        index++;
        a = values[index];
        b = values[index+2];
        //index += 1;
        int y = (int) (a + ((long)(b-a)*coef >> SCALE_BITS));
        //int y = values[index] + (((values[index+2]-values[index])*coef) >> SCALE_BITS);
        ((SFVec2f)m_field[1]).setValue (x, y);
    }
}
