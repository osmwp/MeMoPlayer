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

public class Appearance extends Node {
    Node m_material, m_texture;
    boolean m_isTransparent = false;

    Appearance () {
        super (2);
        //System.out.println ("Appearance created");
        m_field[0] = new SFNode ();    // material
        m_field[1] = new SFNode ();  // texture
    }

    void start (Context c) {
        fieldChanged (m_field[0]);
        fieldChanged (m_field[1]);
        if (m_material != null) { m_material.start (c); }
        if (m_texture != null) { 
            m_texture.start (c); }
    }
    
    void stop (Context c) {
        if (m_material != null) { 
            m_material.stop (c); 
        }
        if (m_texture != null) {
            m_texture.stop (c); 
        }
    }

    boolean compose (Context c, Region clip, boolean forceUpdate) {
        boolean updated = isUpdated (forceUpdate);
        if (m_material != null) {
            updated |= m_material.compose (c, clip, updated);
        } else {
            c.ac.m_color = 0xFFFFFF;
            c.ac.m_transparency = 0;
        }
        if (m_texture != null) {
            updated |= m_texture.compose (c, clip, updated);
        } else {
            c.ac.m_image = null;
        }
        return (updated);
    }

    public void fieldChanged (Field f) {
        m_isUpdated = true;
        if (f == m_field[0]) {
            m_material = ((SFNode)f).getValue ();
        } else {
            m_texture = ((SFNode)f).getValue ();
        }
    }

}
