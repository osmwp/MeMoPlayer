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
# include "stdarg.h"
# include "string.h"
# include "unistd.h"
# include "fcntl.h"

# include "LocaleManager.h"
LocaleEntry::LocaleEntry (int id, char * key, char * message, LocaleEntry * next) {
    m_id = id;
    m_key = strdup (key);
    m_message = strdup (message);
    m_next = next;
    m_mark = -1;
}

LocaleEntry::~LocaleEntry () {
    free (m_key);
    free (m_message);
    if (m_next) {
        delete m_next;
    }
}

LocaleEntry * LocaleEntry::find (char * key) {
    if (strcmp (m_key, key) == 0) {
        return this;
    } else if (m_next) {
        return m_next->find (key);
    }
    return NULL;
}

void LocaleEntry::update (char * key, char * message) {
    if (strcmp (m_key, key) == 0) {
        if (strlen (m_message) > 0) {
            fprintf (stderr, "Locale: message redefined for key '%s' with '%s'\n", key, message);
        }
        free (m_message);
        m_message = strdup (message);
    } else if (m_next) {
        return m_next->update (key, message);
    }
}

void LocaleEntry::encodeAll (FILE * fp) {
    if (m_id >= 0) {
        fprintf (fp, "%c%c", m_id / 256, m_id % 256);
        LocaleManager::writeUTF8 (fp, m_message);
    }
    if (m_next) {
        m_next->encodeAll (fp);
    }
}

void LocaleEntry::saveAll (FILE * fp) {
    fprintf (fp, "%s:%s\n", m_key, m_message);
    if (m_next) {
        m_next->saveAll (fp);
    }
}

void LocaleEntry::setAllMarks (int m) { 
    setMark (m); 
    if (m_next) {
        m_next->setAllMarks (m);
    }
}


LocaleSet::LocaleSet (char * filename) {
    m_filename = strdup (filename ? filename : "unknown");
    m_entry = m_last = NULL;
    m_nbEntries = 0;
}

LocaleSet::~LocaleSet () {
    if (m_entry) {
        delete m_entry;
    }
    free (m_filename);
}

LocaleEntry * LocaleSet::getEntry (char * key) {
    if (m_entry) {
        return m_entry->find (key);
    }
    return NULL;
}

int LocaleSet::findEntry (char * key) {
    LocaleEntry * e = getEntry (key);
    return e == NULL ? -1 : e->m_id;
}

int LocaleSet::addEntry (char * key, char * message) {
    LocaleEntry * l = new LocaleEntry (m_nbEntries, key, message, NULL);
    if (m_entry == NULL) {
        m_entry = l;
    } else {
        m_last->m_next = l;
    }
    m_last = l;
    return m_nbEntries++;
}

int LocaleSet::updateEntry (char * key, char * message) {
    if (m_entry) {
        m_entry->update (key, message);
    }
    return -1;
}

void LocaleSet::setAllMarks (int m) { 
    if (m_entry) {
        m_entry->setAllMarks (m);
    }
}

void LocaleSet::encode (FILE * fp) { 
    fprintf (fp, "%c%c", m_nbEntries / 256, m_nbEntries % 256);
    if (m_entry) {
        m_entry->encodeAll (fp);
    }
}

void LocaleSet::save (FILE * fp) { 
    if (m_entry) {
        m_entry->saveAll (fp);
    }
}

int LocaleSet::compareWithModel (LocaleSet * model) {
    int missing = 0;
    setAllMarks (0);
    model->setAllMarks (0);
    LocaleEntry * e = m_entry;
    m_nbEntries = 0;
    while (e != NULL) {
        LocaleEntry * found = model->getEntry (e->m_key);
        if (found == NULL) {
            fprintf (stdout, "Warning: Localized entry for '%s' is only defined in %s\n", e->m_key, m_filename);
            e->m_id = -1;
        } else {
            e->m_id = found->m_id;
            found->setMark (1);
            m_nbEntries++;
        }
        e = e->m_next;
    }
    e = model->m_entry;
    while (e != NULL) {
        if (e->m_mark == 0) {
            fprintf (stderr, "Error: Localized entry for '%s' is missing in %s\n", e->m_key, m_filename);
            missing++;
        }
        e = e->m_next;
    }
    return missing;
}

// +-----------------------------------------+
// | class LocaleManager                     |
// +-----------------------------------------+

LocaleManager * LocaleManager::s_manager = NULL;
char LocaleManager::key [LOCALE_KEY_LEN];
char LocaleManager::msg [LOCALE_MSG_LEN];

LocaleManager * LocaleManager::getManager () {
    if (s_manager == NULL) {
        s_manager = new LocaleManager ();
    }
    return s_manager;
}

LocaleManager::LocaleManager () {
    m_master = new LocaleSet ("default");
    m_extra = NULL;
    m_missingTranslations = 0;
}

LocaleManager::~LocaleManager () {
    if (m_master) { delete m_master; }
    if (m_extra) { delete m_extra; }
}

void LocaleManager::checkMissingTranslations () {
    int missing = getManager()->m_missingTranslations;
    if (missing == 1) {
        fprintf (stderr, "Locale: One translation is missing (check above error). You're nearly there !\n");
    } else if (missing > 1) {
        fprintf (stderr, "Locale: %i translations are missing (check above errors)\n", missing);
    }
}

void LocaleManager::writeUTF8 (FILE * fp, char * v) {
    // 233 = 11101001 = 110xxxxx + 10 yyyyyy => 11000011 + 10101001 = C3A9
    //fwrite (v, 1, strlen (v), fp);
    for (unsigned int i = 0; i < strlen (v); i++) {
        switch (v[i]) {
            // convert some DOS chars commonly used
        case 0xE9: fprintf (fp, "%c%c", 0xC3, 0xA9); break; // e cute
        case 0xE8: fprintf (fp, "%c%c", 0xC3, 0xA8); break; // e grave
        case 0xE7: fprintf (fp, "%c%c", 0xC3, 0xA7); break; // c cedil
        case 0xE0: fprintf (fp, "%c%c", 0xC3, 0xA0); break; // a grave
        case 0xEF: fprintf (fp, "%c%c", 0xC3, 0xAF); break; // i trema
        case 0xEA: fprintf (fp, "%c%c", 0xC3, 0xAA); break; // e circonflex
        default:   fprintf (fp, "%c", v[i]); break;
        }
    }
    fprintf (fp, "%c", 0);
}

int LocaleManager::getEntryID (char * key) {
    return getManager()->m_master->findEntry (key);
}

int LocaleManager::split (char * v) {
    bool hasMsg = false;
    key[0] = msg[0] = '\0';
    if (v) {
        int len = strlen (v);
        // check for localize string in the form "@[ident:label]" ex: @[HelloWorld:bonjour tout le monde]
        if ( len >= 4 && v[0] == '@' && v[1] == '[') { // the minimal form is @[x]
            //fprintf (stderr, "LOCALE: checking for %s / %d\n", v, len);
            // check for ident
            int i = 2;
            int j = 2;
            while (v[i] != 0 && v[i] != ':' && v[i] != ']') {
                i++;
            }
            if (v[i] == ':') { // we have an ident
                if ( (i-2) >= LOCALE_KEY_LEN) { fprintf (stderr, "Locale: key too big: %s (max is %d)\n", v, LOCALE_KEY_LEN); exit (1); }
                strncpy (key, &v[2], i-2);
                key [i-2] = 0;
                //fprintf (stderr, "    > got a pair, key is %s", key);
                j = i+1;
                while (v[i] != 0 && v[i] != ']') {
                    i++;
                }
                hasMsg = true;
            }
            // @(a:b)  @(abc)
            // 012345  012345
            if (v[i] == ']') { // we have a locale string 
                if (v[i+1] != '\0') { // check if ending marker is really at the end
                    fprintf (stderr, "Locale: syntax error in local definition: %s\n", v);
                    return 0;
                }
                if (key[0] != '\0') {
                    if ( (i-j) >= LOCALE_MSG_LEN) { fprintf (stderr, "Locale: message too big: %s (max is %d)\n", v, LOCALE_MSG_LEN); exit (1); }
                    strncpy (msg, &v[j], i-j);
                    msg[i-j] = 0;
                    //fprintf (stderr, ", msg is %s\n", msg);
                } else {
                    if ( (i-j) >= LOCALE_KEY_LEN) { fprintf (stderr, "Locale: key too big: %s (max is %d)\n", v, LOCALE_KEY_LEN); exit (1); }
                    strncpy (key, &v[j], i-j);
                    key[i-j] = 0;
                    //fprintf (stderr, "    > got the single key %s\n", key);
                }
            } else {
                fprintf (stderr, "error: bad format for locale string %s\n", v);
            }
        }
    }
    return key[0] != '\0' ? (hasMsg ? 2 : 1) : 0;
}

int LocaleManager::addEntry (char * v) {
    int mode = split (v);
    if (mode != 0) { // has key
        int id = getManager()->m_master->findEntry (key);
        if (id == -1) { // no previous entry
            //fprintf (stderr, "$$ adding new Locale entry: %s : %s\n", key, msg);
            id = getManager()->m_master->addEntry (key, msg);
        } else if (strlen (msg) != 0) { // check if message is (re)defined by a non empty translation
            fprintf (stderr, "Warning: redefining new Locale entry: %s : %s\n", key, msg);
            getManager ()->m_master->updateEntry (key, msg);
        } // else redefined entry with empty string => ignore !
          // this allows to use the @[key:] multiple times without warnings when not using default locale.
        return id;
    }
    return -1;
}

void LocaleManager::saveDefault () {
    if (m_master == NULL || m_master->nbEntries() == 0) {
        // nothing to save!
        return;
    }

    FILE * fp = fopen ("default.lng", "wb");
    if (fp) {
        m_master->save (fp);
        fclose (fp);
    } else {
        fprintf (stderr, "Error: cannot open default.lng for writing\n");
    }
}

// void LocaleManager::encodeDefault () {
//     FILE * fp = fopen ("default.loc", "wb");
//     if (fp) {
//         if (m_master) {
//             m_master->encode (fp);
//         }
//         fclose (fp);
//     } else {
//         fprintf (stderr, "Fatal error: cannot open default.loc for writing\n");
//         exit (1);
//     }
// }

void LocaleManager::encodeExtra (FILE * fp, char * filename, char * langName) {
    LocaleSet * extra = new LocaleSet (filename);
    char c = fgetc (fp);
    while (!feof (fp)) {
        // check for comment
        while (c == '#') {
            while (!feof (fp) && c != '\n') {
                c = fgetc (fp);
            }
            c = fgetc (fp);
        }
        char * tmp = key;
        while (!feof (fp) && c != ':') {
            *tmp++ = c;
            c = fgetc (fp);
        }
        *tmp = 0;
        tmp = msg;
        c = fgetc (fp);
        while (!feof (fp) && c != '\r' && c != '\n') {
            if (c == '\\') {
                c = fgetc (fp);
                switch (c) {
                case 'U': c = 16; break; // ^P
                case 'D': c = 14; break; // ^N
                case 'R': c = 6;  break; // ^F
                case 'L': c = 2;  break; // ^B
                case 't': c = 9;  break; // ^I
                case 'n': c = 10; break; // ^J
                case 'r': c = 13; break; // ^M
                }
            }
            *tmp++ = c;
            c = fgetc (fp);
        }
        *tmp = 0;
        extra->addEntry (key, msg);
        if (c == '\r') { // eat optional \n after \r
           c = fgetc(fp);
           if (c != '\n') {
               continue;
           }
        }
        c = fgetc (fp);
    }
    int missing = extra->compareWithModel (m_master);
    if (missing > 0) {
        fprintf (stderr, "Error: the localization file %s is missing %i translation(s)!\n", filename, missing);
        m_missingTranslations += missing;
    }
    fp = fopen (langName, "wb");
    if (fp) {
        extra->encode (fp);
        fclose (fp);
    } else {
        fprintf (stderr, "Fatal error: cannot open %s for writing\n", langName);
        exit (1);
    }
    delete extra;
}
