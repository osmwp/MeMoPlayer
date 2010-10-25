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
import java.io.*;

class Link {
    Link m_next;
    Object m_o;

    Link (Object o, Link next) {
        m_o = o;
        m_next = next;
    }
    int size () {
        if (m_next == null) {
            return 1;
        }
        return 1 + m_next.size ();
    }
    Link remove (Object o) {
        if (m_o == o) {
            return m_next;
        } else if (m_next != null) {
            m_next = m_next.remove (o);
        }
        return this;
    }
    // Implementation of a circular linked list
    static Link addToTail (Object o, Link tail) {
        Link newTail = new Link(o, null);
        if (tail != null && tail.m_next != null) {
            newTail.m_next = tail.m_next;
            tail.m_next = newTail;
        } else {
            newTail.m_next = newTail;
        }
        return newTail;
    }
    static Object removeFromHead (Link tail) {
        Object o = null;
        if(tail != null) {
            Link head = tail.m_next;
            if (head != null && head != tail) {
                tail.m_next = head.m_next;
                o = head.m_o;
            } else if (head == tail) {
                tail.m_next = null;
                o = tail.m_o;
                tail.m_o = null;
            }
        }
        return o;
    }
}

abstract public class Field implements ScriptAccess{

    protected final static int LENGTH_IDX = 255;
    protected final static int OBJECT_IDX = 254;
    // code for all types
    protected final static int SFBOOL_CODE = 1;
    protected final static int SFCOLOR_CODE = 2;
    protected final static int SFFLOAT_CODE = 3;
    protected final static int SFINT32_CODE = 4;
    protected final static int SFNODE_CODE = 5; 
    protected final static int SFROTATION_CODE = 6; 
    protected final static int SFSTRING_CODE = 7; 
    protected final static int SFTIME_CODE = 8;
    protected final static int SFVEC2F_CODE = 9;
    protected final static int SFVEC3F_CODE = 10;
    protected final static int MFBOOL_CODE = 11;
    protected final static int MFCOLOR_CODE = 12;
    protected final static int MFFLOAT_CODE = 13;
    protected final static int MFINT32_CODE = 14;
    protected final static int MFNODE_CODE = 15;
    protected final static int MFROTATION_CODE = 16;
    protected final static int MFSTRING_CODE = 17;
    protected final static int MFVEC2F_CODE = 18;
    protected final static int MFVEC3F_CODE = 19;

    Link m_root;
    int m_id; // used by Script node to get the right index

    Field (Observer o) {
        addObserver (o);
    }
    Field () { }

    void decode (DataInputStream dis, Node [] table, Decoder decoder) {
    }

    final void decode (DataInputStream dis) {
    }

    final void decode (DataInputStream dis, Node [] table) {
    }

    void addObserver (Observer o) {
        if (o != null) {
            m_root = new Link (o, m_root);
        }
        //System.out.println ("$$ adding observer to "+this);
    }

    boolean removeObserver (Observer o) {
        int s1 = 0, s2 = 0;
        if (m_root != null) {
            s1 = m_root.size ();
            m_root = m_root.remove (o);
            s2 = m_root == null ? 0 : m_root.size ();
        }
        return (s1 > s2);
    }


    void notifyChange () {
        MyCanvas.composeAgain = true;
        Link link = m_root;
        while (link != null) {
            ((Observer)link.m_o).fieldChanged (this);
            link = link.m_next;
        }
    }
    
    abstract void copyValue (Field f);

    public static Field createFieldById (int id) {
        switch (id) {
        case SFBOOL_CODE : return new SFBool (true, null);
        case SFCOLOR_CODE : return new SFColor (0, 0, 0, null);
        case SFFLOAT_CODE : return new SFFloat (0, null);
        case SFINT32_CODE : return new SFInt32 (0, null);
        case SFNODE_CODE : return new SFNode (null);
        case SFROTATION_CODE : return new SFRotation (/*0, 0, 0, 0*/null);
        case SFTIME_CODE : return new SFTime (0, null);
        case SFSTRING_CODE : return new SFString ("", null);
        case SFVEC2F_CODE : return new SFVec2f (0, 0, null);
        case SFVEC3F_CODE : return new SFVec3f (0, 0, 0, null);
            //case MFBOOL_CODE : return new MFBool (null);
        case MFCOLOR_CODE : return new MFColor ();
        case MFINT32_CODE : return new MFInt32 ();
        case MFFLOAT_CODE : return new MFFloat ();
        case MFNODE_CODE : return new MFNode (null);
        case MFROTATION_CODE : return new MFRotation (null);
        case MFSTRING_CODE : return new MFString ();
        case MFVEC2F_CODE : return new MFVec2f ();
        case MFVEC3F_CODE : return new MFVec3f ();
        default: 
            System.err.println ("createFieldById: unknown code: "+id);
            return null;
        }
    }
    public ScriptAccess use (int index) {
        ////System.out.println ("Field.use: unexpected call");
        return null;
    }

    public void set (int index, Register r, int offset) {
        ////System.out.println ("Field.set: unexpected call");
    }

    public void get (int index, Register r, int offset) {
        ////System.out.println ("Field.get: unexpected call");
    }
}

