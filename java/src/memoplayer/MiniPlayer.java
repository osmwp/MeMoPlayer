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
import javax.microedition.midlet.*;


public class MiniPlayer extends MIDlet {
    private MyCanvas m_canvas = null;
    static Display s_display = null;
    static MiniPlayer self = null;

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
            s_display = Display.getDisplay(this);
            s_display.setCurrent(m_canvas);
        }
    }

    public void pauseApp() {}
    
    public void destroyApp (boolean unconditional) {
        clean();
    }
    
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
        //Logger.println ("MiniPlayer.vibrate: "+ms);
        if (s_display != null) {
            s_display.vibrate (ms);
        }
    }
    
    static String getJadProperty (String name) {
        String p = self != null ? self.getAppProperty(name) : null;
        return p != null ? p : "";
    }
    
    static boolean openUrl (String url) {
        try {
            return self.platformRequest(url);
        } catch (Exception e) {
            return false;
        }
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
