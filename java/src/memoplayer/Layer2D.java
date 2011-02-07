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

public class Layer2D extends Group {
    int m_sx, m_sy;
    boolean m_first;
    Region m_backup;
    Region m_innerBounds;

    Layer2D () {
        super (2);
        m_field[1] = new SFVec2f (-1<<16, -1<<16, this); // size
        m_backup = new Region ();
        m_innerBounds = new Region ();
        m_ac = new AppearanceContext ();
        m_ac.m_region = m_region = new Region (); // because it will not be done by a upper Shape!!
    }

    void start (Context c) {
        super.start (c);
        fieldChanged (m_field[1]);
    }

    void render (Context c) {
        if (m_first) {
            m_first = false;
            m_backup.x0 = c.gc.getClipX ();
            m_backup.y0 = c.gc.getClipY ();
            m_backup.x1 = c.gc.getClipWidth ();
            m_backup.y1 = c.gc.getClipHeight ();
//#ifdef debug.clipping
            //MCP: Print and draw the clipping zone
            c.gc.setColor(0, 255, 0);
            c.gc.drawRect(m_region.x0-1, m_region.y0-1, m_region.x1-m_region.x0+2,m_region.y1-m_region.y0+2);
//#endif
            c.gc.clipRect (m_region.x0, m_region.y0, m_region.x1-m_region.x0-1,m_region.y1-m_region.y0-1);
        } else {
            c.gc.setClip (m_backup.x0, m_backup.y0, m_backup.x1, m_backup.y1);
            m_first = true;
        }
    }

    int getWidth () { return (m_region.x1 - m_region.x0); }
    int getHeight () { return (m_region.y1 - m_region.y0); }

    // we have to return true because we always want to have underneath children clipped
    // by the layer and it may be smaller than its kids so be discarded but not the kids
    public boolean regionIntersects (Region clip) {
        return true; 
    }
    
    void computeRegion (Context c) {
        int w = m_sx == -1 ? c.width : m_sx;
        int h = m_sy == -1 ? c.height : m_sy;
        m_region.setFloat (-w/2, h/2, w/2+(w+1)%2, -h/2-(h+1)%2);
        c.matrix.transform (m_region);
        m_region.toInt ();
        m_region.getRotationAndNormalize ();
    }

    boolean compose (Context c, Region clip, boolean forceUpdate) {
        boolean updated = isUpdated (forceUpdate);
        computeRegion (c);
        Region currentBounds = c.bounds;
        try {
            if (isVisible(clip, currentBounds)) {
                //MCP: The children bounds are the intersection between
                // the context bounds and the Layer2D region.
                m_innerBounds.applyIntersection(m_region, currentBounds);
                c.addRenderNode (this);
                //MCP: m_backup is used as a clean clip for children
                m_backup.setInt(c.width, c.height, 0, 0);
                c.bounds = m_innerBounds;
                updated |= innerCompose (c, m_backup, forceUpdate);
                c.addRenderNode (this);
                m_first = true;
                //MCP: Clip of children should not be out of innerBounds
                if(updated && m_backup.applyIntersection(m_backup, m_innerBounds)) {
                    m_ac.addClip (clip, m_backup);
                }
            } else {
                //MCP: Children nodes must always be composed (even when layer is not visible).
                c.bounds = null;
                updated |= innerCompose (c, m_backup, forceUpdate);
            }
        } finally {
            // Always restore bounds whatever happens to children (like OoME)
            c.bounds = currentBounds;
        }
        return updated;
    }

    boolean innerCompose (Context c, Region clip, boolean forceUpdate) {
        return super.compose (c, clip, forceUpdate);
    }

    public void fieldChanged (Field f) {
        m_isUpdated = true;
        if (f == m_field[1]) {
            m_sx = FixFloat.fix2int(((SFVec2f)f).m_x);
            m_sy = FixFloat.fix2int(((SFVec2f)f).m_y);
        }
    }

}
