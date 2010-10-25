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

public class Event {
    final static int NONE = 0;
    final static int MOUSE = 32;
    final static int MOUSE_PRESSED = 32;
    final static int MOUSE_DRAGGED = 32+1;
    final static int MOUSE_RELEASED = 32+2;
    final static int KEY = 64;
    final static int KEY_PRESSED = 64;
    final static int KEY_RELEASED = 64+1;

    int m_type; // NONE, KEY_PRESSED, KEY_RELEASED, MOUSE_PRESSED, MOUSE_DRAGGED, MOUSE_RELEASED
    int m_key; // 0 1 2 3 4 5 6 7 8 9 0 ...
    int m_x, m_y; // MOUSE 
    InputSensor m_grabSensor;
    Event m_next;
    
    Event () {
    }

    void setKey (int keyType, int key) {
        set (keyType, key, 0);
    }

    void setMouse (int mouseType, int x, int y) {
        set (mouseType, x, y);
    }

    boolean isKeyEvent () {
        return (m_type & KEY) == KEY;
    }

    boolean isMouseEvent () {
        return (m_type & MOUSE) == MOUSE;
    }

    void set (int eventType, int p1, int p2) {
        m_type = eventType;
        if (isKeyEvent()) {
            m_key = p1;
        } else {
            m_x = p1;
            m_y = p2;
        }
        m_next = null;
        m_grabSensor = null;
    }

//     void append2 (Event e) {
//         if (m_next == null) {
//             m_next = e;
//         } else {
//             m_next.append2 (e);
//         }
//     }
 
    void setFrom (Event e) {
        m_type = e.m_type;
        if (isKeyEvent()) {
            m_key = e.m_key;
        } else {
            m_x = e.m_x;
            m_y = e.m_y;
        }
        m_grabSensor = e.m_grabSensor;
    }
    
    boolean isInside (Region r) {
        return m_x >= r.x0 && m_x <= r.x1 && m_y >= r.y0 && m_y <= r.y1; 
    }
    
    // Change a KEY_PRESSED event into a KEY_RELEASE
    // Change a MOUSE_PRESSED/DRAGGED event into a MOUSE_RELEASE
    boolean convertToReleased() {
        if (m_type == KEY_PRESSED) {
            m_type = KEY_RELEASED;
            return true;
        } else if (m_type == MOUSE_PRESSED || m_type == MOUSE_DRAGGED) {
            m_type = MOUSE_RELEASED;
            return true;
        }
        return false;
    }
}
