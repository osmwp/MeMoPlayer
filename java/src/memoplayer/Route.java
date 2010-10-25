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

public class Route implements Observer {
    Field m_out;

    Route (int inNode, int inField, int outNode, int outField, Node [] table) {
        //System.out.println ("Route: "+inNode+"."+inField+" TO "+outNode+"."+outField);
        Node n = Node.getNodeByID (inNode, table);
        if (n != null) {
            Field m_in = n.getFieldByID (inField);
            if (m_in != null) { 
                m_in.addObserver (this);
            } else {
                System.err.println ("Route: cannot find 'in' field: "+inField);
            }
        } else {
            System.err.println ("Route: cannot find 'in' node: "+inNode);
        }
        n = Node.getNodeByID (outNode, table);
        if (n != null) {
            m_out = n.getFieldByID (outField);
            if (m_out == null) {
                System.err.println ("Route: cannot find 'out' field: "+outField);
            }
        } else {
            System.err.println ("Route: cannot find 'out' node: "+outNode+" ("+table[outNode]+")");
        }
    }

    static void decode (DataInputStream is, Node [] table) {
        try {
            int inID = is.readByte ();
            while (inID != 0) {
                int inIdx = is.readByte ()-1;
                int outID = is.readByte ();
                int outIdx = is.readByte ()-1;
                new Route (inID, inIdx, outID, outIdx, table);
                inID = is.readByte ();
            }
        } catch (Exception e) {
            System.err.println ("Route: error during decoding: "+e);
        }
    }
    
    
    final public void fieldChanged (Field field) {
        //System.out.println ("call to Route.fieldChanged"+field);
        m_out.copyValue (field);
    }
}
