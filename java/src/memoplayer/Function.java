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

class Function {
    final static int END_OF_CODE = 255;

    byte [] m_codeTable;
    int m_codeOffset; // Offset to the function code
    String [] m_strTable;
    int [] m_intTable;

    Function (byte[] data, int codeOffset, String[] strTable, int[] intTable) {
        m_codeTable = data;
        m_codeOffset = codeOffset;
        m_strTable = strTable;
        m_intTable = intTable;
    }
    
    boolean run (Machine m, Context c, int regBase) {
        final Register [] register = m.m_register;
        ScriptAccess currentField = c.script, pushedField = null;
        int currentIndex = -1, pushedIndex = -1;
        Register r;
        int pc = m_codeOffset;
        int a, b, d, e;
        int opcode = ((int)m_codeTable[pc++]&0xFF);

        while (opcode != END_OF_CODE) {
            switch (opcode) { // 
            case ByteCode.ASM_NOP: 
                break;
            case ByteCode.ASM_LOAD_REG_INT:
                a = ((int)m_codeTable[pc++]&0xFF);
                b = ((int)m_codeTable[pc++]&0xFF);
                register[regBase+a].setInt (m_intTable[b]);
                break;
            case ByteCode.ASM_LOAD_REG_FLT:
                a = ((int)m_codeTable[pc++]&0xFF);
                b = ((int)m_codeTable[pc++]&0xFF);
                register[regBase+a].setFloat (m_intTable[b]);
                break;
            case ByteCode.ASM_LOAD_REG_STR:
                a = ((int)m_codeTable[pc++]&0xFF);
                b = ((int)m_codeTable[pc++]&0xFF);
                register[regBase+a].setString (m_strTable[b]);
                break;
            case ByteCode.ASM_MOVE_REG_REG:
                a = ((int)m_codeTable[pc++]&0xFF);
                b = ((int)m_codeTable[pc++]&0xFF);
                register[regBase+a].set (register[regBase+b]);
                break;
            case ByteCode.ASM_ADD_REG_REG:
                a = ((int)m_codeTable[pc++]&0xFF);
                b = ((int)m_codeTable[pc++]&0xFF);
                register[regBase+a].add (register[regBase+b]);
                break;
            case ByteCode.ASM_SUB_REG_REG:
                a = ((int)m_codeTable[pc++]&0xFF);
                b = ((int)m_codeTable[pc++]&0xFF);
                register[regBase+a].sub (register[regBase+b]);
                break;
            case ByteCode.ASM_MUL_REG_REG:
                a = ((int)m_codeTable[pc++]&0xFF);
                b = ((int)m_codeTable[pc++]&0xFF);
                register[regBase+a].mul (register[regBase+b]);
                break;
            case ByteCode.ASM_DIV_REG_REG:
                a = ((int)m_codeTable[pc++]&0xFF);
                b = ((int)m_codeTable[pc++]&0xFF);
                register[regBase+a].div (register[regBase+b]);
                break;
            case ByteCode.ASM_MOD_REG_REG:
                a = ((int)m_codeTable[pc++]&0xFF);
                b = ((int)m_codeTable[pc++]&0xFF);
                register[regBase+a].mod (register[regBase+b]);
                break;
            case ByteCode.ASM_TEST_EQU:
                a = ((int)m_codeTable[pc++]&0xFF);
                b = ((int)m_codeTable[pc++]&0xFF);
                r = register[regBase+a];
                r.setBool (r.testValue (register[regBase+b]) == 0);
                break;
            case ByteCode.ASM_TEST_NEQ:
                a = ((int)m_codeTable[pc++]&0xFF);
                b = ((int)m_codeTable[pc++]&0xFF);
                r = register[regBase+a];
                r.setBool (r.testValue (register[regBase+b]) != 0);
                break;
            case ByteCode.ASM_TEST_GRT:
                a = ((int)m_codeTable[pc++]&0xFF);
                b = ((int)m_codeTable[pc++]&0xFF);
                r = register[regBase+a];
                r.setBool (r.testValue (register[regBase+b]) > 0);
                break;
            case ByteCode.ASM_TEST_GRE:
                a = ((int)m_codeTable[pc++]&0xFF);
                b = ((int)m_codeTable[pc++]&0xFF);
                r = register[regBase+a];
                r.setBool (r.testValue (register[regBase+b]) >= 0);
                break;
            case ByteCode.ASM_TEST_LES:
                a = ((int)m_codeTable[pc++]&0xFF);
                b = ((int)m_codeTable[pc++]&0xFF);
                r = register[regBase+a];
                r.setBool (r.testValue (register[regBase+b]) < 0);
                break;
            case ByteCode.ASM_TEST_LEE:
                a = ((int)m_codeTable[pc++]&0xFF);
                b = ((int)m_codeTable[pc++]&0xFF);
                r = register[regBase+a];
                r.setBool (r.testValue (register[regBase+b]) <= 0);
                break;
            case ByteCode.ASM_TEST_AND:
                a = ((int)m_codeTable[pc++]&0xFF);
                b = ((int)m_codeTable[pc++]&0xFF);
                r = register[regBase+a];
                r.setBool (r.getInt() != 0 && register[regBase+b].getInt() != 0);
                break;
            case ByteCode.ASM_TEST_OR:
                a = ((int)m_codeTable[pc++]&0xFF);
                b = ((int)m_codeTable[pc++]&0xFF);
                r = register[regBase+a];
                r.setBool ( (r.getInt() + register[regBase+b].getInt()) != 0);
                break;
            case ByteCode.ASM_BIT_AND:
                a = ((int)m_codeTable[pc++]&0xFF);
                b = ((int)m_codeTable[pc++]&0xFF);
                r = register[regBase+a];
                r.setInt (r.getInt() & register[regBase+b].getInt());
                break;
            case ByteCode.ASM_BIT_OR:
                a = ((int)m_codeTable[pc++]&0xFF);
                b = ((int)m_codeTable[pc++]&0xFF);
                r = register[regBase+a];
                r.setInt (r.getInt() | register[regBase+b].getInt());
                break;
            case ByteCode.ASM_BIT_XOR:
                a = ((int)m_codeTable[pc++]&0xFF);
                b = ((int)m_codeTable[pc++]&0xFF);
                r = register[regBase+a];
                r.setInt (r.getInt() ^ register[regBase+b].getInt());
                break;
            case ByteCode.ASM_BIT_INV:
                a = ((int)m_codeTable[pc++]&0xFF);
                r = register[regBase+a];
                r.setInt (~ r.getInt());
                break;
            case ByteCode.ASM_BIT_LS:
                a = ((int)m_codeTable[pc++]&0xFF);
                b = ((int)m_codeTable[pc++]&0xFF);
                r = register[regBase+a];
                r.setInt (r.getInt() << register[regBase+b].getInt());
                break;
            case ByteCode.ASM_BIT_RS:
                a = ((int)m_codeTable[pc++]&0xFF);
                b = ((int)m_codeTable[pc++]&0xFF);
                r = register[regBase+a];
                r.setInt (r.getInt() >> register[regBase+b].getInt());
                break;
            case ByteCode.ASM_BIT_RRS:
                a = ((int)m_codeTable[pc++]&0xFF);
                b = ((int)m_codeTable[pc++]&0xFF);
                r = register[regBase+a];
                r.setInt (r.getInt() >>> register[regBase+b].getInt());
                break;
//#ifdef MM.JsByteCodeCompat
            case ByteCode.ASM_JUMP_COMPAT:
                a = ((int)m_codeTable[pc++]&0xFF);
                pc = m_codeOffset + m_jumpTable[a];
                break;
            case ByteCode.ASM_JUMP_ZERO_COMPAT:
                a = ((int)m_codeTable[pc++]&0xFF);
                b = ((int)m_codeTable[pc++]&0xFF);
                if (register[regBase+a].getInt () == 0) {
                    pc = m_codeOffset + m_jumpTable[b];
                }
                break;
//#endif
            case ByteCode.ASM_JUMP:
                a = ((int)m_codeTable[pc++]&0xFF);
                b = ((int)m_codeTable[pc++]&0xFF);
                pc = m_codeOffset + (a<<8 | b);
                break;
            case ByteCode.ASM_JUMP_ZERO:
                a = ((int)m_codeTable[pc++]&0xFF);
                if (register[regBase+a].getInt () == 0) {
                    b = ((int)m_codeTable[pc++]&0xFF);
                    d = ((int)m_codeTable[pc++]&0xFF);
                    pc = m_codeOffset + (b<<8 | d);
                } else {
                    pc += 2;
                }
                break;
            case ByteCode.ASM_JUMP_NZERO:
                a = ((int)m_codeTable[pc++]&0xFF);
                if (register[regBase+a].getInt () != 0) {
                    b = ((int)m_codeTable[pc++]&0xFF);
                    d = ((int)m_codeTable[pc++]&0xFF);
                    pc = m_codeOffset + (b<<8 | d);
                } else {
                    pc += 2;
                }
                break;
            case ByteCode.ASM_EXT_CALL:
                a = ((int)m_codeTable[pc++]&0xFF);
                b = ((int)m_codeTable[pc++]&0xFF);
                d = ((int)m_codeTable[pc++]&0xFF);
                e = ((int)m_codeTable[pc++]&0xFF);
                ExternCall.doCall (m, c, a, b, register, regBase+d, e);
                break;
            case ByteCode.ASM_INT_CALL:
                a = ((int)m_codeTable[pc++]&0xFF);
                b = ((int)m_codeTable[pc++]&0xFF);
                m.run (c, a, regBase+b);
                break;
            case ByteCode.ASM_RETURN:
                a = ((int)m_codeTable[pc++]&0xFF);
                register [regBase].set (register[regBase+a]);
                return (true);
            case ByteCode.ASM_FIELD_PUSH:
                pushedField = currentField;
                pushedIndex = currentIndex;
                currentField = c.script;
                break;
            case ByteCode.ASM_FIELD_POP:
                currentField = pushedField;
                currentIndex = pushedIndex;
                break;
            case ByteCode.ASM_FIELD_USE_INT:
                a = ((int)m_codeTable[pc++]&0xFF);
                if (currentField != null) {
                    currentField = currentField.use (a);
                }
                break;
            case ByteCode.ASM_FIELD_IDX_REG:
                a = ((int)m_codeTable[pc++]&0xFF);
                currentIndex = register[regBase+a].getInt ();
                break;
            case ByteCode.ASM_FIELD_SET_INT_REG:
                a = ((int)m_codeTable[pc++]&0xFF);
                b = ((int)m_codeTable[pc++]&0xFF);
                if (currentField != null) {
                    currentField.set (a, register[regBase+b], currentIndex);
                }
                currentIndex = -1;
                currentField = c.script;
                break;
            case ByteCode.ASM_FIELD_GET_INT_REG:
                a = ((int)m_codeTable[pc++]&0xFF);
                b = ((int)m_codeTable[pc++]&0xFF);
                if (currentField != null) {
                    currentField.get (a, register[regBase+b], currentIndex);
                }
                currentIndex = -1;
                currentField = c.script;
                break;
            default:
                System.err.println ("Unknown op code: "+opcode);
            }
            opcode = ((int)m_codeTable[pc++]&0xFF);
        }
        return true;
    }

//#ifdef MM.JsByteCodeCompat
    /*
     * CODE FOR DECODING THE OLD BYTECODE FORMAT
     */
    
    int m_curCode, m_nbCode;
    int m_nbJump;
    int m_curStr, m_nbStr;
    int m_curInt, m_nbInt;
    int [] m_jumpTable;

    static StringBuffer s_sb = new StringBuffer(); // for UTF8

    int addConstant (int value) {
        if (m_curInt >= m_nbInt) {
            int oldSize = m_nbInt;
            m_nbInt += 8;
            int [] tmp = new int [m_nbInt]; 
            System.arraycopy(m_intTable, 0, tmp, 0, oldSize);
            m_intTable = tmp;
        }
        m_intTable[m_curInt++] = value;
        return m_curInt-1;
    }

    int addConstant (String value) {
        if (m_curStr >= m_nbStr) {
            int oldSize = m_nbStr;
            m_nbStr += 8;
            String [] tmp = new String [m_nbStr]; 
            System.arraycopy(m_strTable, 0, tmp, 0, oldSize);
            m_strTable = tmp;
        }
        m_strTable[m_curStr++] = value;
        return m_curStr-1;
    }

    int addJump (int value) {
        if (value >= m_nbJump) {
            int oldSize = m_nbJump;
            m_nbJump += 8;
            int [] tmp = new int [m_nbJump]; 
            System.arraycopy(m_jumpTable, 0, tmp, 0, oldSize);
            m_jumpTable = tmp;
        }
        m_jumpTable[value] = m_curCode;
        return 2;
    }

    void addByte (int code) {
        if (m_curCode >= m_nbCode) {
            int oldSize = m_nbCode;
            m_nbCode += 64;
            byte [] tmp = new byte [m_nbCode]; 
            System.arraycopy(m_codeTable, 0, tmp, 0, oldSize);
            m_codeTable = tmp;
        }
        m_codeTable[m_curCode++] = (byte)code;
    }
     
    int add_O (int code) {
        addByte (code);
        return 1;
    }

    int add_OB (int code, DataInputStream is) {
        add_O (code);
        addByte (Decoder.readUnsignedByte (is));
        return 2;
    }

    int add_OBI (int code, DataInputStream is) {
        add_OB (code, is);
        //addIndex (addConstant (Decoder.readInt (is)));
        addByte (addConstant (Decoder.readInt (is)));
        return 6;
    }

    int add_OBS (int code, DataInputStream is) {
        add_OB (code, is);
        int size = Decoder.readString (is, s_sb);
        //addIndex (addConstant (s_sb.toString()));
        addByte (addConstant (s_sb.toString()));
        return size + 2;
    }

    int add_OBB (int code, DataInputStream is) {
        add_OB (code, is);
        addByte (Decoder.readUnsignedByte (is));
        return 3;
    }

    int add_OBBBB (int code, DataInputStream is) {
        add_OBB (code, is);
        addByte (Decoder.readUnsignedByte (is));
        addByte (Decoder.readUnsignedByte (is));
        return 5;
    }

    static int s_saved = 0;
    // cut down unused memory
    void pack () {
//         Logger.println ("Function.pack:");
//         Logger.println ("    code: "+m_curCode+" / "+m_nbCode);
//         Logger.println ("     int: "+m_curInt+" / "+m_nbInt);
        //Logger.println ("Jump table :"+m_nbJump);
        //Logger.println ("Int table :"+m_curInt+" / "+m_nbInt);
        //Logger.println ("Str table :"+m_curStr+" / "+m_nbStr);
 //         int saved = 0;

        byte [] tmpb = new byte [m_curCode]; 
        System.arraycopy(m_codeTable, 0, tmpb, 0, m_curCode);
        m_codeTable = tmpb;
//        saved = m_nbCode - m_curCode;

        int [] tmpi = new int [m_curInt]; 
        System.arraycopy(m_intTable, 0, tmpi, 0, m_curInt);
        m_intTable = tmpi;

//         saved += (m_nbInt - m_curInt) * 4;
//         s_saved += saved;
//         Logger.println ("    savings: "+saved+" / "+s_saved);
    }

    Function (DataInputStream is) {
        //Logger.println("Function from bytes");
        int i = 0;
        int len = Decoder.readInt (is);

        m_codeTable = new byte [m_nbCode = len];
        m_jumpTable = new int [m_nbJump = 4];
        m_intTable = new int [m_nbInt = 16];
        m_strTable = new String [m_nbStr = 16];

        while (i < len) {
            //System.out.println ("Function: i="+i+", len="+len);
            int code = Decoder.readUnsignedByte (is);

            switch (code) {
            case ByteCode.ASM_NOP:
            case ByteCode.ASM_FIELD_PUSH:
            case ByteCode.ASM_FIELD_POP:
                i += add_O (code);
                break;
            case ByteCode.ASM_JUMP_COMPAT:
            case ByteCode.ASM_RETURN:
            case ByteCode.ASM_FIELD_IDX_REG:
            case ByteCode.ASM_FIELD_USE_INT:
            case ByteCode.ASM_BIT_INV:
                i += add_OB (code, is);
                break;
            case ByteCode.ASM_LOAD_REG_INT:
            case ByteCode.ASM_LOAD_REG_FLT:
                i += add_OBI (code, is);
                break;
            case ByteCode.ASM_LOAD_REG_STR:
                i += add_OBS (code, is);
                break;
            case ByteCode.ASM_MOVE_REG_REG:
            case ByteCode.ASM_ADD_REG_REG:
            case ByteCode.ASM_SUB_REG_REG:
            case ByteCode.ASM_MUL_REG_REG:
            case ByteCode.ASM_DIV_REG_REG:
            case ByteCode.ASM_MOD_REG_REG:
            case ByteCode.ASM_TEST_GRE:
            case ByteCode.ASM_TEST_GRT:
            case ByteCode.ASM_TEST_LES:
            case ByteCode.ASM_TEST_LEE:
            case ByteCode.ASM_TEST_EQU:
            case ByteCode.ASM_TEST_NEQ:
            case ByteCode.ASM_TEST_AND:
            case ByteCode.ASM_TEST_OR:
            case ByteCode.ASM_BIT_AND:
            case ByteCode.ASM_BIT_OR:
            case ByteCode.ASM_BIT_XOR:
            case ByteCode.ASM_BIT_LS:
            case ByteCode.ASM_BIT_RS:
            case ByteCode.ASM_BIT_RRS:
            case ByteCode.ASM_INT_CALL:
            case ByteCode.ASM_JUMP_ZERO_COMPAT:
            case ByteCode.ASM_FIELD_SET_INT_REG:
            case ByteCode.ASM_FIELD_GET_INT_REG:
                i += add_OBB (code, is);
                break;
            case ByteCode.ASM_EXT_CALL:
                i += add_OBBBB (code, is);
                break;
            case ByteCode.ASM_LABEL_COMPAT:
                i += addJump (Decoder.readUnsignedByte (is));
                break;
            default:
                System.out.println ("Decoding unknown code: "+code);
            }
        }
        add_O (END_OF_CODE);

        pack ();
    }
//#endif
}