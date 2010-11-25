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

//#ifndef BlackBerry
import javax.microedition.lcdui.Image;
//#endif

public class ImageTexture extends Node implements ImageRequester {
    boolean m_isPng = false;
    boolean m_reloadIC = false;
    int m_state = Loadable.OK;
    String m_url = "";
    ImageContext m_ic;
    DataLoader m_dl;
    
    ImageTexture () {
        super (5);
        m_field[0] = new MFString (this); // url
        m_field[1] = new SFVec2f (0, 0, null); // size
        m_field[2] = new MFString (this); // alternateUrl
        m_field[3] = new SFVec2f (0, 0, this); // newSize
        m_field[4] = new SFBool (false, null); // filter
    }
    
    void start (Context c) {
        fieldChanged (m_field [0]);
    }

    void stop (Context c) {
        if (m_ic != null) {
            m_ic.release();
            m_ic = null;
        }
        if (m_dl != null) {
            m_dl.release();
            m_dl = null;
        }
    }

    boolean isTransparent () { return m_isPng; }
    
    boolean compose (Context c, Region clip, boolean forceUpdate) {
       if (m_isUpdated) { // Only need to re-decode image on fieldChanged
                          // (e.g. not when Shape translates/scales) !
            String url = ((MFString)m_field[0]).getValue (0);
            if (!url.equals (m_url)) {
                stop (null); // force redisplay
                m_url = url;
                if (url.length() != 0 && !c.checkImage (url)) { // not local
                    m_state = Loadable.LOADING;
                    m_dl = new DataLoader (url, this, c);
                    // Dataloader can call back imageReady immediately if image is cached 
                    // Display alternate image while main image is loading if avail
                    if (m_state == Loadable.LOADING) {
                        url = ((MFString)m_field[2]).getValue (0);
                        if (url.length() != 0 && c.checkImage (url)) {
                            m_url = url;
                        }
                    }
                }
            }
        }
       
        // Check isUpdated() only after as it resets m_isUpdated 
        boolean updated = isUpdated (forceUpdate);
        
        if (m_dl != null) {
            if (m_state == Loadable.ERROR) {
                // If error during loading, display error alternate image
                String url = ((MFString)m_field[2]).getValue (1);
                if (url.length() != 0 && c.checkImage (url)) {
                    m_url = url;
                }
                stop (null); // force redisplay
            } else if (m_state == Loadable.LOADED) {
                m_url = m_dl.m_url;
                stop (null); // force redisplay
            }
        }
        
        if (m_ic == null || m_reloadIC) {
            m_reloadIC = false;
            m_isPng = m_url.endsWith ("png");
            m_ic = ImageContext.get (c, m_url);
            // Force to new size
            int maxW = ((SFVec2f)m_field[3]).m_x>>16;
            int maxH = ((SFVec2f)m_field[3]).m_y>>16;
            m_ic.setFilterMode (((SFBool)m_field[4]).getValue ()); // MUST be BEFORE setMaxSize call to be taken in account during scaling!!
            if (maxW < 0 && maxH < 0) {
                maxW *= -1; maxH *= -1;
                if (m_ic.m_srcW > maxW || m_ic.m_srcH > maxH) {
                    m_ic.setMaxSize (c, maxW, maxH, true);
                }
            } else {
                m_ic.setMaxSize (c, maxW, maxH, false);
            }
            // Notify new size of image
            ((SFVec2f)m_field[1]).setValue (m_ic.m_srcW<<16, m_ic.m_srcH<<16);
            updated = true;
        }
        
        // Always set IC to AC on update in case IT is reused by the DEF/USE mechanism 
        if (updated) {
            c.setCurrentImage (m_ic);
        }
        return updated;
    }

    public void fieldChanged (Field f) {
        m_isUpdated = true;
        if (m_field[3] == f) { // updating newSize forces refresh of image
            m_reloadIC = true;
        }
    }

    public void imageReady (Image image) {
        m_state = image != null ? Loadable.LOADED : Loadable.ERROR;
        
    }
}
