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


public class IndexedLineSet2D extends IndexedFaceSet2D {
    
    void render (Context c) {
        if (m_nbPoints == 0) {
            return;
        }
        c.gc.setColor (m_ac.m_color);
        int nbIndexes = ((MFInt32)m_field[0]).m_size;
        int [] indexes = ((MFInt32)m_field[0]).getValues ();
        int p = 0;
        int ci = 0;
        while ((nbIndexes-p) > 2) {
            if (m_nbColors > ci) {
                c.gc.setColor (m_colors[ci]);
            }
            int x1 = m_points[indexes[p]*2];
            int y1 = m_points[indexes[p]*2+1];
            for (++p; p < nbIndexes; p++) {
                int j = indexes[p]*2;
                if (j < 0) {
                    ci++;
                    p++;
                    break;
                }
                int x2 = m_points[j];
                int y2 = m_points[j+1];
                c.gc.drawLine(x1, y1, x2, y2);
                x1 = x2; y1 = y2;
            }
        }
    }
}
