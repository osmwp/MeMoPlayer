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

public class KeySensor extends InputSensor implements Activable {
    // m_nextRepeatTime: -1 : not pressed
    //                    0 : pressed but no repeat time
    //                   >0 : pressed and value is time of next repeat
    private int m_nextRepeatTime = -1;
    
    KeySensor () {
        super (9);
        //System.out.println ("KeySensor created");
        m_field[4] = new SFTime (0);     // releaseTime
        m_field[5] = new SFString ("");  // activeKey
        m_field[6] = new SFTime (0);  // repeatInterval
        m_field[7] = new SFTime (0);  // repeatTime
        m_field[8] = new SFTime (0);  // repeatDelay
   }
 
    boolean compose (Context c, Region clip, boolean forceUpdate) {
        if (m_registered) {
            if (m_nextRepeatTime > 0 && c.time > m_nextRepeatTime) {
                m_nextRepeatTime = c.time + ((SFTime)m_field[6]).getValue ();
                ((SFTime)m_field[7]).setValue (c.time);
            }
        } else if (m_nextRepeatTime >= 0) {
            //MCP: Emulate key release when KeySensor is stopped while a key is pressed
            ((SFTime)m_field[4]).setValue (c.time);
            m_nextRepeatTime = -1;
        }
        return false;
    }
    
    public boolean listenTo (Event e) {
        // KeySensor only reacts to a release key event
        // when already pressed (and vice versa)
        final boolean pressed = m_nextRepeatTime >= 0;
        if (e.isReversedKeyEvent (pressed)) {
            //SKA: use of device with keyboard like blackberry ...
            if (m_bUseKeyboard) {
                return e.m_key < 0 || (e.m_key >= '0' && e.m_key <= '9') || e.m_key=='*' || e.m_key=='#';
            } else if (e.m_key > 0) {
                for (int i = 0; i < m_nbCodes; i++) {
                    if (e.m_key == m_keyCode [i]) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean activate (Context c) {
        Event e = c.event;
        if (m_grabInput && e.m_grabSensor == this || listenTo(e)) {            
            //SKA: use of device with keyboard like blackberry ...
            // System.out.println("KeySensor.activate KEY_PRESSED:"+(e.m_type == Event.KEY_PRESSED)+" SPECIAL_KEY_PRESSED:"+(e.m_type == Event.SPECIAL_KEY_PRESSED)+" m_bUseKeyboard:"+m_bUseKeyboard);
            char key = e.m_key < 0 ? (char)-e.m_key : (char)e.m_key;
            if (e.m_type == Event.KEY_PRESSED) {
                ((SFString)m_field[5]).setValue (String.valueOf (key));
                ((SFTime)m_field[1]).setValue (c.time);
                int interval = ((SFTime)m_field[6]).getValue ();
                if (interval > 0) {
                    int delay = ((SFTime)m_field[8]).getValue ();
                    if (delay > 0) {
                        interval = delay;
                    }
                    m_nextRepeatTime = c.time + interval;
                } else {
                    m_nextRepeatTime = 0; 
                }
            } else if (m_nextRepeatTime >= 0) { //MCP: Only release when key was previously pressed
                ((SFString)m_field[5]).setValue (String.valueOf (key));
                ((SFTime)m_field[4]).setValue (c.time);
                m_nextRepeatTime = -1;
            }
            return m_grabInput;
        }
        return false;
    }

//#ifdef MM.pause
    int getWakeupTime (int time) {
        //Logger.println ("Checking KS for Wakeup: "+m_nextRepeatTime+" "+time);
        if (m_nextRepeatTime > 0) {
            if (m_nextRepeatTime >= time) {
                return m_nextRepeatTime;
            }
            return MyCanvas.SLEEP_CANCELED;
        }
        return MyCanvas.SLEEP_FOREVER;
    }
//#endif
}
