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

void parseTable (char * inName, char *out) {
    FILE * in = fopen (inName, "r");
    if (in == NULL) {
        fprintf (stderr, "Fatal error: cannot open %s for reading\n", inName);
        exit (1);
    }

    out[0] = 0;
    char cpC[2] = { 0, 0 }; 
    int c = fgetc (in);
    while (c != EOF) {
        if (c == '\n') {
            strcat (out," ");
        } else if (c == '#') {
            c = fgetc (in);
            while (c != '\n') {
                c = fgetc (in);
            }
        } else {
            cpC[0] = c;
            strcat (out,cpC);
        }
        c = fgetc (in);
    }
    fclose (in);
    // printf("\n%s => %s\n",inName,out);
}
