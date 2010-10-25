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

public class Bitmap extends Node {
    int m_sx, m_sy, m_rotation;
    boolean m_doScale;

    Bitmap () {
        super (1);
        //System.out.println ("Bitmap created");
        m_field[0] = new SFVec2f (0, 0, this); // scale
    }
    
    void start (Context c) {
        fieldChanged (m_field[0]);
    }

    void render (Context c) {
        //System.out.println ("Bitmap::render "+m_type+" /"+m_region+" updated="+m_isUpdated);
        int w = m_region.x1-m_region.x0+1;
        int h = m_region.y1-m_region.y0+1;
        if (m_type == AppearanceContext.TYPE_BITMAP) {
            m_ac.m_image.drawImage (c.gc, m_region.x0, m_region.y0, w, h, m_ac.m_transparency, m_rotation);
        }
    }

    boolean compose (Context c, Region clip, boolean forceUpdate) {
        boolean updated = isUpdated (forceUpdate);
        m_ac = c.ac;
        if (m_ac.m_image != null) {
            if (updated) { // only compute region on updates !
                int w = m_ac.m_image.m_srcW; 
                int h = m_ac.m_image.m_srcH;
                m_type = AppearanceContext.TYPE_BITMAP;
                //MCP: (k+1)%2 Prevent rescale of odd sized images to even sized images  
                m_region.setFloat (-w/2, h/2, w/2-(w+1)%2, -h/2+(h+1)%2);
                if (m_doScale) {
                    c.matrix.push ();
                    c.matrix.scale (m_sx, m_sy);
                    c.matrix.transform (m_region);
                    c.matrix.pop ();
                } else {
                    c.matrix.transform (m_region);
                }
                m_region.toInt ();
                m_rotation = m_region.getRotationAndNormalize ();
            }

            if (!isVisible(clip, c.bounds)) {
                return updated;
            }

            m_region.x1 += 1;
            m_region.y1 += 1;
            updated |= m_ac.isUpdated (m_region);
            c.addRenderNode (this);

            if (updated) {
                m_ac.addClip (clip, m_region);
            }
            m_region.x1 -= 1;
            m_region.y1 -= 1;
        }
        return updated;
    }

    public void fieldChanged (Field f) {
        m_isUpdated = true;
        m_sx = ((SFVec2f)f).m_x;
        m_sy = ((SFVec2f)f).m_y;
        m_doScale = (m_sx > 0) && (m_sy > 0);
    }
}
