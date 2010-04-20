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

# ifndef __CODE__
# define __CODE__
# include <stdio.h>

#include "CodeTables.h"

class Function;

class Register {
public:
    int m_index;
    int m_level;
    bool m_isUsed;
};

# define MAX_REGISTERS 256
# define MAX_LABELS 256

class ByteCode {
    Register m_registery [MAX_REGISTERS];
    int m_topRegister; // the last register actually used
    int m_maxRegister; // the biggest register used

    // the bytecode storage
    unsigned char m_bytecodeStorage [4096];
    unsigned char * m_bytecode;
    bool m_bunchMode;

    int m_breakLabel;
    int m_continueLabel;

    // shared accross all Function of the same Script
    StringTable * m_stringTable;
    IntTable * m_intTable;

    int m_labels[MAX_LABELS];
    int m_maxLabels; 
public:

    enum {
        ASM_ENDOFCODE = 255,
        ASM_ERROR = -1,
        ASM_NOP = 0,
        
        //ASM_ALLOC,
        //ASM_FREE,
        
        ASM_JUMP,             // label/8: goto label
        ASM_JUMP_ZERO,        // reg label/8: if val(ref) == 0 goto label 
        ASM_LABEL,            // DEPRECATED, NOT USED ANYMORE
        ASM_EXT_CALL,         // label/8 label/8 reg
        ASM_INT_CALL,         // label/8 reg
        ASM_RETURN,           // reg
        //ASM_JUMP_IFNZ,       

        ASM_LOAD_REG_INT, // reg, int: load int into reg
        ASM_LOAD_REG_FLT,
        ASM_LOAD_REG_STR,
        ASM_MOVE_REG_REG,
        //ASM_LOAD_REG_REL,
        //ASM_SAVE_REG_REL,

        ASM_FIELD_PUSH,        // push the current object and set the current object to Script object 
        ASM_FIELD_POP,         // pop the last pushed object as the current object
        ASM_FIELD_USE_INT,     // i/8: use field i
        ASM_FIELD_IDX_REG,     // i/8: use index i as an arry offset of current field for set/get access
        ASM_FIELD_GET_INT_REG, // i/8, reg: store value at index i into reg 
        ASM_FIELD_SET_INT_REG, // i/8, reg: store value of reg at index i
        
        ASM_ADD_REG_REG,
        ASM_SUB_REG_REG,
        ASM_MUL_REG_REG,
        ASM_DIV_REG_REG,
        ASM_MOD_REG_REG,
        
        ASM_TEST_EQU,
        ASM_TEST_NEQ,
        ASM_TEST_GRT,
        ASM_TEST_GRE,
        ASM_TEST_LES,
        ASM_TEST_LEE,
        ASM_TEST_AND,
        ASM_TEST_OR,
        
        ASM_BIT_AND,
        ASM_BIT_OR,
        ASM_BIT_XOR,
        ASM_BIT_INV,
        ASM_BIT_LS,
        ASM_BIT_RS,
        ASM_BIT_RRS,

        //ASM_RET,
        //ASM_PUSH_BASE,
        //ASM_POP_BASE,
    };

    ByteCode (StringTable * stringTable, IntTable * intTable);

    void addByte (int i);
    void addInt (int i);
    void addFloat (float f);
    void addString (char * s);

    void add (int opcode) ;
    void add (int opcode, int reg1);
    void add (int opcode, int reg1, int reg2);
    void add (int opcode, int reg1, int reg2, int reg3);
    void add (int opcode, int reg1, int reg2, int reg3, int reg4);
    void add (int opcode, int reg1, char * s);

    // release the register of index i in the pool
    void freeRegister (int i);

    // bunch mode: if true then all available registers will be allocated on top, returns previous mode 
    bool setRegBunchMode (bool yes);

    // get a free register and set it the corresponding level
    // calls the auxillary function above and record the max reg used
    int getRegister (int level = 0);

    // auxillary function
    int getRegister2 (int level = 0);

    // return the maximum number of registers used in a function
    int getMaxRegisters () { return m_maxRegister + 1; }

    void printRegisters ();

    // generate the java class as a collectino of final static int 
    static void generate (char * n);

    // generate the java class as a collection of final static int 
    static void print ();

    static void dump (unsigned char * data, int len);
    
    // return the current bytecode set and its length in bytes
    unsigned char * getCode (int & len);

    // return the current jumptable and its length in bytes
    unsigned char * getJumpTable (int & len);

    // Set the label at index to the current offset
    void setLabel (int index);

    // Return a new unique index for a label
    int getLabel ();

    int setBreakLabel (int labelIndex);
    int setContinueLabel (int label);
    int getBreakLabel ();
    int getContinueLabel ();

};

class Code {
public:
    enum {
        CODE_THEEND = -2,
        CODE_ERROR= -1,
        CODE_NOP = 0,       // no operation 
        CODE_ASSIGN = 1,    // =
        CODE_INT = 2,       // an interger litteral
        CODE_CHAR = 3,      // an interger litteral
        CODE_FLOAT = 4,     // a float litteral
        CODE_STRING = 5,    // a string litteral
        CODE_NAME = 6,      // a Class litteral
        CODE_PARAM,         // a function call parameter
        CODE_CALL_STATIC,   // call a static method like Browser.print 
        CODE_CALL_FUNCTION, // call an internal function
        CODE_RETURN,        // end of function and return a value
        CODE_NEW_VAR,       // create a new var
        CODE_GET_VAR,       // get the value a a var
        CODE_SET_VAR,       // set the value a a var
        CODE_USE_FIELD,     // set the current base to the field idx
        CODE_USE_IDX_FIELD, // set the current base to the field idx
        CODE_SET_FIELD,     // set the value of the current field
        CODE_GET_FIELD,     // set the value of the current field
        CODE_PLUS,          // + 
        CODE_SELFPLUS,      // += 
        CODE_INC,           // ++ 
        CODE_MINUS,         // - 
        CODE_SELFMINUS,     // -=
        CODE_DEC,           // --
        CODE_MULT,          // * 
        CODE_SELFMULT,      // *= 
        CODE_DIV,           // / 
        CODE_SELFDIV,       // /=
        CODE_MODULO,        // / 
        CODE_BLOCK,         // { }
        CODE_IF,            // if 
        CODE_WHILE,         // while
        CODE_FOR,           // for
        CODE_CONTINUE,      // continue
        CODE_BREAK,         // break
        CODE_SWITCH,        // switch
        CODE_CASE,          // case
        CODE_DEFAULT,       // default
        CODE_BIT_AND,       // &
        CODE_BIT_OR,        // |
        CODE_BIT_XOR,       // ^
        CODE_BIT_INV,       // ~
        CODE_BIT_LSHIFT,    // <<
        CODE_BIT_RSHIFT,    // >>
        CODE_BIT_RRSHIFT,   // >>>
        CODE_EQUAL,         // == 
        CODE_NOTEQUAL,      // != 
        CODE_GREATER,       // > 
        CODE_GREATEQ,       // >= 
        CODE_LESSER,        // < 
        CODE_LESSEQ,        // <= 
        CODE_LOG_AND,       // && 
        CODE_LOG_OR,        // || 
        CODE_TERNARY_COMP,  // ?:
        CODE_NOT,           // !
        CODE_PRE_INC,       // ++x
        CODE_PRE_DEC,       // --x
        CODE_ASSIGN_AND_RETURN,// = but return value
    };
    enum {
        STORE_FIELD_NUMERIC = 1,
        STORE_FIELD_FLOAT,
        STORE_FIELD_FIELD,
    };
private:
    /** the opcode type, one of the OPCODE enum value */
    int m_type;

    /** in case of name reference (is duplicated and freed internally) */
    char * m_name;

    /** in case of class name reference (is duplicated and freed internally) */
    char * m_class;

    /** in case of integer value */
    int m_ival;

    /** in case of float value */
    float m_fval;

    /* in case of a one child operation. @see m_third */
    Code * m_first;

    /* in case of a two childs operation */
    Code * m_second;

    /* in case of a three childs operation (e.g. if: test part in m_first, 
     * then part in m_second and else part in m_third.*/
    Code * m_third;

    /** the next Code to eval */
    Code * m_next;

public:
    Code (char value);
    Code (int value);
    Code (float value);
    Code (char * name, bool isString = false);
    //Code (char * name, char * className, bool isVar = false);
    Code (int op, Code * first, Code * second = NULL, Code * third = NULL);
    /** delete this code and recursively all its sibblings (m_thirst, m_second, m_third, m_next) */
    ~Code ();
    Code * cloneInvertAccess ();
    void setSecond (Code * c) { m_second = c; }
    void setThird (Code * c) { m_third = c; }
    void destroy ();
    Code * getFirst () { return m_first; }
    void printAll (int n = 0);
    void append (Code * code);
    Code * getNext ();
    int getLength ();
    bool generateAll (ByteCode * bc, Function * f);
    static int getOpArity (int op);
    static int getOpPrecedence (int op);
    static bool isOpRightAssociative (int op);
private:
    void init ();
    //void setOperation (int op, Code * first = NULL, Code * second = NULL, Code * third = NULL);
    void printAux (const char * left, const char * middle, const char * right, int level = 0);
    void print (int n = 0);
    void dump ();
    bool isTerm ();

    void setSecondToLast (Code * code);
    int generateBinary (int opcode, ByteCode * bc, Function * f);
    int generate (ByteCode * bc, Function * f, int reg);
};

# endif
