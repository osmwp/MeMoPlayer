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

public class Scene implements Loadable {    
    /**
     * Max size the scene DataLinks can occupy in memory
     */
    static int s_maxDataLinkSize = 500*1024;
    
    static {
    	String s = MiniPlayer.getJadProperty ("MeMo-MaxDLSize");
        if (s != "") {
            try { s_maxDataLinkSize = Integer.parseInt(s)* 1024; } catch (Exception e) { }
        } 
    }
    
    private static ObjLink s_inputGrabSensors; // All registered InputSensors with grabFocus
    
    Node m_node;
    Decoder m_decoder;
    String m_sceneName, m_originalName;
    private ObjLink m_timeSensors;
    private ObjLink m_inputSensors;
    private ObjLink m_sleepyNodes;
    DataLink m_dataLink; // dynamic datalinks (used by ImageTexture)
    ObjLink m_blacklist; // blacklisted urls (used by ImageTexture)
    FileQueue m_fq;
    
    /**
     * Must be called by MyCanvas before scenes compose to detect 
     * if an InputSensor with grabFocus matches the current event.
     */
    public static void checkGrabInputSensors(Event e) {
        ObjLink ol = s_inputGrabSensors;
        while (ol != null && ol.m_object != null) {
            InputSensor is = (InputSensor)ol.m_object;
            if (is.listenTo (e)) {
                e.m_grabSensor = is;
                return;
            }
            ol = ol.m_next;
        }
    }

    Scene (Decoder decoder, String name) {
        m_originalName = name;
        
        if (decoder == null || name.startsWith ("http://") || name.startsWith ("file://") || name.startsWith("cache://") || name.startsWith("jar://")) {
            m_decoder = new Decoder (name);
        } else if (decoder.getSceneData (name) != null) {      
            m_decoder = decoder;
        } else {
            name = decoder.baseName+name;
            m_decoder = new Decoder (name);
        }
        m_sceneName = name;
    }

    public String getName () {
        return m_originalName;
    }

    public int getState () {
        return m_decoder.getState ();
    }

    public int getCurrent () {
        return m_decoder.getCurrent ();
    }

    public int getDuration () {
        return m_decoder.getDuration ();
    }

    public String getErrorMessage () {
        return "";
    }

    void start (Context c) {
        if (m_node == null) { //MCP: Prevent re-decoding scene each time scene is started/stopped 
            m_decoder.getScene (m_sceneName, this);
        }
        if (m_node != null) { 
            Scene prevScene = c.scene;
            c.scene = this;
            m_node.start (c);
            c.scene = prevScene;
        }
    }

    void render (Context c) {
        if (m_node != null) { 
            Scene prevScene = c.scene;
            c.scene = this;
            m_node.render (c);
            c.scene = prevScene;
        }
    }

    void stop (Context c) {
        if (m_node != null) { 
            Scene prevScene = c.scene;
            c.scene = this;
            m_node.stop (c);
            c.scene = prevScene;
            ObjLink.releaseAll(m_blacklist);
            ObjLink.releaseAll(m_sleepyNodes);
            m_blacklist = null;
            m_sleepyNodes = null;
            if (m_fq != null) m_fq.clean();
            // Cleanup possible touch event
            if (c.m_lastTouchedScene == this) {
                c.m_lastTouchedScene = null;
                c.m_lastTouchedSensor = null;
                c.m_sensedNode = null;
            }
        }
    }

    void destroy (Context c) {
        if (m_node != null) { 
            m_node.destroy (c);
            m_node = null;
        }
        m_node = null;
    }

    void register (Activable a) {
        if (a instanceof InputSensor) {
            //System.out.println ("Scene.register: "+a+", grab="+grab+", MS="+m_masterScene);
            // InputSensor with grabFocus are kept in their own list
            if (((InputSensor)a).m_grabInput) {
                s_inputGrabSensors = ObjLink.create (a, s_inputGrabSensors);
            } else {
                m_inputSensors = ObjLink.create (a, m_inputSensors);
            }
        } else if (a instanceof TimeSensor) {
            m_timeSensors = ObjLink.create (a, m_timeSensors);
        }
    }

    void unregister (Activable a) {
        //System.out.println ("Scene.unregister: "+a);
        if (a instanceof InputSensor) {
            if (((InputSensor)a).m_grabInput && s_inputGrabSensors != null) {
                s_inputGrabSensors = s_inputGrabSensors.remove (a);
            } else if (m_inputSensors != null) {
                m_inputSensors = m_inputSensors.remove (a);
            }
        } else if (a instanceof TimeSensor) {
            if (m_timeSensors != null) {
                m_timeSensors = m_timeSensors.remove (a);
            }
        }
    }

    void activateActivables (Context c, ObjLink ol) {
        while (ol != null && ol.m_object != null) {
            ((Activable)ol.m_object).activate (c);
            ol = ol.m_next;
        }
    }

    void activateInputSensors (Context c) {
        Event e = c.event;
        if (e.isKeyEvent()) {
            if (e.m_grabSensor == null) {
                // Event was not grabbed, check all InputSensors of the scene
                activateActivables (c, m_inputSensors);
            } else if (e.m_grabSensor.m_scene == this) {
                // Event is grabbed by an InputSensor of this scene, activate it 
                ((Activable)e.m_grabSensor).activate(c);
                e.m_type = Event.NONE;
            }
        }
    }
    
    boolean innerCompose (Context c, Region clip, boolean forceUpdate) {
        boolean updated = false;
        if (m_node != null) {
            Scene prevScene = c.scene;
            c.scene = this;
            c.activateLastTouchSensor();
            activateInputSensors (c);
            activateActivables (c, m_timeSensors);
            updated = m_node.compose (c, clip, forceUpdate);
            if (m_fq != null) m_fq.loadNext();
            c.scene = prevScene;
        }
        return updated;
    }
    
    boolean compose (Context c, Region clip, boolean forceUpdate) {
        if (m_node != null) {
            c.clearRenderNodes ();
            c.clip.setInt (c.width, c.height, 0, 0);
            c.resetMatix();
            return innerCompose (c, clip, forceUpdate);
        }
        return false;
    }
    
    final Image getImage (String name) {
        DataLink dl = DataLink.find (m_dataLink, name);
        return dl == null ? null : dl.getImage ();
    }
    
    final boolean checkImage (String name) {
        return DataLink.find (m_dataLink, name) != null;
    }

    /**
     * Load a byte array as a new Datalink of this scene
     */
    public void addData(String name, byte[] data, int type, boolean force) {
        // Purge list of added DataLinks to keep it low in memory
        if (m_dataLink != null) {
            m_dataLink = m_dataLink.purge(0, s_maxDataLinkSize-data.length);
        }
        
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
//#ifdef MM.weakreference
            previous.m_objectRef = null;
//#else
            previous.m_object = null;
//#endif
        }
    }
    
    /**
     * Remove a DataLink of the scene
     */
    public void removeData (String name) {
        if (m_dataLink != null) {
            m_dataLink = m_dataLink.remove (name);
        }
    }
    
    void addBlacklist (String url) {
        m_blacklist = ObjLink.create (url, m_blacklist);
    }
    
    boolean isBlacklisted (String url) {
        ObjLink ol = m_blacklist;
        while (ol != null) {
            if (url.equals (ol.m_object)) {
                return true;
            }
            ol = ol.m_next;
        }
        return false;
    }
    
    final FileQueue getFileQueue() {
        if (m_fq == null) {
            m_fq = new FileQueue();
        }
        return m_fq;
    }
    
    void registerSleepy (Node n) {
        m_sleepyNodes = ObjLink.create (n, m_sleepyNodes);
    }
    
    void unregisterSleepy (Node n) {
        if (m_sleepyNodes != null) {
            m_sleepyNodes = m_sleepyNodes.remove (n);
        }
    }

//#ifdef MM.pause
    // Search for the smallest wakeup time across sleepy nodes, TimeSensors & InputSensors
    // returns SLEEP_CANCELED, SLEEP_FOREVER or a wakeup time
    int getWakeupTime (int time) {
        int wt, t = MyCanvas.SLEEP_FOREVER;
        ObjLink ol = m_sleepyNodes;
        while (ol != null) {
            wt = ((Node)ol.m_object).getWakeupTime (time);
            if (wt == MyCanvas.SLEEP_CANCELED) return wt;
            if (wt < t) t = wt; // keep smallest time
            ol = ol.m_next;
        }
        ol = m_timeSensors;
        while (ol != null) {
            wt = ((Node)ol.m_object).getWakeupTime (time);
            if (wt == MyCanvas.SLEEP_CANCELED) return wt;
            if (wt < t) t = wt; // keep smallest time
            ol = ol.m_next;
        }
        ol = m_inputSensors;
        while (ol != null) {
            wt = ((Node)ol.m_object).getWakeupTime (time);
            if (wt == MyCanvas.SLEEP_CANCELED) return wt;
            if (wt < t) t = wt; // keep smallest time
            ol = ol.m_next;
        }
        ol = s_inputGrabSensors;
        while (ol != null) {
            InputSensor is = (InputSensor)ol.m_object;
            if (is.m_scene == this) {
                wt = is.getWakeupTime (time);
                if (wt == MyCanvas.SLEEP_CANCELED) return wt;
                if (wt < t) t = wt; // keep smallest time
            }
            ol = ol.m_next;
        }
        return t;
    }
    
    // Called by MyCanvas on master scene only
    // returns SLEEP_CANCELED, SLEEP_FOREVER or a sleep duration
    int getSleepDuration (int time) {
        int t = getWakeupTime (time);
        if (t != MyCanvas.SLEEP_FOREVER && t != MyCanvas.SLEEP_CANCELED) {
            t -= time; // pausing for a limited time, convert to a duration
        }
        return t;
    }
//#endif
}
 
