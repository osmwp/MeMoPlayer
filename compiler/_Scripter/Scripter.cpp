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

# include "Scripter.h"
# include "XMLTokenizer.h"
//# include "ImageTool.h" 
# include "FileTool.h" 
# include "Trace.h" 
# include <stdio.h>
# include <stdlib.h>

# define EXTRA_CHUNK 40960


static char IODTemplate1 [] = "\
InitialObjectDescriptor {\n\
    objectDescriptorID 1\n\
    esDescr [\n\
        ES_Descriptor {\n\
            ES_ID 3\n\
            \n\
            decConfigDescr DecoderConfigDescriptor {\n\
                objectTypeIndication 2\n\
                streamType 3\n\
                bufferSizeDB 20\n\
                decSpecificInfo BIFSv2Config {\n\
                    isCommandStream TRUE\n\
                    pixelMetrics TRUE\n\
                    pixelWidth	%d\n\
                    pixelHeight %d\n\
                }\n\
            }\n\
            slConfigDescr SLConfigDescriptor {\n\
                useAccessUnitStartFlag TRUE\n\
                useAccessUnitEndFlag TRUE\n\
                useTimeStampsFlag TRUE\n\
                timeStampResolution 1000\n\
                timeStampLength 32\n\
            }			    \n\
        }\n\
    ]\n\
}\n\
";

static char IODTemplate2 [] = "\
InitialObjectDescriptor {\n\
    objectDescriptorID 1\n\
    esDescr [\n\
        ES_Descriptor {\n\
            ES_ID 3\n\
            \n\
            decConfigDescr DecoderConfigDescriptor {\n\
                objectTypeIndication 2\n\
                streamType 3\n\
                bufferSizeDB 20\n\
                decSpecificInfo BIFSv2Config {\n\
                    isCommandStream TRUE\n\
                    pixelMetrics TRUE\n\
                    pixelWidth	%d\n\
                    pixelHeight %d\n\
                }\n\
            }\n\
            slConfigDescr SLConfigDescriptor {\n\
                useAccessUnitStartFlag TRUE\n\
                useAccessUnitEndFlag TRUE\n\
                useTimeStampsFlag TRUE\n\
                timeStampResolution 1000\n\
                timeStampLength 32\n\
            }			    \n\
        }\n\
        ES_Descriptor {  \n\
            ES_ID 4  \n\
            OCR_ES_ID 3  \n\
            decConfigDescr DecoderConfigDescriptor {  \n\
                objectTypeIndication 1  \n\
                streamType 1  \n\
                bufferSizeDB 200  \n\
            }  \n\
            slConfigDescr SLConfigDescriptor {  \n\
                useAccessUnitStartFlag TRUE  \n\
                useAccessUnitEndFlag TRUE  \n\
                useTimeStampsFlag TRUE  \n\
                timeStampResolution 1000  \n\
                timeStampLength 32  \n\
            }  \n\
        }  \n\
    ]\n\
}\n\
";

static char ODImageTemplate [] = "\
        ObjectDescriptor {  \n\
            objectDescriptorID %d  \n\
            esdescr [  \n\
                ES_Descriptor {  \n\
                    es_id %d  \n\
                    streamPriority 16  \n\
                    decConfigDescr DecoderConfigDescriptor {  \n\
                        objectTypeIndication 108  \n\
                        streamType 4  \n\
                        upStream false  \n\
                    }\n\
                    slConfigDescr SLConfigDescriptor {  \n\
                    }  \n\
                    muxInfo muxInfo {  \n\
                        fileName \"%s\"\n\
                    }\n\
                }\n\
            ]\n\
        }\n\
";

int OD::s_odNumber = 50;

OD::OD (char * name, int id, OD_Type type, OD * next) {
    m_name = strdup (name);
    m_id = id;
    m_type = type;
    m_next = next;
}

bool OD::generate (Scripter * s) {
    char * tpl = ODImageTemplate;
    char * buffer = (char *)malloc (strlen (tpl)+1024);
    sprintf (buffer, tpl, m_id, 1000+m_id, m_name);
    s->append (buffer);
    free (buffer); // ok
}

IOD::IOD (int w, int h, bool hasODTrack) {
    set (w, h, hasODTrack);
    m_odList = NULL;
}

bool IOD::set (int w, int h, bool hasODTrack) {
    m_w = w;
    m_h = h;
    m_hasODTrack = hasODTrack;
}

bool IOD::addOD (char * name, int id, OD_Type type) {
    m_odList = new OD (name, id, type, m_odList);
}

bool IOD::generate (Scripter * s) {
    char * tpl = m_hasODTrack?IODTemplate2:IODTemplate1;
    char * tmp = (char *)malloc (strlen (tpl)+64);
    sprintf (tmp, tpl, m_w, m_h);
    s->append (tmp);
    free (tmp); // OK
    s->append ("# ODs list\nRAP AT 0 {\n    UPDATE OD [\n");
    OD * od = m_odList;
    while (od) {
        od->generate (s);
        od = od->getNext ();
    }
    s->append ("    ]\n}\n");
}

class Variable {
    char * m_switch;
    char * m_name;
    char * m_value;
    char * m_default;
    Variable * m_next;
public:
    Variable (char * s, char * name, char * dfltVal, Variable * next) {
        m_switch = s ? strdup (s) : NULL;
        m_name = name ? strdup (name) : NULL;
        m_value = NULL;
        m_next = next;
        m_default = dfltVal ? strdup (dfltVal) : NULL;;
    }
    ~Variable () {
        if (m_switch) {
            free (m_switch);
        }
        if (m_name) {
            free (m_name);
        }
        if (m_value) {
            free (m_value);
        }
        if (m_default) {
            free (m_default);
        }
    }
    char * getSwitch () { return m_switch; }

    char * getName () { return m_name; }

    char * getDefault () { return m_default; }

    void deleteAll () {
        if (m_next) {
            m_next->deleteAll ();
        }
        delete this;
    }

    void setValue (char * value) { m_value = value ? strdup (value) : NULL; }

    char * getValue () {
        if (m_value) {
            return (m_value);
        }
        if (m_default) {
            return (m_default);
        }
        return ("");
    }
    char * getValueByName (char * name) {
        if (strcmp (m_name, name) == 0) {
            return (this->getValue());
        } else if (m_next) {
            return (m_next->getValueByName (name));
        }
        return ("");
    }

    Variable * findBySwitch (char * s) {
        if (strcasecmp (m_switch, s) == 0) {
            return (this);
        } else if (m_next) {
            return (m_next->findBySwitch (s));
        }
        return (NULL);
    }
};

Scripter::Scripter (char * cmdFile, char * param[], CMD_LIST cmd ) {
    m_curBuf = 0;
    m_data[0] = m_data [1] = NULL;
    m_variable = NULL;
    m_cmd = cmd;
    m_iod = NULL;
    m_basename = strdup (cmdFile);
    for (int i = strlen (m_basename)-2; i > 0; i--) {
        if (m_basename[i] == '/') {
            m_basename[i] = '\0';
            break;
        }
    }
    MESSAGE ("==> basename of %s is %s\n", cmdFile, m_basename);
    FILE * fp = fopen (cmdFile, "r");
    m_params = param;

    if (fp) {
        applyCmd (fp);
        fclose (fp);
    } else {
        MESSAGE ("Cannot open %s for reading\n", cmdFile);
    }
}

/** Free alll allocated data */
Scripter::~Scripter () {

}

bool Scripter::loadFile (char * inputFile) {
    for (int i = 0; i < 2; i++) {
        if (m_data[i] != NULL) {
            delete [] m_data[i];
            m_data[i] = NULL;
        }
    }
    int size = 0;
    m_curBuf = 0;
    m_data[0] = readFileContent (inputFile, &size, EXTRA_CHUNK);
    if (m_data[0] == NULL) {
        fprintf (stdout, "Cannot open %s, exiting.\n", inputFile);
        exit (1);
    }
    // convert all windows '\r' to space
    char * tmp = m_data[0];
    while (*tmp) {
        if (*tmp == '\r') {
            *tmp = ' ';
        }   
        tmp++;
    }
    m_data [1] = new char [size+EXTRA_CHUNK];
    memcpy (m_data [1], m_data[0], size+EXTRA_CHUNK);
    return (true);
}

bool Scripter::saveFile (char * outputFile) {
    if (m_data[m_curBuf] == NULL) {
        fprintf (stdout, "set: a file must be loaded first!\n");
        return (false);
    }
    if (outputFile == NULL || outputFile== '\0') {
        return (false);
    }
    FILE * fp = fopen (outputFile, "wb");
    if (fp == NULL) {
        fprintf(stdout, "saveFile: Cannot open file %s\n", outputFile);
        return (false);
    }
    int size = strlen (m_data[m_curBuf]);
    fwrite (m_data[m_curBuf], 1, size, fp);
    fclose (fp);
}

void Scripter::addVar (char * s, char * name, char * deflVal) {
    m_variable = new Variable (s, name, deflVal, m_variable);
}

void Scripter::setVarValue (char * s, char * value) {
    Variable * v = m_variable ? m_variable->findBySwitch (s) : NULL;
    if (v) {
        v->setValue (value);
    }
}

char * Scripter::expandVars (char * cmd) {
    char buffer[1024*40];
    char varName [1024];
    char * tmp = buffer;
    for (; *cmd; cmd++) {
        if (*cmd == '$') {
            cmd++;
            if (*cmd >= '0' && *cmd <= '9') {
                char * param = m_params [*cmd-'0'];
                if (param) {
                    strcpy (tmp, param);
                    tmp += strlen (param);
                } else {
                    ERROR ("No value for var %d\n", *cmd-'0');
                }
            } else if (*cmd== '(') {
                // find the closing parent
                int i;
                for (i = 0, cmd++; *cmd != '\0' && *cmd != ')'; i++, cmd++) {
                    varName[i] = *cmd;
                }
                if (*cmd == ')') {
                    char * value = NULL;
                    varName[i] = '\0';
                    //cmd++;
                    MESSAGE ("DEBUG:Got variable %s\n", varName); 
                    if (strcmp (varName, "base") == 0) {
                        strcpy (tmp, m_basename);
                        tmp += strlen (value);
                    } else if (m_variable != NULL) {
                        char * value = m_variable->getValueByName (varName);
                        if (value) {
                            strcpy (tmp, value);
                            tmp += strlen (value);
                        } else {
                            MESSAGE ("No value defined for %s\n", varName); 
                        }
                    } else {
                        ERROR ("Cannot find var for '%s'\n", varName);
                    }
                } else {
                    ERROR ("Cannot extract var name for '%s'\n", cmd);
                }
            } else {
                ERROR ("Cannot expand var for '%s'\n", cmd);
            }
        } else {
            *tmp++ = *cmd;
        }
    }
    *tmp = '\0';
    return (strdup (buffer));
}

bool Scripter::child (char * cmd) {
    char * buffer = expandVars (cmd);
    fprintf (stdout, "executing %s\n", buffer);
    FILE * fp = popen (buffer, "r");
    if (fp) {
        MESSAGE ("reading command from child\n");
        applyCmd (fp, false);
        pclose (fp);
    }
    free (buffer); //OK
}

bool Scripter::include (char * name) {
    char * buffer = expandVars (name);
    fprintf (stdout, "including %s\n", buffer);
    FILE * fp = fopen (buffer, "r");
    if (fp) {
        MESSAGE ("reading command from include file %s\n", buffer);
        applyCmd (fp, false);
        fclose (fp);
    } else { 
        fprintf (stdout, "cannot open include file %s\n", buffer);
    }
    free (buffer); //ok
}

bool Scripter::exec (char * cmd) {
    char * buffer = expandVars (cmd);
    fprintf (stdout, "executing %s\n", buffer);
    system (buffer);
    free (buffer); // ok
}

Variable * Scripter::createVariable (char ** params, int nbParams) {
    char * name = NULL, * def = NULL, * s = NULL;
    for (int i = 0; i < nbParams*2; i+= 2) {
        //MESSAGE ("createVariable: %d:%s=%s\n", i/2, params[i], params[i+1]);
        if (strcasecmp (params[i], "name") == 0) {
            name = params[i+1];
        } else if (strcasecmp (params[i], "default") == 0) {
            def = params[i+1];
        } else if (strcasecmp (params[i], "switch") == 0) {
            s = params[i+1];
        } else if (strcasecmp (params[i], "type") == 0) {
            continue;
        } else {
            ERROR ("Unknown attribute: '%s'\n", params[i]); 
        }
    }
    m_variable = new Variable (s, name, def, m_variable);
    //MESSAGE ("createVariable: done\n");
    return (m_variable);
}

void smartShift (char ** dst, int offset) {
    char ** src = dst+offset;
    while (*src != NULL) {
        *dst++ = *src++;
    }
    *dst= NULL;
}

void Scripter::fillParams () {
    //MESSAGE ("Scripter::fillParams: %p\n", m_variable);
    if (m_variable == NULL) {
        return;
    }
    int j = 0;
    for (int i = 0; m_params[i] != NULL;) {
        //MESSAGE ("Scripter::fillParams: checking %d/%d = %s\n", i, j++, m_params[i]);
        if (m_params[i][0] == '-') {
            Variable * v = m_variable->findBySwitch (m_params[i]+1);
            if (v) {
                v->setValue (m_params[i+1]);
                //MESSAGE ("Setting variable %s with %s\n", v->getName(), m_params[i+1]);
                //MESSAGE ("Scripter::fillParams: smartShift start\n");
                smartShift (&m_params[i], 2);
                //MESSAGE ("Scripter::fillParams: smartShift done\n");
            } else {
                i++;
            }
        } else {
            i++;
        }
    }
    MESSAGE ("Scripter::fillParams: done\n");
}

bool Scripter::parseArea (XMLTokenizer * t) {
    char ** params;
    int nbParams;
    bool closing;
    char * tag;
    while (true) {
        params = t->getNextTag (&tag, &nbParams, &closing);
        if (tag == NULL) {
            break;
        }
        if (strcasecmp (tag, "area") == 0 && closing) {
            break;
        } else if (strcasecmp (tag, "param") == 0) {
            Variable * v = createVariable (params, nbParams);
            char * tmp = t->getCData ();
            if (m_cmd == CMD_INFO) {
                if (v->getDefault()) {
                    fprintf (stdout, "    -%s %s : %s (optional default value: %s)\n", 
                             v->getSwitch(), v->getName(), tmp, v->getDefault());
                } else {
                    fprintf (stdout, "    -%s %s : %s (required)\n", 
                             v->getSwitch(), v->getName(), tmp);
                }
            }
            t->getClosingTag ("param");
        }
    }
    return (true);
}

bool Scripter::parsePage (XMLTokenizer * t) {
    char ** params;
    int nbParams;
    bool closing;
    char * tag;
    while (true) {
        params = t->getNextTag (&tag, &nbParams, &closing);
        if (strcasecmp (tag, "page") == 0 && closing) {
            break;
        } else if (strcasecmp (tag, "area") == 0) {
            parseArea (t);
        } else if (strcasecmp (tag, "example") == 0) {
            char * tmp = t->getCData ();
            if (m_cmd == CMD_INFO && tmp) {
                char * buffer = expandVars (tmp);
                fprintf (stdout, "\nexample: %s\n", buffer);
                free (buffer); // ok
            }
            t->getClosingTag ("example");
        } else if (strcasecmp (tag, "desc") == 0) {
            char * tmp = t->getCData ();
            if (m_cmd == CMD_INFO && tmp) {
                char * buffer = expandVars (tmp);
                fprintf (stdout, "%s\n", buffer);
                free (buffer); // ok
            }
            t->getClosingTag ("desc");
        }
    }
    MESSAGE ("\nScripter::parsePage: calling fillParams\n\n");
    fillParams ();

    if (m_cmd == CMD_INFO) {
        exit (0);
    }
}

bool Scripter::addIOD (int n, char ** p, XMLTokenizer & t) {
    int i = 0;
    int w = 240, h = 320;
    bool hasODTrack = true;
    while (i < n) {
        if (strcasecmp (p[i*2], "size") == 0) {
            char * buffer = expandVars (p[i*2+1]);
            if (sscanf (buffer, "%d %d", &w, &h) != 2) {
                fprintf (stdout, "bad value for 'size' (%s), line %d\n", buffer, t.getLine());
            }
            free (buffer); // ok
        } else if (strcasecmp (p[i*2], "hasOdTrack") == 0) {
            char * buffer = expandVars (p[i*2+1]);
            hasODTrack = strcasecmp (buffer, "true") == 0;
            free (buffer); // ok
        }
        i++;
    }
    if (m_iod == NULL) {
        m_iod = new IOD (w, h, hasODTrack);
    } else {
        m_iod->set (w, h, hasODTrack);
    }
}

bool Scripter::addImage (int n, char ** p, XMLTokenizer & t) {
    if (m_iod == NULL) {
        m_iod = new IOD (240, 320);
    } 
    char * name = NULL;
    char * field = NULL;
    int i = 0;
    while (i < n) {
        if (strcasecmp (p[i*2], "name") == 0) {
            name = expandVars (p[i*2+1]);
        } else if (strcasecmp (p[i*2], "field") == 0) {
            field = expandVars (p[i*2+1]);
        } else {
            fprintf (stdout, "addmedia: unknown parameter %s", p[i*2]);
        }
        i++;
    }
    if (name != NULL && *name !='\0' && field != NULL) {
        char buffer [16];
        int id = OD::getOdId ();
        sprintf (buffer, "[\"od:%d\"]", id);
        if (setField (field, buffer)) {
            m_iod->addOD (name, id, OD_IMAGE);
            OD::incOdId ();
        } else {
            fprintf (stdout, "addImage: cannot find %s", name);
        }
    } else {
        fprintf (stdout, "addImage: bad parameters for image name(%s) or field (%s)\n", name, field);
    }

    if (name) { free (name); } // ok
    if (field) { free (field); } //ok
}

bool Scripter::addMedia (int n, char ** p, XMLTokenizer & t) {
    if (m_iod == NULL) {
        m_iod = new IOD (240, 320);
    } 
    char * name = NULL;
    char * typeName = NULL;
    OD_Type type = OD_UNKNOWN;
    int id = -1;
    int i = 0;
    while (i < n) {
        if (strcasecmp (p[i*2], "name") == 0) {
            name = expandVars (p[i*2+1]);
        } else if (strcasecmp (p[i*2], "type") == 0) {
            typeName = expandVars (p[i*2+1]);
            if (strcasecmp (typeName, "image") == 0) {
                type = OD_IMAGE;
            } else if (strcasecmp (typeName, "audio") == 0) {
                type = OD_AUDIO;
            } else if (strcasecmp (typeName, "video") == 0) {
                type = OD_VIDEO;
            } else {
                fprintf (stdout, "addmedia: unknwon type %s\n", typeName);
            }
        } else if (strcasecmp (p[i*2], "id") == 0) {
            char * buffer = expandVars (p[i*2+1]);
            if (sscanf (buffer, "%d", &id) != 1) {
                fprintf (stdout, "bad value for 'id' (%s), line %d\n", buffer, t.getLine());
            }            
            free (buffer); // ok
        }
        i++;
    }
    if (name != NULL && type != OD_UNKNOWN) {
        m_iod->addOD (name, id, type);
    } else {
        fprintf (stdout, "addmedia: bad parameters for name(%s) or type (%s)\n", name, typeName);
    }
    if (name) { free (name); } // ok
    if (typeName) { free (typeName); } // ok
}

bool Scripter::add (XMLTokenizer & t) {
}

bool Scripter::build (XMLTokenizer & t) {
    char ** params;
    int nbParams;
    bool closing;
    char * tag;
    
    while (1) {
        params = t.getNextTag (&tag, &nbParams, &closing);
        if (tag == NULL) {
            break;
        }
        MESSAGE ("build: got tag:%s\n", tag);
        if (strcasecmp (tag, "set") == 0) {
            if (nbParams == 1 && strcasecmp (params[0], "field") == 0) {
                char * tmp = t.getCData ();
                if (tmp != NULL) {
                    char * buffer = expandVars (tmp);
                    MESSAGE ("setting %s with %s\n", params[1], buffer);
                    setField (params[1], buffer);
                    free (buffer); // ok
                } else {
                    MESSAGE ("setting %s with \"\"\n", params[1], tmp);
                    setField (params[1], "");
                }
            } else {
                fprintf (stdout, "missing parameter tag set, line %d\n", t.getLine());
            }
            t.getClosingTag ("set");
        } else if (strcasecmp (tag, "load") == 0) {
            if (nbParams == 1 && strcasecmp (params[0], "name") == 0) {
                MESSAGE ("loading %s\n", params[1]);
                loadFile (params[1]);
            } else {
                fprintf (stdout, "missing parameter for tag load, line %d\n", t.getLine());
            }
        } else if (strcasecmp (tag, "include") == 0) {
            if (nbParams == 1 && strcasecmp (params[0], "name") == 0) {
                MESSAGE ("including %s\n", params[1]);
                include (params[1]);
            } else {
                fprintf (stdout, "missing parameter for tag include, line %d\n", t.getLine());
            }
        } else if (strcasecmp (tag, "save") == 0) {
            if (nbParams == 1 && strcasecmp (params[0], "name") == 0) {
                char * buffer = expandVars (params[1]);
                MESSAGE ("saving %s\n", buffer);
                if (m_iod) {
                    //m_iod->generate (this);
                }
                saveFile (buffer);
                free (buffer); //
            } else {
                fprintf (stdout, "missing parameter for tag save, line %d\n", t.getLine());
            }
        } else if (strcasecmp (tag, "child") == 0) {
            if (nbParams == 1 && strcasecmp (params[0], "exe") == 0) {
                MESSAGE ("importing from child cmd %s\n", params[1]);
                child (params[1]);
            } else {
                fprintf (stdout, "missing parameter exe for tag child, line %d\n", t.getLine());
            }
        } else if (strcasecmp (tag, "scene") == 0) {
            fprintf (stdout, "adding IOD\n");
            addIOD (nbParams, params, t);
        } else if (strcasecmp (tag, "addmedia") == 0) {
            fprintf (stdout, "adding media\n");
            addMedia (nbParams, params, t);
        } else if (strcasecmp (tag, "addimage") == 0) {
            fprintf (stdout, "adding image\n");
            addImage (nbParams, params, t);
        } else if (strcasecmp (tag, "add") == 0) {
            add (t);
        } else if (strcasecmp (tag, "exec") == 0) {
            char * tmp = t.getCData ();
            if (tmp != NULL) {
                exec (tmp);
            } else {
                fprintf (stdout, "missing CData for tag exec, line %d\n", t.getLine());
            }
            t.getClosingTag ("exec");
        } else if (strcasecmp (tag, "build") == 0 && closing) {
            break;
        } else {
            fprintf (stdout, "unknown tag: %s, line %d\n", tag, t.getLine());
        }
    }
}


bool Scripter::preview (XMLTokenizer & t) {
    char ** params;
    int nbParams;
    bool closing;
    char * tag;
    
    char * tmp = t.getCData ();
    if (tmp != NULL) {
        exec (tmp);
    } else {
        fprintf (stdout, "missing CData for tag exec, line %d\n", t.getLine());
    }
    return (t.getClosingTag ("preview"));
}

bool Scripter::applyCmd (FILE * fp, bool master) {
    char ** params;
    int nbParams;
    bool closing;
    char * tag;
    XMLTokenizer t (fp);
    MESSAGE ("applyCmd: start\n");
    if (master) {
        params = t.getNextTag (&tag, &nbParams, &closing);
        if (strcasecmp (tag, "genMP4") != 0) {
            fprintf (stdout, "<genMP4> expected\n");
            return (false);
        }
    }

    while (true) {
        params = t.getNextTag (&tag, &nbParams, &closing);
        if (tag == NULL) {
            break;
        }
        MESSAGE ("applyCmd: got tag:%s\n", tag);
        if (strcasecmp (tag, "page") == 0) {
            parsePage (&t);
        } else if (strcasecmp (tag, "preview") == 0) {
            if (m_cmd == CMD_PREVIEW) {
                preview (t);
            } else {
                t.eatToClosingTag ("preview");
            }
        } else if (strcasecmp (tag, "build") == 0) {
            if (m_cmd == CMD_BUILD) {
                char * src = NULL, * dst = NULL;
                for (int i = 0; i < nbParams*2; i+= 2) {
                    MESSAGE ("createVariable: %d:%s=%s\n", i/2, params[i], params[i+1]);
                    if (strcasecmp (params[i], "source") == 0) {
                        src = params[i+1];
                    } else if (strcasecmp (params[i], "target") == 0) {
                        dst = params[i+1];
                    }
                }
                if (src != NULL && dst != NULL) {
                    MESSAGE ("src=%s, dst=%s => %d", FileList::compareTime (src, dst));
                }
                build (t);
            } else {
                t.eatToClosingTag ("build");
            }
        } else if (strcasecmp (tag, "genMP4") == 0 && closing) {
            break;
        } else {
            fprintf (stdout, "unknown tag: %s, line %d\n", tag, t.getLine());
            return (false);
        }
    }
    return (true);
}

bool Scripter::setField (char * name, int value) {
    char buffer [1024];
    sprintf (buffer, "%d", value);
    return (setField (name, buffer));
}

bool Scripter::setField (char * name, bool value) {
    return (setField (name, value ? "TRUE" : "FALSE"));
}

bool Scripter::setField (char * name, float value) {
    char buffer [1024];
    sprintf (buffer, "%g", value);
    return (setField (name, buffer));
}

bool Scripter::setField (char * name, float val1, float val2) {
    char buffer [1024];
    sprintf (buffer, "%g %g", val1, val2);
    return (setField (name, buffer));
}

bool Scripter::setField (char * name, float val1, float val2, float val3) {
    char buffer [1024];
    sprintf (buffer, "%g %g %g", val1, val2, val3);
    return (setField (name, buffer));
}

bool Scripter::setField (char * name, char * value) {
    if (m_data[m_curBuf] == NULL) {
        fprintf (stdout, "set: a file must be loaded first!\n");
        return (false);
    }
    if (name == NULL || value == NULL) {
        MESSAGE ("setField: Either field name or value is bad (null value)\n");
        return (false);
    }
    char defName [256] = "DEF ";
    char fieldName [256] = "";
    // first isolate the def and field names and copy them in the above buffer
    int i = 4;
    while (*name != '\0' && *name != '.') {
        defName[i] = *name;
        i++;
        name++;
    }
    defName[i] = '\0';
    if (*name == '\0' || *name != '.') {
        MESSAGE ("Scripter::setField: bad format in \"%s\" : the dot is missing (should be \"DEF.field\"\n", name);
        return (false);
    }
    i = 0;
    name++;
    while (*name != '\0') {
        fieldName[i] = *name;
        i++;
        name++;
    }
    fieldName[i] = '\0';
    // copy the scene reference buffer to the new scene buffer 
    char * src = m_data[m_curBuf];
    char * dst = m_data[1-m_curBuf];
    // find the def name point
    char * tmp1 = strstr (src, defName);
    if (tmp1 == NULL || tmp1 == src) {
        MESSAGE ("Scripter::setField: cannot find the DEF name %s in the wrl scene\n", defName);
        return (false);
    }
    // find the field name point
    char * tmp2 = strstr (tmp1, fieldName);
    if (tmp2 == NULL || tmp2 == src) {
        MESSAGE ("Scripter::setField: cannot find the field name %s after the defName\n", fieldName);
        return (false);
    }
    // copy the first block (from beginning to start of field name)
    int blockLength = tmp2-src;
    strncpy (dst, src, blockLength);
    // copy the field name
    strcpy (dst+blockLength, fieldName);
    blockLength += strlen (fieldName);
    // add a blank char
    dst[blockLength++] = ' ';
    // copy the new field value
    strcpy (dst+blockLength, value);
    blockLength += strlen (value);
    // copy the second block (from the next newline to the end of buffer)
    while (*tmp2 != '\0' && *tmp2 != '\n') {
        tmp2++;
    }
    strcat (dst+blockLength, tmp2);
    // toggle active wrl buffer
    m_curBuf = 1 - m_curBuf;
    return (true);
}

void Scripter::append (char * s) {
    strcat (m_data[m_curBuf], s);
    //fprintf (stdout, "APPEND:\n%s\n**APPEND**\n", m_data[m_curBuf]);
    //m_curBuf = 1 - m_curBuf;
}

char * Scripter::readFileContent (char * fileName, int * dataLen, int extraSize) {
    if (fileName == NULL || *fileName == '\0') {
        if (dataLen != NULL) {
            *dataLen = 0;
        }
        return (NULL);
    }
    FILE * fp = fopen (fileName, "rb");
    if (fp == NULL) {
        MESSAGE("readFileContent: Cannot open file %s\n", fileName);
        return (NULL);
    }
    fseek (fp, (long)0, SEEK_END);
    long size = ftell (fp);
    fseek (fp, (long)0, SEEK_SET);
    char * data = new char [size+1+extraSize];
    fread (data, 1, size+1, fp);
    data[size] = '\0';
    fclose (fp);
    if (dataLen != NULL) {
        *dataLen = size+1;
    }
    //MESSAGE ("readFileContent: successfully read %d bytes\n", size);
    return (data);
}

void listDir (char * dir) {
    char buffer[1024];
    if (dir == NULL) {
        //GetCurrentDirectory (1024, buffer);
    } else {
        strcpy (buffer, dir);
    }
    strcat (buffer, "\\*.*");
    FileList fl (buffer);
    char * name;
    int n = 0;
    fprintf (stdout, "dir %s:\n", buffer);
    while (name = fl.getNextFile ()) {
        fprintf (stdout, "%d: %s\n", n++, name);
        free (name); //
    }
    fprintf (stdout, "%d files\n", n);
}

void usage (int argc, char * argv []) {
    fprintf (stdout, "usage: %s [--d] [--p] [--l] cmdFile [param]* : apply template with parameters\n", argv[0], argc); 
    fprintf (stdout, "       %s [--d] --i cmdFile: show info\n", argv[0], argc); 
    fprintf (stdout, "       --d debug mode\n");
    fprintf (stdout, "       --p execute preview section\n");
    fprintf (stdout, "       --l execute publish section\n");
    exit (1);
}

# define MAX_PARAMS 1024
int main (int argc, char * argv []) {
    if (argc < 2) {
        usage (argc, argv);
    }
    
    bool checkParam = true;
    CMD_LIST cmd = CMD_BUILD;
    char * name = NULL;
    char * params [MAX_PARAMS];
    int nbParam = 0;

    memset (params, 0, MAX_PARAMS*sizeof(char*));
    for (int i = 1; i < argc; i++) {
        if (checkParam && argv[i][0] == '-' && argv[i][1] == '-' ) {
            switch (argv[i][2]) {
            case 'd': debug = true; break;
            case 'i': cmd = CMD_INFO; break;
            case 'p':  cmd = CMD_PREVIEW; break;
            case 'l': cmd = CMD_PUBLISH; break;
            }
        } else {
            checkParam = false;
            if (name == NULL) {
                params[nbParam++] = name = argv[i];
                MESSAGE ("cmdFile = %s\n", argv[i]);
            } else {
                params[nbParam++] =  argv[i];
                MESSAGE ("params [%d] = %s\n", nbParam-1, argv[i]);
            }
        }
    }
    if (name == NULL) {
        fprintf (stdout, "no cmdFile specified\n");
        usage (argc, argv);
    }
    Scripter scripter (name, params, cmd);
}
