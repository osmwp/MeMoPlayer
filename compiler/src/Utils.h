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

# define MAGIC_SCENE  0xAAAA
# define MAGIC_IMAGE  0xBBBB
# define MAGIC_SCRIPT 0xCCCC
# define MAGIC_PROTO  0xDDDD
# define MAGIC_LOCALE 0xEEEE
# define MAGIC_MMEDIA 0x5555
# define MAGIC_CSS    0xCC55
# define MAGIC_FONT   0xF0E1
# define MAGIC_BML    0xB111
# define MAGIC_END    0XFFFF

class Link; 

class MultiPathFile {
    static Link * s_path;
public:
    static bool addPath (char * path);
    static void addMultiplePaths (char * paths);
    static void removeLastPath ();
    static void removePath (char * path);
    
    // open a file by trying the filename, then concantenation of each path and the file name
    static FILE * fopen (const char * filename, const char * mode);

    // return complete path to a file if found and accessible, or NULL
    static char * find (const char * filename, const char * mode);
};


class MediaList {
    Link * root;
  public:
    MediaList () { root = NULL; }
    // Add multiple media
    void addMedia (const char * files);
    // Include all listed media in final M4M file
    int dump (FILE * fp);
};

int lastIndexOf (char * s, char c);
