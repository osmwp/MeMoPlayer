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

public class CoordinateInterpolator2D extends Interpolator {
    int m_nbCoords;
    CoordinateInterpolator2D () {
        super (4);
        //System.out.println ("CoordinateInterpolator2D created");
        m_field[1] = new MFVec2f (); // value_changed
        m_field[3] = new MFVec2f (); // keyValue
    }
    
    void start (Context c) {
        int s1 =  ((MFVec2f)m_field[3]).m_size;
        int s2 = ((MFFloat)m_field[2]).m_size;
        m_nbCoords = s2 > 0 ? s1 / s2 : 0; 
        //System.out.println ("Coordinterp2D: size is "+s1+"/"+s2+" = "+m_nbCoords);
        ((MFVec2f)m_field[1]).ensureCapacity (m_nbCoords);
    }
    
    void interpolateValue (int index, int coef) {
        //System.out.println ("C2DI.interpolate: "+index+" / "+coef);
        int [] input = ((MFVec2f)m_field[3]).getValues();
        int [] output = ((MFVec2f)m_field[1]).getValues();
        int i1 = m_nbCoords*index*2;
        int i2 = i1+m_nbCoords*2;
        for (int i = 0; i < m_nbCoords*2; i++) {
            output[i] = input[i1+i] + (((input[i2+i]-input[i1+i])*coef) >> SCALE_BITS);
        }
        m_field[1].notifyChange ();
    }

}
