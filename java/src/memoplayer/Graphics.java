//#condition BlackBerry

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

// Graphics wrapper class for midp Graphics class

public class Graphics {

	public static final int TOP      = net.rim.device.api.ui.Graphics.TOP;
	public static final int LEFT     = net.rim.device.api.ui.Graphics.LEFT;
	public static final int BASELINE = net.rim.device.api.ui.Graphics.BASELINE;
	public static final int HCENTER  = net.rim.device.api.ui.Graphics.HCENTER;
    public static final int SOLID    = 0;
    public static final int DOTTED   = 0;
	
	public net.rim.device.api.ui.Graphics m_gc;

	public Graphics(net.rim.device.api.ui.Graphics gc) {
		m_gc = gc;
	}

	public Graphics(net.rim.device.api.system.Bitmap bitmap) {
//#ifdef BlackBerry.Touch
        m_gc = net.rim.device.api.ui.Graphics.create(bitmap);
//#else
        m_gc = new net.rim.device.api.ui.Graphics(bitmap);
//#endif
	}

	int m_x=0,m_y=0;
	int m_w=0,m_h=0;

    int stackCounter = 0;
	
    public void popAllContext() {
        while ( stackCounter > 0 ) {
            popContext();
        }
    }

    public void popContext() {
    	if(stackCounter==0)
    		return;
		m_gc.popContext();
		stackCounter--;
	}

	public void clipRect(int x, int y, int w, int h) {

	    m_gc.pushContext(x, y, w, h, 0, 0);
		stackCounter++;
	}

	public void setClip(int x, int y, int w, int h) {
		m_x=x; m_y=y;
		m_w=w; m_h=h;

		popAllContext();

		if( m_w==0 && m_h==0) {
		    return;
        }

        // save previous clip and push new clip
		m_gc.pushContext(x, y, w, h, 0, 0);
		stackCounter++;
	}

	public int getClipX() {
		return m_x;
	}
	public int getClipY() {
		return m_y;
	}

	public int getClipWidth() {
		return m_w;
	}

	public int getClipHeight() {
		return m_h;
	}

	public void fillTriangle(int x1,int y1,
				             int x2, int y2,
				             int x3, int y3) {
		int[] x = new int[4];
		int[] y = new int[4];
		x[0]=x1; y[0]=y1;
		x[1]=x2; y[1]=y2;
		x[2]=x3; y[2]=y3;
		x[3]=x1; y[3]=y1;
		m_gc.drawFilledPath(x,y,null,null);
	}

	public void drawRGB(int[] rgbData, int offset, int scanlength,
			            int x, int y, int width, int height, boolean processAlpha) {
		if(processAlpha)
			m_gc.drawARGB(rgbData, offset, scanlength, x, y, width, height);
		else
			m_gc.drawRGB(rgbData, offset, scanlength, x, y, width, height);
	}

	public void drawChar(char character, int x, int y, int flags) {
		int w = m_gc.getFont().getAdvance(character);
		m_gc.drawText(character, x, y, flags, w);
	}

	public void drawString(String str, int x, int y, int flags) {
		m_gc.drawText(str, x, y, flags);
	}

	public void drawSubstring(String str, int offset, int len, int x, int y, int anchor) {
		int w = m_gc.getFont().getAdvance(str);
		m_gc.drawText(str, offset, len, x, y, anchor,w);
	}
	
	public void drawImage(Image img, int x, int y, int anchor) {
		m_gc.drawBitmap ( x, y, img.getWidth(), img.getHeight(), img.m_bitmap, 0, 0);
	}

    public void setColor(int color) {
        m_gc.setColor( color );
    }

    public void setColor(int red, int green, int blue) {
        m_gc.setColor( (red << 16) | (green << 8) | blue );
    }

	public void drawLine(int x1, int y1, int x2, int y2) {
		m_gc.drawLine( x1, y1, x2, y2);
	}

	public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
		m_gc.drawArc(x, y, width, height, startAngle, arcAngle);
	}

	public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
		m_gc.fillArc(x, y, width, height, startAngle, arcAngle);
	}

	public void drawRect(int x, int y, int width, int height) {
		m_gc.drawRect( x, y, width, height);
	}

	public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		m_gc.drawRoundRect( x, y, width, height, arcWidth, arcHeight);
	}

	public void fillRect(int x, int y, int width, int height) {
		m_gc.fillRect( x, y, width, height);
	}

	public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		m_gc.fillRoundRect( x, y, width, height, arcWidth, arcHeight);
	}

	public void setFont(Font f) {
		m_gc.setFont( f.m_font );
	}

	public int charWidth(char c) {
		return m_gc.getFont().getAdvance(c);
	}

	public void setStrokeStyle(int style) {
	    // do nothing
	}
}
