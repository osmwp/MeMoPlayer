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
 * Paired strings accessible from Browser.getCooky/setCooky.
 * 
 * When using Namespace, protected paired string set in the
 * "admin" mode (see Namespace.java) can only be read but not
 * modified from the other Namespaces (where widgets run).
 * The others Namespaces can still exchanged data by setting
 * values to keys that are not already in use in the "admin" mode.
 */
public class CookyManager {
    String m_key, m_value;
    CookyManager m_next;
    static CookyManager s_root;
    static CookyManager s_protected;

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

    boolean setRec (String key, String value) {
        if (m_key.equals (key)) {
            m_value = value;
            return true;
        }
        return (m_next != null) ? m_next.setRec (key, value) : false;
    }

    static synchronized void set (String key, String value) {
//#ifdef MM.namespace
        if (Namespace.getName() == "") {
            // All keys set in admin mode are store are protected
            if (s_protected != null && s_protected.setRec(key, value)) {
                return;
            }
            s_protected = new CookyManager (key, value, s_protected);
            return;
        }
//#endif
        if (s_root != null && s_root.setRec(key, value)) {
            return;
        }
        s_root = new CookyManager (key, value, s_root);
    }

    static synchronized String get (String key) {
//#ifdef MM.namespace
        if (s_protected != null) {
            // Always access first to keys saved in the protected mode
            String value = s_protected.find (key);
            if (value != "") {
                return value;
            }
        }
//#endif
        return (s_root !=null) ? s_root.find (key) : "";
    }
    
    static synchronized void remove (String key) {
//#ifdef MM.namespace
        if (Namespace.getName() == "" && s_protected != null) {
            // Only admin mode can delete protected values
            s_protected = s_protected.removeRec (key);
        }
//#endif
        if (s_root != null) {
            s_root = s_root.removeRec(key);
        }
    }

    static synchronized void clean() {
//#ifdef MM.namespace
        s_protected = null;
//#endif
        CookyManager cm = s_root;
        while (cm != null) {
            cm.m_key = null;
            cm.m_value = null;
            cm = cm.m_next;
        }
        s_root = null;
    }
}
