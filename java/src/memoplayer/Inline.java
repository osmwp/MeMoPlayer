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

public class Inline extends Node {
    Scene m_scene;
    boolean m_isUpdated;
    int m_initTime;
    int m_state;

    Inline () {
        super (3);
        //System.out.println ("Inline created");
        m_field[0] = new MFString (this);
        m_field[1] = new MFString ();
        m_field[2] = new MFString ();
    }

    String findLocFile (String locale) {
        int nbLocales = ((MFString)m_field[1]).m_size;
        for (int i = 0; i < nbLocales; i+=2) {
            if (((MFString)m_field[1]).getValue (i).equalsIgnoreCase (locale)) {
                return ((MFString)m_field[1]).getValue (i+1);
            }
        }
        return null;
    }

    // The value contained in locale is lang_country (2 letters each) like:
    // en_US, fr_FR, es_ES, fr_CA , ...
    static String getMobileLanguage () {
        String curLocale = System.getProperty("microedition.locale");
        // Logger.println ("getMobileLanguage: "+curLocale);
        // code to get the user language mobile setting
        if (curLocale == null) {
            return "en";
        }
        return curLocale.substring (0, 2);
    }

    static String [] countryCode = { "+33", "+44", "+39", "+32", "+41", "+34", "+351", "+421", "+123"};
    static String [] countryName = { "FR" , "UK" , "IT" , "BE",  "CH",  "SP" , "PT",   "SL",   "EMUL"};
    // The value contained in smsc is a phone number
    static String getMobileCountry () {
        String smsc = System.getProperty("wireless.messaging.sms.smsc");
        if (smsc != null) {
            for (int i = 0; i < countryCode.length; i++) {
                if (smsc.startsWith (countryCode [i])) {
                    return countryName [i];
                }
            }
        }
        return "FR";
    }

    void updateLocaleTable (Decoder decoder) {
        // try to find the defined LOCALE
        int nbUsages =  ((MFString)m_field[2]).m_size;
        for (int i = 0; i < nbUsages; i++) {
            String usage = ((MFString)m_field[2]).getValue (i);
            if (usage.equalsIgnoreCase ("auto")) {
                usage = getMobileLanguage();
            }
            String file = findLocFile (usage);
            if (decoder.setLocale (file)) {
                return;
            } else {
                Logger.println ("Warning: data not found for locale: "+usage);
            }
        }
    }

    void start (Context c) {
        c.scene.registerSleepy (this);
        if (m_scene == null) { //MCP: Only force update of field 'url' on first init
            fieldChanged (m_field[0]);
        } else {           
            m_scene.start (c);
        }
    }

    void render (Context c) {
        if (m_state == Loadable.READY && m_scene != null) {
            c.removeLoadable (m_scene); // just in case
            c.addLoadable (m_scene);
            m_scene.render (c);
        }       
    }

    void stop (Context c) {
        c.scene.unregisterSleepy (this);
        if (m_scene != null) {
            c.removeLoadable (m_scene);
            c.decoder = m_scene.m_decoder;
            m_scene.stop (c);
        }
    }

    // needed for sharing script bytecodes
    void destroy(Context c) {
        if (m_scene != null) {
            c.removeLoadable (m_scene);
            Decoder d = c.decoder;
            c.decoder = m_scene.m_decoder;
            m_scene.stop (c);
            m_scene.destroy (c);
            c.decoder = d;
            m_scene = null;
            Runtime.getRuntime ().gc ();
        }
    }

    boolean compose (Context c, Region clip, boolean forceUpdate) {
        boolean updated = forceUpdate || m_isUpdated;
        Decoder d = c.decoder;
        String name = "undefined";
        if (m_isUpdated) {
            clip.setInt (0, 0, c.width, c.height);
            m_isUpdated = false;
//#ifdef profiling
            Logger.profileStart ();
//#endif
            destroy (c); // Destroy currently loaded scene if any
//#ifdef profiling
            Logger.profileEnd ("Inline: scene destroy");
//#endif
            name = ((MFString) m_field [0]).getValue (0);
            //System.out.println ("Inline.compose: "+this+" url='"+name+" @ "+c.time);
            if (name != null && name.length() > 0){
                try {
                    updateLocaleTable (d);
//#ifdef profiling
                    Logger.profileStart ();
//#endif
                    m_scene = new Scene (d, name);
//#ifdef profiling
                    Logger.profileEnd ("for creating scene "+name);
//#endif
                    //Runtime rt = Runtime.getRuntime ();
                    //Logger.println ("Inline create scene "+name+" : "+rt.freeMemory()+" / "+rt.totalMemory ());
                    c.addLoadable (m_scene);
                    m_state = Loadable.LOADING;
                } catch (Exception e) {
                    System.err.println ("Inline: cannot load "+name+" :"+e);
                    m_state = Loadable.ERROR;
                }
            } else {
                m_state = Loadable.CLOSED;
            }
        }
        switch (m_state) {
        case Loadable.CLOSED:
            break;
        case Loadable.ERROR:
//#ifdef MM.namespace
            Namespace.throwException (Namespace.SCENE_ERROR);
//#endif
            m_state = Loadable.CLOSED;
            break;
        case Loadable.LOADING:
            if (m_scene.getState () == Loadable.LOADED) {
                m_state = Loadable.LOADED; // no break because we want to run case below now
            } else { // still LOADING or ERROR
                if (m_scene.getState () == Loadable.ERROR) {
                    m_state = Loadable.ERROR;
                }
                MyCanvas.composeAgain = true;
                break;
            }
        case Loadable.LOADED:
            m_initTime = c.time;
            c.time = 0;
            c.decoder = m_scene.m_decoder;
            //System.out.println ("Inline.compose: start scene with C3D"+c.c3D+"  @ "+c.time);
            m_scene.start (c);
//#ifdef profiling
            Logger.profileStart ();
//#endif
            c.time = m_initTime;
//#ifdef profiling
            Logger.profileEnd ("for scene starting");
//#endif
            m_state = Loadable.READY;
            clip.setInt (0, 0, c.width, c.height);
            //System.gc();
            //System.out.println ("Inline.compose: "+this+" LOADED => READY @ "+c.time);
            //break; do not break as we want to compose just afterwards
        case Loadable.READY:
            c.time -= m_initTime;
            c.decoder = m_scene.m_decoder;
            String oldUrl = c.newUrl;
            updated |= m_scene.innerCompose (c, clip, updated);
            c.time += m_initTime;
            if (c.newUrl != null && c.newUrl != oldUrl) {
                if (c.newUrlCount == 0) {
                    ((MFString)m_field[0]).setValue (0, c.newUrl);
                    c.newUrl = null;
                } else {
                    c.newUrlCount--;
                }
            } else {
                c.newUrl = oldUrl;
            }
            break;
        }
        c.decoder = d;
        return updated;
    }

    public void fieldChanged (Field f) {
        m_isUpdated = true;
    }

//#ifdef MM.pause
    int getWakeupTime (int time) {
        if (m_scene == null) {
            return MyCanvas.SLEEP_FOREVER;
        }
        time = m_scene.getWakeupTime (time-m_initTime);
        if (time != MyCanvas.SLEEP_FOREVER && time != MyCanvas.SLEEP_CANCELED) {
            time += m_initTime; // convert back to parent scene time
        }
        return time;
    }
//#endif

}
