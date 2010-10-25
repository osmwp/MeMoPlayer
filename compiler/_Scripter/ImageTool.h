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

# ifndef __IMAGETOOL__
# define __IMAGETOOL__

class Rect {
public:
    int left, top, right, bottom;
};

class Image {
    int * m_data;
    int m_width, m_height;
    bool m_transparency;
public:
    Image (char * filename);
    ~Image ();
    bool save (char * filename, Rect * r = NULL);
    int * getData () { return m_data; }
    int getWidth () { return m_width; }
    int getHeight () { return m_height; }

    bool getDiffRect (Image * target, Rect & r, bool debug = false);
    bool resize (int width, int height);
};

# endif
