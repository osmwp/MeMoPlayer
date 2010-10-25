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

// linked list of property pairs (name, value)
// they are parsed from a file and used in procesing directives
class Property {
    char * m_name;
    char * m_value;
    Property * m_next;
public:
    Property (char * name, char * value, Property * next) {
        m_name = name ? strdup (name) : NULL;
        m_value = value ? strdup (value) : NULL;
        m_next = next;
    }
    Property * find (char * name) {
        if (strcmp (m_name, name) == 0) {
            return this;
        } else if (m_next) {
            return m_next->find (name);
        } else {
            return NULL;
        }
    }
    void print () {
        if (m_next) {
            m_next->print ();
        }
        fprintf (stderr, "- %s%s%s\n", m_name, m_value ? "=":"", m_value ? m_value:""); 
    }
};


# define MAX_BUFFER_LEN 32*1024
# define MAX_PROPERTY_LEN 1*1024
class Process {
    FILE * m_in; // the original file
    FILE * m_out; // the resulting file
    Property * m_props;
    bool m_verbose;
    int m_line, m_lastError;
    char * m_filename;
    char m_buffer [MAX_BUFFER_LEN];
    char m_property [MAX_PROPERTY_LEN];
public:
    Process (Property * props, char * filename, bool verbose);
    bool processCondition ();
    bool processIfdef (bool ifdef);
    bool processExpand ();
    int processComment (int commands);
    int processUncomment (bool original = false);
    int processBody (int commands);
    bool processDebug ();
    void revertLine (bool original = false);

    int getCommand (char * buffer, char * property);
    int findCommand (char * buffer, char * property = NULL);
    bool endsWithMark (char * t);
    void addMark (char * t);
    int findMarker (char * buffer, char c = '#');
    int findComment (char * buffer);

    bool getLine ();
    void warning (char * msg);
};
