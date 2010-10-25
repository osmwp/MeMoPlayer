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
import javax.microedition.lcdui.Image;

interface ImageRequester {
    void imageReady (Image image);
}

interface TextRequester {
    void textReady (String string);
}

class DataLoader {
    static DataLoader s_root = null;

    File m_file;
    Scene m_scene;
    String m_url;
    ImageRequester m_imageRequester;
    TextRequester m_textRequester;
    DataLoader m_next;

    final static void check () {
        if (s_root != null) {
            s_root = s_root.run ();
        }
    }

    void notifyRequester (byte [] data) {
        if (m_imageRequester != null) {
            Image image = null;
            if (data != null) {
                m_scene.addData (m_url, data, Decoder.MAGIC_IMAGE, true);
                image = m_scene.getImage (m_url);
                if (image == null) { // error during decoding => backlist & remove
                    m_scene.addBlacklist (m_url);
                    m_scene.removeData (m_url);
                }
            }
            m_imageRequester.imageReady (image);
        } else if (m_textRequester != null) {
            //Logger.println ("DataLoader: notifyRequester for text "+data);
            m_textRequester.textReady (data == null ? "" : new String (data)); 
        }
    }

    DataLoader (String url, ImageRequester ir, Context c) {
        m_imageRequester = ir;
        setup (url, c);
    }

    DataLoader (String url, TextRequester tr, Context c) {
        m_textRequester = tr;
        //Logger.println ("DataLoader: request text "+url);
        setup (url, c);
    }

    void release () {
        m_file = null; // asks to clean dataloader on next check
        m_imageRequester = null;
        m_textRequester = null;
        m_scene = null;
    }
    
    void setup (String url, Context c) {
        if (url == null || url.length() == 0) { 
            return;
        }
        //MCP: check if image is in DataLink without requesting it
        // (to prevent useless Image.createImage() calls)
        if (m_imageRequester != null && c.checkImage (url)) {
            m_imageRequester.imageReady (c.getImage (url)); 
            return;
        }

        //MCP: Direct cache loading
        if (url.startsWith("cache://")) {
            String myUrl = url.substring (8);
            // Ignore the optional source url after the comma
            int i = myUrl.indexOf(',');
            if (i != -1) {
                myUrl = myUrl.substring(0, i);
            }
            //i = CacheManager.getManager().find (myUrl);
            byte [] data = CacheManager.getManager().getByteRecord(myUrl);
            if (data != null) { // we have it on the cache
                m_url = url;
                m_scene = c.scene;
                notifyRequester (data);
                return;
            }
        }
 
        // not available locally, trying to load it
        if (c.scene.isBlacklisted (url)) { // already known as a incorrect url
            notifyRequester (null); 
            return;
        }

        //Logger.println ("DataLoader.setup: Queuing for '"+url+"'");
        // queue this DataLoader
        m_file = c.scene.getFileQueue().getFile(url);
        m_scene = c.scene;
        m_url = url;
        m_next = s_root;
        s_root = this;
    }

    public DataLoader run () {
        if (m_file != null) {
            switch (m_file.getState ()) {
            case Loadable.LOADED:
                if (m_file.getData () != null) { // Add image to scene
                    notifyRequester (m_file.getData()); 
                    break;
                } // no data so blacklist the url by continuing below
            case Loadable.ERROR:
                m_scene.addBlacklist (m_file.getName());
                notifyRequester (null); 
                break;
            default:
                // we keep this DataLoader 
                m_next = m_next == null ? null : m_next.run ();
                return this;
            }
            m_file = null;
        }
        return m_next == null ? null : m_next.run (); // remove this DataLoader from the list
    }

}
