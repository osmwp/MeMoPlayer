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

public class TouchSensor extends Node {
    boolean m_isOver = false;
    boolean m_stateChanged = false;
    boolean m_modeChanged = false;
    boolean m_grabAll = false;
    Node m_sensedNode = null;
    Point m_p = null;

    TouchSensor () {
        super (6);
        //System.out.println ("TouchSensor created");
        m_field[0] = new SFBool (true, this);  // enabled
        m_field[1] = new SFBool (false);       // isActive
        m_field[2] = new SFBool (false);       // isOver
        m_field[3] = new SFTime (0);           // touchTime
        m_field[4] = new SFVec3f (0, 0, 0);    // hitPoint_changed
        m_field[5] = new SFString ("", this);  // mode ("normal", "grabAll")
   }
 
    void start (Context c) {
        if (c.m_groupingNode != null && ((SFBool)m_field[0]).getValue ()) {
            c.m_groupingNode.register(this);
        }
        fieldChanged (m_field[5]);
        m_p = new Point ();
    }

    void stop (Context c) {
        if (c.m_groupingNode != null) {
            c.m_groupingNode.unregister(this);
        }
        m_sensedNode = null;
        m_p = null;
    }
    
    public void activate (Context c, Event event) {
        //Logger.println ("TouchSensor.activate: "+event.m_type+"/"+event.m_x+event.m_y+" for "+this);
        
        m_p.set (event.m_x<<16, event.m_y<<16);
        c.matrix.revTransform (m_p);
        
        //MCP: The hitPoint_changed field must be updated *before* over signals
        // so we can get the right value when other signals are routed.
        ((SFVec3f)m_field[4]).setValue (m_p.x, m_p.y, 0);
        
        if (event.m_type == Event.MOUSE_DRAGGED) {
            boolean isInside = event.isInside (m_region);
            if (m_isOver != isInside) {
                ((SFBool)m_field[2]).setValue(m_isOver = isInside); // isOver
            }
        } else if (event.m_type == Event.MOUSE_RELEASED) {
            ((SFBool)m_field[1]).setValue(false); // isActive
            if (m_isOver) {
                ((SFBool)m_field[2]).setValue(m_isOver = false); // isOver
                ((SFTime)m_field[3]).setValue (c.time); // touchTime
            }
            m_sensedNode = null;
        } else { // Event can only be MOUSE_PRESSED
            ((SFBool)m_field[1]).setValue(true); // isActive
            ((SFBool)m_field[2]).setValue(m_isOver = true); // isOver
        }
        m_region = null;
    }

    boolean compose (Context c, Region clip, boolean forceUpdate) {
        if (m_isUpdated) {
            if (m_modeChanged) {
                m_modeChanged = false;
                String mode = ((SFString)m_field[5]).getValue().toLowerCase();
                m_grabAll = mode.equals("graball");
            }
            if (m_stateChanged) {
                m_stateChanged = false;
                if (c.m_groupingNode != null) {
                    if (((SFBool)m_field[0]).getValue ()) {
                        c.m_groupingNode.register(this);
                    } else {
                        c.m_groupingNode.unregister(this);
                    }
                }
            }
            m_isUpdated = false;
        }
        return false;
    }

    public void fieldChanged (Field f) {
        if (f == m_field[0]) { // 
            m_stateChanged = true;
        } else if (f == m_field[5]) { // cannot be anything else
            m_modeChanged = true;
        } 
        m_isUpdated = true;
    }

}
    
