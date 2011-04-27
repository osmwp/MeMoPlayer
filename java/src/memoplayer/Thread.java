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
 * This class overloads the default Thread class to
 * allow Namespace.getCacheManager() to always return the Namespace
 * that was in use when the Thread was spawned.
 */
public class Thread extends java.lang.Thread {

//#ifdef MM.namespace
    public static Thread s_mainThread = null;
   
    public static boolean isMainThread () {
        return currentThread() == s_mainThread;
    }
    
    public static String currentNamespace () {
        java.lang.Thread t = currentThread();
        if (t == s_mainThread) {
            return Namespace.getName();
        } else if (t instanceof Thread) {
            return ((Thread)t).m_namespace;
        }
        throw new RuntimeException ("Thread.currentNamespace: Not called from a memoplayer.Thread !");
    }
    
    /**
     * Force this thread namespace (only for threads common to multiple namespaces)
     */
    public void forceNamespace (String namespace) {
        m_namespace = namespace;
    }
    
    // Each instance of a thread in the MeMo gets the current Namspace name on creation
    private String m_namespace = Namespace.getName();
//#endif

    public Thread() {
        super();
    }

    public Thread(Runnable target, String name) {
        super(target, name);
    }

    public Thread(Runnable target) {
        super(target);
    }

    public Thread(String name) {
        super(name);
    }
}
