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

public class TaskListenerCb implements TaskListener {
    private int m_signal;
    private int m_state;
    Script m_script;
    int m_f;
    Register[] m_params;
    
    public TaskListenerCb(Scene scene, Script s, int f, Register[] params, String signal) {
        m_script = s;
        m_f = f;
        m_params = params;     
        m_signal = Task.SIGNAL_STATE;
        m_state = 0;
        if (signal.startsWith("onStart")) {
            m_state  = Task.STATE_RUNNING;
        } else if (signal.startsWith("onSuccess")) {
            m_state  = Task.STATE_FINISHED;
        } else if (signal.startsWith("onError")) {
            m_state  = Task.STATE_ERROR;
        } else if (signal.startsWith("onProgress")) {
            m_signal = Task.SIGNAL_PROGRESS;
        }
        if (m_state == 0) {
            params[params.length-1] = new Register(); // Completed by changed()
        }
    }
    
    // Synchronised to prevent m_params being changed by workerThread while super.activate(c) is called by main thread
    public synchronized void changed(Task t, int s) {
        if (s == m_signal) {
            switch (s) {
            case Task.SIGNAL_STATE:
                int state = t.getState();
                if (m_state == 0) {
                    m_params[m_params.length-1].setInt(state);
                    m_script.addCallback(m_f, m_params);
                } else if (m_state  == state) {
                    m_script.addCallback(m_f, m_params);
                }
                MiniPlayer.wakeUpCanvas();
                break;
            case Task.SIGNAL_PROGRESS:
                m_params[m_params.length-1].setInt(t.getProgress());
                m_script.addCallback(m_f, m_params);
                MiniPlayer.wakeUpCanvas();
                break;
            default:
            }
        }
    }
    
    public synchronized void clean() {
        m_params = null;
        m_script = null;
    }
}
