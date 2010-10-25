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

public class TimeSensor extends Node implements Activable {
    SFFloat m_fraction;
    int m_startTime, m_restartTime, m_stopTime, m_cycleInterval, m_ci2;
    boolean m_loop,  m_started;
    int m_initTime, m_baseTime, m_lastFrac;

    TimeSensor () {
        super (7);
        //System.out.println ("TimeSensor created");
        m_field[0] = new SFTime (0, this); // startTime
        m_field[1] = new SFTime (0, this); // stopTime
        m_field[2] = new SFBool (false, this); // loop 
        m_field[3] = new SFTime (1<<16, this); // cycleInterval
        m_field[4] = m_fraction = new SFFloat (0, null); // fraction
        m_field[5] = new SFBool (false, null); // isActive
        m_field[6] = new SFTime (0, null); // cycleTime

        m_started = false;
    }

    void start (Context c) {
        fieldChanged (m_field[0]);
        fieldChanged (m_field[1]);
        fieldChanged (m_field[2]);
        fieldChanged (m_field[3]);
        c.scene.register (this);
        m_restartTime = -1;
    }

    public void fieldChanged (Field f) {
        if (f == m_field [0]) {
            if (!m_started) {
                m_startTime = ((SFTime)f).getValue ();
            } else {
                m_restartTime = ((SFTime)f).getValue ();
            }
        } else if (f == m_field [1]) {
            m_stopTime = ((SFTime)f).getValue ();
        } else if (f == m_field [2]) {
            m_loop = ((SFBool)f).getValue ();
        } else if (f == m_field [3]) {
            m_cycleInterval = ((SFTime)f).getValue ();
            m_ci2 = FixFloat.time2fix(m_cycleInterval);
        }
    }

    void stop (Context c) {
        c.scene.unregister (this);
    }

//#ifdef MM.pause
    public int getWakeupTime (int time) {
        if (m_started) { // implies that time >= m_startTime
            if (m_fraction.m_root != null) { // TS with listeners
                return MyCanvas.SLEEP_CANCELED;
            } else if (m_stopTime > m_startTime) { // TS can stop on m_stopTime
                if (time >= m_stopTime) {  // TS will stop now
                    return MyCanvas.SLEEP_CANCELED;
                } else { // TS will stop in the future
                    return m_loop ? m_stopTime : Math.min(m_stopTime, m_startTime+m_cycleInterval);
                }
            } else { // stopTime is invalid
                if (m_loop) { // TS will never stop, wakeup for next cycleTime
                    return time + m_cycleInterval - (time - m_startTime) % m_cycleInterval;
                } else if (time - m_startTime >= m_cycleInterval) { // TS will stop now
                    return MyCanvas.SLEEP_CANCELED;
                } else { // TS will stop for next cycleTime
                    return m_startTime + m_cycleInterval;
                }
            }
        } else if (isActivable(time)) {
            return MyCanvas.SLEEP_CANCELED; // TS will restart now, do not sleep
        } else if (m_startTime > time && isActivable(m_startTime)) {
            return m_startTime; // sleep until next timer start
        }
        return MyCanvas.SLEEP_FOREVER;
    }
//#endif

    protected boolean isActivable (int time) { //, int startTime, int stopTime, int cycleInterval, boolean loop) {
        if (time >= m_startTime) { 
            if (time >= m_stopTime && m_stopTime > m_startTime) {
                return (false);
            }
            if (m_loop == true || (time - m_startTime) < m_cycleInterval) {
                return (true); 
            }
        }
        return (false);
    }
    
    protected boolean isDesactivable (int time) { //, int startTime, int stopTime, int cycleInterval, boolean loop) {
        if (time >= m_stopTime && m_stopTime > m_startTime) {
            return (true);
        }
        if (m_loop == false && (time - m_startTime) >= m_cycleInterval) {
            return (true);
        }
        return (false);

    }
    
    int getFraction (int time) {
        return FixFloat.fixDiv (FixFloat.time2fix(time - m_baseTime), m_ci2) & 0xFFFF;
    }

    public boolean activate (Context c) {
        int time = c.time;
        if (m_started) {
            if (m_restartTime == m_stopTime) {
                m_startTime = m_stopTime;
            }
            if (isDesactivable (time)) {
                m_started = false;
                m_fraction.setValue(1<<16);
                m_lastFrac = -1; // delay end of timer
                MyCanvas.composeAgain = true;
            } else {
                int frac = getFraction(time);
                m_fraction.setValue(frac);
                if (frac < m_lastFrac) {
                    ((SFTime)m_field[6]).setValue (time);
                }
                m_lastFrac = frac;
            }
        } else if (m_lastFrac < 0) { // process delayed end of timer
            ((SFTime)m_field[6]).setValue (time);
            ((SFBool)m_field[5]).setValue (false);
            m_lastFrac = 0;
        } else if (isActivable (time)) {
            m_baseTime = time;
            m_started = true;
            m_fraction.setValue(getFraction(time));
            m_lastFrac = 0;
            ((SFBool)m_field[5]).setValue (true);
        }
        return false;
    }
}
