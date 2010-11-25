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

// Image wrapper class for midp Image class

import net.rim.device.api.system.Bitmap;

public class Image {

	Bitmap m_bitmap;

	protected Image() {}

	protected Image(int width, int height) {
		m_bitmap = new Bitmap(width,height);
	}

	protected Image(net.rim.device.api.system.Bitmap bitmap) {
		m_bitmap = bitmap;
	}

	public static Image createImage(int width, int height) {
		return new Image(width,height);
	}

	public Graphics getGraphics() {
		return new Graphics( m_bitmap );
	}

	public void getRGB( int[] rgbData, int offset, int scanLength, int x, int y, int width, int height) { 
		m_bitmap.getARGB(rgbData, offset, scanLength, x, y, width, height);
	}

	public void setRGB( int[] rgbData, int offset, int scanLength, int x, int y, int width, int height) { 
		m_bitmap.setARGB(rgbData, offset, scanLength, x, y, width, height);
	}

	public int getWidth() {
		return m_bitmap.getWidth();
	}

	public int getHeight() {
		return m_bitmap.getHeight();
	}

	public static Image createImage(byte[] imageData, int imageOffset, int imageLength) {
		Bitmap bitmap = Bitmap.createBitmapFromBytes(imageData, imageOffset, imageLength, 1) ;
		return new Image(bitmap);
	}

	public static Image createRGBImage(int[] rgb, int width, int height, boolean processAlpha) {
		Bitmap bitmap = new Bitmap(width, height) ;
		bitmap.setARGB( rgb, 0, width, 0, 0, width, height); 
		return new Image(bitmap);
	}
}
