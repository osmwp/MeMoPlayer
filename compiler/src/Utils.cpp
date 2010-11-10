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
# include "fcntl.h"
# include <sys/wait.h>

# include "Utils.h"

extern FILE * myStderr;

extern bool endsWith (const char * t, const char * e); // defined in Compiler.cpp

int lastIndexOf (char * s, char c) {
    int l = strlen (s);
    int i;
    for (i = l-1; i >= 0; i--) {
        if (s[i] == c) { break; }
    }
    return i;
}


class Link {
public:
    char * value;
    Link * next;
    Link (char * v, Link * n) { value = v; next = n; }
    ~Link () { if (value) free (value); }
    Link * remove(char * v) {
        if (strcmp(v, value) == 0) {
            delete this;
            return next;
        } else if (next != NULL) {
            next = next->remove(v);
        }
        return this;
    }
};


bool MultiPathFile::addPath (char * path) {
    //fprintf (myStderr, ">>> MultiPathFile::addPath: adding from %s\n", path);
    int i = lastIndexOf (path, '/');
    if (i >= 0) {
        int cd = open (".", O_RDONLY);
        path [i] = '\0';
        chdir (path);
        path [i] = '/';
        char * tmp = (char *)malloc (4096);
        s_path = new Link (getcwd (tmp, 4096), s_path);
        //fprintf (myStderr, ">>> MultiPathFile::addPath: adding  %s\n", tmp);
        fchdir (cd);
        close(cd);
        return true;
    }
    return false;
}

void MultiPathFile::addMultiplePaths (char * paths) {
    //fprintf (myStderr, ">>> MultiPathFile::addMultiplePaths: adding from %s\n", paths);
    char cwdTmp[4096];
    int cd = open (".", O_RDONLY);
    char * tmp = strdup(paths);
    int i = lastIndexOf(tmp,';');
    while (i>=0) {
        char * p = tmp+i+1;
        tmp[i] = '\0';
        chdir (p);
        getcwd (cwdTmp, 4096);
        fchdir (cd);
        //fprintf (myStderr, ">>> MultiPathFile::addMultiplePaths: add %s\n", cwdTmp);
        s_path = new Link(strdup(cwdTmp), s_path);
        i = lastIndexOf(tmp,';');
    }
    chdir (tmp);
    free (tmp);
    getcwd (cwdTmp, 4096);
    fchdir (cd);
    //fprintf (myStderr, ">>> MultiPathFile::addMultiplePaths: add %s\n", cwdTmp);
    s_path = new Link(strdup(cwdTmp), s_path);
    close(cd);
}

void MultiPathFile::removeLastPath () {
    if (s_path != NULL) {
        //fprintf (myStderr, ">>> MultiPathFile::removeLastPath: removing %s\n", s_path->value);
        Link * l = s_path;
        s_path = s_path->next;
        delete l;
    }
}

/**
 * Removes a path added by addPath()
 */
void MultiPathFile::removePath (char * path) {
    int i = lastIndexOf (path, '/');
    if (i >= 0 && s_path != NULL) {
        path [i] = '\0';
        s_path = s_path->remove(path);
        path [i] = '/';
    }
}

// open a file by trying the filename, then concantenation of each path and the file name
FILE * MultiPathFile::fopen (const char * filename, const char * mode) {
    FILE * fp = ::fopen (filename, mode);
    //fprintf (myStderr, ">>> MultiPathFile::fopen: trying %s\n", filename);
    if (fp) { return fp; }
    Link * l = s_path;
    char buffer [4096];
    while (l) {
        sprintf (buffer, "%s/%s", l->value, filename);
        //fprintf (myStderr, ">>> MultiPathFile::fopen: trying %s\n", buffer);
        fp = ::fopen (buffer, mode);
        if (fp) { return fp; }
        l = l->next;
    }
    return (NULL);
}

// return complete path to a file if found and accessible, or NULL
char* MultiPathFile::find (const char * filename, const char * mode) {
    // Best way to find if a file exists and is accessible seems to just use fopen...
    FILE * fp = ::fopen (filename, mode);
    if (fp) {
      fclose (fp); 
      return strdup (filename);
    }
    Link * l = s_path;
    char buffer [4096];
    while (l) {
        sprintf (buffer, "%s/%s", l->value, filename);
        fp = ::fopen (buffer, mode);
        if (fp) {
          fclose (fp);
          return strdup (buffer);
        }
        l = l->next;
    }
    return (NULL);
}

Link * MultiPathFile::s_path = NULL;

// MediaList section

extern int includeMediaFile (FILE * fp, const char * name, bool mandatory = true); // from Type.cpp :-/

int MediaList::dump (FILE * fp) {
    Link * l = root;
    int size = 0;
    while (l) {
        size += includeMediaFile (fp, l->value);
        l = l->next;
    }
    return size;
}

void MediaList::addMedia (const char * files) {
    char * tmp = strdup(files);
    int i = lastIndexOf(tmp,',');
    while (i>=0) {
        char * p = tmp+i+1;
        tmp[i] = '\0';
        root = new Link (strdup(p), root);
        i = lastIndexOf(tmp,',');
    }
    root = new Link (strdup(tmp), root);
    free (tmp);
}
