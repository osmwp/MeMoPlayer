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

// class to hold a local entry which is a pair key/message taht can be linked with another entry
class LocaleEntry {
public:
    int m_id; // the number that will be stored instead of the SFString
    char * m_key; // the key that will be defined inside the WRL in the form @[key:message]
    char * m_message; // the message that will be defined inside the WRL in the form @[key:message]
    int m_mark; // a temporary marker used when checking if another localization file match the reference
    LocaleEntry * m_next; // to link entries together

    LocaleEntry (int id, char * key, char * message, LocaleEntry * next);

    ~LocaleEntry () ;

    LocaleEntry * find (char * key);

    void update (char * key, char * message);

    void encodeAll (FILE * fp);

    void saveAll (FILE * fp);

    void setMark (int m) { m_mark = m; }

    void setAllMarks (int m);
};

// a set of entries, contains a root of LocalEntry, the number of entries set and utility methods
class LocaleSet {
    
    char * m_filename; //mostly for error message purposes
    LocaleEntry * m_entry, * m_last;
    int m_nbEntries;

public:
    LocaleSet (char * filename);
    ~LocaleSet ();

    int nbEntries () { return m_nbEntries; }

    int findEntry (char * key);

    LocaleEntry * getEntry (char * key);

    int addEntry (char * key, char * message);

    int updateEntry (char * key, char * message);

    //int updateEntry (char * key, char * message);

    void setAllMarks (int m);

    void encode (FILE * fp);

    void save (FILE * fp);

    int compareWithModel (LocaleSet * model);
};

# define LOCALE_KEY_LEN 1024
# define LOCALE_MSG_LEN 4096

class LocaleManager {
    LocaleSet * m_master;
    LocaleSet * m_extra;
    int m_missingTranslations;

    static LocaleManager * s_manager;

public :

    LocaleManager ();

    ~LocaleManager ();
    
    // save the master file as a text file in "default.lng"
    void saveDefault (); 

    // encode the master file as a binary file in "default.loc"
    // void encodeDefault (); 

    // encode an extra file according to master using the lang name like FR, EN, IT
    void encodeExtra (FILE * fp, char * fileName, char * langName);

    static char key [LOCALE_KEY_LEN]; // tmp array to store current key
    static char msg [LOCALE_MSG_LEN];// tmp array to store current massage

    static int split (char * v); 

    static void writeUTF8 (FILE * fp, char * v);

    static int getEntryID (char * key);

    static int addEntry (char * message);

    static LocaleManager * getManager ();
    
    // warn if at least one Locale is missing in a translation file
    static void checkMissingTranslations ();

};
