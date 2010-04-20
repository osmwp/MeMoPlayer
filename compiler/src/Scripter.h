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

# define INITIALIZE_ID 0

#include "Code.h"

class Scene; 

class ByteCode;

class Var {
public:
    char * m_name;
    int m_index, m_level;
    Var * m_next;

    Var (char * name, int level, int index, Var * next) {
        m_name = name;
        m_level = level;
        m_index = index;
        m_next = next;
    }

    void setIndex (int i) { m_index = i; }

    int getIndex () { return m_index; }

    Var * find (char * name) {
        if (strcmp (name, m_name) == 0) {
            return (this);
        } else if (m_next) {
            return m_next->find (name);
        } else {
            return (NULL);
        }
    }

    void print  (bool first = true) {
        if (m_next) {
            first = false;
            m_next->print (first);
        } else {
            first = true;
        }
        if (m_level < 1) {
            printf ("%s%s", first ? "": ", ", m_name);
            first = false;
        }
    }

    Var * purgeAll (int level, ByteCode * bc);

    Var * remove (Var * v) {
        if (v == this) {
            return m_next;
        } else {
            m_next = m_next->remove (v);
        }
    }
};

class Code;
class ByteCode;

class Function {
    char * m_name;
    Var * m_vars;
    int m_nbVars;
    Node * m_node;
    Code * m_code;
    int m_len; // length of the bytecode
    unsigned char *  m_data; // the bytecode itself
    int m_counter;
    bool m_inLoop;
    int m_switchLevel;
public:
    int m_blockLevel;
public:
    Function (Node * node);

    void setByteCode (int len, unsigned char * data) {
        m_len = len;
        m_data = data;
    }

    unsigned char * getByteCode (int & len) {
        len = m_len;
        return m_data;
    }

    int getCounter () { return m_counter++; }

    // Create a new var accoding to its name (may shadow an exiting one)
    Var * addVar (char * name, int level, int index);

    // remove a var accoding to its name
    void removeVar (char * name);

    // remove all vars accoding to their level
    void removeVars (int level, ByteCode * bc);

    // return an existing Var (accoding to its name) or NULL
    Var * findVar (char * name);
 

    void printVars ();
 
    // parse the whole function
    Code * parse (Tokenizer * t, bool verbose);

    // return the name of the parsed function
    char * getName () { return m_name; }

    // init the parameters whith register indexes
    void setParamsIndex (ByteCode * bc);

    // allocate a index to this function (called after parsing)
    int allocIndex ();

private:
    // parse in between { and }
    Code * parseBlock (Tokenizer * t);

    Code * parseLoopBlock (Tokenizer * t);

    // parse an instruction 
    // if checkSemi = false ignore the trailing ';' (for third part of 'for')
    Code * parseInstr (Tokenizer * t, bool checkSemi = true);

    // parse an identifier ???
    Code * parseIdent (Tokenizer * t, char * token);
    
    Code * parseVarDeclaration (Tokenizer * t, bool hasVar);

    Code * parseFor (Tokenizer * t);
    
    Code * parseSwitch (Tokenizer * t);
    
    Code * parseSwitchLabel (Tokenizer * t, char * s, int type, Code * next);

    Code * parseReturn (Tokenizer * t);

    // parse an affectation symbol like =,  +=, -=, /=, *=,
    // if the affectation is also an operation the code is returned and self is set to true
    int parseAssign (Tokenizer * t, bool & self);

    // parse an operation like +, -, /, *, %, >>, <<, 
    int parseOperation (Tokenizer * t, bool pushBack=false);

    // check the next operator and get its precedence
    int checkOperation (Tokenizer * t, int & arity, int & precedence);
    int checkOperation (Tokenizer * t, int & arity, int & precedence, bool & rightAssocitive);

    Code * selfAssign (int operation, Code * self, Code * value, bool returnValue); 

    // parse a litteral value or a var name (including field access)
    Code * parseVarOrVal (Tokenizer * t);

    // parse the recepter of an expresion like i in i = 3 
    Code * parseLValue (Tokenizer * t, char * token);

    // parse an assignation like i = (2*j)+3. This is LValue = expression
    Code * parseAssign (Tokenizer * t);

    // parse a test expression with enclosing parents like (i == 3)
    Code * parseTest (Tokenizer * t);

    // parse a computational expression like (2*i)+3
    Code * parseExpr (Tokenizer * t, int min_precedence=0);
    
    // recursive helper for parseExpr to handle operators precedence
    Code * parseExprRec (Tokenizer * t, Code * lhs, int min_precedence);
    
    // parse a unary expr like explicity parenthesis ( expr ) or unary ops like ++a, !b or ~c
    Code * parseUnaryExpr (Tokenizer * t);

    // parse a pre operator expr like ++x or --c
    Code * parsePreOperator (Tokenizer * t, bool returnValue);

    // parse call to an external method like Browser.print("hello world");
    //Code * parseExternCall (int objID, Tokenizer * t);

    // parse call to an external method like Date.getDay ();
    Code * parseExternFunc (int objID, Tokenizer * t);

    // parse call to an internal function like foo (bar, "hello world");
    //Code * parseInternCall (char * funcName, Tokenizer * t);

    // parse call to an internal functio like foo (bar, "hello world");
    Code * parseInternFunc (char * funcName, Tokenizer * t);

    // parse a list of parameters like '(a, b+2)'
    Code * parseParams (Tokenizer*t);

    // parse a field access, codeVar and codeField are setter or getter opcodes
    Code * parseFieldAccess (Tokenizer * t, char * token, int codeVar, int codeField);

    // return the node field if defined, NULL otherwize
    Field * findField (char * name);
};


class Scripter {
    Tokenizer * m_tokenizer;
    char * m_code;
    Node * m_node;
    int m_maxRegisters;
    IntTable m_intTable;
    StringTable m_stringTable;
    unsigned char m_totalData [64*1024];
    int m_totalLen;
    bool m_verbose;
public: 
    Scripter (Node * node, Tokenizer * t, FILE * fp, bool verbose);
private:
    bool getFunction ();
    void writeData (unsigned char * data, int len);
};
 
