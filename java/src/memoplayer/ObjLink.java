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

public class ObjLink {
    Object m_param;
    float m_z = -3;
    Object m_object;
    ObjLink m_next;

    static ObjLink s_root;


    public static ObjLink create (Object o, ObjLink n) {
        return create (o, null, -2f, n);
    }

    public static ObjLink create (Object o, float z, ObjLink n) {
        return create (o, null, z, n);
    }


    public static ObjLink create (Object o, Object p, ObjLink n) {
        return create (o, p, -1f, n);
    }

    public synchronized static ObjLink create (Object o, Object p, float z, ObjLink n){//,int isVideo,int p_Layer,String name) {
        if (s_root == null) {
            //System.err.println ("Objlink: create => new :o="+o+", p="+p);
            return new ObjLink (o, p, z, n);//,isVideo,p_Layer,name);
        }
        ObjLink ol = s_root;
        s_root = s_root.m_next;
        //System.err.println ("OL.create old "+ol+" for "+o+" next is "+n);
        ol.m_object = o;
        ol.m_param = p;
        ol.m_z = z;
        ol.m_next = n;

        return ol;
    }

    public synchronized static void release (ObjLink ol) {
        //Logger.println("release de "+ol.toString()+" nb element:"+nb);
        ol.m_param = null;
        ol.m_object = null;
        ol.m_next = s_root;
        s_root = ol;

    }

    public static void releaseAll (ObjLink ol) {
        while (ol != null) {
            ObjLink tmp = ol;
            ol = ol.m_next;
            release (tmp);
        }
    }

    public ObjLink remove (Object o) {
        ObjLink ol;
        if (o == m_object) {
            ol = m_next;
            ObjLink.release (this);
        } else {
            if (m_next != null) {
                m_next = m_next.remove (o);
            }
            ol = this;
        }
        return (ol);
    }

    public ObjLink removeAll () {
        if (m_next != null) {
            m_next = m_next.removeAll ();
        }
        ObjLink.release (this);
        return null;
    }

    private ObjLink (Object o, Object p, float z, ObjLink n) {
        m_object = o;
        m_param = p;
        m_z = z;
        m_next = n;
    }
}

