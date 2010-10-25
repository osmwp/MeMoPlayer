//#condition api.array
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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;


public class JSArray {
    final static int MAX_ARRAYS = 32;
    final static int MAX_ENUMS = 16;
    final static int ARRAYS_KEY = 12345678;
    static Object s_arrays[];
    static Enumeration s_enumerations[];
    //private static int nb_array = 0;
    
    static void doArray (Context c, int m, Register [] registers, int r, int nbParams) {
        Vector vector = null;
        Hashtable htable = null;
        Object value = null;
        int id = registers[r].getInt() - ARRAYS_KEY;
        if(s_arrays == null) {
            s_arrays = new Object[MAX_ARRAYS];
        }
        if (m > 3) {
            // Make sure array is present
            if (id<0 || id >= MAX_ARRAYS || s_arrays[id] == null) {
                registers[r].setInt(-1);
                return;
            }
            if(s_arrays[id] instanceof Vector) {
                vector = (Vector)s_arrays[id];
            } else {
                htable = (Hashtable)s_arrays[id];
            }
        }
        try {
            switch (m) {
            case 0: // newArray()
                registers[r].setInt(addArray(new Vector()));
                return;
            case 1: // newCollection()
                registers[r].setInt(addArray(new Hashtable()));
                return;
            case 2: // free(id)
                //if (s_arrays[id] != null) {
                  //nb_array--;
                  //Logger.println("("+nb_array+") Freeing array at slot:"+id);
                //}
                s_arrays[id] = null;
                //Logger.println("Freeing array at slot:"+id);
                return;
            case 3: // clean()
                clean();
                return;
            case 4: // addElement(id, value[, at])
                value = getParam(registers[r+1]);
                if(vector != null) {
                    if (nbParams == 3) {
                        vector.insertElementAt(value, registers[r+2].getInt());   
                    } else {
                        vector.addElement(value);
                    }
                    id = vector.indexOf(value);
                } else {
                    id = value.hashCode();
                    htable.put(new Integer(id), value);
                }
                registers[r].setInt(id);
                return;
            case 5: // setElement(id, key, value)
                value = getParam(registers[r+2]);
                if(vector != null) {
                    int key = registers[r+1].getInt();
                    try {
                        vector.setElementAt(value, key);
                        registers[r].setInt(key);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        registers[r].setInt(-1);    
                    }
                } else {
                    Object key = registers[r+1].get();
                    htable.put(key, value);
                    setParam(registers[r], key);
                }
                return;
            case 6: // getElement(id, key)
          
                if (vector != null) {
                    try {
                        value = vector.elementAt(registers[r+1].getInt());
                    } catch (ArrayIndexOutOfBoundsException e) {
                        value = null;
                    }
                } else {
                    value = htable.get(registers[r+1].get());
                }
                setParam(registers[r], value);
                return;
            case 7: // removeElement(id, key)
                if (vector != null) {
                    vector.removeElementAt(registers[r+1].getInt());
                } else {
                    htable.remove(registers[r+1].get());
                }
                return;
            case 8: // isArray(id)
                registers[r].setBool(vector != null);
                return;
            case 9: // isCollection(id)
                registers[r].setBool(htable != null);
                return;
            case 10: // size(id)
                if (vector != null) {
                    registers[r].setInt(vector.size());
                } else {
                    registers[r].setInt(htable.size());
                }
                return;
            case 11: // elements(id)
                if (s_enumerations == null) s_enumerations = new Enumeration[MAX_ENUMS];
                int eid = ExternCall.getFreeSlot(s_enumerations);
                if (eid != -1) {
                    if (vector != null) {
                        s_enumerations[eid] = vector.elements();
                    } else {
                        s_enumerations[eid] = htable.elements();
                    }
                }
                registers[r].setInt(eid);
                return;
            case 12: // keys(id)
                if (s_enumerations == null) s_enumerations = new Enumeration[MAX_ENUMS];
                eid = ExternCall.getFreeSlot(s_enumerations);
                if (eid != -1) {
                    if (vector != null) {
                        eid = -1; // no keys for vectors
                    } else {
                        s_enumerations[eid] = htable.keys();
                    }
                }
                registers[r].setInt(eid);
                return;
            case 13: // dump (id)
            {
                StringBuffer sb = new StringBuffer();
                if (vector != null) {
                    dumpArray(sb, vector, "",'\n');
                } else {
                    dumpArray(sb, htable, "",'\n');
                }
                System.out.println(sb.toString());
                return;
            }
            case 14: // removeAllElements (id)
            {
                if (vector != null) {
                    vector.removeAllElements();
                } else {
                    htable.clear();
                }
                return;
            }
            default:
                System.err.println ("doArray (m:"+m+")Static call: Invalid method");
                return;
            }
        } catch (Exception e) {
            System.err.println("Exception in Array API at function "+m);
            e.printStackTrace();
            registers[r].setInt(-1);
        }
    }


    static void doEnumeration (Context c, int m, Register [] registers, int r, int nbParams) {
        int i = registers[r].getInt();
        Enumeration e = null;
        if (s_enumerations != null && i>=0 && i<MAX_ENUMS) {
            e = s_enumerations[i];
        }
        if (e != null) {
            switch (m) {
            case 0: // hasMoreElements(id)
                registers[r].setBool(e.hasMoreElements());
                return;
            case 1: // nextElement(id)
                setParam(registers[r], e.nextElement());
                return;
            case 2: // free(id)
                e = null;
                s_enumerations[i] = null;
                registers[r].setBool(true);
                return;
            default:
                System.err.println ("Static call: Invalid method");
            }
        }
        registers[r].setInt(-1);
    }
    
    static void clean() {
        if (s_arrays != null){
            for (int i=0; i<s_arrays.length; i++) {
                if (s_arrays[i] != null) {
                    s_arrays[i] = null;
                }
            }
            s_arrays = null;
        }
        if (s_enumerations != null){
            for (int i=0; i<s_enumerations.length; i++) {
                if (s_enumerations[i] != null) {
                    s_enumerations[i] = null;
                }
            }
            s_enumerations = null;
        }
    }

    // Expose an array (Vector or Hastable) to Javascript code trough the Array API
    static int addArray(Object array) {
        if(s_arrays == null) {
            s_arrays = new Object[MAX_ARRAYS];
        }
        int i = ExternCall.getFreeSlot(s_arrays);
        if (i != -1) {
            //Logger.println("Allocaton an array at slot:"+i);
            //nb_array++;
            //Logger.println("("+nb_array+")Allocaton an array at slot:"+i);
            s_arrays[i] = array;
            i += ARRAYS_KEY;
        } else {
            Logger.println("ERROR: Max arrays ("+MAX_ARRAYS+") reached !");
        }
        return i;
    }

    static Object[] getParams(Register[] registers, int r, int nbParams) {
        if (registers[r+nbParams-1].getString().equals("PARAMS_END")) nbParams--;
        Object[] params = new Object[nbParams];
        int i = 0;
        while (i < nbParams) {
            params[i] = getParam(registers[r+i]);
            i++;
        }
        return params;
    }

    static Object getParam(Register param) {
        int val = param.getInt() - ARRAYS_KEY;
        if(s_arrays != null && val >=0 && val < s_arrays.length && s_arrays[val] != null) {
            return s_arrays[val];
        } else {
            return param.get();
        }
    }

    static void setParam(Register param, Object value) {
        if (value == null) {
            param.setInt(-1);
        } else if (value instanceof String) {
            param.setString((String)value);
        } else if (value instanceof Integer) {
            param.setInt(((Integer)value).intValue());
        } else if (value instanceof FixFloat) {
            param.setFloat( ((FixFloat)value).get());
        } else if (value instanceof Vector || value instanceof Hashtable) {
            param.setInt(addArray(value));
        } else {
            param.setInt(-1);
        }
    }

    static void dumpArray(StringBuffer sb, Object value, String level, char sep) {
        if (value == null) {
            sb.append(level+"null");
        } else if (value instanceof String) {
            sb.append(level+'"'+value.toString()+'"');
        } else if (value instanceof Integer) {
            sb.append(level+((Integer)value).toString());                
        } else if (value instanceof FixFloat) {
            sb.append(level+FixFloat.toString(((FixFloat)value).get()));
        } else if (value instanceof Vector) {
            Vector v = (Vector)value;
            sb.append(level+"["+sep);
            for (int i=0; i<v.size(); i++) {
                dumpArray(sb, v.elementAt(i),level+' ',sep);
                if (i!=v.size()-1) sb.append(","+sep);
            }
            sb.append(sep+level+']');
        } else if (value instanceof Hashtable) {
            Hashtable h = (Hashtable)value;
            sb.append(level+'{'+sep);
            Enumeration e = h.keys();
            while(e.hasMoreElements()) {
                Object k = e.nextElement();
                dumpArray(sb, k, level+' ',sep);
                sb.append(" : ");
                dumpArray(sb, h.get(k), level+' ',sep);
                if (e.hasMoreElements()) {
                    sb.append(level+','+sep);
                }
            }
            sb.append(sep+level+'}');
        } else {
            sb.append(level+"Unknown element : "+value+sep);
        }
    }
}
