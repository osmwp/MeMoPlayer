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

//  OutputDebugString
//#include "Windows.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

# define MESSAGE(...) if (debug) fprintf (stderr, __VA_ARGS__)

enum CMD_LIST {
    CMD_BUILD = 0,
    CMD_PREVIEW = 1,
    CMD_PUBLISH = 2,
    CMD_CLEAN = 3,
    CMD_INFO = 4
};

class XMLTokenizer;
class Variable;

class Scripter;

enum OD_Type {
    OD_UNKNOWN = -1,
    OD_IMAGE = 0,
    OD_AUDIO = 1,
    OD_VIDEO = 2
};

class OD {
    static int s_odNumber;
public:
    char * m_name;
    int m_id;
    OD_Type m_type;
    OD * m_next;
public:
    OD (char * name, int id, OD_Type type, OD * next);
    bool generate (Scripter * s);
    OD * getNext () { return m_next; }
    static int getOdId () { return s_odNumber; }
    static void incOdId () { s_odNumber++; }
    static int getEsiD (int odId) { return (odId+1000); }
};

class IOD {
    int m_w, m_h;
    bool m_hasODTrack;
    OD * m_odList;
public:
    IOD (int w, int h, bool hasODTrack = true);
    bool set (int w, int h, bool hasODTrack);
    bool addOD (char * name, int id, OD_Type type);
    bool generate (Scripter * s);
};

class Scripter {
protected:
    CMD_LIST m_cmd;
    int m_curBuf;
    char * m_data[2];
    char * m_basename;
    char ** m_params;
    Variable * m_variable;
    IOD * m_iod;

public:
    /** Initialize the internal members */
    Scripter (char * cmdFile, char* param [], CMD_LIST cmd);
    
    /** Free alll allocated data */
    ~Scripter ();

    void append (char * s);
protected:

    bool setField (char * name, int value);
    bool setField (char * name, bool value);
    bool setField (char * name, float value);
    bool setField (char * name, float val1, float val2);
    bool setField (char * name, float val1, float val2, float val3);
    bool setField (char * name, char * value);


    bool loadFile (char * inputFile);
    bool addIOD (int n, char ** p, XMLTokenizer & t);
    bool addMedia (int n, char ** p, XMLTokenizer & t);
    bool addImage (int n, char ** p, XMLTokenizer & t);
    bool add (XMLTokenizer & t);
    bool build (XMLTokenizer & t);
    bool preview (XMLTokenizer & t);
    bool applyCmd (FILE * fp, bool master = true);
    bool saveFile (char * outputFile);
    bool child (char * cmd);
    bool include (char * name);
    bool exec (char * cmd);
    bool parsePage (XMLTokenizer * t);
    bool parseArea (XMLTokenizer * t);

    Variable * createVariable (char ** params, int nbParams);
    void fillParams ();
    void addVar (char * s, char * name, char * deflVal); 
    void setVarValue (char * s, char * value);
    char * expandVars (char * msg);

    char * readFileContent (char * fileName, int * dataLen = NULL, int extraSize = 0);
    bool saveFileContent (char * fileName, char * data);
};


