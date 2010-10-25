//#condition api.jsonrpc
/*
* This software has been developed by France Telecom, FT/BD/DIH/HDM
*
* Copyright France Telecom 2008
*
* COPYRIGHT:
*    This file is the property of FRANCE TELECOM. It cannot be copied,  
*    used, or modified without obtaining an authorization from the
*    authors or a mandated member of FRANCE TELECOM.
*    If such an authorization is provided, any modified version or copy
*    of the software has to contain this header.
*
* WARRANTIES:
*    This software is made available by the authors in the hope that it
*    will be useful, but without any warranty. France Telecom is not
*    liable for any consequence related to the use of the provided
*    software.
*
* AUTHORS: Renaud Cazoulat, Marc Capdevielle
*
* VERSION: 0.00
*
* DATE: 17/09/2008 (major revision)
*/
package memoplayer;

/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is mozilla.org code.
 *
 * The Initial Developer of the Original Code is Mozilla Foundation.
 * Portions created by the Initial Developer are Copyright (C) 2006
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Douglas Crockford <douglas@crockford.com> (state machine, C version)
 *   Robert Sayre <sayrer@gmail.com> (Java version)
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;
//import java.math.BigInteger;

public class JsonDecoder {

  /*
 Characters are mapped into these 32 symbol classes. This allows for
 significant reductions in the size of the state transition table.
  */

  // XXXsayrer TODO: investigate J2MEPolish defines for these

  /* error */
  static final int S_ERR = -1;

  /* space */
  static final int S_SPA = 0;

  /* other whitespace */
  static final int S_WSP = 1;

  /* {  */
  static final int S_LBE = 2;

  /* } */
  static final int S_RBE = 3;

  /* [ */
  static final int S_LBT = 4;

  /* ] */
  static final int S_RBT = 5;

  /* : */
  static final int S_COL = 6;

  /* , */
  static final int S_COM = 7;

  /* " */
  static final int S_QUO = 8;

  /* \ */
  static final int S_BAC = 9;

  /* / */
  static final int S_SLA = 10;

  /* + */
  static final int S_PLU = 11;

  /* - */
  static final int S_MIN = 12;

  /* . */
  static final int S_DOT = 13;

  /* 0 */
  static final int S_ZER = 14;

  /* 123456789 */
  static final int S_DIG = 15;

  /* a */
  static final int S__A_ = 16;

  /* b */
  static final int S__B_ = 17;

  /* c */
  static final int S__C_ = 18;

  /* d */
  static final int S__D_ = 19;

  /* e */
  static final int S__E_ = 20;

  /* f */
  static final int S__F_ = 21;

  /* l */
  static final int S__L_ = 22;

  /* n */
  static final int S__N_ = 23;

  /* r */
  static final int S__R_ = 24;

  /* s */
  static final int S__S_ = 25;

  /* t */
  static final int S__T_ = 26;

  /* u */
  static final int S__U_ = 27;

  /* ABCDF */
  static final int S_A_F = 28;

  /* E */
  static final int S_E = 29;

  /* everything else */
  static final int S_ETC = 30;


  /*
 This table maps the 128 ASCII characters into the 32 character classes.
 The remaining Unicode characters should be mapped to S_ETC.
  */
  static final int[] ascii_class = {
      S_ERR, S_ERR, S_ERR, S_ERR, S_ERR, S_ERR, S_ERR, S_ERR,
      S_ERR, S_WSP, S_WSP, S_ERR, S_ERR, S_WSP, S_ERR, S_ERR,
      S_ERR, S_ERR, S_ERR, S_ERR, S_ERR, S_ERR, S_ERR, S_ERR,
      S_ERR, S_ERR, S_ERR, S_ERR, S_ERR, S_ERR, S_ERR, S_ERR,

      S_SPA, S_ETC, S_QUO, S_ETC, S_ETC, S_ETC, S_ETC, S_ETC,
      S_ETC, S_ETC, S_ETC, S_PLU, S_COM, S_MIN, S_DOT, S_SLA,
      S_ZER, S_DIG, S_DIG, S_DIG, S_DIG, S_DIG, S_DIG, S_DIG,
      S_DIG, S_DIG, S_COL, S_ETC, S_ETC, S_ETC, S_ETC, S_ETC,

      S_ETC, S_A_F, S_A_F, S_A_F, S_A_F, S_E, S_A_F, S_ETC,
      S_ETC, S_ETC, S_ETC, S_ETC, S_ETC, S_ETC, S_ETC, S_ETC,
      S_ETC, S_ETC, S_ETC, S_ETC, S_ETC, S_ETC, S_ETC, S_ETC,
      S_ETC, S_ETC, S_ETC, S_LBT, S_BAC, S_RBT, S_ETC, S_ETC,

      S_ETC, S__A_, S__B_, S__C_, S__D_, S__E_, S__F_, S_ETC,
      S_ETC, S_ETC, S_ETC, S_ETC, S__L_, S_ETC, S__N_, S_ETC,
      S_ETC, S_ETC, S__R_, S__S_, S__T_, S__U_, S_ETC, S_ETC,
      S_ETC, S_ETC, S_ETC, S_LBE, S_ETC, S_RBE, S_ETC, S_ETC
  };

  /*
 The state transition table takes the current state and the current symbol,
 and returns either a new state or an action. A new state is a number between
 0 and 29. An action is a negative number between -1 and -9. A JSON text is
 accepted if the end of the text is in state 9 and mode is MODE_DONE.
  */
  static final int[][] state_transition_table = {
      /* 0*/ { 0, 0, -8, -1, -6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
      /* 1*/ { 1, 1, -1, -9, -1, -1, -1, -1, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
      /* 2*/ { 2, 2, -8, -1, -6, -5, -1, -1, 3, -1, -1, -1, 20, -1, 21, 22, -1, -1, -1, -1, -1, 13, -1, 17, -1, -1, 10, -1, -1, -1, -1},
      /* 3*/ { 3, -1, 3, 3, 3, 3, 3, 3, -4, 4, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3},
      /* 4*/ {-1, -1, -1, -1, -1, -1, -1, -1, 3, 3, 3, -1, -1, -1, -1, -1, -1, 3, -1, -1, -1, 3, -1, 3, 3, -1, 3, 5, -1, -1, -1},
      /* 5*/ {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 6, 6, 6, 6, 6, 6, 6, 6, -1, -1, -1, -1, -1, -1, 6, 6, -1},
      /* 6*/ {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 7, 7, 7, 7, 7, 7, 7, 7, -1, -1, -1, -1, -1, -1, 7, 7, -1},
      /* 7*/ {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 8, 8, 8, 8, 8, 8, 8, 8, -1, -1, -1, -1, -1, -1, 8, 8, -1},
      /* 8*/ {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 3, 3, 3, 3, 3, 3, 3, 3, -1, -1, -1, -1, -1, -1, 3, 3, -1},
      /* 9*/ { 9, 9, -1, -7, -1, -5, -1, -3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
      /*10*/ {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 11, -1, -1, -1, -1, -1, -1},
      /*11*/ {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 12, -1, -1, -1},
      /*12*/ {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
      /*13*/ {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 14, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
      /*14*/ {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 15, -1, -1, -1, -1, -1, -1, -1, -1},
      /*15*/ {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 16, -1, -1, -1, -1, -1},
      /*16*/ {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
      /*17*/ {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 18, -1, -1, -1},
      /*18*/ {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 19, -1, -1, -1, -1, -1, -1, -1, -1},
      /*19*/ {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 9, -1, -1, -1, -1, -1, -1, -1, -1},
      /*20*/ {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 21, 22, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
      /*21*/ { 9, 9, -1, -7, -1, -5, -1, -3, -1, -1, -1, -1, -1, 23, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
      /*22*/ { 9, 9, -1, -7, -1, -5, -1, -3, -1, -1, -1, -1, -1, 23, 22, 22, -1, -1, -1, -1, 24, -1, -1, -1, -1, -1, -1, -1, -1, 24, -1},
      /*23*/ { 9, 9, -1, -7, -1, -5, -1, -3, -1, -1, -1, -1, -1, -1, 23, 23, -1, -1, -1, -1, 24, -1, -1, -1, -1, -1, -1, -1, -1, 24, -1},
      /*24*/ {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 25, 25, -1, 26, 26, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
      /*25*/ {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 26, 26, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
      /*26*/ { 9, 9, -1, -7, -1, -5, -1, -3, -1, -1, -1, -1, -1, -1, 26, 26, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
      /*27*/ {27, 27, -1, -1, -1, -1, -2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1},
      /*28*/ {28, 28, -8, -1, -6, -1, -1, -1, 3, -1, -1, -1, 20, -1, 21, 22, -1, -1, -1, -1, -1, 13, -1, 17, -1, -1, 10, -1, -1, -1, -1},
      /*29*/ {29, 29, -1, -1, -1, -1, -1, -1, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}
  };

  /*
  * As above, but for data values like strings, numbers, etc
  *
  * -9: Append
  * -8: Append-Single-Escape
  * -7: Append Unicode Escape
  * -5: Append Number
  * -1: Error // shouldn't happen much, as the first table should handle most validation
  */
  static final int[][] value_state_transition_table = {
   /*                   0     1     2     3     4     5     6     7     8     9     10    11    12    13    14    15    16    17    18    19    20    21    22    23    24    25    26    27    28    29    30*/
   /*                   S_SPA S_WSP S_LBE S_RBE S_LBT S_RBT S_COL S_COM S_QUO S_BAC S_SLA S_PLU S_MIN S_DOT S_ZER S_DIG S__A_ S__B_ S__C_ S__D_ S__E_ S__F_ S__L_ S__N_ S__R_ S__S_ S__T_ S__U_ S_A_F S_E   S_ETC */
   /* IN_VALUE  0 */  { 0,    0,   -1,   -1,   -1,   -1,   -1,   -1,    1,   -1,   -1,   -1,   -5,   -1,   -5,   -5,   -1,   -1,   -1,   -1,   -1,   12,   -1,   21,   -1,   -1,   17,   -1,   -1,   -1,   -1},
   /* IN_STRING 1 */  {-9,   -9,   -9,   -9,   -9,   -9,   -9,   -9,    1,    2,   -9,   -9,   -9,   -9,   -9,   -9,   -9,   -9,   -9,   -9,   -9,   -9,   -9,   -9,   -9,   -9,   -9,   -9,   -9,   -9,   -9},
   /* IN_BACK   2 */  {-1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -8,   -8,   -8,   -1,   -1,   -1,   -1,   -1,   -1,   -8,   -1,   -1,   -1,   -8,   -1,   -8,   -8,   -1,   -8,    8,   -1,   -1,   -1},
   /* IN_NUMBER 3 */  { 3,    3,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -5,   -5,   -5,   -1,   -1,   -1,   -1,   -5,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -5,   -1},
   /* IN_FLOAT  4 */  { 4,    4,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -5,   -5,   -1,   -1,   -1,   -1,   -5,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -5,   -1},
   /* NUM_EXP   5 */  {-1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -5,   -5,   -1,   -5,   -5,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1},
   /* FLOAT_EXP 6 */  {-1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -5,   -5,   -1,   -5,   -5,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1},
   /* EXP_DIGIT 7 */  { 7,    7,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -5,   -5,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1},
   /* UNICODE#1 8 */  {-1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -7,   -7,   -7,   -7,   -7,   -7,   -7,   -7,   -1,   -1,   -1,   -1,   -1,   -1,   -7,   -7,   -1},
   /* UNICODE#2 9 */  {-1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -7,   -7,   -7,   -7,   -7,   -7,   -7,   -7,   -1,   -1,   -1,   -1,   -1,   -1,   -7,   -7,   -1},
   /* UNICODE#3 10*/  {-1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -7,   -7,   -7,   -7,   -7,   -7,   -7,   -7,   -1,   -1,   -1,   -1,   -1,   -1,   -7,   -7,   -1},
   /* UNICODE#4 11*/  { 1,    1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -7,   -7,   -7,   -7,   -7,   -7,   -7,   -7,   -1,   -1,   -1,   -1,   -1,   -1,   -7,   -7,   -1},
   /* f         12*/  {-1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   13,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1},
   /* a         13*/  {-1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   14,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1},
   /* l         14*/  {-1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   15,   -1,   -1,   -1,   -1,   -1},
   /* s         15*/  {-1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   16,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1},
   /* e false   16*/  {16,   16,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1},
   /* t         17*/  {-1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   18,   -1,   -1,   -1,   -1,   -1,   -1},
   /* r         18*/  {-1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   19,   -1,   -1,   -1},
   /* u         19*/  {-1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   20,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1},
   /* e true    20*/  {20,   20,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1},
   /* n         21*/  {-1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   22,   -1,   -1,   -1},
   /* u         22*/  {-1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   23,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1},
   /* l         23*/  {-1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   24,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1},
   /* l null    24*/  {24,   24,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1,   -1}
  };

  static final int MODE_DONE = 1;
  static final int MODE_KEY = 2;
  static final int MODE_OBJECT = 3;
  static final int MODE_ARRAY = 4;

  private static final int MAX_DEPTH = 1024;
  private int[] stack;
  private int the_top;

  private boolean push(int mode) {
    the_top += 1;
    if (the_top >= MAX_DEPTH) {
      return false;
    }
    stack[the_top] = mode;
    return true;
  }

  private boolean pop(int mode) {
    if (the_top < 0 || stack[the_top] != mode) {
      return false;
    }
    stack[the_top] = 0;
    the_top -= 1;
    return true;
  }

  public static final int INVALID = 0;
  public static final int OBJECT = 1;
  public static final int ARRAY = 2;

  private int type = INVALID;
  public int getType() {
    return this.type;
  }

  private StringBuffer sb = new StringBuffer();

  private void clearBuf() {
    this.sb.setLength(0);
  }

  private Object makeValue (String valBuf, int value_type) throws IOException {
    Object obj = null;

    switch (value_type) {
      case 1:
        obj = valBuf; // String
        break;
      case 3:
        obj = new Integer(Integer.parseInt(valBuf));
        break;
      case 5:
        //obj = Double.valueOf(valBuf);
        obj = new FixFloat(Double.valueOf(valBuf).floatValue());
        break;
      case 4:
      case 6:
        //obj = new Float(Float.parseFloat(valBuf));
        obj = new FixFloat(Float.parseFloat(valBuf));
        break;
      case 16:
        obj = new Integer(0);
        break;
      case 20:
        obj = new Integer(1);
        break;
      case 24:
        //obj = new JSONNull();
        obj = new Integer(-1);
        break;
      default:
        throw new IOException();
    }

    return obj;
  }

  public Object decode(String s) throws Exception{
    // getBytes() on empty string throws ArrayOutOfBoundsException on Samsung F480
    return s != null && s.length() != 0 ? decode(new ByteArrayInputStream(s.getBytes())) : null;
  }

  public Object decode(InputStream is) throws Exception {
    int the_state = 0;
    int b;  /* the next character */
    int c;  /* the next character class */
    int s;  /* the next state */
    int the_index;
    the_top = -1;
    stack = new int[MAX_DEPTH];
    push(MODE_DONE);

    type = INVALID;
    Stack objStack = new Stack();
    Object rootObject = null;
    String key = null;
    int value_state = 0;
    int vs = 0;
    int utf = 0; // for unicode escapes
    clearBuf();
    
    char[] buf = new char[512];
    int readCount = 0;
    InputStreamReader isr = new InputStreamReader(is);
    while (readCount != -1) {
      readCount = isr.read(buf, 0, 512);
      
//#ifdef api.traffic
      // InputStream alwas comes from an HTTP connection
      Traffic.update (readCount);
//#endif

      for (the_index = 0; the_index < readCount; the_index += 1) {
        b = buf[the_index];
        if ((b & 127) == b) {
          c = ascii_class[b];
          if (c <= S_ERR) {
            throw new IOException();
          }
        } else {
          c = S_ETC;
        }

        s = state_transition_table[the_state][c];
        if (s < 0) {
          switch (s) {
            // empty }
            case-9:
              if (!pop(MODE_KEY)) {
                throw new IOException();
              }
              objStack.pop();
              value_state = 0;
              the_state = 9;
              break;

              // {
            case-8:
              if (!push(MODE_KEY)) {
                throw new IOException();
              }

              if (the_top == 1) {
                type = OBJECT;
                rootObject = new Hashtable();
                objStack.push(rootObject);
              }

              if (the_top > 1) {
                if (stack[the_top - 1] == MODE_OBJECT) {
                  Hashtable h = (Hashtable) objStack.lastElement();
                  Hashtable newHash = new Hashtable();
                  h.put(key, newHash);
                  objStack.push(newHash);
                  key = null;
                } else if (stack[the_top - 1] == MODE_ARRAY) {
                  Vector v = (Vector) objStack.lastElement();
                  Hashtable newHash = new Hashtable();
                  v.addElement(newHash);
                  objStack.push(newHash);
                }
              }

              clearBuf();
              value_state = 0;
              the_state = 1;
              break;

              // }
            case-7:
              if (!pop(MODE_OBJECT)) {
                throw new IOException();
              }

              if (objStack.size() > 0) {
                Hashtable h = (Hashtable) objStack.pop();
                //System.out.println("key: " + key + " value_state: " + value_state);
                if (value_state != 0) {
                  h.put(key, makeValue(sb.toString(), value_state));
                }
              }
              
              clearBuf();
              value_state = 0;
              the_state = 9;
              break;

              // [
            case-6:
              if (!push(MODE_ARRAY)) {
                throw new IOException();
              }

              if (the_top == 1) {
                type = ARRAY;
                rootObject = new Vector();
                objStack.push(rootObject);
              }

              
              if (the_top > 1) {
                if (stack[the_top - 1] == MODE_OBJECT) {
                  Hashtable h = (Hashtable) objStack.lastElement();
                  Vector newVector = new Vector();
                  h.put(key, newVector);
                  objStack.push(newVector);
                  key = null;
                } else if (stack[the_top - 1] == MODE_ARRAY) {
                  Vector v = (Vector) objStack.lastElement();
                  Vector newVector = new Vector();
                  v.addElement(newVector);
                  objStack.push(newVector);
                }
              }

              this.clearBuf();
              value_state = 0;
              the_state = 2;
              break;

              // ]
            case-5:
              if (objStack.size() > 0) {
                Vector v = (Vector) objStack.pop();
                if (value_state != 0) {
                  v.addElement(makeValue(sb.toString(), value_state));
                }
              }

              if (!pop(MODE_ARRAY)) {
                throw new IOException();
              }

              clearBuf();
              value_state = 0;
              the_state = 9;
              break;

              // "
            case-4:
              switch (stack[the_top]) {
                case MODE_KEY:
                  key = sb.toString();
                  sb.setLength(0);
                  the_state = 27;
                  break;
                case MODE_ARRAY:
                case MODE_OBJECT:
                  the_state = 9;
                  break;
                default:
                  return null; 
              }
              break;

              // ,
            case-3:
              switch (stack[the_top]) {
                case MODE_OBJECT:
                  if (pop(MODE_OBJECT) && push(MODE_KEY)) {
                    if (value_state != 0) {
                      Hashtable h = (Hashtable) objStack.lastElement();
                      //System.out.println("add: " + value_state);
                      h.put(key, makeValue(sb.toString(), value_state));
                    }
                    key = null;
                    the_state = 29;
                  }
                  break;
                case MODE_ARRAY:
                  if (value_state != 0) {
                    Vector v = (Vector) objStack.lastElement();
                    //System.out.println("add: " + value_state);
                    v.addElement(makeValue(sb.toString(), value_state));
                  }
                  the_state = 28;
                  break;
                default:
                  throw new IOException("Comma appears at illegal location.");
              }
              clearBuf();
              value_state = 0;
              break;

              // :
            case-2:
              if (pop(MODE_KEY) && push(MODE_OBJECT)) {
                the_state = 28;
                value_state = 0;
                break;
              }

              // syntax error
            case-1:
              throw new IOException("Syntax error.");

          }

        } else { // state >= 0, deal with data types
          //System.out.println("      state: " + the_state + " " + " s: " + s + " char: " + (char) b + " mode: " + stack[the_top]);
          vs = value_state_transition_table[value_state][c];
          //System.out.println("value state: " + value_state + " " + "vs: " + vs);
          if (vs < 0) {
            switch (vs) {
              case -9:
                sb.append((char) b);
                break;
              case -8:
                switch (b) {
                  case '"':
                    sb.append("\"");
                    break;
                  case '\\':
                    sb.append("\\");
                    break;
                  case '/':
                    sb.append("/");
                    break;
                  case 'b':
                    sb.append("\b");
                    break;
                  case 'f':
                    sb.append("\f");
                    break;
                  case 'n':
                    sb.append("\n");
                    break;
                  case 'r':
                    sb.append("\r");
                    break;
                  case 't':
                    sb.append("\t");
                    break;
                }
                value_state = 1; // back to string
                break;
              case -7:
                utf += Character.digit((char)b, 16) << (4 * (11 - value_state));
                if (value_state < 11) {// less than UNICODE#4
                  value_state += 1;
                } else {
                  sb.append((char) utf);
                  utf = 0;
                  value_state = 1;
                }
                break;
              case -5:
                sb.append((char) b);
                if (b == '.') {
                  value_state = 4;
                } else if (b == 'E' || b == 'e') {
                  value_state += 2;
                }

                if (value_state < 3) {
                  value_state = 3;
                }
                break;
              case -1:
                throw new IOException();
            }
          } else {
             value_state = vs;
          }

          the_state = s;
        }

      }
    }

    if(!(the_state == 9 && pop(MODE_DONE))) {
      throw new IOException("Syntax error.");      
    }

    return rootObject;
  }
}
