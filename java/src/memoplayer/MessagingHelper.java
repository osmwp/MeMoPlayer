//#condition jsr.wma
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

import java.io.IOException;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.wireless.messaging.*;

//#ifdef MM.pushSMS
import javax.microedition.io.PushRegistry;

//#endif

public class MessagingHelper {

    /**
     * Send SMS.
     * 
     * @return true if the message is sent without error, false otherwise
     */
    public static boolean sendSMS(String phoneNumber, String msgToSend) {
        MessageConnection clientConn = null;
        try {
            clientConn = (MessageConnection) Connector.open("sms://"
                    + phoneNumber);
        } catch (Exception e) {
            System.err.println("Client connection could not be obtained");
            Logger.println("Client connection could not be obtained");
            return false;
        }
        TextMessage tmsg = (TextMessage) clientConn
                .newMessage(MessageConnection.TEXT_MESSAGE);
        tmsg.setPayloadText(msgToSend);
        try {
            clientConn.send(tmsg);
            return true;
        } catch (Exception e) {
            System.err.println("Could not send message");
            Logger.println("Could not send message");
            Logger.println(" " + e);
            return false;
        } finally {
            try {
                clientConn.close();
            } catch (IOException ioExc) {
                System.err.println("Could not close connection");
            }
        }
    }

    // #ifdef jsr.wma2
    /**
     * Send MMS.
     * 
     * @return true if the message is sent without error, false otherwise
     */
    public static boolean sendMMS(String phoneNumber, String title,
            String msgToSend, String imagePath, Decoder d) {
        MessageConnection clientConn = null;
        MultipartMessage mmsMessage = null;
        try {
            clientConn = (MessageConnection) Connector.open("mms://"
                    + phoneNumber);
        } catch (Exception e) {
            System.err.println("Client connection could not be obtained");
            return false;
        }

        try {
            // Load image as ByteArray
            File imageFile = new File(imagePath);

            // TODO verify file type (jpeg, png, ...)
            // TODO check image length and allow message sending
            byte[] imageBytes = imageFile.readBytes(imageFile.getLen());

            // Load text as ByteArray
            byte[] textBytes = msgToSend.getBytes("UTF-8");

            // Build parts
            MessagePart imagePart = new MessagePart(imageBytes, 0,
                    imageBytes.length, "image/jpeg", "id0", "image", null);
            MessagePart textPart = new MessagePart(textBytes, 0,
                    textBytes.length, "text/plain", "id1", "message", "UTF-8");

            // Build MMS
            mmsMessage = (MultipartMessage) clientConn.newMessage("multipart");
            mmsMessage.setSubject(title);
            mmsMessage.addMessagePart(imagePart);
            mmsMessage.addMessagePart(textPart);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("could not build message");
            return false;
        }

        try {
            clientConn.send(mmsMessage);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Could not send message");
            return false;
        } finally {
            try {
                clientConn.close();
            } catch (IOException ioExc) {
                System.err.println("Could not close connection");
            }
        }
    }

    // #endif

    // #ifdef MM.pushSMS

    static String midletClass = "memoplayer.MiniPlayer";
    static boolean pushAvailable = false;
    static {
        // check if PushRegistry is available on the device
        Class pushClass = null;
        try {
            pushClass = Class.forName("javax.microedition.io.PushRegistry");
            if (pushClass != null)
                pushAvailable = true;
        } catch (Exception e) {
        } catch (Throwable t) {
        }
    }

    /**
     * Utilise la fonctionnalité PushRegistry pour lire le contenu d'un SMS
     * arrivé sur un port particulier. Cette fonction permet ici de démarrer
     * l'appplication à l'arrivée d'un SMS et de paramétrer celle-ci avec les
     * données du SMS entrant en enregistrant le contenu dans des Cooky,
     * utilisables ensuite dans le vrml par Browser.getCooky.<br/>
     * Les informations du SMS doivent être de cette forme: key=value;<br/>
     * La key "scn" indique l'URI d'une scêne que l'application va charger au
     * lancement.<br/>
     * Necéssite l'inscription du PushRegistry dynamiquement ou dans le jad:<br/>
     * Ex de build:<attribute name="MIDlet-Push-1"
     * value="sms://:5432, MobilePlatform, *"/>
     * 
     * @param wakeUp
     *            true
     * @return true si un SMS a été lu
     */
    public static boolean receiveSMS(boolean wakeUp) {
        if (pushAvailable == false) {
            return false;
        }

        // FT-04/10/07 lancement de l'application par sms (PushRegistry)
        // List of active sms connections
        String connections[];
        // Check if any inbound connection are awaiting
        try {
            connections = PushRegistry.listConnections(wakeUp); // true = only
                                                                // the ones with
                                                                // data waiting,
                                                                // false = all

            // if there is any inbound data
            if (connections.length != 0) {

                String wakeUpConnection = connections[0];

                // We are woken up by the PushRegistry capacity.
                MessageConnection msgConn = (MessageConnection) Connector
                        .open(wakeUpConnection);
                TextMessage txtMsg = (TextMessage) msgConn.receive();
                CookyManager.set("PUSH_DATA", txtMsg.getPayloadText());

                if (wakeUpConnection.startsWith("sms://:")) {
                    wakeUpConnection = wakeUpConnection.substring(7);
                }
                CookyManager.set("PUSH_PORT", wakeUpConnection);

                String sender = txtMsg.getAddress();
                if (sender.startsWith("sms://")) {
                    sender = sender.substring(6);
                }
                CookyManager.set("PUSH_SENDER", sender);

                // Logger.println("PUSH_DATA: "+txtMsg.getPayloadText());
                // Logger.println("PUSH_PORT: "+wakeUpConnection);
                // Logger.println("PUSH_SENDER: "+sender);

                msgConn.close();
                return true;

            }
        } catch (Throwable exp) {
            Logger.println("MessagingHelper: " + exp.toString() + ": "
                    + exp.getMessage());
        }
        return false;
    }

    public static boolean registerConnection(int port) {
        if (pushAvailable == false) {
            return false;
        }

        try {
            // check parameters
            if ((port < 0) || (port > 65535))
                return false;

            // check reserved ports
            for (int i = s_forbiddenPorts.length - 1; i >= 0; i--) {
                if (port == s_forbiddenPorts[i]) {
                    Logger.println("registerConnection on forbidden port: "
                            + port);
                    return false;
                }
            }

            String pushConnection = "sms://:" + port;

            Logger.println("registerConnection " + midletClass + " on "
                    + pushConnection);

            PushRegistry.registerConnection(pushConnection, midletClass, "*");

            Logger.println("registerConnection OK");

            return true;

        } catch (Exception e) {
            Logger.println("registerConnection failed: " + e);
        }

        return false;
    }

    public static boolean unregisterConnection(int port) {
        if (pushAvailable == false) {
            return false;
        }

        // check parameters
        if ((port < 0) || (port > 65535))
            return false;

        // check reserved ports
        for (int i = s_forbiddenPorts.length - 1; i >= 0; i--) {
            if (port == s_forbiddenPorts[i]) {
                Logger.println("unregisterConnection on forbidden port: "
                        + port);
                return false;
            }
        }

        String pushConnection = "sms://:" + port;
        return PushRegistry.unregisterConnection(pushConnection);
    }

    public static int isRegisteredConnection(int port) {
        if (pushAvailable == false) {
            return -3;
        }

        // check parameters
        if ((port < 0) || (port > 65535))
            return -2;

        // check reserved ports
        for (int i = s_forbiddenPorts.length - 1; i >= 0; i--) {
            if (port == s_forbiddenPorts[i]) {
                Logger.println("isRegisteredConnection on forbidden port: "
                        + port);
                return -2;
            }
        }

        String pushConnection = "sms://:" + port;
        String pushedMidlet = PushRegistry.getMIDlet(pushConnection);
        if (pushedMidlet != null) {
            if (pushedMidlet.equalsIgnoreCase(midletClass)) {
                Logger.println("found push registered midlet on port " + port);
                return 1;
            }
            Logger.println("unknown push registered midlet: " + pushedMidlet);
            return -1;
        }

        Logger.println("isRegisteredConnection: " + port + " not found");
        return 0;
    }

    // #endif

    static Vector s_smsListeners = new Vector();

    static final int s_forbiddenPorts[] = { 2805, // WAP WTA secure
                                                  // connection-less session
                                                  // service
            2923, // WAP WTA secure session service
            2948, // WAP Push connectionless session service (client side)
            2949, // WAP Push secure connectionless session service (client
                  // side)
            5502, // Service Card reader
            5503, // Internet access configuration reader
            5508, // Dynamic Menu Control Protocol
            5511, // Message Access Protocol
            5512, // Simple Email Notification
            9200, // WAP connectionless session service
            9201, // WAP session service
            9202, // WAP secure connectionless session service
            9203, // WAP secure session service
            9207, // WAP vCal Secure
            49996,// SyncML OTA configuration
            49999 // WAP OTA configuration
    };

    public static boolean startListenSMS(int port, String messageName) {

        // check parameters
        if ((port < 0) || (port > 65535))
            return false;

        // check reserved ports
        for (int i = s_forbiddenPorts.length - 1; i >= 0; i--) {
            if (port == s_forbiddenPorts[i])
                return false;
        }

        // check if not already listening
        for (int i = (s_smsListeners.size() - 1); i >= 0; i--) {
            SmsListener smsListener = (SmsListener) s_smsListeners.elementAt(i);
            if (smsListener.m_port == port) {
                return false;
            }
        }

        // starts listening and store object
        SmsListener newSmsListener = new SmsListener(port, messageName);
        if (newSmsListener.start() == false) {
            Logger.println("startListenSMS failed on port: " + port);
            return false;
        }

        s_smsListeners.addElement(newSmsListener);

        return true;
    }

    public static boolean stopListenSMS(int port) {

        // search in vector
        for (int i = (s_smsListeners.size() - 1); i >= 0; i--) {
            SmsListener smsListener = (SmsListener) s_smsListeners.elementAt(i);
            if (smsListener.m_port == port) {
                // remove object
                s_smsListeners.removeElementAt(i);
                smsListener.stop();
                smsListener = null;
                return true;
            }
        }

        // not found
        return false;
    }

    public static void stopAllSmsListener() {
        for (int i = (s_smsListeners.size() - 1); i >= 0; i--) {
            SmsListener smsListener = (SmsListener) s_smsListeners.elementAt(i);
            // remove object
            s_smsListeners.removeElementAt(i);
            smsListener.stop();
            smsListener = null;
        }
    }

    // A message listener that spawns a new thread every
    // time a message arrives, receives the message on
    // that thread, and then forwards the message (or any
    // exception) to a waiting message receiver.

    static class SmsListener implements MessageListener {

        boolean stop = false;
        MessageConnection m_smsConn;
        int m_port = -1;
        String m_msgName;

        public SmsListener(int port, String messageName) {
            m_port = port;
            m_msgName = messageName;
        }

        public boolean start() {
            try {
                // Open the connection on the specified port
                m_smsConn = (MessageConnection) Connector.open("sms://:"
                        + m_port);
                m_smsConn.setMessageListener(this);
                return true;
            } catch (Exception e) {
                Logger.println("Error opening SMS connection on port: "
                        + m_port + " " + e);
            }
            return false;
        }

        // Stops the processing of messages.
        public void stop() {
            stop = true;
            try {
                if (m_smsConn != null) {
                    m_smsConn.close();
                }
            } catch (IOException e) {
                Logger.println("Error closing SMS port: " + m_port + " " + e);
            }

            m_smsConn = null;
        }

        // Called whenever a message arrives for the given
        // connection. Starts a thread to receive and forward
        // the message.

        public void notifyIncomingMessage(MessageConnection messageConnection) {
            Logger.println("Received a new SMS on port: " + m_port);
            if (!stop) {
                new Runner(this).start();
            }
        }

        // Helper class: when started, receives a message
        // and forwards it to the ultimate message receiver.
        private class Runner extends Thread {
            private SmsListener m_smsListener;

            Runner(SmsListener smsListener) {
                this.m_smsListener = smsListener;
            }

            public void run() {
                javax.wireless.messaging.Message message = null;
                Throwable exception = null;

                try {
                    message = m_smsListener.m_smsConn.receive();
                } catch (Throwable e) {
                    exception = e;
                }

                if (message instanceof BinaryMessage) {
                    Logger.println("Received and ignoring binary SMS from: "
                            + message.getAddress());
                    return;
                }

                MFString mf = new MFString(3);
                String sender = message.getAddress();
                // filter protocole from sender phone number
                if (sender.startsWith("sms://")) {
                    sender = sender.substring(6);
                    int idx2pt = sender.indexOf(':');
                    if (idx2pt > 1) {
                        sender = sender.substring(0, idx2pt);
                    }
                }

                // filter port number from sender
                if (sender.endsWith(":" + m_port)) {
                    int idx2pt = sender.lastIndexOf(':');
                    sender = sender.substring(0, idx2pt);
                }

                // if no message specified
                if ((m_smsListener.m_msgName == null)
                        || (m_smsListener.m_msgName.length() < 1)) {
                    mf.setValue(0, "SMS");
                    mf.setValue(1, ((TextMessage) message).getPayloadText());
                    mf.setValue(2, "" + sender);
                    mf.setValue(3, "" + m_port);
                    Message.sendMessage("MEMOPLAYER", mf);
                } else {
                    mf.setValue(0, "" + sender);
                    mf.setValue(1, "" + m_port);
                    mf.setValue(2, ((TextMessage) message).getPayloadText());
                    Message.sendMessage(m_smsListener.m_msgName, mf);
                }
                MiniPlayer.wakeUpCanvas();
            }
        }
    }
}
