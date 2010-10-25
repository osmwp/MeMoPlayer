//#condition api.xparse2
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
import javax.microedition.io.*;
import java.io.*;


public class XParsePush {

    final static int URL = 1; 
    final static int STRING = 2; 
    final static int KEEP_ALIVE = 4; 
    final static int DEBUG = 8; 

    private boolean m_debugMode; // set to true when ORed in contructir param
    public boolean m_again; // flag used to continue/stop the push thread
    public boolean m_refresh = true; // flag when new data are available

    public XNode m_rootNode; // the parsed DOM
    public XNode m_currentNode; // used to navigate through the DOM

    // use to output one string
    final void out (String msg) {  if (m_debugMode) { Logger.println (msg); } }

    // use to output two strings (avoid multiple 
    final void out (String msg1, String msg2) {  if (m_debugMode) { Logger.print (msg1); Logger.println (msg2); } }


    /** Create a parser from data stored in teh URL and according to mode 
        if mode is STRING, use data in URL as XML data
        if mode is URL, use url to retreive a DOM
        
        if DEBUG is ORed to mode, some info messages are displayed
     */
    public XParsePush (final String url, final int mode) {
        m_debugMode =  (mode & DEBUG) == DEBUG;
        out ("XParsePush: "+url+", "+mode);
        if (url == null) {
            out ("XparsePush: invalid null url") ;
            return;
        }
        if ((mode & STRING) == STRING) {
            createFromString (url);
        } else {
            new Thread () {
                public void run () {
                    if ( (mode & KEEP_ALIVE) ==  KEEP_ALIVE) {
                        openUrlAndKeepAlive (url);
                    } else {
                        openUrl (url);
                    }
                    MiniPlayer.wakeUpCanvas();
                }
            }.start ();
        }

    }

    public void close () {
        m_again = false; // finish thead if any
        m_rootNode = null; // clean up DOM
        m_currentNode = null; // clean up DOM
    }

    // create an XML DOM from the param string 
    public void createFromString (String data) {
        out ("XParsePush;createFromString: ", data);
        Xparse parser = new Xparse ();
        m_rootNode = parser.parse (data);
    }

    // open the url and read data to create one DOM
    public void openUrl (String url) {
        File file = new File (url); // open in sync mode => return when data are ready or error occured
        if (file != null && file.getState() == Loadable.READY){ // we have data
            createFromString (file.readAll()); // create the DOM
            file.close (Loadable.CLOSED); // cleanup connection
        }
    }

    // open the url and keep the connection alive to read new XML data when available and create new DOM accordingly
    public void openUrlAndKeepAlive (String url) {
        StringBuffer sb = new StringBuffer(); // to acculuate data from network
        SocketConnection connection = null;
        InputStream is = null;
        OutputStream os = null;

        try {
            byte[] data = null; // placehoder for reading data
            out ("openUrlAndKeepAlive: opening:", url);
            // open stream
            connection = (SocketConnection)Connector.open (url, Connector.READ_WRITE);
            is = connection.openInputStream();
            os = connection.openOutputStream();
            m_again = true;
            while (m_again) {
                int nbChars = is.available();
                out ("XParsePush.openUrlAndKeepAlive: nb chars avail: "+nbChars);

                if (nbChars > 0) {
                    if (data == null || data.length < nbChars) {
                        data = new byte [nbChars];
                    }
                    is.read (data);
                    if (nbChars > 5) { // ignore small keep alive chunks and accumulate data
                        sb.append (new String (data, "UTF-8")); 
                    } else {
                        out ("XParsePush.openUrlAndKeepAlive: ignoring: "+data);
                    }
                    if (sb.length() > 10) { // ignore small content, probably an error or content not yet arrived
                        try { // protect thread agains exception raised by parse because of malformed XML data 
                            createFromString (sb.toString());
                            m_refresh = true;
                        } catch (Exception e){
                            m_rootNode = null; m_refresh=false;
                            out ("XParsePush.openUrlAndKeepAlive: invalid document");
                        }
                        sb.setLength (0);
                    }
                }
                Thread.sleep(1000);
            }
        } catch (Exception e){
            m_rootNode = null;
            if (m_debugMode) {
                out ("XParsePush.openUrlAndKeepAlive: Exception "+e);
                e.printStackTrace();
            }
        }

        try {
            out ("XParsePush.openUrlAndKeepAlive: closing socket");
            if (is != null) { is.close(); }
            if (os != null) { os.close(); }
            if (connection != null) { connection.close(); }
        } catch (Exception e) { Logger.println ("--"+e.getMessage()); }
        is = null;
        connection = null;
    }

}


