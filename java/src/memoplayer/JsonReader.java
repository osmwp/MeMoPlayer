//#condition api.xparse
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

// a JSON value is a "native" type: string number array object or constant (true, false and null)

class JsonValue {
    int m_type;
    String m_sval; // the string value
    int m_nval; // the number value
    JsonObject m_oval; // the object value
    JsonArray m_aval; // the array value

    JsonValue (String val) {
        m_sval = val;
        m_type = JsonReader.STRING;
    }
    JsonValue (int val) {
        m_nval = val;
        m_type = JsonReader.NUMBER;
    }
    JsonValue (JsonObject val) {
        m_oval = val;
        m_type = JsonReader.OBJECT;
    }
    JsonValue (JsonArray val) {
        m_aval = val;
        m_type = JsonReader.ARRAY;
    }
    JsonValue (boolean val) {
        m_type = val ? JsonReader.TRUE : JsonReader.FALSE;
    }
    JsonValue () {
        m_type = JsonReader.NULL;
    }

    JsonValue goTo (String name) {
        return m_type == JsonReader.OBJECT ? m_oval.get (name) : null;
    }

    JsonValue goTo (int count) {
        return m_type == JsonReader.ARRAY ? m_aval.get (count) : null;
    }
    
    int getType () {
        return m_type;
    }

    int getSize () {
        return m_type == JsonReader.ARRAY ? m_aval.m_size : 1;
    }

    int getNumberValue () { return m_nval; }

    String getStringValue () { return m_sval; }

//#ifdef xml.debug
    public String toString () {
        switch (m_type) {
        case JsonReader.ARRAY: return m_aval == null ? "null" : m_aval.toString();
        case JsonReader.NULL: return "null ";
        case JsonReader.TRUE: return "true ";
        case JsonReader.FALSE: return "false ";
        case JsonReader.STRING: return m_sval+" ";
        case JsonReader.NUMBER: return ""+(m_nval/65536f)+" ";
        case JsonReader.OBJECT: return m_oval == null ? "null" : m_oval.toString();
        }
        return "??";
    }
//#endif
}

class JsonArray {
    final static int CHUNK_SIZE = 32;
    int m_size;
    int m_realSize;
    JsonValue [] m_data;

    JsonArray () {
        m_size = 0;
        m_data = new JsonValue [m_realSize = CHUNK_SIZE];
    }

    void add (JsonValue o) {
        if (m_size == m_realSize) { // array is full
            JsonValue [] tmp = new JsonValue [m_realSize += CHUNK_SIZE];
            System.arraycopy (m_data, 0, tmp, 0, m_size);
            m_data = tmp;
        }
        m_data [m_size++] = o;
    }

    void pack () {
        if (m_size < m_realSize) { // array has extra space
            JsonValue [] tmp = new JsonValue [m_size];
            System.arraycopy (m_data, 0, tmp, 0, m_size);
            m_data = tmp;
        }
    }

    JsonValue get (int count) {
        return m_size > count ?  m_data[count] : null;
    }

//#ifdef xml.debug
    public String toString () {
        String r = "[ ";
        for (int i = 0; i < m_size; i++) {
            r += m_data[i];
            if (i < (m_size -1)) {
                r += ", ";
            }
        }
        r += " ] ";
        return r;
    }
//#endif
}

class JsonPair {
    String m_name; // the name of the pair
    JsonValue m_value;
    JsonPair m_next = null;

    JsonPair (String name, JsonValue value) {
        m_name = name;
        m_value = value;
    }

    JsonValue get (String name) {
        if (name.equals (m_name)) {
            return m_value;
        }
        return m_next != null ? m_next.get (name) : null;
    }

//#ifdef xml.debug
    public String toString () {
        String r = "";
        if (m_next != null) {
            r = m_next.toString ()+ ", ";
        }
        return r + m_name +" : "+m_value;
    }
//#endif
}

// a Json object is a list of pair string / value. Value is one of the constant defined in JsonReader
class JsonObject {
    JsonPair m_root = null;

    JsonObject () { }

    void add (JsonPair p) {
        p.m_next = m_root;
        m_root = p;
    }

    JsonValue get (String name) {
        return m_root != null ? m_root.get (name) : null;
    }

    public String toString () {
        return "{ "+m_root+" } ";
    }
}

public class JsonReader extends BaseReader {
    final static int ERROR  = -1;
    final static int STRING = 1;
    final static int NUMBER = 2;
    final static int OBJECT = 3;
    final static int ARRAY  = 4;
    final static int TRUE   = 5;
    final static int FALSE  = 6;
    final static int NULL   = 7;

    JsonValue m_root;
    JsonValue m_current;

    public JsonReader (String buffer, int mode) {
        super (buffer, mode);
    }
    
    void closeSpecific () {
        m_root = null;
        m_current = null;
    }
    
    boolean parse () {
        try {
            //Logger.println ("------- New JSON DOM with "+m_buffer);
            m_root = new JsonValue (getObject ());
            //Logger.println ("------- DONE by "+m_root);
        } catch (Exception e) {
            m_root = null;
            out ("JsonReader.parse: Exception while creating DOM: "+e);
            return false;
        }
//#ifdef xml.debug
        out ("Jsonreader.parse: "+m_root); 
//#endif
        return true;
    }

    // the path string contains /nodeName[count]/...
    public boolean find (String path) {
        int idx, start = 0;
        if (path == null || path.length () == 0) { 
            return false;
        }
        if (path.charAt(0) == '/') { // from root
            m_current = m_root;
            start = 1;
        } 
        if (m_current == null) { // no root or previous find
            out ("Jsonreader.find("+path+"): no root or previous find! ");
            return false;
        }
        idx = path.indexOf ('/', start);
        if (idx == -1) { // no other '/'
            if ( (idx = path.length ()) == 0) { // just '/' in the path
                return true;
            }
        }
        try {
            while (idx > 0) {
                String nodeName = path.substring (start, idx); 
                int idx2 = nodeName.indexOf ('[');
                if (idx2 < 0) {
                    m_current = m_current.goTo (nodeName);
                } else {
                    //Logger.println ("Jsonreader.find: goto1 "+nodeName.substring (0, idx2));
                    //Logger.println ("Jsonreader.find: goto2 "+parseOccurence (nodeName));
                    m_current = m_current.goTo (nodeName.substring (0, idx2));
                    if (m_current != null) {
                        m_current = m_current.goTo (XmlDom.parseOccurence (nodeName)-1);
                    }
                }
                if (m_current == null) {
                    out ("JsonReader.find: path  ("+path+") not found");
                    return false;
                }
                start = idx+1;
                if (start >= path.length ()) {
                    idx = -1;
                } else {
                    idx = path.indexOf ('/', start);
                    if (idx == -1 && start < path.length ()) { // no trailing '/'
                        idx = path.length ();
                    }
                }
            }
            return true;
        } catch (Exception e) {
            out ("JsonReader.find: got exception "+e);
            m_current = null;
            return false;
        }
        
    }

    public int getType () {
        return m_current == null ?JsonReader.ERROR : m_current.getType ();
    }

    public int getSize () {
        return m_current == null ? 0 : m_current.getSize ();
    }

    public int getNumberValue () {
        return m_current == null ? 0 : m_current.getNumberValue ();
    }

    public String getStringValue () { 
        return m_current == null ? "" : m_current.getStringValue ();
    }

    private JsonObject getObject () {
        skipSpaces ();
        if (eatChar ('{') == false) {
            return null;
        }
        JsonObject root = new JsonObject ();
        if (getChar() != '}') {
            root.add (getPair ());
            char c = skipSpaces ();
            // should be a ',' (comma)
            while ( c != '\0' && c != '}' ) {
                if (eatChar (',') == false) {
                    out ("Json.getObject: ',' expected instead of "+c+" while parsing oject at "+m_pos);
                    break;
                }
                root.add (getPair ());
                c = skipSpaces ();
            }
        }
        if (eatChar ('}') == false) {
            out ("Json.getObject: '}' expected instead of "+getChar()+" while parsing oject at "+m_pos);
        }
        return root;
    }

    private JsonPair getPair () {
        String name = getString ();
        if (name == null) {
            name = "";
        }
        if (skipSpaces() != ':') { // kind of empty object
            return new JsonPair (name, new JsonValue ());
        }
        getNextChar (); // eat ':'
        return new JsonPair (name, getValue ());
    }

//     protected String getString () {
//         return super.getString (false);
//     } 

    private JsonValue getValue () {
        char c = skipSpaces ();
        switch (c) {
        case '"' :
            return new JsonValue (getString ());
        case '0' :
        case '1' :
        case '2' :
        case '3' :
        case '4' :
        case '5' :
        case '6' :
        case '7' :
        case '8' :
        case '9' :
        case '-' :
            return new JsonValue (getNumber ());
        case '[' :
            return new JsonValue (getArray ());
        case '{' :
            return new JsonValue (getObject ());
        case 'T' :
        case 't' :
        case 'f' :
        case 'F' :
            return new JsonValue (getBoolean ());
        case 'N' :
        case 'n' :
            if (getNull ()) {
                return new JsonValue ();
            }
        default:
            out ("JsonReader.getValue: syntax error while parsing value at "+m_pos);
            getNextChar ();
        }
        return null;
    }


    private JsonArray getArray () {
        skipSpaces ();
        if (eatChar ('[') == false) { return null; }
        JsonArray root = new JsonArray ();
        char c = skipSpaces ();
        if (c != ']') {
            root.add (getValue());
            c = skipSpaces ();
            while ( c != '\0' && c != ']' ) {
                if (eatChar (',') == false) {
                    out ("JsonReader.getArray: ',' expected instead of '"+c+"'("+((int)c)+")while parsing array at "+m_pos);
                    break;
                }
                root.add (getValue());
                c = skipSpaces ();
            }
            root.pack ();
        }
        getNextChar (); // skip ']'
        return root;
    }

    int getNumber () {
        String s = getNextNumber ();
        if (s != null) {
            return FixFloat.float2fix ( Double.valueOf(s).floatValue() );
        } 
        return 0;
    }

    private boolean getBoolean () {
        String s = getNextToken ();
        return s != null && (s.equals ("true") || s.equals ("TRUE"));
    }
    
    private boolean getNull () {
        String s = getNextToken ();
        return s != null && (s.equals ("null") || s.equals ("NULL"));
    }

}
