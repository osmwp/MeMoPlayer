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

public class InputSensor extends Node implements Activable {
    int [] m_keyCode;
    int m_nbCodes;
    boolean m_registered = false, m_grabInput;
    boolean m_bUseKeyboard = false;
    Scene m_scene;

    InputSensor (int n) {
        super (n);
        m_field[0] = new SFString ("", this); // url
        m_field[1] = new SFTime (0, null);    // eventTime
        m_field[2] = new SFBool (true, this);    // activate
        m_field[3] = new SFBool (false);    // grabFocus
    }

    InputSensor () {
        this (4);
    }

    void start (Context c) {
        m_scene = c.scene;
        //MCP: m_grabInput must be set *before* field 'activate' is checked !
        m_grabInput = ((SFBool)m_field[3]).getValue ();
        //Logger.println ("InputSensor.start: t="+c.time+" TS="+c.m_timeShift);
        fieldChanged (m_field[0]);
        fieldChanged (m_field[2]);
    }

    void stop (Context c) {
        if (m_registered) {
            m_registered = false;
            m_scene.unregister (this);
            //System.err.println ("IS.unregister: "+this);
        }
        m_scene = null;
    }
    
    public boolean listenTo (Event e) {
        if (e.m_type == Event.KEY_PRESSED) {
            for (int i = 0; i < m_nbCodes; i++) {
                if (e.m_key == m_keyCode [i]) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean activate (Context c) {
        if (c.event.m_grabSensor == this || listenTo(c.event)) {
            ((SFTime)m_field[1]).setValue (c.time);
            return m_grabInput;
        }
        return false;
    }

    public void fieldChanged (Field f) {
        if (f == m_field[0]) {
            String s = ((SFString)m_field[0]).getValue ();
            m_nbCodes = s.length();
            m_keyCode = new int [m_nbCodes];
            m_bUseKeyboard = false;
            for (int i = 0; i < m_nbCodes; i++) {
                m_keyCode[i] = s.charAt (i);
                if(m_keyCode[i]=='@')
                    m_bUseKeyboard = true;
            }
        } else if (f == m_field[2] && m_scene != null) {
            if (((SFBool)f).getValue ()) {
                if (!m_registered) {
                    m_registered = true;
                    //System.err.println ("IS.register: "+this+"/"+((SFString)m_field[0]).getValue ()+" : "+m_grabInput);
                    m_scene.register (this);
                }
            } else {
                if (m_registered) {
                    m_registered = false;
                    //System.err.println ("IS.unregister: "+this+"/"+((SFString)m_field[0]).getValue ()+" : "+m_grabInput);
                    m_scene.unregister (this);
                }
            }
        }
    }

}
