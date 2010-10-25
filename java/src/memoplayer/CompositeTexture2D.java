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

import javax.microedition.lcdui.*;

class CompositeTexture2D extends Group {
    
    int m_w, m_h, m_w2, m_h2;
    javax.microedition.lcdui.Image m_img;
    Region m_clip;
    AppearanceContext m_ac;
    Graphics m_gc;
    ImageContext m_ic;
    Context m_backupContext;
    Point m_p;

    CompositeTexture2D () {
        super (2);
        m_clip = new Region ();
        m_field[1] = new SFVec2f (128<<16, 128<<16, null); // size
        m_backupContext = new Context ();
        m_p = new Point();
    }

    int getPowerOf2 (int n) {
        int j = 2;
        while (j < n) { j *= 2; }
        return j;
    }

    void start (Context c) {
        super.start (c);
        m_w = FixFloat.fix2int(((SFVec2f)m_field[1]).m_x);
        m_h = FixFloat.fix2int(((SFVec2f)m_field[1]).m_y);
        m_w2 = m_w;
        m_h2 = m_h;
        m_img = Image.createImage (m_w2, m_h2);
        m_gc = m_img.getGraphics ();
        m_ic = new ImageContext ();
        //m_ic.setImage (m_img, c.c3D != null);
    }

    void stop (Context c) {
        super.stop(c);
        m_img = null;
        m_ic = null;
        m_gc = null;
    }

    boolean compose (Context c, Region clip, boolean forceUpdate) {
        
        // Handle MOUSE Events, after backup but before matrix reset
        boolean forwardMouseEvent = c.event.isMouseEvent();
        if (forwardMouseEvent) {
            m_p.set(c.event.m_x<<16, c.event.m_y<<16);
            c.matrix.revTransform(m_p);
        }

        c.copyTo (m_backupContext);
        c.update (m_gc, m_w, m_h);
        c.resetMatix();
        c.clip = m_clip;
        c.clip.setInt (c.width, c.height, 0, 0);
        c.m_renderNodes = null;
        c.m_lastRenderNode = null;

        // Move event coords to new coordinate system
        if (forwardMouseEvent) {
            c.matrix.transform(m_p);
            c.event.m_x = m_p.x >> 16;
            c.event.m_y = m_p.y >> 16;
        }

        boolean updated = super.compose (c, c.clip, false);
        if (updated) {
            c.renderAll (c.clip);
            c.gc.setClip (0, 0, 0, 0);
            m_ic.setImage (m_img);
        }
        c.clearRenderNodes ();
        m_backupContext.copyTo (c);
        c.setCurrentImage (m_ic);
        return updated;
    }
}
