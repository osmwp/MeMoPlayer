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

public class Script extends Node implements ScriptAccess {
    private final static int IMMEDIATE_CALLBACK = -1;

    public boolean releaseMachineOnInit;
    SFString m_url;
    Machine m_machine; // may be released on start after the initialized method call
    Context m_c;
    boolean m_initDone = false;
    boolean m_invoking;
//#ifdef api.jsonrpc
    int m_lastFieldRef; // HACK for functions reference passing
//#endif
    ObjLink m_postponedCalls; // list of postponed calls (FIFO)
    ScriptCallback m_cbList; // ordered list of delayed callbacks, immediate callbacks are kept in front 

    // Can be called from any background thread to ask for a callback to be invoked
    void addCallback (int method, Register[] params) {
        addCallback(method, params, IMMEDIATE_CALLBACK);
    }

    // Can be called from any background thread to ask for a callback to be invoked
    // Synchronized for concurrent access to m_cbList
    synchronized void addCallback (int method, Register[] params, int time) {
        ScriptCallback cb = ScriptCallback.create ();
        cb.m_function = method;
        cb.m_params = params;
        cb.m_time = time;
        if (time == IMMEDIATE_CALLBACK || m_cbList == null || time < m_cbList.m_time) {
            // immediate callback or empty list or first, just add as first
            cb.m_next = m_cbList;
            m_cbList = cb;
        } else {
            // delayed callback, insert at the right position in the list
            ScriptCallback c = m_cbList;
            ScriptCallback next = c.m_next;
            while (next != null) {
                if (time > next.m_time) {
                    c = next;
                    next = c.m_next;
                } else { // found position
                    break;
                }
            }
            // insert between c and next
            cb.m_next = next;
            c.m_next = cb;
        }
    }

    // Synchronized for concurrent access to m_cbList
    synchronized void invokeCallbacks (Context c) {
        ScriptCallback cb = m_cbList;
        int time = c.time;
        m_invoking = true;
        while (cb != null) {
            if (cb.m_time < time) {
                m_machine.invoke (cb.m_function, c, cb.m_params);
                if (m_postponedCalls != null) {
                    invokePostponedCalls (m_postponedCalls, time);
                }
                ScriptCallback old = cb;
                cb = cb.m_next;
                ScriptCallback.release (old);
            } else { // delayed callback still in the future, stop
                break;
            }
        }
        m_invoking = false;
        m_cbList = cb; // first delayed callback in the future or null
    }

    // Compute wakeup time for closest timed callback, if any
    // Synchronized for concurrent access to m_cbList
    synchronized int getWakeupTime (int time) {
        if (m_cbList != null) {
            int t = m_cbList.m_time; // get time of first callback
            if (t == IMMEDIATE_CALLBACK || t < time) {
               return MyCanvas.SLEEP_CANCELED; // pending immediate callback
            } else {
               return t - time; // compute sleep for first delayed callback
            }
        }
        return MyCanvas.SLEEP_FOREVER;
    }

    Script () {
        super (64);
        m_field[0] = m_url = new SFString ("", null);
        m_nbFields = 1;
    }
    
    void start (Context c) {
        m_c = c;
        if (!m_initDone) {
            m_initDone = true;
            String url = m_url.getValue ();
            DataLink dl = c.getDataLink (url);
            if (dl != null) {
                FunctionBank fb = (FunctionBank)dl.getObject ();
                if (fb == null) {
                    byte [] data = dl.getScript ();
                    if (data != null) {
                        fb = new FunctionBank(data);
                        // Adding FunctionBank to the Script's datalink
                        dl.setObject(fb);
                    }
                }
                if (fb != null) {
                    //Logger.println("Script: reusing "+dl.m_count+"times functionbank for url "+url+" / "+this);
                    m_machine = new Machine (fb, m_nbFields);
//#ifndef MM.weakreference    
                    dl.m_count++;
//#endif
                }
            }            
            if (m_machine != null) {
                releaseMachineOnInit = true;
                for (int i = 1; i < m_nbFields; i++) {
                    if (m_machine.hasFunction (i+1)) {
                        m_field[i].addObserver (this);
                        //MCP: Set field.m_id to -1 when field 
                        // is used by multiple Scripts (see bellow)
                        m_field[i].m_id = (m_field[i].m_id == 0) ? i+1 : -1;
                        releaseMachineOnInit = false;
                    }
                }
                if (m_machine.hasFunction (0)) {
                    invokeMethod (null, 0, 0);
                }
                //MCP: Release machine if there is no function observing Script fields
                //     and no Javascript API using callbacks called during initialize()
                if (releaseMachineOnInit) {
                    m_machine = null;
                } else {
                    c.scene.registerSleepy (this);
                }
            }
        }
    }

    void destroy (Context c) {
        if (m_machine != null) {
            //Logger.println("Script: cleaning machine for url "+url+" / "+this);
            m_machine = null;
//#ifndef MM.weakreference
            // Cleaning FunctionBank if not used anymore
            DataLink dl = c.getDataLink (m_url.getValue());
            if (dl != null && --dl.m_count == 0) {
                dl.m_object = null;
            }
//#endif
            c.scene.unregisterSleepy (this);
        }
        m_url = null;
        m_c = null;
    }
    
    public boolean compose (Context c, Region clip, boolean forceUpdate) {
        if (m_cbList != null && m_machine != null) {
            Script s = c.script;
            c.script = this;
            invokeCallbacks (c);
            c.script = s;
        }
        return false;
    }

    public void fieldChanged (Field f) {
        if (m_machine != null && f != m_url) {
            if (f.m_id > 0) {
                invokeMethod(f, f.m_id, FixFloat.time2fix (Context.time));
            } else {
                //MCP: When field is shared by multiple Script nodes (by IS) 
                // and each of these scripts have a function matching this field,
                // the functions id cannot be store in the field.
                for (int i = 1; i < m_nbFields; i++) {
                    if (m_field[i] == f) {
                        invokeMethod (f, i+1, FixFloat.time2fix (Context.time));
                        break;
                    }
                }
            }
        }
    }
    
    private void invokeMethod (Field f, int id, int time) {
        if (m_invoking == false) {
            m_invoking = true;
            Script s = m_c.script;
            m_c.script = this;
            m_machine.invoke (m_c, id, f, time);
            // After each call to a method, check for postponed calls
            if (m_postponedCalls != null) {
                invokePostponedCalls (m_postponedCalls, time);
            }
            m_invoking = false;
            m_c.script = s;
        } else {
            // Postponing method call !
            m_postponedCalls = ObjLink.create (f, id, m_postponedCalls);            
        }
    }

    void invokePostponedCalls (ObjLink l, int time) {
        // Call the older calls first (FIFO)
        if (l.m_next != null) {
            invokePostponedCalls (l.m_next, time);
            l.m_next = null;
        }
        m_postponedCalls = null;
        m_machine.invoke (m_c, (int)l.m_z, (Field)l.m_object, time);
        // Immediately call postponed calls generated by this call
        if (m_postponedCalls != null) {
            invokePostponedCalls (m_postponedCalls, time);
        }
    }
    
    public ScriptAccess use (int index) {
        index--;
        if (index > 0 && index < m_nbFields) {
            return m_field [index];
        } else {
//#ifdef api.jsonrpc
            m_lastFieldRef = index + 1; // HACK for functions reference passing
//#endif
            return this;
        }
        
    }
    
    public void set (int index, Register r, int offset) {
        //System.out.println ("Script.set: unexpected call");
    }

    public void get (int index, Register r, int offset) {
        //System.out.println ("Script.get: unexpected call: ");
//#ifdef api.jsonrpc
        r.setInt(m_lastFieldRef); // HACK for functions reference passing
//#endif
    }

}
