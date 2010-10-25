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

public class AppearanceContext {
    final static int TYPE_UNKNOWN = 0;
    final static int TYPE_RECTANGLE = 1;
    final static int TYPE_BITMAP = 2;
    final static int TYPE_VIDEO = 3;

    ImageContext m_image;
    int m_color;
    int m_transparency;
    boolean m_filled;
    boolean m_hasMaterial;
//#ifdef api.mm
    MediaObject m_mediaObject;
//#endif
    Region m_oldRegion;
    Region m_region;//RCA 121107
    AppearanceContext () { 
        m_oldRegion = new Region (0, 0, 0, 0);
        m_region = new Region (0, 0, 0, 0);//RCA 121107
    }

    void addClip (Region clip, Region current) {
        clip.add (m_oldRegion);
        clip.add (current);
        m_oldRegion.set (current);
    }

    boolean isUpdated (Region current) {
        return !current.equals (m_oldRegion); 
    }

//#ifdef api.mm
    boolean isVideo () {
        return m_mediaObject != null && 
            m_mediaObject.m_type == MediaObject.VIDEO &&
            m_mediaObject.getState() == Loadable.PLAYING;
    }
//#endif
}
