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

# ifndef __FILE_TOOL__
# define __FILE_TOOL__
class FileList {
    void * m_hSearch;     
    void * m_findData;
    bool m_valid;
    bool m_started;
public:
    FileList (char * dir);
    ~FileList();
    char * getNextFile ();
    static void getCurrentDir (char * buffer, int bufLen);

    // return -1 if F1 is older than f2, 0 is same date, +1 if f1 newer than f2
    // -2 if one of the file cannot be read
    static int compareTime (char * f1, char * f2);
};

# endif
