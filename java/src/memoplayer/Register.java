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

class Register {
    static final int TYPE_INT = 1;
    static final int TYPE_FLOAT = 2;
    static final int TYPE_STRING = 4;
    static final int TYPE_NODE = 8;
    static final int TYPE_FIELD = 16;

    int m_type;
    int m_ival;
    String m_sval;
    Object m_oval;

    Register () {
    }

    public String toString () {
        return getString ();
    }

    void setBool (boolean b) { 
        m_type = TYPE_INT;
        m_ival =  b ? 1 : 0;
        m_sval = null;
    }        

    boolean getBool () { 
        if (m_type == TYPE_INT || m_type == TYPE_FLOAT) {
            return (m_ival != 0);
        } else {
            return (false);
        }
    }        

    void setInt (int i) { 
        m_type = TYPE_INT;
        m_ival =  i;
        m_sval = null;
    }

    int getInt () {
        if (m_type == TYPE_INT) {
            return (m_ival);
        } else if (m_type == TYPE_FLOAT) {
            return (m_ival >> 16);
        } else if (m_type == TYPE_NODE) {
            return (m_oval == null ? 0 : 1);
        } else {
            return (0);
        }
    }

    void setFloat (int i) { 
        m_type = TYPE_FLOAT;
        m_ival =  i;
        m_sval = null;
    }        

    int getFloat () {
        if (m_type == TYPE_INT) {
            return (m_ival << 16);
        } else if (m_type == TYPE_FLOAT) {
            return (m_ival);
        } else {
            return (0);
        }
    }

    int getColorComponent() {
        return (getFloat ()*255) >> 16;
    }

    void setColorComponent(int c) {
        m_type = TYPE_FLOAT;
        m_ival = FixFloat.fixDiv (c<<16, 255<<16);
        m_sval = null;
    }

    void setString (String s) { 
        m_type = TYPE_STRING;
        m_sval =  s == null ? "" : s;
    }

    String getString () {
        if (m_type == TYPE_INT) {
            return Integer.toString (m_ival);
        } else if (m_type == TYPE_FLOAT) {
            return FixFloat.toString (m_ival);
        } else if (m_type == TYPE_STRING) {
            return m_sval;
        } else if (m_type == TYPE_FIELD) {
            return m_oval != null ? m_oval.toString () : "NULL";
        } else if (m_type == TYPE_NODE) {
            return m_oval != null ? m_oval.toString () : "NULL";
        } else {
            return "undefined";
        }
    }

    void setNode (Node n) { 
        m_type = TYPE_NODE;
        m_oval =  n;
    }

    Node getNode () {
        if (m_type == TYPE_NODE) {
            return (Node)m_oval;
        } else {
            return null;
        }
    }

    void setField (Field f) { 
        m_type = TYPE_FIELD;
        m_oval =  f;
    }

    Field getField () {
        if (m_type == TYPE_FIELD) {
            return (Field)m_oval;
        } else {
            return null;
        }
    }

    void set (Register r) {
        m_type = r.m_type;
        m_ival = r.m_ival;
        m_sval = r.m_sval;
        m_oval = r.m_oval;
    }
    
//#ifdef api.array
    //MCP: Get value of the register as an object
    //     used for Arrays API and serialization for persistence
    Object get() {
        switch(m_type) {
        case TYPE_INT: return new Integer(m_ival);
        case TYPE_FLOAT: return new FixFloat(m_ival);
        case TYPE_STRING: return m_sval;
        case TYPE_NODE: return m_oval;
        //case TYPE_FIELD: return m_oval;
        default: return new Object(); // Or null ?
        }
    }
//#endif

    void add (Register r) {
        if (m_type == TYPE_INT) {
            if (r.m_type == TYPE_INT) {
                setInt (m_ival+r.m_ival);
            } else if (r.m_type == TYPE_FLOAT) {
                setFloat ((m_ival << 16)+r.m_ival);
            } else if (r.m_type == TYPE_STRING) {
                setString (getString() + r.m_sval);
            } else {
                setString (getString() + r.m_oval);
            }
        } else if (m_type == TYPE_FLOAT) {
            if (r.m_type == TYPE_INT) {
                setFloat (m_ival+(r.m_ival << 16));
            } else if (r.m_type == TYPE_FLOAT) {
                setFloat (m_ival+r.m_ival);
            } else if (r.m_type == TYPE_STRING) {
                setString (FixFloat.toString(m_ival) + r.m_sval);
            } else {
                setString (FixFloat.toString(m_ival) + r.m_oval);
            }
        } else if (m_type == TYPE_STRING) {
            if (r.m_type == TYPE_INT) {
                setString (m_sval+Integer.toString (r.m_ival));
            } else if (r.m_type == TYPE_FLOAT) {
                setString (m_sval+FixFloat.toString (r.m_ival));
            } else if (r.m_type == TYPE_STRING) {
                setString (m_sval + r.m_sval);
            } else {
                setString (m_sval + r.m_oval);
            }
        } else { // node
            setInt (0);
        }
    }

    void sub (Register r) {
        if (m_type == TYPE_INT) {
            if (r.m_type == TYPE_INT) {
                setInt (m_ival-r.m_ival);
            } else if (r.m_type == TYPE_FLOAT) {
                setFloat ((m_ival << 16)-r.m_ival);
            }
        } else if (m_type == TYPE_FLOAT) {
            if (r.m_type == TYPE_INT) {
                setFloat (m_ival-(r.m_ival << 16));
            } else if (r.m_type == TYPE_FLOAT) {
                setFloat (m_ival-r.m_ival);
            }
        }
    }

    void mul (Register r) {
        if (m_type == TYPE_INT) {
            if (r.m_type == TYPE_INT) {
                setInt (m_ival*r.m_ival);
            } else if (r.m_type == TYPE_FLOAT) {
                setFloat (FixFloat.fixMul( (m_ival << 16), r.m_ival));
            }
        } else if (m_type == TYPE_FLOAT) {
            if (r.m_type == TYPE_INT) {
                setFloat (FixFloat.fixMul( m_ival, (r.m_ival << 16)));
            } else if (r.m_type == TYPE_FLOAT) {
                setFloat (FixFloat.fixMul (m_ival, r.m_ival));
            }
        }
    }

    void div (Register r) {
        int d = r.m_ival;
        if (d == 0) {
            setInt (0);
        } else {
            if (m_type == TYPE_INT) {
                if (r.m_type == TYPE_INT) {
                    setInt (m_ival/d);
                } else if (r.m_type == TYPE_FLOAT) {
                    setFloat (FixFloat.fixDiv( (m_ival << 16), d));
                }
            } else if (m_type == TYPE_FLOAT) {
                if (r.m_type == TYPE_INT) {
                    setFloat (FixFloat.fixDiv( m_ival, (d << 16)));
                } else if (r.m_type == TYPE_FLOAT) {
                    setFloat (FixFloat.fixDiv (m_ival, d));
                }
            }
        }
    }

    void mod (Register r) {
        int m = r.getInt ();
        setInt (m > 0 ? getInt() % m : getInt ());
    }

    int testValue (Register r) {
        if (m_type == TYPE_INT) {
            if (r.m_type == TYPE_INT) {
                return m_ival - r.m_ival;
            } else if (r.m_type == TYPE_FLOAT) {
                return (m_ival << 16) - r.m_ival;
            } else if (r.m_type == TYPE_NODE) {
                return (m_ival == 0 && r.m_oval == null) ? 0 : 1;
            } else {
                return 1;
            }
        } else if (m_type == TYPE_FLOAT) { 
            if (r.m_type == TYPE_INT) { 
                return m_ival - (r.m_ival << 16);
            } else if (r.m_type == TYPE_FLOAT) { 
                return m_ival - r.m_ival;
            } else {
                return 1;
            }
        } else if (m_type == TYPE_STRING){
            if (r.m_type == TYPE_INT) { 
                return 1;
            } else if (r.m_type == TYPE_FLOAT) { 
                return 1;
            } else {
                return m_sval.compareTo (r.m_sval);
            }
        } else { // TYPE_NODE
            if (r.m_type == TYPE_NODE) { 
                return m_oval == r.m_oval ? 0 : 1;
            } else if (r.m_type == TYPE_INT) {
                return (r.m_ival == 0 && m_oval == null) ? 0 : 1;
            }
            return 1;
        }
    }
}
