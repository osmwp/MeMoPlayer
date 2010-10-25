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

// this class maintains the list of all strings used for a given font 
// and generates a bitmap as well as a description of the position of each char

class FontManager {
    char * m_fontName;
    int m_size;
    FontManager * m_next;
    char * m_map;
    int m_len;
    
    static FontManager * s_root;

public:
    FontManager (char * fontName, int size, FontManager * next);
    ~FontManager ();
    FontManager * find (char * fontName, int size);
    void add (char * map);
    int dump (char * execPath, FILE * fp);
    static FontManager * findOrCreate (char * fontName, int size);
    static void addMap (char * fontName, int size, char * map);
    static int dumpAll (char * execPath, FILE * fp);

};

