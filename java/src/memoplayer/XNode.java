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
 * $Id: Node.java,v 1.2 2001/01/05 19:32:05 classen Exp $
 */


import java.util.Hashtable;

/**
 * A node of an XML DOM tree;
 *
 * @author    Michael Cla�en
 * @version   $Revision: 1.2 $
 */
public class XNode {

  public /*String*/int type;
  public String name;
  public String value;
  public Hashtable attributes;
  public int uid;
  public XJSArray contents;
  public XJSArray index;

  /* node types
  static final String Element = "element";
  static final String CharData = "chardata";
  static final String PI = "pi";
  static final String Comment = "comment";*/
  
  static final int TYPE_ELEM = 1;
  static final int TYPE_CDATA = 2;
  static final int TYPE_PI = 3;
  //static final int TYPE_COMMENT = 4;
  Xparse m_parser;

  public XNode(Xparse parser){
      m_parser=parser;
  }
  public XNode(){
  }
/////////////////////////
//// the object constructors for the hybrid DOM

  /**
   * factory method for element nodes
   *
   * @return    a Node of type element
   */
   XNode createElement() {
      XNode n = new XNode();
      n.type = TYPE_ELEM;//Element;// int !!
      n.name = new String();
      n.attributes = new Hashtable();
      n.contents = new XJSArray();
      n.uid = m_parser.count++;
      m_parser.index.setElementAt(n, n.uid);
    return n;
  }

  /**
   * factory method for the root element
   *
   * @return    a rootelement Node
   */
   XNode createRootelement() {
    return createElement();
  }

  /**
   * factory method for chardata nodes
   *
   * @return    a chardata Node
   */
   XNode createChardata() {
      XNode n = new XNode();
      n.type = TYPE_CDATA;//CharData;
      n.value = new String();
    return n;
  }

  /**
   * factory method for PI nodes
   *
   * @return    a PI Node
   */
   XNode createPi() {
      XNode n = new XNode();
      n.type = TYPE_PI;//PI;
      n.value = new String();
    return n;
  }

  /**
   * factory method for comment nodes
   *
   * @return    a comment Node
   */
  /*static XNode createComment()
  {
      XNode n = new XNode();
      n.type = TYPE_COMMENT;//Comment;
      n.value = new String();
    return n;
  }*/

  /**
   * returns the character data in the first child element;
   * returns nonsense if the first child element ist not chardata
   *
   * @return    the characters following an element
   */
  public String getCharacters() {
      return contents.length()==0?null:((XNode)contents.elementAt(0)).value;
      
  }
  public String getAttribute(String key){
      return (String)attributes.get(key);
  }

  /**
   * find the node matching a certain occurrence of the path description
   *
   * @param     path an XPath style expression without leading slash
   * @param     occur array indicating the n'th occurrence of a node matching each simple path expression
   *
   * @return    the n'th Node matching the path description
   */
  public XNode find(String path, /*int[]*/String occur) {
      int sepIndex=occur.indexOf(";");
      //int nbOccur=0;
      XJSArray tabOccur = new XJSArray();
      tabOccur.split(occur,";");
      
      XNode n = this;
      XJSArray a = new XJSArray();
    a.split(path, "/");
    int i = 0;
    while (i < a.length()) {
      n = findChildElement(n, (String)a.elementAt(i),Integer.parseInt((String)tabOccur.elementAt(i)));
      if (n == null) return null;
      i++;
    }
    return n;
  }

  /**
   * find the child node matching a certain occurrence of a simple path description
   *
   * @param     parent the parent node to start from
   * @param     simplePath one element of an XPath style expression
   * @param     occur the n'th occurance of a node matching the path expression
   *
   * @return    the n'th child Node matching the simple path description
   */
  XNode findChildElement(XNode parent, String simplePath, int occur) {
      XJSArray a = parent.contents;
    XNode n;
    int  found = 0;
    int i = 0;
    String tag;
    do {
      n = (XNode)a.elementAt(i);
      if(n==null) return null;//FTE 28/05/08
      ++i;
      tag = (n.name != null) ? n.name : "";
      int colonPos = tag.indexOf(':');
      tag = (colonPos == -1) ? tag : tag.substring(colonPos + 1);
      if (simplePath.equals(tag)) ++found;
    } while (i < a.length() && found < occur);
    return (found == occur) ? n : null;
  }

}

