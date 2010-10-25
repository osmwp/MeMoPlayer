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

public interface SocketConnection extends StreamConnection {
    static byte DELAY = 0;
    static byte LINGER = 1;
    static byte KEEPALIVE = 2;
    static byte RCVBUF = 3;
    static byte SNDBUF = 4;

    String getAddress ();

    String getLocalAddress ();

    int getLocalPort ();

    int getPort ();

    int getSocketOption (byte option);

    void setSocketOption (byte option, int value);

}