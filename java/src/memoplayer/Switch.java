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

public class Switch extends Node {
    MFNode m_choice;
    Node m_node;
    
    Switch () {
        super (2);
        // //System.out.println ("Switch created");
        m_field[0] = new SFInt32 (0, this); // whichChoice
        m_field[1] = new MFNode (); // choices 
    }
    
    void start (Context c) {
        m_choice = (MFNode)m_field[1];
        //fieldChanged (m_field[0]);
        checkForNodeToStart (c);
    }
    
    void stop (Context c) {
        if (m_node != null) {
            m_node.stop (c);
            m_node = null;
        }
    }

    // needed for sharing script bytecodes
    void destroy (Context c) {
      if (m_choice != null) {
        for (int i = 0; i < m_choice.m_size; i++) {
            m_choice.m_node[i].destroy (c);
        }
        m_choice = null;
      }
    }

    void render (Context c) {
        if (m_node != null) {
            m_node.render (c);
        }
    }
    
    void checkForNodeToStart (Context c) {
        int i = ((SFInt32)m_field[0]).getValue ();
        if (m_node != null) { 
            m_node.stop (c);
        }
        if (m_choice != null) { // can be null if the switch is not started (i.e in an inactive branch of another switch)
            m_node = (i >= 0 && i < m_choice.m_size) ? m_choice.m_node[i] : null;
        }
        if (m_node != null) { 
            m_node.start (c); 
        }
    }

    boolean compose (Context c, Region clip, boolean forceUpdate) {
        //try{//FTE test
        boolean updated = forceUpdate || m_isUpdated;
        if (m_isUpdated) {
            checkForNodeToStart (c);
        }
        if (updated) {
            clip.setInt (0, 0, c.width, c.height);
            m_isUpdated = false;
        }
        if (m_node != null) {
            return m_node.compose (c, clip, forceUpdate);
        }
        return updated;
        //}catch(Exception e){e.printStackTrace();return true;}
    }

    public void fieldChanged (Field f) {
        m_isUpdated = true;
    }

}
