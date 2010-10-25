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
 * $Id: JSArray.java,v 1.1 2000/04/03 07:43:37 classen Exp $
 */


import java.util.Hashtable;
import java.util.Vector;

/**
 * Mimics a Javascript array;
 * not completely generic because only specific properties are allowed
 * <p>would need Reflection to make it completely generic
 *
 * @author    Michael Cla�en
 * @version   $Revision: 1.1 $
 */
class XJSArray {

  // underlying element holder
  private Vector v = new Vector();

  // constants for the currently supported object properties
 /* static final String Name = "name";
  static final String Attributes = "attributes";
  static final String Contents = "contents";
  static final String Value = "value";*/
  
  static final int TYPE_NAME = 1;
  static final int TYPE_ATTR = 2;
  static final int TYPE_CONTENT = 3;
  static final int TYPE_VALUE = 4;

  /**
   * gets the object with a certain index
   *
   * @param     idx the object's index in the array
   * @return    Object at the respective index
   */
  Object elementAt(int idx) {
    if (idx>v.size()-1) return null;
    return v.elementAt(idx);
  }

  /**
   * sets the object with a certain index
   *
   * @param     val the object to be set in the array
   * @param     idx the object's index in the array
   */
  void setElementAt(Object val, int idx) {
    v.insertElementAt(val, idx);
  }

  /**
   * sets the object with a certain index
   *
   * @param     val the object' property to be set
   * @param     idx the object's index in the array
   * @param     prop the name of the object's property to be set;
   *            currently can only be: name, attributes, contents, value
   */
  void setElementAt(Object val, int idx, /*String*/int prop) {
      XNode n = (XNode) v.elementAt(idx);
      switch(prop){

      case TYPE_NAME:
          n.name = (String) val;
          break;
      case TYPE_ATTR:
          n.attributes = (Hashtable) val;
          break;
      case TYPE_CONTENT:
          n.contents = (XJSArray) val;
          break;
      case TYPE_VALUE:
          n.value = (String) val;
          break;

      }

  }

  /**
   * returns the length / size of the array
   *
   * @return    the array length
   */
  int length() {
    return v.size();
  }

  /**
   * splits a string into an array of strings, broken at a distinct separator
   *
   * @param     str the String to be split
   * @param     sep the seperator at which to split
   */
  void split(String str, String sep) {
    v.removeAllElements();
    int oldidx = 0, newidx, skip = sep.length();
    while((newidx = str.indexOf(sep, oldidx)) != -1) {
      v.addElement(str.substring(oldidx, newidx));
      oldidx = newidx + skip;
    }
    v.addElement(str.substring(oldidx));
  }

  /**
   * join this array into one string delimited by a separator
   *
   * @param     sep the seperator to put in between the array elements
   * @return    the joined String
   */
  String join(String sep) {
    int no = 0;
    StringBuffer sb = new StringBuffer();
    while (no < v.size()) {
      sb.append(v.elementAt(no));
      if (++no < v.size()) sb.append(sep);
    }
    return sb.toString();
  }

}
