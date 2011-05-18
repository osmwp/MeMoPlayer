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

public class MovieTexture extends MediaNode  {

//#ifdef BlackBerry
    TransportDetective m_trDetective = new TransportDetective();
//#endif

    int m_srcWidth=-1;
    int m_srcHeight=-1;

    MovieTexture () {
        super (5,MediaObject.VIDEO, MediaObject.PLAYBACK);
        //System.out.println ("MovieTexture created");
        m_field[3] = new SFVec2f(-1,-1,null); // movieSize : video src size
        m_field[4] = new SFBool(false,this); // fullScreen : display video full screen or not
    }


    boolean specificCompose (Context c, Region clip, boolean forceUpdate) {
    	if( m_media != null ) {
	    	if( (m_srcWidth != m_media.m_srcWidth) || (m_srcHeight != m_media.m_srcHeight) ) {
	    		updateVideoSize(m_media.m_srcWidth,m_media.m_srcHeight);
	    	}
    	}
    	
        /*if (m_media != null){
          if (c.time >= ((SFTime)m_field[1]).getValue ()) {
          if (m_media.getState () == MediaObject.STATE_READY){
          //c.exclude.setInt (0, 0, 176, 144);
          //m_media.play ();
          }
          }
          }*/
        return (false);
    }

    public void updateVideoSize(int width, int height){
    	SFVec2f vec = (SFVec2f)m_field[3];
    	width  = width<<16;
    	height = height<<16;
    	if( (vec.m_x != width ) || (vec.m_y != height) ) {
    		vec.setValue(width,height);
    	}
    }

    public void fieldChanged (Field f) {
        // Logger.println("+MovieTexture fieldChanged");
        if (f == m_field[4]) {
            if (m_media!=null) {
                boolean fs = ((SFBool)m_field[4]).getValue();
                // Logger.println("fullScreen"+fs);
                m_media.setFullScreen(fs);
            }
            return;
        }
        // Logger.println("-MovieTexture fieldChanged");
        super.fieldChanged(f);
    }

//#ifdef BlackBerry
    public net.rim.device.api.servicebook.ServiceRecord getWrapServiceRecord() {
        return m_trDetective.getWap2ServiceRecord();
    }
//#endif
}
