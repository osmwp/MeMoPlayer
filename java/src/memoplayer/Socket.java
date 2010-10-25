//#condition api.socket
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

import javax.microedition.io.StreamConnection;
import javax.microedition.io.Connector;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.OutputStream;

public class Socket implements Runnable {

    private String socketHost = "";             // Socket host
    private int socketPort = 0;                 // Socket port
    private StreamConnection connection = null; // Connection
    private Thread m_thread = null;             // Data receiver Thread
    private InputStream in = null;              // The input
    private OutputStream out = null;            // The output
    private byte[] buffer = new byte[0];        // Data buffer

    /**
     *  Socket constructor.
     */
    public Socket (String socketHost, int socketPort) {
        Logger.println("Socket new Socket: "+socketHost+":"+socketPort);
        this.socketHost = socketHost;
        this.socketPort = socketPort;
    }

    /**
     *  Request the client to connect to the server.
     */
    public void connect () {
        m_thread = new Thread(this);
        m_thread.start();
    }
    
    /**
     *  Request the client to disconnect from the server.
     *  @param async must be true if asynchronous
     */
    public void disconnect (boolean async) {
        if (async) {
            new Thread () {
                public void run () {
                    disconnect();
                }
            }.start ();
        }
        else {
            disconnect();
        }
    }
    
    /**
     *  Request the client to disconnect from the server.
     */
    private synchronized void disconnect () {
        if (connection == null) {
            Logger.println("Socket not connected!");
            return;
        }
        try {
            in.close();
            out.close();
            connection.close();
            m_thread.join();
        } 
        catch (Exception e) {
            Logger.println("Socket.disconnect Exception : "+e.getMessage());
            e.printStackTrace();
        }
        in = null;
        out = null;
        connection = null;
        Logger.println("Disconnected");
        sendOnMessage("DISCONNECTED");
    }

    /**
     *  Request the client to send a message to the server.
     *  @param msg The message to send
     *  @param async must be true if asynchronous
     */
    public void send (final String msg, boolean async) {
        if (async) {
            new Thread () {
                public void run () {
                    send(msg.getBytes());
                }
            }.start ();
        }
        else {
            send(msg.getBytes());
        }
    }

    /**
     *  Request the client to send a file to the server.
     *  @param fileId The File Id
     *  @param async must be true if asynchronous
     */
    public void sendFile (final int fileId, boolean async) {
        if (async) {
            new Thread () {
                public void run () {
                    send(JSFile.getDataBytes(fileId, false, null));
                }
            }.start ();
        }
        else {
            send(JSFile.getDataBytes(fileId, false, null));
        }
    }
    
    /**
     *  Request the client to send a message to the buffer.
     *  @param msg The message to send
     *  @param async must be true if asynchronous
     */
    public void sendToBuffer (final String msg, boolean async) {
        if (async) {
            new Thread () {
                public void run () {
                    sendToBuffer(msg.getBytes());
                }
            }.start ();
        }
        else {
            sendToBuffer(msg.getBytes());
        }
    }

    /**
     *  Request the client to send a file to the buffer.
     *  @param fileId The File Id
     *  @param async must be true if asynchronous
     */
    public void sendFileToBuffer (final int fileId, boolean async) {
        if (async) {
            new Thread () {
                public void run () {
                    sendToBuffer(JSFile.getDataBytes(fileId, false, null));
                }
            }.start ();
        }
        else {
            sendToBuffer(JSFile.getDataBytes(fileId, false, null));
        }
    }
    
    /**
     *  Request the client to send the buffer to the server (sync or async).
     *  @param async must be true if asynchronous
     */
    public void sendBuffer (boolean async) {
        if (async) {
            new Thread () {
                public void run () {
                    sendBuffer();
                }
            }.start ();
        }
        else {
            sendBuffer();
        }
    }
    
    /**
     *  Request the client to send a byte array to the server.
     *  @param tab The message to send
     */
    private synchronized void send (byte[] tab) {
        Logger.println("Send: "+tab);
        try {
            if (out != null) {
                out.write(tab);
                out.flush();
                Logger.println("Sent: " + tab);
                sendOnMessage("SENT");
            } 
            else {
                Logger.println("Unable to send: " + tab);
                sendOnMessage("EXCEPTION-Unable to send "+tab);
            }
        } 
        catch (IOException e) {
            Logger.println("Unable to send: " + tab + " (Exception) : "+e.getMessage());
            sendOnMessage("EXCEPTION-Unable to send "+tab+" : "+e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     *  Request the client to send a byte array to the buffer.
     *  @param tab the byte array
     */
    private synchronized void sendToBuffer (byte[] tab) {
        byte[] newBuffer = new byte[buffer.length+tab.length];
        System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
        System.arraycopy(tab, 0, newBuffer, buffer.length, tab.length);
        buffer = newBuffer;
        Logger.println("SentToBuffer: " + tab);
        sendOnMessage("SENT_TO_BUFFER");
    }
    
    /**
     *  Request the client to send the buffer to the server.
     */
    private synchronized void sendBuffer () {
        Logger.println("Send buffer: "+buffer);
        try {
            if (out != null) {
                out.write(buffer);
                out.flush();
                buffer = new byte[0];
                Logger.println("Sentbuffer: " + buffer);
                sendOnMessage("SENT_BUFFER");
            } 
            else {
                Logger.println("Unable to sendBuffer: " + buffer);
                sendOnMessage("EXCEPTION-Unable to sendBuffer "+buffer);
            }
        } 
        catch (IOException e) {
            Logger.println("Unable to sendBuffer: " + buffer + " (Exception) : "+e.getMessage());
            sendOnMessage("EXCEPTION-Unable to sendBuffer "+buffer+" : "+e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     *  Start receiver Thread.
     */
    public void run () {
        if (connection != null) {
            Logger.println("Already connected!");
            return;
        }
        try {
            connection = (StreamConnection) Connector.open("socket://"+socketHost+":"+socketPort);
            in = connection.openInputStream();
            out = connection.openOutputStream();
            Logger.println("Socket Connected");
            sendOnMessage("CONNECTED");
        } 
        catch (IOException e) {
            Logger.println("Socket.connect Exception : "+e.getMessage());
            sendOnMessage("EXCEPTION-Socket.connect : "+e.getMessage());
            e.printStackTrace();
            in = null;
            out = null;
            connection = null;
            return;
        }
        if (connection != null) {
            String received = "";
            byte[] startChar = new byte[1];
            byte[] data;
            try {
                 int tempAvailable;
                 while(in.read(startChar,0,1) != -1) {
                     tempAvailable = in.available();
                     data = new byte[tempAvailable+1];
                     data[0] = startChar[0];
                     in.read(data,1,tempAvailable);
                     received = new String(data);
                     Logger.println("Received from server: " + received);
                     sendOnMessage(received);
                 }
            } 
            catch(Exception e) {
                if ((e.getClass().equals(InterruptedIOException.class)) || (e.getClass().equals(IOException.class))) {
                    Logger.println("Thread interrupted");
                }
                else {
                    Logger.println("Unable to receive (Exception) : "+e.getMessage());
                    sendOnMessage("EXCEPTION-Unable to receive : "+e.getMessage());
                    e.printStackTrace();
                }
                in = null;
                out = null;
                connection = null;
            }
        }
    }
    
    /**
     *  Is connected ?
     */
    public boolean isConnected () {
        return connection != null;
    }
    
    /**
     *  Send messages on message node socketHost+":"+socketPort.
     *  @param message The message to send
     */
    private synchronized void sendOnMessage (String message) {
        Message.sendMessage(socketHost+':'+socketPort,message);
    }
}
