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

package javax.microedition.io;

import j2ab.android.io.AndroidHttpConnection;
import j2ab.android.io.AndroidURLConnection;
import j2ab.android.io.file.AndroidFileConnection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.wireless.messaging.MessageConnection;

import android.util.Log;

public class Connector {
    public static final int READ = 0x01;
    public static final int WRITE = 0x02;
    public static final int READ_WRITE = READ | WRITE;

    private static final String PROTOCOL_FILE = "file:";
    private static final String PROTOCOL_HTTP = "http:";
    private static final String PROTOCOL_HTTPS = "https:";
    private static final String PROTOCOL_SMS = "sms:";

    public static final Connection open (String name) throws IOException {
        return open (name, READ_WRITE);
    }

    public static final Connection open (String name, int mode) throws IOException {
        Connection connection;
        if (name.startsWith (PROTOCOL_FILE)) {
            connection = new AndroidFileConnection (name);
        } else if (name.startsWith (PROTOCOL_HTTP) || name.startsWith (PROTOCOL_HTTPS)) {
            connection = new AndroidHttpConnection (name);
        } else if (name.startsWith(PROTOCOL_SMS)) {
            connection = new MessageConnection(name);
        } else {
            Log.e ("Connector", "Sorry, unsuported scheme for: "+name);
            connection = new AndroidURLConnection (name);
        }
        return connection;
    }

    public static final DataInputStream openDataInputStream (String name) throws IOException {
        return new DataInputStream (openInputStream (name));
    }

    public static final DataOutputStream openDataOutputStream (String name) throws IOException {
        return new DataOutputStream (openOutputStream (name));
    }

    public static final InputStream openInputStream (String name) throws IOException {
        Connection connection = open (name, READ);
        return ((InputConnection) connection).openInputStream ();
    }

    public static final OutputStream openOutputStream (String name) throws IOException {
        Connection connection = open (name, WRITE);
        return ((OutputConnection) connection).openOutputStream ();
    }
}
