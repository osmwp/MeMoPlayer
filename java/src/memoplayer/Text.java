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
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Image;

public class Text extends Node {
    String [] m_s;
    FontStyle m_fontStyle;
    Region [] m_box;
    int m_len, m_prevFgColor = -1;
    Image m_img; // RC 13/10/07 the offscreen image
    ImageContext m_imgCtx; // RC 13/10/07: used for text scalling/rotation (draw in an offscreen buffer)
    boolean m_offscreen; // RC 13/10/07 when resized or rotated
    int m_rotation; // RC 13/10/07 the rotation quadrant  
    Region m_textBox = new Region (); //MCP: original text box is only refreshed on computeDims

    Text() {
        this (2);
    }

    Text (int fields) {
        super (fields);
        //System.out.println ("Text created");
        m_field[0] = new MFString (this); // string
        m_field[1] = new SFNode (); // fontStyle
    }

    void start (Context c) {
        fieldChanged (m_field[0]);
        //fieldChanged (m_field[1]);
        m_fontStyle = (FontStyle)((SFNode)m_field[1]).getValue ();
        if (m_fontStyle == null) {
            m_fontStyle = new FontStyle ();
        }
        m_fontStyle.start (c);
    }

    void stop (Context c) {
        if (m_fontStyle != null) {
            m_fontStyle.stop (c);
        }
    }

    void draw (Graphics g, int fgColor, int x, int y) {
        ExternFont font = m_fontStyle.m_externFont;
        font.setAsCurrent (g, fgColor); //g.setFont (m_fontStyle.m_font);
        int min = g.getClipY();
        int max = min + g.getClipHeight();
        for (int i = 0; i < m_len; i++) {
            if ( (y+m_box[i].y0) > max) {
                break;
            }
            if ( (y+m_box[i].y1) > min) {
                font.drawString (g, m_s[i], x+m_box[i].x0, y+m_box[i].y0, Graphics.TOP|Graphics.LEFT);
                //g.drawString (m_s[i], x+m_box[i].x0, y+m_box[i].y0, Graphics.TOP|Graphics.LEFT);
            }
        }
    }

    void render (Context c) {//RCA
        if (m_isUpdated) { // an update occures AFTER the compose
            return;
        }
        if (m_offscreen && m_imgCtx != null) {
            m_imgCtx.drawImage (c.gc, m_region.x0, m_region.y0, m_region.getWidth(), m_region.getHeight(), m_ac.m_transparency, m_rotation);
        } else {
            draw (c.gc, m_ac.m_color, m_region.x0-m_textBox.x0, m_region.y0-m_textBox.y0);
        }
    }

    boolean compose (Context c, Region clip, boolean forceUpdate) {
        boolean updated = isUpdated (false) | m_fontStyle.compose (c, clip, false);        
        
        if (m_s == null) return false;

        m_ac = c.ac;

        if (updated) {
            computeDims (c);
            m_img = null; // MCP: force refresh of the offscreen buffer 
        }
        
        if (updated || forceUpdate) { // only compute region (and dependencies) on updates !
            int w = m_textBox.getWidth();
            int h = m_textBox.getHeight();
            computeRegion (c); 
            
            m_offscreen = m_ac.m_transparency > 0 || m_region.getWidth() != w || m_region.getHeight() != h || m_rotation != 0;
            if (m_offscreen && h > 0 && w > 0) { // render in an offscreen image and using an ImageContext
                boolean updateCtx = false;
                if (m_img == null) {
                    m_img = Image.createImage (w, h);
                    updateCtx = true;
                }
                if (m_imgCtx == null) {
                    m_imgCtx = new ImageContext ();
                    updateCtx = true;
                }
                if (updateCtx) {
                    m_imgCtx.setImage (m_img);
                    m_imgCtx.setFilterMode (m_fontStyle.getFilterMode ());
                }
                int fgColor = m_ac.m_color;
                if (updateCtx || fgColor != m_prevFgColor) {
                    int bgColor = 0xFF000000 + ((fgColor & 0xFF) > 128 ? 0 : 255);
                    m_prevFgColor = fgColor;
                    Graphics g = m_img.getGraphics();
                    g.setColor (bgColor);
                    g.fillRect (0, 0, w, h);
                    draw (g, fgColor, -m_textBox.x0, -m_textBox.y0);
                    m_imgCtx.makeTransparency (bgColor, fgColor);
                }
            }
        }

        if (!isVisible(clip, c.bounds)) {
            return updated;
        }

        updated |= m_ac.isUpdated (m_region) | forceUpdate;
        if (updated) {
            m_ac.addClip (clip, m_region);
        }
        c.addRenderNode (this);

        return updated;
    }

    protected void computeDims (Context c) {
        ExternFont font = m_fontStyle.m_externFont;
        int y, h = font.getHeight ();
        int nb = m_s == null ? 0 : m_len;
        //MCP: Reuse old Region objects and only allocate new ones for bigger sizes 
        int oldLen = m_box == null ? 0 : m_box.length;
        if (nb > 0 && nb > oldLen) {
            Region[] box = new Region [nb];
            for (int i = 0; i < nb; i++) {
                box[i] = i < oldLen ? m_box[i] : new Region ();
            }
            m_box = box;
        }

        switch (m_fontStyle.justifyV) {
        case FontStyle.BOTTOM:   y = font.getTopPosition () - h*nb; break;
        case FontStyle.MIDDLE:   y = font.getTopPosition () - h*nb/2; break;
        case FontStyle.BASELINE: y = -font.getBaselinePosition (); break;
        default:
        case FontStyle.TOP:      y = font.getTopPosition (); break;
        }
        
        // Compute the new region
        m_textBox.setInt(10000,10000, 0, 0);
        for (int i = 0; i < nb; i++) {
            if (m_s[i] == null) {
                m_len = i; break;
            }
            m_box[i].y1 = y+h;
            m_box[i].y0 = y;
            y += h;
            if (m_fontStyle.justifyH == FontStyle.MIDDLE) {
                m_box[i].x1 = font.stringWidth (m_s[i])/2;
                m_box[i].x0 = -m_box[i].x1;
            } else if (m_fontStyle.justifyH == FontStyle.RIGHT) {
                m_box[i].x1 = 0;
                m_box[i].x0 = -font.stringWidth (m_s[i]);
            } else {
                m_box[i].x0 = 0;
                m_box[i].x1 = font.stringWidth (m_s[i]);
            }
            m_textBox.addInt(m_box[i].x0, m_box[i].y0, m_box[i].x1, m_box[i].y1);
        }
        
        m_textBox.x0 -= 1; //RCA: fix for a bug with the 'J' letter on SE phones
        m_textBox.x1 += 2;
    }
    
    public void computeRegion (Context c) {
        m_region.set(m_textBox);
        m_region.y0 *= -1; // invert the y coords to match VRML way
        m_region.y1 *= -1;
        m_region.toFloat();
        c.matrix.transform(m_region);
        m_region.toInt();
        m_rotation = m_region.getRotationAndNormalize ();
    }

    public void fieldChanged (Field f) {
        m_isUpdated = true;
        if (f == m_field[0]) {
            //MCP: Warning: m_len != m_s.length, see MFString class.
            m_s = ((MFString)m_field[0]).m_value;
            m_len =  ((MFString)m_field[0]).m_size;
        }
    }

}
