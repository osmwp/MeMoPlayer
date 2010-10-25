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

/**
 * Paired strings accessible from Browser.getCooky/setCooky 
 */
public class CookyManager {
    String m_key, m_value;
    CookyManager m_next;
    static CookyManager s_root;

    CookyManager (String key, String value, CookyManager next) {
        m_key = key;
        m_value = value;
        m_next = next;
    }
    
    String find (String key) {
        if (m_key.equals (key)) {
            return m_value;
        }
        return (m_next != null) ? m_next.find (key) : "";
    }
    
    private CookyManager removeRec (String key) {
        if (key.equals (m_key)) {
            m_key = m_value = null;
            return m_next != null ? m_next.removeRec (key) : null;
        }
        if (m_next != null) {
            m_next = m_next.removeRec (key);
        }
        return this;
    }

    static void set (String key, String value) {
        //System.err.println ("SetCooky: "+key+", "+value);
        s_root = new CookyManager (key, value, s_root);
    }

    static String get (String key) {
        //System.err.println ("GetCooky: "+key+" => "+((s_root !=null) ? s_root.find (key) : ""));
        return (s_root !=null) ? s_root.find (key) : "";
    }
    
    static void remove (String key) {
        if (s_root != null) {
            s_root = s_root.removeRec(key);
        }
    }

    static void clean() {
        CookyManager cm = s_root;
        while (cm != null) {
            cm.m_key = null;
            cm.m_value = null;
            cm = cm.m_next;
        }
        s_root = null;
    }
}
