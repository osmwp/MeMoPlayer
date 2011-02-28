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
# include "unistd.h"
# include "process.h"
# define CMD_NONE   0
# define CMD_IFDEF  1
# define CMD_IFNDEF 2
# define CMD_ELSE 4
# define CMD_ENDIF 8
# define CMD_CONDITION   16
# define CMD_EXPAND 32
# define CMD_DEBUG 64
# define CMD_UNKNOWN -1
# define CMD_INFINITE 65536

char * cmdName [] = { "ifdef", "ifndef", "else", "endif", "condition", "expand", "debug"};
int cmdValue [] = { CMD_IFDEF, CMD_IFNDEF, CMD_ELSE, CMD_ENDIF, CMD_CONDITION, CMD_EXPAND, CMD_DEBUG };

bool Process::endsWithMark (char * t) {
     int l = strlen (t);
     int i = l;
     if (l > 2 && t[l-1] == '\n') {
         i--;
     }
     if (l > 2 && t[l-2] == '\r') {
         warning ("CR detected in endsWithMark");
         i--;
     }
     if (i > 3 && strncmp (t+i-3, "//!", 3)==0) {
         strcpy (t+i-3, t+i);
         return true;
     }
     return false;
}

void Process::addMark (char * t) {
     int l = strlen (t);
     int i = l;
     if (l > 2 && t[l-1] == '\n') {
         i--;
     }
     if (l > 2 && t[l-2] == '\r') {
         warning ("CR detected in endsWithMark");
         i--;
     }
     strncpy (t+i+3, t+i, l-i); 
     t[l+3] = 0;
     strncpy (t+i, "//!", 3); 
}

int getIndex (char * buffer, int target) {
    int l = strlen (buffer);
    if (l > 0 && buffer[l-1] == '\n') {
        buffer[--l] = 0;
    }
    for (int i = 0; i < l; i++) {
        if (buffer[i] == target) {
            return i;
        }
    }
    return -1;
}

void Process::warning (char * msg) {
    if (m_line != m_lastError) {
        fprintf (stderr, "%s:%d: Warning: %s in\n %s", m_filename, m_line, msg, m_buffer);
        m_lastError = m_line;
    }
}

int Process::findMarker (char * buffer, char c) {
    int l = strlen (buffer);
    int index = 0;
    // skip spaces
    for (index = 0; index < l; index++) {
        if (buffer[index] != ' ' && buffer[index] != '\t') {
            break;
        }
    }
    if ((l-index) < 3) { // must be at least //#\n
        return 0;
    }
    if (buffer[index++] != '/') { return 0; }
    if (buffer[index++] != '/') { return 0; }
    while (index < l && (buffer[index] == ' ' || buffer[index] == '\t')) {
        index++;
    }
    if (buffer[index++] != c) { return 0; }
    if (index != 3) {
        fprintf (stderr, "WARNING: extra spaces (%d) between // and # : %s, correcting that ...\n", (index-3), buffer);
        strncpy (buffer, "//#", 3);
        strcpy (buffer+3, buffer+index);
        fprintf (stderr, "... fixed with: %s\n", buffer);
        index = 3;
    }
    return index;
}

int Process::findComment (char * buffer) {
    return findMarker (buffer, '@');
}

bool getProperty (char * s1, char * s2) {
    while (*s1 && (*s1 == ' ' || *s1 == '\t')) {
        s1++;
    }
    if (!*s1) { 
        return false;
    }
    while (*s1 && *s1 != ' ' && *s1 != '\t' && *s1 != '\r' && *s1 != '\n') {
        *s2++ = *s1++;
    }
    *s2 = 0;
    return true;
}

Property * readProps (char * filename, bool verbose) {
    if (verbose) { fprintf (stderr, "parsing property file %s\n", filename); }
    Property * root = NULL;
    FILE * fp = fopen (filename, "r");
    if (fp != NULL) {
        char buffer [1024*4], name [1024], value [1024];
        while (fgets (buffer, 1024*4, fp)) {
            if (buffer[0] == '#') {
                continue;
            }
            int n = getIndex (buffer, '=');
            if ( n >= 0) {
                strncpy (name, buffer, n);
                name [n] = 0;
                getProperty (buffer+n+1, value);
                root = new Property (name, value, root);
                if (verbose) { fprintf (stderr, "    %s=%s\n", name, value); }
            } else {
                getProperty (buffer+n+1, name);
                value [0] = 0;
                root = new Property (name, NULL, root);
                if (verbose) { fprintf (stderr, "    %s\n", name); }
            }
        }
        fclose (fp);
    }
    return (root);
}

bool startsWith (char * s1, char * s2) {
    //fprintf (stderr, "comparing %s / %s\n", s1, s2);
    return strncmp (s1, s2, strlen (s2)) == 0;
}

int getProperty (int value, char * s1, char * s2) {
    if (s2 && !getProperty (s1, s2)) {
        *s2 = 0;
    }
    return value;
}

int Process::getCommand (char * buffer, char * property) {
    int nbCmds = sizeof (cmdName)/sizeof (char*);
    // try to eat spaces
    char * org = buffer;
    while (*buffer == ' ' || *buffer== '\t') {
        buffer++;
    }
    for (int i = 0; i < nbCmds; i++) {
        if (startsWith (buffer, cmdName[i])) {
            if (buffer != org) {
                warning ("Extra space after //# (should be nothing betwen # and directive)");
            }
            return getProperty (cmdValue[i], buffer+strlen (cmdName[i]), property);
        }
    }
    return CMD_UNKNOWN;
}

int Process::findCommand (char * buffer, char * property) {
    int index = findMarker (buffer);
    if (index > 0) {
        //fprintf (stderr, "find marker: %d\n", index);
        int cmd = getCommand (buffer+index, property);
        if (cmd != CMD_NONE && index > 3) {
            warning ("extra space before //# (should be at beginnning of the line)");
        }
        return cmd;
    }
    return CMD_NONE;
}

bool Process::getLine () {
    static char tmp [MAX_BUFFER_LEN];
    if (fgets (tmp, MAX_BUFFER_LEN, m_in)) {
        char * s = tmp;
        char * d = m_buffer;
        while (*s) {
            if (*s == '\t') {
                *d++ = ' ';
                *d++ = ' ';
                *d++ = ' ';
                *d++ = ' ';
            } else if (*s == '\r') {
                fprintf (stderr, "WARNING: CR detected in getLine: %s", tmp);
            } else {
                *d++ = *s;
            }
            s++;
        }
        *d = 0;
        m_line++;
        return true;
    }
    return false;
}

// comment until directive include inside ORed commands
int Process::processUncomment (bool original) {
    while (getLine()) {
        revertLine (original);
    }
    return CMD_NONE;
}

// comment until directive include inside ORed commands
int Process::processComment (int commands) {
    int nested = 0;
    while (getLine()) {
        int cmd = findCommand (m_buffer, m_property);
        if (cmd == CMD_IFDEF || cmd == CMD_IFNDEF) {
            nested++;
        }
        fprintf (m_out, "%s%s", cmd != CMD_NONE || findComment (m_buffer) > 2 ? "" : "//@", m_buffer);
        if (cmd > 0 && ((commands & cmd) == cmd) && nested <= 0) { // stop!!
            return cmd;
        }
        if (cmd == CMD_ENDIF) {
            nested--;
        }
    }
    return CMD_NONE;
}


void Process::revertLine (bool original) {
    char * buffer = m_buffer;
    // first remove all //@
    int idx = findComment (m_buffer); 
    if (idx > 0) {
        buffer += idx;
    }
    // check for trailing //!
    if (endsWithMark (buffer)) {
        if (original) {
            fprintf (m_out, "//#%s", buffer);
        } else {
            addMark (buffer);
            fprintf (m_out, "%s", buffer);
        }
    } else {
        if (!original && findCommand (buffer) == CMD_UNKNOWN) {
            addMark (buffer);
            fprintf (m_out, "%s", buffer+findMarker (buffer, '#'));
        } else {
            fputs (buffer, m_out);
        }
    }
}

bool Process::processCondition () {
    if (m_verbose) { fprintf (stderr, ". processing Condition %s\n", m_property); }
    if (m_props != NULL && m_props->find (m_property)) { // eventually prepending '//#' if not already there
        if (m_verbose) { fprintf (stderr, ".. removing comments\n"); }
        processBody (CMD_INFINITE);
    } else { // undoing
        if (m_verbose) { fprintf (stderr, ".. commenting out\n"); }
        processComment (CMD_NONE);
    }
    return true;
}

bool Process::processDebug () {
    if (m_verbose) { fprintf (stderr, ". processing debug %s\n", m_property); }
    if (getLine ()) {
        if (m_props != NULL && m_props->find (m_property)) { // eventually prepending '//@' if not already there
            if (m_verbose) { fprintf (stderr, ".. removing comments\n"); }
            revertLine (false);
        } else { // undoing
            if (m_verbose) { fprintf (stderr, ".. commenting out\n"); }
            fprintf (m_out, "%s%s", findComment (m_buffer) > 2 ? "" : "//@", m_buffer);
        }
    }
    return true;
}

bool Process::processIfdef (bool ifdef) {
    if (m_verbose) { fprintf (stderr, ". processing %s %s\n", ifdef ? "ifdef" : "ifndef", m_property); }
    bool defined = m_props != NULL && m_props->find (m_property);
    if ( (defined && ifdef) || (!defined && !ifdef) ) { 
        if (m_verbose) { fprintf (stderr, ".. leaving blank\n"); }
        while (getLine()) {
            int cmd = findCommand (m_buffer, m_property);
            revertLine ();
            switch (cmd) {
            case CMD_ENDIF:
                return true;
            case CMD_ELSE:
                processComment (CMD_ENDIF);
                return true;
            case CMD_IFDEF:
            case CMD_IFNDEF:
                processIfdef (cmd == CMD_IFDEF);
                continue;
            case CMD_DEBUG:
                processDebug ();
                continue;
            case CMD_NONE:
                continue;
            }
        }
    } else { // commenting until #else or #endif
        if (m_verbose) { fprintf (stderr, ".. commenting\n"); }
        if (processComment (CMD_ENDIF|CMD_ELSE) == CMD_ELSE) {
            processBody (CMD_ENDIF);
        }
    }
    return true;
}

int Process::processBody (int commands) {
    while (getLine()) {
        revertLine ();
        int cmd = findCommand (m_buffer+(strncmp (m_buffer, "//@", 3) == 0 ? 3 : 0), m_property);
        if (cmd > 0 && (commands & cmd) == cmd) { // stop!!
            return cmd;
        }
        switch (cmd) {
        case CMD_CONDITION:
            processCondition ();
            break;
        case CMD_IFDEF:
        case CMD_IFNDEF:
            processIfdef (cmd == CMD_IFDEF);
            break;
        case CMD_ELSE:
        case CMD_ENDIF:
        case CMD_DEBUG:
            processDebug ();
            break;
        default:
            ;
        }        
    }
    return CMD_NONE;
}

Process::Process (Property * props, char * filename, bool verbose) {
    m_props = props;
    m_verbose = verbose;
    m_filename = filename;
    m_in = fopen (filename, "r");
    if (m_in == NULL) {
        fprintf (stderr, "Cannot open %s for reading\n", filename);
        exit (1);
    }
    sprintf (m_buffer, "%s.TMP", filename);
    m_out = fopen (m_buffer, "w");
    if (m_out == NULL) {
        fprintf (stderr, "Cannot open %s for writing\n", m_buffer);
        exit (1);
    }
    if (m_verbose) { fprintf (stderr, "Processing %s\n", filename); }
    m_line = 0;
    m_lastError = -1;
    if (m_props) {
        processBody (CMD_INFINITE);
    } else {
        processUncomment (true);
    }
    fclose (m_in);
    fclose (m_out);
    // rename files in->.old out->.java
    sprintf (m_buffer, "%s.old", filename);
    rename (filename, m_buffer);
    sprintf (m_buffer, "%s.TMP", filename);
    rename (m_buffer, filename);
}

void usage (char * exeName) {
    fprintf (stderr, "usage: %s [-p file.def] [-v] file1.java ... filen.java1\n", exeName);
    fprintf (stderr, "preprocess all java files using antenna or netbeans format\n");
    fprintf (stderr, "if '-p file.def' is specified, before any java file, file.def os used to define properies, one per line on the form: property[=value]\n");
    fprintf (stderr, "exemple:\napi.file\nversion.number=3.6a\nmode=verbose\ndebug\n");
    fprintf (stderr, "if '-p file.def' is not specified, then preprocessing is removed from file to revert to original (e.g. to commit in SNV repository)\n");
    fprintf (stderr, "if -v is specified, each modification is logged on stdout\n");
    exit (1);
}

int main (int argc, char * argv []) {
    if (argc < 2) {  usage (argv[0]); }
    Property * root = NULL;
    bool verbose = false;
    char * propertyFile = NULL;
    int i;
    for (i = 1; i < argc; i++) {
        if (strcmp (argv[i], "-p") == 0) {
            propertyFile = argv [++i];
        } else if (strcmp (argv[i], "-v") == 0) {
            verbose = true;
        } else {
            break;
        }
    }
    if (propertyFile) {
        root = readProps (propertyFile, verbose);
    }
    for (; i < argc; i++) {  
        Process process  (root, argv[i], verbose);
    }
}
