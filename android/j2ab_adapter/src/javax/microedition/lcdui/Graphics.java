/*
 * Copyright (C) 2009 The J2AB Project
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

package javax.microedition.lcdui;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Canvas.VertexMode;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;

public class Graphics {
    public static final int HCENTER  = 1;
    public static final int VCENTER  = 2;
    public static final int LEFT     = 4;
    public static final int RIGHT    = 8;
    public static final int TOP      = 16;
    public static final int BOTTOM   = 32;
    public static final int BASELINE = 64;
    
    public static final int SOLID    = 0;
    public static final int DOTTED   = 1;
    
    private static final int HRZ_MASK = 0x0F;
    private static final int VRT_MASK = 0xF0;

    public android.graphics.Canvas canvas;
    private javax.microedition.lcdui.Font font;
    private Paint paint, tmpPaint;
    private Rect rect = new Rect ();
    private RectF rectf = new RectF ();
    private Path path = new Path();
    private int tx, ty;
    private Matrix mMatrix;

    public Graphics(Bitmap bitmap) {
        this (new android.graphics.Canvas (bitmap));
    }

    public Graphics(android.graphics.Canvas aCanvas) {
        setFont (Font.getDefaultFont ());
        canvas = aCanvas;
        paint = new Paint ();
        tmpPaint = new Paint ();
        canvas.save ();
    }

    public android.graphics.Canvas getCanvas () {
        return canvas;
    }

    public int getClipX () {
        return canvas.getClipBounds ().left;
    }

    public int getClipY () {
        return canvas.getClipBounds ().top;
    }

    public int getClipWidth () {
        return canvas.getClipBounds ().width ();
    }

    public int getClipHeight () {
        return canvas.getClipBounds ().height ();
    }

    public int getColor () {
        return paint.getColor () & 0x00FFFFFF;
    }

    public void setColor (int color) {
        paint.setColor (0xFF000000 | color);
    }
    
    public void setColor (int r, int g, int b) {
    	paint.setColor ((r & 0xFF) << 32 | (g & 0xFF) << 16 | (b & 0xFF));
    }

    public void setStrokeStyle (int style) {
        if (style == DOTTED) {
            paint.setStrokeWidth (2);
        } else if (style == SOLID) {
            paint.setStrokeWidth (0);
        }
    }
    
    // Non standard !
    public void setAlpha (int alpha) {
        paint.setAlpha(alpha);
    }

    public void fillRect (int x, int y, int width, int height) {
        canvas.drawRect (x, y, x + width, y + height, paint);
    }

    public void fillRoundRect (int x, int y, int width, int height, int rx, int ry) {
        rectf.left = x;
        rectf.right = x+width;
        rectf.top = y;
        rectf.bottom = y+height;
        canvas.drawRoundRect (rectf, rx, ry, paint);
    }
    
    public void fillRect (int x, int y, int width, int height, int alpha) {
        tmpPaint.set(paint);
        tmpPaint.setAlpha(alpha);
        canvas.drawRect (x, y, x + width, y + height, tmpPaint);
    }

    public void drawScaledImage (javax.microedition.lcdui.Image image, int x, int y, int width, int height, boolean filter) {
        rect.left = x;
        rect.right = x+width;
        rect.top = y;
        rect.bottom = y+height;
        tmpPaint.setFilterBitmap (filter);
        canvas.drawBitmap (image.getBitmap (), null, rect, tmpPaint);
    }
    
    public void drawScaledRotatedImage (javax.microedition.lcdui.Image image, int x, int y, int width, int height, int angle, boolean filter) {
        Bitmap src = image.getBitmap();
        int w = src.getWidth();
        int h = src.getHeight();
        tmpPaint.set (paint);
        tmpPaint.setFilterBitmap (filter);
        if (angle != 0) {
            if (mMatrix == null) {
                mMatrix = new Matrix();
            }
            float scaleWidth = width / (float) w;
            float scaleHeight = height / (float) h;
            final Matrix matrix = mMatrix;
            matrix.reset();
            matrix.postTranslate(-w/2, -h/2);
            matrix.postRotate(angle);
            matrix.postTranslate(w/2, h/2);
            matrix.postScale(scaleWidth, scaleHeight);
            matrix.postTranslate(x, y);
            canvas.drawBitmap(src, matrix, tmpPaint);
        } else if (w != width || h != height) {
            drawScaledImage (image, x, y, width, height, filter);
        } else {
            canvas.drawBitmap (image.getBitmap (), x, y, tmpPaint);
        }
    }
    
    public void drawImage (javax.microedition.lcdui.Image image, int x, int y, int anchor) {
        if ((anchor & LEFT) == 0) {
            if ((anchor & HCENTER) != 0) {
                x -= image.getWidth () / 2;
            } else if ((anchor & RIGHT) != 0) {
                x -= image.getWidth ();
            }
        }
        if ((anchor & TOP) == 0) {
            if ((anchor & VCENTER) != 0) {
                y -= image.getHeight () / 2;
            } else if ((anchor & BOTTOM) != 0) {
                y -= image.getHeight ();
            }
        }
        canvas.drawBitmap (image.getBitmap (), x, y, null);
    }

    public void drawLine (int x1, int y1, int x2, int y2) {
        canvas.drawLine (x1, y1, x2, y2, paint);
    }

    public void drawRect (int x, int y, int width, int height) {
        tmpPaint.set(paint);
        tmpPaint.setStyle (Style.STROKE);
        canvas.drawRect (x, y, x + width, y + height, tmpPaint);
    }

    public void drawRoundRect (int x, int y, int width, int height, int rx, int ry) {
        rectf.left = x;
        rectf.right = x+width;
        rectf.top = y;
        rectf.bottom = y+height;
        tmpPaint.set(paint);
        tmpPaint.setStyle (Style.STROKE);
        canvas.drawRoundRect (rectf, rx, ry, tmpPaint);
    }

    public void drawArc (int x, int y, int width, int height, int startAngle, int arcAngle) {
        rectf.left = x;
        rectf.right = x+width;
        rectf.top = y;
        rectf.bottom = y+height;
        tmpPaint.set(paint);
        tmpPaint.setStyle (Style.STROKE);
        canvas.drawArc (rectf, -startAngle, -arcAngle, false, tmpPaint);
    }

    public void drawRGB (int[] rgbData, int offset, int scanlength, int x, int y, int width, int height, boolean processAlpha) {
        canvas.drawBitmap (rgbData, offset, scanlength, x, y, width, height, processAlpha, paint);
    }

    public void fillTriangle (int x1, int y1, int x2, int y2, int x3, int y3) {
        path.reset();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        path.lineTo(x3, y3);
        path.close();
        tmpPaint.set(paint);
        tmpPaint.setStyle (Style.FILL);
        canvas.drawPath(path, tmpPaint);
    }

    public javax.microedition.lcdui.Font getFont () {
        return font;
    }

    public void setFont (javax.microedition.lcdui.Font aFont) {
        Paint typefacePaint = aFont.getTypefacePaint ();
        if (paint != null) {
            paint.setTextSize (typefacePaint.getTextSize ());
            paint.setTypeface (typefacePaint.getTypeface ());
            paint.setUnderlineText (typefacePaint.isUnderlineText ());
            paint.setFlags (typefacePaint.getFlags ());
        } else {
            paint = new Paint (typefacePaint);
        }
        font = aFont;
    }

    public void drawChar (char c, int x, int y, int anchor) {
        drawString (Character.toString (c), x, y, anchor);
    }

    public void drawString (String str, int x, int y, int anchor) {
        Align align;
        switch (anchor & HRZ_MASK) {
        case HCENTER: align = Align.CENTER; break;
        case RIGHT:   align = Align.RIGHT;  break;
        default:      align = Align.LEFT;
        }
        switch (anchor & VRT_MASK) {
        case BASELINE: break; // TODO
        case BOTTOM:  y -= paint.getFontMetrics (null); break;
        default:      y += (int) paint.getTextSize ();
        }
        paint.setTextAlign (align);
        canvas.drawText (str, x, y, paint);
    }

    public void drawSubstring (String str, int offset, int len, int x, int y, int anchor) {
        Align align;
        switch (anchor & HRZ_MASK) {
        case HCENTER: align = Align.CENTER; break;
        case RIGHT:   align = Align.RIGHT;  break;
        default:      align = Align.LEFT;
        }
        switch (anchor & VRT_MASK) {
        case BASELINE: break; // TODO
        case BOTTOM:  y -= paint.getFontMetrics (null); break;
        default:      y += (int) paint.getTextSize ();
        }
        paint.setTextAlign (align);
        canvas.drawText (str, offset, offset + len, x, y, paint);
    }

    public void clipRect (int x, int y, int w, int h) {
        int H = canvas.getHeight(), W = canvas.getWidth();
        if (x < 0) x=0; 
        if (y < 0) y=0;
        w += x;
        h += y;
        if (w > W) w = W;
        if (h > H) h = H;
        canvas.clipRect (x, y, w, h);
    }

    public void setClip (int x, int y, int w, int h) {
        int H = canvas.getHeight(), W = canvas.getWidth();
        if (x < 0) x=0;            if (y < 0) y=0;
        w += x;                    h += y;
        if (w > W) w = W;          if (h > H) h = H;
        canvas.restore ();
        canvas.save ();
        canvas.translate (tx, ty);
        canvas.clipRect (x, y, w, h);
    }

    public void fillArc (int x, int y, int width, int height, int startAngle, int arcAngle) {
        rectf.left = x;
        rectf.right = x+width;
        rectf.top = y;
        rectf.bottom = y+height;
        tmpPaint.set(paint);
        tmpPaint.setStyle(Style.FILL);
        canvas.drawArc (rectf, -startAngle, -arcAngle, true, tmpPaint);
    }

    public void translate (int x, int y) {
        tx += x;
        ty += y;
        canvas.translate (x, y);
    }

    public int getTranslateX () {
        return tx;
    }

    public int getTranslateY () {
        return ty;
    }
}
