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

public class MediaNode extends Node  {
    final static int STATE_ERROR = -1;
    final static int STATE_NEW = -1;
    final static int STATE_CREATED = -1;
    Region m_region;

    boolean m_urlChanged=false; 
    int m_startTime, m_restartTime, m_stopTime, m_type, m_recording;
    MediaObject m_media;

    MediaNode (int nbFields, int type, int recording) {
        super (nbFields);
        m_region = new Region ();
        m_region.set (-1, -1, -1, -1);
        m_type = type;
        m_recording = recording;
        m_field[0] = new MFString (this); // url
        m_field[1] = new SFTime (0, this);  // startTime
        m_field[2] = new SFTime ((-1)<<16, this);  // stopTime
    }

    MediaNode (int type, int recording) {
        this (3, type, recording);
    }

    MediaNode (int type) {
        this (3, type, MediaObject.PLAYBACK);
    }
    
    void start (Context c) { 
        //fieldChanged (m_field[0]);
        //m_media = new MediaObject ();
        c.scene.registerSleepy (this);
    }

    void stop (Context c) {
        if (m_media != null) {
            m_region.set (m_media.m_region);
            try {
                m_media.close ();
            } catch (Exception e) {
                System.err.println ("MediaNode.stop: Exception: '"+e+"'");
            }
            m_media = null;
        }
        c.scene.unregisterSleepy (this);
    }

    boolean specificCompose (Context c, Region clip, boolean forceUpdate) {
        return (false);
    }

    boolean compose (Context c, Region clip, boolean forceUpdate) {
        boolean updated = m_isUpdated | forceUpdate;
        if (m_isUpdated) {
            m_isUpdated = false;
            if (m_urlChanged) {
                if (m_media != null) {
                    m_region.set (m_media.m_region);
                    c.removeLoadable (m_media);
                    m_media.close ();
                    m_media = null;
                    m_urlChanged = false;
                }
            }
        }
        int time = c.time;
        //Logger.println ("Media:"+m_media+", time:"+time+", startTime"+m_startTime+", stopTime"+m_stopTime);
        if (m_media == null && time >= m_startTime && 
            (time < m_stopTime || (m_startTime >= m_stopTime || m_restartTime >= m_stopTime)) ) {
            m_startTime = m_restartTime;
            String s = ((MFString)m_field[0]).getValue (0);
            if (s != null & s.length() > 0) {
                //System.err.println ("MediaNode.compose: opening: '"+s+"'");
                m_media = new MediaObject (m_type, m_recording);
                if (m_media.m_region.x0 != -1) {
                    m_media.m_region.set (m_region);
                } 
                m_media.open (s, c.canvas, c.decoder);
                c.addLoadable (m_media);
            }
        } else if (m_media != null && time >= m_stopTime && m_stopTime > m_startTime) {
            c.removeLoadable (m_media);
            m_media.close ();
            m_media = null;
        }
        if (c.ac != null) {
            c.ac.m_mediaObject = m_media;
        }
        updated |= specificCompose (c, clip, updated);
  
        return updated;
    }

    public void fieldChanged (Field f) {
        m_isUpdated = true;
        if (f == m_field[0]) {
            m_urlChanged = true;
        } else if (f == m_field [1]) {
            if (m_media == null) {
                m_startTime = ((SFTime)f).getValue ();
            } else {
                m_restartTime = ((SFTime)f).getValue ();
            }
        } else if (f == m_field [2]) {
            m_stopTime = ((SFTime)f).getValue ();
        }
    }

//#ifdef MM.pause
    int getWakeupTime (int time) {
        if (m_media != null) { // prevent sleep when node has a media
            return MyCanvas.SLEEP_CANCELED;
        } else { // no media, checking for next start of node
            if (time < m_startTime) { // starts in the future, sleep until then
                return m_startTime;
            }
            // already started, but still no media
            if (time < m_stopTime || m_startTime >= m_stopTime || m_restartTime >= m_stopTime) {
                return MyCanvas.SLEEP_CANCELED; // node might try to start media on next compose
            }
            return MyCanvas.SLEEP_FOREVER;
        }
    }
//#endif
}
