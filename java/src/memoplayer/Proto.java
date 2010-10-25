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

public class Proto extends Node implements ScriptAccess {
    Node m_node;
    Proto () {
        super (64);
        //System.out.println ("PROTO created");
        m_nbFields = 0;
    }
    
    void addNode (Node n) {
        if (m_node == null) {
            m_node = n;
        } else {
            Node tmp = m_node;
            while (tmp.m_next != null) {
                tmp = tmp.m_next;
            }
            tmp.m_next = n;
        }
    }

    void start (Context c) {
        Node n = m_node;
        while (n != null) {
            n.start (c);
            n = n.m_next;
        }
    }

    void stop (Context c) {
        Node n = m_node;
        while (n != null) {
            n.stop (c);
            n = n.m_next;
        }
    }
    void destroy (Context c) {
        Node n = m_node;
        while (n != null) {
            n.destroy (c);
            n = n.m_next;
        }
        m_node = null;
    }

    void render (Context c) {
        Node n = m_node;
        while (n != null) {
            n.render (c);
            n = n.m_next;
        }
    }
    
    public boolean compose (Context c, Region clip, boolean forceUpdate) {
        boolean updated = forceUpdate;
        //m_lastTime = c.time;
        Node n = m_node;
        while (n != null) {
            updated |= n.compose (c, clip, forceUpdate);
            n = n.m_next;
        }
        return updated;
    }

    public void fieldChanged (Field f) {
    }
    
    public ScriptAccess use (int index) {
        //Logger.println ("Proto: using field @ "+index);
        index--;
        if (index > 0 && index < m_nbFields) {
            Logger.println ("Proto: using field: "+m_field [index]);
            return m_field [index];
        } else {
            return this;
        }
    }
    
    public void set (int index, Register r, int offset) {
        //System.out.println ("Script.set: unexpected call");
    }

    public void get (int index, Register r, int offset) {
        //System.out.println ("Script.get: unexpected call");
    }
}
