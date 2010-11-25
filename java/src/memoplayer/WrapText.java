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
//#endif

public class WrapText extends Text {
    private final static int BLINK_INTERVALL = 500;
    private final static int BLINK_DISABLED = -1;
    private final static int NONE = 0, UP = 1, DOWN = 2, LEFT = 3, RIGHT = 4, UNCHANGED = 5;
    
    private StringBuffer m_buff = new StringBuffer();
    private int m_cursorX, m_cursorY, m_cursorH; // coordinates on screen (for draw)
    private int m_cursorPos, m_cursorLine, m_cursorOffset;    // position in text (for line jumps)
    private boolean m_cursorUpdate, m_cursorBlinkState, m_editableUpdate, m_editable;
    private int m_nextBlinkTime;
    private int m_editBoxH, m_editBoxW; // clipping size in edit mode
    private Region m_editBox; // final clipping in edit mode
    int m_startX, m_startY; // text offset, used for scrolling in edit mode
    int[] m_breakLines; // track nb chars swallowed in line breaks in edit mode

    WrapText () {
        super (9);
        m_field[2] = new SFVec2f (0, 0, null); // size_changed
        m_field[3] = new SFInt32 (0, this); // maxWidth (DEPRECATED)
        m_field[4] = new SFInt32 (0, this); // maxLines (0 for unlimited)
        m_field[5] = new SFVec2f (0, 0, this); // maxSize
        m_field[6] = new SFBool (false, this); // editable
        m_field[7] = new SFInt32 (-3, this); // set_cursor
        m_field[8] = new SFInt32 (-3); // cursor_changed
        //m_field[9] = new SFColor (0,0,0, this); // cursorColor
    }
    
    void start(Context c) {
        super.start(c);
        fieldChanged (m_field[6]); // check editable
        fieldChanged (m_field[7]); // check set_cursor
        m_nextBlinkTime = BLINK_DISABLED;
        m_cursorBlinkState = false;
        m_cursorLine = -1;
        m_cursorOffset = -1;
    }
    
    void stop(Context c) {
        super.stop (c);
        stopBlink (c);
    }
    
    private String getTextAsString() {
        int len = ((MFString)m_field[0]).m_size;
        String[] s = ((MFString)m_field[0]).m_value;
        m_buff.setLength(0);
        if(len > 1) { // concatenate all in one string...
            m_buff.append(s[0]);
            for (int i=1; i<len; i++) {
                if (s[i] != null) {
                    m_buff.append('\n'+s[i]);
                }
            }
            return m_buff.toString();
        } else {
            return s[0];
        }
    }
    
    /**
     * Wrapping is done when either text uses multiple lines 
     * or total string is bigger than the maxWidth 
     * or breaklines are detected 
     * @param maxLines max number of lines to use for line wrap (text will be truncated if longer)
     * @param maxWidth max width of a line in pixel
     */
    private void wrap(int maxLines, int maxWidth) {
        boolean isOrig = m_s == ((MFString)m_field[0]).m_value;
        ExternFont font = m_fontStyle.m_externFont;
        String s = getTextAsString();
        
        if (s.length() != 0 || m_len > 1 || font.stringWidth(s) > maxWidth || s.indexOf('\n') != -1) {
            final char[] chars = s.toCharArray();
            final int length = chars.length;
            final int spaceWidth = font.charWidth(' ');
            final int charWitdh = font.charWidth('W');
            
            int space = 0;
            int spaceLeft = maxWidth;
            int wordStart = 0;

            // Unlimited wrap uses a doubling reallocation (30,60,120,etc...)
            boolean noMaxLines = false;
            if (maxLines == 0) {
                noMaxLines = true;
                maxLines = 30;
            }
            
            if (isOrig || m_s.length < maxLines) {
                m_s = new String[maxLines]; // do not overwrite the original MFString m_value
            } else if (noMaxLines) { 
                maxLines = m_s.length; // reuse last array size as maxLines when no max
            }
            
            s = null;
            m_len = 0;
            m_buff.setLength(0);
            
            for (int i=0; i<=length; i++) {
                if (i!=length && chars[i]>32) continue; // skip all text characters
                int wordSize = i-wordStart;
                //if (wordSize >= 0) { // skip multiple spaces => disable for edit mode
                    int wordWidth = font.charsWidth(chars, wordStart, wordSize);
                    //System.err.println("WORD:"+new String(chars, wordStart, wordSize)+":"+wordWidth+'/'+spaceLeft+'/'+space);
                    if (wordWidth + space > spaceLeft) { // overfull, need a new line for new word
                        
                        // check new lines are still allowed
                        if (m_len+1 >= maxLines) {
                            if (noMaxLines) { // allocate more lines to buffer
                                String[] tmp_s = m_s;
                                int l = m_s.length < maxLines ? m_s.length : maxLines;
                                maxLines *= 2;
                                m_s = new String[maxLines];
                                System.arraycopy(tmp_s, 0, m_s, 0, l);
                                //System.err.println("Wrapping : noMaxLines new max :"+maxLines);
                            } else { // truncate as no more lines can be used
                                final String dots = "...";
                                final int dotsWidth = font.stringWidth(dots);
                                                          
                                if (spaceLeft >= space + charWitdh + dotsWidth) {
                                    // Reduce word size until it fits on the line with the dots
                                    while (spaceLeft < space + wordWidth + dotsWidth && --wordSize > 1) {
                                        wordWidth = font.charsWidth(chars, wordStart, wordSize);
                                    }
                                    if (space != 0) {
                                        m_buff.append(' ');
                                    }
                                    m_buff.append(chars, wordStart, wordSize);
                                    //System.err.println("Truncated word: "+m_buff);
                                } else {
                                    // Remove space from previous word if no space to add dots
                                    if (spaceLeft < dotsWidth) {
                                        int l = m_buff.length();
                                        l -= l < 3 ? l : 3; // Math.min(l, 3);
                                        m_buff.setLength(l);
                                    }
                                    //System.err.println("No space to truncate, add dots.");
                                }
                                m_buff.append(dots);
                                space = spaceWidth;
                                break; // Maxlines reached !
                            }
                        }
                            
                        // When word size is bigger than the line, split it
                        if (wordWidth >= maxWidth) {
                            final char minus = '-'; // add minus to cut the word, exept in edit mode
                            final int minusWidth = font.charWidth(minus);
                            if (spaceLeft > space + 3*charWitdh + minusWidth) {
                                int origSize = wordSize;
                                // Reduce word until it fits with the minus sign
                                while (spaceLeft < space + wordWidth + minusWidth && --wordSize > 1) {
                                    wordWidth = font.charsWidth(chars, wordStart, wordSize);
                                }
                                if (space != 0) {
                                    m_buff.append(' ');
                                }
                                m_buff.append(chars, wordStart, wordSize);
                                m_buff.append(minus);
                                space = spaceWidth;
                                //System.err.println("Word split: "+m_buff);
                                // next word is the size what is left
                                i -= origSize - wordSize + 1;
                                wordSize = 0;
                                if (m_editable) {
                                    setBreakLine (m_len, -1, true); // Adding a minus char
                                }
                            } else if (space != 0) { // Not enough space to split word
                                //System.err.println("Flush and split on next line");
                                // This will flush the current line, and try to split on next line
                                i -= wordSize + 1;
                                wordSize = 0;
                            } else { // Not enough space to split the word on an empty line !!! 
                                // Just add the word on next line...
                                //System.err.println("WrapText: Can not split word: "+new String(chars, wordStart, wordSize)+", not enough space...");
                            }
                        } else if (m_editable) { // Keep track of line breaks for edit mode
                            setBreakLine (m_len, 1, true); // Swallowing the space char
                        }
                        
                        // Flush current line
                        if (space != 0) {
                            m_s[m_len++] = m_buff.toString();
                            m_buff.setLength(0);
                            space = 0;
                        }

                        // Adding word on new line
                        if (wordSize != 0) {
                            m_buff.append(chars, wordStart, wordSize);
                            spaceLeft = maxWidth - wordWidth;
                            space = spaceWidth;
                        } else {
                            spaceLeft = maxWidth;
                        }

                    } else {
                        // Adding word on current line
                        if (space != 0) {
                            m_buff.append(' ');
                        }
                        m_buff.append(chars, wordStart, wordSize);
                        space = spaceWidth;
                        spaceLeft -= wordWidth + spaceWidth;
                    }
                //}
                
                if (i != length && (chars[i] == '\r' || chars[i] == '\n')) { // newline
                    if (m_len+1 >= maxLines) { // check new lines are still allowed
                        if (noMaxLines) { // allocate more lines to buffer
                            String[] tmp_s = m_s;
                            int l = m_s.length < maxLines ? m_s.length : maxLines;
                            maxLines *= 2;
                            m_s = new String[maxLines];
                            System.arraycopy(tmp_s, 0, m_s, 0, l);
                            //System.err.println("Wrapping : noMaxLines new max :"+maxLines);
                        } else {
                            break; // Maxlines reached !
                        }
                    }
                    // Check for a double char \r\n new line
                    if (i+1<length && chars[i] == '\r' &&  chars[i+1] == '\n') {
                        i++; // Jump \n
                        if (m_editable) {
                            setBreakLine (m_len, 2, true); // Swallowing two chars
                        }
                    } else if (m_editable) {
                        setBreakLine(m_len, 1, true); // Swallowing the return char
                    }
                    m_s[m_len++] = m_buff.toString();
                    m_buff.setLength(0);
                    spaceLeft = maxWidth;
                    space = 0;
                }
                
                wordStart = i+1;
            }
            
            if (m_editable) {
                setBreakLine (m_len, 1, false); // Last line
            }
            
            if (m_buff.length() != 0 && m_len < maxLines) { // check new lines are still allowed
                //System.err.println("Dumping buffer:"+m_buff);
                m_s[m_len++] = m_buff.toString();
            } else {
                m_s[m_len++] = "";
            }
        }
        m_buff.setLength(0);
    }
    
    void initBreakLines () {
        if (m_breakLines == null) {
            m_breakLines = new int[10];
        } else { // reuse & reset
            for (int i=0; i<m_breakLines.length; i++) {
                m_breakLines[i] = 0;
            }
        }
    }
    
    void setBreakLine (int line, int value, boolean reservePlace) {
        if (m_breakLines.length <= line) {
            int[] old = m_breakLines;
            m_breakLines = new int[reservePlace ? old.length * 2 : line+1];
            System.arraycopy (old, 0, m_breakLines, 0, old.length);
        }
        m_breakLines[line] = value;
    }
    
    protected void computeDims (Context c) {
        int maxWidth = ((SFInt32)m_field[3]).getValue();
        int maxLines = ((SFInt32)m_field[4]).getValue();
        int maxHeight = ((SFVec2f)m_field[5]).m_y>>16;
        if (maxWidth == 0) { // Ignore maxSize.x if maxWidth is set
            maxWidth = ((SFVec2f)m_field[5]).m_x>>16;
        }
        // /!\ computeDims is called when when Font changes
        m_cursorH = m_fontStyle.m_externFont.getHeight();
        // Get min between maxLines and maxHeight (depends on font)
        maxHeight /= m_cursorH;
        if (maxHeight > 0 && (maxHeight < maxLines || maxLines == 0)) {
            maxLines = maxHeight;
        }
        maxHeight = maxLines; // saving final user-defined maxLines
        if (m_editable) {
            m_editBoxW = maxWidth;
            m_editBoxH = maxLines * m_cursorH;
            if (maxLines == 1) {
                maxWidth = Integer.MAX_VALUE; // disable wrapping in single line mode
            } else if (maxLines > 1) {
                maxLines = 0; // disable truncating in multi-line mode
            }
            initBreakLines (); // init breaklines even if not wrapping (used by setCursorPos)
        }
        // To wrap or not to wrap ?
        if (maxWidth > 0 && maxLines >= 0) {
            wrap (maxLines, maxWidth);
        }
        // Compute text boxes position and size
        super.computeDims(c);
        // Notify user with size_changed
        if (m_editable) {
            if (maxWidth == 0 || maxHeight == 0) {
                // user did not give any limit, use text size
                m_editBoxW = m_textBox.getWidth ();
                m_editBoxH = m_textBox.getHeight ();
            }
            ((SFVec2f)m_field[2]).setValue (m_editBoxW<<16, m_editBoxH<<16);
            // Check text scroll after updating the cursorPos
            setCursorPos (m_cursorPos); 
            updateCursorCoord ();
            checkScroll (UNCHANGED);
            startBlink (c);
        } else {
            ((SFVec2f)m_field[2]).setValue (m_textBox.getWidth()<<16, m_textBox.getHeight()<<16);
        }
    }
    
    public void computeRegion (Context c) {
        c.matrix.push ();
        c.matrix.translate (-m_startX<<16, m_startY<<16);
        super.computeRegion (c);
        c.matrix.pop ();
        // Compute final clipping region for edit box
        if (m_editable) {
            m_editBox.setFloat (0, 0, m_editBoxW, -m_editBoxH);
            c.matrix.transform (m_editBox);
            m_editBox.toInt ();
        }
    }
    
    void draw (Graphics g, int fgColor, int x, int y) {
        // Draw text
        super.draw (g, fgColor, x, y);
        // Draw the cursor
        if (m_cursorBlinkState) {
            //if (m_cursorColor != -1) g.setColor (m_cursorColor);
            g.setColor(fgColor); // mandatory as bitmap font will not set the color
            g.setStrokeStyle(Graphics.SOLID);
            g.drawLine (x+m_cursorX, y+m_cursorY, x+m_cursorX, y+m_cursorY+m_cursorH);
        }
    }
    
    void render(Context c) {
        if (m_editable) {
            Graphics g = c.gc;
            // Save current clip, use the edit box clip
            int cx = g.getClipX ();
            int cy = g.getClipY ();
            int cw = g.getClipWidth ();
            int ch = g.getClipHeight();
            // Set edit box clip region
            g.clipRect (m_editBox.x0, m_editBox.y0, m_editBox.getWidth(), m_editBox.getHeight());
            super.render(c);
            // Restore clip
            g.setClip(cx, cy, cw, ch);
        } else {
            super.render(c);
        }
    }
    
    boolean compose (Context c, Region clip, boolean forceUpdate) {
        boolean update;
        if (m_editableUpdate) {
            if (forceUpdate || m_isUpdated) { // postpone editable events after text update
                MyCanvas.composeAgain = true;
            } else {
                m_editableUpdate = false;
                boolean editable = ((SFBool)m_field[6]).getValue (); // editable
                if (editable != m_editable) {
                    m_editable = editable;
                    if (m_editable) {
                        m_cursorPos = m_cursorLine = m_cursorOffset = 0;
                        m_cursorX = m_cursorY = 0;
                        ((SFInt32)m_field[8]).setValue (0); // cursor_changed
                        if (m_editBox == null) {
                            m_editBox = new Region ();
                        }
                    } else {
                        m_startY = m_startX = 0;
                        stopBlink(c);
                        m_ac.addClip (clip, m_region); // TODO: add cursor region only to clip!
                    }
                    m_isUpdated = true; // force reflow of text
                }
            }
        } 
        if (m_editable) {
            int newCursorPos = m_cursorPos;
            if (m_cursorUpdate) {
                if (forceUpdate || m_isUpdated) { // postpone editable events after text update
                    MyCanvas.composeAgain = true;
                } else {
                    m_cursorUpdate = false;
                    int cursorDirection = NONE;
                    newCursorPos = ((SFInt32)m_field[7]).getValue (); // set_cursor
                    if (newCursorPos == -1 || newCursorPos == -2) { // previous/next line
                        if (newCursorPos == -1) { // previous line
                            if (m_cursorLine > 0) { // can move cursor up
                                m_cursorLine--;
                                cursorDirection = UP;
                            } else { // already reached top of text
                                ((SFInt32)m_field[8]).setValue (-1); // cursor_changed
                            }
                        } else { // next line
                            if (m_cursorLine >= 0 && m_cursorLine < m_len-1) {
                                m_cursorLine++;
                                cursorDirection = DOWN;
                            } else { // already reach bottom of text
                                ((SFInt32)m_field[8]).setValue (-2); // cursor_changed
                            }
                        }
                        m_cursorOffset = findCursorOffset (m_cursorLine, m_cursorX);
                        updateCursorCoord ();
                        newCursorPos = getCursorCharPos();
                    } else if (m_cursorPos != newCursorPos && setCursorPos (newCursorPos)) {
                        int oldCursorY = m_cursorY;
                        updateCursorCoord ();
                        if (oldCursorY == m_cursorY) { // same line
                            cursorDirection = newCursorPos > m_cursorPos ? RIGHT : LEFT;
                        } else {
                            cursorDirection = oldCursorY > m_cursorY ?  UP : DOWN;
                        }
                    } else {
                        newCursorPos = m_cursorPos; // out of range, ignore
                    }
                    if (cursorDirection != NONE) {
                        if (m_offscreen) { // force offscreen render when cursor moves
                            m_prevFgColor = 0xabcdef; // hack to force image repaint
                            forceUpdate = true;
                        }
                        forceUpdate |= checkScroll (cursorDirection);
                    }
                }
            }
            
            update = super.compose (c, clip, forceUpdate);

            // update compute cursor position *after* text update
            if (newCursorPos != m_cursorPos) { // new cursor position
                startBlink (c);
                m_cursorPos = newCursorPos;
                ((SFInt32)m_field[8]).setValue (newCursorPos); // cursor_changed
                m_ac.addClip (clip, m_region); // TODO: optimize clip
                update = true;
            /*} else if (c.event.isMouseEvent() && c.event.isInside (m_region)) { // Touch support for cursor
                Point p = new Point();
                p.set (c.event.m_x<<16, c.event.m_y<<16);
                c.matrix.push ();
                c.matrix.translate (-m_startX<<16, m_startY<<16);
                c.matrix.revTransform (p);
                c.matrix.pop ();
                p.toInt();
                if (setCursorPos (p.x, -p.y)) {
                    updateCursorCoord();
                    checkScroll(UNCHANGED);
                    m_isUpdated = true;
                    MyCanvas.composeAgain = true;
                    startBlink (c);
                    m_cursorPos = getCursorCharPos();
                    ((SFInt32)m_field[8]).setValue (m_cursorPos); // cursor_changed
                }
                m_ac.addClip (clip, m_region); // TODO: optimize clip */
            } else if (!m_offscreen && c.time >= m_nextBlinkTime) {
                m_cursorBlinkState = ! m_cursorBlinkState;
                m_nextBlinkTime = c.time + BLINK_INTERVALL;
                m_ac.addClip (clip, m_region); // TODO: optimize clip
                update = true;
            }
        } else {
            update = super.compose(c, clip, forceUpdate);
        }
        return update;
    }
    
    // Convert text position to screen coords
    private boolean updateCursorCoord () {
        if (m_cursorLine >= 0 && m_cursorLine < m_len) {
            m_cursorY = m_box[m_cursorLine].y0;
            m_cursorX = m_box[m_cursorLine].x0;
            if (m_cursorOffset > 0) {
                String sub = m_s[m_cursorLine];
                if (m_cursorOffset < sub.length ()) {
                    sub = sub.substring (0, m_cursorOffset);
                }
                m_cursorX += m_fontStyle.m_externFont.stringWidth (sub);
            }
            return true;
        }
        return false;
    }
    
    private void startBlink (Context c) {
        updateCursorCoord ();
        if (m_nextBlinkTime == BLINK_DISABLED) {
            c.scene.registerSleepy (this);
        }
        if (!m_offscreen) { // cancel blink on offscreen text render
            m_nextBlinkTime = c.time + BLINK_INTERVALL;
        }
        m_cursorBlinkState = true;
    }
    
    private void stopBlink (Context c) {
        c.scene.unregisterSleepy (this);
        m_nextBlinkTime = BLINK_DISABLED;
        m_cursorBlinkState = false;
    }
    
    private int getCursorCharPos () {
        int cursoCharPos = 0;
        for (int i=0; i<m_cursorLine; i++) {
            cursoCharPos += m_s[i].length() + m_breakLines[i];
        }
        return cursoCharPos + m_cursorOffset;
    }
    
    private boolean setCursorPos (int charPos) {
        int line;
        for (line = 0; line < m_len; line++) { // find line & offset
            int l = m_s[line].length() + m_breakLines[line];
            if (charPos < l) break;
            charPos -= l;
        }
        if (line < m_len && charPos >= 0 && charPos <= m_s[line].length()) {
            m_cursorLine = line;
            m_cursorOffset = charPos;
            return true;
        }
        return false;
    }
    
    /*private boolean setCursorPos (int x, int y) {
        for (int line = 0; line < m_len; line++) {
            Region r = m_box[line];
            if (y >= r.y0 && y < r.y1) { // find line
                m_cursorLine = line;
                if (x < r.x0) { // left of line
                    m_cursorOffset = 0;
                } else if (x > r.x1) { // right of line
                    m_cursorOffset = m_s[line].length ();
                } else { // in the line
                    m_cursorOffset = findCursorOffset (line, x);
                }
                return true;
            }
        }
        return false;
    }*/
    
    private int findCursorOffset (int line, int x) {
        x -= m_box[line].x0;
        char[] s = m_s[line].toCharArray();
        int first = 0, last = s.length, offset = 0, w;
        while (first <= last) { // dichotomic search
            offset = (last + first) / 2;
            w = m_fontStyle.m_externFont.charsWidth (s, 0, offset);
            if (Math.abs (w - x) <= 2) break;
            if (w > x) last = offset - 1;
            else      first = offset + 1;
        }
        return offset;
    }
    
    private boolean checkScroll (int direction) {
        int scrollLimit;
        switch (direction) {
        case UP:
            scrollLimit = m_textBox.getHeight() - m_editBoxH;
            if (scrollLimit > 0) { // can scroll
                if (m_cursorY < m_startY) {
                    m_startY = m_cursorY;
                    return true;
                }
            }
            break;
        case DOWN:
            scrollLimit = m_textBox.getHeight() - m_editBoxH;
            if (scrollLimit > 0) { // can scroll
                if (m_startY <= m_cursorY - m_editBoxH) {
                    m_startY = m_cursorY - (m_editBoxH - m_cursorH);
                    return true;
                }
            }
            break;
        case LEFT:
            scrollLimit = m_textBox.getWidth() - m_editBoxW;
            if (scrollLimit > 0) { // can scroll
                if (m_startX > m_cursorX) {
                    m_startX = m_cursorX - m_editBoxW / 5;
                    if (m_startX < 0) {
                        m_startX = 0;
                    }
                    return true;
                }
            }
            break;
        case RIGHT:
            scrollLimit = m_textBox.getWidth() - m_editBoxW;
            if (scrollLimit > 0) { // can scroll
                if (m_startX < m_cursorX - m_editBoxW)  {
                    m_startX = (m_cursorX - m_editBoxW) + m_editBoxW / 5;
                    if (m_startX > scrollLimit) {
                        m_startX = scrollLimit;
                    }
                    return true;
                }
            }
            break;
        case UNCHANGED:
            scrollLimit = m_textBox.getHeight() - m_editBoxH;
            if (scrollLimit > 0) { // can scroll vertically
                checkScroll (DOWN); // check that new content did not push cursor down
                if (m_startY > scrollLimit) { // check overflow
                    m_startY = scrollLimit;
                }
            } else if (m_startY != 0) {
                m_startY = 0;
            }
            scrollLimit = m_textBox.getWidth() - m_editBoxW;
            if (scrollLimit > 0) { // can scroll horizontally
                if (m_startX > scrollLimit) { // check overflow
                    m_startX = scrollLimit;
                }
            } else if (m_startX != 0) {
                m_startX = 0;
            }
            return true;
        }
        return false;
    }

//#ifdef MM.pause
    public int getWakeupTime (int time) {
        return m_nextBlinkTime == -1 ? MyCanvas.SLEEP_FOREVER : m_nextBlinkTime;
    }
//#endif
    
    public void fieldChanged (Field f) {
        if (f == m_field[6]) { // editable
            m_editableUpdate = true;
        } else if (f == m_field[7]) { // set_cursor
            m_cursorUpdate = true;
        } else {
            super.fieldChanged(f);
        }
    }
}
