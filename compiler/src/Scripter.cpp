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

# include "stdio.h"
# include "stdlib.h"
# include "string.h"
# include "Tokenizer.h"
# include "Types.h"
# include "Code.h"
# include "Scripter.h"
# include "ExternCalls.inc"

extern FILE * myStderr;

void ensure (char c, const char * msg, Tokenizer * t) {
    if (t->check (c) == false) {
        fprintf (myStderr, msg, t->getFile(), t->getLine ());
        exit (1);
    }
}

class NameLink {
protected:
    char * m_name;
    char * m_oldName; // optionnal deprecated function name
    int m_index;
    NameLink * m_next;
    
public: 

    NameLink (char * name, int index, NameLink * next) {
        m_name = name;
        m_oldName = NULL;
        m_index = index;
        m_next = next;
    }

    NameLink (char * name, char * oldName, int index, NameLink * next) {
        m_name = name;
        m_oldName = oldName;
        m_index = index;
        m_next = next;
    }
    virtual ~NameLink () {}

    NameLink * find (char * name) {
        if (strcmp (name, m_name) == 0) {
            return this;
        }
        if (m_oldName && strcmp (name, m_oldName) == 0) {
            return this;
        }
        if (m_next != NULL) {
            return m_next->find (name);
        }
        return NULL;
    }
    NameLink * find (int index) {
        if (index ==  m_index) {
            return this;
        }
        if (m_next != NULL) {
            return m_next->find (index);
        }
        return NULL;
    }

    int getIndex (char * name) {
        NameLink * nl = find (name);
        return nl ? nl->m_index : -1;
    }

    int getIndex () {
        return m_index;
    }

    virtual void print () {
        if (m_next) {
            m_next->print ();
        }
        fprintf (myStderr, "    %s\n", m_name);
    }
};

class ClassLink : public NameLink {
    NameLink * m_functions;
    int m_funcIndex;
    
public:
    ClassLink (char * name, int index, ClassLink * next) : NameLink (name, index, next) {
        m_funcIndex = 0;
        m_functions = NULL;
    }
    void addFunction (char * name, char * oldName = NULL) {
        m_functions = new NameLink (name, oldName, m_funcIndex++, m_functions);
    }

    int getFuncIndex (char * name) {
        return m_functions->getIndex (name);
    }

    void print () {
        if (m_next) {
            m_next->print ();
        }
        fprintf (myStderr, "%s {\n", m_name);
        m_functions->print ();
        fprintf (myStderr, "}\n");
    }
};

class ExternClasses {
    ClassLink * m_classes;
    int m_classIndex;
public:
    ExternClasses (char * filename) { 
        m_classIndex = 0;
        m_classes = NULL;
        Tokenizer t (filename, false, "ExternCalls", 1);
        char * token = t.getNextToken ();
        while (token) { // new class
            m_classes = new ClassLink (token, m_classIndex++, m_classes);
            if (t.check ('{') == false) {
                fprintf (myStderr, "Syntax error in %s line %d '{' expected\n", filename, t.getLine ());
                exit (1);
            }
            token = t.getNextToken ();
            while (token) {
                if (t.check ('|')) {
                    char * token2 = t.getNextToken ();
                    if (token2 != NULL) {
                        m_classes->addFunction (token, token2);
                    } else {
                        fprintf (myStderr, "ExternCalls.def:%d: Syntax error: alternate function name is expected after '|'\n", t.getLine ());
                        exit (1);
                    }
                } else {
                    m_classes->addFunction (token);
                }
                token = t.getNextToken ();
            }
            if (t.check ('}') == false) {
                fprintf (myStderr, "Syntax error in %s line %d '}' expected\n", filename, t.getLine ());
                exit (1);
            }
            token = t.getNextToken ();
        }
        //m_classes->print ();
    }

    int getIndex (char * className) {
        ClassLink * cl = (ClassLink*)m_classes->find (className);
        return cl ? cl->getIndex () : -1;
    }

    int getIndex (int id, char * funcName) {
        ClassLink * cl = (ClassLink*)m_classes->find (id);
        return cl ? cl->getFuncIndex (funcName) : -1;
    }
};

static ExternClasses * s_externClasses = NULL;

static int getObjectID (char * o) {
    return s_externClasses->getIndex (o);
}

static int getMethodID (int objID, char * m) {
    return s_externClasses->getIndex (objID, m);
}

Var * Var::purgeAll (int level, ByteCode * bc) {
    Var * next = m_next ? m_next->purgeAll (level, bc) : NULL;
    if (m_level >= level) {
        bc->freeRegister (m_index);
        return next;
    }
    m_next = next;
    return (this);
}

Function::Function (Node * node) {
    m_vars = NULL;
    m_nbVars = 0;
    m_code = NULL;
    m_node = node;
    m_blockLevel = 1;
    m_inLoop = false;
    m_switchLevel = 0;
}

Var * Function::addVar (char * name, int level, int index) {
    //printf (" -------------- adding var %s %d -> %d\n", name, level, index);
    //printVars ();
    m_nbVars++;
    m_vars = new Var (name, level, index, m_vars);
    //printVars ();
    //printf (" --------------\n");
    return m_vars;
}

Var * Function::findVar (char * name) {
    return m_vars ? m_vars->find (name) : NULL;
}

void Function::printVars () {
    Var * v = m_vars;
    printf (" [[ ");
    while (v) {
        printf ("'%s'/%d->%d ", v->m_name, v->m_level, v->m_index);
        v = v->m_next;
    }
    printf (" ]]\n");
}

void Function::removeVar (char * name) {
    //return m_vars ? m_vars->find (name) : NULL;
}

void Function::removeVars (int level, ByteCode * bc) {
    if (m_vars) {
        m_vars = m_vars->purgeAll (level, bc);
    }
    //return m_vars ? m_vars->find (name) : NULL;
}

Field * Function::findField (char * name) {
    if (m_node) {
        return m_node->findField (name);
    } else {
        fprintf (myStderr, "findField %s in NULL node!!!\n", name);
    }
    return (NULL);
}

Code * appendCode (Code * root, Code * val) {
    if (root) {
        root->append (val);
    } else {
        root = val;
    }
    return (root);
}

Code * Function::parseFieldAccess (Tokenizer * t, char * token, int codeVar, int codeField) {
    Code * lvalue = NULL;
    bool isArray = false;
    Var * var = m_vars ? m_vars->find (token) : NULL;
    if (var) {
        lvalue = new Code (codeVar, new Code (token));
        if (t->check ('[')) { // parse indexed access
            //fprintf (myStderr, "DBG:parseFieldAcess: got '['\n");
            lvalue = appendCode (lvalue, new Code (Code::CODE_USE_IDX_FIELD, parseExpr (t)));
            if (t->check (']') == false) {
                fprintf (myStderr, "%s:%d: JS syntax error: ']' expected\n", t->getFile(), t->getLine());
                exit (1);
            }
            isArray = true;
            //fprintf (myStderr, "DBG:parseFieldAcess: got ']'\n");
        }
        if (t->check ('.')) {
            token = t->getNextToken ();
        } else { // should be '='
            //fprintf (myStderr, "DBG: the token is final => using index 0\n");
            return appendCode (lvalue, new Code (codeField, new Code (0)));
        }
        return lvalue;
    }
    Field * lastField = NULL;
    while (true) {
        if (token == NULL) {
            fprintf (myStderr, "%s:%d: JS syntax error: field expected\n", t->getFile(), t->getLine());
            return NULL;
        }
        Field * field = NULL;
        if (lastField) {
            // shoudl check for a SFNode or a MF*
            field = lastField->findField (token);
        } else {
            field = findField (token);
        }
        if (field) { // is it a field ?
            //MCP: is it a reference to a (un)declared function ?
            if (codeField == Code::CODE_GET_FIELD && lvalue == NULL && 
                (field->m_type == Field::TYPE_ID_SFDEFINED || field->m_type == Field::TYPE_ID_SFTMP)) {
                return new Code (field->m_number);
            }
            //fprintf (myStderr, "DBG:parseFieldAcess %s: got field %d\n", token, field->m_number);
            lvalue = appendCode (lvalue, new Code (Code::CODE_USE_FIELD, new Code (field->m_number)));
            lastField = field;
        } else {
            if (lastField != NULL) {
              int index = lastField->findIndex (token, isArray);
              //fprintf (myStderr, "DBG:parseFieldAcess: %s.%s -> %d\n", lastField->m_name, token, index);
              if (index < 0) {
                  fprintf (myStderr, "%s:%d: JS syntax error: unknown field: %s\n", t->getFile(), t->getLine(), token);
                  exit (1);
              }
              return appendCode (lvalue, new Code (codeField, new Code (index)));
            } else if (codeField == Code::CODE_GET_FIELD && lvalue == NULL) {
                //MCP: token is not a field or var but it might be a reference to a not-yet-defined function !
                Field * field = m_node->addField (strdup ("SFTmp"), token, true);
                field->m_lineNum = t->getLine ();
                return new Code (field->m_number);
            } else {
                fprintf (myStderr, "%s:%d: JS syntax error: unknown field %s\n", t->getFile(), t->getLine (), token);
                exit (1);
            }
        }
        if (t->check ('[')) { // parse indexed access
            //fprintf (myStderr, "DBG:parseFieldAcess: got '['\n");
            lvalue = appendCode (lvalue, new Code (Code::CODE_USE_IDX_FIELD, parseExpr (t)));
            if (t->check (']') == false) {
                fprintf (myStderr, "%s:%d: JS syntax error: ']' expected\n", t->getFile(), t->getLine());
                exit (1);
            }
            isArray = true;
            //fprintf (myStderr, "DBG:parseFieldAcess: got ']'\n");
        }
        if (t->check ('.')) {
            token = t->getNextToken ();
        } else { // should be '='
            int index  = 0;
            if (isArray == false && field != NULL && field->isMFField()) {
                index = 254; // see java Field.OBJECT_IDX: we want to use teh object, not the first element
            }
            //fprintf (myStderr, "DBG: the token is final => using index %d\n", index);
            return appendCode (lvalue, new Code (codeField, new Code (index)));
        }
    }
    return lvalue;
}

Code * Function::parseLValue (Tokenizer * t, char * token) {
    Code * tmp = parseFieldAccess (t, token, Code::CODE_SET_VAR, Code::CODE_SET_FIELD);
    if (tmp == NULL) {
        fprintf (myStderr, "%s:%d: JS syntax error: parsing lvalue\n", t->getFile(), t->getLine());
    }
    return tmp;
}

Code * Function::parseIdent (Tokenizer * t, char * token) {
    Code * tmp;
    if ( t->check ('(', true)) {
        //fprintf (myStderr, "parsing internal func call %s\n", token);
        tmp = parseInternFunc (token, t);
    } else  {
        tmp = parseFieldAccess (t, token, Code::CODE_GET_VAR, Code::CODE_GET_FIELD);
    }
    if (tmp == NULL) {
        fprintf (myStderr, "%s:%d: JS syntax error: parsing identifier\n", t->getFile(), t->getLine());
    }
    return tmp;
}

Code * Function::parseAssign (Tokenizer * t) {
    // cases are :
    // lvalue = expr;
    // if (test) { expr; } [ else { expr }©∂

/*
  Code * lvalue = parseLValue (t, token);
  if (t->check ('=') == false) {
  return (NULL);
  }
  Code * expr = parseExpr (t);
  return (new Code (Code::ASSIGN, lvalue, expr));
*/
    return (NULL);
}

Code * Function::parsePreOperator (Tokenizer * t) {
    t->skipSpace ();
    char c = t->GETC ();
    if ((c == '-' || c == '+') && t->check (c)) {
        char * s = t->getNextToken ();
        if (s != NULL) {
            Code * lvalue = parseLValue (t, s);
            if (lvalue != NULL) {
                int operation = c == '-' ? Code::CODE_MINUS : Code::CODE_PLUS;
                return selfAssign (operation, lvalue, new Code ((int)1));
            }
        }
        fprintf (myStderr, "%s:%d: JS syntax error: Pre operator (%c%cX) must only be followed by a variable or a field.\n", t->getFile(), t->getLine (), c, c);
        exit (1);
    }
    t->UNGETC (c);
    return NULL;
}

Code * Function::selfAssign (int operation, Code * self, Code * value) {
    Code * compute = new Code (operation, self->cloneInvertAccess(), value);
    return new Code (Code::CODE_ASSIGN, self, compute);
}

int Function::parseAssign (Tokenizer * t, bool & self) {
    self = false;
    if (t->check('+')) {
        if ( (self = t->CHECK ('=')) == true) {
            return (Code::CODE_PLUS);
        } else {
            t->UNGETC ('+');
        }
    } else if (t->check('-')) {
        if ( (self = t->CHECK ('=')) == true) {
            return (Code::CODE_MINUS);
        } else {
            t->UNGETC ('-');
        }
    } else if (t->check('/')) {
        if ( (self = t->CHECK ('=')) == true) {
            return (Code::CODE_DIV);
        } else {
            t->UNGETC ('/');
        }
    } else if (t->check('*')) {
        if ( (self = t->CHECK ('=')) == true) {
            return (Code::CODE_MULT);
        } else {
            t->UNGETC ('*');
        }
    } else if (t->check('%')) {
        if ( (self = t->CHECK ('=')) == true) {
            return (Code::CODE_MODULO);
        } else {
            t->UNGETC ('%');
        }
    } else if (t->check('&')) {
        if ( (self = t->CHECK ('=')) == true) {
            return (Code::CODE_BIT_AND);
        } else {
            t->UNGETC ('&');
        }
    } else if (t->check('|')) {
        if ( (self = t->CHECK ('=')) == true) {
            return (Code::CODE_BIT_OR);
        } else {
            t->UNGETC ('|');
        }
    } else if (t->check('^')) {
        if ( (self = t->CHECK ('=')) == true) {
            return (Code::CODE_BIT_XOR);
        } else {
            t->UNGETC ('^');
        }
    } else if (t->check('<')) {
        if (t->check('<')) {
            if ( (self = t->CHECK ('=')) == true) {
                return (Code::CODE_BIT_LSHIFT);
            } else {
                t->UNGETC ('<');
            }
        } else {
            t->UNGETC ('<');
        }
    } else if (t->check('>')) {
        if (t->check('>')) {
            if (t->check('>')) {
                if ( (self = t->CHECK ('=')) == true) {
                    return (Code::CODE_BIT_RRSHIFT);
                } else {
                    t->UNGETC ('>');
                }
            } else if ( (self = t->CHECK ('=')) == true) {
                return (Code::CODE_BIT_RSHIFT);
            } else {
                t->UNGETC ('>');
            }
        } else {
          t->UNGETC ('>');
        }
    } else if (t->check('=')) {
        return (Code::CODE_ASSIGN);
    }
    return (Code::CODE_ERROR);
}

int Function::checkOperation (Tokenizer * t, int & arity, int & precedence) {
  int op = parseOperation (t, true);
  arity = Code::getOpArity (op);
  precedence = Code::getOpPrecedence (op);
  return op;
}

int Function::checkOperation (Tokenizer * t, int & arity, int & precedence, bool & rightAssociative) {
  int op = checkOperation (t, arity, precedence);
  rightAssociative = Code::isOpRightAssociative (op);
  return op;
}

int Function::parseOperation (Tokenizer * t, bool pushBack) {
    int op = Code::CODE_ERROR;
    t->skipSpace ();
    char c = t->GETC ();
    switch (c) {
    case '+':
        op = t->CHECK ('+', pushBack) ? Code::CODE_INC : Code::CODE_PLUS;
        break;
    case '-':
        op = t->CHECK ('-', pushBack) ? Code::CODE_DEC : Code::CODE_MINUS;
        break;
    case '/':
        op = Code::CODE_DIV;
        break;
    case '*':
        op = Code::CODE_MULT;
        break;
    case '%':
        op = Code::CODE_MODULO;
        break;
    case '&':
        op = t->CHECK ('&', pushBack) ? Code::CODE_LOG_AND : Code::CODE_BIT_AND;
        break;
    case '|':
        op = t->CHECK ('|', pushBack) ? Code::CODE_LOG_OR : Code::CODE_BIT_OR;
        break;
    case '=':
        if (t->CHECK ('=', pushBack)) {
            op = Code::CODE_EQUAL;
        }
        break;
    case '!':
        if (t->CHECK ('=', pushBack)) {
            op = Code::CODE_NOTEQUAL;
        }
        break;
    case '<':
        if (t->CHECK ('=', pushBack)) {
            op = Code::CODE_LESSEQ;
        } else if (t->CHECK ('<', pushBack)) {
        	  op = Code::CODE_BIT_LSHIFT;
        } else {
            op = Code::CODE_LESSER;
        }
        break;
    case '>':
        if (t->CHECK ('=', pushBack)) {
            op = Code::CODE_GREATEQ;
        } else if (t->CHECK ('>')) {
            op = t->CHECK ('>', pushBack) ? Code::CODE_BIT_RRSHIFT : Code::CODE_BIT_RSHIFT;
            t->UNGETC ('>');
        } else {
            op = Code::CODE_GREATER;
        }
        break;
    case '^':
        op = Code::CODE_BIT_XOR;
        break;
    case '?': 
        op = Code::CODE_TERNARY_COMP;
        break;
    }
    if (pushBack || op == Code::CODE_ERROR) {
        t->UNGETC (c);
    }
    return op;
}

static bool checkInside (Tokenizer * t, const char * s) {
    while (*s) {
        if (t->check (*s, true)) {
            return true;
        }
        s++;
    }
    return (false);
}

Code * Function::parseExpr (Tokenizer * t, int min_precedence) {
    return parseExprRec (t, parseUnaryExpr (t), min_precedence);
}

Code * Function::parseExprRec (Tokenizer * t, Code * left, int min_precedence) {
    int arity = 0, precedence = 0;
    Code * right;
    int op = checkOperation (t, arity, precedence); // check op and pushback
    while (arity == 2 && precedence >= min_precedence) {
        parseOperation (t); // eat operator
        right = parseUnaryExpr (t);
        // Lookahead for an operator with higher precedence 
        bool rightAssociative = false;
        int lookaheadPrec = 0;
        int lookahead = checkOperation (t, arity, lookaheadPrec, rightAssociative); // check op and pushback
        while ((arity == 2 && lookaheadPrec > precedence) ||
            (rightAssociative && lookaheadPrec == precedence)) {
            right = parseExprRec (t, right, lookaheadPrec);
            lookahead = checkOperation (t, arity, lookaheadPrec, rightAssociative); // check op and pushback
        }
        left = new Code (op, left, right);
        op = checkOperation (t, arity, precedence); // check op and pushback
    }
    if (arity == 1) {
        //TODO: support unary operators
        fprintf (myStderr, "%s:%d: JS syntax error : unary operator unsupported in this expression.\n", t->getFile(), t->getLine ());
        exit (1);
        //parseOperation (t); // eat unary operator
        //return new Code (op, left);
    } else if (arity == 3) { // ? : operator
        parseOperation (t); // eat ? operator
        right = parseExpr (t, precedence);
        if (t->check (':')) {
            return new Code (op, left, right, parseExpr (t, precedence));
        } else {
            fprintf (myStderr, "%s:%d: JS syntax error: missing ':' after '?' for ternary expression.\n", t->getFile(), t->getLine ());
            exit (1);
        }
    }
    return left;
}

Code * Function::parseUnaryExpr (Tokenizer * t) {
    // cases are :
    // unaryOp expr
    // ( expr )
    // litteral
    // var
    Code * code;
    if ((code = parsePreOperator (t)) != NULL) { // pre inc/decrement operator
      return code;
    }
    if (t->check ('-')) { // unary minus operator
        return new Code (Code::CODE_MULT, new Code (-1), parseUnaryExpr (t));
    }
    if (t->check ('~')) { // unary ~ bit operator
        return new Code (Code::CODE_BIT_INV, parseUnaryExpr (t));
    }
    if (t->check ('!')) { // unary ! not operator
        return new Code (Code::CODE_EQUAL, parseUnaryExpr (t), new Code(0));
    }
    if (t->check ('(')) {
        Code * expr = parseExpr (t);
        if (t->check (')') == false) {
            fprintf (myStderr, "%s:%d: JS syntax error: missing ')'\n", t->getFile(), t->getLine ());
            exit (1);
        }
        return expr;
    } else {
        return parseVarOrVal (t);
    }
}

Code * Function::parseTest (Tokenizer * t) {
    // ( expr )
    Code * code = NULL;
    if (t->check ('(') == false) {
        fprintf (myStderr, "%s:%d: JS syntax error: missing '('\n", t->getFile(), t->getLine ());
        return (NULL);
    }
    code = parseExpr (t);
    if (t->check (')') == false) {
        fprintf (myStderr, "%s%d: JS syntax error: missing ')'\n", t->getFile(), t->getLine ());
        return (NULL);
    }
    return code;
}

Code * Function::parseVarOrVal (Tokenizer * t) {
    // cases are :
    // litteral
    // var

    char * s  = t->getNextString ();

    if (s) {
        //fprintf (myStderr, "DBG: got String '%s'\n", s);
        return new Code (s, true);
    }
    s = t->getNextToken ();
    if (s) {
        if (strcmp (s, "true") == 0) {
            return new Code (1);
        } else if (strcmp (s, "false") == 0 || strcmp (s, "null") == 0) {
            return new Code (0);
        }
        int objID = getObjectID (s);
        if (objID > -1) {
            return parseExternFunc (objID, t);
        }
        //fprintf (myStderr, "DBG: got ident '%s'\n", s);
        return parseIdent (t, s);
    }
    bool intFlag = false, isNumber;
    float f = t->getNextFloat (&isNumber, &intFlag);
    if (isNumber) {
        if (intFlag) {
            //fprintf (myStderr, "DBG: got int '%d'\n", int (f));
            return new Code (int (f));
        } else {
            //fprintf (myStderr, "DBG: got float '%g'\n", f);
            return new Code (f);
        }
    }

    return (NULL);
}

Code * Function::parseParams (Tokenizer*t) {
    if (t->check ('(') == false) {
        fprintf (myStderr, "%s:%d: JS syntax error: '(' expecteds\n", t->getFile(), t->getLine ());
        exit (1);
    }
    Code * code = NULL;
    Code * tmp = parseExpr (t);
    if (tmp) {
        code = new Code (Code::CODE_PARAM, tmp);
        while (t->check (',')) {
            tmp = parseExpr (t);
            if (tmp == NULL) {
                fprintf (myStderr, "%s:%d: JS syntax error: expression expected in call\n", t->getFile(), t->getLine ());
                exit (1);
            }
            code->append (new Code (Code::CODE_PARAM, tmp));
        }
    }
    if (t->check (')') == false) {
        fprintf (myStderr, "%s:%d: JS syntax error: ')' expecteds\n", t->getFile(), t->getLine ());
        exit (1);
    }
    //fprintf (myStderr, "DBG: end of params\n");
    return (code);
}

// Code * Function::parseExternCall (int objID, Tokenizer * t) {
//     Code * tmp = parseExternFunc (objID, t);
//     if (t->check (';') == false) {
//         fprintf (myStderr, "JS syntax error line %d: missing ';' at end of method call\n", t->getLine ());
//         exit (1);
//     }
//     return (tmp);
// }

Code * Function::parseExternFunc (int objID, Tokenizer * t) {
    //fprintf (myStderr, "DBG: got static object %d\n", objID);
    if (t->check ('.') == false) {
        fprintf (myStderr, "%s:%d: JS syntax error: missing '.'\n", t->getFile(), t->getLine ());
        exit (1);
    }
    char * s = t->getNextToken ();
    if (s == NULL) {
        fprintf (myStderr, "%s:%d: JS syntax error: missing method after '.' \n", t->getFile(), t->getLine ());
        exit (1);
    }
    int methodID = getMethodID (objID, s);
    if (methodID < 0) {
        fprintf (myStderr, "%s:%d: JS syntax error: unknown  method %s\n", t->getFile(), t->getLine (), s);
        exit (1);
    }
    //fprintf (myStderr, "DBG: got static call %d.%d \n", objID, methodID);
    if (t->check ('(', true) == true) {
        return new Code (Code::CODE_CALL_STATIC, new Code (objID), new Code(methodID), parseParams (t));
    } else {
        return new Code (Code::CODE_CALL_STATIC, new Code (objID), new Code(methodID), NULL);
    }
}

// Code * Function::parseInternCall (char * funcName, Tokenizer * t) {
//     Code * tmp = parseInternFunc (funcName, t);
//     if (t->check (';') == false) {
//         fprintf (myStderr, "JS syntax error line %d: missing ';' at end of function call\n", t->getLine ());
//         exit (1);
//     }
//     return (tmp);
// }

Code * Function::parseInternFunc (char * funcName, Tokenizer * t) {
    //fprintf (myStderr, "DBG %d: got function name %s\n", __LINE__, funcName);
    //fprintf (myStderr, "DBG: got static call %d.%d \n", objID, methodID);
    int funcId = m_node->findFieldIdx (funcName);
    if (funcId == -1) {
        if (strcmp (funcName, "initialize") == 0) {
            funcId = INITIALIZE_ID;
        } else {
            Field * field = m_node->addField (strdup ("SFTmp"), funcName, true);
            funcId = field->m_number;
            field->m_lineNum = t->getLine ();
        }
        //fprintf (myStderr, "DBG %d: got function id %d for name %s\n", __LINE__, funcId, funcName);
    }
    Code * tmp = new Code (Code::CODE_CALL_FUNCTION, new Code (funcId), parseParams (t));
    return (tmp);
}


Code * Function::parseFor (Tokenizer * t) {
    m_blockLevel++;
    ensure ('(', "%s:%d: JS syntax error: '(' expected after 'for'\n", t);
    Code * init = parseInstr (t); // got ';' already parsed
    Code * test = parseExpr (t);
    ensure (';', "%s:%d: JS syntax error: ';' expected after test part of 'for' header\n", t);
    Code * post = parseInstr (t, false);
    ensure (')', "%s:%d: JS syntax error: ')' expected after 'for' header\n", t);
    Code * block = parseLoopBlock (t);
    m_blockLevel--;
    init->append (new Code (Code::CODE_FOR, test, block, post));
    return new Code (Code::CODE_BLOCK, init);
}

Code * Function::parseSwitch (Tokenizer * t) {
    Code * test = parseTest (t);
    if (t->check ('{', true) == false) {
        fprintf (myStderr, "%s:%d: JS syntax error 'switch' must be followed by a block eg. switch (condition) { some code }\n", t->getFile(), t->getLine());
        exit (1);
    }
    int switchLevel = m_switchLevel;
    m_switchLevel = m_blockLevel + 1;
    Code * block = parseBlock (t);
    m_switchLevel = switchLevel;
    return new Code (Code::CODE_SWITCH, test, block);
}

Code * Function::parseSwitchLabel (Tokenizer * t, char * s, int type, Code * next) {
    if (m_switchLevel != m_blockLevel) {
        fprintf (myStderr, "%s:%d: JS syntax error '%s' keyword is only allowed in 'switch' blocks.\n", t->getFile(), t->getLine(), s);
        exit (1);
    }
    if (type == Code::CODE_CASE && next == NULL) {
        fprintf (myStderr, "%s:%d: JS syntax error while parsing expression associated to '%s' within 'switch' blocks.\n", t->getFile(), t->getLine(), s);
        exit (1);
    }
    if (t->check (':') == false) {
        fprintf (myStderr, "%s:%d: JS syntax error: ':' expected after '%s' keyword\n", t->getFile(), t->getLine (), s);
        exit (1);
    }
    return new Code (type, next);
}

Code * Function::parseReturn (Tokenizer * t) {
    bool parent = t->check ('(');
    Code * tmp = parseExpr (t);
    if (parent) {
        ensure (')', "%s:%d: JS syntax error: ')' expected after 'return'\n", t);
    }
    ensure (';', "%s:%d: JS syntax error: ';' expected to end 'return'\n", t);
    return new Code (Code::CODE_RETURN, tmp);
}

Code * Function::parseVarDeclaration (Tokenizer * t) {
    //fprintf (myStderr, "DBG: got token VAR\n");
    char * varName = t->getNextToken ();
    if (varName == NULL) {
        fprintf (myStderr, "%s:%d: JS syntax error: variable name expected\n", t->getFile(), t->getLine());
        exit (1);
    }
    //fprintf (myStderr, "DBG: got var name %s\n", varName);
    m_vars = new Var (varName, m_blockLevel, -1, m_vars);
    Code * value = NULL;
    if (t->check ('=')) {
        value = parseExpr (t);
    }
    return new Code (Code::CODE_NEW_VAR, new Code (varName), value);
}

Code * Function::parseInstr (Tokenizer * t, bool checkSemi) {
    // var name = expr;
    // name = expr;
    // name.field = expr;
    // Object.method (expr);
    // if (test) { instr; } [else { instr; } ]
    if (checkInside (t, ";)}")) {
        if (checkSemi) { t->check (';'); }
        return new Code (Code::CODE_NOP, NULL);
    }
    Code * tmp = NULL;
    char * s = t->getNextToken ();
    if (s) {
        int objID = getObjectID (s);
        if (objID > -1) {
            tmp = parseExternFunc (objID, t);
            if (checkSemi) {
                ensure (';', "%s:%d: JS syntax error: missing ';' to end external method call\n", t);
            }
            return (tmp);
        } else if (strcmp (s, "var") == 0) { // var declaration
            tmp = parseVarDeclaration (t);
            if (checkSemi) {
                ensure (';', "%s:%d: JS syntax error: missing ';' to end var declaration\n", t);
            }
            return (tmp);
        } else if (strcmp (s, "if") == 0) {
            Code * tmp = new Code (Code::CODE_IF, parseTest (t), parseBlock (t));
            char * token = t->getNextToken ();
            if (token) {
                if (strcmp (token, "else") == 0) {
                    tmp->setThird (parseBlock (t));
                } else {
                    t->push (token);
                }
            }
            return (tmp);
        } else if (strcmp (s, "while") == 0) {
            tmp = new Code (Code::CODE_WHILE, parseTest (t), parseLoopBlock (t));
            return (tmp);
        } else if (strcmp (s, "for") == 0) {
            return parseFor (t);
        } else if (strcmp (s, "return") == 0) {
            return parseReturn (t);
        } else if (strcmp (s, "continue") == 0) {
            if (m_inLoop) {
                ensure (';', "%s:%d: JS syntax error: ';' expected after 'continue'\n", t);
                return new Code (Code::CODE_CONTINUE, NULL);
            }
            fprintf (myStderr, "%s:%d: JS syntax error: 'continue' is only allowed in 'while' and 'for' statements.\n", t->getFile(), t->getLine());
            exit (1);
        } else if (strcmp (s, "break") == 0) {
            if (m_inLoop || m_switchLevel > 0) {
                ensure (';', "%s:%d: JS syntax error: ';' expected after 'break'\n", t);
                return new Code (Code::CODE_BREAK, NULL);
            }
            fprintf (myStderr, "%s:%d: JS syntax error: 'break' is only allowed in 'while', 'for' and 'switch' statements.\n", t->getFile(), t->getLine());
            exit (1);
        } else if (strcmp (s, "switch") == 0 && t->check ('(', true)) {
            return parseSwitch (t);
        } else if (strcmp (s, "case") == 0) {
            return parseSwitchLabel (t, s, Code::CODE_CASE, parseExpr (t));
        } else if (strcmp (s, "default") == 0) {
            return parseSwitchLabel (t, s, Code::CODE_DEFAULT, NULL);
        } else if (t->check ('(', true)) { // function call
            Code *tmp = parseInternFunc (s, t);
            if (checkSemi) {
                ensure (';', "%s:%d: JS syntax error: missing ';' to end function call\n", t);
            }
            return (tmp);
        } else { // instruction
            Code * lvalue = parseLValue (t, s);
            bool self = false;
            int operation = parseAssign (t, self);
            if (operation == Code::CODE_ASSIGN || self == true) {
                if (self) {
                    tmp = selfAssign (operation, lvalue, parseExpr (t));
                } else {
                    tmp = new Code (Code::CODE_ASSIGN, lvalue, parseExpr (t));
                }
                if (checkSemi) {
                    ensure (';', "%s:%d: JS syntax error: missing ';' to end instruction\n", t);
                }
                return tmp;
            }
            operation = parseOperation (t);
            if (Code::getOpArity (operation) == 1) { // ++ or --
                tmp = selfAssign (operation, lvalue, new Code ((int)1));
                if (checkSemi) {
                    ensure (';', "%s:%d: JS syntax error: missing ';' to end instruction\n", t);
                }
                return (tmp);
            }
            fprintf (myStderr, "%s:%d: JS syntax error (assignement expected)\n", t->getFile(), t->getLine());
        }
    } else { // not a token, maybe a pre operator followed by a field or var (eg, ++myVar) ?
        Code * code = parsePreOperator (t);
        if (code != NULL) { // pre inc/decrement operator
            if (checkSemi) {
                ensure (';', "%s:%d: JS syntax error: missing ';' to end instruction\n", t);
            }
            return code;
        }
    }
    fprintf (myStderr, "%s:%d: JS syntax error : unexpected expression\n", t->getFile(), t->getLine());
    exit (1);
}


Code * Function::parseLoopBlock (Tokenizer * t) {
    int inLoop = m_inLoop;
    m_inLoop = true;
    Code * tmp = parseBlock (t);
    m_inLoop = inLoop;
    return tmp;
}

Code * Function::parseBlock (Tokenizer * t) {
    //fprintf (myStderr, "DBg: start parsing bloc\n");
    if (t->check ('{') == false) {
        //fprintf (myStderr, "'{' expected after end of args line %d\n", t->getLine ());
        return parseInstr (t);
    }
    m_blockLevel++;
    Code * bodyCode = parseInstr (t);
    while (true) {
        if (t->check ('}')) {
            m_blockLevel--;
            return new Code (Code::CODE_BLOCK, bodyCode);
        }
        Code * tmp = parseInstr (t);
        if (tmp) {
            bodyCode->append (tmp);
        } else {
            break;
        }
    }
    m_blockLevel--;
    if (t->check ('}') == false) {
        fprintf (myStderr, "%s:%d: JS syntax error: '}' expected after end of block\n", t->getFile(), t->getLine ());
        return (false);
    }
    return new Code (Code::CODE_BLOCK, bodyCode);
}

Code * Function::parse (Tokenizer * t, bool verbose) {
    m_blockLevel = 0;
    char * token = t->getNextToken ();
    if (token == NULL) {
        if (t->_EOF() == false) {
            fprintf (myStderr, "%s:%d: WARNING: function expected (next char is '%c')\n", t->getFile(), t->getLine (), t->GETC());
        }
        return (NULL);
    }
    if (strcmp (token, "function") != 0) {
        fprintf (myStderr, "%s:%d: 'function' expected\n", t->getFile(), t->getLine ());
        exit (1);
    }
    char * funcName = t->getNextToken ();
    if (funcName == NULL) {
        fprintf (myStderr, "%s:%d: function name expected\n", t->getFile(), t->getLine ());
        exit (1);
    }
    m_name = funcName;
    //fprintf (myStderr, "DBG: got func name %s\n", funcName);
    if (t->check ('(') == false) {
        fprintf (myStderr, "%s:%d: '(' expected after function name\n", t->getFile(), t->getLine ());
        exit (1);
    }
    // parse args
    while (true) {
        char * argName = t->getNextToken ();
        if (argName == NULL) { // empty list
            break;
        }
        m_vars = new Var (argName, 0, -1, m_vars);
        if (t->check (',') == false) {
            break; // end of list
        }
    }
    if (t->check (')') == false) {
        fprintf (myStderr, "%s:%d: missing ',' or ')' in args declaration\n", t->getFile(), t->getLine ());
        exit (1);
    }
    m_code = parseBlock (t);
    if (m_code && !ByteCode::s_compat) {
        // New bytecode format: add an explicit code to mark the end of the function
        m_code->append (new Code (Code::CODE_THEEND, NULL));
    }
    //fprintf (myStderr, "Function '%s' parsed correctly\n", funcName);
    if (verbose) {
        printf ("function %s (", funcName);
        if (m_vars) { m_vars->print (); }
        printf (") ");
        if (m_code) {
            m_code->printAll (0);
        }
        printf ("\n");
    }
    return (m_code);
}

int Function::allocIndex () {
    int index = m_node->findFieldIdx (getName ());
    if (index == -1) {
        if (strcmp (getName(), "initialize") == 0) {
            index = INITIALIZE_ID;
        } else {
            m_node->addField (strdup ("SFDefined"), getName (), true);
            index = m_node->findFieldIdx (getName ());
        }
    } else {
        Field * field = m_node->findField (getName());
        if (field->m_type == Field::TYPE_ID_SFTMP) {
            field->m_type = Field::TYPE_ID_SFDEFINED;
        }
    }
    return index;
}

void setVarIndex (Var * v, ByteCode * bc) {
    if (v) {
        if (v->m_next) {
            setVarIndex (v->m_next, bc);
        }
        v->setIndex (bc->getRegister());
    }
}

void Function::setParamsIndex (ByteCode * bc) {
    setVarIndex (m_vars, bc);
}

Scripter::Scripter (Node * node, Tokenizer * t, FILE * fp, bool verbose) {
    m_tokenizer = t;  
    m_node = node;
    m_totalLen = 0;
    m_maxRegisters = 0;
    m_verbose = verbose;
    
    int len = 0, nbFunctions = 0;
    unsigned char * data;
    
    if (s_externClasses == NULL) {
        s_externClasses = new ExternClasses (externCallsDef);
    }

    if (m_tokenizer->checkToken ("javascript")) {
        if (!m_tokenizer->check (':')) {
            fprintf (myStderr, "%s:%d: Warning: the char ':' is expected after token 'javascript'\n", m_tokenizer->getFile(), m_tokenizer->getLine());
        }
    }
    
    while (getFunction ()) nbFunctions++;

    // now check all fields for remaining SFTmp, meaning that functions have been used but not defined 
    Field * field = m_node->m_field;
    while (field != NULL) {
        if (field->m_type == Field::TYPE_ID_SFTMP) {
            fprintf (stderr, "%s:%d function called but not defined, or syntax error: %s\n", 
                     m_tokenizer->getFile(), field->m_lineNum, field->m_name);
            exit (1);
        }
        field = field->m_next;
    }

    if (ByteCode::s_compat) {
        // Add a last zero byte to mark the end of functions
        m_totalData[m_totalLen++] = 0;
        // Write 255 marker (older bytecode format) and max registers
        fprintf (fp, "%c%c", 255, m_maxRegisters);
    } else {
        // Write max registers (<255)
        // 255 is reserved for detection of the older
        // bytecode format on the player side
        fprintf (fp, "%c", m_maxRegisters);
        // Write String table size
        fprintf (fp, "%c", m_stringTable.getSize ());
        // Write String table
        data = m_stringTable.generate (len);
        fwrite (data, 1, len, fp);
        free (data);
        // Write Int table size
        fprintf (fp, "%c", m_intTable.getSize ());
        // Write Int table
        data = m_intTable.generate (len);
        fwrite (data, 1, len, fp);
        free (data);
        // Write nb of functions
        fprintf (fp, "%c", nbFunctions);
    }
    // Write all functions
    fwrite (m_totalData, 1, m_totalLen, fp);
}

bool Scripter::getFunction () {
    int len = 0;
    unsigned char * data = NULL;
    Function f (m_node);
    if (m_verbose) {
        printf ("----------------------\n");
        printf ("-- Parsing function --\n");
    }
    Code * code = f.parse (m_tokenizer, m_verbose);
    if (code == NULL) {
        if (m_verbose) {
            printf ("-- Empty function --\n");
            printf ("--------------------\n");
        }
        return false;
    }
    int index = f.allocIndex ();
    if (index > 252) {
        fprintf (myStderr, "%s:%d: Scripting error: Max number of fields and methods reached in Script node ! (function %s has index > 252)\n", 
                 m_tokenizer->getFile(), m_tokenizer->getLine(), f.getName ());
        exit (1);
    }
    if (m_verbose) {
        printf ("-- Generating bytecode for function %s --\n", f.getName ());
        f.printVars ();
    }
    ByteCode bc (&m_stringTable, &m_intTable);
    f.setParamsIndex (&bc);
    f.removeVars (1, &bc);
    code->generateAll (&bc, &f);
    f.removeVars (1, &bc);
    
    //fprintf (myStderr, "Scripter.getFunction: %s has max regs = %d\n", f.getName (), bc.getMaxRegisters ());
    if (bc.getMaxRegisters () > m_maxRegisters) {
        m_maxRegisters = bc.getMaxRegisters ();
        if (m_maxRegisters > 254) { // 255 is reserved for detecting the previous bytecode format on player side
            fprintf (myStderr, "Scripting error: Function %s is using too much registers: %d (maximum 253), refactor your code !\n",
                     f.getName (), bc.getMaxRegisters ());
            exit (1);
        }
    }
    
    // Copy function index
    m_totalData[m_totalLen++] = (index+1) & 0xFF;
    // Copy function code
    data = bc.getCode (len);
    writeData (data, len);
    if (len > 0) {
        if (m_verbose) ByteCode::dump (data, len);
        free (data);
    } else {
        fprintf (myStderr, "Scripter.parseFunction: no bytecode generated!\n");
    }
    if (m_verbose) {
        f.printVars ();
        printf ("-- Bytecode generated --\n");
        printf ("------------------------\n");
    }
    return true;
}

void Scripter::writeData (unsigned char * data, int len) {
    //printf ("DBG: writeData: %d (%d)\n", data, len);
    unsigned char * s = (unsigned char *) &len;
    m_totalData[m_totalLen++] = s[3];
    m_totalData[m_totalLen++] = s[2];
    m_totalData[m_totalLen++] = s[1];
    m_totalData[m_totalLen++] = s[0];
    if (len > 0) {
        memcpy (&m_totalData[m_totalLen], data, len);
        m_totalLen += len;
    }
}

