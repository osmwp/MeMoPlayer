//#condition api.jsonrpc
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

interface TaskListener {
    public void changed(Task task, int signal);
    public void clean();
}

public abstract class Task {
    final static int STATE_NONE = 0; 
    final static int STATE_READY = 1;
    final static int STATE_RUNNING = 2;
    final static int STATE_FINISHED = 4;
    final static int STATE_ERROR = 8;
    
    final static int SIGNAL_STATE = 16;
    final static int SIGNAL_PROGRESS = 32;
    
    private int m_id = 0;
    private int m_state = STATE_NONE;
    private int m_progress = 0;
    
    private ObjLink m_listeners;
        
    // Called by the TaskQueue to excecute the task
    abstract void doTheJob () throws Exception;
    
    // Can be called by main thread once doTheJob() has completed
    abstract Object getResult ();
    
    // Called when no exception are raised during the doTheJob() method.
    void onSuccess() { clean(); }
    
    // By sending back TRUE, onError() methods can ask to the TaskQueue to retry excecuting the task
    // Default is no retry.
    boolean onError (Exception e) { clean(); return false; }
    
    
    // Called by main thread
    public synchronized void addTaskListener (TaskListener tl) {
        m_listeners = ObjLink.create(tl, m_listeners);   
    }
    
    // Called by worker thead when state or progress are updated
    private synchronized void notifyListeners (int signal) {
        ObjLink tl = m_listeners;
        while (tl != null) {
            ((TaskListener)tl.m_object).changed(this, signal);
            tl = tl.m_next;
        }    
    }
    
    // Called by worker thread on task completion 
    private synchronized void clean() {
        if (m_listeners != null) {    
            ObjLink tl = m_listeners;
            while (tl != null) {
                ((TaskListener)tl.m_object).clean();
                tl = tl.m_next;
            } 
            m_listeners = m_listeners.removeAll();
        }
    }
    
    // Setters and getters //
    void setId (int id) { m_id = id; }
    void setState (int state) { m_state = state; notifyListeners(SIGNAL_STATE); }
    void setProgress (int progress) { m_progress = progress; notifyListeners(SIGNAL_PROGRESS); }

    int getId() { return m_id; }    
    int getState () { return m_state; }
    int getProgress () { return m_progress; }
}
