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

public class Shape extends Node {
    
    AppearanceContext m_appContext;
    Appearance m_appearanceNode;
    Node m_geometryNode;
   
    Shape () {
        super (2);
        //System.out.println ("Shape created");
        m_field[0] = new SFNode (); // appearance
        m_field[1] = new SFNode (); // geometry
        m_appContext = new AppearanceContext ();
    }

    void start (Context c) {
        fieldChanged (m_field[0]);
        fieldChanged (m_field[1]);
        if (m_appearanceNode != null) { m_appearanceNode.start (c); }
        if (m_geometryNode != null) { m_geometryNode.start (c); }
    }

    void stop (Context c) {
        if (m_appearanceNode != null) { 
            m_appearanceNode.stop (c); }
        if (m_geometryNode != null) { 
            m_geometryNode.stop (c); 
        }
    }

    void destroy (Context c) {
        if (m_appearanceNode != null) { m_appearanceNode.destroy (c); }
        if (m_geometryNode != null) { m_geometryNode.destroy (c); }
    }
    
    boolean compose (Context c, Region clip, boolean forceUpdate) {
        boolean updated = isUpdated (forceUpdate);
        c.ac = m_appContext;
        m_appContext.m_hasMaterial = false;
        if (m_appearanceNode != null) {
            updated |= m_appearanceNode.compose (c, clip, updated);
        } else {
            m_appContext.m_color = 0xFFFFFF;
            m_appContext.m_transparency = 0;
            m_appContext.m_image = null;
        }
        if (m_geometryNode != null) {
            m_geometryNode.m_region = m_appContext.m_region;
            updated |= m_geometryNode.compose (c, clip, updated);
        }
        c.ac = null;
        return updated;
    }

    public void fieldChanged (Field f) {
        m_isUpdated = true;
        if (f == m_field[0]) {
            m_appearanceNode = (Appearance)((SFNode)f).getValue ();
        } else {
            m_geometryNode = ((SFNode)f).getValue ();
        }
    }

}
