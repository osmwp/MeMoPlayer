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

// For handling callbacks on Script's methods
// All fields are public for fast access from the Script class
public class ScriptCallback {
    private static ScriptCallback s_pool;  // pool of free ScriptCallbacks
    
    public static synchronized ScriptCallback create () {
        ScriptCallback c;
        if (s_pool != null) {
            c = s_pool;
            s_pool = s_pool.m_next;
            c.m_next = null;
        } else {
            c = new ScriptCallback();
        }
        return c;
    }

    public static synchronized void release (ScriptCallback c) {
        c.m_params = null;
        c.m_next = s_pool;
        s_pool = c;
    }
    
    public ScriptCallback m_next;
    public int m_function;
    public Register[] m_params;
    public int m_time;
}
