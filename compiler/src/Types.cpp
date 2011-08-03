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
# include "stdarg.h"
# include "string.h"
# include "unistd.h"
# include "ctype.h"
//# include "fcntl.h"
//# include <sys/wait.h>

# include "Tokenizer.h"
# include "Utils.h"
# include "Types.h"
# include "Scripter.h"
# include "LocaleManager.h"
# include "FontManager.h"

extern FILE * myStderr;

extern bool endsWith (const char * t, const char * e); // defined in Compiler.cpp
extern bool startsWith (const char * t, const char * e); // defined in Compiler.cpp

// int lastIndexOf (char * s, char c) {
//     int l = strlen (s);
//     int i;
//     for (i = l-1; i >= 0; i--) {
//         if (s[i] == c) { break; }
//     }
//     return i;
// }


//static char * indexS;

static bool isNonLocal(const char* name) {
    if( (name!=NULL) && (
            startsWith (name, "cache://") || 
            startsWith (name, "http://") || 
            startsWith (name, "file://") || 
            startsWith (name, "jar://")  ) ) {
        return true;
    }
    return false;
}

static void createTmpFileName (char * buffer, char * file, char * ext) {
    int i = lastIndexOf (file, '/');
    if (i > -1) {
        char * dir = file;
        file = dir+i+1;
        dir [i] = '\0';
        sprintf (buffer, "%s/tmp%s.%s", dir, file, ext); 
        dir [i] = '/';
    } else {
        sprintf (buffer, "tmp%s.%s", file, ext); 
    }
}

static const char * imgExt [6] = { ".png", ".PNG", ".jpg", ".JPG", ".gif", ".GIF" };
bool isImageName (const char * name) {
    if( isNonLocal(name) )
        return false;
    for (int i = 0; i < 6; i++) {
        if (endsWith (name, imgExt[i])) {
            return (true);
        }
    }
    return (false);
}

// multimedia file extensions
static const char * mmediaExt [] = { ".amr", ".mp3", ".m4a",
                                     ".3gp", ".wav", ".dcf",
                                     ".mpg", ".mp4", ".avi",
                                     ".wmv", NULL };
// check if filename contains multimedia extensions
bool isMMediaName (const char * name) {

    if( isNonLocal(name) )
        return false;

    // check if not null
    if( name == NULL )
        return false;
        
    // check if it contains at least 4 characters  
    int l = strlen(name);
    if(l<4)
        return false;

    // get and convert extension to lower case
    char ext[5];
    l-=4;
    int i=0;
    for(i=0;i<4;i++) {
        ext[i] = tolower(name[l+i]);
    }
    ext[4]=0;
    i=0;
    
    // check every extension
    while( mmediaExt[i] != NULL ) {
        if (endsWith (ext, mmediaExt[i])) {
            // match found
            return (true);
        }
        i++;
    }
    
    // no match
    return (false);
}

void myPrintf (int n, const char *fmt, ...) {
    static int ident = 0;
    if (n > 0) ident += n;
    for (int i = 0; i< ident; i++) {
        putc (' ', stdout);
    }
    if (n < 0) ident += n;
    va_list ap;
    va_start(ap, fmt);
    (void) vprintf(fmt, ap);
}

void exchangeBytes (char * s) {
    char t = s[0];
    s[0] = s[3];
    s[3] = t;
    t = s[1];
    s[1] = s[2];
    s[2] = t;
}

void write (FILE * fp, float f) {
# if 1
    int i = (int)(f*65536);
    char * s = (char *)&i;
    exchangeBytes (s);
    fwrite (s, 1, 4, fp);  
# else
    char * s = (char *)&f;
    exchangeBytes (s);
    fwrite (s, 1, 4, fp);  
# endif
}

void write (FILE * fp, int f) {
    char * s = (char *)&f;
    exchangeBytes (s);
    fwrite (s, 1, 4, fp);  
}

class ChunkList {
    char * m_name;
    ChunkList * m_next;
    static ChunkList * s_root;
    
    ChunkList (char * name, ChunkList * next) {
        m_name = name; 
        m_next = next; 
    }
public:
    static void add (const char * name) {
        s_root = new ChunkList (strdup (name), s_root); 
    }
    static bool find (const char * name) {
        ChunkList * tmp = s_root;
        while (tmp) {
            if (strcmp (tmp->m_name, name) == 0) {
                return (true);
            }
            tmp = tmp->m_next;
        }
        return (false);
    }
    static void print (char * name) {
        ChunkList * tmp = s_root;
        fprintf (stdout, "DBG:ChunkList.print: adding %s in :", name);
        while (tmp) {
            fprintf (stdout, "%s, ", tmp->m_name);
            tmp = tmp->m_next;
        }
        fprintf (stdout, "\n");
    }
};

ChunkList * ChunkList::s_root = NULL;

int includeFile (FILE * fp, const char * fileName, const char * name, int magic, 
                        bool mandatory = true) {
    if (ChunkList::find (name)) {
        //myPrintf (0, "    -- include %s already included]\n", name);
        return (0);
    }
    ChunkList::add (name);
    FILE * fp2 = MultiPathFile::fopen (fileName, "rb");
    long size = 0;
    if (fp2) {
        write (fp, magic);
        fwrite (name, 1, strlen (name), fp);
        fprintf (fp, "%c", 0);
        fseek (fp2, 0, SEEK_END);
        size = ftell (fp2);
        myPrintf (0, "    ++ include %s / %s size: %d magic: %X]\n", fileName, name, (int)size, magic);
        char * tmp = (char *)malloc (size);
        fseek (fp2, 0, SEEK_SET);
        fread (tmp, 1, size, fp2);
        fclose (fp2);
        write (fp, (int)size);
        fwrite (tmp, 1, size, fp);
        free (tmp);
    } else if (mandatory) {
        fprintf (myStderr, "**** includeFile: cannot open %s\n", name);
        exit (1);
    }
    return (size);
}

int includeMediaFile (FILE * fp, const char * name, bool mandatory = true) {
    int magic = 0;
    if (isImageName (name)) {
        magic = MAGIC_IMAGE;
    } else if (isMMediaName (name)) {
        magic = MAGIC_MMEDIA;
    } else if (endsWith (name, ".bml")) {
        magic = MAGIC_BML;
    } else if (endsWith (name, ".css")) {
        magic = MAGIC_CSS;
    }
    if (magic) {
        return includeFile (fp, name, name, magic, mandatory);
    }
    fprintf (stderr, "Error: Unsupported type for direct inclusion of file: %s.", name);
    exit (1);
}


const char * assoc [] = { 
    "unknown",
    "SFBool",     //1
    "SFColor",    //2
    "SFFloat",    //3
    "SFInt32",    //4
    "SFNode",     //5
    "SFRotation", //6
    "SFString",   //7
    "SFTime",     //8
    "SFVec2f",    //9
    "SFVec3f",    //10
    "MFBool",     //11
    "MFColor",    //12
    "MFFloat",    //13
    "MFInt32",    //14
    "MFNode",     //15
    "MFRotation", //16
    "MFString",   //17
    "MFVec2f",    //18
    "MFVec3f",    //19
    "SFTmp",      //20
    "MFField",    //21
    "SFDefined",  //22
    "MFAny",      //23
};

void spaces (int n) {
    while (n-- > 0) {
        fputc (' ', stdout);
    }
}

Field::Field () {
    m_clone = true;
    m_modified = false;
    m_number = -1;
    m_isDynamic = false;
    m_protoIdx = -2;
}

Field::Field (char * type, char * name, Field * next) {
    if (type) {
        m_type = typeId (type);
        free (type);
    } else {
        m_type = 0;
    }
    m_name = name; 
    m_next = next;
    m_clone = false;
    m_isDynamic = false;
    m_protoIdx = -2;
}

Field::~Field () {
    if (!m_clone) free (m_name);
}

void Field::copyFrom (Field * m) {
    m_type = m->m_type;
    m_name = m->m_name;
    m_next = NULL;
    m_number = m->m_number;
    m_clone = true;
    m_protoIdx = -3; //m_protoIdx;
}

Field * Field::cloneAll () {
    Field * c = clone ();
    if (m_next) {
        c->m_next = m_next->cloneAll();
    }
    return (c);
}

int Field::typeId (char * n) {
    int len = sizeof (assoc)/sizeof(char *);
    for (int i = 0; i < len; i++) {
        if (strcmp (assoc[i], n) == 0) {
            return (i);
        }
    }
    return (0);
}

void Field::encode (FILE * fp) {
    if (m_next) {
        m_next->encode (fp);
    }
    if (m_modified) {
        if (m_isDynamic) { // new field of a script node 
//             fprintf (myStderr, "encoding field %s %d/%d, dyn=%d, is=%d\n", 
//                      m_name, m_number, m_type, m_isDynamic, m_protoIdx);
            if (m_protoIdx > 0) { // and in addition is is a IS in a proto!!
                fprintf (fp, "%c%c%c", 253, m_number, m_protoIdx);
            } else {
                fprintf (fp, "%c%c%c", 255, m_number, m_type);
                encodeValue (fp);
            }
        } else if (m_protoIdx > 0) { // field is IS in a proto
            fprintf (fp, "%c%c%c", 254, m_number, m_protoIdx);
        } else {
            fprintf (fp, "%c", m_number);
            encodeValue (fp);
        }
    }
}

void Field::print (int n, bool fieldType) {
    if (m_next) {
        m_next->print (n, fieldType);
    }
    if (m_modified) {
        spaces (n);
        if (fieldType || m_isDynamic) {
            printf ("%s ", assoc[m_type]);
        }
        printf ("%s ", m_name);
        if (m_protoIdx > 0) {
            printf ("IS %d", m_protoIdx);
        } else {
            printValue (n);
        }
        if (fieldType) {
            printf (" # field ID: %d\n", m_number);
        } else {
            printf ("\n");
        }
    }
}

void Field::printValue (int n) {
    printf ("undefined");
}

/***********************
 * NodeName section   **
 ***********************/
NodeAssoc::NodeAssoc (char * name, Node * node, int id, NodeAssoc * next) {
    m_name = strdup (name?name:"ARGHH");
    m_id = id;
    m_node = node;
    m_next = next;
}

Node * NodeAssoc::findNode (const char * name) {
    if (strcasecmp (name, m_name) == 0) {
        return m_node;
    }
    if (m_next) {
        return m_next->findNode (name);
    }
    return NULL;
}

int NodeAssoc::findNodeID (const char * name) {
    if (strcasecmp (name, m_name) == 0) {
        return m_id;
    }
    if (m_next) {
        return m_next->findNodeID (name);
    }
    return -1;
}

int NodeAssoc::findFieldIdx (const char * nodeName, const char * fieldName) {
    Node * n = findNode (nodeName);
    return n ? n->findFieldIdx (fieldName) : -1;
}

// NodeName * NodeName::s_root = NULL;
// int NodeName::s_count = 1;
// char * NodeName::s_name = "root";

// int NodeName::s_countStack [256] = { 0 }; 
// NodeName * NodeName::s_rootStack [256] = { NULL }; 
// char * NodeName::s_nameStack [256] = { NULL }; 

// int NodeName::s_stackIdx = 0;

// void NodeName::push (char * sceneName) {
//     if (s_stackIdx < 255) {
//         //fprintf (myStderr, "==========================================\n");
//         //fprintf (myStderr, "== NodeName::push: %d %p\n", s_count, s_root);
//         //fprintf (myStderr, "==========================================\n");
//         s_countStack [s_stackIdx] = s_count;
//         s_count = 1;
//         s_rootStack [s_stackIdx] = s_root;
//         s_root = NULL;
//         s_nameStack [s_stackIdx] = s_name;
//         s_name = sceneName;
//         s_stackIdx++;
//     } else {
//         fprintf (myStderr, "INTERNAL ERROR: NodeName::push: stack full\n");
//     }
// }

// void NodeName::pop () {
//     if (s_stackIdx > 0) {
//         s_stackIdx--;
//         s_count = s_countStack [s_stackIdx];
//         s_root = s_rootStack [s_stackIdx];
//         s_name = s_nameStack [s_stackIdx];
//         //fprintf (myStderr, "==========================================\n");
//         //fprintf (myStderr, "== NodeName::pop: %d %p\n", s_count, s_root);
//         //fprintf (myStderr, "==========================================\n");
//     } else {
//         fprintf (myStderr, "INTERNAL ERROR: NodeName::pop: stack empty\n");
//     }
// }


/***********************
 * Node section       **
 ***********************/
Node * Node::s_lastTextNode = NULL;


Node::Node () {
    m_name = NULL;
    m_defName = NULL;
    m_field = NULL;
    m_next = NULL;
    m_clone = false;
    m_fieldNumber = 1;
    m_number = -1;
    m_isUsed = false;
}

Node::Node (char * name, Node * next) {
    m_name = name;
    m_defName = NULL;
    m_field = NULL;
    m_scene = NULL;
    m_next = next;
    m_clone = false;
    m_fieldNumber = 1;
    m_number = -1;
    m_isUsed = false;
}

Node * Node::find (char * name) {
    if (strcasecmp (name, m_name) == 0) {
        return (this);
    } else if (m_next) {
        return m_next->find (name);
    } else {
        return (NULL);
    }
}

void Node::setDefName (char * defName) {
    m_defName = defName;
    if (defName) {
        int id = m_scene->findNodeID (defName);
        if (id > -1) {
            fprintf (myStderr, "Warning: %s already used as DEF\n", defName);
        } else {
            m_scene->addAssoc (defName, this);
        }
        m_defID = m_scene->findNodeID (defName);
        //fprintf (myStderr, "Node::setDefName: added %s as %d\n", defName, m_defID);  
    }
}

void Node::setIsUsed (char * useName, Tokenizer * t) {
    m_defName = useName;
    m_defID = m_scene->findNodeID (m_defName);
    if (m_defID == -1) {
        fprintf (myStderr, "%s:%d: syntax error: invalide USE name: %s\n", t->getFile(), t->getLine (), useName); 
        exit (1);
    }
    m_isUsed = true;
}

Field * Node::findField (const char * name) {
    Field * f;
    if (m_isUsed) {
        //fprintf (myStderr, "Node::findField %s.%s: isUsed true\n", m_defName, name);
        Node * n = m_scene->findNode (m_defName);
        //fprintf (myStderr, "Node::findField: => using %p\n", n);
        if (n == NULL) {
            return NULL;
        }
        f = n->m_field;
    } else {
        f = m_field;
    } 
    while (f) {
        //fprintf (myStderr, "Node::findField %s %s\n", name, f->m_name);
        if (strcasecmp (name, f->m_name) == 0) {
            return (f);
        } else if (strncmp (name, "set_", 4) == 0) {
            if (strcasecmp (name+4, f->m_name) == 0) {
                return (f);
            }
        } else if (endsWith (name, "_changed")) {
            int idx = strlen (name)-8;
            if (strncasecmp(name, f->m_name, idx) == 0) {
                return (f);
            }
        }
        f = f->m_next;
    }
    return (NULL);
}

int Node::findFieldIdx (const char * name) {
    Field * f = findField (name);
    return (f ? f->m_number : -1);
}

Node * Node::clone (Node * next) {
    Node * n = new Node (m_name, next);
    n->m_clone = true;
    n->m_number = m_number;
    n->m_fieldNumber = m_fieldNumber;
    n->m_field = m_field->cloneAll ();
    return (n);
}

Field * Node::addField (char *type, char * name, bool isDynamic) {
  return addField (type, name, isDynamic, m_fieldNumber++);
}

Field * Node::addField (char *type, char * name, bool isDynamic, int fieldNumber) {
    m_field = createField (type, name, m_field);
    if (m_field) {
        m_field->setDynamic ();
        m_field->m_modified = true;
        m_field->m_number = fieldNumber;
        //fprintf (myStderr, "Node::addField: %s -> %d\n", name, m_field->m_number);
    } else {
        fprintf (myStderr, "Syntax error : unknown field type %s\n", type);
        exit (1);
    }
    return (m_field);
}

int Node::getNbFields () {
    int nb = 0;
    Field * f = m_field;
    while (f) {
        f = f->m_next;
        nb++;
    }
    return nb;
}

void Node::print (int n, bool fieldType, bool initSpaces) {
    if (m_next) {
        m_next->print (n, fieldType);
        printf ("\n");
    }
    if (initSpaces) {
        spaces (n);
    }
    if (m_isUsed) {
        printf ("USE %d\n", m_defID); //NodeName::findNodeID (m_defName));
    } else {
        if (m_defName) { 
            printf ("DEF %d ", m_defID); //NodeName::findNodeID (m_defName)); 
        }
        printf ("%s { # node ID: %d\n", m_name, m_number);
        if (m_field) {
            m_field->print (n+(initSpaces ? 4 : 0), fieldType);
        }
        spaces (n-(initSpaces ? 0 : 4));
        printf ("}");
    }
}

extern char * execPath;

int Node::encodeSpecial (FILE * fp, bool verbose) {
    int total = 0;
    if (m_next) {
        m_next->encodeSpecial (fp, verbose);
    }
    // case of image
    if (strcmp (m_name, "ImageTexture") == 0) {
        total = encodeImages ("url", fp, total);
        total = encodeImages ("alternateUrl", fp, total);
    } else if (  (strcmp (m_name, "AudioClip") == 0)
               ||(strcmp (m_name, "MovieTexture") == 0)) {
    	// case of sound or video
   	    MFString * f = (MFString*) findField ("url"); 
        if (f) {
   	        int i = 0;
       	    while (f->getValue(i) != NULL) {
           	    if (strlen (f->getValue(i)) > 0) {
               	    char * name = f->getValue(i); 
                   	//fprintf (myStderr, "Node::encodeSpecial: saving sound %s\n", name);
                    if (name && strncmp (name, "@[", 2) != 0) {
                        if(isNonLocal(name)==false)
   	                        total += includeFile (fp, name, name, MAGIC_MMEDIA);
       	            }
           	    }
              	i++;
            }
   	    }
     } else if (strcmp (m_name, "Script") == 0) {
        SFString * f = (SFString*)findField ("url"); 
        if (f) {
            char * buffer = f->getValue(); 
            if (buffer && strlen (buffer) > 0) {
                FILE * fp2 = fopen ("tmpScript.bin", "wb");
                if (fp2 == NULL) {
                    fprintf (myStderr, "ERROR: Cannot open tmpScript.bin for writing\n");
                    exit (1);
                }
                Tokenizer * t = new Tokenizer (buffer, true, f->m_fn, f->m_line);
                Scripter s (this, t, fp2, verbose);
                delete t;
                fclose (fp2);
                char tmpName [255];
                sprintf (tmpName, "%s/Script_%d", m_scene->getSceneName (), m_defID);//NodeName::findNodeID (m_defName));
                total += includeFile (fp, "tmpScript.bin", tmpName, MAGIC_SCRIPT);
            }
        }
    } else if (strcmp (m_name, "Inline") == 0 || strcmp (m_name, "Anchor") == 0 ) {
        MFString * f = (MFString*)findField ("url"); 
        if (f) {
            for (int i = 0; i < f->getSize (); i++) {
                char * buffer = f->getValue(i); 
                if (buffer && strlen (buffer) > 0&& 
                    !startsWith (buffer, "cache://") && 
                    !startsWith (buffer, "http://") && 
                    !startsWith (buffer, "file://") && 
                    !startsWith (buffer, "jar://") ) {
                    //char tmp [255];
                    //fprintf (myStderr, "Node::encodeSpecial: encoding inline %s\n", buffer);
                    if (endsWith (buffer, ".m4m") == false) {
                        fprintf (myStderr, "Error: Cannot inline file: '%s'. File must end with the '.m4m' extension.\n", buffer);
                        fprintf (myStderr, "Error: The compiler will then search for the corresponding source file ending with '.wrl'.\n");
                        exit (1);
                    }
                    char * text = strdup (buffer);
                    strcpy (text+strlen(text)-3, "wrl");
                    Scene scene (buffer, true);
                    
                    bool added = MultiPathFile::addPath (text);
                    FILE * in = MultiPathFile::fopen (text, "rb");
                    if (in == NULL) {
                        fprintf (myStderr, "cannot open inlined scene '%s'\n", text);
                    } else {
                        m_scene->push (text);
                        myPrintf (4, ">> Start compiling scene %s\n", text);
                        scene.parse (text, in, verbose);
                        int tmpTotal = scene.encode (fp, verbose);
                        myPrintf (-4, "<< End of scene %s compilation [%d B]\n", text, tmpTotal);
                        m_scene->pop ();
                        fclose (in); 
                        total += tmpTotal;
                    }
                    if (added) MultiPathFile::removePath (text);
                }
            }
        }
        f = (MFString*)findField ("languages"); 
        if (f) {
            for (int i = 0; i < f->getSize (); i++) {
                char * buffer = f->getValue(i);
                if (buffer && strlen (buffer) > 0 && endsWith (buffer, ".lng")) {
                    bool added = MultiPathFile::addPath (buffer);
                    FILE * in = MultiPathFile::fopen (buffer, "rb");
                    if (in == NULL) {
                        fprintf (myStderr, "cannot open language file '%s'\n", buffer);
                    } else {
                        char * text = strdup (buffer);
                        strcpy (text+strlen(text)-3, "loc");
                        int tmpTotal = 0;
                        LocaleManager::getManager()->encodeExtra (in, buffer, text);
                        total += includeFile (fp, text, buffer, MAGIC_LOCALE, false);
                        fclose (in);
                        total += tmpTotal;
                        free (text);
                    }
                    if (added) MultiPathFile::removePath (buffer);
                }
            }
        }
    } else if (strcmp (m_name, "Style") == 0) {
        MFString * f = (MFString*)findField ("url"); 
        if (f) {
            for (int i = 0; i < f->getSize (); i++) {
                char * buffer = f->getValue(i);
                if (buffer && strlen (buffer) > 0 && endsWith (buffer, ".css")) {
                    bool added = MultiPathFile::addPath (buffer);
                    FILE * in = MultiPathFile::fopen (buffer, "rb");
                    if (in == NULL) {
                        fprintf (myStderr, "cannot open CSS file '%s'\n", buffer);
                    } else {
                        fclose (in);
                        total += includeFile (fp, buffer, buffer, MAGIC_CSS, true);
                    }
                    if (added) MultiPathFile::removePath (buffer);
                }
            }
        }
    } else if (strcmp (m_name, "Text") == 0) {
        s_lastTextNode = this;
    } else if (strcmp (m_name, "FontStyle") == 0) {
        MFString * f = (MFString*)findField ("family"); 
        char * fontName = f ? f->getValue(0) : NULL;
        SFFloat * s = (SFFloat*)findField ("size");
        int size = s ? (int)(s->getValue ()) : -1;
        if (fontName != NULL && strlen (fontName) > 0 && 
            strcmp (fontName, "SANS") != 0 &&
            strcmp (fontName, "SERIF") != 0 &&
            strcmp (fontName, "TYPEWRITER") != 0 &&
            size > -1) {
            // tentative to build a font map
            char * defaultMap = (char*)" !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}éèêàâÉÈÊÀÂïîÏÎüûÜÛçÇùÙôÔßñıÑ";
            char * charMap = (char *)"";
            int len = strlen (fontName);
            for (int i = 0; i < len; i++) {
                if (fontName[i] == '/') {
                    fontName[i] = '\0';
                    if (strlen (fontName+i+1) > 0) {
                        charMap = fontName+i+1;
                        if (strcmp (charMap, "*") == 0) {
                            charMap = defaultMap;
                        }
                    }
                    break;
                }
            }
            FontManager::addMap (fontName, size, charMap);
            // add text in the string field of associated parent Text node 
            if (s_lastTextNode != NULL) {
                MFString * string = (MFString*) s_lastTextNode->findField ("string");
                if (string != NULL) {
                    for (int i = 0; i < string->getSize(); i++) {
                        //fprintf (stderr, "$$ text from string: %s\n", string->getValue (i));
                        FontManager::addMap (fontName, size, string->getValue (i));
                    }
                }
            }
        }
    } else if (isProto () ) {
        //fprintf (myStderr, "Encode Special: PROTO %s\n", m_name);
        Field * f = m_field;
        while (f) {
            //fprintf (myStderr, "....field %s %d\n", f->m_name, f->m_type);
            if (f->m_type == Field::TYPE_ID_MFSTRING) {
                MFString * mfs = (MFString*) f;
                int i = 0;
                char * name = mfs->getValue(i);
                while (name != NULL) {
                    //fprintf (myStderr, "Node::encodeSpecial of proto: check %s\n", name);
                    if (isImageName (name)) {
                        //fprintf (myStderr, "Node::encodeSpecial of proto: try to save image %s\n", name);
                        total += includeFile (fp, name, name, MAGIC_IMAGE, false);
                    } else if (endsWith (name, "m4m") && 
                               !startsWith (name, "cache://") && 
                               !startsWith (name, "http://") && 
                               !startsWith (name, "file://") && 
                               !startsWith (name, "jar://") ) {
                        char * text = strdup (name);
                        strcpy (text+strlen(text)-3, "wrl");
                        Scene scene (name, true);
                        FILE * in =  MultiPathFile::fopen (text, "rb");
                        if (in == NULL) {
                            fprintf (myStderr, "cannot open inlined scene '%s'\n", text);
                        } else {
                            m_scene->push (text);
                            myPrintf (4, ">> Start compiling scene %s\n", text);
                            scene.parse (text, in, verbose);
                            int tmpTotal = scene.encode (fp, verbose);
                            myPrintf (-4, "<< End of scene %s compilation [%d B]\n", text, tmpTotal);
                            m_scene->pop ();
                            fclose (in);     
                            total += tmpTotal;
                        }
                    } else if (endsWith (name, "bml")) {
                        total += includeFile (fp, name, name, MAGIC_BML, false);
                    } else if (isMMediaName (name)) {
                        total += includeFile (fp, name, name, MAGIC_MMEDIA, false);
                    } 
                    name = mfs->getValue(++i); 
                }
            }
            f = f->m_next;
        }
    }
    
    if (m_field) {
        total += m_field->encodeSpecial (fp, verbose);
    }
    s_lastTextNode = NULL; // shoudl be necessary only when the current Node is a Text, but the test is as expansive as the affectation
    return total;
}

int Node::encodeImages (const char * fieldName, FILE * fp, int total) {
    MFString * f = (MFString*) findField (fieldName);
    if (f) {
        int i = 0;
        char * name;
        while ((name = f->getValue(i++)) != NULL) {
            if (strlen (name) > 0 && strncmp (name, "@[", 2) != 0 && !isNonLocal (name)) {
                //fprintf (myStderr, "Node::encodeSpecial: saving image %s\n", name);
                total += includeFile (fp, name, name, MAGIC_IMAGE);
            }
        }
    }
    return total;
}

void Node::tableEntry (FILE * fp) {
    if (m_next) {
        m_next->tableEntry (fp);
    }
    if (strcmp (m_name, "UnusedNode") != 0) {
      fprintf (fp, "    public final static int %s = %d;\n", m_name, m_number);
    }
}


void Node::encodeValue (FILE * fp) {
    if (m_next) {
        m_next->encodeValue (fp);
    }
    if (m_isUsed) {
        fprintf (fp, "%c", 4);
        int id = m_scene->findNodeID (m_defName);
        write (fp, id);
        //fprintf (myStderr, ">> USE %s -> %d\n", m_defName, id);
    } else {
        if (m_defName) {
            fprintf (fp, "%c", 2);
            int id = m_scene->findNodeID (m_defName);
            write (fp, id);
        } else {
            fprintf (fp, "%c", 1);
        }
        fprintf (fp, "%c", m_number);
        if (strcmp (m_name, "Script") == 0) {
            encodeScript (fp);
        } else if (m_field) {
            m_field->encode (fp);
        }
        fprintf (fp, "%c", 0);
    }
}

void Node::encodeScript (FILE * fp) {
    //fprintf (myStderr, "********\nNode::encodeScript\n******\n");
    SFString * url = (SFString*)findField ("url");
    fprintf (fp, "%c%c", 252, m_field == NULL ? 1 : 1+m_field->getCount()); // write the number of dynamic fields 
    if (url) {
        char * code = url->getValue ();
        char buffer [1024];
        sprintf (buffer, "%s/Script_%d", m_scene->getSceneName(), m_defID);//NodeName::findNodeID (m_defName));
        url->setValue (buffer);
        //fprintf (myStderr, "Node::encodeScript: new url: %s\n", url->getValue ());
        m_field->encode (fp);
        url->setValue (code);
    } else {
        m_field->encode (fp);
    }
}

/***********************
 * PROTO section      **
 ***********************/
#define PROTO_MAX_FIELDS 250

Proto::Proto () : Node () {
    m_scene = NULL;
}

Proto::Proto (Tokenizer * t, Scene * master, Proto * next) : Node () {
    m_fieldNumber = 1;
    m_field = NULL;
    m_scene = NULL;
    m_master = master;
    m_name = t->getNextToken ();
    m_next = next;
    
    if (m_name == NULL) {
        fprintf (myStderr, "%s:%d: syntax error: PROTO declaration: missing name\n", t->getFile(), t->getLine ());
        exit (1);
    }
    if (!t->check ('[')) {
        fprintf (myStderr, "%s:%d: syntax error: PROTO declaration: missing '[' after %s\n", t->getFile(), t->getLine (), m_name);
        exit (1);
    }
    while (true) {
        char * scope = t->getNextToken ();
        if (scope == NULL || (strcmp (scope, "exposedField") != 0 &&
                              strcmp (scope, "field") != 0 &&
                              strcmp (scope, "eventIn") != 0 &&
                              strcmp (scope, "eventOut") != 0)) {
            fprintf (myStderr, "%s:%d: syntax error: PROTO declaration: unknown field scope: %s\n", t->getFile(), t->getLine(), scope);
            exit (1);
        }
        char * type = t->getNextToken ();
        char * fieldName = t->getNextToken ();
        //fprintf (myStderr, "DBG: Scene.parseProto: adding field '%s %s %s'\n", scope, type, fieldName);
        if (m_fieldNumber > PROTO_MAX_FIELDS) {
            fprintf (myStderr, "%s:%d: proto error: maximum of %d fields allowed'\n", t->getFile(), t->getLine (), PROTO_MAX_FIELDS);
            exit (1);
        }
        m_field = createField (type, fieldName, m_field);
        if (m_field) {
            m_field->setDynamic ();
            m_field->m_modified = true;
            m_field->m_number = m_fieldNumber++;
            //fprintf (myStderr, "Node::addField: %s -> %d\n", fieldName, m_field->m_number);
        } else {
            fprintf (myStderr, "%s:%d: syntax error: unknown field type %s\n", t->getFile(), t->getLine (), type);
            exit (1);
        }
        
        if (strcmp (scope, "exposedField") == 0 || strcmp (scope, "field") == 0) {
            m_field->parseValue (master, t);
        }
        if (t->check(']')) {
            break;
        }
    }
    if (!t->check ('{')) {
        fprintf (myStderr, "%s:%d: syntax error: PROTO: missing '{' after fields declaration\n", t->getFile(), t->getLine ());
        exit (1);
    }
    //NodeName::push (m_name);
    m_scene = new Scene (m_name, true);
    m_scene->m_parentProto = this;
    m_scene->parse (t, true, false);
    //NodeName::pop ();
    if (!t->check ('}')) {
        fprintf (myStderr, "%s:%d: syntax error: PROTO: missing final '}'\n", t->getFile(), t->getLine ());
        exit (1);
    }
}


void Proto::printAll (int n) {
    spaces (n);
    printf ("PROTO %s [\n", m_name);
    m_field->print (n+4, false);
    spaces (n);
    printf ("] {\n");
    m_scene->print (n+4);
    printf ("\n");
    spaces (n);
    printf ("}\n");
}

Node * Proto::clone (Node * next) {
    Proto * n = new Proto ();
    n->m_name = m_name;
    n->m_next = next;
    n->m_clone = true;
    n->m_number = m_number;
    n->m_fieldNumber = m_fieldNumber;
    n->m_field = m_field->cloneAll ();
    n->m_scene = m_scene;
    return (n);
}

void Proto::print (int n, bool fieldType, bool initSpaces) {
    if (m_next) {
        m_next->print (n, fieldType);
        printf ("\n");
    }
    if (initSpaces) {
        spaces (n);
    }
    if (m_isUsed) {
        printf ("USE %d\n", m_defID); //NodeName::findNodeID (m_defName));
    } else {
        if (m_defName) { 
            printf ("DEF %d ", m_defID); //NodeName::findNodeID (m_defName)); 
        }
        printf ("%s { # PROTO\n", m_name);
        if (m_field) {
            m_field->print (n+(initSpaces ? 4 : 0), fieldType);
        }
        spaces (n-(initSpaces ? 0 : 4));
        printf ("}");
    }
}

void Proto::encodeValue (FILE * fp) {
    if (m_next) {
        m_next->encodeValue (fp);
    }
    if (m_isUsed) {
        fprintf (fp, "%c", 4);
        int id = m_scene->findNodeID (m_defName);
        write (fp, id);
        fprintf (myStderr, "???? PROTO is used %s -> %d\n", m_defName, id);
    } else {
        if (m_defName) {
            fprintf (fp, "%c", 10);
            //int id = m_scene->findNodeID (m_defName);
            //fprintf (myStderr, "$$$$ Proto::encodeValue:%s => %d/%d\n", m_defName, id, m_defID);
            write (fp, m_defID);
        } else {
            fprintf (fp, "%c", 9);
        }
        fwrite (m_name, 1, strlen (m_name), fp);
        fprintf (fp, "%c", 0);
        fprintf (fp, "%c%c", 252, m_field == NULL ? 1 : 1+m_field->getCount()); // write the number of dynamic fields 
        //m_field->encode (fp);
        m_field->encode (fp);
        fprintf (fp, "%c", 0);
    }
}

int Proto::encode (FILE * fp, bool verbose) {
    int total = 0;
    if (m_scene->m_root) {
        char buffer [2048];
        
        //createTmpFileName (buffer, m_name, "proto");
        sprintf (buffer, "tmp%s.proto", m_name);
        
        FILE * tmp = fopen (buffer, "wb");
        if (tmp == NULL) {
            fprintf (myStderr, "**** Proto::encodeValue: cannot open %s\n", buffer);
            exit (1);
        }
        
        if (m_field) {
            m_field->encode (tmp);
        }
        fprintf (tmp, "%c", 0);
        
        //NodeName::push (m_name);
        m_scene->m_root->encodeValue (tmp);
        fprintf (tmp, "%c", 0);
        if (m_scene->m_route) {
            m_scene->m_route->encodeValue (tmp);
        }
        fprintf (tmp, "%c", 0);
        fclose (tmp);
        total += includeFile (fp, buffer, m_name, MAGIC_PROTO);
        total += m_scene->m_root->encodeSpecial (fp, verbose);
        total += this->encodeSpecial(fp, verbose);
        
        //NodeName::pop ();
    }
    return total;
}

int Proto::getFieldId (const char *name) {
    Field * f = m_field;
    while (f) {
        //fprintf (myStderr, "Node::findField %s %s\n", name, f->m_name);
        if (strcasecmp (name, f->m_name) == 0) {
            return (f->getTypeId());
        }
        f = f->m_next;
    }
    return (-1);
}

// Node * Proto::findOuterProto (char *name) {
//     if (m_master) {
//         return m_master->find (name);
//     }
//     return NULL;
// }

int Proto::findFieldIdx (const char *name) {
    Field * f = m_field;
    while (f) {
        //fprintf (myStderr, "Node::findField %s %s\n", name, f->m_name);
        if (strcasecmp (name, f->m_name) == 0) {
            return (f->m_number);
        }
        f = f->m_next;
    }
    return (-1);
}

/***********************
 * Route section      **
 ***********************/
Route::Route (int inNameIdx, int inFieldIdx, 
              int outNameIdx, int outFieldIdx, Route * next) {
    m_nodeIn = inNameIdx;
    m_fieldIn = inFieldIdx;
    m_nodeOut = outNameIdx;
    m_fieldOut = outFieldIdx;
    m_next = next;
}

void Route::encodeValue (FILE * fp) {
    if (m_next) {
        m_next->encodeValue (fp);
    }
    fprintf (fp, "%c%c%c%c", m_nodeIn, m_fieldIn, m_nodeOut, m_fieldOut);
}

void Route::print (int n) {
    if (m_next) {
        m_next->print (n);
    }
    spaces (n);
    printf ("ROUTE %d.%d TO %d.%d\n", m_nodeIn, m_fieldIn, m_nodeOut, m_fieldOut);
}

#include "NodeTable.inc"

/***********************
 * Scene section      **
 ***********************/

Scene::Scene (char * name, bool verbose) {
    m_root = NULL;
    m_route = NULL;
    m_name = name;
    m_nodeTable = new NodeTable (name, NULL);
    m_proto = m_parentProto = NULL;
    if (m_model == NULL) {
        m_model = parseModel (nodeTableDef);
        //m_model = parseModel ("NodeTable.def");
        if (verbose) m_model->print (0, true);
        fflush (stdout);
    }
}

Node * Scene::clone (char * name, Node * next) {
    if (m_model == NULL) {
        fprintf (myStderr, "Error: no model loaded!!\n");
        exit (1);
    }
    Node * node = m_model->find (name); 
    Node * clone = NULL;
    if (node) {
        clone = node->clone (next);
    } else {
        if (m_proto) {
            node = m_proto->find (name);
        }
        if (node) {
            clone = node->clone (next);
        } else if (m_parentProto && m_parentProto->m_master) {
            clone = m_parentProto->m_master->clone (name, next);
            //fprintf (myStderr, "Scene::clone: cloning parent proto %s : %p\n", name, clone);
        }
    }
    if (clone) {
        clone->setScene (this);
        return clone;
    }
    fprintf (myStderr, "clone %s: no node found!!\n", name);
    return (NULL);
}

Node * Scene::parseNodeModel (Tokenizer * t, Node * root) {
    char * name = t->getNextToken ();
    if (name == NULL) {
        //fprintf (myStderr, "Scene::parseNodeModel: Cannot read Node name, line %d\n", t->getLine());
        return (NULL);
    }
    if (strcmp (name, "NULL") == 0) {
        return NULL;
    }
    //fprintf (myStderr, "parseNodeModel: parsing %s\n", name);
    Node * node = new Node (name, root);
    if (!t->check ('{')) {
        fprintf (myStderr, "missing '{' after %s\n", name);
        delete node;
        return (NULL);
    }
    node->m_number = m_nodeNumber++;
    while (!t->check('}')) {
        char * type = t->getNextToken ();
        char * field = t->getNextToken ();
        char * type2 = NULL;
        char * field2 = NULL;
        // Check for presence of an alternative field name using the pipe separator
        if (field != NULL && t->check('|')) {
            type2 = strdup (type);
            field2 = t->getNextToken ();
        }
        //fprintf (myStderr, "    adding field '%s' '%s'\n", type, field);
        Field * f = node->addField (type, field, false);
        f->parseValue (this, t);
        // Add second field with the same number as the previous one
        if (f != NULL && field2 != NULL && strlen (field2) > 0) {
          node->addField (type2, field2, false, f->m_number);
        }
    }
    return (node);
}

void Scene::parseRoute (Tokenizer * t) {
    char * nodeIn = t->getNextToken ();
    if (nodeIn == NULL) { fprintf (myStderr, "parseRoute: In node missing\n"); exit (1); }
    if (!t->check ('.')) { fprintf (myStderr, "parseRoute: missing '.'\n"); exit (1); }
    char * fieldIn = t->getNextToken ();
    if (fieldIn == NULL) { fprintf (myStderr, "parseRoute: In field missing\n"); exit (1); }
    char * to = t->getNextToken (); 
    if (to == NULL || strcmp (to, "TO") != 0) {
        fprintf (myStderr, "Scene::parseRoute: missing TO\n");
    }
    char * nodeOut = t->getNextToken ();
    if (nodeOut == NULL) { fprintf (myStderr, "parseRoute: Out node missing\n"); exit (1); }
    if (!t->check ('.')) { fprintf (myStderr, "parseRoute: missing '.'\n"); exit (1); }
    char * fieldOut = t->getNextToken ();
    if (fieldOut == NULL) { fprintf (myStderr, "parseRoute: Out field missing\n"); exit (1); }
    int inNameIdx, outNameIdx;
    int inFieldIdx, outFieldIdx;
    inNameIdx = findNodeID (nodeIn);
    if (inNameIdx == -1) {
        fprintf (myStderr, "%s:%d: syntax error: ROUTE declaration: IN node '%s' does not exist\n", t->getFile(), t->getLine(), nodeIn);
        exit (1);
    }
    inFieldIdx = findFieldIdx (nodeIn, fieldIn);
    if (inFieldIdx == -1) {
        fprintf (myStderr, "%s:%d: syntax error: ROUTE declaration: IN field '%s' does not exist\n", t->getFile(), t->getLine(), fieldIn);
        exit (1);
    }
    outNameIdx = findNodeID (nodeOut);
    if (outNameIdx == -1) {
        fprintf (myStderr, "%s:%d: syntax error: ROUTE declaration: OUT node '%s' does not exist\n", t->getFile(), t->getLine(), nodeOut);
        exit (1);
    }
    
    outFieldIdx = findFieldIdx (nodeOut, fieldOut);
    if (outFieldIdx == -1) {
        fprintf (myStderr, "%s:%d: syntax error: ROUTE declaration: OUT field '%s' does not exist\n", t->getFile(), t->getLine(), fieldOut);
        exit (1);
    }
    int typeIn = findNode (nodeIn)->findField (fieldIn)->m_type;
    int typeOut = findNode (nodeOut)->findField (fieldOut)->m_type;
    if (typeIn != typeOut) {
        fprintf (myStderr, "%s:%d: ROUTE types mismatch: %s (in) is %s and %s (out) is %s", t->getFile(), t->getLine(), fieldIn, assoc [typeIn], fieldOut, assoc [typeOut]);
        exit (1);
    }
    m_route = new Route (inNameIdx, inFieldIdx, outNameIdx, outFieldIdx, m_route);
}

void Scene::parseProto (Tokenizer * t, bool verbose) {
    m_proto =  new Proto (t, this, m_proto);
    if (verbose) {
        m_proto->printAll (0);
    }
}

void Scene::parseExternProto (Tokenizer * t, bool verbose) {
    char * name = t->getNextToken ();
    if (!t->check ('[')) {
        fprintf (myStderr, "%s:%d: syntax error: EXTERNPROTO declaration: missing '[' after %s\n", t->getFile(), t->getLine (), m_name);
        exit (1);
    }
    name = t->getNextToken ();
    while (name) {
        name = t->getNextToken ();
    }
    if (!t->check (']')) {
        fprintf (myStderr, "%s:%d: syntax error: EXTERNPROTO declaration: missing ']' after %s params definition\n", t->getFile(), t->getLine (), m_name);
        exit (1);
    }
    char * filename = t->getNextString ();
    if (filename) {
        MultiPathFile::addPath(filename);
        FILE * fp = MultiPathFile::fopen(filename, "rb");
        t->include (fp, filename);
    }
}

Node * Scene::parseNode (Tokenizer * t, Node * root) {
    char * name = t->getNextToken ();
    //fprintf (myStderr, "Scene::parseNode: got token %s\n", name);
    char * defName = NULL;
    if (name == NULL) {
        //fprintf (myStderr, "Scene::parseNode: Cannot read Node name, line %d\n", t->getLine());
        //exit (1);
        return (NULL);
    }
    if (strcmp (name, "NULL") == 0) {
        return NULL;
    }
    if (strcasecmp (name, "PROTO") == 0) {
        parseProto (t, false);
        return (parseNode (t, root));
    }
    
    if (strcasecmp (name, "EXTERNPROTO") == 0) {
        parseExternProto (t, false);
        return (parseNode (t, root));
    }
    
    if (strcasecmp (name, "ROUTE") == 0) {
        parseRoute (t);
        return (parseNode (t, root));
    }
    if (strcasecmp (name, "USE") == 0) {
        defName = t->getNextToken ();
        Node * node = new Node ((char*)"USE", root);
        node->setScene (this);
        node->setIsUsed (defName, t);
        return (node);
    }
    if (strcasecmp (name, "DEF") == 0) {
        defName = t->getNextToken ();
        name = t->getNextToken ();
        if (name == NULL) {
            fprintf (myStderr, "%s:%d: syntax error: Cannot read Node name after DEF\n", t->getFile(), t->getLine());
            return (NULL);
        }
    }
    bool isScript = strcmp (name, "Script") == 0;
    
    Node * node = clone (name, root);
    if (node == NULL) {
        fprintf (myStderr, "%s:%d: syntax error: unknown node %s\n", t->getFile(), t->getLine (), name);
        exit (1);
    }
    if (!t->check ('{')) {
        fprintf (myStderr, "%s:%d: missing '{' after %s\n", t->getFile(), t->getLine(), name);
        delete node;
        return (NULL);
    }
    node->setDefName (defName);
    int dbgCount = 0;
    while (true) {
        char * fieldName = t->getNextToken ();
        Field * field = fieldName ? node->findField (fieldName) : NULL;
        if (field == NULL) {
            if (isScript) {
                char * scope = fieldName;
                if (scope == NULL || (strcmp (scope, "exposedField") != 0 &&
                                      strcmp (scope, "field") != 0 &&
                                      strcmp (scope, "eventIn") != 0 &&
                                      strcmp (scope, "eventOut") != 0)) {
                    fprintf (myStderr, "%s:%d: syntax error: unknown field scope: %s\n", t->getFile(), t->getLine(), scope);
                    exit (1);
                }
                char * type = t->getNextToken ();
                fieldName = t->getNextToken ();
                if (strcasecmp (fieldName, "url") == 0) {
                    fprintf (myStderr, "%s:%d: script error: Declaring a dynamic field called 'url' is not allowed in a Script node. It is reserved for scripting content.\n", t->getFile(), t->getLine ());
                    exit (1);
                }
                //fprintf (myStderr, "Scene.parseNode: adding Script field '%s' '%s'\n", type, fieldName);
                //fprintf (myStderr, "Scene.parseNode: to node %s / %d\n", node->m_name, node->m_fieldNumber);
                if (node->getNbFields () > 252) {
                    fprintf (myStderr, "%s:%d: script error: maximum of 252 fields allowed'\n", t->getFile(), t->getLine ());
                    exit (1);
                }
                field = node->addField (type, fieldName, true);
                
                if (t->checkToken ("IS")) {
                    char * tmp = t->getNextToken ();
                    //fprintf (myStderr, "DBG: script field parse IS with %s\n", tmp);
                    int id = -666;
                    if (tmp == NULL || m_parentProto == NULL || (id = m_parentProto->findFieldIdx (tmp)) < 1) {
                        fprintf (myStderr, "%s:%d: syntax error: unknown PROTO field after IS: %s (%p,%d)\n", t->getFile(), t->getLine(), tmp, m_proto, id);
                        exit (1);
                    }
                    field->m_protoIdx = id;
                    if (field->getTypeId () != m_parentProto->getFieldId (tmp)) {
                        fprintf (myStderr, "%s:%d: syntax error: %s type mismatch for IS \n", t->getFile(), t->getLine(), tmp);
                        exit (1);
                    }
                } else if (strcmp (scope, "exposedField") == 0 || strcmp (scope, "field") == 0) {
                    field->parseValue (this, t);
                }
                
//                 if (strcmp (scope, "exposedField") == 0 || strcmp (scope, "field") == 0) {
//                     if (t->checkToken ("IS")) {
//                         char * tmp = t->getNextToken ();
//                         //fprintf (myStderr, "DBG: script field parse IS with %s\n", tmp);
//                         int id = -666;
//                         if (tmp == NULL || m_parentProto == NULL || (id = m_parentProto->findFieldIdx (tmp)) < 1) {
//                             fprintf (myStderr, "%s:%d: syntax error: unknown PROTO field after IS: %s (%p,%d)\n", t->getFile(), t->getLine(), tmp, m_proto, id);
//                             exit (1);
//                         }
//                         field->m_protoIdx = id;
//                         if (field->getTypeId () != m_parentProto->getFieldId (tmp)) {
//                             fprintf (myStderr, "%s:%d: syntax error: %s type mismatch for IS \n", t->getFile(), t->getLine(), tmp);
//                             exit (1);
//                         }
//                     } else {            
//                         field->parseValue (this, t);
//                     }
//                 }
                
                field = NULL;
            } else if (fieldName != NULL && strcmp (fieldName, "ROUTE") == 0)  { // 3DSMAX exporter put ROUTEs at strange position
                parseRoute (t);
                //return (parseNode (t, root));
            } else if (fieldName != NULL)  {
                fprintf (myStderr, "%s:%d: syntax error: unknown field: '%s'\n", t->getFile(), t->getLine(), fieldName);
                exit (1);
            }
        } else {
            // Temporary hack to warn user about the maxSize change !
            if (strcasecmp (name, "imagetexture") == 0 && strcasecmp (fieldName, "maxsize") == 0) {
              fprintf (stderr, "%s:%d: warning: the 'maxSize' field in ImageTexture is now deprecated, use the 'newSize' field !\n", t->getFile(), t->getLine());
            }
            if (t->checkToken ("IS")) {
                char * tmp = t->getNextToken ();
                //fprintf (myStderr, "DBG: parse IS with %s\n", tmp);
                int id = -666;
                if (tmp == NULL || m_parentProto == NULL || (id = m_parentProto->findFieldIdx (tmp)) < 1) {
                    fprintf (myStderr, "%s:%d: syntax error: unknown PROTO field after IS: %s (%p,%d)\n", t->getFile(), t->getLine(), tmp, m_proto, id);
                    exit (1);
                }
                field->m_protoIdx = id;
                if (field->getTypeId () != m_parentProto->getFieldId (tmp)) {
                    fprintf (myStderr, "%s:%d: syntax error: %s type mismatch for IS \n", t->getFile(), t->getLine(), tmp);
                    exit (1);
                }
            } else {
                //field->m_protoIdx = 2;
                field->parseValue (this, t);
            }
            field->m_modified = true;
        }
        if (t->check('}')) {
            //fprintf (myStderr, "end of parsing node %s\n",node->m_name);
            break;
        } else if (t->check(']')) {
            fprintf (myStderr, "%s:%d: syntax error: unexpected ']' while parsing %s\n", t->getFile(), t->getLine(), node->m_name);
            exit (1);            
        } else if (++dbgCount > 250) {
            fprintf (myStderr, "Internal loop error while parsing *BAD* scene or too much fields in a Script or PROTO (max 250). Please check your content before this point:\n");
            for (int z = 0; z < 100; z++) {
                fprintf (myStderr, "%c", t->GETC ());
            }
            fprintf (myStderr, "\n");
            exit (1);
        }
    }
    return (node);
}

Node * Scene::parse (char * fn, FILE * fp, bool verbose) {
    MultiPathFile::addPath (fn);
    Tokenizer t (fp, false, fn);
    return parse (&t, false, verbose);
}

void Scene::print (int n) {
    m_root->print (n);
    if (m_route) {
        m_route->print (n);
    }
}

Node * Scene::parse (Tokenizer * t, bool fromProto, bool verbose) {
    Node * node = parseNode (t, m_root);
    int n = 0;
    while (node) {
        n++;
        m_root = node;
        node = parseNode (t, m_root);
    }
    // proto nodes are allowed to be empty in player or,
    // enclose everything in a group to have onlny one top node
    if ((fromProto && n == 0) || n > 1) { 
        //fprintf (myStderr, "multiple nodes : %d\n", n);
        Node * tmp = clone ((char*)"Group", NULL);
        MFNode * field = (MFNode *)tmp->findField ("children");
        field->setValue (m_root, n);
        field->m_modified = true;
        m_root = tmp;
    }
    if (verbose) {
        print (0);
    }
    LocaleManager::getManager()->saveDefault ();
    return (m_root);
}

int Scene::encode (FILE * fp, bool verbose) {
    int total = 0;
    char buffer [2048];
    if (fp) {
        Proto * proto = m_proto;
        while (proto) {
            total += proto->encode (fp, verbose);
            proto = (Proto*)proto->m_next;
        }
        if (m_root) {
            createTmpFileName (buffer, m_name, (char*)"bin");
            //sprintf (buffer, "tmp%s.bin", m_name);
            FILE * tmp = fopen (buffer, "wb");
            if (tmp == NULL) {
                fprintf (myStderr, "**** Scene::encode: cannot open %s\n", buffer);
                exit (1);
            }
            m_root->encodeValue (tmp);
            if (m_route) {
                m_route->encodeValue (tmp);
            }
            fprintf (tmp, "%c", 0);
            fclose (tmp);
            total += includeFile (fp, buffer, m_name, MAGIC_SCENE);
            Node::s_lastTextNode = NULL;
            total += m_root->encodeSpecial (fp, verbose);
        }
    }
    return (total);
}

void Scene::dumpTable (FILE * fp) {
    if (fp) {
        fprintf (fp, "package memoplayer;\n\n");
        fprintf (fp, "public interface NodeTable {\n");
        if (m_model) {
            m_model->tableEntry (fp);
        }
        fprintf (fp, "}\n");
    }
}

Node * Scene::parseModel (char * n) {
    Node * model = NULL;
    Tokenizer t (n, false, "MODEL",1);
    Node * node = parseNodeModel (&t, model);
    while (node) {
        model = node;
        node = parseNodeModel (&t, model);
    }
    return (model);
    
//     Node * model = NULL;
//     FILE * fp = fopen (n, "r");
//     if (fp == NULL) {
//     fprintf (myStderr, "cannot open model '%s'\n", n);
//     return (NULL);
//     }
    
//     Tokenizer t (fp, false);
//     Node * node = parseNodeModel (&t, model);
//     while (node) {
//     model = node;
//     node = parseNodeModel (&t, model);
//     }
//     fclose (fp);
//     return (model);
}

Node * Scene::m_model = NULL;
int Scene::m_nodeNumber = 0;


SFBool::SFBool (char * type, char * name, Field * next)
    : Field (type, name, next) {
    v = false;
}

void SFBool::printValue (int n) { 
    printf ("%s", v ? "TRUE":"FALSE"); 
}

void SFBool::encodeValue (FILE * fp) { 
    fprintf (fp, "%c", v ? 1 : 0);
}

void SFBool::parseValue (Scene * scene, Tokenizer *t) {
    char * s = t->getNextToken ();
    v = s && strcasecmp (s, "true") == 0;
    if (s) free (s);
}

Field * SFBool::clone () {
    SFBool * f = new SFBool (NULL, NULL, NULL);
    f->copyFrom (this);
    f->v = v;
    return (f);
}


SFTmp::SFTmp (char * type, char * name, Field * next)
    : Field (type, name, next) {
}

void SFTmp::printValue (int n) { 
    fprintf (myStderr, "SFTmp::printValue: unexpected call\n");
}

void SFTmp::encodeValue (FILE * fp) { 
    fprintf (myStderr, "SFTmp::encodeValue: unexpected call\n");
}

void SFTmp::parseValue (Scene * scene, Tokenizer *t) {
    fprintf (myStderr, "SFTmp::parseValue: unexpected call\n");
}

Field * SFTmp::clone () {
    fprintf (myStderr, "SFTmp::clone: unexpected call\n");
    SFTmp * f = new SFTmp (NULL, NULL, NULL);
    f->copyFrom (this);
    return (f);
}

SFInt32::SFInt32 (char * type, char * name, Field * next)
    : Field (type, name, next) {
    v = -1;
}

void SFInt32::printValue (int n) { printf ("%d", v); }

void SFInt32::encodeValue (FILE * fp) { write (fp, v); }

void SFInt32::parseValue (Scene * scene, Tokenizer *t) {  v = t->getNextInt (); }

Field * SFInt32::clone () {
    SFInt32 * f = new SFInt32 (NULL, NULL, NULL);
    f->copyFrom (this);
    f->v = v;
    return (f);
}

SFFloat::SFFloat (char * type, char * name, Field * next)
    : Field (type, name, next) {
    v = -1;
}

void SFFloat::printValue (int n) { printf ("%g", v); }

void SFFloat::encodeValue (FILE * fp) { 
    write (fp, v);
}

void SFFloat::parseValue (Scene * scene, Tokenizer *t) {  
    v = t->getNextFloat ();
}

Field * SFFloat::clone () {
    SFFloat * f = new SFFloat (NULL, NULL, NULL);
    f->copyFrom (this);
    f->v = v;
    return (f);
}

SFTime::SFTime (char * type, char * name, Field * next)
    : Field (type, name, next) {
    v = -1;
}

void SFTime::printValue (int n) { printf ("%g", v); }

void SFTime::encodeValue (FILE * fp) { 
    int tmp = int (v*1000);
    write (fp, tmp);
}

void SFTime::parseValue (Scene * scene, Tokenizer *t) {  
    v = t->getNextFloat ();
}

Field * SFTime::clone () {
    SFTime * f = new SFTime (NULL, NULL, NULL);
    f->copyFrom (this);
    f->v = v;
    return (f);
}

SFVec2f::SFVec2f (char * type, char * name, Field * next)
    : Field (type, name, next) {
    x = y = -1;
}
void SFVec2f::printValue (int n) { printf ("%g %g", x, y); }

void SFVec2f::encodeValue (FILE * fp) { 
    write (fp, x); write (fp, y);
}

void SFVec2f::parseValue (Scene * scene, Tokenizer *t) {  
    x = t->getNextFloat ();
    y = t->getNextFloat ();
}

Field * SFVec2f::clone () {
    SFVec2f * f = new SFVec2f (NULL, NULL, NULL);
    f->copyFrom (this);
    f->x = x;
    f->y = y;
    return (f);
}

int SFVec2f::findIndex (char * name, bool isArray) {
    if (isArray) { return -1; }
    if (strcmp (name, "x") == 0) {
        return 1;
    } else if (strcmp (name, "y") == 0) {
        return 2;
    }
    return (-1);
}



SFVec3f::SFVec3f (char * type, char * name, Field * next)
    : Field (type, name, next) {
    x = y = z = -1;
}
void SFVec3f::printValue (int n) { printf ("%g %g %g", x, y, z); }

void SFVec3f::encodeValue (FILE * fp) { 
    write (fp, x); write (fp, y); write (fp, z);
}

void SFVec3f::parseValue (Scene * scene, Tokenizer *t) {  
    x = t->getNextFloat ();
    y = t->getNextFloat ();
    z = t->getNextFloat ();
}

Field * SFVec3f::clone () {
    SFVec3f * f = new SFVec3f (NULL, NULL, NULL);
    f->copyFrom (this);
    f->x = x;
    f->y = y;
    f->z = z;
    return (f);
}

int SFVec3f::findIndex (char * name, bool isArray) {
    if (isArray) { return -1; }
    if (strcmp (name, "x") == 0) {
        return 1;
    } else if (strcmp (name, "y") == 0) {
        return 2;
    } else if (strcmp (name, "z") == 0) {
        return 3;
    }
    return (-1);
}

// SFRotation
SFRotation::SFRotation (char * type, char * name, Field * next)
    : Field (type, name, next) {
    x = y = z = a = -1;
}
void SFRotation::printValue (int n) { printf ("%g %g %g %g", x, y, z, a); }

void SFRotation::encodeValue (FILE * fp) { 
    write (fp, x); write (fp, y); write (fp, z); write (fp, a);
}

void SFRotation::parseValue (Scene * scene, Tokenizer *t) {  
    x = t->getNextFloat ();
    y = t->getNextFloat ();
    z = t->getNextFloat ();
    a = t->getNextFloat ();
}

Field * SFRotation::clone () {
    SFRotation * f = new SFRotation (NULL, NULL, NULL);
    f->copyFrom (this);
    f->x = x;
    f->y = y;
    f->z = z;
    f->a = a;
    return (f);
}

int SFRotation::findIndex (char * name, bool isArray) {
    if (isArray) { return -1; }
    if (strcmp (name, "x") == 0) {
        return 1;
    } else if (strcmp (name, "y") == 0) {
        return 2;
    } else if (strcmp (name, "z") == 0) {
        return 3;
    } else if (strcmp (name, "a") == 0) {
        return 4;
    }
    return (-1);
}

// SFString
SFString::SFString (char * type, char * name, Field * next)
    : Field (type, name, next) {
    v = NULL;
}

SFString::~SFString () { if (v) free (v); if(m_fn) free(m_fn); }

void SFString::printValue (int n) { 
    printf ("\"%s\"", v ? v : ""); 
}

static void writeUTF8 (FILE * fp, char * v) {
    char key [LOCALE_KEY_LEN];
    char msg [LOCALE_MSG_LEN];
    
    key[0] = msg[0] = 0;
    
    if (v) {
        int nb = LocaleManager::split (v);
        if (nb == 0) { // not a localized string
            LocaleManager::writeUTF8 (fp, v);
        } else if (nb == 1 || nb == 2) {
            int locID = LocaleManager::getEntryID (LocaleManager::key);
            if (locID == -1) {
                fprintf (stderr, "warning: locale string not found for %s\n", LocaleManager::key);
                LocaleManager::writeUTF8 (fp, LocaleManager::key);
            } else {
                fprintf (fp, "%c%c%c%c", 255, locID / 256, locID % 256, 0);
            }
        } else {
            fprintf (stderr, "unexpected error in localized string splitting\n");
        }
    } else { // empty string but have to write a 0 anyway
        fprintf (fp, "%c", 0);
    }
}

// static void writeUTF8_aux (FILE * fp, char * v) {
//     // 233 = 11101001 = 110xxxxx + 10 yyyyyy => 11000011 + 10101001 = C3A9
//     //fwrite (v, 1, strlen (v), fp);
//     for (unsigned int i = 0; i < strlen (v); i++) {
//         switch (v[i]) {
//             // convert some DOS chars commonly used
//         case 0xE9: fprintf (fp, "%c%c", 0xC3, 0xA9); break; // e cute
//         case 0xE8: fprintf (fp, "%c%c", 0xC3, 0xA8); break; // e grave
//         case 0xE7: fprintf (fp, "%c%c", 0xC3, 0xA7); break; // c cedil
//         case 0xE0: fprintf (fp, "%c%c", 0xC3, 0xA0); break; // a grave
//         case 0xEF: fprintf (fp, "%c%c", 0xC3, 0xAF); break; // i trema
//         case 0xEA: fprintf (fp, "%c%c", 0xC3, 0xAA); break; // e circonflex
//         default:   fprintf (fp, "%c", v[i]); break;
//         }
//     }
// }

void SFString::encodeValue (FILE * fp) { 
    writeUTF8 (fp, v);
}

void SFString::parseValue (Scene * scene, Tokenizer *t) {
    m_fn = strdup(t->getFile()); 
    m_line = t->getLine ();
    v = t->getNextString (); 
    LocaleManager::addEntry (v);
}

Field * SFString::clone () {
    SFString * f = new SFString (NULL, NULL, NULL);
    f->copyFrom (this);
    f->v = v ? strdup (v) : NULL;
    return (f);
}

SFColor::SFColor (char * type, char * name, Field * next)
    : Field (type, name, next) {
    r = g = b = 0;
}

void SFColor::printValue (int n) { 
    printf ("%g %g %g", r, g, b); 
}

void SFColor::encodeValue (FILE * fp) { 
    fprintf (fp, "%c%c%c", char(r*255), char (g*255), char (b*255));
}

void SFColor::parseValue (Scene * scene, Tokenizer *t) {  
    r = t->getNextFloat ();
    g = t->getNextFloat ();
    b = t->getNextFloat ();
}

Field * SFColor::clone () {
    SFColor * f = new SFColor (NULL, NULL, NULL);
    f->copyFrom (this);
    f->r = r;
    f->g = g;
    f->b = b;
    return (f);
}

int SFColor::findIndex (char * name, bool isArray) {
    if (isArray) { return -1; }
    if (strcmp (name, "r") == 0) {
        return 1;
    } else if (strcmp (name, "g") == 0) {
        return 2;
    } else if (strcmp (name, "b") == 0) {
        return 3;
    } else if (strcmp (name, "hex") == 0) {
        return HEX_IDX;
    }
    return (-1);
}

SFNode::SFNode (char * type, char * name, Field * next)
    : Field (type, name, next) {
    m_node = NULL;
}

void SFNode::printValue (int n) { 
    if (m_node) {
        m_node->print (n+4, false, false);
        //printf ("\n"); 
    } else {
        printf ("NULL\n");
    }
}

void SFNode::encodeValue (FILE * fp) { 
    if (m_node) {
        m_node->encodeValue (fp);
    } else {
        fprintf (fp, "%c", 0);
    }
}

int SFNode::encodeSpecial (FILE * fp, bool verbose) { 
    int total = 0;
    if (m_next) {
        total = m_next->encodeSpecial (fp, verbose);
    }
    if (m_node) {
        total += m_node->encodeSpecial (fp, verbose);
    }
    return (total);
}

void SFNode::parseValue (Scene * scene, Tokenizer *t) {  
    m_node = scene->parseNode (t, NULL);
}

Field * SFNode::clone () {
    SFNode * f = new SFNode (NULL, NULL, NULL);
    f->copyFrom (this);
    f->m_node = m_node;
    return (f);
}

Field * SFNode::findField (const char * name) {
    if (m_node) {
        return m_node->findField (name);
    }
    return (NULL);
}

int SFNode::findIndex (const char * name, bool isArray) {
    if (isArray) { return -1; }
    if (m_node) {
        //fprintf (myStderr, "SFNode::findIndex: is a node %s\n", m_node->m_name);
        return m_node->findFieldIdx (name);
    }
    return (-1);
}

MFField::MFField (char * type, char * name, Field * next) 
    : Field (type, name, next) {
    m_size = 0;
} 

int MFField::findIndex (char * name, bool isArray) {
    if (!isArray) { 
        if (strcmp (name, "length") == 0) {
            return LENGTH_IDX;
        }
    }
    return -1;
}

MFNode::MFNode (char * type, char * name, Field * next)
    : MFField (type, name, next) {
    m_node = NULL;
}

void MFNode::printValue (int n) { 
    if (m_size == 0) {
        printf ("NULL");
    } else {
        printf ("[\n");
        m_node->print (n+4);
        spaces (n);
        printf ("]");
    }
}

void MFNode::encodeValue (FILE * fp) { 
    fprintf (fp, "%c", m_size);
    if (m_size > 0) {
        m_node->encodeValue (fp);
    } 
}

int MFNode::encodeSpecial (FILE * fp, bool verbose) { 
    int total = 0;
    if (m_next) {
        total = m_next->encodeSpecial (fp, verbose);
    }
    if (m_size > 0) {
        total += m_node->encodeSpecial (fp, verbose);
    }
    return (total);
}

void MFNode::parseValue (Scene * scene, Tokenizer *t) {  
    int counter = 0;
    if (t->check ('[')) {
        while (!t->_EOF() && !t->check(']')) {
            Node * node = scene->parseNode (t, m_node);
            if (node) { 
                m_node = node; 
                m_size++;
            } else {
                counter++;
                if (counter > 2) {
                    fprintf (myStderr, "%s:%d: syntax error, node expected\n", t->getFile(), t->getLine ());
                    exit (1);
                }
            }
        }
    } else {
        m_node = scene->parseNode (t, m_node);
        if (m_node) { // can be null if no node ("NULL" token)
            m_size++;
        }
    }
}

Field * MFNode::clone () {
    MFNode * f = new MFNode (NULL, NULL, NULL);
    f->copyFrom (this);
    f->m_node = NULL;
    return (f);
}


MFInt32::MFInt32 (char * type, char * name, Field * next)
    : MFField (type, name, next) {
    m_size = 0;
}

void MFInt32::printValue (int n) { 
    if (m_size > 0) {
        printf ("[ ");
        for (int i = 0; i < m_size; i++) {
            printf ("%d ", m_array[i]);
        } 
        printf ("]");
    }
}

void MFInt32::encodeValue (FILE * fp) { 
    encodeSize (fp);
    if (m_size > 0) {
        for (int i = 0; i < m_size; i++) {
            write (fp, m_array[i]);
        }
    } 
}

void MFInt32::parseValue (Scene * scene, Tokenizer *t) {  
    m_size = 0;
    if (t->check ('[')) {
        while (!t->_EOF() && !t->check(']')) {
            m_array [m_size++] = t->getNextInt();
        }
    } else {
        m_array [0] = t->getNextInt();
        m_size = 1;
    }
}

Field * MFInt32::clone () {
    MFInt32 * f = new MFInt32 (NULL, NULL, NULL);
    f->copyFrom (this);
    return (f);
}

MFFloat::MFFloat (char * type, char * name, Field * next)
    : MFField (type, name, next) {
    m_size = 0;
}

void MFFloat::printValue (int n) { 
    if (m_size == 0) {
        printf ("NULL");
    } else {
        printf ("[ ");
        for (int i = 0; i < m_size; i++) {
            printf ("%g ", m_array[i]);
        } 
        printf ("]");
    }
}

void MFField::encodeSize (FILE * fp) {
    int size = m_size;
    if (size > 255) {
        int n = size / 255;
        fprintf (fp, "%c%c", 255, n);
        size -= 255*n;
    }
    fprintf (fp, "%c", size);
}

void MFFloat::encodeValue (FILE * fp) { 
    encodeSize (fp);
    if (m_size > 0) {
        for (int i = 0; i < m_size; i++) {
            write (fp, m_array[i]);
        }
    } 
}

void MFFloat::parseValue (Scene * scene, Tokenizer *t) {  
    m_size = 0;
    if (t->check ('[')) {
        while (!t->_EOF() && !t->check(']')) {
            m_array [m_size++] = t->getNextFloat();
        }
    } else {
        m_array [0] = t->getNextFloat();
        m_size = 1;
    }
}

Field * MFFloat::clone () {
    MFFloat * f = new MFFloat (NULL, NULL, NULL);
    f->copyFrom (this);
    return (f);
}

MFVec2f::MFVec2f (char * type, char * name, Field * next)
    : MFField (type, name, next) {
    m_size = 0;
}

void MFVec2f::printValue (int n) { 
    if (m_size == 0) {
        printf ("NULL");
    } else {
        printf ("[ ");
        for (int i = 0; i < m_size; i++) {
            printf ("%g %g ", m_array[i*2] , m_array[i*2+1]);
        } 
        printf ("]");
    }
}

void MFVec2f::encodeValue (FILE * fp) { 
    encodeSize (fp);
    if (m_size > 0) {
        for (int i = 0; i < m_size; i++) {
            write (fp, m_array[i*2]);
            write (fp, m_array[i*2+1]);
        }
    } 
}

void MFVec2f::parseValue (Scene * scene, Tokenizer *t) {  
    m_size = 0;
    if (t->check ('[')) {
        while (!t->_EOF() && !t->check(']')) {
            m_array [m_size*2] = t->getNextFloat();
            m_array [m_size*2+1] = t->getNextFloat();
            m_size++;
        }
    } else {
        m_array [0] = t->getNextFloat();
        m_array [1] = t->getNextFloat();
        m_size = 1;
    }
}
Field * MFVec2f::clone () {
    MFVec2f * f = new MFVec2f (NULL, NULL, NULL);
    f->copyFrom (this);
    return (f);
}

int MFVec2f::findIndex (char * name, bool isArray) {
    if (!isArray) { 
        if (strcmp (name, "length") == 0) {
            return LENGTH_IDX;
        }
        return -1; 
    }
    if (strcmp (name, "x") == 0) {
        return 1;
    } else if (strcmp (name, "y") == 0) {
        return 2;
    }
    return (-1);
}





MFVec3f::MFVec3f (char * type, char * name, Field * next)
    : MFField (type, name, next) {
    m_size = 0;
}

void MFVec3f::printValue (int n) { 
    if (m_size == 0) {
        printf ("NULL");
    } else {
        printf ("[ ");
        for (int i = 0; i < m_size; i++) {
            printf ("%g %g %g ", m_array[i*3] , m_array[i*3+1], m_array[i*3+2]);
        } 
        printf ("]");
    }
}

void MFVec3f::encodeValue (FILE * fp) { 
    encodeSize (fp);
    if (m_size > 0) {
        for (int i = 0; i < m_size; i++) {
            write (fp, m_array[i*3]);
            write (fp, m_array[i*3+1]);
            write (fp, m_array[i*3+2]);
        }
    } 
}

void MFVec3f::parseValue (Scene * scene, Tokenizer *t) {  
    m_size = 0;
    if (t->check ('[')) {
        while (!t->_EOF() && !t->check(']')) {
            m_array [m_size*3] = t->getNextFloat();
            m_array [m_size*3+1] = t->getNextFloat();
            m_array [m_size*3+2] = t->getNextFloat();
            m_size++;
        }
    } else {
        m_array [0] = t->getNextFloat();
        m_array [1] = t->getNextFloat();
        m_array [2] = t->getNextFloat();
        m_size = 1;
    }
}
Field * MFVec3f::clone () {
    MFVec3f * f = new MFVec3f (NULL, NULL, NULL);
    f->copyFrom (this);
    return (f);
}

int MFVec3f::findIndex (char * name, bool isArray) {
    if (!isArray) { 
        if (strcmp (name, "length") == 0) {
            return LENGTH_IDX;
        }
        return -1; 
    }
    if (strcmp (name, "x") == 0) {
        return 1;
    } else if (strcmp (name, "y") == 0) {
        return 2;
    } else if (strcmp (name, "z") == 0) {
        return 3;
    }
    return (-1);
}

// MFRotation
MFRotation::MFRotation (char * type, char * name, Field * next)
    : MFField (type, name, next) {
    m_size = 0;
}

void MFRotation::printValue (int n) { 
    if (m_size == 0) {
        printf ("NULL");
    } else {
        printf ("[ ");
        for (int i = 0; i < m_size; i++) {
            printf ("%g %g %g %g", m_array[i*4] , m_array[i*4+1], m_array[i*4+2], m_array[i*4+3]);
        } 
        printf ("]");
    }
}

void MFRotation::encodeValue (FILE * fp) { 
    encodeSize (fp);
    if (m_size > 0) {
        for (int i = 0; i < m_size; i++) {
            write (fp, m_array[i*4]);
            write (fp, m_array[i*4+1]);
            write (fp, m_array[i*4+2]);
            write (fp, m_array[i*4+3]);
        }
    } 
}

void MFRotation::parseValue (Scene * scene, Tokenizer *t) {  
    m_size = 0;
    if (t->check ('[')) {
        while (!t->_EOF() && !t->check(']')) {
            m_array [m_size*4] = t->getNextFloat();
            m_array [m_size*4+1] = t->getNextFloat();
            m_array [m_size*4+2] = t->getNextFloat();
            m_array [m_size*4+3] = t->getNextFloat();
            m_size++;
        }
    } else {
        m_array [0] = t->getNextFloat();
        m_array [1] = t->getNextFloat();
        m_array [2] = t->getNextFloat();
        m_array [3] = t->getNextFloat();
        m_size = 1;
    }
}
Field * MFRotation::clone () {
    MFRotation * f = new MFRotation (NULL, NULL, NULL);
    f->copyFrom (this);
    return (f);
}

int MFRotation::findIndex (char * name, bool isArray) {
    if (!isArray) { 
        if (strcmp (name, "length") == 0) {
            return LENGTH_IDX;
        }
        return -1; 
    }
    if (strcmp (name, "x") == 0) {
        return 1;
    } else if (strcmp (name, "y") == 0) {
        return 2;
    } else if (strcmp (name, "z") == 0) {
        return 3;
    } else if (strcmp (name, "a") == 0) {
        return 4;
    }
    return (-1);
}



// MFColor
MFColor::MFColor (char * type, char * name, Field * next)
    : MFField (type, name, next) {
    m_size = 0;
}

void MFColor::printValue (int n) { 
    if (m_size == 0) {
        printf ("NULL");
    } else if (m_size > 0) {
        printf ("[ ");
        for (int i = 0; i < m_size; i++) {
            printf ("%g %g %g ", m_array[i*3], m_array[i*3+1], m_array[i*3+2]);
        } 
        printf ("]");
    }
}

void MFColor::encodeValue (FILE * fp) { 
    encodeSize (fp);
    if (m_size > 0) {
        for (int i = 0; i < m_size; i++) {
            fprintf (fp, "%c%c%c",
                     char (m_array[i*3]*255),
                     char (m_array[i*3+1]*255),
                     char (m_array[i*3+2]*255));
        }
    } 
}

void MFColor::parseValue (Scene * scene, Tokenizer *t) {  
    m_size = 0;
    if (t->check ('[')) {
        bool ok = true;
        while (!t->_EOF () && !t->check (']') && ok) {
            m_array [m_size*3] = t->getNextFloat (&ok, NULL);
            if (ok) {
                m_array [m_size*3+1] = t->getNextFloat (&ok, NULL);
                if (ok) {
                    m_array [m_size*3+2] = t->getNextFloat (&ok, NULL);
                    m_size++;
                }
            }
        }
        if (!ok) {
            fprintf (myStderr, "%s:%d: syntax error: cannot parse color value #%d\n", t->getFile(), t->getLine (), m_size); 
        }
    } else {
        m_array [0] = t->getNextFloat();
        m_array [1] = t->getNextFloat();
        m_array [2] = t->getNextFloat();
        m_size = 1;
    }
}

Field * MFColor::clone () {
    MFColor * f = new MFColor (NULL, NULL, NULL);
    f->copyFrom (this);
    return (f);
}

int MFColor::findIndex (char * name, bool isArray) {
    if (!isArray) { 
        if (strcmp (name, "length") == 0) {
            return LENGTH_IDX;
        }
        return -1; 
    }
    if (strcmp (name, "r") == 0) {
        return 1;
    } else if (strcmp (name, "g") == 0) {
        return 2;
    } else if (strcmp (name, "b") == 0) {
        return 3;
    } else if (strcmp (name, "hex") == 0) {
        return HEX_IDX;
    }
    return (-1);
}

MFString::MFString (char * type, char * name, Field * next)
    : MFField (type, name, next) {
    m_size = 0;
}

char * MFString::getValue (int i) {
    return i >= 0 && i < m_size ? m_array[i] : NULL;
}

void MFString::printValue (int n) { 
    if (m_size == 0) {
        printf ("NULL");
    } else if (m_size > 0) {
        printf ("[ ");
        for (int i = 0; i < m_size; i++) {
            printf ("\"%s\" ", m_array[i]?m_array[i]:"");
        } 
        printf ("]");
    }
}

void MFString::encodeValue (FILE * fp) { 
    encodeSize (fp);
    if (m_size > 0) {
        for (int i = 0; i < m_size; i++) {
//            char * v = m_array [i];
//             if (m_array [i]) {
//                 fwrite (v, 1, strlen (v), fp);
//             }
//             fprintf (fp, "%c", 0);
            writeUTF8 (fp, m_array[i]);
        }
    }
}

void MFString::parseValue (Scene * scene, Tokenizer *t) {  
    m_size = 0;
    m_line = t->getLine ();
    if (t->check ('[')) {
        while (!t->_EOF() && !t->check(']')) {
            m_array [m_size] = t->getNextString();
            LocaleManager::addEntry (m_array[m_size]);
            m_size++;
        }
    } else {
        m_array [0] = t->getNextString();
        LocaleManager::addEntry (m_array[0]);
        m_size=1;
    }
}

Field * MFString::clone () {
    MFString * f = new MFString (NULL, NULL, NULL);
    f->copyFrom (this);
    return (f);
}


MFAny::MFAny (char * type, char * name, Field * next)
    : MFField (type, name, next) {
    m_size = 0;
}

char * MFAny::getValue (int i) {
    return i >= 0 && i < m_size ? m_array[i] : NULL;
}

void MFAny::printValue (int n) { 
    if (m_size == 0) {
        printf ("NULL");
    } else if (m_size > 0) {
        printf ("[ ");
        for (int i = 0; i < m_size; i++) {
            printf ("\"%s\" ", m_array[i]?m_array[i]:"");
        } 
        printf ("]");
    }
}

void MFAny::encodeValue (FILE * fp) { 
    encodeSize (fp);
    if (m_size > 0) {
        for (int i = 0; i < m_size; i++) {
        }
    }
}

void MFAny::parseValue (Scene * scene, Tokenizer *t) {  
    m_size = 0;
    m_line = t->getLine ();
    if (t->check ('[')) {
        while (!t->_EOF() && !t->check(']')) {
            m_array [m_size] = t->getNextString();
            m_size++;
        }
    } else if (t->checkToken ("NULL")) {
        m_size=0;
    }
}

Field * MFAny::clone () {
    MFAny * f = new MFAny (NULL, NULL, NULL);
    f->copyFrom (this);
    return (f);
}

Field * createField (char * type, char * name, Field * next) {
    if (type == NULL) {
        return (NULL);
    }
    if (strcmp (type, "SFInt32") == 0) {
        return (new SFInt32 (type, name, next));
    } else if (strcmp (type, "SFBool") == 0) {
        return (new SFBool (type, name, next));
    } else if (strcmp (type, "SFFloat") == 0) {
        return (new SFFloat (type, name, next));
    } else if (strcmp (type, "SFTime") == 0) {
        return (new SFTime (type, name, next));
    } else if (strcmp (type, "SFVec2f") == 0) {
        return (new SFVec2f (type, name, next));
    } else if (strcmp (type, "SFVec3f") == 0) {
        return (new SFVec3f (type, name, next));
    } else if (strcmp (type, "SFRotation") == 0) {
        return (new SFRotation (type, name, next));
    } else if (strcmp (type, "SFColor") == 0) {
        return (new SFColor (type, name, next));
    } else if (strcmp (type, "SFNode") == 0) {
        return (new SFNode (type, name, next));
    } else if (strcmp (type, "SFString") == 0) {
        return (new SFString (type, name, next));
    } else if (strcmp (type, "MFNode") == 0) {
        return (new MFNode (type, name, next));
    } else if (strcmp (type, "MFInt32") == 0) {
        return (new MFInt32 (type, name, next));
    } else if (strcmp (type, "MFFloat") == 0) {
        return (new MFFloat (type, name, next));
    } else if (strcmp (type, "MFVec2f") == 0) {
        return (new MFVec2f (type, name, next));
    } else if (strcmp (type, "MFVec3f") == 0) {
        return (new MFVec3f (type, name, next));
    } else if (strcmp (type, "MFRotation") == 0) {
        return (new MFRotation (type, name, next));
    } else if (strcmp (type, "MFColor") == 0) {
        return (new MFColor (type, name, next));
    } else if (strcmp (type, "MFString") == 0) {
        return (new MFString (type, name, next));
    } else if (strcmp (type, "MFAny") == 0) {
        return (new MFAny (type, name, next));
    } else if (strcmp (type, "SFTmp") == 0) {
        return (new SFTmp (type, name, next));
    } else if (strcmp (type, "SFDefined") == 0) {
        return (new SFTmp (type, name, next));
    } else {
        fprintf (myStderr, "Type not supported: '%s'\n", name);
    }
    return (NULL);
};
