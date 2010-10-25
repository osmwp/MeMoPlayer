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

void makeNodeTable (char * inName, char * outName, char * tableName) {
    FILE * in = fopen (inName, "r");
    if (in == NULL) {
        fprintf (stderr, "Fatal error: cannot open %s for reading\n", inName);
        exit (1);
    }
    FILE * out = fopen (outName, "w");
    if (out == NULL) {
        fprintf (stderr, "Fatal error: cannot open %s for writing\n", outName);
        exit (1);
    }
    fprintf (out, "//automatically generated\nstatic char %s [] = \"", tableName);
    int c = fgetc (in);
    while (c != EOF) {
        if (c == '\n') {
            fputc (' ', out);
        } else if (c == '"') {
            fputc ('\\', out);
            fputc ('"', out);
        } else if (c == '#') {
            c = fgetc (in);
            while (c != '\n') {
                c = fgetc (in);
            }
        } else {
            fputc (c, out);
        }
        c = fgetc (in);
    }
    fprintf (out, "\";\n");
    fclose (in);
    fclose (out);

}

void usage (char * exeName) {
    fprintf (stderr, "usage: %s file.def file.inc [tableName]: compile the def file in order to be included\n", exeName);
    fprintf (stderr, "if tableName is specified, it is used for teh array name instead of nodeTableDef\n");
    exit (1);
}

int main (int argc, char * argv []) {
    if (argc < 3) {  usage (argv[0]); }
    makeNodeTable (argv[1], argv[2], argc > 3 ? argv[3] : (char*)"nodeTableDef");
}
