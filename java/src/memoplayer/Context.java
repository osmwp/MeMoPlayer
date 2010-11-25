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
import javax.microedition.lcdui.Image;
//#endif

public class Context {

    final static int MAX_ACTIVABLES = 1024;

    MyCanvas canvas;
    Graphics gc;
    Scene scene;
    Decoder decoder;
    Script script;
    ObjLink m_renderNodes, m_lastRenderNode;;
    ObjLink m_loadables;
    Matrix matrix, initMatrix;
    static int time; // currentTime
    int width, height; // size of the scene
    Event event; // the current value (m_type is NONE if no pending event)
    
    boolean again; // if set to false player is ending 
    String newUrl; // if set the upper parent should play the url
    int newUrlCount; // the number of scene it should go: 0 => the upper scene, 1 => the upper upper scene and -1 the main scene
    Region clip, exclude, bounds;
    boolean m_forceUpdate;
    AppearanceContext ac;
    boolean isLayer=false;
    boolean m_hasVideo;
    // for touch sensor management
    Group m_groupingNode = null; // this is used to set the current grouping node for TouchSensor
    TouchSensor m_sensor = null; // the current propagated sensor
    Node m_sensedNode = null; // the current active node that "grab" pointer inputs,  if not null!
    // Only the last TouchSensor must be triggered over all scenes when multiple TouchSensors overlap
    TouchSensor m_lastTouchedSensor; // the last Touch to activate
    Scene m_lastTouchedScene = null; // the last scene with a TouchSensor to activate
    Event m_lastTouchedEvent = null;
    Node m_lastTouchedNode = null;
    Matrix m_lastTouchedMatrix = null;
    
    int fps, cpu; // the current fps and cpu usage (0..100)


    Context () {
        ac = null;
        matrix = new Matrix (true);
        clip = new Region ();
        bounds = new Region();
        exclude = new Region ();
        exclude.setFloat (0, 0, -1, -1);
        event = new Event ();
        m_lastTouchedEvent = new Event();
        initMatrix = new Matrix (false);
        m_lastTouchedMatrix = new Matrix(false);
    }

    Context (Graphics g, Decoder d, Scene s, int w, int h) {
        super ();
        update (s, d);
        update (g, w, h);
    }
    
    void copyTo (Context c) {
        c.canvas = canvas;
        c.gc = gc;
        c.scene = scene;
        c.decoder = decoder;
        c.m_renderNodes = m_renderNodes;
        c.m_lastRenderNode = m_lastRenderNode;
        c.m_loadables = m_loadables;
        // deep copy the matrix
        c.matrix.copyFrom (matrix);
        c.initMatrix.copyFrom (initMatrix);
        c.time = time;
        c.width = width; 
        c.height = height;
        c.event.setFrom(event);
        c.again = again;
        c.newUrl = newUrl;
        c.newUrlCount = newUrlCount;
        c.clip = clip; 
        c.bounds.set(bounds);
        c.exclude = exclude;
        c.m_forceUpdate = m_forceUpdate;
        c.ac = ac;
        c.isLayer = isLayer;
        c.m_hasVideo = m_hasVideo;
    }


    void update (Scene s, Decoder d) {
        scene = s;
        decoder = d;
        newUrl = null;
    }
    
    void update (Graphics g, int w, int h) {
        gc = g;
        width = w;
        height = h;
        initMatrix.loadIdentity();
        initMatrix.translate ((w/2)<<16, (h/2)<<16);
        initMatrix.scale (1<<16, (-1)<<16);
        bounds.setInt(0, 0, w, h);
    }
    
    void resetMatix() {
        matrix.setValueFrom (initMatrix);
    }

    void setCurrentImage (ImageContext ic) {
        ac.m_image = ic;
    }

    DataLink getDataLink (String name) {
        //System.out.println ("Context.getImage "+name+" in "+decoder);
        return decoder.getDataLink (name);
    }

    Image getImage (String name) {
        //System.out.println ("Context.getImage "+name+" in "+decoder);
        Image img = decoder.getImage (name);
        if (img == null) {
            img = scene.getImage(name);
        }
        return img;
    }
    
    boolean checkImage (String name) {
        if (decoder.checkImage(name)) {
            return true;
        }
        return scene.checkImage (name);
    }

    String getFullImageName (String name) {
        //System.out.println ("Context.getImage "+name+" in "+decoder);
        return ""+decoder+":"+name;
    }

  //  byte [] getScript (String name) {
  //      return decoder.getScript (name);
  //  }


    void clearRenderNodes () {
        m_lastRenderNode = null;
        ObjLink.releaseAll (m_renderNodes);
        m_renderNodes = null;
    }

    void addRenderNode (Node r) {
        ObjLink rn = ObjLink.create (r, r.m_ac, null);
        if (m_lastRenderNode != null) {
            m_lastRenderNode.m_next = rn;
        } else {
            m_renderNodes = rn;
        }
        m_lastRenderNode = rn;
        // check for pointer event 
        if (m_sensor != null && event.isMouseEvent ()) {
            Node sensedNode = m_sensor.m_grabAll ? m_sensor.m_sensedNode : m_sensedNode;
            // 
            if (sensedNode == null) {
                if (event.m_type == Event.MOUSE_PRESSED && event.isInside (r.m_region) &&
                    bounds != null && event.isInside (bounds)) { // prevent touch on clipped elements
                    m_sensor.m_region = r.m_region;
                    if (m_sensor.m_grabAll) {
                        m_sensor.m_sensedNode = r;
                        m_sensor.activate(this, event);
                    } else {
                        //MCP: Do not activate the TouchSensor now as other overlapping
                        // TouchSensors might overwrite these values. Only the last
                        // TouchSensor will be activated on next scene compose.
                        m_lastTouchedSensor = m_sensor;
                        m_lastTouchedScene = scene;
                        m_lastTouchedEvent.setFrom(event);
                        m_lastTouchedMatrix.setValueFrom(matrix);
                        m_lastTouchedNode = r;
                        MyCanvas.composeAgain = true; // force next scene compose !!!
                    }
                }
            } else if (sensedNode == r) {
                m_sensor.m_region = r.m_region;
                m_sensor.activate(this, event);
            }
        }
    }
    
    /**
     * Activate the last (most overlapping) TouchSensor with MOUSE_PRESSED event.
     * This method must be called at each compose of all scenes.
     */
    void activateLastTouchSensor() {
        if (m_lastTouchedScene == scene) {
            matrix.push();
            matrix.setValueFrom(m_lastTouchedMatrix);
            m_lastTouchedSensor.activate(this, m_lastTouchedEvent);
            matrix.pop();
            m_lastTouchedScene = null;
            m_lastTouchedSensor = null;
            m_sensedNode = m_lastTouchedNode;
        }
    }
  
    void renderAll (Region clip) {
        //System.out.println("RenderAll: "+clip.toString());
        clip.setClipRegion (gc);
        m_lastRenderNode = null;
        ObjLink ol = m_renderNodes;
        while (ol != null) {
            AppearanceContext ac = (AppearanceContext)ol.m_param;
            Node rn = (Node) ol.m_object;
            rn.m_region = ac.m_region;
            if (rn.regionIntersects (clip)) {
                rn.m_ac = ac;
                rn.render (this);
            }
            ol = ol.m_next;
        }
//#ifdef BlackBerry
        gc.popContext();
//#endif
    }
    
    void renderAllFromVideo (Region clip) {
        //System.out.println("renderAllFromVideo");
        clip.setClipRegion (gc);
        boolean hasVideo = false;
        m_lastRenderNode = null;
        ObjLink ol = m_renderNodes;
        while (ol != null) {
            Node rn = (Node)ol.m_object;
            if (hasVideo) {
                AppearanceContext ac = (AppearanceContext)ol.m_param;
                rn.m_ac = ac;
                rn.m_region = ac.m_region;
                rn.render (this);
            }
            hasVideo |= rn.m_isVideo;
            ol = ol.m_next;
        }
//#ifdef BlackBerry
        gc.popContext();
//#endif
    }

    void addLoadable (Loadable l) {
        m_loadables = ObjLink.create (l, m_loadables);
    }

    void removeLoadable (Loadable l) {
        if (m_loadables != null) {
            m_loadables = m_loadables.remove (l);
        }
    }
    Loadable findLoadable (String name) {
        ObjLink ol = m_loadables;
        while (ol != null) {
            Loadable l = (Loadable)ol.m_object;
            if (name.equals (l.getName ())) {
                return l;
            }
            ol = ol.m_next;
        }
        return (null);
    }
}
