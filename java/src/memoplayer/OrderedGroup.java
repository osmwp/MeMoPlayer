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

public class OrderedGroup extends Group {
    int [] m_order;
    OrderedGroup () {
        super (2);
        //System.out.println ("OrderedGroup created");
        // m_field[0] is created by class Group
        m_field[1] = new MFFloat (); // order
        m_field[1].addObserver (this);
    }

    void start (Context c) {
        super.start (c);
        fieldChanged (m_field[1]);
        m_order = new int [m_children.m_size];
    }

    final protected void swap (int [] order, Node [] children, int i1, int i2) {
        int f = order [i1]; order [i1] = order [i2]; order [i2] = f;
        Node n = children [i1]; children [i1] = children [i2]; children [i2] = n;
    } 

    final protected int findMini (int [] tab, int start, int len) {
        int res = start;
        for (int i = start+1; i < len; i++) {
            if (tab[i] < tab[res]) { res = i; }
        }
        return (res);
    }

    boolean compose (Context c, Region clip, boolean forceUpdate) {
        boolean updated = forceUpdate | m_isUpdated;
        int len = m_order.length;
        if (m_isUpdated) {
            int [] order = ((MFFloat) m_field [1]).getValues ();
            len = order.length < m_children.m_size ? order.length : m_children.m_size;
            
            int mini = -1, current = order[0], index = 0;

            for (int i = 0; i < len; i++) {
                current = 1000<<16;
                for (int j = 0; j < len; j++) {
                    if (order[j] > mini && order [j] < current) {
                        index = j; current = order [j];
                    }
                }
                m_order [i] = index;
                mini = order [index];
            }
            m_isUpdated = false;
        }
        for (int i = 0; i < len; i++) {
            updated |= m_children.m_node[m_order[i]].compose (c, clip, forceUpdate);
        }
        return updated;
    }

    public void fieldChanged (Field f) {
        if (f == m_field[1]) {
            m_isUpdated = true;
        }
    }

}
