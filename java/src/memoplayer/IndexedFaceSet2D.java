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

public class IndexedFaceSet2D extends Node {
    MFVec2f m_coord;
    MFColor m_color;
    int [] m_points;
    int [] m_colors;
    int m_nbPoints, m_nbColors;

    IndexedFaceSet2D () {
        super (3);
        //System.out.println ("IndexedFaceSet2D created");
        m_field[0] = new MFInt32 (this); // index
        m_field[1] = new SFNode (); // coord
        m_field[2] = new SFNode (); // color
    }

    void start (Context c) {
        fieldChanged (m_field[0]);
        Node tmp = ((SFNode)m_field[1]).getValue ();
        if (tmp != null) {
            m_coord = (MFVec2f) tmp.m_field[0];
            m_coord.addObserver (this);
            fieldChanged (m_coord);
        }
        tmp = ((SFNode)m_field[2]).getValue ();
        if (tmp != null) {
            m_color = (MFColor) tmp.m_field[0];
            m_color.addObserver (this);
            fieldChanged (m_color);
        }
    }

//     void stop (Context c) {
//     }
    
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
            int x2 = m_points[indexes[++p]*2];
            int y2 = m_points[indexes[p]*2+1];
            for (++p; p < nbIndexes; p++) {
                int j = indexes[p]*2;
                if (j < 0) {
                    ci++;
                    p++;
                    break;
                }
                int x3 = m_points[j];
                int y3 = m_points[j+1];
                //System.out.println ("render "+x1+", "+y1+", "+x2+", "+y2+", "+x3+", "+y3); 
                c.gc.fillTriangle (x1, y1, x2, y2, x3, y3);
                x2 = x3; y2 = y3;
            }
            //System.out.println ("render2: "+p+"/"+nbIndexes);
        }

    }

    void computePoints (Matrix m, boolean updated) {
        if (updated && m_nbPoints > 0) {
            Point p = new Point ();
            int [] org = m_coord.getValues ();
            int len = m_nbPoints*2;
            // First point
            p.set (org[0], org[1]);
            m.transform (p);
            p.toInt ();
            m_points[0] = p.x;
            m_points[1] = p.y;
            m_region.set (p.x, p.y, p.x, p.y);
            // Other points
            for (int i = 2; i < len; i+=2) {
                p.set (org[i], org[i+1]);
                m.transform (p);
                p.toInt ();
                m_points[i] = p.x;
                m_points[i+1] = p.y;
                m_region.addInt (p.x, p.y, p.x, p.y);
            }
        }
    }

    boolean compose (Context c, Region clip, boolean forceUpdate) {
        boolean updated = isUpdated (forceUpdate);
        m_ac = c.ac;
        
        computePoints (c.matrix, updated);
        
        if (!isVisible(clip, c.bounds)) {
            return updated;
        }
        
        updated |= m_ac.isUpdated (m_region);
        c.addRenderNode (this);
        if (updated) {
            m_ac.addClip (clip, m_region);
        }
        
        return updated;
    }

    public void fieldChanged (Field f) {
        m_isUpdated = true;
        if (f == m_field[0]) {  // index
        } else if (f == m_coord) { 
            m_nbPoints = m_coord.m_size;
            m_points = new int [ m_nbPoints * 2];
        } else if (f == m_color) { 
            m_nbColors = m_color.m_size;
            m_colors = new int [ m_nbColors];
            for (int i = 0; i < m_nbColors; i++) {
                m_colors[i] = m_color.getRgb(i);
            }
        }
    }

}
