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
import javax.microedition.rms.*;

abstract class CacheManager {
    final static String DELETED = "__DELETED";
    final static String EMPTY   = "__EMPTY";
    private final static int BACKEND_RMS = 0;
    private final static int BACKEND_FILE = 1;
    private final static int BACKEND_RMS2 = 2;
    private final static int BACKEND_RMS3 = 3;

    static String s_basePath = null;
    static int s_backend = selectBackend ();

    static CacheManager s_mgr; // Ref to the current manager (changed by setStore)
    static CacheManager s_masterMgr; // Ref to the privileged manager

    private static int selectBackend() {
        s_basePath = MiniPlayer.getJadProperty ("MeMo-CachePath");
        if (s_basePath.length () > 0) {
//#ifdef MM.CacheUseRms2
            if (s_basePath.equals("RMS2")) {
                Logger.println ("Using CacheManager of type RMS2");
                return BACKEND_RMS2;
            }
//#endif
//#ifdef MM.CacheUseRms3
            if (s_basePath.equals("RMS3")) {
                Logger.println ("Using CacheManager of type RMS3");
                return BACKEND_RMS3;
            }
//#endif
//#ifdef MM.CacheUseFile
            if (!s_basePath.startsWith ("file://")) { // a system path ?
                String systemPath = System.getProperty (s_basePath);
                if (systemPath != null && systemPath.startsWith("file://")) {
                    s_basePath = systemPath;
                    Logger.println ("Using CacheManager of type FILE using "+systemPath);
                    return BACKEND_FILE;
                }
            } else if (!s_basePath.endsWith ("/")) {
                s_basePath += '/';
            }
            Logger.println ("Using CacheManager of type FILE");
            return BACKEND_FILE;
//#endif
        }
        Logger.println ("Using CacheManager of type RMS");
        return BACKEND_RMS;
    }

    public static void deleteAll () {
        switch (s_backend) {
//#ifdef MM.CacheUseFile
        case BACKEND_FILE:
            FileCacheManager.deleteAllFiles ();
            break;
//#endif
//#ifdef MM.CacheUseRms2
        case BACKEND_RMS2:
            RMSCacheManager2.deleteAllRMS ();
            break;
//#endif
//#ifdef MM.CacheUseRms3
        case BACKEND_RMS3:
            RMSCacheManager3.deleteAllRMS ();
            break;
//#endif
        default:
            RMSCacheManager.deleteAllRMS();
        }
    }

    // completely delete a record store, called by JSPersist
    static void delete (String baseName) {
        switch (s_backend) {
//#ifdef MM.CacheUseFile
        case BACKEND_FILE:
            FileCacheManager.deleteFiles (baseName);
            break;
//#endif
//#ifdef MM.CacheUseRms2
        case BACKEND_RMS2:
            RMSCacheManager2.deleteRMS (baseName);
            break;
//#endif
//#ifdef MM.CacheUseRms3
        case BACKEND_RMS3:
            RMSCacheManager3.deleteRMS (baseName);
            break;
//#endif
        default:
            RMSCacheManager.deleteRMS (baseName);
        }
    }

    // Return the current manager, by default its the master manager
    // but CacheManager.setStore() can override it with another one temporally.
    public static CacheManager getManager () {
//#ifdef MM.namespace
        // When using Namespace, the current manager changes dynamically
        // based on the Namespace node or the current Thread.
        return Namespace.getCacheManager();
    }
    
    public static CacheManager getCurrentManager () {
//#endif
        if (s_mgr == null) {
            s_mgr = getMasterManager ();
        }
        return s_mgr;
    }
    
    // Singleton to return the privileged "master" manager
    public static CacheManager getMasterManager () {
        if (s_masterMgr == null) {
            s_masterMgr = createManager ("");
        }
        return s_masterMgr;
    }

    public static void setStore (String s) {
        // Only close previous store if it's not the master manager
        if (s_mgr != null && s_mgr != s_masterMgr) {
            s_mgr.close();
        }
        // When jumping back to the master manager, reuse it
        if (s.equals ("")) {
            s_mgr = getMasterManager();
        } else {
//#ifdef MM.namespace
            s_mgr = Namespace.getCacheManager(s);
//#else
            s_mgr = createManager (s);
//#endif
        }
    }

    // Clean up on application exit
    public static void clean () {
        if (s_masterMgr != null) {
            s_masterMgr.close ();
            s_masterMgr = null;
        }
        // If closing midlet while still on another Manager
        if (s_mgr != s_masterMgr) {
            s_mgr.close ();
            s_mgr = null;
        }
//#ifdef MM.namespace
        // When using namespace also clean other Managers
        Namespace.clean ();
//#endif
        
//#ifdef MM.CacheUseRms2
        // RMSCacheManager2 ignores close() calls, only the closeAll() call
        // will close all RecordStores on application exit !
        if (s_backend == BACKEND_RMS2) {
            RMSCacheManager2.closeAll ();
        }
//#endif
//#ifdef MM.CacheUseRms3
        // RMSCacheManager3 ignores close() calls, only the closeAll() call
        // will close all RecordStores on application exit !
        if (s_backend == BACKEND_RMS3) {
            RMSCacheManager3.closeAll ();
        }
//#endif
    }

    // instantiate a manager, according to the jad preperty
    public static CacheManager createManager (String basename) {
        switch (s_backend) {
//#ifdef MM.CacheUseFile
      case BACKEND_FILE:
          return new FileCacheManager (basename);
//#endif
//#ifdef MM.CacheUseRms2
      case BACKEND_RMS2:
          return RMSCacheManager2.getInstance(basename);
//#endif
//#ifdef MM.CacheUseRms3
      case BACKEND_RMS3:
          return RMSCacheManager3.getInstance(basename);
//#endif
      default:
          return new RMSCacheManager (basename);
      }        
    }

    protected static int computeHashNumber (byte [] data) {
        if (data == null || data.length == 0)  {
            return 0;
        } 
        int hash = 5381;
        // nbPoints max (1%, min (10, data.length)) 
        int nbPoints = (data.length >= 1000) ? (data.length / 100) : (data.length >= 10 ? 10 : data.length); 
        int incr = data.length / nbPoints;
        int index = 0;
        for (int i = 0; i < nbPoints; i++) {
            hash = ((hash << 5) + hash) + data [index]; /* hash * 33 + c */
            index += incr;
        }
        return hash;
    }

    protected static byte [] computeHashcode (byte [] data, byte [] code) {
        int hash = computeHashNumber (data);
        code [0] = (byte) (hash & 0xFF);
        code [1] = (byte) ((hash >> 8) & 0xFF);
        code [2] = (byte) ((hash >> 16) & 0xFF);
        code [3] = (byte) ((hash >>> 24) & 0xFF);
        return code;
    }

    protected static int intSum (byte [] b) {
        return (b[0] + (b[1]<<8) + (b[2]<<16) + (b[3]<<24));
    }

    protected static boolean compareChecksums (byte [] s1, byte [] s2)  {
        //Logger.println ("verifyChecksum for "+intSum(s1)+" & "+intSum(s2));
        return s1.length == 4 && s2.length == 4 && intSum(s1) == intSum(s2);
    }

    protected static boolean isEqual (byte [] s1, byte [] s2)  {
        int l1 = s1.length;
        int l2 = s2.length;
        if (l1 != l2) {
            return false;
        }
        for (int i = 0; i < l1; i++) {
            if (s1[i] != s2[i]) {
                return false;
            }
        }
        return true;
    }


    protected CacheManager (String name) {
    }

    //private boolean open () { }

    // open the three stores and clean up the stores if an exception is raised
    //private boolean safeOpen () { return false; }
    
    // silently force closing of all stores, called by traffic
    abstract void close ();

    // called by Browser.deleteAllRecords
    abstract void erase ();

    abstract int getSizeAvailable ();

    abstract byte[] getByteRecord (String s);

    abstract boolean setRecord (String s, byte[] data);
    
    abstract boolean deleteRecord (String s);
    
    abstract boolean hasRecord (String s);

    static byte [] getStringData (String str) { 
        if ( (str!=null) && (str.length()!=0) ) {
            try { return str.getBytes("UTF-8"); } catch (Exception e) { }
            try { return str.getBytes("utf-8"); } catch (Exception e) { }
            try { return str.getBytes("utf8"); } catch (Exception e) { }
            try { return str.getBytes(); } catch (Exception e) { }
        }
        // getBytes() on empty string throws ArrayOutOfBoundsException on Samsung F480
        return new byte[0];
    }

    static String getDataString (byte [] data) { 
        if ( (data!=null) && (data.length!=0) ) {
            try { return new String (data, "UTF-8"); } catch (Exception e) { }
            try { return new String (data, "utf-8"); } catch (Exception e) { }
            try { return new String (data, "utf8"); } catch (Exception e) { }
            try { return new String (data); } catch (Exception e) { }
        }
        return "";
    }

    boolean setRecord (String s, String data) {
        return setRecord (s, getStringData(data));
    }

    String getRecord (String s) {
        return getDataString(getByteRecord (s));
    }

}
