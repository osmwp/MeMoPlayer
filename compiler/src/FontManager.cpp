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
# include <sys/wait.h>
# include <unistd.h>
# include "Utils.h"
# include "FontManager.h"

bool endsWith (const char * t, const char * e);

FontManager * FontManager::s_root = NULL;

FontManager::FontManager (char * fontName, int size, FontManager * next) {
    m_fontName = strdup (fontName);
    m_size = size;
    m_next = next;
    m_map = NULL;
    m_len = 0;
}

FontManager::~FontManager () {
    free (m_fontName);
}

FontManager * FontManager::find (char * fontName, int size) {
    if (strcasecmp (fontName, m_fontName) == 0 && m_size == size) {
        return this;
    } else if (m_next != NULL) {
        return m_next->find (fontName, size);
    } else {
        return NULL;
    }
}


void FontManager::add (char * newMap) {
    //fprintf (stderr, "$$ Font %s / %d: adding '%s' to '%s'\n", m_fontName, m_size, newMap ? newMap : "", m_map ? m_map : ""); 
    int newLen = 0;
    if (newMap == NULL || (newLen = strlen (newMap)) == 0) {
        return;
    }
    char * tmp = (char *) malloc (m_len+newLen+1);
    if (m_map != NULL) {
        strcpy (tmp, m_map);
        free (m_map);
    }
    m_map = tmp;
    strcpy (m_map+m_len, newMap);
    m_len += newLen;
}

FontManager * FontManager::findOrCreate (char * fontName, int size) {
    FontManager * fm = NULL;
    if (s_root != NULL) {
        fm = s_root->find (fontName, size);
    }
    if (fm == NULL) {
        //fprintf (stderr, "$$ Font %s / %d: need to create font\n", fontName, size);
        fm = s_root = new FontManager (fontName, size, s_root);
    } else {
        //fprintf (stderr, "$$ Font %s / %d: reuse existing\n", fontName, size);
    }
    return fm;
}

void FontManager::addMap (char * fontName, int size, char * map) {
    findOrCreate (fontName, size)->add (map);
}

int FontManager::dumpAll (char * execPath, FILE * fp) {
    int total = 0;
    FontManager * fm = s_root;
    while (fm != NULL) {
        //fprintf (stderr, "FontManager::dumping: %s %d\n", fm->m_fontName, fm->m_size);
        total += fm->dump (execPath, fp);
        fm = fm->m_next;
    }
    return total;
}

extern int includeFile (FILE * fp, const char * fileName, char * name, int magic, bool mandatory = true);

int FontManager::dump (char * execPath, FILE * fp) {
    int total = 0;
    // find the right fontname
    static const char * fontExt [] = { ".ttf", ".otf", ".dfont", ".tfd", ""};
    char * fullPath = NULL;
    char fullName [2048];
    for (int i = 0; i < 5; i++) {
        sprintf (fullName, "%s%s", m_fontName, fontExt[i]);
        fullPath = MultiPathFile::find (fullName, "r");
        if (fullPath) {
            break;
        }
    } 
    if (fullPath) {
        char fontSize [256];
        char mapName [1024];
        char execName [2048] = "";
        pid_t pid;
        int status;
        sprintf (fontSize, "%d", m_size);
        sprintf (mapName, "%s_%d", m_fontName, m_size);
        pid = fork ();
        if (m_map == NULL || strlen (m_map) == 0) {
            m_map = (char*)"*";
        }
        if (pid == 0) { //On est dans le fils
            //fprintf (stderr, "$$ ./fontExtractor -d  %s %s %s %s\n", fullName, fontSize, mapName, m_map);
            sprintf (execName, "%s/fontExtractor", execPath);
            execl(execName, execName, /*"-d",*/ fullPath, fontSize, mapName, m_map, NULL);
            fprintf (stderr, "fontExtractor failed for %s %s\n", fullName, fontSize);
            exit (1);
        }  else if (pid != -1) {
            if (waitpid (pid, &status, 0) != -1) {
                //fprintf (stderr, "./fontExtractor exited with a status of %d\n", WEXITSTATUS(status));
                bool noSize = false; //endsWith (fullName,".tfd");
                if (WEXITSTATUS(status) == 0) {
                    if (noSize) {
                        sprintf (fullName, "%s.png", m_fontName);
                    } else {
                        sprintf (fullName, "%s_%d.png", m_fontName, m_size);
                    }
                    total += includeFile (fp, fullName, fullName, MAGIC_IMAGE, false); 

                    if (noSize) {
                        sprintf (fullName, "%s.desc", m_fontName);
                    } else {
                        sprintf (fullName, "%s_%d.desc", m_fontName, m_size);
                    }
                    total += includeFile (fp, fullName, fullName, MAGIC_FONT, false); 
                }
            }
        }
        free (fullPath);
    } else {
        fprintf (stderr, "connot find font %s\n", m_fontName);
    }
    return total;
}

