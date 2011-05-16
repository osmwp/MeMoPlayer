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

# define ARRAY_MAX_SIZE 1024*10

class Scene; 
class Node;

class Field {
public:
    Field * m_next;
    int m_type;
    char * m_name;
    bool m_clone;
    bool m_modified;
    int m_number;
    int m_lineNum; // the line number where a SFTmp field is first defined
    char * m_fn;
    int m_line;
    bool m_isDynamic;
    int m_protoIdx;
    
    enum {
        TYPE_ID_SFBOOL = 1,
        TYPE_ID_SFCOLOR = 2,
        TYPE_ID_SFFLOAT = 3,
        TYPE_ID_SFINT32 = 4,
        TYPE_ID_SFNODE = 5,
        TYPE_ID_SFROTATION = 6,
        TYPE_ID_SFSTRING = 7,
        TYPE_ID_SFTIME = 8,
        TYPE_ID_SFVEC2F = 9,
        TYPE_ID_SFVEC3F = 10,
        TYPE_ID_MFBOOL = 11,
        TYPE_ID_MFCOLOR = 12,
        TYPE_ID_MFFLOAT = 13,
        TYPE_ID_MFINT32 = 14,
        TYPE_ID_MFNODE = 15,
        TYPE_ID_MFROTATION = 16,
        TYPE_ID_MFSTRING = 17,
        TYPE_ID_MFVEC2F = 18,
        TYPE_ID_MFVEC3F = 19,
        TYPE_ID_SFTMP = 20,
        TYPE_ID_MFFIELD = 21,
        TYPE_ID_SFDEFINED = 22,
        TYPE_ID_MFANY = 23,
    };

    enum {
        LENGTH_IDX = 255,
        OBJECT_IDX = 254,
        HEX_IDX = 253,
    };

    Field () ;
    
    Field (char * type, char * name, Field * next) ;
    
    virtual ~Field ();
    
    void copyFrom (Field * m) ;

    void setDynamic (bool isDynamic = true) { m_isDynamic = isDynamic; }
    
    Field * cloneAll () ;
    
    int typeId (char * n);
    
    void encode (FILE * fp);
    
    void print (int n, bool fieldType);
    
    virtual void printValue (int n);
    
    virtual void parseValue (Scene * scene, Tokenizer *t) {} ;
    
    virtual void encodeValue (FILE * fp) = 0;

    virtual int encodeSpecial (FILE * fp, bool verbose) { 
        if (m_next) {
            return m_next->encodeSpecial (fp, verbose);
        }
        return 0;
    }
    
    virtual Field * clone () = 0;

    virtual Field * findField (const char * name) { return NULL; }
    
    virtual int findIndex (char * name, bool isArray) { return -1; }
    
    virtual int getTypeId () = 0;

    int getCount () { return m_next != NULL ? 1 + m_next->getCount () : 1; }

    virtual int isMFField () { return 0; }
};

Field * createField (char * type, char * name, Field * next);

class NodeAssoc {
    char * m_name;
    Node * m_node;
    int m_id;
    NodeAssoc * m_next;
public:
    
    NodeAssoc (char * name, Node * node, int id, NodeAssoc * next);
    Node * findNode (const char * name);
    int findNodeID (const char * name);
    int findFieldIdx (const char * nodeName, const char * fieldName);
};

class NodeTable {
    NodeAssoc * m_root;
    int m_count;
    char * m_name;
    NodeTable * m_next;
public:
    NodeTable (char * name, NodeTable * next) { 
        m_name = name; m_next = next; m_root = NULL; m_count = 1; 
    }
    void addAssoc (char * name, Node * node) {
        m_root = new NodeAssoc (name, node, m_count++, m_root);
    }
    Node * findNode (char * name) {
        return m_root ? m_root->findNode (name) : NULL;
    }
    int findNodeID (char * name) {
        return m_root ? m_root->findNodeID (name) : -1;
    }
    int findFieldIdx (const char * nodeName, const char * fieldName) {
        return m_root ? m_root->findFieldIdx (nodeName, fieldName) : -1;
    }
    NodeTable * getSafeNext () { return m_next ? m_next : NULL; }
    char * getName () { return m_name; }
};

class Node {
public:
    char * m_name, * m_defName;
    int m_defID; // used when the Node table is not availl.
    Field * m_field;
    Node * m_next;
    Scene * m_scene; // the scene this node belongs to
    bool m_clone;
    int m_fieldNumber;
    int m_number;
    bool m_isUsed;
    static Node * s_lastTextNode; // used in parseSpecial to give this node to the child FontStyle
    Node ();
    Node (char * name, Node * next);

    virtual ~Node () {}
    
    virtual bool isProto() { return (false); }

    Node * find (char * name);

    void setScene (Scene * scene) { m_scene = scene; }

    void setDefName (char * defName);
    
    void setIsUsed (char * useName, Tokenizer * t);
    
    Field * findField (const char * name);

    virtual int findFieldIdx (const char * name);
    
    virtual Node * clone (Node * next);
    
    Field * addField (char *type, char * name, bool isDynamic);
    
    Field * addField (char *type, char * name, bool isDynamic, int fieldNumber);
    
    int getNbFields ();

    virtual void print (int n, bool fieldType = false, bool initSpaces = true);
     
    virtual void encodeValue (FILE * fp);

    void encodeScript (FILE * fp);

    void tableEntry (FILE * fp);

    virtual int encodeSpecial (FILE * fp, bool verbose);
    
    int encodeImages (const char * fieldName, FILE * fp, int total);
};

class Proto : public Node {
public:
    Scene * m_scene; // the inner scene of the proto
    Scene * m_master; // the scene this proto belongs to
    //int m_fieldNumber;
    //Proto * m_next;

    Proto ();
    Proto (Tokenizer * t, Scene * master, Proto * next);

    virtual bool isProto() { return (true); }

    void printAll (int n);
     
    void print (int n, bool fieldType = false, bool initSpaces = true);
    void encodeValue (FILE * fp);

    Node * clone (Node * next);

    int encode (FILE * fp, bool verbose);

    int findFieldIdx (const char * name);

    int getFieldId (const char *name);

    //Node * findOuterProto (char *name);
};


class Route {
public:
    int m_nodeIn, m_nodeOut;
    int m_fieldIn, m_fieldOut;
    Route * m_next;

    Route (int inNameIdx, int inFieldIdx,
	   int outNameIdx, int outFieldIdx, Route * route);
    void print (int n);
    void encodeValue (FILE * fp);
};

class Scene {
public:
    static Node * m_model;
    NodeTable * m_nodeTable;
    Node * m_root;
    Route * m_route; 
    char * m_name;
    Proto * m_proto, * m_parentProto; // a child proto and the proto this scene belongs to

    static int m_nodeNumber;

    Scene (char * name, bool verbose = false);
    
    Node * clone (char * name, Node * next);

    Node * parseNodeModel (Tokenizer * t, Node * root);
    
    void parseRoute (Tokenizer * t) ;

    void parseProto (Tokenizer * t, bool verbose);

    void parseExternProto (Tokenizer * t, bool verbose);

    Node * parseNode (Tokenizer * t, Node * root);
    
    Node * parse (char* fn, FILE * fp, bool verbose);

    Node * parse (Tokenizer * t, bool fromProto, bool verbose);
    
    void print (int n);

    int encode (FILE * fp, bool verbose);

    void dumpTable (FILE * fp);
    
    Node * parseModel (char * n);
    // association tables: name <> node
    void push (char * name) { m_nodeTable = new NodeTable (name, m_nodeTable); }
    void pop () { m_nodeTable = m_nodeTable->getSafeNext(); }
    char * getSceneName () { return m_nodeTable->getName (); }
    void addAssoc (char * name, Node * node) { m_nodeTable->addAssoc (name, node); }
    Node * findNode (char * defName) { return m_nodeTable->findNode (defName); }
    int findNodeID (char * defName) { return m_nodeTable->findNodeID (defName); }
    int findFieldIdx (const char * nodeName, const char * fieldName) { return m_nodeTable->findFieldIdx (nodeName, fieldName); }

};

class SFBool : public Field {
     bool v;
public:
    SFBool (char * type, char * name, Field * next);
    virtual void printValue (int n);
    virtual void encodeValue (FILE * fp);
    virtual void parseValue (Scene * scene, Tokenizer *t);
    virtual Field * clone ();
    int getTypeId () { return TYPE_ID_SFBOOL; }
};

class SFTmp : public Field {
public:
    SFTmp (char * type, char * name, Field * next);
    virtual void printValue (int n);
    virtual void encodeValue (FILE * fp);
    virtual void parseValue (Scene * scene, Tokenizer *t);
    virtual Field * clone ();
    int getTypeId () { return TYPE_ID_SFTMP; }
};

class SFInt32 : public Field {
     int v;
public:
    SFInt32 (char * type, char * name, Field * next);
    virtual void printValue (int n);
    virtual void encodeValue (FILE * fp);
    virtual void parseValue (Scene * scene, Tokenizer *t);
    virtual Field * clone ();
    int getTypeId () { return TYPE_ID_SFINT32; }
};

class SFFloat : public Field {
     float v;
public:
    SFFloat (char * type, char * name, Field * next);
    virtual void printValue (int n);
    virtual void encodeValue (FILE * fp);
    virtual void parseValue (Scene * scene, Tokenizer *t);
    virtual Field * clone ();
    int getTypeId () { return TYPE_ID_SFFLOAT; }
    float getValue () { return v; }
};

class SFTime : public Field {
     float v;
public:
    SFTime (char * type, char * name, Field * next);
    virtual void printValue (int n);
    virtual void encodeValue (FILE * fp);
    virtual void parseValue (Scene * scene, Tokenizer *t);
    virtual Field * clone ();
    int getTypeId () { return TYPE_ID_SFTIME; }
};

class SFVec2f : public Field {
     float x, y;
public:
    SFVec2f (char * type, char * name, Field * next);
    virtual void printValue (int n);
    virtual void encodeValue (FILE * fp);
    virtual void parseValue (Scene * scene, Tokenizer *t);
    virtual Field * clone ();
    virtual int findIndex (char * name, bool isArray);
    int getTypeId () { return TYPE_ID_SFVEC2F; }
};

class SFVec3f : public Field {
    float x, y, z;
public:
    SFVec3f (char * type, char * name, Field * next);
    virtual void printValue (int n);
    virtual void encodeValue (FILE * fp);
    virtual void parseValue (Scene * scene, Tokenizer *t);
    virtual Field * clone ();
    virtual int findIndex (char * name, bool isArray);
    int getTypeId () { return TYPE_ID_SFVEC3F; }
};

class SFRotation : public Field {
    float x, y, z, a;
public:
    SFRotation (char * type, char * name, Field * next);
    virtual void printValue (int n);
    virtual void encodeValue (FILE * fp);
    virtual void parseValue (Scene * scene, Tokenizer *t);
    virtual Field * clone ();
    virtual int findIndex (char * name, bool isArray);
    int getTypeId () { return TYPE_ID_SFROTATION; }
};

class SFString : public Field {
    char * v;
public:
    SFString (char * type, char * name, Field * next);
    ~SFString ();
    char * getValue () { return v; }
    void setValue (char * str) { v = str; }
    virtual void printValue (int n);
    virtual void encodeValue (FILE * fp);
    virtual void parseValue (Scene * scene, Tokenizer *t);
    virtual Field * clone ();
    int getTypeId () { return TYPE_ID_SFSTRING; }
};

class SFColor : public Field {
     float r, g, b;
public:
    SFColor (char * type, char * name, Field * next);
    virtual void printValue (int n);
    virtual void encodeValue (FILE * fp);
    virtual void parseValue (Scene * scene, Tokenizer *t);
    virtual Field * clone ();
    virtual int findIndex (char * name, bool isArray);
    int getTypeId () { return TYPE_ID_SFCOLOR; }
};

class SFNode : public Field {
    Node * m_node;
public:
    SFNode (char * type, char * name, Field * next);
    virtual void printValue (int n);
    virtual void encodeValue (FILE * fp);
    virtual int encodeSpecial (FILE * fp, bool verbose);
    virtual void parseValue (Scene * scene, Tokenizer *t);
    virtual Field * clone ();
    virtual Field * findField (const char * name);
    virtual int findIndex (const char * name, bool isArray);
    int getTypeId () { return TYPE_ID_SFNODE; }
};

class MFField : public Field {
protected:
     int m_size;
public:
    MFField (char * type, char * name, Field * next);
    int getSize () { return m_size; }
    void encodeSize (FILE * fp);
    virtual int findIndex (char * name, bool isArray);
    int getTypeId () { return TYPE_ID_MFFIELD; }
    virtual int isMFField () { return 1; }
};

class MFNode : public MFField {
     Node * m_node;
public:
    MFNode (char * type, char * name, Field * next);
     virtual void printValue (int n);
    virtual void encodeValue (FILE * fp);
    virtual int encodeSpecial (FILE * fp, bool verbose);
    virtual void parseValue (Scene * scene, Tokenizer *t);
    virtual Field * clone ();
    int getTypeId () { return TYPE_ID_MFNODE; }
    void setValue (Node *n, int s) { m_node = n; m_size = s;  }
};

class MFInt32 : public MFField {
    int m_array [ARRAY_MAX_SIZE];
public:
    MFInt32 (char * type, char * name, Field * next);
    virtual void printValue (int n);
    virtual void encodeValue (FILE * fp);
    virtual void parseValue (Scene * scene, Tokenizer *t);
    virtual Field * clone ();
    int getTypeId () { return TYPE_ID_MFINT32; }
};

class MFFloat : public MFField {
    float m_array [ARRAY_MAX_SIZE];
public:
    MFFloat (char * type, char * name, Field * next);
    virtual void printValue (int n);
    virtual void encodeValue (FILE * fp);
    virtual void parseValue (Scene * scene, Tokenizer *t);
    virtual Field * clone ();
    int getTypeId () { return TYPE_ID_MFFLOAT; }
};

class MFVec2f : public MFField {
    float m_array [ARRAY_MAX_SIZE*2];
public:
    MFVec2f (char * type, char * name, Field * next);
    virtual void printValue (int n);
    virtual void encodeValue (FILE * fp);
    virtual void parseValue (Scene * scene, Tokenizer *t);
    virtual Field * clone ();
    virtual int findIndex (char * name, bool isArray);
    int getTypeId () { return TYPE_ID_MFVEC2F; }
};

class MFVec3f : public MFField {
    float m_array [ARRAY_MAX_SIZE*3];
public:
    MFVec3f (char * type, char * name, Field * next);
    virtual void printValue (int n);
    virtual void encodeValue (FILE * fp);
    virtual void parseValue (Scene * scene, Tokenizer *t);
    virtual Field * clone ();
    virtual int findIndex (char * name, bool isArray);
    int getTypeId () { return TYPE_ID_MFVEC3F; }
};

class MFRotation : public MFField {
    float m_array [ARRAY_MAX_SIZE*4];
public:
    MFRotation (char * type, char * name, Field * next);
    virtual void printValue (int n);
    virtual void encodeValue (FILE * fp);
    virtual void parseValue (Scene * scene, Tokenizer *t);
    virtual Field * clone ();
    virtual int findIndex (char * name, bool isArray);
    int getTypeId () { return TYPE_ID_MFROTATION; }
};

class MFColor : public MFField {
    float m_array [ARRAY_MAX_SIZE*3];
public:
    MFColor (char * type, char * name, Field * next);
    virtual void printValue (int n);
    virtual void encodeValue (FILE * fp);
    virtual void parseValue (Scene * scene, Tokenizer *t);
    virtual Field * clone ();
    virtual int findIndex (char * name, bool isArray);
    int getTypeId () { return TYPE_ID_MFCOLOR; }
};

class MFString : public MFField {
    char * m_array [ARRAY_MAX_SIZE];
public:
    MFString (char * type, char * name, Field * next);
    char * getValue (int i);
    virtual void printValue (int n);
    virtual void encodeValue (FILE * fp);
    virtual void parseValue (Scene * scene, Tokenizer *t);
    virtual Field * clone ();
    //virtual int findIndex (char * name, bool isArray);
    int getTypeId () { return TYPE_ID_MFSTRING; }
};


class MFAny : public MFField {
    char * m_array [ARRAY_MAX_SIZE];
public:
    MFAny (char * type, char * name, Field * next);
    char * getValue (int i);
    virtual void printValue (int n);
    virtual void encodeValue (FILE * fp);
    virtual void parseValue (Scene * scene, Tokenizer *t);
    virtual Field * clone ();
    //virtual int findIndex (char * name, bool isArray);
    int getTypeId () { return TYPE_ID_MFSTRING; }
};


