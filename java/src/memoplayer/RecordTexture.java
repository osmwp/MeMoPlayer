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
import javax.microedition.media.Manager;

public class RecordTexture extends MediaNode  {
//#ifdef jsr.amms
    boolean m_stopped = true;
//#endif
    boolean m_singleShot = true;
    boolean m_enabled = false;

    protected final static int IDX_ENABLE     = 3;
    protected final static int IDX_ENCODING   = 4;
    protected final static int IDX_SINGLESHOT = 5;
    protected final static int IDX_FILE_URL   = 6;
    protected final static int RECORD_TEXTURE_FIELD_COUNT = 7;

    RecordTexture () {
        super (RECORD_TEXTURE_FIELD_COUNT, MediaObject.VIDEO, MediaObject.RECORDING);
        //System.out.println ("RecordTexture created");
        m_field[IDX_ENABLE]     = new SFBool (false, this); // enable
        m_field[IDX_ENCODING]   = new SFString ("");        // encoding
        m_field[IDX_SINGLESHOT] = new SFBool (true, this);  // singleShot
        m_field[IDX_FILE_URL]   = new MFString ();          // fileUrl
    }

    void start (Context c) {     
        // ((MFString)super.m_field[0]).setValue(0, "capture://audio_video");
        super.start (c);
        fieldChanged (m_field[IDX_SINGLESHOT]);
        fieldChanged (m_field[IDX_ENABLE]);
        //c.addLoadable (m_media);
    }

    boolean compose (Context c, Region clip, boolean forceUpdate) {
    	long composeTime = System.currentTimeMillis(); 
    	
    	if (m_enabled==false) {
            if (m_media != null) {
            	closeMedia(c);
        		return true;
            }
    		return false;
    	}

	    if (m_urlChanged) {
        	closeMedia(c);
            m_urlChanged = false;
    		return true;
	    }

        if (m_singleShot) {

        	// open media if needed
            if (m_media == null) {
        		m_recording = MediaObject.SNAPSHOT;
           		openMedia(c);
                return true;
            }

            if (c.ac != null) {
            	// Logger.println("RT update ac");
                c.ac.m_mediaObject = m_media;
            }
            if (m_media.m_region.x0 != -1) {
            	// Logger.println("RT update region");
                m_media.m_region.set (m_region);
            }

            int time = c.time;

            if ( ((time<m_startTime) || (m_startTime<0)) && ((time<m_restartTime) || (m_restartTime<0)) ) {
            	// nothing to do ...
            	return false;
            }

                m_media.setVisible (false);
                byte [] data = null;
                // try snapShot with desired size
            String encoding = ((SFString)m_field[IDX_ENCODING]).getValue();
            if( (encoding == null) || (encoding.length()==0) )
            	encoding = null;
            data = m_media.getSnapshot (encoding);
                if( data != null && data.length>0 ) {
                String filename = ((MFString)m_field[IDX_FILE_URL]).getValue(0);
                // Logger.println("Saving snapshot in: "+filename);
                if (filename.startsWith("cache://")) {
                	CacheManager.getManager().setRecord (filename.substring(8),data);
                } else {
                    File file = new File (filename, File.MODE_OVERWRITE); 
                    if (file!=null) {
                    if (file.writeAllBytes(data) == false) {
	                        Logger.println ("RT.compose: cannot write data to: "+filename);
                    }
                    file.close (Loadable.CLOSED);
                }
                }
            }
                c.clip.setInt (0, 0, c.width, c.height);
                m_media.setVisible (true);
            ((SFTime)m_field[IDX_START_TIME]).setValue ((-1)<<16);

            int duration = (int)(System.currentTimeMillis() - composeTime); 

            ((SFTime)m_field[IDX_STOP_TIME]).setValue (c.time+duration);
        } else {
//#ifdef jsr.amms

        	// open media if needed
            if (m_media == null) {
            	m_recording = MediaObject.RECORDING;
                openMedia(c);
                return false;
            }

            if (m_media.m_region.x0 != -1) {
                m_media.m_region.set (m_region);
            }

            int time = c.time;

            if ( ((time<m_startTime) || (m_startTime<0)) && ((time<m_restartTime) || (m_restartTime<0)) ) {
            	// nothing to do ...
            	return false;
            }

            // start record if not started
            if ( m_media.m_isRecording == false ) {
                String filename = ((MFString)m_field[IDX_FILE_URL]).getValue(0);
                // Logger.println("Saving record in: "+filename);
            	if ( m_media.startRecord(filename) == false ) {
            		// error starting record
                	m_stopped = true;
            	}
            } else {
            	// stop record if stop time reached
                if ( (time>=m_stopTime) && (m_stopTime>=0) ) {
                	m_media.stopRecord();
                	m_stopped = true;
                	// stop startTime
                    ((SFTime)m_field[IDX_START_TIME]).setValue ((-1)<<16);
            }
            }
//#endif
        }
        return false;
    }

    public void fieldChanged (Field f) {
        if (f == m_field[IDX_SINGLESHOT]) {
            m_singleShot = ((SFBool)f).getValue ();
            return;
        } else if (f == m_field [IDX_ENABLE]) {
            m_enabled = ((SFBool)f).getValue ();
            return;
        }
            super.fieldChanged(f);
            }
                
//#ifdef MM.pause
    int getWakeupTime (int time) {
    	// no need to be waken up
        return MyCanvas.SLEEP_FOREVER;
            }
//#endif
        }
