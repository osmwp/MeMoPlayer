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
import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
//#else
import net.rim.device.api.ui.UiApplication;
import net.rim.blackberry.api.browser.Browser;
import net.rim.blackberry.api.browser.BrowserSession;
import net.rim.device.api.ui.Screen;
import java.util.Hashtable;
import java.io.DataInputStream;
//#endif


//#ifndef BlackBerry
public class MiniPlayer extends MIDlet {
//#else
public class MiniPlayer extends UiApplication {
//#endif

    private MyCanvas m_canvas = null;

//#ifndef BlackBerry
    static Display s_display = null;
    static MiniPlayer self = null;
//#else
    static protected MiniPlayer self = null;
	static protected Hashtable m_jadProperties;
//#endif

//#ifndef BlackBerry
// MIDLET VERSION
    public void startApp() {
        self = this;
//#ifdef ulow
        Logger.println ("Miniplayer.startApp");
//#endif
//#ifdef api.traffic
        Traffic.loadTotal();
//#endif
        
//#ifdef MM.pushSMS
        // Check if MIDlet was started by a SMS Push
        MessagingHelper.receiveSMS (true);
//#endif
        
        if (m_canvas == null) {
            m_canvas = new MyCanvas (this);
        }
            s_display = Display.getDisplay(this);
            s_display.setCurrent(m_canvas);
        }

    public void pauseApp() {}
    
    public void destroyApp (boolean unconditional) {
        clean();
    }
    
	// END OF MIDLET VERSION
//#else
    // BLACKBERRY SPECIFIC

    // Constructor
    public MiniPlayer() {
    	loadJadProperties("jad.properties");
    	m_canvas = new MyCanvas(this);
    	pushScreen( m_canvas );
        // Prevent UI from rotating our screen.
        // Ui.getUiEngineInstance().setAcceptableDirections( DEFAULT_ORIENTATION );
//#ifdef api.traffic
        Traffic.loadTotal();
//#endif
    }

    // load midlet style properties from specified file
    static void loadJadProperties(String fileName) {
        m_jadProperties = new Hashtable();
        try {
            DataInputStream dis = Decoder.getResStream (fileName);
            String line = Decoder.readLine (dis);
            while( (line!=null)  && (line.length()>0) ) {
                int i=line.indexOf(": ");
                if(i>0) {
                    String propertyName  = line.substring(0,i).trim();
                    String propertyValue = line.substring(i+2).trim();
                    m_jadProperties.put(propertyName,propertyValue);
                }
                line = Decoder.readLine (dis);
        }
        } catch (Exception e) {
        }
    }
    

// END OF BLACKBERRY SPECIFIC
//#endif
    
    public void clean() {
        JSFile.clean(null);
        FileQueue.stopThread();
//#ifdef api.traffic
        Traffic.saveTotal();
//#endif
        CacheManager.clean();
        self = null;
    }

    static void vibrate (int ms) {
//#ifndef BlackBerry
        //Logger.println ("MiniPlayer.vibrate: "+ms);
        if (s_display != null) {
            s_display.vibrate (ms);
        }
//#else
        net.rim.device.api.system.Alert.startVibrate(ms);
//#endif
    }
    
    static String getJadProperty (String name) {
        String p=null;
//#ifndef BlackBerry
        if( self != null ) {
            p =  self.getAppProperty(name);
        }
//#else
        if( m_jadProperties != null ) {
            p = (String)m_jadProperties.get(name);
        }
//#endif
        return ( p != null ? p : "");
    }
    
    static boolean openUrl (String url) {
        try {
//#ifndef BlackBerry
            return self.platformRequest(url);
//#else
//#ifdef BlackBerry.Signed
            BrowserSession site = Browser.getDefaultSession();
            site.displayPage(url);
            return true;
//#endif
//#endif
        } catch (Exception e) { }
            return false;
    }
    
    /**
     * Simulate KEY or MOUSE events
     */
    static void addEvent (int eventType, int p1, int p2) {
        self.m_canvas.addEvent(eventType, p1, p2);
    }
    
    static void wakeUpCanvas() {
        if (self != null && self.m_canvas != null) {
            self.m_canvas.wakeUp();
        }
    }
    
//#ifdef MM.pfs
    private static String PFS_BASE_URL = "";
    
    /*
     * Returns the target base url for the server platform (PFS)
     */
    public static String getPfsBaseUrl() {
        if (PFS_BASE_URL == "") {
            // Search for the PFS JAR property
            PFS_BASE_URL = getJadProperty("PFS");
            if (PFS_BASE_URL == "") {
                // Search for a cooky base PFS target
                PFS_BASE_URL = CookyManager.get("baseUrl");
            }
        }
        return PFS_BASE_URL;
    }
//#endif
}
