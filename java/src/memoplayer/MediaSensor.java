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

public class MediaSensor extends Node {
    Loadable m_loadable;
    int m_duration;
    int m_current;
    int m_state;

    final static private int IDX_URL=0;
    final static private int IDX_MEDIA_TIME=1;
    final static private int IDX_MEDIA_DURATION=2;
    final static private int IDX_ISACTIVE=3;
    final static private int IDX_STATUS=4;

    MediaSensor () {
        super (5);
        //System.out.println ("MediaSensor created");
        m_field[IDX_URL]            = new MFString (this);  // url
        m_field[IDX_MEDIA_TIME]     = new SFTime (0);       // mediaCurrentTime
        m_field[IDX_MEDIA_DURATION] = new SFTime (0);       // mediaDuration
        m_field[IDX_ISACTIVE]       = new SFBool (false);   // isActive
        m_field[IDX_STATUS]         = new MFString ();      // status
    }

    void start (Context c) {
        fieldChanged (m_field[IDX_URL]);
    }

    void stop (Context c) {
    }

    boolean compose (Context c, Region clip, boolean forceUpdate) {
        if (m_isUpdated) {
            String name = ((MFString)m_field[IDX_URL]).getValue (0);
            if (name == null || name.length () == 0) {
                m_loadable = null;
                m_isUpdated = false;
            }
            m_loadable = c.findLoadable (name);
            if (m_loadable != null) {
                m_isUpdated = false;
            }
        }
        if (m_loadable != null) {
            int state = m_loadable.getState ();
            if (state != m_state) {
                m_state = state;
                MFString s = (MFString)m_field[IDX_STATUS];
                s.resize (1);
                switch (state) {
                case Loadable.OPENING:
                    s.setValue (0, Loadable.STATE_MSG[state]);
                    break;
                case Loadable.LOADING:
                case Loadable.PLAYING:
                    s.setValue (0, Loadable.STATE_MSG[state]);
                    ((SFBool)m_field[IDX_ISACTIVE]).setValue (true);
                    int duration = m_loadable.getDuration ();
                    if (duration != m_duration) {
                        ((SFTime)m_field[IDX_MEDIA_DURATION]).setValue (m_duration = duration);
                    }
                    break;
                case Loadable.BUFFERING:
                    ((SFBool)m_field[IDX_ISACTIVE]).setValue (false);
                    s.setValue (0, Loadable.STATE_MSG[state]);
                    break;
                case Loadable.PAUSED:
                    s.setValue (0, Loadable.STATE_MSG[state]);
                    break;
                case Loadable.LOADED:
                case Loadable.STOPPED:
                    ((SFBool)m_field[IDX_ISACTIVE]).setValue (false);
                    s.setValue (0, Loadable.STATE_MSG[state]);
                    break;
                case Loadable.ERROR:
                    s.setValueSilently (1, m_loadable.getErrorMessage ());
                    s.setValue (0, Loadable.STATE_MSG[state]);
                    break;
                default:
                    s.setValue (0, Loadable.STATE_MSG[state]);
                }
            }
            if (m_state == Loadable.PLAYING || state == Loadable.LOADING) {
                int v = m_loadable.getCurrent ();
                if (v != m_current) {
                    m_current = v;
                    ((SFTime)m_field[IDX_MEDIA_TIME]).setValue (v);
                }
            }
        }
        return false;
    }

    public void fieldChanged (Field f) {
        m_state = Loadable.READY;
        m_isUpdated = true;
        m_duration = -2;
        m_current = -1;
    }

}
