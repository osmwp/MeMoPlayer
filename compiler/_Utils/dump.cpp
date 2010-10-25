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

void exchangeBytes (int * i) {
    char * s = (char *) i;
    char t = s[0];
    s[0] = s[3];
    s[3] = t;
    t = s[1];
    s[1] = s[2];
    s[2] = t;
}

void usage (char * exeName) {
    fprintf (stderr, "usage: %s[-v] file.m4m : dump the m4m file structure\n", exeName);
    exit (1);
}

bool endsWith (char * t, char * e) {
     int l1 = strlen (t);
     int l2 = strlen (e);
     if (l1 < l2) {
	  return (false);
     }
     while (l2 > 0) {
	  if (t[--l1] != e[--l2]) {
	       return (false);
	  }
     }
     return (true);
}

int readInt (FILE * fp) {
    int i;
    if (fread (&i, 1, 4, fp) != 4) {
        exit (1);
    }
    exchangeBytes (&i);
    return (i);
}

char * readString (FILE * fp) {
    char buffer [2048];
    int l = 0;
    int c = (char) fgetc (fp);
    while (c > 0) {
        buffer [l++] = (char)(c&0xff);
        c = (char) fgetc (fp);
    }
    buffer [l] = '\0'; 
    return (strdup (buffer));
}

void dump (char * file, bool verbose) {
    FILE * fp = fopen (file, "r");
    int magic;
    int size;
    char * name;
    while (true) {
        magic = readInt (fp);
        if (magic == 0xFFFF) {
            fprintf (stderr, ">> END\n");
            break;
        }
        fprintf (stderr, "magic: %X\n", magic);
        fprintf (stderr, "name:  %s\n", readString (fp));
        fprintf (stderr, "size:  %d\n", size = readInt (fp));
        fseek (fp, size, SEEK_CUR);
    }
}

int main (int argc, char * argv []) {
    if (argc < 2) {  usage (argv[0]); }
    bool verbose = false;
    int start = 1;
    char * file = NULL;
    while (start < argc) {
        if (strcmp (argv[start], "-v") ==0) {
            verbose = true;
        } else {
            file = argv [start];
        }
        start++;
    }
    if (file != NULL && endsWith (file, "m4m")) {
        dump (file, verbose);
    } else {
        usage (argv[0]);
    }
    return (0);
}
