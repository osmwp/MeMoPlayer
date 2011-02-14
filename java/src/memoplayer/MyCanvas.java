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
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.TextBox;
import javax.microedition.midlet.MIDlet;
//#else
import net.rim.device.api.ui.UiApplication;
//#ifdef BlackBerry.Signed
import net.rim.device.api.system.Display;
//#endif
//#ifdef BlackBerry.Touch
import net.rim.device.api.ui.TouchEvent;
//#endif
//#endif

import java.io.DataInputStream;

// according to platform, use the coressponding inheritance
//#ifndef BlackBerry
public class MyCanvas extends javax.microedition.lcdui.Canvas implements Runnable {
//#else
public class MyCanvas extends net.rim.device.api.ui.container.MainScreen
//#ifndef BlackBerryTouch
     implements Runnable
//#else 
     implements Runnable,net.rim.device.api.system.AccelerometerListener
//#endif        
{
//#endif

    public final static int SLEEP_TRESHOLD = 500; // Duration (in ms) under which sleep will not occur    
    public final static int SLEEP_FOREVER = Integer.MAX_VALUE;
    public final static int SLEEP_CANCELED  = -1;
    private final static int PAINT_NONE  = 0;
    private final static int PAINT_SCENE = 1;
    private final static int PAINT_LOADER  = 2;
    private final static int PAINT_CONSOLE  = 3;
    private final static int PAINT_PAUSE  = 4;
    private final static int PAINT_UNPAUSE  = 5;
    private final static int PAINT_MEMBAR  = 6;
    private final static int PAINT_WHITE  = 7;

    public final static String s_platform = System.getProperty("microedition.platform");
    
    // During screen resize, paint a white screen during 3 compose/render cycles.
    // First is to notify the UI, second is to let UI scale, third for security
    private final static int RESIZE_CYCLES = 3;
    
//#ifndef BlackBerry
    private MIDlet midlet;
//#else
    private UiApplication uiApp;
//#ifdef BlackBerry.Touch
    private int m_accOrientation=-1;
//#ifdef BlackBerry.Signed
    private int m_orientation;
//#endif
//#endif
//#endif

    private int m_width, m_height;    
    boolean forceUpdate;
    private Object m_updateLock = new Object(); // locks access to forceUpdate
    
    String sceneName; // the name of  the current scene playing
    StringBuffer msg = new StringBuffer (); // for the loading message

//#ifdef debug.console
    private int m_debugStep;
//#endif
    static int s_memBar = 0, s_cpuBar = 0;
    static boolean s_pauseIndicator = false;
    static boolean s_debugClip = false;
    // ignore size change notification, ie Samsung Croy GT-S3100
    static boolean s_bIgnoreSizeChange = false;

    private Context context;
    Thread thread;
    public boolean isHidden = false;
    public static boolean composeAgain = false;

    int m_paintType = PAINT_NONE;
    static Object s_paintLock = new Object (); // locks access to m_paintType & render nodes
    int m_resizeCycle = RESIZE_CYCLES; // counter for white screen

    Decoder decoder;
    Scene scene;
    KeyboardHandler m_keyboardHandler;
    
    private Event m_event, m_lastEvent = new Event();
    private static Event s_rootEvent;

    public boolean jp8=false;

    Region r = new Region (); // temporary region used when video is shown
//#ifdef MM.overlay
    Image m_backBuf; // used to speed up overlay rendering
    Graphics m_backGc; // used to speed up overlay rendering
    int [] m_backData;
//#endif
    static boolean s_OverlaySupported = false;
//#ifndef BlackBerry
    protected MyCanvas (MIDlet m) {
        midlet = m;
//#else
    protected MyCanvas (UiApplication app) {
        super(net.rim.device.api.ui.Manager.NO_VERTICAL_SCROLL|net.rim.device.api.ui.Manager.NO_HORIZONTAL_SCROLL);

        uiApp = app;
//#ifdef BlackBerry.Touched
//#ifdef BlackBerry.Signed
        m_orientation = Display.getOrientation();
//#endif
//#endif
//#endif
        context = new Context ();

        if (s_platform != null) { //MCP: Some SE emulators return null ?!
//#ifndef BlackBerry
            Logger.println ("System:"+s_platform);
//#else
            Logger.println ("System:"+s_platform+" "+net.rim.device.api.system.DeviceInfo.getDeviceName());
//#endif
            jp8 = s_platform.startsWith ("SonyEricssonW910");
        }
        
        // default keypad/keyboard handler
        m_keyboardHandler = new KeyboardHandler (this);

        // SKA: check in jad if ignoring size change notification
        String jadSizeChange = MiniPlayer.getJadProperty ("MEMO-IGNORE_SIZE_CHANGE");
        if (jadSizeChange != null && jadSizeChange.equalsIgnoreCase ("true")) {
            s_bIgnoreSizeChange = true;
        }
        
        //MCP: Nokia N70 is slow, putting setFullScreenMode() sooner
        // will make MIDlet manager call showNotify while
        // the Canvas constructor is not yet returned !
        setFullScreenMode (true);
    }

    private void forceUpdate () {
        synchronized (m_updateLock) { 
            forceUpdate = true; 
        }
    }
    
    protected void showNotify () {
        setFullScreenMode (true);
        if (thread == null) {
            //MCP: Only set canvas size with showNotify() the first time.
            // A bug on Nokia N95 sends back the wrong size when this is done on
            // later calls to showNotify() (eg after an LCDUI screen is dismissed).
            setSize (getWidth (), getHeight ());
            thread = new Thread (this);
            thread.start ();
//#ifdef MM.namespace
            Thread.s_mainThread = thread;
//#endif
        } else {
            isHidden = false;
            releaseLastEvent();
            forceUpdate();
            wakeUp();
        }
    }
    
    public void wakeUp() {
//#ifdef MM.pause
        //Logger.println("MyCanvas.wakeUp()");
        synchronized (thread) {
            composeAgain = true;
            thread.notify(); 
        }
//#endif 
    }
    
    protected void hideNotify() {
        isHidden = true;
    }

    protected void sizeChanged (int w, int h) {
        setSize (w, h);
        if (thread != null) {
            wakeUp ();
        }
    }

    void setSize (int w, int h) {
        if (w != m_width || h != m_height) {
            boolean firstTime = m_width == 0;

            // ignore size change notification if specified
            if( !firstTime && s_bIgnoreSizeChange ) {
                return;
            }

            // Ignore odd size screen (on LG Etna 2), convert to even size !
            m_width  = w%2==0 ? w : w + 1;
            m_height = h%2==0 ? h : h + 1;
            forceUpdate ();
            if (!firstTime) {
                // Notify UI, display a white screen and wait for 3 cycles
                Message.sendMessage ("MEMOPLAYER", "SIZE_CHANGED");
                synchronized (s_paintLock) {
                    m_paintType = PAINT_WHITE;
                    repaint ();
                }
                m_resizeCycle = 0; // wait 3 pass before allowing scene paint
            }
            //TODO m_backBuf = null; // force realloc of backBuff to new size
        }
    }


//#ifdef debug.console
    protected void checkConsole(int a) {
        if (a == '#' && m_debugStep == 0) {
            m_debugStep = 1;
        } else if (a == '*' && m_debugStep == 1) {
            m_debugStep = 2;
        } else if (a == '#' && m_debugStep == 2) {
            m_debugStep = 3;
            a = 'U';
        } else if (a == '*' && m_debugStep == 3) {
            forceUpdate();
            m_debugStep = 0;
        }

        if (m_debugStep == 3) {
            Logger.setKey (a);
//#ifdef BlackBerry
            repaint ();
//#endif
            wakeUp ();
        }
    }
//#endif

    protected void processKey(int a, int key) {
//#ifdef debug.console
        if (m_debugStep != 3) {
            if (a == 'X' && key > 0) {
                a = -key; // Pass keyboard value as negative (see KeySensor) !
            }
            addEvent (Event.KEY_PRESSED, a);
//#ifdef BlackBerry
            // HACK: no key up event for BlackBerry
            addEvent (Event.KEY_RELEASED, a);
//#endif
        } else {
//#ifdef BlackBerry
            // force repaint
            repaint ();
//#endif
            wakeUp ();
        }
//#else
        if (a == 'X' && key > 0) {
            a = -key; // Keyboard value !
        }
        addEvent (Event.KEY_PRESSED, a);
//#ifdef BlackBerry
        // HACK: no key up event for BlackBerry
        addEvent (Event.KEY_RELEASED, a);
//#endif
//#endif

//#ifdef jsr.nokia-ui 
        BackLight.setLastActiveTime (System.currentTimeMillis());
//#endif
    }



//#ifndef BlackBerry
    protected void keyPressed (int key) {
//#else
    protected boolean keyDown (int key, int time) {
//#endif

        // Logger.println ("keyPressed: "+key);
        int a = m_keyboardHandler.convertKey (key);

//#ifdef BlackBerry
        // if BlackBerry convertKey returns 0
        // then return false to generate keyChar event
        if(a==0) {
            return false;
        }
//#endif

//#ifdef debug.console
        checkConsole(a);
//#endif

        // process key value
        processKey(a,key);

//#ifdef BlackBerry
        return true;
//#endif
    }

//#ifndef BlackBerry
    protected void keyReleased (int key) {
        int a = m_keyboardHandler.convertKey (key);
        if (a == 'X' && key > 0) {
            a = -key; // Keyboard value !
        }
        addEvent (Event.KEY_RELEASED, a);
    }
//#endif

    final static int BUG_OFFSET = 0; // 55 for M600 emulator, 0 for phones

    protected void pointerPressed (int x, int y) {
//#ifdef debug.console
        if (m_debugStep == 3) {
            if (!Logger.setPointer (x, y, m_width, m_height)) {
                forceUpdate();
                m_debugStep = 0;
            }
            wakeUp();
//#ifdef BlackBerry.Touch
            repaint();
//#endif
            wakeUp();
            return;
        } else if (x<m_width/10 && y<m_height/10) { // top/left corner 
            m_debugStep++;
        }
//#endif
        addEvent (Event.MOUSE_PRESSED, x, y+BUG_OFFSET);
    }

    protected void pointerDragged (int x, int y) {
        addEvent (Event.MOUSE_DRAGGED, x, y+BUG_OFFSET);
    }

    protected void pointerReleased (int x, int y) {
        addEvent (Event.MOUSE_RELEASED, x, y+BUG_OFFSET);
    }

    private void addEvent (int eventType, int key) {
        addEvent (eventType, key, -1);
    }

    void addEvent (int eventType, int p1, int p2) {
//#ifdef debug.console
        if (m_debugStep == 3) return;
//#endif
        synchronized (this) { // Proguard 4.2 optimization step ignores
            Event e;          // synchronized defined at method level
            if (eventType == Event.MOUSE_DRAGGED) { // if there is already a
                e = m_event;                        // mouse-dragged, replace it
                while (e != null) {
                    if (e.m_type == Event.MOUSE_DRAGGED) {
                        e.m_x = p1;
                        e.m_y = p2;
                        wakeUp ();
                        return;
                    }
                    e = e.m_next;
                }
            } // otherwize just append a new event
            if (s_rootEvent == null) {
                e = new Event();
            } else {
                e = s_rootEvent;
                s_rootEvent = s_rootEvent.m_next;
            }
            e.set(eventType, p1, p2);
            if (m_event == null) {
                m_event = e;
            } else {
                Event n = m_event;
                while (n.m_next != null) {
                    n = n.m_next;
                }
                n.m_next = e;
            }
        }
        wakeUp ();
    }

    private void pickEvent (Event event) {
        synchronized (this) {
            // pick an event from waiting queue
            Event e = m_event;
            if (e != null) {
                m_event = e.m_next;
                // set as current event
                event.setFrom(e);
                // also keep as last event
                m_lastEvent.setFrom(e);
                // put it back in purge queue
                e.m_next = s_rootEvent;
                s_rootEvent = e;
            } else {
                event.m_type = event.NONE;
            }
        }
    }
    
    // Prevents loosing some RELEASE events when canvas is hidden
    // called by showNotify()
    private void releaseLastEvent() {
        synchronized (this) {
            Event e = m_lastEvent;
            if (e.convertToReleased()) {
                if (e.isKeyEvent()) {
                    addEvent(e.m_type, e.m_key);
                } else {
                    addEvent(e.m_type, e.m_x, e.m_y);
                }
                e.m_type = Event.NONE;
            }
        }
    }
    
//#ifndef BlackBerry
    public void paint (javax.microedition.lcdui.Graphics g) {
//#else
    static boolean bFirstPaint = true;
    public void paint (net.rim.device.api.ui.Graphics gc) {

        if(bFirstPaint) {
                bFirstPaint = false;
                showNotify();
        }

//#ifdef BlackBerry.Touched
//#ifdef BlackBerry.Signed
        // check if orientation has changed
        if( m_orientation != Display.getOrientation() ) {
            // changes detected
            m_orientation = Display.getOrientation();
            // readjust Height/Width
            setSize (getWidth (), getHeight ());
            return;
        }
//#endif
//#endif

        Graphics g = new Graphics(gc);

//#endif

        synchronized (s_paintLock) {
//#ifdef debug.safePaint
        try {
//#endif
        switch (m_paintType) {
        case PAINT_WHITE:
            g.setClip (0, 0, m_width, m_height);
            g.setColor (255, 255, 255);
            g.fillRect (0, 0, m_width, m_height);
//#ifdef BlackBerry
            g.popContext();
//#endif
            return;
        case PAINT_LOADER:
            int pos = (m_height - 20) / 2;
            g.setClip (0, 0, m_width, m_height);
            g.setColor (0x909090);
            g.fillRect (0, 0, m_width, m_height);
            g.setColor (0);
            g.setFont (Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, FontStyle.s_fontSmall));
            g.drawString (sceneName, 20, 60, Graphics.LEFT|Graphics.TOP);
            g.drawString (""+scene.getDuration ()+" Bytes", 20, 80, Graphics.LEFT|Graphics.TOP);
            g.setFont (Font.getDefaultFont());
            g.setColor (0x606060);
            g.fillRect (20-1, pos-1, 136+2, 22+2);
            msg.setLength (0); 
            int fill = (scene.getCurrent() * 136) / scene.getDuration ();
            int percent = (scene.getCurrent() * 100) / scene.getDuration ();
            msg.append ("Loading: "); msg.append (percent); msg.append ("%");
            g.setColor (0x606060);
            g.fillRect (20-1, pos-1, 136+2, 22+2);
            g.setColor (0xFF6600);
            g.fillRect (20, pos, fill, 22);
            g.setColor (0);
            g.drawString (msg.toString (), 23, pos+2, Graphics.TOP|Graphics.LEFT);
            break;
//#ifdef debug.console
        case PAINT_CONSOLE:
            g.setClip (0, 0, m_width, m_height);
            Logger.show (g, m_width, m_height);
            break;
        case PAINT_MEMBAR:
            g.setClip (0, 0, m_width, 2);
            g.setStrokeStyle(Graphics.SOLID);
            g.setColor(0x000000);
            g.fillRect(0, 0, m_width, 2);
            g.setColor(0xFF0000);
            g.drawLine(0, 0, s_cpuBar, 0);
            g.setColor(0x00FF00);
            g.drawLine(0, 1, s_memBar, 1);
            break;
        case PAINT_PAUSE:
            g.setClip(2, 2, m_width/20, m_height/20);
            g.setColor(0xFF0000);
            g.fillRect(2, 2, m_width/40-2, m_height/20);
            g.fillRect(2+m_width/40, 2, m_width/40-2, m_height/20);
            break;
        case PAINT_UNPAUSE:
            context.clip.set(2, 2, m_width/20+1, m_height/20+1);
//#endif
        default:
        case PAINT_SCENE:
            if (s_OverlaySupported && context.m_hasVideo) {
                g.setClip (0, 0, m_width, m_height);
                //redraw each rectangle around the video
                context.gc = g;
                Region e = context.exclude;    
                // draw center first before being prehempted by video system
                g.setClip (0, 0, m_width, m_height);
//#ifdef MM.overlay
                if (m_backData == null) { 
                    break;//exception au demarrage du au thread sleep
                }
                //g.setClip (e.x0, e.y0, e.x1-e.x0, e.y1-e.y0);
                int dstW = e.x1-e.x0-1;
                int dstH = e.y1-e.y0-1;
                g.drawRGB (m_backData, 0, dstW, e.x0, e.y0, dstW, dstH, true);
//#endif
                // draw bottom square
                //Logger.println(context.clip.x0+","+context.clip.y0+"  "+(context.clip.x1-context.clip.x0)+","+(context.clip.y1-context.clip.y0));
                if (r.applyIntersection (context.clip, 0, e.y1-1, m_width, m_height)) { // bottom
                    context.renderAll (r);
                }
                // draw top square
                if (r.applyIntersection (context.clip, 0, 0, m_width, e.y0)) { // top
                    context.renderAll (r);
                }
                // draw left square
                if (r.applyIntersection (context.clip, 0, e.y0, e.x0, e.y1)) { // left
                    context.renderAll (r);
                }
                // draw right square
                if (r.applyIntersection (context.clip, e.x1-1, e.y0, m_width, e.y1)) { // right
                    context.renderAll (r);
                }
//#ifdef MM.overlay
                if (jp8) {
                    g.drawRGB (m_backData, 0, dstW, e.x0, e.y0, dstW, dstH, true);
                }
//#endif
            } else {
                context.gc = g;
//#ifdef BlackBerry
                // BlackBerry: override clip zone to entire screen
                context.clip.setInt(0, 0, m_width, m_height);
//#endif
                context.renderAll (context.clip);
//#ifdef debug.console
                if (s_debugClip) {
                    //MCP: Print and draw the clipping zone
                    g.setClip (0, 0, m_width, m_height);
                    r = context.clip;
                    System.out.println(r.x0+":"+r.y0+" "+(r.x1-r.x0-1)+"x"+(r.y1-r.y0-1));
                    g.setColor(255, 0, 0);
                    g.drawRect(r.x0, r.y0, r.x1-r.x0-1, r.y1-r.y0-1);
                }
//#endif
            }
        }
//#ifdef debug.safePaint
        //MCP: Catch OutOfMemoryException errors during paint
        } catch (Throwable t) {
            Logger.println("Error during paint: "+t);
            //MCP: Free render nodes list to prevent MyCanvas thread lock
            context.m_renderNodes = null;
            context.m_lastRenderNode = null;
            System.gc();
        }
//#endif
        m_paintType = PAINT_NONE;
        }
//#ifdef BlackBerry
        g.popAllContext();
//#endif
    }
   
    void flushGraphics (int x, int y, int w, int h, int type) {
        synchronized (s_paintLock) {
            if (m_resizeCycle < RESIZE_CYCLES) {
                return; // ignore paints during resize
            }
//#ifdef debug.console
            m_paintType = m_debugStep == 3 ? PAINT_CONSOLE : type;
//#else
            m_paintType = type;
//#endif
            if (m_paintType == PAINT_SCENE) {
                if (w > 0 && h > 0) {
                    repaint (x, y, w, h);
                } else {
                    return;
                }
            }
//#ifndef BlackBerry
            else {
                repaint ();
            }
//#endif
        }
//#ifndef BlackBerry
        serviceRepaints ();
//#endif
        Thread.yield();
    }

    void displayConsole (String msg) {
        Logger.println (msg);
//#ifdef debug.console
        m_debugStep = 3;
        while (m_debugStep == 3) {
            //Event ev = pickEvent (); // will change m_debugStep
            flushGraphics (0, 0, m_width, m_height, PAINT_CONSOLE);
        }
//#endif
    }

    public void run () {
        // Try 'scn' cookie set by SMS from PushRegistry
        sceneName = CookyManager.get("scn");
        // Try 'boot' parameter in JAD (for MicroEmulator Applet boot)  
        if (sceneName == "") {
            sceneName = MiniPlayer.getJadProperty("boot");
        }
        // Try scene from the 'boot.txt' file in JAR
        if (sceneName == "") {
            DataInputStream dis = Decoder.getResStream ("boot.txt");
            sceneName = dis != null ? Decoder.readLine (dis) : null;
        }
        if (sceneName == null) {
            Logger.println ("Player: no scene to load!!! (check boot.txt)");
        } else {
            Logger.println ("Player: Loading scene '"+sceneName+"'");
        }
        while (sceneName != null && sceneName.length() > 0) {
            decoder = null; //new Decoder (sceneName);
            scene = new Scene (decoder, sceneName);
            decoder = scene.m_decoder;
            context.update (scene, decoder);
            sceneName = play (sceneName);
        }
//#ifndef BlackBerry
        ((MiniPlayer)midlet).clean();
        midlet.notifyDestroyed();
//#else
        ((MiniPlayer)uiApp).clean();
        System.exit(0);
//#endif
    }

    /**
     * @param sceneName
     * @return
     */
    public String play (String sceneName) {
        // for the computation of FPS
        int count = 0, idle = 0;
        long time;
        int sleepDuration;
        
        // to activate calls
        long startTime;
        long t;
        
        context.canvas = this;

        while (scene.getState () == Loadable.OPENING) {
            try { Thread.sleep (500); } catch (Exception e) {;}
        }
        if (!decoder.m_file.m_isLocal) {
            flushGraphics (0, 0, m_width, m_height, PAINT_LOADER);
        }        

        //StringBuffer msg = new StringBuffer ();

        while (scene.getState () == Loadable.LOADING) {
            try { Thread.sleep (500); } catch (Exception e) {;}
            if (!decoder.m_file.m_isLocal) {
                flushGraphics(0, 0, m_width, m_height, PAINT_LOADER);
            }
        }
        if (scene.getState () == Loadable.ERROR) {
            displayConsole ("Error while loading main scene");
            return null;
        }
        
        //MCP: Set the context matrix before start
        context.update(null, m_width, m_height);
        
        //KLF: Reset context time on new master scene
        context.time = 0;
        try {
            scene.start (context); 
        } catch (Exception e) {
            Logger.println ("MyCanvas:run exception during scene start: "+e);
            e.printStackTrace ();
        }

        // ready to start
        t = time = startTime = System.currentTimeMillis ();
        context.again = true;
        boolean update = true;
        Region e;//FTE    
        while (context.again) {
            
            // Check user input and update positions if necessary
            context.time = (int)(t-startTime);
            pickEvent (context.event);
            try {
                composeAgain = false;
                context.m_hasVideo = false;
                
                if (context.event.isKeyEvent()) {
                    Scene.checkGrabInputSensors(context.event);
                }
                
                synchronized (s_paintLock) {
                    context.clearRenderNodes ();
                    scene.compose (context, context.clip, update);
                }
                
                if (context.newUrl != null) {
                    scene.stop (context);
                    scene.destroy(context);
                    return (context.newUrl);
                }
                
            } catch (Exception exc) {
                exc.printStackTrace ();
                displayConsole ("Main loop: Exception caught: "+exc);
                break;
            }
            
            
            //MCP: Make sure no node is still marked as pressed after a mouse release
            if (context.event.m_type == Event.MOUSE_RELEASED) {
                context.m_sensedNode = null;
            }
            
//#ifdef debug.console
            // Prevent refresh of the mem/cpu bar part of screen
            if (s_memBar>0 && context.clip.y0<2) { 
                context.clip.y0 = 2;
            }
//#endif
            
            // Flush the off-screen buffer
            int x = context.clip.x0;
            int y = context.clip.y0;
            int w = context.clip.x1 - x;
            int h = context.clip.y1 -y;
            
//#ifdef MM.overlay
            if (s_OverlaySupported && context.m_hasVideo) {
                if (m_backBuf == null) {
                    m_backBuf = Image.createImage (m_width, m_height);
                    m_backGc = m_backBuf.getGraphics ();
                    m_backData = new int [m_width *m_height];
                }
                /*Region*/ e = context.exclude;                
                //center: render everything offscreen to make a single blit
                if (r.applyIntersection (context.clip, e.x0, e.y0, e.x1, e.y1)) { // center
                    int dx = r.x0;
                    int dy = r.y0;
                    int lx = r.x1-dx;//-1;
                    int ly = r.y1-dy;//!-1;
                    if (lx > 0 && ly > 0) {
                        context.gc = m_backGc;
                        m_backGc.setClip (0, 0, m_width, m_height);
                        m_backGc.setColor (0x000800);
                        m_backGc.fillRect (dx, dy, lx, ly);
                        context.renderAllFromVideo (r);
                        int dstW = e.x1-e.x0-1;
                        int dstH = e.y1-e.y0-1;
                        
                        m_backBuf.getRGB (m_backData, 0, dstW, e.x0, e.y0, dstW-e.x0, dstH);
                        
                        int l = dstW*dstH-1;
                        for (; l >= 0; l--) {
                            if (m_backData [l] == 0xFF000800) {
                                m_backData [l] = 0;
                            }
                        } 
                    }
                }
            }
//#endif
            flushGraphics(x, y, w+1, h+1, PAINT_SCENE);
            
            // Check framerate
            long delta  = System.currentTimeMillis () - t;
            if (delta < 20) { // Limit framerate to 50fps
                try { Thread.sleep (20 - delta); } catch (Exception exc) { ; }
                idle += 20 - delta;
            } else if (delta > 250) { // Under 4fps, compensate time
                startTime += delta - 250;
            }
            
            // FPS computation and display
            count++;
            if ( (t-time) > 1000) {
                context.fps = count; //(1000*count) / (int)((time-t));
                context.cpu = idle;
//#ifdef debug.console
                if (s_memBar > 0) {
                    System.gc();
                    s_memBar = m_width - (int)(m_width * Runtime.getRuntime().freeMemory() / Runtime.getRuntime().totalMemory());
                    s_cpuBar = m_width - (m_width * idle) / (int)(t-time);
                    flushGraphics(0, 0, m_width, 2, PAINT_MEMBAR);
                }
//#endif
                time = t;
                idle = count = 0;
            }

            t = System.currentTimeMillis ();
            
            // Force 3 cycles during resize before enabling PAINT_SCENE again
            if (m_resizeCycle < RESIZE_CYCLES) {
                m_resizeCycle++;
                composeAgain = true;
                if (m_resizeCycle == RESIZE_CYCLES) {
                    forceUpdate = true; // last cycle, refresh all !
                }
            }
            
            //MCP: Messages queues must be switched
            composeAgain |= Message.switchQueues();
            
//#ifdef MM.pause
            //MCP: Pause thread when canvas is not displayed or no activity
            if (isHidden) {
                sleepDuration = SLEEP_FOREVER;
//#ifdef debug.console
            } else if (m_debugStep == 3 ) {
                sleepDuration = SLEEP_CANCELED;
//#endif
            } else if (composeAgain || m_event != null) {
                sleepDuration = SLEEP_CANCELED;
            } else {
                sleepDuration = scene.getSleepDuration ((int)(t-startTime));
            }
            if (sleepDuration > SLEEP_TRESHOLD) {
                long p = t;
                //Logger.println ("Pausing for "+sleepDuration+"ms");
//#ifdef debug.console
                if (s_pauseIndicator) flushGraphics (0, 0, 0, 0, PAINT_PAUSE);
//#endif
                try {
                    synchronized (thread) {
                        // Using 0 instead of Integer.MAX_VALUE to pause
                        // forever to prevent crash on the LG Callisto.
                        thread.wait (sleepDuration == SLEEP_FOREVER ? 0 : sleepDuration);
                    }
                } catch (InterruptedException ie) { }
                t = System.currentTimeMillis ();
                if (sleepDuration == SLEEP_FOREVER) { // no time compensation when paused by inactivity
                    long pauseInterval = t - p;
                    //Logger.println ("Unpaused after "+pauseInterval);
                    time += pauseInterval;
                    startTime += pauseInterval;
                }
//#ifdef debug.console
                if (s_pauseIndicator) flushGraphics (0, 0, 0, 0, PAINT_UNPAUSE);
//#endif
            }
//#endif
            
            DataLoader.check();
            
            // Thread safe forceUpdate notification
            synchronized (m_updateLock) {
                update = forceUpdate;
                forceUpdate = false;
            }
            
            // Check if screen size changed
            if (update && (m_width != context.width || m_height != context.height)) {
                context.update(null, m_width, m_height);
            }
        }
        scene.stop (context); 
        return null;
    }
    
//#ifdef BlackBerry
// BlackBerry specific methods    

    protected void onExposed() {
        showNotify ();
    }

    protected void onObscured() {
        hideNotify();
    }

    protected void onFocusNotify(boolean focus) {
        if( focus )
			showNotify();
        else
            hideNotify();
    }

//#ifdef BlackBerry.Touch
    protected boolean touchEvent(TouchEvent message) {
     int x = message.getGlobalX(1);
     int y = message.getGlobalY(1);
     
     switch(message.getEvent()) {
             case TouchEvent.DOWN:
                     pointerPressed( x, y);
                       return true;
             case TouchEvent.UP:
                     pointerReleased( x, y);
                     return true;
             case TouchEvent.MOVE:
                     pointerDragged( x, y);
                     return true;
             default:
     }
        
     return false;
    }

    void onData(net.rim.device.api.system.AccelerometerData accData) {
     int newOrientation = accData.getOrientation();
     if ( m_accOrientation != newOrientation ) {
             setSize (getWidth (), getHeight ());
             repaint();
     }
    }
//#endif

    // trackball management
    protected boolean navigationMovement(int dx, int dy, int status, int time) {
        int keyCode=-1;
        // check horizontal movement
        if(dx!=0) {
            if(dx>0) {
                keyCode='R';
            } else {
                dx = -dx;
                keyCode='L';
            }
            for(int i=0; i<dx; i++) {
                processKey(keyCode,0);
            }
        }

        // check vertical movement
        if(dy!=0) {
            if(dy>0) {
                keyCode='D';
            } else {
                dy = -dy;
                keyCode='U';
            }
            for(int i=0; i<dy; i++) {
                processKey(keyCode,0);
            }
        }
        return true;
    }
                                     
    // trackball click management
    protected boolean navigationClick(int status, int time) {
//#ifndef BlackBerry.Touch
//#ifdef debug.console
        if (m_debugStep == 3) {
          Logger.setKey ('E');
          repaint();
          wakeUp ();
          return true;
        }
//#endif
        addEvent (Event.KEY_PRESSED, 'E'); 
//#endif
        return true;
    }

    protected boolean navigationUnclick(int status, int time) {
//#ifndef BlackBerry.Touch
        addEvent (Event.KEY_RELEASED, 'E'); 
//#endif
        return true;
    }

    // special key management
    protected boolean keyControl(char c, int status, int time) {
        if(c==net.rim.device.api.system.Characters.CONTROL_VOLUME_DOWN) {
                Message.sendMessage ("VOLUME", "DOWN");
        } else if(c==net.rim.device.api.system.Characters.CONTROL_VOLUME_UP) {
                Message.sendMessage ("VOLUME", "UP");
        }

        return super.keyControl(c, status, time);
    }

    // keyRepeat
    protected boolean keyRepeat (int key, int time) {
        return keyDown(key,time);
    }

    // keyChar get char from keyBoard
    protected boolean keyChar ( char c, int status, int time) {
        
//#ifdef debug.console
        checkConsole(c);
//#endif

        int key='X';
        // check if it is a keypad numeric key
        if ( (c>='0') && (c<='9') ) {
            key = c;
        } else if( (c=='*') || (c=='#')) {
            key = c;
        }

        // process key value
        processKey(key,c);

        return false;
    }

    // repaint wrapper functions
    private void repaint() {
        // invalidate();
        uiApp.repaint();
    }
    private void repaint(int x,int y,int width,int height) {
        invalidate(x,y,width,height);
    }
    // stub setFullScreenMode, RIM apps are always fullscreen
    private void setFullScreenMode(boolean mode){
    }
//#endif

}
