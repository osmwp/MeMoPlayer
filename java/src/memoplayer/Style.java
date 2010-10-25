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

package memoplayer;

public class Style extends Node {


    /** list of css */
    private static ObjLink s_allStyles;
    
//#ifdef api.richText
    private CSSList [] m_sheets;
//#endif
    private String [] m_openFiles;
    private int m_nbSheets;
    Style () {
        super(1);
        m_field[0] = new MFString (this); // url
    }
    
    
    void start (Context c) {
//#ifdef api.richText        
        openCssFiles (c);
//#endif
        m_isUpdated = false;
    }
    
    void stop (Context c) {
//#ifdef api.richText        
        closeCssFiles (c);
//#endif
   }

//#ifdef api.richText        
    CSSList openCssFile (Context c, String url) {
        // try from adatalink
        String data = c.decoder.getCssData (url);
        if (data == null && url != null && url.startsWith ("jar://")) { // try if is a jar
            File file = new File (url);
            data = file.readAll ();
        }
        if (data == null || data.length() == 0) {
            return null;
        } else {
            CSSReader reader = new CSSReader (data, CSSReader.BUFFER);//+CSSReader.DEBUG);
            return reader.getList ();
        }
    }

    void openCssFiles (Context c) {
        m_nbSheets = ((MFString)m_field[0]).m_size;
        m_sheets = new CSSList [m_nbSheets];
        m_openFiles = new String [m_nbSheets];
        for (int i = 0; i < m_nbSheets; i++) {
            m_sheets[i] = openCssFile (c,  m_openFiles [i] = ((MFString)m_field[0]).getValue (i));
        }
        s_allStyles = ObjLink.create (this, s_allStyles);
    }

    // only reopen the chnegd url: keep the same CSS data if teh url is teh same at the same index
    void reopenCssFiles (Context c) {
        int prevNbSheets = m_nbSheets;
        CSSList [] prevSheets = m_sheets;
        String [] prevFiles = m_openFiles;
        m_nbSheets = ((MFString)m_field[0]).m_size;
        m_sheets = new CSSList [m_nbSheets];
        m_openFiles = new String [m_nbSheets];

        // compare existing elements 
        int nbMin = prevNbSheets < m_nbSheets ? prevNbSheets : m_nbSheets;
        for (int i = 0; i < nbMin; i++) {
            m_openFiles[i] = ((MFString)m_field[0]).getValue (i);
            if (prevFiles[i].equals (m_openFiles[i]) == false) { // not the same
                m_sheets[i] = openCssFile (c,  m_openFiles [i]); // read the new one
                //Logger.println ("reopenCssFiles: index#"+i+" opening "+m_openFiles [i]);
            } else {
                m_sheets[i] = prevSheets[i]; // reuse existing one
                //Logger.println ("reopenCssFiles: index#"+i+" reusing "+m_openFiles [i]);
            }
        }

        if (prevNbSheets < m_nbSheets) { // more elements now => need to add them in the current list
            for (int i = prevNbSheets; i < m_nbSheets; i++) { // add new elements
                m_sheets[i] = openCssFile (c,  m_openFiles [i] = ((MFString)m_field[0]).getValue (i));
                //Logger.println ("reopenCssFiles: index#"+i+" adding "+m_openFiles [i]);
            }
        } // else => less elements now so just let clean up existing extra lists

        for (int i = 0; i < prevNbSheets; i++) { // clean up the lists of previous elements for GC
            prevFiles[i] = null;
            prevSheets [i] = null;
        }
    }

    void closeCssFiles (Context c) {
        if (s_allStyles != null) {
            s_allStyles = s_allStyles.remove (this);
        } else {
            Logger.println ("Style.stop: unexpected null static root");
        }
        for (int i = 0; i < m_nbSheets; i++) {
            m_sheets[i] = null;
        }
        m_sheets = null;
        m_nbSheets = 0;
    }
//#endif

    boolean compose(Context c, Region clip, boolean forceUpdate) {
        if (m_isUpdated) {
            m_isUpdated = false;
//#ifdef api.richText
            reopenCssFiles (c);
//#endif
        }
        return false;
    }
    
    
    public void fieldChanged(Field f) {
        m_isUpdated = true;
    }


//#ifdef api.richText 
    static int getInteger (CSSProp p, int coef) {
        if (p.m_type == CSSProp.TYPE_PERCENT) {
            return (int)((p.m_value*(long)coef)/100) >> 16;
        } else if (p.m_type == CSSProp.TYPE_PIXEL) {
            return p.m_value >> 16;
        } else {
            return (p.m_value*coef) >> 16;
        }
    }

    static int getFixFloat (CSSProp p, int coef) {
        if (p.m_type == CSSProp.TYPE_PERCENT) {
            return (int)((p.m_value*(long)coef)/100);
        } else if (p.m_type == CSSProp.TYPE_PIXEL) {
            return p.m_value;
        } else {
            return p.m_value*coef;
        }
    }

    static int getFixFloat2 (CSSProp p, int coef) {
        if (p.m_type2 == CSSProp.TYPE_PERCENT) {
            return (int)((p.m_value2*(long)coef)/100);
        } else if (p.m_type2 == CSSProp.TYPE_PIXEL) {
            return p.m_value2;
        } else {
            return p.m_value2*coef;
        }
    }

//     private CSSList findList (String tagName) {
//         for (int i = m_nbSheets-1; i >= 0; i--) {
//             CSSList l = m_sheets[i].findByTag (tagName);
//             if (l != null) {
//                 Logger.println ("Style.findList: found "+tagName+" in sheet # "+i);
//                 return l;
//             }
//         }
//         Logger.println ("Style.findList: cannot find "+tagName+" in "+this);
//         return null;
//     }

    CSSProp findProp (String tagName, int id) {
        for (int i = m_nbSheets-1; i >= 0; i--) {
            CSSList l = m_sheets[i] != null ? m_sheets[i].findByTag (tagName) : null;
            if (l != null) {
                //Logger.println ("Style.findList: found "+tagName+" in sheet # "+i);
                CSSProp p = l.m_properties;
                while (p != null) {
                    if (p.m_id == id) {
                        //Logger.println ("Style.findList: found attr #"+id+" in tag "+tagName+" of sheet #"+i);
                        return p;
                    }
                    p = p.m_next;
                }
            }
        }
        //Logger.println ("Style.findList: cannot find attr #"+id+" in "+tagName);
        return null;
    }

    static CSSProp getProperty (MFString tagName, String attrName) {
        ObjLink ol = s_allStyles;
        int id = CSSList.findAttrIndex (attrName);
        CSSProp p;
        if (id > -1 && tagName != null) { 
            while (ol != null) {
                for (int i = tagName.m_size-1; i >= 0; i--) {
                    if (ol.m_object != null) {
                        p = ((Style)ol.m_object).findProp (tagName.m_value[i], id);
                        if (p != null) {
                            return p;
                        }
                    }
                }
                ol = ol.m_next;
            }
        }
        return null;
    }
    static CSSProp getProperty (String tagName, String attrName) {
        ObjLink ol = s_allStyles;
        int id = CSSList.findAttrIndex (attrName);
        if (id > -1 && tagName != null) { 
            while (ol != null) {
                CSSProp p = ((Style)ol.m_object).findProp (tagName, id);
                if (p != null) {
                    return p;
                }
                ol = ol.m_next;
            }
        }
        return null;
    }
//#endif
}
