/*
 * Copyright (C) 2009 The J2AB Project
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

package javax.microedition.media;

import j2ab.android.media.AndroidPlayer;

import java.io.IOException;
import java.io.InputStream;

public class Manager {
    public static final String TONE_DEVICE_LOCATOR = "device://tone";
    public static final String RTSP_LOCATOR = "rtsp://";
    public static final String HTTP_LOCATOR = "http://";
    public static final String SDCARD_LOCATOR = "/sdcard";
    

    public static Player createPlayer (InputStream ins, String type) throws IOException, MediaException {
        return new AndroidPlayer (ins, type);
    }

    public static Player createPlayer (String locator) throws IOException, MediaException {
        if (TONE_DEVICE_LOCATOR.equals (locator)) {
            return new TonePlayer ();
        } else if (locator.startsWith (HTTP_LOCATOR)
                || locator.startsWith (RTSP_LOCATOR)
                || locator.startsWith (SDCARD_LOCATOR)) {
            return new AndroidPlayer (locator);
        } else {
            throw new MediaException ("Unsupported locator: "+locator);
        }
    }

    public static void playTone (int note, int duration, int volume) throws MediaException {
        throw new MediaException ("Unsupported feature");
    }

    public static String[] getSupportedContentTypes (String protocol) {
        return new String[] { "audio/3gp", "audio/amr", "audio/mp3", "audio/wav", "video/3gpp", "video/mpeg4"};
    }

    public static String[] getSupportedProtocols (String type) {
        return new String[] { "http", "rtsp" };
    }
}
