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

import java.util.*;
import javax.microedition.io.*;

//#ifdef jsr.75
import javax.microedition.io.file.*;

/** 
    Utility class for JSFile 
 */ 
class FileInfo {
    String name;
    String url;
    boolean isDir;
}
//#endif

/**
   This class implements functions for the JavaScript File package 
 */
class JSFile {
    
    // for File operations (open read, close)
    final static int MAX_STREAMS = 32;
    static File [] s_streams = null;

    static void clean (Context c) {
        for (int i = 0; i < MAX_STREAMS; i++) {
            close (c, i);
        }
    }
    
    private static boolean isFileOpen (int idx) {
        return (idx >= 0 && idx < MAX_STREAMS && s_streams != null && s_streams[idx] != null);
    }

    static int open (Context c, String path, boolean async) {
        return open (c, path, File.MODE_READ+(async ? File.MODE_ASYNC : 0) );
    }

    static int open (Context c, String path, int mode) {
        int i = 0;
        if (s_streams == null) {
            s_streams = new File [MAX_STREAMS];
            for (i = 0; i < MAX_STREAMS; i++) { s_streams[i] = null; }
            i = 0;
        } else {
            i = ExternCall.getFreeSlot (s_streams);
        }
        if (i >= 0 && i < MAX_STREAMS) {
            s_streams[i] = new File (path, mode);
            if (c != null) {
                c.addLoadable(s_streams[i]);
            }
            return i;
        }
        return -1;
    }

    static void close (Context c, int idx) {
        if (isFileOpen (idx)) {
            try { s_streams [idx].close (Loadable.CLOSED); } catch (Exception e) { }
            if (c != null) {
                c.removeLoadable(s_streams [idx]);
            }
            s_streams[idx] = null;
        }
    }
    
    static String getLine (int idx) {
        if (isFileOpen (idx)) {
            return (s_streams[idx].readLine ());
        }
        return "";
    }

    static boolean eof (int idx) {
        if (isFileOpen (idx)) {
            return (s_streams[idx].isEof ());
        }
        return (true);
    }

    static String getData (int idx, boolean async, String encoding) {
        if (isFileOpen (idx)) {
//#ifdef MM.base64
            if (encoding.equals("base64")) {
                byte[] buff = s_streams[idx].startReadAllBytes(async);
                return buff != null ? JSBase64Coder.encode(buff) : "";
            }
//#endif
            return s_streams[idx].startReadAll (async, encoding);
        }
        return "";
    }
    
    static byte[] getDataBytes (int idx, boolean async, String mode) {
        if (isFileOpen (idx)) {
            return s_streams[idx].startReadAllBytes(async);
        }
        return new byte[0];
    }

    static int getStatus (int idx) {
        if (isFileOpen (idx)) {
            return (s_streams[idx].getState () | (s_streams[idx].getMode() & File.MODE_WRITE));
        }
        return Loadable.ERROR;
    }
    
    static int getLength (int idx) {
        if (isFileOpen (idx)) {
            return s_streams[idx].getLen();
        }
        return -1;
    }
   
    static boolean writeData (int idx, String data, boolean async, String encoding) {
        if (isFileOpen (idx)) {
            return s_streams[idx].startWriteAll (data, async, encoding);
        }
        return false;
    }
    
    
//#ifdef jsr.75
    // JSR-75 dependent code
    
    // for filesystem management
    static int s_nbFiles;
    static FileInfo [] s_files;
    static String s_path = "file:///";
    
    static int list (String path) {
        if (path.equals ("") || path.equals("file://") || path.equals("file:///") ) {
            return listRoot ();
        } else {
            return listDir (path);
        }
    }

    static int listDir (String path) {
        s_nbFiles = 0;
        FileConnection fc;
        try {
            fc = (FileConnection) Connector.open(path);
        } catch (Exception e) {
            Logger.println ("JSFile: Exception while listing dir: "+e.getMessage());
            return 0;
        }
        if (fc == null) {
            return 0;
        }
        try {
            Enumeration filelist = fc.list ();
            if (filelist == null) {
                return 0;
            }
            while (filelist.hasMoreElements()) {
                s_nbFiles++;
                filelist.nextElement();
            }
            if (s_files == null || s_nbFiles > s_files.length) {
                s_files = new FileInfo [s_nbFiles];
                for (int i = 0; i < s_nbFiles; i++) {
                    s_files[i] = new FileInfo ();
                }
            }
            filelist = fc.list ();
            int i = 0;
            while (filelist.hasMoreElements()) {
                s_files[i].name = (String) filelist.nextElement();
                s_files[i].isDir = (s_files[i].name).endsWith ("/");
                i++;
            }
            fc.close();
        } catch (Exception e) {
            s_nbFiles = 0;
            Logger.println("JSFile.listDir: Exception: "+e.getMessage());
        }
        return s_nbFiles;
    }

    static String getName (int idx) {
        if (idx >= 0 && idx < s_nbFiles) {
            return s_files[idx].name;
        } else {
            Logger.println ("JSFile.getName: out of scope: "+ idx);
            return "";
        }
    }
    
    static boolean isDir (int idx) {
        if (idx >= 0 && idx < s_nbFiles) {
            return s_files[idx].isDir;
        } else {
            System.err.println ("JSFile.isDir: out of scope: "+ idx);
        }
        return false;
    }

    static String getFullPath (int idx) {
        if (idx >= 0 && idx < s_nbFiles) {
            if (idx == 0) {
                String prev = s_path;
                if (s_path.length () > 8) { // 8 chars in "file:///"
                    prev = prev.substring (0, prev.lastIndexOf ('/'));
                }
                return (prev);
            } else {
                return (s_path+s_files[idx].name);
            }
        } else {
            Logger.println ("JSFile.getFullPath: out of scope: "+ idx);
        }
        return "";
    }
    
    static int listRoot () {
        s_nbFiles = 0;

        try {
            Enumeration rootlist = FileSystemRegistry.listRoots();
            if (rootlist == null) {
                return 0;
            }
            while (rootlist.hasMoreElements()) {
                s_nbFiles++;
                rootlist.nextElement();
            }
            if (s_files == null || s_nbFiles > s_files.length) {
                s_files = new FileInfo [s_nbFiles];
                for (int i = 0; i < s_nbFiles; i++) {
                    s_files[i] = new FileInfo ();
                }
            }
            rootlist = FileSystemRegistry.listRoots();
            int i = 0;
            while (rootlist.hasMoreElements()) {
                s_files[i].name = (String) rootlist.nextElement();
                s_files[i].isDir = true;
                i++;
            }
        } catch (Exception e) {
            s_nbFiles = 0;
            Logger.println("JSFile.listRoot: Exception: "+e.getMessage());
        }
        return s_nbFiles;
    }
//#endif
    
}
