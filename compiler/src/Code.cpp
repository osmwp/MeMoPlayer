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

# include <stdio.h>
# include <stdlib.h>
# include <string.h>

# include "Code.h"
# include "Tokenizer.h"
# include "Types.h"
# include "Scripter.h"

ByteCode::ByteCode (StringTable * stringTable, IntTable * intTable) {
    m_bytecode = m_bytecodeStorage;
    for (int i = 0; i < MAX_REGISTERS; i++) {
        m_registery[i].m_index = i;
        m_registery[i].m_isUsed = false;
        m_registery[i].m_level = 0;
    }
    m_topRegister = -1;
    m_bunchMode = false;
    m_maxRegister = 0;
    m_maxLabels = 0;

    m_stringTable = stringTable;
    m_intTable = intTable;
}

void ByteCode::freeRegister (int i) {
    //printf ("11 freeRegister: %d (%d)\n", i, m_topRegister);
    if (i >= 0 && i < MAX_REGISTERS) {
        m_registery[i].m_isUsed = false;
        m_registery[i].m_level = 0;
        if (i == m_topRegister) {
            while (i >= 0 && m_registery[i].m_isUsed == false) {
                i--;
            }
            m_topRegister = i;
            //printf (" $$ freeRegister: top is %d\n", i);
        }
    } else {
        printf ("ERROR: freeRegister: bad index %d\n", i);
        ((char *)0)[0] = 0;
    }
    //printf ("22 freeRegister: %d (%d)\n", i, m_topRegister);
    //printRegisters ();
}


bool ByteCode::setRegBunchMode (bool yes) {
    bool old = m_bunchMode;
    m_bunchMode = yes;
    return old;
}

int ByteCode::getRegister (int level) {
    int i = getRegister2 (level);
    if (i > m_maxRegister) {
        m_maxRegister = i;
    }
    return i;
}

int ByteCode::getRegister2 (int level) {
    if (m_bunchMode) {
        int i = ++m_topRegister;
        m_registery[i].m_isUsed = true;
        m_registery[i].m_level = level;
        return i;
    }
    for (int i = 0; i < MAX_REGISTERS; i++) {
        if (m_registery[i].m_isUsed == false) {
            m_registery[i].m_isUsed = true;
            m_registery[i].m_level = level;
            //printf (" $$ getRegister: %d\n", i);
            if (i > m_topRegister) { 
                m_topRegister = i;
                //printf (" $$ getRegister: top is %d\n", i);
            }
            //printRegisters ();
            return i;
        }
    }
    fprintf (stderr, "Error: no more free register\n");
    return (-1);
}

void ByteCode::printRegisters () {
    printf ("[[");
    for (int i = 0; i <= m_topRegister; i ++) {
        printf ("%c", m_registery[i].m_isUsed ? '1' : '0');
    }
    printf ("]]\n");
}

static void exchangeBytes (unsigned char * s) {
    char t = s[0];
    s[0] = s[3];
    s[3] = t;
    t = s[1];
    s[1] = s[2];
    s[2] = t;
}

int ByteCode::getLabel () {
    if (m_maxLabels >= MAX_LABELS) {
        fprintf (stderr, "Max labels (%d) per Script reached !\n",MAX_LABELS);
        exit (-1);
    }
    return m_maxLabels++;
}

void ByteCode::setLabel (int labelIndex) {
    m_labels [labelIndex] = m_bytecode - m_bytecodeStorage;
}

void ByteCode::addByte (int i) {
    *m_bytecode++ = (unsigned char)(i & 0xFF);
}

void ByteCode::addInt (int i) {
    addByte (m_intTable->findOrAdd (i));
}

void ByteCode::addFloat (float f) {
    addInt (int(f*65536));
}

void ByteCode::addString (char * s) {
    addByte (m_stringTable->findOrAdd (s));
}

void ByteCode::add (int opcode) {
    addByte (opcode);
}

void ByteCode::add (int opcode, int reg1) {
    add (opcode);
    addByte (reg1);
}

void ByteCode::add (int opcode, int reg1, int reg2) {
    add (opcode, reg1);
    addByte (reg2);
}

void ByteCode::add (int opcode, int reg1, int reg2, int reg3) {
    add (opcode, reg1, reg2);
    addByte (reg3);
}

void ByteCode::add (int opcode, int reg1, int reg2, int reg3, int reg4) {
    add (opcode, reg1, reg2, reg3);
    addByte (reg4);
}

void ByteCode::add (int opcode, int reg1, char * s) {
    add (opcode, reg1);
    addString (s);
}

unsigned char * ByteCode::getCode (int & len) {
    len = m_bytecode - m_bytecodeStorage;
    if (len <= 0) {
        return NULL;
    }
    char * tmp = (char *)malloc (len);
    memcpy (tmp, m_bytecodeStorage, len);
    return ((unsigned char *)tmp);
}

unsigned char * ByteCode::getJumpTable (int & len) {
    len = m_maxLabels * 4; // sizeof (int)
    unsigned char * buff = NULL;
    if (len > 0) {
        buff = (unsigned char *) malloc (len);
        unsigned char * t = buff;
        unsigned char * s; 
        for (int i=0; i<m_maxLabels; i++) {
            s = (unsigned char *) &m_labels[i];
            *(t++) = s[3];
            *(t++) = s[2];
            *(t++) = s[1];
            *(t++) = s[0];
        }
    }
    return buff;
}

int ByteCode::setBreakLabel (int label)  {
  int oldLabel = m_breakLabel;
  m_breakLabel = label;
  return oldLabel;
}

int ByteCode::getBreakLabel () {
  return m_breakLabel;
}

int ByteCode::setContinueLabel (int label)  {
  int oldLabel = m_continueLabel;
  m_continueLabel = label;
  return oldLabel;
}

int ByteCode::getContinueLabel () {
  return m_continueLabel;
}

void printInt (unsigned char * data) {
    exchangeBytes (data);
    printf ("%d ", *((int*)data));
    exchangeBytes (data);
}

void printFloat (unsigned char * data) {
    exchangeBytes (data);
    int tmp = * ((int *)data);
    printf ("%g ", (tmp/65536.0f));
    exchangeBytes (data);
}

void printByte (unsigned char * data) {
    printf ("%d ", data[0]);
}

void ByteCode::dump (unsigned char * data, int len) {
    unsigned char * end = data + len;
    while (data < end) {
        switch (*data) {
        case ASM_NOP: printf ("    ASM_NOP\n"); data++; break;
        case ASM_LOAD_REG_INT: 
            printf ("    ASM_LOAD_REG_INT "); data++; 
            printByte (data); data += 1;
            printInt (data); data += 4;
            break;
        case ASM_LOAD_REG_FLT: 
            printf ("    ASM_LOAD_REG_FLT "); data++; 
            printByte (data); data += 1;
            printFloat (data); data += 4;
            break;
        case ASM_LOAD_REG_STR: 
            printf ("    ASM_LOAD_REG_STR "); data++; 
            printByte (data); data += 1;
            printf ("'");
            while (*data) {
                printf ("%c", *data); data += 1;
            }
            printf ("'");
            data ++;
            break;
        case ASM_MOVE_REG_REG: 
            printf ("    ASM_MOV_REG_REG "); data++; 
            printByte (data); data += 1;
            printByte (data); data += 1;
            break;

        case ASM_FIELD_PUSH:
            printf ("    ASM_FIELD_PUSH "); data++; 
            break;
        case ASM_FIELD_POP:
            printf ("    ASM_FIELD_POP "); data++; 
            break;
        case ASM_FIELD_USE_INT:
            printf ("    ASM_FIELD_USE_INT "); data++; 
            printByte (data); data += 1;
            break;
        case ASM_FIELD_IDX_REG:
            printf ("    ASM_FIELD_IDX_REG "); data++; 
            printByte (data); data += 1;
            break;
        case ASM_FIELD_SET_INT_REG:
            printf ("    ASM_FIELD_SET_INT_REG "); data++; 
            printByte (data); data += 1;
            printByte (data); data += 1;
            break;
        case ASM_FIELD_GET_INT_REG:
            printf ("    ASM_FIELD_GET_INT_REG "); data++; 
            printByte (data); data += 1;
            printByte (data); data += 1;
            break;

        case ASM_ADD_REG_REG: 
            printf ("    ASM_ADD_REG_REG "); data++; 
            printByte (data); data += 1;
            printByte (data); data += 1;
            break;
        case ASM_SUB_REG_REG: 
            printf ("    ASM_SUB_REG_REG "); data++; 
            printByte (data); data += 1;
            printByte (data); data += 1;
            break;
        case ASM_MUL_REG_REG: 
            printf ("    ASM_MUL_REG_REG "); data++; 
            printByte (data); data += 1;
            printByte (data); data += 1;
            break;
        case ASM_DIV_REG_REG: 
            printf ("    ASM_DIV_REG_REG "); data++; 
            printByte (data); data += 1;
            printByte (data); data += 1;
            break;
        case ASM_MOD_REG_REG: 
            printf ("    ASM_MOD_REG_REG "); data++; 
            printByte (data); data += 1;
            printByte (data); data += 1;
            break;
        case ASM_TEST_EQU: 
            printf ("    ASM_TEST_EQU "); data++; 
            printByte (data); data += 1;
            printByte (data); data += 1;
            break;
        case ASM_TEST_NEQ:
            printf ("    ASM_TEST_NEQ "); data++; 
            printByte (data); data += 1;
            printByte (data); data += 1;
            break;
        case ASM_TEST_LES:
            printf ("    ASM_TEST_LES "); data++; 
            printByte (data); data += 1;
            printByte (data); data += 1;
            break;
        case ASM_TEST_LEE:
            printf ("    ASM_TEST_LEE "); data++; 
            printByte (data); data += 1;
            printByte (data); data += 1;
            break;
        case ASM_TEST_GRT:
            printf ("    ASM_TEST_GRT "); data++; 
            printByte (data); data += 1;
            printByte (data); data += 1;
            break;
        case ASM_TEST_GRE:
            printf ("    ASM_TEST_GRE "); data++; 
            printByte (data); data += 1;
            printByte (data); data += 1;
            break;
        case ASM_TEST_AND:
            printf ("    ASM_TEST_AND "); data++; 
            printByte (data); data += 1;
            printByte (data); data += 1;
            break;
        case ASM_TEST_OR:
            printf ("    ASM_TEST_OR "); data++; 
            printByte (data); data += 1;
            printByte (data); data += 1;
            break;
        case ASM_BIT_AND:
            printf ("    ASM_BIT_AND "); data++; 
            printByte (data); data += 1;
            printByte (data); data += 1;
            break;
        case ASM_BIT_OR:
            printf ("    ASM_BIT_OR "); data++; 
            printByte (data); data += 1;
            printByte (data); data += 1;
            break;
        case ASM_BIT_XOR:
            printf ("    ASM_BIT_XOR "); data++; 
            printByte (data); data += 1;
            printByte (data); data += 1;
            break;
        case ASM_BIT_INV:
            printf ("    ASM_BIT_INV "); data++; 
            printByte (data); data += 1;
            break;
        case ASM_BIT_LS:
            printf ("    ASM_BIT_LS "); data++; 
            printByte (data); data += 1;
            printByte (data); data += 1;
            break;
        case ASM_BIT_RS:
            printf ("    ASM_BIT_RS "); data++; 
            printByte (data); data += 1;
            printByte (data); data += 1;
            break;
        case ASM_BIT_RRS:
            printf ("    ASM_BIT_RRS "); data++; 
            printByte (data); data += 1;
            printByte (data); data += 1;
            break;
        case ASM_JUMP:
            printf ("    ASM_JMP_ZERO "); data++; 
            printByte (data); data += 1;
            break;
        case ASM_JUMP_ZERO:
            printf ("    ASM_JMP_ZERO "); data++; 
            printByte (data); data += 1;
            printByte (data); data += 1;
            break;
        case ASM_EXT_CALL:
            printf ("    ASM_EXT_CALL "); data++; 
            printByte (data); data += 1;
            printByte (data); data += 1;
            printByte (data); data += 1;
            printByte (data); data += 1;
            break;
        case ASM_INT_CALL:
            printf ("    ASM_INT_CALL "); data++; 
            printByte (data); data += 1;
            printByte (data); data += 1;
            break;
        case ASM_RETURN:
            printf ("    ASM_RETURN "); data++; 
            printByte (data); data += 1;
            break;
        default:
            printf ("Unknwon asm code: %d\n", *data);
            return;
        }
        printf ("\n");
    }
}

void ByteCode::generate (char * n) {
    FILE * fp = fopen (n, "wb");
    if (fp == NULL) {
        fprintf (stderr, "Error: cannot open %s for writing\n", n);
        return;
    }
    fprintf (fp, "// File automatically generated, do not edit\n");
    fprintf (fp, "\npackage memoplayer;\n\n");
    fprintf (fp, "public class ByteCode {\n");
    fprintf (fp, "    final static int ASM_ERROR = %d;\n", ASM_ERROR);
    fprintf (fp, "    final static int ASM_NOP = %d;\n", ASM_NOP);
    //fprintf (fp, "    final static int ASM_ALLOC = %d;\n", ASM_ALLOC);
    //fprintf (fp, "    final static int ASM_FREE = %d;\n", ASM_FREE);
    fprintf (fp, "    final static int ASM_JUMP = %d;\n", ASM_JUMP);
    fprintf (fp, "    final static int ASM_JUMP_ZERO = %d;\n", ASM_JUMP_ZERO);
    fprintf (fp, "    final static int ASM_EXT_CALL = %d;\n", ASM_EXT_CALL);
    fprintf (fp, "    final static int ASM_INT_CALL = %d;\n", ASM_INT_CALL);
    fprintf (fp, "    final static int ASM_RETURN = %d;\n", ASM_RETURN);
    fprintf (fp, "    final static int ASM_LOAD_REG_INT = %d;\n", ASM_LOAD_REG_INT);
    fprintf (fp, "    final static int ASM_LOAD_REG_FLT = %d;\n", ASM_LOAD_REG_FLT);
    fprintf (fp, "    final static int ASM_LOAD_REG_STR = %d;\n", ASM_LOAD_REG_STR);
    //fprintf (fp, "    final static int ASM_LOAD_REG_REL = %d;\n", ASM_LOAD_REG_REL);
    fprintf (fp, "    final static int ASM_MOVE_REG_REG = %d;\n", ASM_MOVE_REG_REG);
    //fprintf (fp, "    final static int ASM_SAVE_REG_REL = %d;\n", ASM_SAVE_REG_REL);
    //     fprintf (fp, "    final static int ASM_INC_REG = %d;\n", ASM_INC_REG);
    //     fprintf (fp, "    final static int ASM_DEC_REG = %d;\n", ASM_DEC_REG);
    fprintf (fp, "    final static int ASM_ADD_REG_REG = %d;\n", ASM_ADD_REG_REG);
    fprintf (fp, "    final static int ASM_SUB_REG_REG = %d;\n", ASM_SUB_REG_REG);
    fprintf (fp, "    final static int ASM_MUL_REG_REG = %d;\n", ASM_MUL_REG_REG);
    fprintf (fp, "    final static int ASM_DIV_REG_REG = %d;\n", ASM_DIV_REG_REG);
    fprintf (fp, "    final static int ASM_MOD_REG_REG = %d;\n", ASM_MOD_REG_REG);
    fprintf (fp, "    final static int ASM_TEST_EQU = %d;\n", ASM_TEST_EQU);
    fprintf (fp, "    final static int ASM_TEST_NEQ = %d;\n", ASM_TEST_NEQ);
    fprintf (fp, "    final static int ASM_TEST_GRT = %d;\n", ASM_TEST_GRT);
    fprintf (fp, "    final static int ASM_TEST_GRE = %d;\n", ASM_TEST_GRE);
    fprintf (fp, "    final static int ASM_TEST_LES = %d;\n", ASM_TEST_LES);
    fprintf (fp, "    final static int ASM_TEST_LEE = %d;\n", ASM_TEST_LEE);
    fprintf (fp, "    final static int ASM_TEST_AND = %d;\n", ASM_TEST_AND);
    fprintf (fp, "    final static int ASM_TEST_OR = %d;\n", ASM_TEST_OR);
    fprintf (fp, "    final static int ASM_BIT_AND = %d;\n", ASM_BIT_AND);
    fprintf (fp, "    final static int ASM_BIT_OR = %d;\n", ASM_BIT_OR);
    fprintf (fp, "    final static int ASM_BIT_XOR = %d;\n", ASM_BIT_XOR);
    fprintf (fp, "    final static int ASM_BIT_INV = %d;\n", ASM_BIT_INV);
    fprintf (fp, "    final static int ASM_BIT_LS = %d;\n", ASM_BIT_LS);
    fprintf (fp, "    final static int ASM_BIT_RS = %d;\n", ASM_BIT_RS);
    fprintf (fp, "    final static int ASM_BIT_RRS = %d;\n", ASM_BIT_RRS);
    //fprintf (fp, "    final static int ASM_RET = %d;\n", ASM_RET);
    //fprintf (fp, "    final static int ASM_PUSH_BASE = %d;\n", ASM_PUSH_BASE);
    //fprintf (fp, "    final static int ASM_POP_BASE = %d;\n", ASM_POP_BASE);

    fprintf (fp, "    final static int ASM_FIELD_PUSH = %d;\n", ASM_FIELD_PUSH);
    fprintf (fp, "    final static int ASM_FIELD_POP = %d;\n", ASM_FIELD_POP);
    fprintf (fp, "    final static int ASM_FIELD_USE_INT = %d;\n", ASM_FIELD_USE_INT);
    fprintf (fp, "    final static int ASM_FIELD_IDX_REG = %d;\n", ASM_FIELD_IDX_REG);
    fprintf (fp, "    final static int ASM_FIELD_SET_INT_REG = %d;\n", ASM_FIELD_SET_INT_REG);
    fprintf (fp, "    final static int ASM_FIELD_GET_INT_REG = %d;\n", ASM_FIELD_GET_INT_REG);
    fprintf (fp, "}\n");
}


bool debugOutput = true;

# define PRINTF if (debugOutput) printf

Code::Code (char value) {
    init ();
    m_type = CODE_CHAR;
    m_ival = value;
}

Code::Code (int value) {
    init ();
    m_type = CODE_INT;
    m_ival = value;
}

Code::Code (float value) {
    init ();
    m_type = CODE_FLOAT;
    m_fval = value;
}

Code::Code (char * name, bool isString) {
    init ();
    if (isString) {
        m_type = CODE_STRING;
        m_name = name;
    } else {
        m_type = CODE_NAME;
        m_name = strdup (name);
    }
}

// Code::Code (char * name, char * className, bool isVar) {
//     init ();
//     m_type = isVar ? CODE_STRING : CODE_NAME;
//     m_name = strdup (name);
//     if (className) {
//         m_class = strdup (className);
//     }
// }

Code::Code (int op, Code * left, Code * right, Code * third) {
    init ();
    m_type = op;
    m_first = left;
    m_second = right;
    m_third = third;
}

Code::~Code () {
    if (m_name && m_type != CODE_STRING) {
        free (m_name);
    }
    if (m_class) {
        free (m_class);
    }
}


Code * Code::cloneInvertAccess () {
    int t = m_type;
    if (t == CODE_SET_VAR) {
        t = CODE_GET_VAR;
    } else if (t == CODE_GET_VAR) {
        t = CODE_SET_VAR;
    } else if (t == CODE_SET_FIELD) {
        t = CODE_GET_FIELD;
    } else if (t == CODE_SET_FIELD) {
        t = CODE_GET_FIELD;
    }
    Code * tmp = new Code (t, NULL, NULL, NULL);
    tmp->m_ival = m_ival;
    tmp->m_fval = m_fval;
    tmp->m_name = m_name;

    if (m_first) {
        tmp->m_first = m_first->cloneInvertAccess ();
    }
    if (m_second) {
        tmp->m_second = m_second->cloneInvertAccess ();
    }
    if (m_third) {
        tmp->m_third= m_third->cloneInvertAccess ();
    }
    if (m_next) {
        tmp->m_next = m_next ->cloneInvertAccess (); 
    }
    return (tmp);
}

void Code::destroy () {
    if (m_first) {
        m_first->destroy ();
    }
    if (m_second) {
        m_second->destroy ();
    }
    if (m_third) {
        m_third->destroy ();
    }
    if (m_next) {
        m_next->destroy ();
    }
    delete this;
}

bool Code::isTerm () {
    return (m_type == CODE_CHAR || m_type == CODE_INT || m_type == CODE_NAME);
}

void printSpaces (int n, const char * before = NULL, const char * after = NULL) {
    if (before) {
        printf ("%s", before);
    }
    while (n-->0) {
        printf (" ");
    }
    if (after) {
        printf ("%s", after);
    }
}

void Code::printAux (const char * left, const char * middle, const char * right, int level) {
    if (m_first != NULL) {
        if (level == 0) {
            printf ("%s", left);
        }
        m_first->print ();
    } else { 
        printf ("NULL");
    }
    printf ("%s", middle);
    if (m_second != NULL) {
        m_second->print ();
        if (level == 0) {
            printf ("%s", right);
        }
    } else { 
        printf ("NULL");
    }
}

/*void Code::dump () {
    printf ("// type  : %d\n", m_type);
    printf ("// value : %d\n", m_ival );
    printf ("// name  : %s\n", m_name ? m_name : "NULL");
    printf ("// first : %p\n", m_first ? m_first : 0);
    printf ("// second: %p\n", m_second ? m_second : 0);
    printf ("// third : %p\n", m_third ? m_third : 0);
    printf ("// next  : %p\n", m_next ? m_next : 0);
    }*/


void Code::print (int indentLevel) {
    switch (m_type) {
    case CODE_NOP:
        break;
    case CODE_INT: 
        printf ("%d", m_ival); break;
    case CODE_FLOAT: 
        printf ("%g", m_fval); break;
    case CODE_CHAR:
        if (m_ival == '\n') {
            printf ("'\\n'");
        } else if (m_ival == '\t') {
            printf ("'\\t'");
        } else if (m_ival == '\0') {
            printf ("'\\0'");
        } else {
            printf ("'%c'", m_ival); 
        }
        break;
    case CODE_STRING: 
        printf ("\"%s\"", m_name); break;
    case CODE_NAME: 
        printf ("%s", m_name); break;
    case CODE_PARAM:
        m_first->print ();
        if (m_next) {
            printf (", ");
            m_next->print (0);
        }
        break;
    case CODE_CALL_STATIC:
        printSpaces (indentLevel);
        m_first->print (0);
        printf ("."); 
        m_second->print (0);
        printf (" (");
        if (m_third) { m_third->print (); }
        printf (");\n");
        break;
    case CODE_CALL_FUNCTION:
        printSpaces (indentLevel);
        m_first->print (0);
        printf (" (");
        if (m_second) { m_second->print (); }
        printf (");\n");
        break;
    case CODE_RETURN:
        printSpaces (indentLevel);
        printf (" (");
        if (m_first) { m_first->print (); }
        printf (");\n");
        break; 
    case CODE_NEW_VAR:
        printSpaces (indentLevel);
        printf ("var %s", m_first ? m_first->m_name : "null");
        break;
    case CODE_SET_VAR:
    case CODE_GET_VAR:
        if (m_first) { m_first->print (0); }
        break;
    case CODE_ASSIGN:
    case CODE_ASSIGN_AND_RETURN:
    case CODE_SELFPLUS: 
    case CODE_SELFMINUS: 
    case CODE_SELFDIV: 
    case CODE_SELFMULT: 
        printSpaces (indentLevel);
        if (m_first) {
            m_first->print (0);
        } else {
            printf ("ERROR: no lvalue defined!\n");
        }
        if (m_second) {
            switch (m_type) {
            case CODE_ASSIGN_AND_RETURN:
            case CODE_ASSIGN:  printf (" = "); break;
            case CODE_SELFPLUS:  printf (" += "); break;
            case CODE_SELFMINUS:  printf (" -= "); break;
            case CODE_SELFMULT:  printf (" *= "); break;
            case CODE_SELFDIV:  printf (" /= "); break;
            default: printf (" ??? "); break;
            }
            m_second->print (0);
        }
        printf (";\n"); break;
    case CODE_SET_FIELD: 
        printf (".%d<-", m_first->m_ival); break;
    case CODE_GET_FIELD: 
        printf (".%d->", m_first->m_ival); break;
    case CODE_USE_FIELD: 
        printf ("*(%d)", m_first->m_ival); 
        if (m_next) {
            m_next->print (0);
        } else {
            printf ("ERROR");
        }
        break;
    case CODE_USE_IDX_FIELD: 
        printf ("[");
        m_first->print (0); 
        printf ("]");
        break;
    case CODE_PLUS: 
        printAux ("(", " + ", ")", indentLevel); break;
    case CODE_MINUS:
        printAux ("(", " - ", ")", indentLevel); break;
    case CODE_MULT: 
        printAux ("(", " * ", ")", indentLevel); break;
    case CODE_DIV: 
        printAux ("(", " / ", ")", indentLevel); break;
    case CODE_MODULO: 
        printAux ("(", " %% ", ")", indentLevel); break;
    case CODE_EQUAL: 
        printAux ("(", " == ", ")", indentLevel); break;
    case CODE_NOTEQUAL: 
        printAux ("(", " != ", ")", indentLevel); break;
    case CODE_GREATER: 
        printAux ("(", " > ", ")", indentLevel); break;
    case CODE_GREATEQ: 
        printAux ("(", " >= ", ")", indentLevel); break;
    case CODE_LESSER: 
        printAux ("(", " < ", ")", indentLevel); break;
    case CODE_LESSEQ: 
        printAux ("(", " <= ", ")", indentLevel); break;
    case CODE_LOG_AND: 
        printAux ("(", " && ", ")", indentLevel); break;
    case CODE_LOG_OR: 
        printAux ("(", " || ", ")", indentLevel); break;
    case CODE_BIT_AND:
        printAux ("(", " & ", ")", indentLevel); break;
    case CODE_BIT_OR:
        printAux ("(", " | ", ")", indentLevel); break;
    case CODE_BIT_XOR:
        printAux ("(", " ^ ", ")", indentLevel); break;
    case CODE_BIT_INV:
        printSpaces (indentLevel);
        printf ("~ (");
        if (m_first) { m_first->print (); }
        printf (")\n");
        break;
    case CODE_BIT_LSHIFT:
        printAux ("(", " << ", ")", indentLevel); break;
    case CODE_BIT_RSHIFT:
        printAux ("(", " >> ", ")", indentLevel); break;
    case CODE_BIT_RRSHIFT:
        printAux ("(", " >>> ", ")", indentLevel); break;
    case CODE_BLOCK: 
        printf (" {\n" );
        if (m_first) {
            m_first->printAll (indentLevel+4);
        }
        printSpaces (indentLevel, NULL, "}");
        break;
    case CODE_IF: 
        printSpaces (indentLevel, NULL, "if ");
        if (m_first != NULL) {
            m_first->print ();
        }
        if (m_second) {
            if (m_second->m_type == CODE_BLOCK) {
                m_second->printAll (indentLevel);
            } else {
                printSpaces (indentLevel+4, " {\n");
                m_second->print ();
                printSpaces (indentLevel, NULL, "}");
            }
        }
        if (m_third) {
            printf (" else" );
            if (m_third->m_type == CODE_BLOCK) {
                m_third->printAll (indentLevel);
            } else {
                printSpaces (indentLevel+4, " {\n");
                m_third->print ();
                printSpaces (indentLevel, NULL, "}");
            }
        }
        printf ("\n");
        break;
    case CODE_WHILE: 
        printSpaces (indentLevel, NULL, "while ");
        if (m_first != NULL) {
            m_first->print ();
        }
        if (m_second) {
            if (m_second->m_type == CODE_BLOCK) {
                m_second->printAll (indentLevel);
            } else {
                printSpaces (indentLevel+4, " {\n");
                m_second->print ();
                printSpaces (indentLevel, NULL, "}");
            }
        }
        printf ("\n");
        break;
    case CODE_FOR: 
        printSpaces (indentLevel, NULL, "for (; ");
        if (m_first != NULL) {
            m_first->print ();
        }
        printf ("; ");
        if (m_third != NULL) {
          m_third->print ();
        } 
        printf (")");
        if (m_second) {
            if (m_second->m_type == CODE_BLOCK) {
                m_second->printAll (indentLevel);
            } else {
                printSpaces (indentLevel+4, " {\n");
                m_second->print ();
                printSpaces (indentLevel, NULL, "}");
            }
        }
        printf ("\n");
        break;
    case CODE_CONTINUE:
        printSpaces (indentLevel, NULL, "continue;\n");
        break;
    case CODE_BREAK:
        printSpaces (indentLevel, NULL, "break;\n");
        break;
    case CODE_TERNARY_COMP:
        printf ("( ");
        if (m_first) { m_first->print (); }
        printf (" ? ");
        if (m_second) { m_second->print (); }
        printf (" : ");
        if (m_third) { m_third->print (); }
        printf (")");
        break;

    case CODE_ERROR:
    default:  
        printf (" ERROR %d\n", m_type);
    }  
}

void Code::printAll (int indentLevel) {
    print (indentLevel);
    if (m_next) {
        m_next->printAll (indentLevel);
    }   
}

void Code::init () {
    m_type = CODE_NOP;
    m_ival = 0;
    m_name = NULL;
    m_class = NULL;
    m_next = m_first = m_second = m_third = NULL;
}

void Code::setSecondToLast (Code * code) {
    if (m_next) {
        m_next->setSecondToLast (code);
    } else {
        m_second = code;
    }
}
 
void Code::append (Code * code) {
    if (m_next) {
        m_next->append (code);
    } else {
        m_next = code;
    }
}

Code * Code::getNext () { 
    return (m_next); 
}

int Code::getLength () {
    if (m_next) {
        return (1+m_next->getLength ());
    } else {
        return (1);
    }
}

bool Code::generateAll (ByteCode * bc, Function * f) {
    generate (bc, f, 0);
    if (m_next) {
        m_next->generateAll (bc, f);
    } 
    return (true);
}

int Code::generateBinary (int opcode, ByteCode * bc, Function * f) {
    int reg1 = m_first->generate (bc, f, 0);
    int reg2 = m_second->generate (bc, f, 0);
    bc->add (opcode, reg1, reg2);
    bc->freeRegister (reg2);
    return (reg1);

}

int Code::generate (ByteCode * bc, Function * f, int reg) {
    if (reg < 0) {
        printf ("ERROR: generate: bad index %d\n", reg);
        ((char *)0)[0] = 0;
    }
    int reg1, reg2, reg3, nbParams;
    int breakLabel, continueLabel;
    int cnt1, cnt2;
    bool prevMode;
    Var * var;
    Code * tmp;
    Code * defaultLabel = NULL;
    switch (m_type) {
    case CODE_THEEND:
        bc->add (ByteCode::ASM_ENDOFCODE);
        break;
    case CODE_NOP:
        bc->add (ByteCode::ASM_NOP);
        break;
    case CODE_INT: 
        reg1 = bc->getRegister (f->m_blockLevel);
        bc->add (ByteCode::ASM_LOAD_REG_INT, reg1);
        bc->addInt (m_ival);
        return reg1;
    case CODE_FLOAT: 
        reg1 = bc->getRegister (f->m_blockLevel);
        bc->add (ByteCode::ASM_LOAD_REG_FLT, reg1);
        bc->addFloat (m_fval);
        return reg1;
    case CODE_STRING: 
        reg1 = bc->getRegister (f->m_blockLevel);
        bc->add (ByteCode::ASM_LOAD_REG_STR, reg1, m_name);
        return reg1;
    case CODE_NAME:
        printf ("generate CODE_NAME (%s) not implemented", m_name);
        return -1;
    case CODE_IF: 
        reg1 = m_first-> generate (bc, f, reg);
        reg2 = bc->getLabel ();
        bc->add (ByteCode::ASM_JUMP_ZERO, reg1, reg2);
        bc->freeRegister (reg1);
        m_second->generate (bc, f, reg);
        if (m_third) {
            reg3 = bc->getLabel ();
            bc->add (ByteCode::ASM_JUMP, reg3); // jump to end of else 
            bc->setLabel (reg2);
            m_third->generate (bc, f, reg);
            bc->setLabel (reg3);
        } else {
            bc->setLabel (reg2);
        }
        return (-1);
    case CODE_WHILE: 
        reg1 = bc->getLabel ();
        bc->setLabel (reg1);
        reg2 = m_first-> generate (bc, f, reg);
        reg3 = bc->getLabel ();
        bc->add (ByteCode::ASM_JUMP_ZERO, reg2, reg3);
        bc->freeRegister (reg2);
        continueLabel = bc->setContinueLabel (reg1);
        breakLabel = bc->setBreakLabel (reg3);
        m_second->generate (bc, f, reg);
        bc->add (ByteCode::ASM_JUMP, reg1); // jump to end of else 
        bc->setLabel (reg3);
        bc->setBreakLabel (breakLabel);
        bc->setContinueLabel (continueLabel);
        return (-1);
    case CODE_FOR:
        reg1 = bc->getLabel ();
        bc->setLabel (reg1);                           // mark condition
        reg2 = m_first-> generate (bc, f, reg);        // condition instruction
        reg3 = bc->getLabel ();
        bc->add (ByteCode::ASM_JUMP_ZERO, reg2, reg3); // test loop condition
        bc->freeRegister (reg2);
        cnt1 = bc->getLabel ();
        continueLabel = bc->setContinueLabel (cnt1);   // set new continue / break labels
        breakLabel = bc->setBreakLabel (reg3);         //  before start of loop (and keep the old ones)
        m_second->generate (bc, f, reg);               // loop instructions
        bc->setLabel (cnt1);                           // mark post instruction (for continue jumps)
        m_third->generate (bc, f, reg);                // post instruction
        bc->add (ByteCode::ASM_JUMP, reg1);            // jump back to loop condition
        bc->setLabel (reg3);                           // mark loop exit
        bc->setBreakLabel (breakLabel);                // restore previous break label
        bc->setContinueLabel (continueLabel);          // restore previous continue label
        return (-1);
    case CODE_CONTINUE:
        bc->add (ByteCode::ASM_JUMP, bc->getContinueLabel ());
        return (-1);
    case CODE_BREAK:
        bc->add (ByteCode::ASM_JUMP, bc->getBreakLabel ());
        return (-1);
    case CODE_DEFAULT:
    case CODE_CASE:
        bc->setLabel (m_ival); // use label index stored in m_ival
        return (-1);
    case CODE_SWITCH:
        reg1 = m_first-> generate (bc, f, reg);
        reg2 = bc->getLabel ();
        breakLabel = bc->setBreakLabel (reg2);
        // Parse all BLOCK's CASE & DEFAULT to build test & jump code
        tmp = m_second->m_first;
        while (tmp) {
            if (tmp->m_type == CODE_CASE) {
                tmp->m_ival = bc->getLabel (); // reuse m_ival to store label index
                reg3 = tmp->m_first->generate (bc, f, reg);
                bc->add (ByteCode::ASM_TEST_NEQ, reg3, reg1);
                bc->add (ByteCode::ASM_JUMP_ZERO, reg3, tmp->m_ival);
                bc->freeRegister (reg3);
            } else if (tmp->m_type == CODE_DEFAULT) {
                tmp->m_ival = bc->getLabel (); // reuse m_ival to store label
                defaultLabel = tmp; // keep default until the end
            }
            tmp = tmp->m_next;
        }
        bc->add (ByteCode::ASM_JUMP, defaultLabel ? defaultLabel->m_ival : reg2);
        bc->freeRegister (reg1);
        m_second->generate (bc, f, reg);
        bc->setLabel (reg2);
        bc->setBreakLabel (breakLabel);
        return (-1);
    case CODE_BLOCK:
        f->m_blockLevel++;
        if (m_first) {
            m_first->generateAll (bc, f);
        }
        f->removeVars (f->m_blockLevel, bc);
        f->m_blockLevel--;
        return -1;
    case CODE_PARAM:
        return m_first->generate (bc, f, reg);
        break;
    case CODE_CALL_STATIC:
        reg1 = m_first->m_ival;
        reg2 = m_second->m_ival;
        prevMode = bc->setRegBunchMode (true);
        if (m_third) {
            nbParams = 1;
            reg3 = m_third->generate (bc, f, reg);
            tmp = m_third->m_next; 
            while (tmp) {
                nbParams++;
                tmp->generate (bc, f, reg);
                tmp = tmp->m_next;
            }
        } else {
            nbParams = 0;
            reg3 = bc->getRegister (f->m_blockLevel); // at least one register to store the return value
        }
        bc->setRegBunchMode (prevMode);
        bc->add (ByteCode::ASM_EXT_CALL, reg1, reg2, reg3, nbParams);
        if (m_third) {
            tmp = m_third->m_next; 
            int r = reg3+1;
            while (tmp) {
                bc->freeRegister (r++);
                tmp = tmp->m_next;
            }
        }
        return reg3;
    case CODE_CALL_FUNCTION:
        reg1 = m_first->m_ival;
        prevMode = bc->setRegBunchMode (true);
        if (m_second) {
            reg2 = m_second->generate (bc, f, reg);
            tmp = m_second->m_next; 
            while (tmp) {
                tmp->generate (bc, f, reg);
                tmp = tmp->m_next;
            }
        } else {
            reg2 = bc->getRegister (f->m_blockLevel); // at least one register to store the return value
        }
        bc->setRegBunchMode (prevMode);
        bc->add (ByteCode::ASM_INT_CALL, reg1, reg2);
        if (m_second) {
            tmp = m_second->m_next; 
            int r = reg2+1;
            while (tmp) {
                bc->freeRegister (r++);
                tmp = tmp->m_next;
            }
        }
        return reg2;
    case CODE_RETURN:
        if (m_first) {
            reg1 = m_first->generate (bc, f, reg);
        } else {
            reg1 = bc->getRegister (f->m_blockLevel);
            bc->add (ByteCode::ASM_LOAD_REG_INT, reg1);
            bc->addInt (0);
        }
        bc->add (ByteCode::ASM_RETURN, reg1);
        return -1;
    case CODE_NEW_VAR:
        reg1 = bc->getRegister (f->m_blockLevel);
        var = f->addVar (m_first->m_name, f->m_blockLevel, reg1);
        if (var) {
            var->setIndex (reg1);
            bc->add (ByteCode::ASM_MOVE_REG_REG, reg1, reg);
            bc->freeRegister (reg);
        } else {
            fprintf (stderr, "error: no var found for %s\n", m_first->m_name);
        }
        return -1;
    case CODE_ASSIGN: 
    case CODE_ASSIGN_AND_RETURN: // same as ASSIGN but return the value
        if (m_second) {
            reg1 = m_second->generate (bc, f, reg);
            m_first->generate (bc, f, reg1);
        } else {
            reg1 = bc->getRegister (f->m_blockLevel);
            bc->add (ByteCode::ASM_LOAD_REG_INT, reg1);
            bc->addInt (0);
            m_first->generate (bc, f, reg1);
        }
        if (m_type == CODE_ASSIGN_AND_RETURN) {
            return reg1;
        }
        bc->freeRegister (reg1);
        return -1;
    case CODE_GET_VAR:
        var = f->findVar (m_first->m_name);
        //f->printVars ();
        if (var) {
            reg1 = bc->getRegister (f->m_blockLevel);
            reg2 = var->getIndex ();
            bc->add (ByteCode::ASM_MOVE_REG_REG, reg1, reg2);
            //printf (">> CODE_GET_VAR: %s is %d\n", m_first->m_name, reg2);
            return (reg1);
        } else {
            fprintf (stderr, "error: no var found for %s\n", m_first->m_name);
            exit (1);
        }
        return -1;
    case CODE_SET_VAR:
        var = f->findVar (m_first->m_name);
        if (var) {
            reg1 = var->getIndex ();
            bc->add (ByteCode::ASM_MOVE_REG_REG, reg1, reg);
        } else {
            fprintf (stderr, "error: no var found for %s\n", m_first->m_name);
            exit (1);
        }
        return -1;
    case CODE_SET_FIELD:
        bc->add (ByteCode::ASM_FIELD_SET_INT_REG, m_first->m_ival, reg);
        return -1;
    case CODE_GET_FIELD: 
        reg1 = bc->getRegister (f->m_blockLevel);
        bc->add (ByteCode::ASM_FIELD_GET_INT_REG, m_first->m_ival, reg1);
        //fprintf (stderr, ".%d->", m_first->m_ival); break;
        //printf ("$$ CODE_GET_FIELD => %d\n", reg1);
        return reg1;
        //printf (".%d->", m_first->m_ival); break;
    case CODE_USE_FIELD:
        bc->add (ByteCode::ASM_FIELD_USE_INT, m_first->m_ival);
        if (m_next) {
            reg1 = m_next->generate (bc, f, reg);
        } else {
            reg1 = -1; 
        }
        //bc->freeRegister (reg);

        //printf ("$$ CODE_USE_FIELD => %d\n", reg1);
        return reg1;
    case CODE_USE_IDX_FIELD:
        bc->add (ByteCode::ASM_FIELD_PUSH);
        reg1 = m_first->generate (bc, f, reg);
        bc->add (ByteCode::ASM_FIELD_POP);
        bc->add (ByteCode::ASM_FIELD_IDX_REG, reg1);
        bc->freeRegister (reg1);
        reg2 = m_next->generate (bc, f, reg);
        //printf ("$$ CODE_USE_FIELD => %d\n", reg1);
        return reg2;
    case CODE_INC:
    case CODE_PLUS: 
        return (generateBinary (ByteCode::ASM_ADD_REG_REG, bc, f));
    case CODE_DEC:
    case CODE_MINUS:
        return (generateBinary (ByteCode::ASM_SUB_REG_REG, bc, f));
    case CODE_MULT: 
        return (generateBinary (ByteCode::ASM_MUL_REG_REG, bc, f));
    case CODE_DIV: 
        return (generateBinary (ByteCode::ASM_DIV_REG_REG, bc, f));
    case CODE_MODULO: 
        return (generateBinary (ByteCode::ASM_MOD_REG_REG, bc, f));
    case CODE_EQUAL: 
        return (generateBinary (ByteCode::ASM_TEST_EQU, bc, f));
    case CODE_NOTEQUAL: 
        return (generateBinary (ByteCode::ASM_TEST_NEQ, bc, f));
    case CODE_GREATER: 
        return (generateBinary (ByteCode::ASM_TEST_GRT, bc, f));
    case CODE_GREATEQ: 
        return (generateBinary (ByteCode::ASM_TEST_GRE, bc, f));
    case CODE_LESSER: 
        return (generateBinary (ByteCode::ASM_TEST_LES, bc, f));
    case CODE_LESSEQ: 
        return (generateBinary (ByteCode::ASM_TEST_LEE, bc, f));
    case CODE_LOG_AND: 
        return (generateBinary (ByteCode::ASM_TEST_AND, bc, f));
    case CODE_LOG_OR: 
        return (generateBinary (ByteCode::ASM_TEST_OR, bc, f));
    case CODE_BIT_AND: 
        return (generateBinary (ByteCode::ASM_BIT_AND, bc, f));
    case CODE_BIT_OR: 
        return (generateBinary (ByteCode::ASM_BIT_OR, bc, f));
    case CODE_BIT_XOR: 
        return (generateBinary (ByteCode::ASM_BIT_XOR, bc, f));
    case CODE_BIT_INV: 
        reg1 = m_first->generate (bc, f, reg);
        bc->add (ByteCode::ASM_BIT_INV, reg1);
        return (reg1);
    case CODE_BIT_LSHIFT: 
        return (generateBinary (ByteCode::ASM_BIT_LS, bc, f));
    case CODE_BIT_RSHIFT: 
        return (generateBinary (ByteCode::ASM_BIT_RS, bc, f));
    case CODE_BIT_RRSHIFT: 
        return (generateBinary (ByteCode::ASM_BIT_RRS, bc, f));
    case CODE_TERNARY_COMP: // reg1 ? reg2 : reg3
        reg1 = m_first-> generate (bc, f, reg);
        cnt1 = bc->getLabel ();
        bc->add (ByteCode::ASM_JUMP_ZERO, reg1, cnt1);
        reg2 = m_second->generate (bc, f, reg);
        bc->add (ByteCode::ASM_MOVE_REG_REG, reg1, reg2);
        bc->freeRegister (reg2);
        cnt2 = bc->getLabel ();
        bc->add (ByteCode::ASM_JUMP, cnt2); // jump to end 
        bc->setLabel (cnt1);
        reg3 = m_third->generate (bc, f, reg);
        bc->add (ByteCode::ASM_MOVE_REG_REG, reg1, reg3);
        bc->freeRegister (reg3);
        bc->setLabel (cnt2);
        return (reg1);
    default:
        fprintf (stderr, "Code::generate: unknown code %d\n", m_type);
    }
    return (-1);
};

int Code::getOpArity (int op) {
    switch (op) {
    case CODE_NOT:
    case CODE_PRE_INC:
    case CODE_PRE_DEC:
    case CODE_BIT_INV:
    case CODE_INC:
    case CODE_DEC:
        return 1;
    case CODE_PLUS:
    case CODE_MINUS:
    case CODE_MULT:
    case CODE_DIV:
    case CODE_MODULO:
    case CODE_GREATER:
    case CODE_GREATEQ:
    case CODE_LESSER:
    case CODE_LESSEQ:
    case CODE_EQUAL:
    case CODE_NOTEQUAL:
    case CODE_LOG_AND:
    case CODE_LOG_OR:
    case CODE_BIT_AND:
    case CODE_BIT_OR:
    case CODE_BIT_XOR:
    case CODE_BIT_LSHIFT:
    case CODE_BIT_RSHIFT:
    case CODE_BIT_RRSHIFT:
        return 2;
    case CODE_TERNARY_COMP:
        return 3;
    default:
        fprintf (stderr, "Code::getOpArity: unknown code %d\n", op);
    case CODE_ERROR:
        return 0;
    }
}

int Code::getOpPrecedence (int op) {
    switch (op) {
    case CODE_INC:
    case CODE_DEC:
        return 12;
    case CODE_PRE_INC:
    case CODE_PRE_DEC:
    case CODE_BIT_INV:
    case CODE_NOT:
        return 11;
    case CODE_MULT:
    case CODE_DIV:
    case CODE_MODULO:
        return 10;
    case CODE_PLUS:
    case CODE_MINUS:
        return 9;
    case CODE_BIT_LSHIFT:
    case CODE_BIT_RSHIFT:
    case CODE_BIT_RRSHIFT:
        return 8;
    case CODE_GREATER:
    case CODE_GREATEQ:
    case CODE_LESSER:
    case CODE_LESSEQ:
        return 7;
    case CODE_EQUAL:
    case CODE_NOTEQUAL:
        return 6;
    case CODE_BIT_AND:
        return 5;
    case CODE_BIT_XOR:
        return 4;
    case CODE_BIT_OR:
        return 3;
    case CODE_LOG_AND:
        return 2;
    case CODE_LOG_OR:
        return 1;
    case CODE_TERNARY_COMP:
        return 0;
    default:
        fprintf (stderr, "Code::getOpPrecedence: unknown code %d\n", op);
    case CODE_ERROR:
        return 0;
    }
}

bool Code::isOpRightAssociative (int op) {
    switch (op) {
    case CODE_PRE_INC:
    case CODE_PRE_DEC:
    case CODE_BIT_INV:
    case CODE_NOT:
    case CODE_TERNARY_COMP:
        return true;
    }
    return false;
}

