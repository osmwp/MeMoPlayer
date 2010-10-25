//#condition api.bmlEncoder
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

import java.io.UnsupportedEncodingException;

class BmlWriter implements XmlVisitor {
    int  m_l = 0, m_tagsCnt = 0, m_openTypePos;
    String[] m_tags = new String[30];
    byte[] m_buffer = new byte[1024*10];
    
    public byte[] getBml () {
        // Keep body buffer and size
        byte[] body = m_buffer;
        int bodyLenght = m_l;
        // Allocate new buffer for table and generate table
        m_buffer = new byte[1024*2];
        m_l = 0;
        encodeTable ();
        // Allocate final buffer, copy table and body
        if (m_l > 0 && bodyLenght > 0) {
            byte[] bml = new byte[4+m_l+bodyLenght];
            bml[0] = 'B';
            bml[1] = 'M';
            bml[2] = 'L';
            bml[3] = '1';
            System.arraycopy (m_buffer, 0, bml, 4, m_l);
            System.arraycopy (body, 0, bml, 4+m_l, bodyLenght);
            m_buffer = null;
            m_tags = null;
            return bml;
        }
        return null;
    }
    
    // Private encoding helpers
    
    private int getTagId (String tag) {
        for (int id = 0; id < m_tagsCnt; id++) {
            if (m_tags[id].equals (tag)) {
                return id;
            }
        }
        if (m_tagsCnt == m_tags.length) { // realloc
            String[] old = m_tags;
            m_tags = new String[m_tagsCnt * 2];
            System.arraycopy (old, 0, m_tags, 0, m_tagsCnt);
        }
        m_tags[m_tagsCnt] = tag;
        return m_tagsCnt++;
    }
    
    private void realloc (int newSize) {
        byte[] old = m_buffer;
        m_buffer = new byte[newSize];
        System.arraycopy(old, 0, m_buffer, 0, old.length);
    }
    
    private void add (int b) {
        if (m_l == m_buffer.length) {
            realloc (m_l * 2);
        }
        m_buffer[m_l++] = (byte)b;
    }
    
    private void add (byte[] b) {
        if (m_l + b.length >= m_buffer.length) {
            realloc ((m_l*2 > m_l+b.length) ? m_l*2 : m_l*2 + b.length);
        }
        System.arraycopy(b, 0, m_buffer, m_l, b.length);
        m_l += b.length;
    }
    
    private void encodeSize (int size) {
        if (size > 255) {
            int n = size / 255;
            add (255);
            add (n);
            size -= 255*n;
        }
        add (size);
    }
    
    private void encodeString (String s) {
        // getBytes() on empty string throws ArrayOutOfBoundsException on Samsung F480
        if (s.length() != 0) {
            try {
                add (s.getBytes ("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                add (s.getBytes ());
            }
        }
        add (0);
    }
    
    private void encodeTable () {
        encodeSize (m_tagsCnt);
        for (int i = 0; i < m_tagsCnt; i++) {
            encodeString (m_tags[i]);
        }
    }
    
    // Implements XmlVisitor
    
    public void setLeave (String l, boolean startingSpace, boolean endingSpace) {
        add (3);
        encodeString (l);
    }

    public void open (String t) {
        m_openTypePos = m_l; // keep position in case tag is self closing
        add (1);
        encodeSize (getTagId (t));
    }

    public void endOfAttributes (boolean selfClosing) {
        add (0);
        if (selfClosing) {
            m_buffer[m_openTypePos] = 2; // finally, its a self closing tag
            add (0); // double 0 because close will not be called
        }
    }

    public void close (String t) {
        add (0);
    }

    public void addAttribute (String name, String value) {
        encodeSize (getTagId (name) + 1);
        encodeString (value);
    }
}
