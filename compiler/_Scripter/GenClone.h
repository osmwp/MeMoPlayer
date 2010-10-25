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

# ifndef __GENCLONE__
# define __GENCLONE__

class GenClone {
protected:
    int m_nbImgs;
    Image * m_img [100];
    Rect m_rect[100]; 

public:
    /** Initialize the internal members */
    GenClone (char * cmdFile, bool test = false);
    
    /** Free alll allocated data */
    ~GenClone ();

protected:
    void parseXML (char * dir);
    void rescale (Image * img, int w, int h);
    void loadImages (char * dir, int w, int h);
    void saveImages (char * dir);
    void cropImages (char * dirbool, bool test);
    void convertAudio (char * dir);
};


# endif
