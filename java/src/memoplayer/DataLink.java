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
import javax.microedition.lcdui.Image;
//#endif

//#ifdef MM.weakreference
import java.lang.ref.WeakReference;
//#endif

public class DataLink {
    final static int DATA_SCENE  = 0;
    final static int DATA_IMAGE  = 1;
    final static int DATA_SCRIPT = 2;
    String m_name;
    byte [] m_data;
    int m_type;
    
    DataLink m_next;
//#ifdef MM.weakreference
    //MCP: Keep images using weak references so 
    // they can be collected by GC when not used anymore
    WeakReference m_objectRef;
//#else
    Object m_object;
    int m_count;
//#endif
    
    DataLink (String name, byte [] data, int type, DataLink next) {
        m_name = name;
        m_data = data;
        m_type = type;
        m_next = next;
    }

    static DataLink find (DataLink dl, String name) {
        while (dl != null && ! dl.m_name.equals (name)) {
            dl = dl.m_next;
        }
        return dl;
    }

    static DataLink findLast (DataLink dl, int magic) {
        DataLink last = null;
        while (dl != null) {
            if (dl.m_type == magic) {
                last = dl;
            }
            dl = dl.m_next;
        }
        return last;
    }
    
    /**
     * Recursively remove first DataLink by name.
     * Note: recursion limits its use to small DataLink lists (like ones in scene) on Android
     */
    DataLink remove (String name) {
        if (m_name.equals(name)) {
            return clean();
        } else if (m_next != null) {
            m_next = m_next.remove(name);
        }
        return this;
    }
    
    
    /**
     * Recursively remove DataLink (starting with the older ones)
     * when the total size of all DataLink is greater than limitSize
     * Note: recursion limits its use to small DataLink lists (like ones in scene) on Android
     */
    DataLink purge (int totalSize, int limitSize) {
        if (m_data != null) {
            totalSize += m_data.length;
        }
//#ifdef MM.weakreference
        Object m_object = getObject();
//#endif
        if (m_object != null && m_object instanceof Image) {
            Image i = (Image)m_object;
            totalSize += i.getHeight() * i.getHeight() * 4; // worst case ARGB
        }
        if (m_next != null) {
            m_next = m_next.purge(totalSize, limitSize);
        }
        if (totalSize > limitSize) {
            //System.out.println("DL.purge: "+m_name+": "+totalSize+"/"+limitSize);
            return clean();
        }
        return this;
    }

    /**
     * Clean DataLink, returning next linked DataLink
     */
    DataLink clean () {
        m_name = null;
        m_data = null;
//#ifdef MM.weakreference
        m_objectRef = null;
//#else
        m_object = null;
//#endif
        DataLink d = m_next;
        m_next = null;
        return d;
    }
    
    final byte [] getScene () { return m_type == Decoder.MAGIC_SCENE ? m_data : null; }
        
    final byte [] getScript () { return m_type == Decoder.MAGIC_SCRIPT ? m_data : null; }

    final byte [] getProto () { return m_type == Decoder.MAGIC_PROTO ? m_data : null; }
 
    final byte [] getLocale () { return m_type == Decoder.MAGIC_LOCALE ? m_data : null; }

    final byte [] getFontDesc () { return m_type == Decoder.MAGIC_FONT ? m_data : null; }

    final byte [] getBml () { return m_type == Decoder.MAGIC_BML ? m_data : null; }

    final byte [] getMMedia () { return m_type == Decoder.MAGIC_MMEDIA ? m_data : null; }

    final String getCss () { 
        if (m_type == Decoder.MAGIC_CSS) {
            try { return new String (m_data, "UTF-8"); } catch (Exception e) { }
            try { return new String (m_data, "utf-8"); } catch (Exception e) { }
            try { return new String (m_data, "utf8"); } catch (Exception e) { }
            try { return new String (m_data); } catch (Exception e) { }
        }
        return "";
    }


    final Object getObject () {
        Object o = null;
//#ifdef MM.weakreference
        if (m_objectRef != null) {
            o = m_objectRef.get ();
            if (o == null) {
                m_objectRef = null;
                //Logger.println("DL.getImage: "+m_name+" was cleared by GC");
            }
        }
//#else
        o = m_object;
//#endif
        return o;
    }
    
    final void setObject(Object o) {
//#ifdef MM.weakreference
        m_objectRef = new WeakReference (o);
//#else
        m_object = o;
        m_count = 1;
//#endif        
    }

    final Image getImage () {
//#ifdef MM.weakreference
        Image m_object = (Image)getObject ();
//#endif
        if (m_object != null) {
            return (Image)m_object;
        }
        if (m_data != null && m_type == Decoder.MAGIC_IMAGE) {
            try {
                m_object = Image.createImage (m_data, 0, m_data.length);
//#ifdef MM.weakreference
                setObject (m_object);
//#endif
            } catch (Throwable e) {
                Logger.println ("Datalink.getImage(): Could not create image for "+ m_name + " " + m_data.length+" => "+e);
            }
//#ifndef MM.weakreference
            m_data = null;
//#endif
            return (Image)m_object;
        }
        return null;
    }
}


