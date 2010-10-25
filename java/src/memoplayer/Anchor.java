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

public class Anchor extends Node {
    private Context m_context;

    Anchor () {
        super (3);
        //System.out.println ("Anchor created");
        m_field[0] = new MFString (); // url
        m_field[1] = new MFString (); // parameter
        m_field[2] = new SFBool (false, this); // activate
    }
    
    void start (Context c) {
        m_context = c;
        m_isUpdated = false;
    }    
    boolean compose (Context c, Region clip, boolean forceUpdate) {
        if (m_isUpdated) {
            m_isUpdated = false;
            String url = ((MFString)m_field[0]).getValue (0);
            if (url.equals("") || url.endsWith ("m4m")) {
                m_context.newUrl = url;
                String target = ((MFString)m_field[1]).getValue (0);
                if (target == null) {
                    m_context.newUrlCount = -1;
                } else if (target.equals ("self")) {
                    m_context.newUrlCount = 0;
                } else if (target.equals ("parent")) {
                    m_context.newUrlCount = 1;
                } else {
                    m_context.newUrlCount = -1;
                }
            } else {
                MiniPlayer.openUrl(url);
            }
        }
        return (forceUpdate);
    }

    public void fieldChanged (Field f) {
        if (((SFBool)f).getValue()){
            m_isUpdated = true;
        }
    }

}
