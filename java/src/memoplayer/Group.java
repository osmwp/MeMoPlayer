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

public class Group extends Node {
    MFNode m_children;
    int m_size;

    Group () { this (1); }
    
    Group (int n) {
        super (n);
        //System.out.println ("Group created");
        m_field[0] = new MFNode (null); // children
    }
    
    void start (Context c) {
        m_children = (MFNode)m_field[0];
        Group tmp = c.m_groupingNode;
        c.m_groupingNode = this;
        m_size = m_children.m_size;
        for (int i = 0; i < m_size; i++) {
            m_children.m_node[i].start (c);
        }
        c.m_groupingNode = tmp;
    }
    
    void stop (Context c) {
        for (int i = 0; i < m_size; i++) {
            m_children.m_node[i].stop (c);
        }
    }

    void destroy (Context c) {
        if (m_children != null && m_children.m_node != null) { // nasty problem when the group children is a proto ISed from parameter that is an USE from other nodes
            for (int i = 0; i < m_size; i++) {
                m_children.m_node[i].destroy (c);
            }
        }
        m_children = null;
    }

    void render (Context c) {
        for (int i = 0; i < m_size; i++) {
            m_children.m_node[i].render (c);
        }
    }
    
    boolean compose (Context c, Region clip, boolean forceUpdate) {
        boolean updated = forceUpdate;
        TouchSensor tmp = c.m_sensor;
        Group groupingNode = c.m_groupingNode;
        c.m_groupingNode = this;
        if (m_sensor != null) {
            c.m_sensor = m_sensor;
        }
        for (int i = 0; i < m_size; i++) {
            updated |= m_children.m_node[i].compose (c, clip, forceUpdate);
        }
        c.m_groupingNode = groupingNode;
        c.m_sensor = tmp;
        return updated;
    }

    void register (TouchSensor t) {
        m_sensor = t;
            
    }

    void unregister (TouchSensor t) {
        if (m_sensor == t) {
            m_sensor = null;
        }
    }
}
