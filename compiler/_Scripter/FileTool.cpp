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

# include "FileTool.h"
# include <stdio.h>
# include <unistd.h>
//# include <windows.h>
//# include <winbase.h>
# include "Trace.h"

FileList::FileList (char * dirname) {
//     m_findData = (WIN32_FIND_DATA*)new WIN32_FIND_DATA;
//     m_hSearch = (void*)FindFirstFile(dirname, (WIN32_FIND_DATA*)m_findData); 
//     m_valid = m_hSearch != INVALID_HANDLE_VALUE ;
    m_started = false;
}

FileList::~FileList() {
    if (m_valid) {
        //FindClose((HANDLE)m_hSearch);
    }
    //delete (WIN32_FIND_DATA*)m_findData;
}

char * FileList::getNextFile () {
//     if (m_valid) {
//         if (m_started == false) {
//             m_started = true;
//             return (strdup (((WIN32_FIND_DATA*)m_findData)->cFileName));
//         } else if (FindNextFile((HANDLE)m_hSearch, (WIN32_FIND_DATA*)m_findData) == false) {
//             FindClose((HANDLE)m_hSearch);
//             m_valid = true;
//             return (NULL);
//         }
//         return (strdup (((WIN32_FIND_DATA*)m_findData)->cFileName));
//     } else {
//         return (NULL);
//     }
    return (NULL);
}

void FileList::getCurrentDir (char * buffer, int bufLen) {
    getcwd (buffer, bufLen);
}

int FileList::compareTime (char * f1, char * f2) {
//     HANDLE hFile; 
//     FILETIME creationTime, lastAccessTime, lastWriteTime1, lastWriteTime2;
	
//     hFile = CreateFile(f1, GENERIC_READ, FILE_SHARE_READ, NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL); 
//     if (hFile == INVALID_HANDLE_VALUE) {  // no file or unable to be read
//         return -2;
//     }
//     if (!GetFileTime(hFile, &creationTime, &lastAccessTime, &lastWriteTime1)) {
//         CloseHandle (hFile);
//         return -2;
//     }
//     CloseHandle (hFile);
//     hFile = CreateFile(f2, GENERIC_READ, FILE_SHARE_READ, NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL); 
//     if (hFile == INVALID_HANDLE_VALUE) {  // no file or unable to be read
//         return -2;
//     }
//     if (!GetFileTime(hFile, &creationTime, &lastAccessTime, &lastWriteTime2)) {
//         CloseHandle (hFile);
//         return -2;
//     }
//     CloseHandle (hFile);
//     return (CompareFileTime (&lastWriteTime1, &lastWriteTime2));
    return (0);
}
