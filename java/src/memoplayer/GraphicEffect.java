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
//#ifndef BlackBerry
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
//#endif

public class GraphicEffect {
    public final static int ROUND_RECT = 0;
    public final static int CIRCLE = 1;
    
    private int m_w, m_h, m_trs, m_color, m_rx, m_ry;
    private int [] m_data;
    private boolean m_visible = true;
    private Image m_img;
    GraphicEffect () { }

    boolean checkForSizeChanged (int w, int h) {
        if (w != m_w || h != m_h) {
            m_data = new int [w*h];
            m_w = w;
            m_h = h;
            return true;
        }
        return false;
    }
    void setData (int v) {
        int l = m_w*m_h;
        for (int i = 0; i < l; i++) {
            m_data [i] = v;
        }
    }

    void fillBlock (Graphics g, int x, int y, int w, int h, int color, int trs) {
//#ifdef platform.android
        g.setColor (color);
        g.setAlpha (255-((trs*255)>>16));
        g.fillRect (x, y, w, h);
        g.setAlpha (255);
//#else
        if (checkForSizeChanged (w, h) || trs != m_trs || color != m_color) {
            m_trs = trs;
            m_color = color;
            int alpha = 255-FixFloat.fix2int (trs*255);
            m_visible =  alpha > 1;
            setData ( (color & 0x00FFFFFF)|(alpha<<24) );
        }
        if (m_visible) {
//#ifdef MM.blitWithImage
            if (ImageContext.s_blitWithImage) {
                g.drawImage(Image.createRGBImage (m_data, w, h, true), x, y, 0);
                return;
            }
//#endif
            int n_w=w;
            int n_h=h;
            if(x<0){ 
                n_w+=x;
                x=0;//pb sur samsung si x<0 FTE 06/11/07                
            }
            if(y<0){
                n_h+=y;
                y=0;
            }
            g.drawRGB (m_data, 0, w, x, y,n_w,n_h, true);
        }
//#endif
    }
    
    void fillEffect (Graphics g, int x, int y, int w, int h, int color, int trs, int rx, int ry, int effect) {
//#ifdef platform.android
        g.setColor (color);
        g.setAlpha (255-((trs*255)>>16));
        switch (effect) {
        case ROUND_RECT: g.fillRoundRect (x, y, w, h, rx, ry); break;
        case CIRCLE:     g.fillArc(x, y,  w, h, rx, ry);          break;
        }
        g.setAlpha (255);
//#else
        Graphics gc = null;
        if (checkForSizeChanged (w, h)) {
            m_img = Image.createImage (w, h);
            m_color = color-1; // force to recompute color (and then alpha)
            m_rx = rx; m_ry = ry; // prevent cleaning image the first time
        } 
        if (m_rx != rx || m_ry != ry) { // clean image when radius changes
            m_rx = rx; m_ry = ry;
            gc = m_img.getGraphics ();
            gc.setColor (0xFFFFFF);
            gc.fillRect (0, 0, w, h);
            m_color = color-1; // force to recompute color (and then alpha)
        }
        if (m_color != color) {
            m_color = color;
            if (gc == null) gc = m_img.getGraphics ();
            gc.setColor (0);
            switch (effect) {
            case ROUND_RECT: gc.fillRoundRect (0, 0, w, h, rx, ry); break;
            case CIRCLE:     gc.fillArc (0, 0, w, h, rx, ry);       break;
            }
            gc = null;
            m_trs = trs-1; // force to recompute alpha
        }
        if (m_trs != trs) {
            m_trs = trs;
            int alpha = 255-FixFloat.fix2int (trs*255);
            m_visible =  alpha > 1;
            m_img.getRGB (m_data, 0, w, 0, 0, w, h);
            int v = m_color | (alpha << 24);
            int l = w*h;
            for (int i = 0; i < l; i++) {
                m_data [i] = m_data[i] == 0xFFFFFFFF ? 0 : v;
            }
        }
        if (m_visible) {
//#ifdef MM.blitWithImage
            if (ImageContext.s_blitWithImage) {
                g.drawImage(Image.createRGBImage (m_data, w, h, true), x, y, 0);
                return;
            }
//#endif
            g.drawRGB (m_data, 0, w, x, y, w, h, true);
        }
//#endif  
    }
}
