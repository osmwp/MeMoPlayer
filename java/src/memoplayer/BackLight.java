//#condition jsr.nokia-ui
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
//#ifndef BlackBerry.Signed
import com.nokia.mid.ui.*;
//#endif
//#endif

public class BackLight implements Runnable {

    private boolean m_again;
    private int cycle, timeout; 
    private long lastTime;
    private static BackLight s_backLight = null;
//#ifndef BlackBerry
    private static boolean s_available = false;
    static {
        try {
            Class.forName ("com.nokia.mid.ui.DeviceControl");
            s_available = true;
        } catch (ClassNotFoundException e) { }
    }
//#else
//#ifdef BlackBerry.Signed
    private static boolean s_available = true;
//#else
    private static boolean s_available = false;
//#endif
//#endif

    public BackLight() {
        timeout = 10000;
        startThread ();
    }

    void startThread () {
        if (m_again == false) {
            new Thread (this).start();
        } // otherwize means already started
    }

    static void setLastActiveTime (long time) {
        if (s_backLight != null) {
            s_backLight.lastTime = time;
        }
    }
    
    static boolean isAvailable () {
        return s_available;
    }
    
    static void start () {
        if (s_available) {
            if (s_backLight == null) {
                s_backLight = new BackLight ();
            } else {
                s_backLight.startThread ();
            }
        }
    }

    static void stop () {
        if (s_backLight != null) {
            s_backLight.m_again = false;
        }
    }

    public void run () {
        lastTime = System.currentTimeMillis ();
        m_again = true;
        int delta = 0;
        while (m_again) {
            cycle++;
            try {
//#ifndef BlackBerry
                if (cycle >=  2) {
                    cycle = 0;
                    DeviceControl.setLights (0, 0);
                }
                DeviceControl.setLights (0, 100);
//#else
//#ifdef BlackBerry.Signed
	        	net.rim.device.api.system.Backlight.enable(true,255);
//#endif
//#endif
                while (delta < timeout) {
                    lastTime = System.currentTimeMillis ();
                    Thread.sleep (timeout);
                    delta = (int) (System.currentTimeMillis ()-lastTime);
                }
                delta = 0;
            } catch (Exception e) {
                Logger.println ("deviceControl not supported: "+e);
                m_again = false;
            }
        }
    }
}

