//#condition MM.namespace
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

//#ifdef MM.weakreference
import java.lang.ref.WeakReference;
//#endif

/**
 * VRML Node to prevent data collision in CacheManager
 * Namespace allows to :
 *  - Protect from RuntimeException.
 *  - Prevent access to some Javascript methods (small security model).
 *  - Prevent data collision for CacheManager.
 *  
 *  For the security mode, the default and "empty" namespace
 *  is considered as the admin mode where everything is allowed.
 *  
 *  To limit code the admin mode, use the following code snippet:
 *    if (Namespace.getName () != '') {
 *      // ONLY ADMIN CODE
 *    }
 *  
 */
class Namespace extends Group {
    final static String LOW_MEMORY = "LOW_MEMORY";
    final static String GENERIC_ERROR = "ERROR";
    final static String SCENE_ERROR = "SCENE_ERROR";
    
    private static Namespace s_current = null;
    private static ObjLink s_list;
    
    /**
     * Return the current namespace name. 
     * Must only be called from the main thead !
     * To get the current namespave name from another thread,
     * use Thead.currentNamespace().
     */
    public final static String getName() {
        return s_current != null ? s_current.m_name : ""; 
    }

    public static CacheManager getCacheManager () {
        if (Thread.isMainThread()) {
            if (s_current != null && s_current.m_name != "") {
                return s_current.getInternalCacheManager();
            }
            return CacheManager.getCurrentManager();
        }
        return getCacheManager (Thread.currentNamespace ());
    }
    
    public static synchronized CacheManager getCacheManager (String name) {
        if (name.equals("")) { // Fall back to defaut CM
            return CacheManager.getCurrentManager();
        }
        ObjLink ol = s_list, prev = null;
        while (ol != null) {
            if (ol.m_object == name) {
                Object o;
//#ifdef MM.weakreference
                o = ((WeakReference)ol.m_param).get();
                if (o == null) { // ref was GCed, remove ObjLink
                    if (prev != null) {
                        prev.m_next = ol.m_next;
                    } else { // ol == first element
                        s_list = null;
                    }
                    ObjLink.release(ol);
                    break;
                }
//#else
                o = ol.m_param;
//#endif
                //Logger.println("Namespace: "+name+": reusing from cache");
                return (CacheManager)o;
            }
            prev = ol;
            ol = ol.m_next;
        }
        CacheManager mgr = CacheManager.createManager (name);
//#ifdef MM.weakreference
        s_list = ObjLink.create(name, new WeakReference(mgr), s_list);
//#else
        s_list = ObjLink.create(name, mgr, s_list);
//#endif
        //Logger.println("Namespace: "+name+": new CM cached");
        return mgr;
     }
     
     public static synchronized void clean () {
         ObjLink.releaseAll(s_list);
     }
     
     public static void throwException (String code) {
         if (s_current != null) {
             s_current.exception(code);
         }
     }
    
    // Note: always use intern version of String for "m_name"
    // as we do a lot of comparison using the == for speed purpose.
    String m_name = "";
    CacheManager m_mgr = null;
    
    public Namespace () {
        super (3);
        m_field[1] = new MFString(this); // name
        m_field[2] = new MFString(); // exception
    }
    
    void start (Context c) {
        fieldChanged(m_field[1]);
        Namespace old = s_current;
        s_current = this;
        try {
            super.start(c);
        } catch (OutOfMemoryError e) {
            exception(LOW_MEMORY);
            System.gc();
        } catch (Throwable e) {
            e.printStackTrace();
            exception(GENERIC_ERROR);
        }
        s_current = old;
    }
    
    void stop(Context c) {
        Namespace old = s_current;
        s_current = this;
        m_name = "";
        m_mgr = null;
        try {
            super.stop(c);
        } catch (OutOfMemoryError e) {
            exception(LOW_MEMORY);
            System.gc();
        } catch (Throwable e) {
            e.printStackTrace();
            exception(GENERIC_ERROR);
        }
        s_current = old;
    }
    
    boolean compose (Context c, Region clip, boolean forceUpdate) {
        Namespace old = s_current;
        boolean updated = false;
        s_current = this;
        try {
            updated = super.compose (c, clip, forceUpdate);
        } catch (OutOfMemoryError e) {
            exception(LOW_MEMORY);
            System.gc();
        } catch (Throwable e) {
            e.printStackTrace();
            exception(GENERIC_ERROR);
        }
        s_current = old;
        return updated;
    }
    
    public void fieldChanged (Field field) {
        if (field == m_field[1]) {
            // get String's intern object to make sure comparison with == will work
            String newName = ((MFString)m_field[1]).getValue(0).intern();
            if (!newName.equals(m_name)) {
                m_mgr = null;
            }
            m_name = newName;
        }
    }
    
    private CacheManager getInternalCacheManager () {
        if (m_mgr == null) {
            m_mgr = getCacheManager (m_name);
        }
        return m_mgr;
    }
    
    private void exception(String code) {
        ((MFString)m_field[2]).setValue(0, code);
    }
}
