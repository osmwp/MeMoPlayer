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

package j2ab.android.io.file;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.microedition.io.file.FileConnection;

import android.os.Environment;
import android.os.StatFs;

public class AndroidFileConnection implements FileConnection {

    private static final String SPECIAL_CHARACTERS = "*.^?[]\\";

    private static final String escape (String s) {
        StringBuffer result = new StringBuffer (s.length ());
        for (int i = 0; i < s.length (); i++) {
            char c = s.charAt (i);
            if (SPECIAL_CHARACTERS.indexOf (c) >= 0) {
                result.append ("\\");
            }
            result.append (c);
        }
        return result.toString ();
    }

    public static final File getFile (String url) {
        URI uri = URI.create (url);
        return new File (uri);
    }

    private File file;
    private boolean open;

    public AndroidFileConnection(String url) {
        this (getFile (url));
    }

    public AndroidFileConnection(File file) {
        this.file = file;
        this.open = true;
    }

    public long availableSize () {
        StatFs statFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
        long blockSize = statFs.getBlockSize();
        return statFs.getAvailableBlocks()*blockSize;
    }

    public boolean canRead () {
        return this.file.canRead ();
    }

    public boolean canWrite () {
        return this.file.canWrite ();
    }

    public void create () throws IOException {
        if (!this.file.createNewFile ()) {
            throw new IOException ("file creation failed");
        }
    }

    public void delete () throws IOException {
        if (!this.file.delete ()) {
            throw new IOException ("file deletion failed");
        }
    }

    public long directorySize (boolean includeSubDirs) throws IOException {
        // why is this in the interface?
        return getDirectorySize (this.file, includeSubDirs);
    }

    private long getDirectorySize (File file, boolean recursive) {
        long size = 0;
        File[] children = file.listFiles ();
        for (File child : children) {
            if (child.isDirectory ()) {
                if (recursive) {
                    size += getDirectorySize (child, recursive);
                }
            } else {
                size += child.length ();
            }
        }
        return size;
    }

    public boolean exists () {
        return this.file.exists ();
    }

    public long fileSize () throws IOException {
        return this.file.length ();
    }

    public String getName () {
        return this.file.getName ();
    }

    public String getPath () {
        return this.file.getPath ();
    }

    public String getURL () {
        return this.file.toURI ().toString ();
    }

    public boolean isDirectory () {
        return this.file.isDirectory ();
    }

    public boolean isHidden () {
        return this.file.isHidden ();
    }

    public boolean isOpen () {
        return this.open;
    }

    public long lastModified () {
        return this.file.lastModified ();
    }

    public Enumeration list () throws IOException {
        return list (null, false);
    }

    public Enumeration list (String filter, boolean includeHidden)
            throws IOException {
        FilenameFilter filefilter = null;
        if (filter != null) {
            String[] literalParts = filter.split ("\\*");
            StringBuffer sb = new StringBuffer (filter.length ());
            for (int i = 0; i < literalParts.length; i++) {
                String part = literalParts[i];
                String literalPart = escape (part);
                if (i > 0) {
                    sb.append ("*");
                }
                sb.append (literalPart);
            }
            final Pattern pattern = Pattern.compile (sb.toString ());
            filefilter = new FilenameFilter () {
                public boolean accept (File dir, String name) {
                    return pattern.matcher (name).matches ();
                }
            };
        }
        List<String> list = new LinkedList<String>();
        File[] files = file.listFiles(filefilter);
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    list.add(f.getName()+"/");
                } else {
                    list.add(f.getName());
                }
            }
        }
        return new Vector<String>(list).elements();
    }

    public void mkdir () throws IOException {
        if (!this.file.mkdir ()) {
            throw new IOException ("unable to create directory");
        }

    }

    public DataInputStream openDataInputStream () throws IOException {
        return new DataInputStream (this.openInputStream ());
    }

    public DataOutputStream openDataOutputStream () throws IOException {
        return new DataOutputStream (this.openOutputStream ());
    }

    public InputStream openInputStream () throws IOException {
        return new FileInputStream (this.file);
    }

    public OutputStream openOutputStream () throws IOException {
        return openOutputStream (0);
    }

    public OutputStream openOutputStream (long byteOffset) throws IOException {
        boolean append;
        if (byteOffset == 0) {
            append = false;
        } else if (byteOffset < this.file.length ()) {
            append = true;
        } else {
            throw new IOException ("offsets not supported");
        }
        FileOutputStream fos = new FileOutputStream (this.file, append);
        return fos;
    }

    public void rename (String newName) throws IOException {
        String parentPath = file.getParent();
        if (parentPath == null) {
            throw new IOException ("no parent dir");
        }
        File old = file;
        file = new File (parentPath, newName);
        if (!old.renameTo (file)) {
            throw new IOException ("could not rename");
        }
    }

    public void setFileConnection (String fileName) throws IOException {
        if (fileName.equals ("..")) {
            File directory = file.getParentFile ();
            if (directory == null) {
                throw new IOException ("no parent dir");
            }
            this.file = directory;
        } else {
            this.file = new File (this.file, fileName);
        }
    }

    public void setHidden (boolean hidden) throws IOException {
        throw new IOException ("unsupported");
    }

    public void setReadable (boolean readable) throws IOException {
        throw new IOException ("unsupported");
    }

    public void setWritable (boolean writable) throws IOException {
        throw new IOException ("unsupported");
    }

    public long totalSize () {
        return this.file.length ();
    }

    public void truncate (long byteOffset) throws IOException {
        throw new IOException ("unsupported");
    }

    public long usedSize () {
        return this.file.length ();
    }

    public void close () throws IOException {
        this.open = false;
    }
}
