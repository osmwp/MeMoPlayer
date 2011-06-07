#ifndef __CODE_TABLES__
#define __CODE_TABLES__

// Tables to keep Strings and Number unique for each Script
// Totaly brainless implementation...

#define MAX_STRING 256
#define MAX_INTS 256

class StringTable {
    char *m_entries[MAX_STRING];
    int m_lengths[MAX_STRING];
    int m_nbEntries;
    int m_totalLength;
public:
    StringTable ();
    ~StringTable ();
    int findOrAdd (char * s);
    unsigned char * generate (int &len);
    int getSize ();
};


class IntTable {
    int m_entries [MAX_INTS]; 
    int m_nbEntries;
public:
    IntTable ();
    int findOrAdd (int v);
    unsigned char * generate (int & len);
    int getSize ();
};

#endif // __CODE_TABLES__
