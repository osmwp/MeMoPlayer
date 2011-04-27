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

//MCP: Prevent multiple concurrent async files access to HTTP resources
public class FileQueue {
     
    static File s_currentFile;
    static Thread s_t;
    static boolean s_quit;

    static void startThread (File f) {
        if (s_t == null) {
            s_currentFile = f;
            s_quit = false;
            s_t = new Thread() {
                public void run() {
                    try {
                        while (!s_quit) {
//#ifdef MM.namespace
                            // Force the namespace associated to this thread for each File access
                            forceNamespace (s_currentFile.m_cacheNamespace);
//#endif
                            s_currentFile.openAndLoad();
                            s_currentFile = null;
                            MiniPlayer.wakeUpCanvas();
                            // Sleep
                            try {
                                synchronized (s_t) {
                                    if (s_currentFile == null) {
                                        s_t.wait();
                                    }
                                }
                            } catch (InterruptedException e) {};
                        }
                    } catch (Throwable e) {
                        Logger.println("Exception in FileQueue: "+e.getMessage());
                        s_t = null;
                    }
                }
            };
            s_t.setPriority(Thread.MIN_PRIORITY);
            s_t.start();
        } else {
            // Wake up
            synchronized (s_t) {
                s_currentFile = f;
                s_t.notify();
            }
        }
    }
    
    // called by MyCanvas on application exit
    static void stopThread () {
        if (s_t != null) {
            synchronized (s_t) {
                s_quit = true;
                s_t.notify();
            }
            //try { s_t.join(); } catch(Exception e) {}
            try { s_t.interrupt(); } catch (Exception e) {}
            s_t = null;
        }
    }

    File m_queue;
    
    void clean() {
        // Explicitly dequeue all files to help GC
        File f = m_queue;
        while (f != null) {
            File f2 = f;
            f = f.m_next;
            f2.m_next = null;
        }
        m_queue = null;
    }

    void loadNext() {
        if (s_currentFile == null && m_queue != null) {
            m_queue = m_queue.popQueue(); // Search next file to open
            if (m_queue != null) {
                // Pop from queue
                File f = m_queue;
                m_queue = m_queue.m_next;
                f.m_next = null;
                startThread (f);
            }
        }
    }

    File getFile (String url) {
        String namespace = "";
//#ifdef MM.namespace
        namespace = Thread.currentNamespace();
//#endif

        // Check current file
        if (s_currentFile != null && s_currentFile.match(url, namespace)) {
            return s_currentFile;
        }

        // Find or add in queue
        if (m_queue != null) {
            return m_queue.findQueue(url, namespace);
        }
        // Create new queued file
        return m_queue = new File (url, namespace);
    }
}
