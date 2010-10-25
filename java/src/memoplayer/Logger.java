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
import javax.microedition.lcdui.*;
import javax.microedition.rms.RecordStore;

public class Logger {
    public final static String s_version = "1.4.4";
    private final static int MAX_MESSAGES = 100;
    private static String [] s_messages;
    private static int s_count, s_cw, s_ch, s_offset, s_startRaw;
    private static Font s_font; 
    private static int s_maxLines;
    private static int s_firstLine = 0;
    private static int s_nbLines = 0;
    private static int s_touchBar = -1; // -1: first time, display help
    private static boolean s_fastScroll = false;

    private static long s_startTime = System.currentTimeMillis() ;
//#ifdef profiling
    private static long [] s_timeStack;
    private static int s_currentSlot = 0;

    public synchronized static void profile (String s) {
        println ("[T:"+(System.currentTimeMillis() - s_startTime)+"]"+s);
    }

    public synchronized static void profileStart () {
        s_timeStack[s_currentSlot++] = System.currentTimeMillis();
    }

    public synchronized static void profileEnd (String s) {
        println ("[D:"+(System.currentTimeMillis()-s_timeStack[--s_currentSlot])+"] "+s);
    }

//#endif
 
    static {
        s_messages = new String [MAX_MESSAGES+1];
        s_font = Font.getFont (Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_SMALL); 
        s_cw = s_font.charWidth ('W');
        s_ch = s_font.getHeight ();
        println ("MeMo player V"+s_version);
//#ifdef profiling
        s_timeStack = new long [128];
//#endif

//#ifdef ulow.preload        
        setKey ('2');
        println ("Preloading classes");
        try { Class.forName ("memoplayer.Scene"); } catch (Throwable e) { Logger.println ("Cannot load Scene"); }
        try { Class.forName ("memoplayer.Activable"); } catch (Throwable t) { Logger.println ("cannot load Activable"); }
        try { Class.forName ("memoplayer.Anchor"); } catch (Throwable t) { Logger.println ("cannot load Anchor"); }
        try { Class.forName ("memoplayer.Appearance"); } catch (Throwable t) { Logger.println ("cannot load Appearance"); }
        try { Class.forName ("memoplayer.AppearanceContext"); } catch (Throwable t) { Logger.println ("cannot load AppearanceContext"); }
        try { Class.forName ("memoplayer.AudioClip"); } catch (Throwable t) { Logger.println ("cannot load AudioClip"); }
        try { Class.forName ("memoplayer.BaseReader"); } catch (Throwable t) { Logger.println ("cannot load BaseReader"); }
        try { Class.forName ("memoplayer.Bitmap"); } catch (Throwable t) { Logger.println ("cannot load Bitmap"); }
        try { Class.forName ("memoplayer.ByteCode"); } catch (Throwable t) { Logger.println ("cannot load ByteCode"); }
        try { Class.forName ("memoplayer.CSSReader"); } catch (Throwable t) { Logger.println ("cannot load CSSReader"); }
        try { Class.forName ("memoplayer.CacheManager"); } catch (Throwable t) { Logger.println ("cannot load CacheManager"); }
        try { Class.forName ("memoplayer.Circle"); } catch (Throwable t) { Logger.println ("cannot load Circle"); }
        try { Class.forName ("memoplayer.Color"); } catch (Throwable t) { Logger.println ("cannot load Color"); }
        try { Class.forName ("memoplayer.ColorInterpolator"); } catch (Throwable t) { Logger.println ("cannot load ColorInterpolator"); }
        try { Class.forName ("memoplayer.CompositeTexture2D"); } catch (Throwable t) { Logger.println ("cannot load CompositeTexture2D"); }
        try { Class.forName ("memoplayer.Context"); } catch (Throwable t) { Logger.println ("cannot load Context"); }
        try { Class.forName ("memoplayer.CookyManager"); } catch (Throwable t) { Logger.println ("cannot load CookyManager"); }
        try { Class.forName ("memoplayer.Coordinate2D"); } catch (Throwable t) { Logger.println ("cannot load Coordinate2D"); }
        try { Class.forName ("memoplayer.CoordinateInterpolator2D"); } catch (Throwable t) { Logger.println ("cannot load CoordinateInterpolator2D"); }
        try { Class.forName ("memoplayer.DataLink"); } catch (Throwable t) { Logger.println ("cannot load DataLink"); }
        try { Class.forName ("memoplayer.DataLoader"); } catch (Throwable t) { Logger.println ("cannot load DataLoader"); }
        try { Class.forName ("memoplayer.Decoder"); } catch (Throwable t) { Logger.println ("cannot load Decoder"); }
        try { Class.forName ("memoplayer.Event"); } catch (Throwable t) { Logger.println ("cannot load Event"); }
        try { Class.forName ("memoplayer.ExternCall"); } catch (Throwable t) { Logger.println ("cannot load ExternCall"); }
        try { Class.forName ("memoplayer.ExternFont"); } catch (Throwable t) { Logger.println ("cannot load ExternFont"); }
        try { Class.forName ("memoplayer.Field"); } catch (Throwable t) { Logger.println ("cannot load Field"); }
        try { Class.forName ("memoplayer.File"); } catch (Throwable t) { Logger.println ("cannot load File"); }
        try { Class.forName ("memoplayer.FileQueue"); } catch (Throwable t) { Logger.println ("cannot load FileQueue"); }
        try { Class.forName ("memoplayer.FixFloat"); } catch (Throwable t) { Logger.println ("cannot load FixFloat"); }
        try { Class.forName ("memoplayer.FontStyle"); } catch (Throwable t) { Logger.println ("cannot load FontStyle"); }
        try { Class.forName ("memoplayer.JsonReader"); } catch (Throwable t) { Logger.println ("Cannot load JsonReader"); }
        try { Class.forName ("memoplayer.JsonTask"); } catch (Throwable t) { Logger.println ("Cannot load JsonTask"); }
        try { Class.forName ("memoplayer.KeySensor"); } catch (Throwable t) { Logger.println ("Cannot load KeySensor"); }
        try { Class.forName ("memoplayer.Layer2D"); } catch (Throwable t) { Logger.println ("Cannot load Layer2D"); }
        try { Class.forName ("memoplayer.LcdUI"); } catch (Throwable t) { Logger.println ("Cannot load LcdUI"); }
        try { Class.forName ("memoplayer.Loadable"); } catch (Throwable t) { Logger.println ("Cannot load Loadable"); }
        try { Class.forName ("memoplayer.Logger"); } catch (Throwable t) { Logger.println ("Cannot load Logger"); }
        try { Class.forName ("memoplayer.MFColor"); } catch (Throwable t) { Logger.println ("Cannot load MFColor"); }
        try { Class.forName ("memoplayer.MFFloat"); } catch (Throwable t) { Logger.println ("Cannot load MFFloat"); }
        try { Class.forName ("memoplayer.MFFloatBase"); } catch (Throwable t) { Logger.println ("Cannot load MFFloatBase"); }
        try { Class.forName ("memoplayer.MFInt32"); } catch (Throwable t) { Logger.println ("Cannot load MFInt32"); }
        try { Class.forName ("memoplayer.MFNode"); } catch (Throwable t) { Logger.println ("Cannot load MFNode"); }
        try { Class.forName ("memoplayer.MFRotation"); } catch (Throwable t) { Logger.println ("Cannot load MFRotation"); }
        try { Class.forName ("memoplayer.MFString"); } catch (Throwable t) { Logger.println ("Cannot load MFString"); }
        try { Class.forName ("memoplayer.MFVec2f"); } catch (Throwable t) { Logger.println ("Cannot load MFVec2f"); }
        try { Class.forName ("memoplayer.MFVec3f"); } catch (Throwable t) { Logger.println ("Cannot load MFVec3f"); }
        try { Class.forName ("memoplayer.Machine"); } catch (Throwable t) { Logger.println ("Cannot load Machine"); }
        try { Class.forName ("memoplayer.Material2D"); } catch (Throwable t) { Logger.println ("Cannot load Material2D"); }
        try { Class.forName ("memoplayer.Matrix"); } catch (Throwable t) { Logger.println ("Cannot load Matrix"); }
        try { Class.forName ("memoplayer.MediaControl"); } catch (Throwable t) { Logger.println ("Cannot load MediaControl"); }
        try { Class.forName ("memoplayer.MediaNode"); } catch (Throwable t) { Logger.println ("Cannot load MediaNode"); }
        try { Class.forName ("memoplayer.MediaObject"); } catch (Throwable t) { Logger.println ("Cannot load MediaObject"); }
        try { Class.forName ("memoplayer.MediaSensor"); } catch (Throwable t) { Logger.println ("Cannot load MediaSensor"); }
        try { Class.forName ("memoplayer.Message"); } catch (Throwable t) { Logger.println ("Cannot load Message"); }
        try { Class.forName ("memoplayer.MiniPlayer"); } catch (Throwable t) { Logger.println ("Cannot load MiniPlayer"); }
        try { Class.forName ("memoplayer.MotionSensor"); } catch (Throwable t) { Logger.println ("Cannot load MotionSensor"); }
        try { Class.forName ("memoplayer.MovieRotator"); } catch (Throwable t) { Logger.println ("Cannot load MovieRotator"); }
        try { Class.forName ("memoplayer.MovieTexture"); } catch (Throwable t) { Logger.println ("Cannot load MovieTexture"); }
        try { Class.forName ("memoplayer.MyCanvas"); } catch (Throwable t) { Logger.println ("Cannot load MyCanvas"); }
        try { Class.forName ("memoplayer.Namespace"); } catch (Throwable t) { Logger.println ("Cannot load Namespace"); }
        try { Class.forName ("memoplayer.Node"); } catch (Throwable t) { Logger.println ("Cannot load Node"); }
        try { Class.forName ("memoplayer.NodeTable"); } catch (Throwable t) { Logger.println ("Cannot load NodeTable"); }
        try { Class.forName ("memoplayer.ObjLink"); } catch (Throwable t) { Logger.println ("Cannot load ObjLink"); }
        try { Class.forName ("memoplayer.Observer"); } catch (Throwable t) { Logger.println ("Cannot load Observer"); }
        try { Class.forName ("memoplayer.OrderedGroup"); } catch (Throwable t) { Logger.println ("Cannot load OrderedGroup"); }
        try { Class.forName ("memoplayer.PositionInterpolator2D"); } catch (Throwable t) { Logger.println ("Cannot load PositionInterpolator2D"); }
        try { Class.forName ("memoplayer.Proto"); } catch (Throwable t) { Logger.println ("Cannot load Proto"); }
        try { Class.forName ("memoplayer.RecordTexture"); } catch (Throwable t) { Logger.println ("Cannot load RecordTexture"); }
        try { Class.forName ("memoplayer.Rectangle"); } catch (Throwable t) { Logger.println ("Cannot load Rectangle"); }
        try { Class.forName ("memoplayer.Region"); } catch (Throwable t) { Logger.println ("Cannot load Region"); }
        try { Class.forName ("memoplayer.Register"); } catch (Throwable t) { Logger.println ("Cannot load Register"); }
        try { Class.forName ("memoplayer.RichText"); } catch (Throwable t) { Logger.println ("Cannot load RichText"); }
        try { Class.forName ("memoplayer.Route"); } catch (Throwable t) { Logger.println ("Cannot load Route"); }
        try { Class.forName ("memoplayer.SFBool"); } catch (Throwable t) { Logger.println ("Cannot load SFBool"); }
        try { Class.forName ("memoplayer.SFColor"); } catch (Throwable t) { Logger.println ("Cannot load SFColor"); }
        try { Class.forName ("memoplayer.SFFloat"); } catch (Throwable t) { Logger.println ("Cannot load SFFloat"); }
        try { Class.forName ("memoplayer.SFInt32"); } catch (Throwable t) { Logger.println ("Cannot load SFInt32"); }
        try { Class.forName ("memoplayer.SFNode"); } catch (Throwable t) { Logger.println ("Cannot load SFNode"); }
        try { Class.forName ("memoplayer.SFRotation"); } catch (Throwable t) { Logger.println ("Cannot load SFRotation"); }
        try { Class.forName ("memoplayer.SFString"); } catch (Throwable t) { Logger.println ("Cannot load SFString"); }
        try { Class.forName ("memoplayer.SFTime"); } catch (Throwable t) { Logger.println ("Cannot load SFTime"); }
        try { Class.forName ("memoplayer.SFVec2f"); } catch (Throwable t) { Logger.println ("Cannot load SFVec2f"); }
        try { Class.forName ("memoplayer.SFVec3f"); } catch (Throwable t) { Logger.println ("Cannot load SFVec3f"); }
        try { Class.forName ("memoplayer.ScalarInterpolator"); } catch (Throwable t) { Logger.println ("Cannot load ScalarInterpolator"); }
        try { Class.forName ("memoplayer.Scene"); } catch (Throwable t) { Logger.println ("Cannot load Scene"); }
        try { Class.forName ("memoplayer.Script"); } catch (Throwable t) { Logger.println ("Cannot load Script"); }
        try { Class.forName ("memoplayer.ScriptAccess"); } catch (Throwable t) { Logger.println ("Cannot load ScriptAccess"); }
        try { Class.forName ("memoplayer.Shape"); } catch (Throwable t) { Logger.println ("Cannot load Shape"); }
        try { Class.forName ("memoplayer.SmartHttpConnection"); } catch (Throwable t) { Logger.println ("Cannot load SmartHttpConnection"); }
        try { Class.forName ("memoplayer.SmartHttpCookies"); } catch (Throwable t) { Logger.println ("Cannot load SmartHttpCookies"); }
        try { Class.forName ("memoplayer.Sound2D"); } catch (Throwable t) { Logger.println ("Cannot load Sound2D"); }
        try { Class.forName ("memoplayer.Style"); } catch (Throwable t) { Logger.println ("Cannot load Style"); }
        try { Class.forName ("memoplayer.Switch"); } catch (Throwable t) { Logger.println ("Cannot load Switch"); }
        try { Class.forName ("memoplayer.Task"); } catch (Throwable t) { Logger.println ("Cannot load Task"); }
        try { Class.forName ("memoplayer.TaskListenerCb"); } catch (Throwable t) { Logger.println ("Cannot load TaskListenerCb"); }
        try { Class.forName ("memoplayer.TaskQueue"); } catch (Throwable t) { Logger.println ("Cannot load TaskQueue"); }
        try { Class.forName ("memoplayer.Text"); } catch (Throwable t) { Logger.println ("Cannot load Text"); }
        try { Class.forName ("memoplayer.TimeSensor"); } catch (Throwable t) { Logger.println ("Cannot load TimeSensor"); }
        try { Class.forName ("memoplayer.TouchSensor"); } catch (Throwable t) { Logger.println ("Cannot load TouchSensor"); }
        try { Class.forName ("memoplayer.Traffic"); } catch (Throwable t) { Logger.println ("Cannot load Traffic"); }
        try { Class.forName ("memoplayer.Transform2D"); } catch (Throwable t) { Logger.println ("Cannot load Transform2D"); }
        try { Class.forName ("memoplayer.Upload"); } catch (Throwable t) { Logger.println ("Cannot load Upload"); }
        try { Class.forName ("memoplayer.WrapText"); } catch (Throwable t) { Logger.println ("Cannot load WrapText"); }
        try { Class.forName ("memoplayer.XmlReader"); } catch (Throwable t) { Logger.println ("Cannot load XmlReader"); }
        setKey ('2');
//#endif        
    }
 
    public static void setKey (int c) {
        int step;
        switch (c) {
//#ifdef ulow
        case '8' : // hack because of the meditek sample taht has a broken up key
//#endif
        case 'U' :
            s_offset += s_fastScroll ? 10 : 1;// -= s_offset > 0 ? 1 : 0;
            break;
        case 'D' :
            step = s_fastScroll ? 10 : 1;
            s_offset -= s_offset < step ? s_offset : step;
            break;
        case 'L' :
            step = s_fastScroll ? 20 : 5;
            s_startRaw -= s_startRaw > step ? step : s_startRaw;
            break;
        case 'R' :
            s_startRaw += s_fastScroll ? 20 : 5;
            break;
        case '4' :
            s_fastScroll = ! s_fastScroll;
            Logger.println("Fast scrolling: "+(s_fastScroll ? "enabled" : "disabled"));
            break;
        case '5' :
            MyCanvas.s_debugClip = ! MyCanvas.s_debugClip;
            Logger.println("Clip debug: "+(MyCanvas.s_debugClip ? "enabled" : "disabled"));
            break;
        case '6' :
            Logger.println ("RMS ------------------");
            String[] rms = RecordStore.listRecordStores();
            if (rms != null && rms.length > 0) {
                int totalSize = 0, available = -1;
                StringBuffer sb = new StringBuffer ();
                RecordStore rs = null;
                for (int i = 0; i < rms.length; i++) {
                    sb.append(i);
                    sb.append(": ");
                    sb.append(rms[i]);
                    sb.append(": ");
                    try {
                        rs = RecordStore.openRecordStore(rms[i], false);
                        if (rs != null) {
                            int size = rs.getSize();
                            if (available == -1) {
                                available = rs.getSizeAvailable();
                            }
                            totalSize += size;
                            sb.append(rs.getNumRecords());
                            sb.append(" / ");
                            sb.append (size);
                            rs.closeRecordStore();
                        }
                    } catch (Exception e) {
                        sb.append("NULL");
                    }
                    rs = null;
                    Logger.println (sb.toString());
                    sb.setLength(0);
                    
                }
                Logger.println  ("Used size: "+totalSize);
                Logger.println ("Available size: "+available);
            }
            Logger.println ("RMS ------------------");
        case '7' :
            s_startRaw = 0;
            break;
        case '9' :
            int maxi = 0;
            for (int i = 0; i < s_nbLines; i++) {
                int l = s_messages[i+s_firstLine].length ();
                if (l > maxi) { maxi = l; }
            }
            s_startRaw = maxi - 20;
            break;
        case 'E' :
            s_startRaw = s_offset = 0;
            break;
        case 'Z' :
            s_startRaw = s_offset = 0;
            for (int i = 0; i < s_count; i++) {
                s_messages[i] = null;
            }
            s_count = 0;
            break;
        case '0' :
            MyCanvas.s_pauseIndicator = ! MyCanvas.s_pauseIndicator;
            Logger.println("Pause indicator: "+(MyCanvas.s_pauseIndicator ? "enabled" : "disabled"));
            break;
        case '1' :
            if (MyCanvas.s_memBar>0) {
                Logger.println("Membar: disabled");
                MyCanvas.s_memBar=-1;
            } else {
                Logger.println("Membar: enabled");
                MyCanvas.s_memBar=1;
            }
            break;
        case '2' :
            Logger.println ("Running gc...");
            System.gc ();
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            Runtime rt = Runtime.getRuntime ();
//#ifdef ulow.preload
            Logger.println ("MEM: used "+(rt.totalMemory() - rt.freeMemory())+", free "+rt.freeMemory ());
//#else
            Logger.println ("FreeMem: "+rt.freeMemory()+", total: "+rt.totalMemory ());
//#endif
            break;
        case '3' :
//#ifdef api.mm
            MediaObject.printInfo ();
            MediaObject.printCapabilities ();
//#endif
            break;
        case '#' :
            Logger.println ("MeMo player "+s_version);
            Logger.println ("Help: ");
            Logger.println ("  0: Enable/disable pause indicator");
            Logger.println ("  1: Enable/disable mem bar");
            Logger.println ("  2: run gc & print free mem");
            Logger.println ("  3: Print media infos");
            Logger.println ("  4: Enable/disable fast scroll");
            Logger.println ("  5: Enable/disable clip debug");
            Logger.println ("  6: Print RMS records infos");
            Logger.println ("  7/9: start/end of lines");
            Logger.println ("  Pad: scroll");
            Logger.println ("  C: Clear console");
            Logger.println ("  Fire: go to top");
            Logger.println ("  *: Exit console");
            break;
        case 'C' :
            System.exit (1);
            break;
        }
    }
    
    public static boolean setPointer (int x, int y, int w, int h) {
        int H = h/10;
        int h3 = h/3, w3 = w/3;
        if (s_touchBar>0) {
            if (y>h-H) {
                Logger.setKey('0'+(x*10/w));
            } else if (y<h3) {
                Logger.setKey ('U');
            } else if (y>h-h3-H) {
                Logger.setKey ('D');
            } else if (x < w3) {
                Logger.setKey ('L');
            } else if (x > w-w3) {
                Logger.setKey ('R');
            } else {
                s_touchBar = 0; // disable
                return false;
            }
        } else {
            if (s_touchBar == -1) { // first time, display help
                Logger.setKey('#');
            }
            s_touchBar = 1; // enable
        }
        return true;
    }

    public synchronized static void println (String s) {
        if (s_count == MAX_MESSAGES) {
            System.arraycopy (s_messages, 1, s_messages, 0, MAX_MESSAGES-1);
            s_messages[--s_count] = null;
        }
        print (s);
        System.out.println ();
        s_count++;
    }

    public synchronized static void timePrintln (String s) {
//#ifdef profiling
        println (">>["+(System.currentTimeMillis() - s_startTime)+"] "+s);
//#else
        println (">> "+s);
//#endif
    }

    public synchronized static void print (String s) {
        if (s_messages[s_count] == null) {
            s_messages[s_count] = s;
        } else {
            s_messages[s_count] += s;
        }
        System.out.print (s);
    }
    
    public static void show (Graphics g, int w, int h) {
        if (g == null) return;
        g.setColor (0);
        g.fillRect (0, 0, w, h);
        if (s_touchBar>0) {
            int h2 = h/2, w2 = w/2, H = h/10, y = h - H, W = w/10, W2 = W/2, yH2 = y + H/2;
            g.setColor(0x440000);
            g.fillTriangle(0, h2, 10, h2+10, 10, h2-10);
            g.fillTriangle(w, h2, w-10, h2+10, w-10, h2-10);
            g.fillTriangle(w2, 0, w2-10, 10, w2+10, 10);
            g.fillTriangle(w2, y, w2-10, y-10, w2+10, y-10);
            g.setColor(0xaa0000);
            g.setFont(s_font);
            for (int i=0; i<10;i++) {
                int x = i*W+1;
                g.drawRect(x, y, W-2, H);
                g.drawChar((char)('0'+i), x+W2, yH2, g.BASELINE|g.HCENTER);
            }
            h -= H;
        }
        g.setColor (0X00AA00);
        g.setFont (s_font);
        s_maxLines = h / s_ch;
        s_firstLine = 0;
        if (s_count > s_maxLines) {
            s_firstLine = s_count - s_maxLines - s_offset;
        } else {
            s_firstLine = s_offset;
        }
        if (s_firstLine < 0) {
            s_firstLine = 0;
        }
        s_nbLines = Math.min (s_maxLines, s_count-s_firstLine);
        for (int i = 0; i < s_nbLines; i++) {
            String s = s_messages[i+s_firstLine];
            if (s != null) {
                int l = s.length ()-s_startRaw;
                if (l > 0) {
                    g.drawSubstring (s, s_startRaw, l, 0, i*s_ch, g.TOP|g.LEFT);
                }
            }
        }
        
    }
}
