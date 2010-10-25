//#condition api.xparse2
package memoplayer;

/* Copyright (c) 2000 Michael Cla�en <mclassen@internet.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * $Id: Xparse.java,v 1.1 2000/04/03 07:43:37 classen Exp $
 */

// Derived from Javascript version:

// Ver .91 Feb 21 1998
//////////////////////////////////////////////////////////////
//
//    Copyright 1998 Jeremie
//    Free for public non-commercial use and modification
//    as long as this header is kept intact and unmodified.
//    Please see http://www.jeremie.com for more information
//    or email jer@jeremie.com with questions/suggestions.
//
///////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////
////////// Simple XML Processing Library //////////////////////
///////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////
////   Fully complies to the XML 1.0 spec
////   as a well-formed processor, with the
////   exception of full error reporting and
////   the document type declaration(and it's
////   related features, internal entities, etc).
///////////////////////////////////////////////////////////////


import java.util.Hashtable;

/**
 * Simple XML parser derived from the XParse Javascript parser;
 * Please see http://www.jeremie.com for more information on this.
 * Quoting Jeremie:
 * "Fully complies to the XML 1.0 spec
 * as a well-formed processor, with the
 * exception of full error reporting and
 * the document type declaration(and it's
 * related features, internal entities, etc)."
 *
 * @author    Michael Cla�en
 * @version   $Revision: 1.1 $
 */
public class Xparse {

     XNode XMLNode;
     //public XNode XMLCurrentNode;
     
     public Xparse(){
         XMLNode=new XNode(this);
     }
    /**
     * Helper function for matching Javascript's definition
     * of the substring function to not cause an IndexOutOfBoundsException
     * when length exceeds string length but return the remainder
     * of the string instead MC20001214
     *
     * @param     s the string to slice
     * @param     start the starting position within s
     * @param     length the number of characters to slice
     * @return    the substring
     */
    private String substring(String s, int start, int length) {
        if (s.length() > start + length)
            return s.substring(start, length);
        else
            return s.substring(start);
    }

    /** an internal fragment that is passed between functions
     */
    class Frag {
        public String str;
        public XJSArray ary;
        public String end;
        public Frag()
            {
                this.str = new String();
                this.ary = new XJSArray();
                this.end = new String();
            }
    }

    // global vars to track element UID's for the index
     int count = 0;
     XJSArray index = new XJSArray();

    /**
     * Main public function that is called to
     * parse the XML string and return a root element object
     *
     * @param     src the object's index in the array
     * @return    the parsed XML's root Node
     */
    public XNode parse(String src) {

        count = 0;
        index = new XJSArray();

        Frag frag = new Frag();

        // remove bad \r characters and the prolog
        frag.str = prolog(src);

        // create a root element to contain the document
        XNode root = XMLNode.createRootelement();
        root.name="ROOT";

        // main recursive function to process the xml
        frag = compile(frag);

        // all done, lets return the root element + index + document
        root.contents = frag.ary;
        root.index = index;
        index = new XJSArray();
        return root;
    }

    /**
     * transforms raw text input into a multilevel XJSArray
     *
     * @param     frag the input fragment
     * @return    the output fragment
     */
    Frag compile(Frag frag) {

        // keep circling and eating the str
        while(true)
            {
                // when the str is empty, return the fragment
                if(frag.str.length() == 0)
                    {
                        return frag;
                    }

                int TagStart = frag.str.indexOf("<");

                if(TagStart != 0)
                    {
                        // theres a chunk of characters here, store it and go on
                        int thisary = frag.ary.length();
                        frag.ary.setElementAt(XMLNode.createChardata(), thisary);
                        if(TagStart == -1)
                            {
                                frag.ary.setElementAt(entity(frag.str), thisary, XJSArray.TYPE_VALUE/*.Value*/);
                                frag.str = "";
                            }
                        else
                            {
                                frag.ary.setElementAt(entity(substring(frag.str,0,TagStart)), thisary, XJSArray.TYPE_VALUE/*.Value*/);
                                frag.str = substring(frag.str,TagStart,frag.str.length());
                            }
                    }
                else
                    {
                        // determine what the next section is, and process it
                        if(substring(frag.str,1,2).equals("?"))
                            {
                                frag = tagPI(frag);
                            }
                        else
                            {
                                if(substring(frag.str,1,4).equals("!--"))
                                    {
                                        tagComment(frag);
                                    }
                                else
                                    {
                                        if(substring(frag.str,1,9).equals("![CDATA["))
                                            {
                                                frag = tagCData(frag);
                                            }
                                        else
                                            {
                                                if(substring(frag.str,1,frag.end.length() + 3).equals("/" + frag.end + ">") || strip(substring(frag.str,1,frag.end.length() + 3)).equals("/" + frag.end))
                                                    {
                                                        // found the end of the current tag, end the recursive process and return
                                                        frag.str = substring(frag.str,frag.end.length() + 3,frag.str.length());
                                                        frag.end = "";
                                                        return frag;
                                                    }
                                                else
                                                    {
                                                        frag = tagElement(frag);
                                                    }
                                            }
                                    }
                            }

                    }
            }
        //MC return "";
    }

    //// functions to process different tags

    /**
     * process an XML element
     *
     * @param     frag the input fragment
     * @return    the output fragment
     */
    Frag tagElement(Frag frag)
        {
            // initialize some temporary variables for manipulating the tag
            int close = frag.str.indexOf(">");
            boolean empty = (substring(frag.str,close - 1, close).equals("/"));
            if(empty)
                {
                    close -= 1;
                }

            // split up the name and attributes
            String starttag = normalize(substring(frag.str,1,close));
            int nextspace = starttag.indexOf(" ");
            String attribs = new String();
            String name = new String();
            if(nextspace != -1)
                {
                    name = starttag.substring(0,nextspace);
                    attribs = starttag.substring(nextspace + 1,starttag.length());
                }
            else
                {
                    name = starttag;
                }

            int thisary = frag.ary.length();
            frag.ary.setElementAt(XMLNode.createElement(), thisary);
            frag.ary.setElementAt(strip(name), thisary, XJSArray.TYPE_NAME/*.Name*/);
            if(attribs.length() > 0)
                {
                    frag.ary.setElementAt(attribution(attribs), thisary, XJSArray.TYPE_ATTR/*.Attributes*/);
                }
            if(!empty)
                {
                    // !!!! important,
                    // take the contents of the tag and parse them
                    Frag contents = new Frag();
                    contents.str = substring(frag.str,close + 1,frag.str.length());
                    contents.end = name;
                    contents = compile(contents);
                    frag.ary.setElementAt(contents.ary, thisary, XJSArray.TYPE_CONTENT/*.Contents*/);
                    frag.str = contents.str;
                }
            else
                {
                    frag.str = substring(frag.str,close + 2,frag.str.length());
                }
            return frag;
        }

    /**
     * process an XML processing instruction (PI)
     *
     * @param     frag the input fragment
     * @return    the output fragment
     */
    Frag tagPI(Frag frag)
        {
            int close = frag.str.indexOf("?>");
            String val = substring(frag.str,2, close);
            int thisary = frag.ary.length();
            frag.ary.setElementAt(XMLNode.createPi(), thisary);
            frag.ary.setElementAt(val, thisary, XJSArray.TYPE_VALUE/*.Value*/);
            frag.str = substring(frag.str,close + 2, frag.str.length());
            return frag;
        }

    /**
     * process an XML comment
     *
     * @param     frag the input fragment
     * @return    the output fragment
     */
    void tagComment(Frag frag)
        {
            int close = frag.str.indexOf("-->");
            frag.str = substring(frag.str,close + 3, frag.str.length());
        }

    /**
     * process XML character data (CDATA)
     *
     * @param     frag the input fragment
     * @return    the output fragment
     */
    Frag tagCData(Frag frag)
        {
            int close = frag.str.indexOf("]]>");
            String val = substring(frag.str,9, close);
            int thisary = frag.ary.length();
            frag.ary.setElementAt(XMLNode.createChardata(), thisary);
            frag.ary.setElementAt(val, thisary, XJSArray.TYPE_VALUE/*.Value*/);
            frag.str = substring(frag.str,close + 3, frag.str.length());
            return frag;
        }

    /**
     * util for element attribute parsing
     *
     * @param     attribute string
     * @return    an JSArray of all of the keys = values
     */
    Hashtable attribution(String str)
        {
            Hashtable all = new Hashtable();
            while(true)
                {
                    int eq = str.indexOf("=");
                    if(str.length() == 0 || eq == -1)
                        {
                            return all;
                        }

                    int id1 = str.indexOf("\'");
                    int id2 = str.indexOf("\"");
                    int ids = 0; //MC = new Number();
                    String id = new String();
                    if((id1 < id2 && id1 != -1) || id2 == -1)
                        {
                            ids = id1;
                            id = "\'";
                        }
                    if((id2 < id1 || id1 == -1) && id2 != -1)
                        {
                            ids = id2;
                            id = "\"";
                        }
                    int nextid = str.indexOf(id,ids + 1);
                    String val = str.substring(ids + 1,nextid);

                    String name = strip(str.substring(0,eq));
                    all.put(name, entity(val));
                    str = str.substring(nextid + 1,str.length());
                }
            //MC return "";
        }

    /**
     * util to remove \r characters from input string
     *
     * @param     attribute string
     * @return    the xml string without a prolog
     */
    String prolog(String str)
        {
            XJSArray a = new XJSArray();

            a.split(str, "\r\n");
            str = a.join("\n");
            a.split(str, "\r");
            str = a.join("\n");

            int start = str.indexOf("<");
            if(str.substring(start,start + 3).equals("<?x") || str.substring(start,start + 3).equals("<?X") )
                {
                    int close = str.indexOf("?>");
                    str = str.substring(close + 2,str.length());
                }
            start = str.indexOf("<!DOCTYPE");
            if(start != -1)
                {
                    int close = str.indexOf(">",start) + 1;
                    int dp = str.indexOf("[",start);
                    if(dp < close && dp != -1)
                        {
                            close = str.indexOf("]>",start) + 2;
                        }
                    str = str.substring(close,str.length());
                }
            return str;
        }

    /**
     * util to remove white characters from input string
     *
     * @param     string
     * @return    stripped string
     */
    String strip(String str)
        {
            XJSArray A = new XJSArray();

            A.split(str, "\n");
            str = A.join("");
            A.split(str, " ");
            str = A.join("");
            A.split(str, "\t");
            str = A.join("");

            return str;
        }

    /**
     * util to replace white characters in input string
     *
     * @param     string
     * @return    normalized string
     */
    String normalize(String str)
        {
            XJSArray A = new XJSArray();

            A.split(str, "\n");
            str = A.join(" ");
            A.split(str, "\t");
            str = A.join(" ");

            return str;
        }

    /**
     * util to replace internal entities in input string
     *
     * @param     string
     * @return    string with replaced entitities
     */
    String entity(String str)
        {
            XJSArray A = new XJSArray();

            A.split(str, "&lt;");
            str = A.join("<");
            A.split(str, "&gt;");
            str = A.join(">");
            A.split(str, "&quot;");
            str = A.join("\"");
            A.split(str, "&apos;");
            str = A.join("\'");
            A.split(str, "&amp;");
            str = A.join("&");

            return str;
        }

}
