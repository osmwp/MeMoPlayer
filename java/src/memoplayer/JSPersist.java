//#condition api.persist 
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

import java.io.*;
import java.util.*;

class JSPersist {
    private final static int TYPE_STRING = 0;
    private final static int TYPE_INTEGER = 1;
    private final static int TYPE_FLOAT = 2;
    private final static int TYPE_VECTOR = 3;
    private final static int TYPE_HASHTABLE = 4;
    private final static int TYPE_BYTES = 5;
    
    static void doPersist (Context c, int m, Register [] registers, int r, int nbParams) {
        CacheManager mgr = CacheManager.getManager();
        switch(m) {
        case 0: // exists (recordName)
            registers[r].setBool(mgr.getByteRecord(registers[r].getString()) != null);
            break;
        case 1: // setRecord (recordName)
            registers[r].setBool(setValue(mgr, registers[r].getString(), JSArray.getParam(registers[r+1])));
            break;
        case 2: // getRecord (recordName)
            JSArray.setParam(registers[r], getValue(mgr, registers[r].getString()));
            break;
        case 3: // deleteRecord (recordName)
            mgr.deleteRecord(registers[r].getString());
            break;
        case 4: // deleteStore (storeName)
//#ifdef MM.namespace
            // Deletion of record stores are not allowed for unprivileged namespaces
            if (Namespace.getName() != "") return;
//#endif
            CacheManager.delete(registers[r].getString());
            return;
        case 5: // setStore (storeName)
//#ifdef MM.namespace
            // Changing store is not allowed for unprivileged namespaces
            if (Namespace.getName() != "") return;
//#endif
            CacheManager.setStore (registers[r].getString());
            return;
        case 6: // getSizeAvailable ()
            registers[r].setInt (mgr.getSizeAvailable());
            return;
        default:
            Logger.println ("Persist API: Invalid method");
            break;
        }
    }

    static public Object getValue(CacheManager mgr, String key) {
        byte[] buff = mgr.getByteRecord (key); // mgr.get(mgr.find(key));
        Object value = null;
        if (buff != null && buff.length > 0) {
            ByteArrayInputStream bais = new ByteArrayInputStream(buff);
            try { value = deserialize(new DataInputStream(bais)); } catch (Exception e) {}
            //System.err.println("Persist.getValue: "+key+": "+value);
        } else {
            //System.err.println("Persist.getValue: Value not found: "+key);
        }
        return value;
    }

    static public boolean setValue(CacheManager mgr, String key, Object value) {
        byte [] buff = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            serialize(value, new DataOutputStream(baos));
            buff = baos.toByteArray();
        } catch (Exception e) {
            //System.err.println("Persist.setValue: error during serialization of "+key+": "+e.getMessage());
            return false;
        }

        return mgr.setRecord (key, buff);
//         int id = mgr.find(key);
//         if (id == -1) {
//            //System.err.println("Persist.setValue: adding "+key+":"+buff.length);
//            return mgr.add(key, buff);
//         } else {
//            //System.err.println("Persist.setValue: updating "+key+":"+buff.length);
//            return mgr.set (id, buff);
//         }
    }

    private static Object deserialize(DataInputStream dis) throws Exception {
        int size, i;
        switch(dis.readInt()) {
            case TYPE_STRING:
                return dis.readUTF();
            case TYPE_INTEGER:
                return new Integer(dis.readInt());
            case TYPE_FLOAT:
                return new FixFloat(dis.readInt());
            case TYPE_VECTOR:
                size = dis.readInt();
                Vector array = new Vector(size);
                for(i=0; i<size; i++) {
                    array.addElement(deserialize(dis));
                }
                return array;
            case TYPE_HASHTABLE:
                size = dis.readInt();
                Hashtable hashTable = new Hashtable(size);
                for(i=0; i<size; i++) {
                    hashTable.put(deserialize(dis),deserialize(dis));
                }
                return hashTable;
            case TYPE_BYTES:
                size = dis.readInt();
                byte[] buff = new byte[size];
                dis.read(buff, 0, size);
            default: // error
                throw new Exception("Persist.deserialize: Unsupported serialized data");
        }
    }

    private static void serialize(Object data, DataOutputStream dos) throws Exception {
        if(data == null) {
            throw new Exception("Could not serialize null data");
        } else if (data instanceof String) {
            dos.writeInt(TYPE_STRING);
            dos.writeUTF((String)data);
        } else if (data instanceof Integer) {
            dos.writeInt(TYPE_INTEGER);
            dos.writeInt(((Integer)data).intValue());
        } else if (data instanceof FixFloat) {
            dos.writeInt(TYPE_FLOAT);
            dos.writeInt(((FixFloat)data).get());
        } else if (data instanceof Vector) {
            Vector vector = (Vector) data;
            dos.writeInt(TYPE_VECTOR);
            int size = vector.size();
            dos.writeInt(size);
            for(int i=0; i<size; i++) {
                serialize(vector.elementAt(i), dos);
            }
        } else if (data instanceof Hashtable) {
            Hashtable hashTable = (Hashtable) data;
            dos.writeInt(TYPE_HASHTABLE);
            dos.writeInt(hashTable.size());
            Enumeration keys = hashTable.keys();
            while(keys.hasMoreElements()) {
                Object key = keys.nextElement();
                serialize(key, dos);
                serialize(hashTable.get(key), dos);
            }
        } else if (data instanceof byte[]) {
            dos.write(TYPE_BYTES);
            byte[] buff = (byte[]) data;
            dos.writeInt(buff.length);
            dos.write(buff, 0, buff.length);
        } else {
            throw new Exception("Could not serialize data of type "+data.getClass().getName());
        }
    }
}
