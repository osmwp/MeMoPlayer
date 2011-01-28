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

public class MediaControl extends Node {
    Scene m_scene;
    Loadable m_loadable;
    boolean m_urlChanged, m_mediaStartChanged, m_volumeChanged, m_pauseChanged,m_fullscreenChanged;
    int m_initTime;

    static final int IDX_URL               = 0;
    static final int IDX_MEDIA_START_TIME  = 1;
    static final int IDX_MEDIA_STOP_TIME   = 2;
    static final int IDX_LOOP              = 3;
    static final int IDX_PRE_ROLL          = 4;
    static final int IDX_IS_PRE_ROLLED     = 5;
    static final int IDX_VOLUME            = 6;
    static final int IDX_PLAY_PAUSE        = 7;
    static final int IDX_FULLSCREEN        = 8;
    static final int IDX_COUNT             = 9;

    MediaControl () {
        super (IDX_COUNT);
        //System.out.println ("MediaControl created");
        m_field[IDX_URL]              = new MFString (this);     // url
        m_field[IDX_MEDIA_START_TIME] = new SFTime (0, this);    // mediaStartTime
        m_field[IDX_MEDIA_STOP_TIME]  = new SFTime (0, this);    // mediaStopTime
        m_field[IDX_LOOP]             = new SFBool (false, null);// loop
        m_field[IDX_PRE_ROLL]         = new SFBool (true, null); // preRoll
        m_field[IDX_IS_PRE_ROLLED]    = new SFBool (true, null); // isPreRolled
        m_field[IDX_VOLUME]           = new SFInt32 (50, this);  // volume 0-100
        m_field[IDX_PLAY_PAUSE]       = new SFBool (false, this);// play/pause
        m_field[IDX_FULLSCREEN]       = new SFBool (false, this);// fullscreen
    }

    void start (Context c) {
        // fieldChanged (m_field[0]);
        // fieldChanged (m_field[1]);
        // fieldChanged (m_field[2]);
        // fieldChanged (m_field[6]);

        fieldChanged (m_field[IDX_URL]);
        //fieldChanged (m_field[IDX_MEDIA_START_TIME]);
        //fieldChanged (m_field[IDX_MEDIA_STOP_TIME]);
        fieldChanged (m_field[IDX_VOLUME]);
    }

    void stop (Context c) {
    }

    boolean compose (Context c, Region clip, boolean forceUpdate) {
        boolean updated = forceUpdate || m_isUpdated;
        
        if (m_isUpdated) {
            //m_isUpdated = false;
            if (m_urlChanged) {
                m_urlChanged = false;
                String url = ((MFString) m_field[IDX_URL]).getValue(0);
                if (url == null || url.length() == 0) {
                    m_loadable = null;
                } else {
                    m_loadable = c.findLoadable(url);
                    if (m_loadable != null) {
                        m_isUpdated = false;
                    }
                }
                Logger.println ("m_urlChanged");
            } 
            if( m_mediaStartChanged ) {
                if (m_loadable != null) {
                    m_mediaStartChanged = false;
                    m_isUpdated = false;
                    long mediaTime = ((SFTime) m_field[IDX_MEDIA_START_TIME]).getValue();
                    if (m_loadable.getState() == Loadable.EOM) {
                        ((MediaObject) m_loadable).start();
                    } else {
                    ((MediaObject) m_loadable).setMediaTimePos(1000*mediaTime);
                    }
                    Logger.println ("m_mediaStartChanged");
                }
            }
            if (m_volumeChanged) {
                if (m_loadable != null) {
                    m_volumeChanged = false;
                    m_isUpdated = false;
                    int volume = ((SFInt32) m_field[IDX_VOLUME]).getValue();
                    ((MediaObject) m_loadable).setVolume(volume);
                    Logger.println ("m_volumeChanged "+volume);
                }
            } 
            if (m_pauseChanged) {
                if (m_loadable != null) {
                    m_pauseChanged = false;
                    m_isUpdated = false;
                    boolean pause = ((SFBool)m_field[IDX_PLAY_PAUSE]).getValue ();
                    ((MediaObject) m_loadable).setPause (pause);
                    Logger.println ("m_pauseChanged "+pause);
                }
            }
            if( m_fullscreenChanged ) {
                if (m_loadable != null) {
                    m_fullscreenChanged = false;
                    m_isUpdated = false;
                    boolean fs = ((SFBool)m_field[IDX_FULLSCREEN]).getValue ();
                    ((MediaObject) m_loadable).setFullScreen (fs);
                    Logger.println ("m_fullscreenChanged"+fs);
                }
            }
        }
        return updated;
    }

    public void fieldChanged (Field f) {
        m_isUpdated = true;
        if (f == m_field[IDX_URL]) {
            m_urlChanged = true;
        }
        else if (f == m_field[IDX_MEDIA_START_TIME]) {
            m_mediaStartChanged = true;
        }
        else if (f == m_field[IDX_VOLUME]) {
            m_volumeChanged = true;
        }
        else if (f == m_field[IDX_PLAY_PAUSE]) {
            m_pauseChanged = true;
        }
        else if (f == m_field[IDX_FULLSCREEN]) {
            m_fullscreenChanged = true;
        }
    }

}
