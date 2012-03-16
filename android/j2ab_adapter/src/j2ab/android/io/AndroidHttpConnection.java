/*
 * Copyright (C) 2009 The J2AB Project
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

package j2ab.android.io;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;

import javax.microedition.io.HttpConnection;

import android.util.Log;

public class AndroidHttpConnection extends AndroidURLConnection implements HttpConnection {
    private final static String TAG = "AndroidHttpConnection";
    private HttpURLConnection connection;
    
    public AndroidHttpConnection(String url) throws MalformedURLException, IOException {
        this (new URL(url));
        Log.i (TAG, "constructor: url:"+url);
    }

    public AndroidHttpConnection(URL url) throws IOException {
        this (url.openConnection ());
    }

    public AndroidHttpConnection(URLConnection connection) throws MalformedURLException, IOException {
        super (connection);
        this.connection = (HttpURLConnection)connection;
        this.connection.setInstanceFollowRedirects (false);
        // Disable Gzip compression for newer Android implementation
        // When Gzip compression is enabled, content-length is not the size of the content
        // - MeMo non compressed contents are quite small (XML/BML)
        // - Prevents use of a progress bar for download progress
        this.connection.setRequestProperty("Accept-Encoding", "identity");
    }

    public long getDate () {
        return 0;
    }

    public long getExpiration () {
        return 0;
    }

    public String getHeaderField (String key) {
        return connection.getHeaderField (key);
    }

    public String getHeaderField (int n) {
        return connection.getHeaderField (n);
    }

    public long getHeaderFieldDate (String name, long def) {
        return connection.getHeaderFieldDate (name, def);
    }

    public int getHeaderFieldInt (String name, int def) {
        return connection.getHeaderFieldInt (name, def);
    }

    public String getHeaderFieldKey (int n) {
        return connection.getHeaderFieldKey (n);
    }

    public String getHost () {
        return connection.getURL ().getHost ();
    }

    public long getLastModified () {
        return connection.getLastModified ();
    }

    public int getPort () {
       return connection.getURL ().getPort ();
    }

    public String getProtocol () {
        return connection.getURL ().getProtocol ();
    }

    public String getQuery () {
        return connection.getURL ().getQuery (); 
    }

    public String getRef () {
        return connection.getURL ().getRef ();
    }

    public String getRequestMethod () {
        return connection.getRequestMethod ();
    }

    public String getRequestProperty (String key) {
        return connection.getRequestProperty (key);
    }

    public int getResponseCode () throws IOException {
        return connection.getResponseCode ();
    }

    public String getResponseMessage () throws IOException {
        return connection.getResponseMessage ();
    }

    public String getURL () {
        return connection.getURL ().toString ();
    }

    public void setRequestMethod (String method) throws IOException {
        try { 
            connection.setRequestMethod (method);
            if (HttpConnection.POST.equals (method)) {
                connection.setDoOutput (true);
            }
            connection.setDoInput (true);
        }
        catch (ProtocolException e) {
            throw new IOException (e.getMessage ());
        }
    }

    public void setRequestProperty (String key, String value) {
        connection.setRequestProperty (key, value);
    }

    public String getEncoding () {
        return connection.getHeaderField ("content-encoding");
    }

    public long getLength () {
    	String l = connection.getHeaderField ("content-length");
    	if (l != null) {
    	    try { 
    	        return Long.parseLong (l);  
    	    } catch (NumberFormatException e) {}
        }
        return -1;
    }

    public String getType () {
        return connection.getHeaderField ("content-type");
    }

}
