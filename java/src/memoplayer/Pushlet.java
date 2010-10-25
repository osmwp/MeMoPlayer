//#condition api.pushlet
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

import java.util.Hashtable;
import nl.justobjects.pushlet.client.Protocol;
import nl.justobjects.pushlet.client.PushletClient;
import nl.justobjects.pushlet.client.PushletClientListener;
import nl.justobjects.pushlet.client.PushletEvent;

public class Pushlet implements PushletClientListener {

    // pushlet servlet URL
    private final String PUSHLET_SIGNAL_URL = "PUSHLET_SIGNAL";
    private final String PUSHLET_SERVLET_URL = MiniPlayer.getJadProperty("PUSHLET_SERVLET_URL");

    // PushletClient instance
    private static PushletClient pushletClient;
    private String tempSubject;
    private String tempKey;
    private String tempValue;
    private Pushlet pushlet = this;

    // Join the server and return id
    public void join () {
        new Thread () {
            public void run() {
                System.out.println("Pushlet.join: servlet '"+PUSHLET_SERVLET_URL+"'");
                String result = "";
                try {
                    pushletClient = new PushletClient(PUSHLET_SERVLET_URL);
                    result = pushletClient.join();
                    Hashtable hashtable = new Hashtable(3);
                    hashtable.put("type", "JOINED");
                    hashtable.put("pushletId", result);
                    buildResponse(hashtable);
                }
                catch (Exception e) {
                    System.out.println("Exception at Pushlet.join");
                    e.printStackTrace();
                    onError("EXCEPTION-JOIN");
                }        
            }
        }.start();
    }

    // Leave the server
    public void leave () {
        new Thread () {
            public void run() {
                System.out.println("Pushlet.leave");
                try {
                    pushletClient.leave();
                    Hashtable hashtable = new Hashtable(3);
                    hashtable.put("type", "LEFT");
                    buildResponse(hashtable);
                }
                catch (Exception e) {
                    System.out.println("Exception at Pushlet.leave");
                    onError("EXCEPTION-LEAVE");
                    e.printStackTrace();
                }
            }
        }.start();
    }

    // Listen a subject in stream mode
    public void listen (String subject) {
        tempSubject = subject;
        new Thread () {
            public void run() {
                System.out.println("Pushlet.listen: subject '"+tempSubject+"'");
                try {
                    pushletClient.listen(pushlet, Protocol.MODE_STREAM, tempSubject);
                    Hashtable hashtable = new Hashtable(3);
                    hashtable.put("type", "LISTENED");
                    hashtable.put("subject", tempSubject);
                    buildResponse(hashtable);
                }
                catch (Exception e) {
                    System.out.println("Exception at Pushlet.listen");
                    onError("EXCEPTION-LISTEN");
                    e.printStackTrace();
                }
            }
        }.start();
    }

    // Stop the listener
    public void stopListen () {
        new Thread () {
            public void run() {
                System.out.println("Pushlet.stopListen");
                try {
                    pushletClient.stopListen();
                    Hashtable hashtable = new Hashtable(3);
                    hashtable.put("type", "STOPPED_LISTEN");
                    buildResponse(hashtable);
                }
                catch (Exception e) {
                    System.out.println("Exception at Pushlet.stopListen");
                    onError("EXCEPTION-STOP_LISTEN");
                    e.printStackTrace();
                }
            }
        }.start();
    }

    // Publish a value at a key on a subject
    public void publish (String subject, String key, String value) {
        tempSubject = subject;
        tempKey = key;
        tempValue = value;
        new Thread () {
            public void run() {
                System.out.println("Pushlet.publish: value '"+tempValue+"' with the key '"+tempKey+"' on subject '"+tempSubject+"'");
                Hashtable attrs = new Hashtable(3);
                attrs.put(tempKey,tempValue);
                try {
                    pushletClient.publish(tempSubject, attrs);
                    Hashtable hashtable = new Hashtable(3);
                    hashtable.put("type", "PUBLISHED");
                    hashtable.put("subject", tempSubject);
                    hashtable.put("key", tempKey);
                    hashtable.put("value", tempValue);
                    buildResponse(hashtable);
                }
                catch (Exception e) {
                    System.out.println("Exception at Pushlet.publish");
                    onError("EXCEPTION-PUBLISH");
                    e.printStackTrace();
                }
            }
        }.start();
    }

    // Abort event from server
    public void onAbort(PushletEvent theEvent) {
        System.out.println("Pushlet.onAbort: " + theEvent);
        Hashtable hashtable = theEvent.getAttributes();
        hashtable.put("type", "ABORT");
        buildResponse(hashtable);
    }

    // Data event from server
    public void onData(PushletEvent theEvent) {
        System.out.println("Pushlet.onData");
        Hashtable hashtable = theEvent.getAttributes();
        hashtable.put("type", "DATA");
        buildResponse(hashtable);
    }

    // Heartbeat event from server
    public void onHeartbeat(PushletEvent theEvent) {
        System.out.println("Pushlet.onHeartbeat");
        Hashtable hashtable = theEvent.getAttributes();
        hashtable.put("type", "HEART_BEAT");
        buildResponse(hashtable);
    }

    // Error occurred
    public void onError(String message) {
        System.out.println("Pushlet.onError: " + message);
        Hashtable hashtable = new Hashtable(3);
        hashtable.put("type", "ERROR");
        hashtable.put("msg",message);
        buildResponse(hashtable);
    }

    // send the hashtable response on PUSHLET_URL message node
    private void buildResponse(Hashtable res) {
        Message.sendMessage(PUSHLET_SIGNAL_URL, Integer.toString(JSArray.addArray(res)));
    }
}
