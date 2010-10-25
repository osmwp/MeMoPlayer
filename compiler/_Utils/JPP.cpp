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

void usage (char * exeName) {
    fprintf (stderr, "usage: %s file.def file1.java ... filen.java1\n", exeName);
    fprintf (stderr, "preprocess all java files using antenna or netbeans format\n");
    exit (1);
}

class Property {
    char * m_name;
    char * m_value;
    Property * m_next;

    Property (char * name; char * value, Property * next) {
        m_name = strdup (name);
        m_value = strdup (value);
        m_next = next;
    }
};

Property * readProp (char * filename) {
    FILE * fp = fopen (filename, "r");
    if (fp != null) {
        char buffer [1024*4], name [1024], value [1024];
        while (fgets (buffer, 1024*4, fp)) {
            if (sscanf (buffer, "%s=%s", name, value) == 2) {
                fprintf (stderr, "got property: '%s' = '%s'\n", name, value);
                root = new Property (name, value, root);
            }
        }
        fclose (fp);
    }
    return (root);
}

void process (Property root, char * filename) {
    
}

int main (int argc, char * argv []) {
    if (argc < 3) {  usage (argv[0]); }
    Property root = NULL;
    if (strcmp (argv[1], "-u") != 0) {
        root = readProps (argv [1]);
    }
    for (int i = 2; i < argc; i++) {  
        process  (root, argv[i]);
    }
}
