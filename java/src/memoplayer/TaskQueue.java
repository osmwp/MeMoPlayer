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

public class TaskQueue implements Runnable  {
    private final static byte STATE_STOPPED = 0;
    private final static byte STATE_WORK    = 1;
    private final static byte STATE_PAUSE   = 2;
    private final static byte STATE_PAUSED  = 3;
    private final static byte STATE_QUIT    = 4;
    
    private static TaskQueue s_self;
    public static TaskQueue getInstance() {
        if (s_self == null) {
            s_self = new TaskQueue();
        }
        return s_self;
    }
    private Object m_lock = new Object();
    private Link m_tail;
    private int m_countId = 0;
    private int m_nbTasks = 0;
    private int m_state = STATE_STOPPED;
    private Thread m_thread;
   
    public void run() {
        //System.out.println("WorkerThread: Starting.");
        while (m_state != STATE_QUIT) {
            Task task = null;
            
            synchronized (m_lock) {
                while (m_state != STATE_QUIT && task == null) {
                    //System.out.println("WorkerThread: Checking for new task.");
                    task = (Task)Link.removeFromHead(m_tail);
                    if (task == null || m_state == STATE_PAUSE) {
                        //System.out.println("WorkerThread: Sleeping...");
                        m_state = STATE_PAUSED;
                        m_lock.notify();
                        try { m_lock.wait(); } catch (InterruptedException e) {}               
                        //System.out.println("WorkerThread: Wakeup !");
                    }
                }
            }        
        
            if (task != null) {
                //System.out.println("WorkerThread: Working on task: "+task.getId());
                task.setState(Task.STATE_RUNNING);
                try {
                    task.doTheJob();
                    task.setState(Task.STATE_FINISHED);
                    task.onSuccess();
                } catch (Exception e) {
                    task.setState(Task.STATE_ERROR);
                    if (task.onError(e)) 
                        continue; // Task asked for a new attempt
                }
                //System.out.println("WorkerThread: Finished.");
                Thread.yield();
            }
        }
        //System.out.println("WorkerThread: Exiting.");
        m_state = STATE_STOPPED;
    }
    
    
    public Object runTaskNow (Task task) {
        boolean pauseThread;
        
        synchronized (m_lock) {
           pauseThread = m_state != STATE_STOPPED && m_state != STATE_PAUSED;
        
        
        // Wait for the worker thread to first terminate its job
        if (pauseThread) {
            //System.out.println("MainThread: Pausing WorkerThread");
                m_state = STATE_PAUSE;
                // Wait for worker thread to notify it is paused
                try { m_lock.wait(); } catch (InterruptedException e) {}
                //if (m_state != STATE_PAUSED) {
                    //System.err.println("ERROR: WorkerThread notified but not paused !!");
                //}
            //System.out.println("MainThread: WorkerThread paused");
        }
        }
        
        // Run the given task
        boolean retry;
        do {
            task.setState(Task.STATE_RUNNING);
            retry = false;
            try {
                task.doTheJob();
                task.setState(Task.STATE_FINISHED);
                task.onSuccess();
            } catch (Exception e) {
                task.setState(Task.STATE_ERROR);
                retry = task.onError(e);
            }
        } while (retry);
        
        // if we paused the worker thread, restart it
        if (pauseThread) {
            //System.out.println("MainThread: restart WorkerThread ");
            synchronized (m_lock) {
               m_state = STATE_WORK;
               m_lock.notify();
            }
        }
        
        return task.getResult(); 
    }
    
    public int addTask (Task task) {
        synchronized (m_lock) {
            m_tail = Link.addToTail(task, m_tail);
            m_nbTasks++;
            task.setId(m_countId++);
            task.setState(Task.STATE_READY);
            
            // Start or wake up worker thread
            m_state = STATE_WORK;
            if(m_thread == null) {
                m_thread = new Thread(this);
                m_thread.setPriority(Thread.MIN_PRIORITY);
                m_thread.start();
            } else {
                m_lock.notify();                
            }
            return m_countId;
        }
    }
    
    public void cancelTask (Task task) {
        synchronized (m_lock) {
            if (task.getState() != Task.STATE_READY) 
                return;
            
            if (m_tail != null) {
               m_tail = m_tail.remove(task);
            }
        }
    }
    
    public void cancelAll () {
        synchronized (m_lock) {
            m_tail = null;
            m_nbTasks = 0;
        }
    }
    
    public void quit() {
        synchronized (m_lock) {
            if (m_state == STATE_STOPPED) 
                return;
            m_state = STATE_QUIT;
        
            if (m_thread != null) {
                //synchronized (m_lock) {
                m_lock.notify();                    
        //}
        try { m_thread.join(); } catch (InterruptedException e) {}
                m_thread = null;
            }
            cancelAll();
        }
    }
    
    
    final static int MAX_TASKS = 16;
    static Task [] s_tasks;
    
    static void doJsonRpc (Machine mc, Context c, int m, Register [] registers, int r, int nbParams) {
        Task task = null;
        switch (m) {
        case 0: // call ( async, method, [params, ...], 'PARAMS_END')
        {
            Object[] params = JSArray.getParams(registers, r+2, nbParams-2);
            task = new JsonTask(registers[r+1].getString(), params, params.length, 0);
            break;
        }
        case 1: // getResult (id)
        {
            int id = registers[r].getInt();
            if (s_tasks != null && id>=0 && id<MAX_TASKS && s_tasks[id] != null) {
                JSArray.setParam(registers[r], s_tasks[id].getResult());
                s_tasks[id] = null;
                return;
            }
        }
        case 2: // cancel (id)
        {
            int id = registers[r].getInt();
            if (s_tasks != null && id>=0 && id<MAX_TASKS && s_tasks[id] != null) {
                TaskQueue.getInstance().cancelTask(s_tasks[id]);
                s_tasks[id] = null;
            }
        }
        case 3: // addCallback (taskId, signal, cb, [data, ...],'PARAMS_END')
        {
            int id = registers[r].getInt();
            if (s_tasks != null && id>=0 && id<MAX_TASKS && s_tasks[id] != null) {
                String signal = registers[r+1].getString();
                int function = registers[r+2].getInt();
                if (!mc.hasFunction(function)) {
                    System.err.println("addCallback: Target method ref="+function+" is undefined !");
                    registers[r].setInt(-1);
                    return;
                }
                if (registers[r+nbParams-1].getString().equals("PARAMS_END")) nbParams--;
                nbParams -= 3;
                Register[] params = new Register[nbParams+2]; // +2 : first param is Id, last param is set by listenner
                params[0] = new Register(); params[0].setInt(id); // First param : task id
                while (nbParams > 0) { // copy all data args
                    params[nbParams] = new Register();
                    params[nbParams].set(registers[r+3+nbParams-1]);
                    nbParams--;
                }
                s_tasks[id].addTaskListener(new TaskListenerCb(c.scene, c.script, function, params, signal));
                registers[r].setInt(1);
                return;
            }
        }
        case 4: // clean()
            clean();
            return;
        case 5: // ALL_STATES
            registers[r].setString("allStates");
            return;
        case 6: // ON_PROGRESS
            registers[r].setString("onProgress");
            return;
        case 7: // ON_START
            registers[r].setString("onStart");
            return;
        case 8: // ON_SUCCESS
            registers[r].setString("onSuccess");
            return;
        case 9: // ON_ERROR
            registers[r].setString("onError");
            return;
        default:
            System.err.println ("dotransfer (m:"+m+"): Static call: Invalid method");
            return;
        }

        // register new task
        if (m < 3 && task != null) {

            if (registers[r].getBool()) {
                if (s_tasks == null) {
                    s_tasks = new Task[MAX_TASKS];
                }
                for (int id=0; id<MAX_TASKS; id++) {
                    if (s_tasks[id] == null) {
                        s_tasks[id] = task;
                        TaskQueue.getInstance().addTask(task);
                        registers[r].setInt(id);
                        return;
                    }
                }
            } else {
                JSArray.setParam(registers[r], TaskQueue.getInstance().runTaskNow(task));
                return;
            }
        }
        registers[r].setInt(-1);
    }
    
    static void clean() {
        TaskQueue.getInstance().cancelAll();
        if (s_tasks != null){
            for (int i=0; i<s_tasks.length; i++) {
                if (s_tasks[i] != null) {
                    s_tasks[i] = null;
                }
            }
        }
    }
}
