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

package javax.microedition.io.file;

import java.io.IOException;

import javax.microedition.io.InputConnection;
import javax.microedition.io.OutputConnection;
import javax.microedition.io.StreamConnection;

public interface FileConnection extends javax.microedition.io.Connection,
        InputConnection, OutputConnection, StreamConnection {

    long availableSize ();

    boolean canRead ();

    boolean canWrite ();

    void create () throws IOException;

    void delete () throws IOException;

    long directorySize (boolean includeSubDirs) throws IOException;

    boolean exists ();

    long fileSize () throws IOException;

    java.lang.String getName ();

    java.lang.String getPath ();

    java.lang.String getURL ();

    boolean isDirectory ();

    boolean isHidden ();

    boolean isOpen ();

    long lastModified ();

    java.util.Enumeration list () throws IOException;

    java.util.Enumeration list (java.lang.String filter, boolean includeHidden)
            throws IOException;

    void mkdir () throws IOException;

    java.io.DataInputStream openDataInputStream () throws IOException;

    java.io.DataOutputStream openDataOutputStream () throws IOException;

    java.io.InputStream openInputStream () throws IOException;

    java.io.OutputStream openOutputStream () throws IOException;

    java.io.OutputStream openOutputStream (long byteOffset) throws IOException;

    void rename (java.lang.String newName) throws IOException;

    void setFileConnection (java.lang.String fileName) throws IOException;

    void setHidden (boolean hidden) throws IOException;

    void setReadable (boolean readable) throws IOException;

    void setWritable (boolean writable) throws IOException;

    long totalSize ();

    void truncate (long byteOffset) throws IOException;

    long usedSize ();
}
