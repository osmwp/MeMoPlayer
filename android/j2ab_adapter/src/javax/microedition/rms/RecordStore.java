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

package javax.microedition.rms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Hashtable;

import javax.microedition.midlet.MIDlet;

import android.util.Log;

public class RecordStore {
    private static final String TAG = "RecordStore";
    
    private static final int DEFAULT_MAX_RECORDSTORE_SIZE = 10 * 1024 * 1024;

    //private static final String DATA_EXT = ".dat";

    private static Hashtable<String, RecordStore> CachedRecordStores = new Hashtable<String, RecordStore>();
    private static Hashtable<String, RecordStore> OpenedRecordStores = new Hashtable<String, RecordStore>();
    private static File RootDir;
    
    private static File getRootDir () throws RecordStoreException {
        if (RootDir == null) {
            try {
                RootDir = MIDlet.DEFAULT_MIDLET.getContext ().getFileStreamPath ("RMS");
                if (!RootDir.exists ()) {
                    RootDir.mkdirs ();
                }
                Log.i (TAG, "getRootDir: "+RootDir);
            } catch (Exception ex) {
                Log.i (TAG, "getRootDir: EXCEPTION: "+ex);
                throw new RecordStoreException ("Could not get RMS directory !");
            }
        }
        return RootDir;
    }
    
    private static final File getRecordStoreDirectory (String recordStoreName) throws RecordStoreException {
        try {
            return new File (getRootDir (), recordStoreName+'/');
        } catch (Exception ex) {
            Log.i (TAG, "getRecordStoreDirectory: EXCEPTION: "+ex);
            throw new RecordStoreException ("couldn't get dir " + recordStoreName, ex);
        }
    }

    

    public static RecordStore openRecordStore (String recordStoreName, boolean createIfNecessary) throws RecordStoreException {
        if (OpenedRecordStores.containsKey (recordStoreName)) {
            RecordStore store = OpenedRecordStores.get (recordStoreName);
            store.count++;
            //Log.i(TAG, "openRecordStore: reusing "+store.count+" for "+recordStoreName);
            return store;
        }
        RecordStore store;
        if (CachedRecordStores.containsKey (recordStoreName)) {
            store = CachedRecordStores.get(recordStoreName);
        } else {
            File directory = getRecordStoreDirectory (recordStoreName);
            if (!directory.exists ()) {
                if (createIfNecessary) {
                    try {
                        if (!directory.mkdir ()) {
                            Log.i (TAG, "openRecordStore: EXCEPTION ! ");
                            throw new RecordStoreException ("couldn't create record store " + recordStoreName);
                        }
                        if (!directory.isDirectory ()) {
                            Log.i (TAG, "openRecordStore: EXCEPTION : NOT A DIRECTORY:  "+directory.getPath ());
                            throw new RecordStoreException ("couldn't create record store " + recordStoreName);
                        }
                    } catch (Exception ex) {
                        Log.i (TAG, "openRecordStore: EXCEPTION: "+ex);
                        throw new RecordStoreException ( "couldn't create record store " + recordStoreName, ex);
                    }
                } else {
                    Log.i (TAG, "openRecordStore: EXCEPTION: NOT FOUND ! ");
                    throw new RecordStoreNotFoundException ("no record store "+ recordStoreName);
                }
            }
            store = new RecordStore (directory, recordStoreName);
            CachedRecordStores.put(recordStoreName, store);
        }
        OpenedRecordStores.put (recordStoreName, store);
        //Log.i(TAG, "openRecordStore: new store for "+recordStoreName);
        return store;
    }

    public static void deleteRecordStore (final String recordStoreName) throws RecordStoreException {
        if (OpenedRecordStores.containsKey (recordStoreName)) {
            Log.i (TAG, "deleteRecordStore: OPENED ! ");
            throw new RecordStoreException (recordStoreName+" still opened !");
        }
        CachedRecordStores.remove(recordStoreName);
        File directory = getRecordStoreDirectory (recordStoreName);
        if (!directory.exists () || !directory.isDirectory ()) {
            Log.i (TAG, "deleteRecordStore: NOT EXISTS OR NOT DIRECTORY ! ");
            throw new RecordStoreNotFoundException (recordStoreName);
        } else {
            File[] files = directory.listFiles ();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    files[i].delete ();
                }
            }
            directory.delete ();
        }
    }

    public static final String[] listRecordStores () {
        try {
            return getRootDir ().list ();
        } catch (Exception ex) {
            Log.i (TAG, "listRecordStores: EXECPTION: "+ex);
        }
        return null;
    }

    private File directory;
    private String name;
    private int count;
    private int nextRecordId;

    private RecordStore (File dir, String n) throws RecordStoreException  {
        directory = dir;
        name = n;
        nextRecordId = findNextRecordID ();
        //Log.i (TAG, "New RecordStore '"+n+"': nextRecordID: "+nextRecordId);
    }

    public int addRecord (byte[] data, int offset, int numBytes) throws RecordStoreException {
        int id = nextRecordId++;
        File file = getRecordFile (id);
        try {
            if (!file.createNewFile ()) {
                Log.i (TAG, "addRecord: duplicate record"+file.getPath () + " / "+file.getName ());
                throw new RecordStoreException ("duplicate record " + file.getName ());
            }
            FileOutputStream fos = new FileOutputStream (file, false);
            fos.write (data, offset, numBytes);
            fos.close ();
        } catch (IOException ex) {
            Log.i (TAG, "addRecord: EXCEPTION: "+ex);
            throw new RecordStoreException ("error writing " + file.getAbsolutePath (), ex);
        }
        Log.i (TAG, "addRecord: "+id);
        return id;
    }

    private File getRecordFile (int id) throws RecordStoreNotOpenException {
        if (directory == null) throw new RecordStoreNotOpenException();
        return new File (directory, Integer.toString (id));
    }

    public int getSize () throws RecordStoreNotOpenException {
        if (directory == null) throw new RecordStoreNotOpenException();
        String[] files = directory.list ();
        int size = 0;
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                size += new File (directory, files[i]).length ();
            }
        }
        return size;
    }

    public int getSizeAvailable () throws RecordStoreNotOpenException {
        // guess what, this isn't available!!
        return DEFAULT_MAX_RECORDSTORE_SIZE - this.getSize ();
    }

    public void closeRecordStore () throws RecordStoreException {
        if (count-- > 0) {
            Log.i (TAG, "closeRecordStore("+name+"): not closing, still other using it ! "+count);
            return;
        }
        OpenedRecordStores.remove (name);
        //directory = null;
        //Log.i (TAG, "closeRecordStore("+name+"): closed! ");
        //name = null;
    }

    public void deleteRecord (int recordId) throws RecordStoreException {
        File file = getRecordFile (recordId);
        if (file.exists ()) {
            file.delete ();
        } else {
            throw new RecordStoreException();
        }
    }

    /*public RecordEnumeration enumerateRecords (RecordFilter filter, RecordComparator comparator, boolean keepUpdated) throws RecordStoreException {
        String[] filenames = filter (filter);
        String[] orderedFilenames = order (comparator, filenames);
        return new RecordEnumerationImpl (orderedFilenames);
    }

    private class RecordStoreRecordFilter extends RecordStoreFilenameFilter {
        private RecordFilter filter;

        public RecordStoreRecordFilter(RecordFilter filter) {
            super (RecordStore.this.name);
            this.filter = filter;
        }

        @Override
        public boolean accept (File f, String name) {
            boolean result;
            if (!name.equals (RecordStore.this.name + DATA_EXT)) {
                result = super.accept (f, name);
                if (result) {
                    File file = new File (f, name);
                    try {
                        byte[] data = RecordStore.this.getRecord (file);
                        result = (filter != null) ? (filter.matches (data))
                                : (true);
                    } catch (RecordStoreException ex) {
                        throw new RuntimeException (ex);
                    }

                }
            } else {
                result = false;
            }
            return result;
        }

    }

    private String[] filter (final RecordFilter filter)
            throws RecordStoreException {
        try {
            return this.directory
                    .list (new RecordStoreRecordFilter (filter));
        } catch (RuntimeException ex) {
            throw new RecordStoreException ("unable to filter records", ex);
        }
    }

    private String[] order (RecordComparator comparator, String[] filenames) {
        String[] result;
        if (comparator != null) {
            throw new UnsupportedOperationException ("ordering not supported");
        } else {
            result = filenames;
        }
        return result;
    }*/

    public int findNextRecordID () throws RecordStoreException {
        String[] files = directory.list ();
        if (files != null) {
            int max = 0; int id = 0;
            for (int i = 0; i < files.length; i++) {
                try { id = Integer.parseInt (files[i]); } 
                catch (Exception e) {
                    throw new RecordStoreException("Error: file name is not a record id !"); 
                }
                if (id > max) max = id;
            }
            return max + 1;
        }
        throw new RecordStoreException("Could find next record !");
    }

    /*private Integer parseFilenameToRecordId (String file) {
        String fileNumberString = file.substring (0, file.length () - DATA_EXT.length ());
        fileNumberString = fileNumberString.substring (this.name.length ());
        Integer result;
        if (fileNumberString.length () > 0) {
            result = Integer.parseInt (fileNumberString);
        } else {
            result = null;
        }
        return result;
    }*/

    public int getNumRecords () throws RecordStoreException {
        if (directory == null) throw new RecordStoreNotOpenException();
        File[] list = directory.listFiles ();
        return list == null ? 0 : list.length;
    }

    public byte[] getRecord (int recordId) throws RecordStoreException {
        File file = getRecordFile (recordId);
        return getRecord (file);
    }
    
    public int getRecordSize (int recordId) throws RecordStoreNotOpenException {
        return (int)getRecordFile (recordId).length ();
    }

    public byte[] getRecord (File file) throws RecordStoreException {
        if (!file.exists ()) {
            throw new InvalidRecordIDException ("" + file.getPath ());
        }
        int l = (int)file.length ();
        byte[] buffer = new byte[l];
        FileInputStream fis = null;
        try {
            fis = new FileInputStream (file);
            int r = fis.read (buffer);
            if (r < l) { // pack (should not occur)
                if (r == -1) return null;
                byte[] oldBuff = buffer;
                buffer = new byte[r];
                System.arraycopy (oldBuff, 0, buffer, 0, r);
                Log.e (TAG, "getRecord: HAD TO PACK !!!");
            }
            return buffer;
        } catch (IOException ex) {
            throw new RecordStoreException ("unable to read " + file.getPath (), ex);
        } finally {
            if (fis != null) {
                try {
                    fis.close ();
                } catch (IOException ex) {
                    throw new RecordStoreException ("unable to close " + file.getPath (), ex);
                }
            }
        }

    }

    public void setRecord (int recordId, byte[] newData, int offset,
            int numBytes) throws RecordStoreException {
        File file = getRecordFile (recordId);

        if (!file.exists ()) {
            throw new InvalidRecordIDException ("" + recordId);
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream (file);
            fos.write (newData, offset, numBytes);
        } catch (IOException ex) {
            throw new RecordStoreException ("unable to write " + recordId, ex);
        } finally {
            if (fos != null) {
                try {
                    fos.close ();
                } catch (IOException ex) {
                    throw new RecordStoreException ("unable to close " + recordId, ex);
                }
            }
        }
    }
/*
    private static class RecordStoreFilenameFilter implements FilenameFilter {
        private String name;

        public RecordStoreFilenameFilter(String name) {
            this.name = name;
        }

        // @Override
        public boolean accept (File f, String name) {
            boolean result;
            if (name.startsWith (this.name)) {
                String post = name.substring (this.name.length ());
                if (post.endsWith (DATA_EXT)) {
                    post = post.substring (0, post.length ()
                            - DATA_EXT.length ());
                    if (post.length () == 0) {
                        result = true;
                    } else {
                        try {
                            Integer.parseInt (post);
                            result = true;
                        } catch (NumberFormatException nfe) {
                            result = false;
                        }
                    }
                } else {
                    result = false;
                }
            } else {
                result = false;
            }
            return result;
        }
    }

    private class RecordEnumerationImpl implements RecordEnumeration {
        private String[] filenames;
        private int position;

        public RecordEnumerationImpl(String[] filenames) {
            this.filenames = filenames != null ? filenames : new String[0];
        }

        // @Override
        public void destroy () {
            this.filenames = null;
        }

        // @Override
        public boolean hasNextElement () {
            return this.position < this.filenames.length;
        }

        // @Override
        public boolean hasPreviousElement () {
            return this.position > 0;
        }

        // @Override
        public boolean isKeptUpdated () {
            return false;
        }

        // @Override
        public void keepUpdated (boolean keepUpdated) {
            throw new UnsupportedOperationException ("keepUpdated");
        }

        // @Override
        public byte[] nextRecord () throws RecordStoreException {
            String filename = this.filenames[this.position];
            this.position++;
            return RecordStore.this.getRecord (new File (
                    RecordStore.this.directory, filename));
        }

        // @Override
        public int nextRecordId () {
            String filenameNumberString = this.filenames[this.position];
            this.position++;
            return RecordStore.this
                    .parseFilenameToRecordId (filenameNumberString);
        }

        // @Override
        public int numRecords () {
            return this.filenames.length;
        }

        // @Override
        public byte[] previousRecord () throws RecordStoreException {
            String filename = this.filenames[this.position - 1];
            this.position--;
            return RecordStore.this.getRecord (new File (
                    RecordStore.this.directory, filename));
        }

        // @Override
        public int previousRecordId () {
            String filename = this.filenames[this.position - 1];
            this.position--;
            return RecordStore.this.parseFilenameToRecordId (filename);
        }

        // @Override
        public void rebuild () {
            throw new UnsupportedOperationException ("rebuild");
        }

        // @Override
        public void reset () {
            this.position = 0;
        }

    }*/
}
