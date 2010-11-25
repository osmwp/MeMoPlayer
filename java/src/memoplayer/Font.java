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

// Font wrapper class for midp Font class

public class Font {
	
	public net.rim.device.api.ui.Font m_font;	
	
	static final int 	FACE_MONOSPACE		= javax.microedition.lcdui.Font.FACE_MONOSPACE;
	static final int 	FACE_PROPORTIONAL	= javax.microedition.lcdui.Font.FACE_PROPORTIONAL;
	static final int 	FACE_SYSTEM			= javax.microedition.lcdui.Font.FACE_SYSTEM;
	static final int 	FONT_INPUT_TEXT		= javax.microedition.lcdui.Font.FONT_INPUT_TEXT;
	static final int 	FONT_STATIC_TEXT	= javax.microedition.lcdui.Font.FONT_STATIC_TEXT;
	static final int 	SIZE_LARGE			= 28;
	static final int 	SIZE_MEDIUM			= 24;
	static final int 	SIZE_SMALL			= 20;
	static final int 	STYLE_BOLD			= net.rim.device.api.ui.Font.BOLD;
	static final int 	STYLE_ITALIC		= net.rim.device.api.ui.Font.ITALIC;
	static final int 	STYLE_PLAIN			= net.rim.device.api.ui.Font.PLAIN;
	static final int 	STYLE_UNDERLINED	= net.rim.device.api.ui.Font.UNDERLINED;

    protected Font() {}

    protected Font(net.rim.device.api.ui.Font font) {
    	m_font = font;
    }

    public int 	charsWidth(char[] ch, int offset, int length){
    	return m_font.getAdvance( ch, offset, length);
    }

    public int 	charWidth(char ch){
    	return m_font.getAdvance( ch );
    }

    public int 	getBaselinePosition(){
    	return m_font.getBaseline();
    }

    public static Font 	getDefaultFont(){
    	return new Font(net.rim.device.api.ui.Font.getDefault());
    }

    public static Font 	getFont(int face, int style, int size){
    	return new Font(net.rim.device.api.ui.Font.getDefault().derive(style, size));
    }

    public int 	getHeight(){
    	return m_font.getHeight();
    }

    public int 	getStyle(){
    	return m_font.getStyle();
    }

    public boolean 	isBold(){
    	return m_font.isBold();
    }

    public boolean 	isItalic(){
    	return m_font.isItalic();
    }

    public boolean 	isPlain(){
    	return m_font.isPlain();
    }

    public boolean 	isUnderlined(){
    	return m_font.isUnderlined();
    }

    public int 	stringWidth(String str){
    	return m_font.getAdvance(str);
    }

    public int 	substringWidth(String str, int offset, int len){
    	return m_font.getAdvance(str, offset, len);
    }
}