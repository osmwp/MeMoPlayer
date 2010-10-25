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
import javax.microedition.io.Connector;
import javax.wireless.messaging.*;

//#ifdef MM.pushSMS
import javax.microedition.io.PushRegistry;
//#endif


public class MessagingHelper {

    /**
     * Send SMS.
     * @return true if the message is sent without error, false otherwise 
     */
    public static boolean sendSMS(String phoneNumber, String msgToSend) {
        MessageConnection clientConn = null;
        try {
            clientConn = (MessageConnection) Connector.open("sms://"
                                                            + phoneNumber);
        } catch (Exception e) {
            System.err.println("Client connection could not be obtained");
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
            return false;
        } finally {
            try {
                clientConn.close();
            } catch (IOException ioExc) {
                System.err.println("Could not close connection");
            }
        }
    }
    
//#ifdef jsr.wma2
    /**
     * Send MMS.
     * @return true if the message is sent without error, false otherwise
     */
    public static boolean sendMMS(String phoneNumber, String title, String msgToSend, String imagePath, Decoder d) {
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
            MessagePart imagePart = new MessagePart(imageBytes, 0, imageBytes.length, "image/jpeg", "id0", "image", null);
            MessagePart textPart = new MessagePart(textBytes, 0, textBytes.length, "text/plain", "id1", "message", "UTF-8");
            
            // Build MMS
            mmsMessage = (MultipartMessage) clientConn.newMessage("multipart");
            mmsMessage.setSubject(title);
            mmsMessage.addMessagePart(imagePart);
            mmsMessage.addMessagePart(textPart);
            
        }catch (Exception e) {
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
//#endif

    
//#ifdef MM.pushSMS
    /**
     * Utilise la fonctionnalité PushRegistry pour lire le contenu d'un SMS arrivé sur un port particulier.
     * Cette fonction permet ici de démarrer l'appplication à l'arrivée d'un SMS et de paramétrer celle-ci
     * avec les données du SMS entrant en enregistrant le contenu dans des Cooky, utilisables ensuite dans le
     * vrml par Browser.getCooky.<br/>
     * Les informations du SMS doivent être de cette forme: key=value;<br/>
     * La key "scn" indique l'URI d'une scêne que l'application va charger au lancement.<br/>
     * Necéssite l'inscription du PushRegistry dynamiquement ou dans le jad:<br/>
     * Ex de build:<attribute name="MIDlet-Push-1" value="sms://:5432, MobilePlatform, *"/>
     * @param wakeUp  true 
     * @return true si un SMS a été lu
     */
    public static boolean receiveSMS(boolean wakeUp) {
        //FT-04/10/07 lancement de l'application par sms (PushRegistry)
        // List of active sms connections                
        String connections[];
        // Check if any inbound connection are awayting
        try {
            connections = PushRegistry.listConnections(wakeUp); // true = only the ones with data waiting, false = all
            
            // if there is any inbound data
            if (connections.length != 0) {
                // We are woken up by the PushRegistry capacity.
                MessageConnection msgConn = (MessageConnection)Connector.open(connections[0]);
                TextMessage aMess = (TextMessage) msgConn.receive();
                String msg=aMess.getPayloadText();
                int begin=0;
                int end=msg.indexOf(";",begin);
                int sep=0;
                while(end>0){
                    sep=msg.indexOf("=",begin);
                    CookyManager.set(msg.substring(begin, sep),msg.substring(sep+1, end));
                    begin=end+1;
                    end=msg.indexOf(";",begin);
                }        
                msgConn.close();
                return true;
            
            }
        } catch (Throwable exp) {
            Logger.println("MessagingHelper: "+exp.toString()+": "+exp.getMessage());
        }
        return false;
    }
//#endif
}
