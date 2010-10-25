//#condition api.xparse2
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

public class XParseManager {
    final static int MAX_ARRAY = 16;
    XParsePush [] m_parser;

    static XParseManager s_self = null;

    static int createParser (String url, int mode){
        int id = getInstance().getFreeSlot();
        if (id != -1) {
            getInstance().m_parser[id] = new XParsePush(url, mode);
        }
        return id;
    }

    public static XParsePush getParser (int i) {
        if (i >= 0 && i < MAX_ARRAY) {
            return getInstance().m_parser [i];
        }
        return null;
    }

    public static void clean (int i) {
        if (i >= 0 && i < MAX_ARRAY) {
            getInstance().freeSlot (i);
        }
    }

    private static XParseManager getInstance() {
        if (s_self == null) {
            s_self = new XParseManager();
        }
        return s_self;
    }


    private XParseManager(){
        m_parser = new XParsePush[MAX_ARRAY];
    }

    private void freeSlot (int i) {
        if (m_parser[i] != null){
            m_parser[i].close ();
            m_parser[i] = null;
        }
    }
    
    private int getFreeSlot() {
        for (int i = 0; i < MAX_ARRAY; i++) {
            if (m_parser[i] == null) {
                return i;
            }
        }
        return -1;
    }



}
