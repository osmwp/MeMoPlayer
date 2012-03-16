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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.microedition.io.Connection;
import javax.microedition.io.InputConnection;
import javax.microedition.io.OutputConnection;
import javax.microedition.io.StreamConnection;

public class AndroidURLConnection implements StreamConnection, OutputConnection, InputConnection, Connection {

    protected URLConnection connection;

    public AndroidURLConnection(String url) throws MalformedURLException,
            IOException {
        this (new URL (url));
    }

    public AndroidURLConnection(URL url) throws IOException {
        this (url.openConnection ());
    }

    public AndroidURLConnection(URLConnection connection) {
        this.connection = connection;
    }

    public DataInputStream openDataInputStream () throws IOException {
        return new DataInputStream (openInputStream ());
    }

    public InputStream openInputStream () throws IOException {
        return connection.getInputStream();
    }

    public void close () throws IOException {
        connection = null;
    }

    public DataOutputStream openDataOutputStream () throws IOException {
        return new DataOutputStream (openOutputStream ());
    }

    public OutputStream openOutputStream () throws IOException {
        return connection.getOutputStream ();
    }

}
