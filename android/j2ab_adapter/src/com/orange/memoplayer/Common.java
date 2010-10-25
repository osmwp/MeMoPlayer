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

package com.orange.memoplayer;

import java.io.InputStream;
import java.util.Properties;

import javax.microedition.midlet.MIDlet;

import android.content.ContextWrapper;

public class Common {
    public static int BOOT_NORMAL = 0;
    public static int BOOT_WIDGET = 1;
    public static int BOOT_CONFIGURE = 2;
    private static final String[] sBootProperty = {"boot", "boot-widget", "boot-configure"};

    private static final String JAD_PROPERTIES = "jad.properties";
    private static final String MIDLET_PROPERTY = "midlet";

    @SuppressWarnings("unchecked")
    public static MIDlet createMIDlet(ContextWrapper context, int bootMode) {
        Properties properties = new Properties();
        try {
            InputStream ins = context.getClass().getResourceAsStream("/" + JAD_PROPERTIES);
            properties.load(ins);
        } catch (Exception ex) {
            throw new RuntimeException("error loading " + JAD_PROPERTIES, ex);
        }
        // Register the default path for caching data in MeMoPlayer
        properties.setProperty("MeMo-CachePath", "file://" + context.getFilesDir().getAbsolutePath());
        
        // When booting in special mode, override default boot property with the special one.
        if (bootMode != BOOT_NORMAL) {
            String bootScene = properties.getProperty(sBootProperty[bootMode]);
            if (bootScene != null) {
                properties.setProperty("boot", bootScene);
            }
        }
        
        String midletClassName = properties.getProperty(MIDLET_PROPERTY);
        MIDlet midlet;
        try {
            Class midletClass = Class.forName(midletClassName, true, context.getClassLoader());
            midlet = (MIDlet) midletClass.newInstance();
        } catch (Exception ex) {
            throw new RuntimeException("unable to load class " + midletClassName, ex);
        }
        
        midlet.setup(context, properties);
        return midlet;
    }
}
