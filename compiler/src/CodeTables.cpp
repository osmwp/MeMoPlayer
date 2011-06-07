#include "CodeTables.h"
# include <stdio.h>
# include <stdlib.h>
# include <string.h>

StringTable::StringTable () {
    m_nbEntries = 0;
    m_totalLength = 0;
}

StringTable::~StringTable () {
    for (int i = 0; i < m_nbEntries; i++) {
        free (m_entries[i]);
    }
}

int StringTable::findOrAdd (char * s) {
    int l = strlen (s) + 1;
    for(int i = 0; i < m_nbEntries; i++) {
        if (m_lengths[i] == l && strcmp (m_entries[i], s) == 0) {
            //printf ("DBG: StrinTable: Reusing %s (%d) at %d\n", s, strlen(s), i);
            return i;
        }
    }
    if (m_nbEntries == MAX_STRING) {
        fprintf (stderr, "Script: StringTable is limited to %d entries !\n", MAX_STRING);
        exit (1);
    }
    //printf ("DBG: StringTable: Adding %s (%d) at %d\n", s, strlen(s), m_nbEntries);
    m_entries[m_nbEntries] = strdup(s);
    m_lengths[m_nbEntries] = l;
    m_totalLength += l;
    return m_nbEntries++;
}

unsigned char * StringTable::generate (int &len) {
    len = m_totalLength;
    if (len > 0) {
        unsigned char * buff = (unsigned char *) malloc (len);
        unsigned char * tmp = buff;
        for (int i = 0; i < m_nbEntries; i++) {
            //printf ("DBG: StringTable: memcpy: to %d from %s (%d)\n", tmp, se->m_entry, se->m_l);
            memcpy (tmp, m_entries[i], m_lengths[i]);
            tmp += m_lengths[i];
        }
        //printf ("DBG: StringTable: Generated table %d\n", len);
        return buff;
    }
    return NULL;        
}

int StringTable::getSize () {
    return m_nbEntries;
}


IntTable::IntTable () {
    m_nbEntries = 0;
}

int IntTable::findOrAdd (int v) {
    for (int i=0; i<m_nbEntries; i++) {
        if (m_entries[i] == v) {
            //printf ("DBG: IntTable: Reusing %d at %d\n", v, i);
            return i;
        }
    }
    if (m_nbEntries == MAX_INTS) {
        fprintf (stderr, "Script: IntTable is limited to %d entries !\n", MAX_INTS);
        exit (1);
    }
    m_entries[m_nbEntries] = v;
    //printf ("DBG: IntTable: Adding %d at %d\n", v, m_nbEntries);
    return m_nbEntries++;
}

unsigned char * IntTable::generate (int & len) {
    len = m_nbEntries * 4;
    unsigned char * buff = NULL;
    if (len > 0) {
        buff = (unsigned char *) malloc (len);
        unsigned char * t = buff;
        unsigned char * s; 
        for (int i=0; i<m_nbEntries; i++) {
            s = (unsigned char *) &m_entries[i];
            *(t++) = s[3];
            *(t++) = s[2];
            *(t++) = s[1];
            *(t++) = s[0];
        }
    }
    //printf ("DBG: IntTable: Generated table %d\n", len);
    return buff;
}

int IntTable::getSize () {
    return m_nbEntries;
}

