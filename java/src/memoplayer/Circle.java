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

public class Circle extends Node {
    int m_r, m_sa, m_aa, m_rot; // radius, startAngle, arcAngle, rotation
    GraphicEffect m_ge;

    Circle () {
        super (3);
        //System.out.println ("Circle created");
        m_field[0] = new SFFloat (1, this); // size
        m_field[1] = new SFFloat (0, this); // startAngle
        m_field[2] = new SFFloat (FixFloat.PI_TIMES_2, this); // arcAngle
        m_ge = new GraphicEffect ();
    }
    
    void start (Context c) {
        fieldChanged (m_field[0]);
        fieldChanged (m_field[1]);
        fieldChanged (m_field[2]);
    }
    
    void render (Context c) {
        int w = m_region.x1-m_region.x0;
        int h = m_region.y1-m_region.y0;
        c.gc.setColor (m_ac.m_color);
        if (m_ac.m_filled) {
            if (m_ac.m_transparency > 0) {
                m_ge.fillEffect (c.gc, m_region.x0, m_region.y0, w, h, m_ac.m_color, m_ac.m_transparency, m_rot+m_sa, m_aa, GraphicEffect.CIRCLE);
            } else {
                c.gc.fillArc (m_region.x0, m_region.y0, w, h, m_rot+m_sa, m_aa); 
            }
        } else if (m_ac.m_transparency != 1<<16) {
            c.gc.drawArc (m_region.x0, m_region.y0, w, h, m_rot+m_sa, m_aa); 
        }
    }
    
    boolean compose (Context c, Region clip, boolean forceUpdate) {
        boolean updated = isUpdated (forceUpdate);
        m_ac = c.ac;
        if (updated) { // only compute region on updates !
            m_region.setFloat (-m_r, m_r, m_r, -m_r);
            c.matrix.transform (m_region);
            m_region.toInt ();
            m_rot = m_region.getRotationAndNormalize ();
        }
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
        if (f == m_field[0]) {
            m_r = FixFloat.fix2int(((SFFloat)f).m_f);
        } else if (f == m_field[1]) {
            m_sa = FixFloat.rad2deg(((SFFloat)f).m_f) >> 16;
        } else {
            m_aa = FixFloat.rad2deg(((SFFloat)f).m_f) >> 16;
        }
    }
}
