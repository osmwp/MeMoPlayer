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
import javax.microedition.lcdui.Image;
import java.io.*;

public class Decoder {
    final static int MAGIC_SCENE  = 0xAAAA;
    final static int MAGIC_IMAGE  = 0xBBBB;
    final static int MAGIC_SCRIPT = 0xCCCC;
    final static int MAGIC_PROTO  = 0xDDDD;
    final static int MAGIC_LOCALE = 0xEEEE;
    final static int MAGIC_MMEDIA = 0x5555;
    final static int MAGIC_CSS    = 0xCC55;
    final static int MAGIC_FONT   = 0xF0E1;
    final static int MAGIC_BML    = 0xB111;
    final static int MAGIC_END    = 0xFFFF;

    final static String DEFAULT_LOCALE = "default.lng";
    
    int m_state = Loadable.ERROR;
    //Node scene = null;
    //Route routes = null;
    //int m_state;   // the state of the decoding, see Loadable 
    //int m_current = -1; // the actual number of bytes read
    DataLink m_dataLink;
    String baseName, sceneName;
    //DataInputStream dis;
    File m_file;

    Proto [] m_protoStack;
    int m_currentProto;
    
    String m_locale = null; // name of the locale loaded in the table
    // array of string containing the current localized Strings, set by Inline
    String [] m_localeTable = null; 
 
    static StringBuffer s_sb = new StringBuffer ();
    
    Decoder (String filename) {
        //Logger.println ("Decoder: loading "+filename);
        setState (Loadable.OPENING);
        int idx= filename.lastIndexOf ('/');
        if (idx >= 0) {
            baseName = filename.substring (0,idx+1);
            sceneName = filename.substring (idx+1);;
        } else {
            baseName = "";
            sceneName = filename;
        }
        //new Exception ("Decoder: loading "+filename+" = "+baseName+"/"+sceneName).printStackTrace();
        m_currentProto = -1;
        m_protoStack = new Proto [16];
        final String hackDeMarc = filename;
        if (filename.indexOf ("http://") != -1) {
            new Thread () {
                public void run () {
                    m_file = new File (hackDeMarc);
                    setState (Loadable.LOADING);
                    if (m_file.isInCache () == false) { // not in cache force read all bytes to store it
                        byte [] data = m_file.readAllBytes ();
                        m_file.close (Loadable.CLOSED);
                        m_file = new File (data);
                    }
                    while (decodeMagic ());
                    m_file.close (Loadable.LOADED);
                    MiniPlayer.wakeUpCanvas();
                }
            }.start ();
        } else {
            m_file = new File (filename);
            setState (Loadable.LOADING);
            while (decodeMagic ());
            m_file.close (Loadable.LOADED);
        }
    }

    boolean setLocale (String name) {
        if (name == null) {
            return false;
        } else if (name.equals(m_locale)) {
            return true; // locale already loaded
        }
        byte [] data = getLocaleData (name);
        if (data == null) {
            return false;
        }
        m_locale = name;
        DataInputStream dis = new DataInputStream (new ByteArrayInputStream (data));
        int size = readShort (dis);
        m_localeTable = new String [size];
        for (int i = 0; i < size; i++) {
            int idx = readShort (dis);
            String label = readString (dis);
            if (idx < size) {
                m_localeTable[idx] = label;
            } else {
                Logger.println ("LocalManager: index out of range: "+idx);
            }
        }
        return true;
    }

    Proto getCurrentProto () {
        //Logger.println ("getCurrentProto: @"+m_currentProto+" = "+m_protoStack [m_currentProto]);
        return (m_currentProto < 0 ? null : m_protoStack [m_currentProto]); 
    }
    void pushProto (Proto proto) {
        if (m_currentProto < 15) {
            m_protoStack [++m_currentProto] = proto;
        }
    }
    void popProto () {
        if (m_currentProto > 0) {
            m_protoStack[m_currentProto] = null;
            m_currentProto--;
        }
    }

    synchronized void setState (int state) { m_state = state; }
    synchronized public int getState () { return m_state; }

    synchronized public int getDuration () { return m_file.getLen (); }
    synchronized public int getCurrent () { return m_file.getCurrent (); }

    static DataInputStream getResStream (String name) {
        try {
            InputStream is = Class.forName ("memoplayer.Decoder").getResourceAsStream ("/"+name);
            if(is != null) {
                return new DataInputStream (is);
            }
        } catch (Exception e) {
            Logger.println ("FATAL ERROR: Cannot open mandatory file '"+name+"'");
        }
        return null;
    }


//     DataInputStream getStream (String url) {
//         if (m_file != null) {
//             m_file.close (Loadable.OK);
//         }
//         m_file = new File (url);
//     }

    private boolean decodeChunk (int chunkType) {
        String name = m_file.readString ();
        int size = m_file.readInt ();
        //Logger.println ("Decoder.decodeChunk sleeping "+(size/10));
        //try {Thread.sleep (size/10); } catch (Exception e) {}
        //Logger.println ("Decoder("+this+").decodeChunk name '"+name+"' / "+size);
        if (size > 0) {
            try {
                m_dataLink = new DataLink (name, m_file.readBytes (size), chunkType, m_dataLink);
            } catch (Exception e) {
                Logger.println ("Error: cannot read chunk "+name+"/"+size);
                Logger.println ("Exception caught was "+e);
                m_file.close (Loadable.ERROR);
                setState (Loadable.ERROR);
                return false;
            }
        }
        //m_current += size + 8 + name.length ()+1;
        //Logger.println ("decodeChunk: "+m_current+" / "+m_len);
        return true;
    }

    private boolean decodeMagic () {
        int magic = m_file.readInt ();
        //Logger.println (">>>> Decoder.decode: got magic: "+Integer.toHexString(magic));
        switch (magic) {
        case MAGIC_SCENE:
            return decodeChunk (MAGIC_SCENE);
        case MAGIC_IMAGE:
            return decodeChunk (MAGIC_IMAGE);
        case MAGIC_SCRIPT:
            return decodeChunk (MAGIC_SCRIPT);
        case MAGIC_PROTO:
            return decodeChunk (MAGIC_PROTO);
        case MAGIC_LOCALE:
            return decodeChunk (MAGIC_LOCALE);
        case MAGIC_CSS:
            return decodeChunk (MAGIC_CSS);
        case MAGIC_FONT:
            return decodeChunk (MAGIC_FONT);
        case MAGIC_BML:
            return decodeChunk (MAGIC_BML);
        case MAGIC_MMEDIA:
            return decodeChunk (MAGIC_MMEDIA);
        case MAGIC_END:
            //m_current += 4;
            m_file.close (Loadable.LOADED);
            setState (Loadable.LOADED);
            return (false);
        default:
            Logger.println ("ERROR: bad magic: "+Integer.toHexString(magic));
            m_file.close (Loadable.ERROR);
            setState (Loadable.ERROR);
            return (false);
        }
    }
    
    // Extract a given chunk from an M4M stream
    public static byte[] extractChunck (byte[] data, String targetName) {
        StringBuffer sb = new StringBuffer ();
        DataInputStream dis = new DataInputStream (new ByteArrayInputStream (data));
        try {
            int magic = dis.readInt();
            while (magic != MAGIC_END) {
               readString (dis, sb);
               if (targetName.equals (sb.toString())) {
                   byte[] chunk = new byte [dis.readInt ()];
                   dis.read (chunk);
                   return chunk;
               }
               dis.skip (dis.readInt()); // skip data chunk
               magic = dis.readInt();
            }
        } catch (Exception e) {
        } finally {
            if (dis != null) {
                try { dis.close(); } catch (Exception e2) {}
                dis = null;
            }
        }
        return null;
    }
    
    byte [] getLocaleData (String name) {
        DataLink dl = DataLink.find (m_dataLink, name);
        return dl != null ? dl.getLocale () : null;
    }

    byte [] getSceneData (String name) {
        DataLink dl = null;
        if (m_dataLink != null) {
            String shortName = name;
            int idx= name.lastIndexOf ('/');
            if (idx >= 0) {
                shortName = name.substring (idx+1);;
            }
            dl = DataLink.find (m_dataLink, shortName);
            if (dl == null && (name.startsWith("http://") || name.startsWith("file://")) ) {
                dl = DataLink.findLast (m_dataLink, MAGIC_SCENE); 
            }
            if (dl == null) {
                dl = DataLink.find (m_dataLink, name);
            }
        }
        return (dl != null ) ? dl.getScene () : null;
    }

    boolean getScene (String name, Scene scene) {
        //long t = System.currentTimeMillis();
        byte [] data = getSceneData (name);

        if (data != null) {
            DataInputStream is = new DataInputStream (new ByteArrayInputStream (data));
            Node [] table = new Node [256];
            scene.m_node = Node.decode (is, table, this);
            Route.decode (is, table);
            //Logger.println ("Decoder.getScene: "+name+" in " + (System.currentTimeMillis() - t) + "ms");    
            return true;
        }
        Logger.println ("Decoder.getScene: "+name+" => no data in :" + m_dataLink+" for "+name);
        return false;
    }

    String getCssData (String name) {
        DataLink dl = DataLink.find (m_dataLink, name);
        return dl == null ? null : dl.getCss ();
    }

    byte [] getFontDesc (String name) {
        DataLink dl = DataLink.find (m_dataLink, name);
        return dl == null ? null : dl.getFontDesc ();
    }

//    final byte [] getScript (String name) { 
//        DataLink dl = DataLink.find (m_dataLink, name);
//        return dl == null ? null : dl.getScript ();
//    }

    final DataLink getDataLink (String name) {
        return DataLink.find (m_dataLink, name);
    }

    final Image getImage (String name) {
        DataLink dl = DataLink.find (m_dataLink, name);
        return dl == null ? null : dl.getImage ();
    }

    final byte[] getBml (String name) {
        DataLink dl = DataLink.find (m_dataLink, name);
        return dl == null ? null : dl.getBml ();
    }
    
    final byte[] getMMedia (String name) {
        DataLink dl = DataLink.find (m_dataLink, name);
        return dl == null ? null : dl.getMMedia ();
    }
    
    final boolean checkImage (String name) {
        return DataLink.find (m_dataLink, name) != null;
    }

    final Proto createProto (String name, Node[] iTable, DataInputStream iis) {
        DataLink dl = DataLink.find (m_dataLink, name);
        byte [] data =  dl == null ? null : dl.getProto ();
        if (data != null) {
            DataInputStream is = new DataInputStream (new ByteArrayInputStream (data));
            //Logger.println ("decoding proto "+name);
            Proto proto = new Proto ();
            pushProto  (proto);
            Node[] table = new Node [256];
            // read fields
            proto.read (is, table, this);
            // read instance fields
            popProto ();
            proto.read (iis, iTable, this);
            pushProto  (proto);
            //Logger.println ("proto "+name+ " reading body");
            Node tmp = Node.decode (is, table, this);
            while (tmp != null) {
                proto.addNode (tmp);
                tmp = Node.decode (is, table, this);
            }
            Route.decode (is, table);
            popProto ();
            //Logger.println ("proto "+name+ " decoded");
            return proto;
        }
        return (null);
    }
    
    /**
     * Load a byte array as a new Datalink of this decoder
     */
    public void addData(String name, byte[] data, int type, boolean force) {
        // Find if already loaded in decoder...
        DataLink previous = DataLink.find (m_dataLink, name);
        
        if (!force && previous != null) {
            return;
        }
        
        // Add to DataLink list
        if(previous == null) {
            m_dataLink = new DataLink (name, data, type, m_dataLink);
        } else {
            previous.m_data = data;
            //previous.m_image = null;
        }
    }
    
    // static and generic methods

    static int readInt (DataInputStream dis) {
        try { return dis.readInt ();} catch (Exception e) { return (0); }
    } 

    static int readUnsignedByte (DataInputStream dis) {
        try {
            return dis.readUnsignedByte ();
        } catch (Exception e) {
            return (0);
        }
    } 

    static int readShort (DataInputStream dis) {
        return readUnsignedByte (dis)*256 + readUnsignedByte (dis);
    } 

    static String readString (DataInputStream dis) {
        return readChars (dis,  (byte)0,  (byte)0);
    } 

    String readLocaleString (DataInputStream dis) {
        readChars (dis,  (byte)0,  (byte)0, s_sb, m_localeTable);
        return s_sb.toString();
    } 

    static String readLine (DataInputStream dis) {
        return readChars (dis, (byte)'\r',  (byte)'\n');
    }

    
    /*static String readChars (DataInputStream dis, byte s1, byte s2) {
        s_sb.setLength (0);
        try {
            int b = dis.readUnsignedByte ();
            while (b != s1 && b != s2) {
                s_sb.append ((char)b);
                b = dis.readUnsignedByte ();
            }
        } catch (Exception e) {
            //Logger.println ("Decoder.readChars: read error");
            return null;
        }
        return (s_sb.toString ());
    }*/
    
    
    static String readChars (DataInputStream dis, byte s1, byte s2) {
        readChars (dis, s1,  s2, s_sb, null);
        return s_sb.toString();
    }
    
    static int readString (DataInputStream dis, StringBuffer ret) {
        return readChars (dis,  (byte)0,  (byte)0, ret, null);
    }
    
    static int readLine (DataInputStream dis, StringBuffer sb) {
        return readChars (dis, (byte)'\r',  (byte)'\n', sb, null);
    }
    
    static int readChars (DataInputStream dis, byte s1, byte s2, StringBuffer ret, String [] localeTable) {
        ret.setLength (0);

        int read = 0; 
        try {
            byte b = dis.readByte (); read++; 
            if (b == -1) { // -1 is 255 so we have a localized String so the next 2 bytes are the index in the locale table
                int idx = readShort (dis);
                b = dis.readByte (); 
                if (b != 0) {
                    Logger.println ("Error localized String index not terminated by 0 marker");
                }
                try {
                    ret.append (localeTable[idx]);
                } catch (Exception e) {
                    ret.append ("#undef#");
                }
                return ret.length ();
            }
            while (b != s1 && b != s2) {
                read += readCharUtf8 (b, ret, dis);
                b = dis.readByte (); read++;
            } 
            //RCA 121107
             if (b == s1 && b != (byte)0) { // try to eat the second char because it is \n
                b = dis.readByte (); read++;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.println ("Decoder.readChars: read error"+e+", input stream is "+dis);
            return 0;
        }
        return read;
    } 
    
    static int readCharUtf8(byte b, StringBuffer ret, DataInputStream dis) throws IOException {
        int count = 0;
        char c = 0;
        if ((b & 0x80) == 0) {
            c = (char) b;
        } else if ((b & 0xe0) == 0xc0) {        // 11100000
            c |= (b & 0x1f) << 6;               // 00011111
            c |= (dis.readByte() & 0x3f) << 0;  // 00111111
            count = 1;
        } else if ((b & 0xf0) == 0xe0) {        // 11110000
            c |= (b & 0x0f) << 12;              // 00001111
            c |= (dis.readByte() & 0x3f) << 6;  // 00111111
            c |= (dis.readByte() & 0x3f) << 0;  // 00111111    
            count = 2;
        } else if ((b & 0xf8) == 0xf0) {        // 11111000
            c |= (b & 0x07) << 18;              // 00000111 (move 18, not 16?)
            c |= (dis.readByte() & 0x3f) << 12; // 00111111
            c |= (dis.readByte() & 0x3f) << 6;  // 00111111
            c |= (dis.readByte() & 0x3f) << 0;  // 00111111
            count = 3;
        } else {
            c = '?';
        }
        ret.append(c);
        return count;
    }
}
