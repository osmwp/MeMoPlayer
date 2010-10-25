//#condition api.mm
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

public class Sound2D extends Node {
    Node m_source;
    int m_intensity;

    Sound2D () {
        super (2);
        //System.out.println ("Sound2D created");
        m_field[0] = new SFFloat (1<<16, this); // intensity
        m_field[1] = new SFNode (null); // source
    }

    void start (Context c) {
        fieldChanged (m_field[0]);
        fieldChanged (m_field[1]);
        if (m_source != null) { m_source.start (c); }
    }

    void stop (Context c) {
        if (m_source != null) { 
            m_source.stop (c); }
    }

    boolean compose (Context c, Region clip, boolean forceUpdate) {
        if (m_source != null) {
            m_source.compose (c, clip, false);
            if (m_isUpdated && m_source != null) {
                ((AudioClip)m_source).setVolume (m_intensity);
            }
        } 
        m_isUpdated = false;
        return false;
    }

    public void fieldChanged (Field f) {
        m_isUpdated = true;
        if (f == m_field[0]) {
            m_intensity = ((SFFloat)f).getValue ();
        } else {
            m_source = ((SFNode)f).getValue ();
        }
    }

}
