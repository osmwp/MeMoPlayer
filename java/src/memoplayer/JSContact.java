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

//#ifdef api.pim
import javax.microedition.pim.*;
import java.util.Enumeration;

//#endif

class JSContact {
    static int s_nbContacts = 0;
//#ifdef api.pim
    static JSContact[] s_contacts = null;
    static int [] s_index = null;

    String[] m_info = null;
    Contact m_contact = null;
    
    static PIMList s_PIMList = null;
//#endif
    final static String UNDEFINED = "undefined";

    final static int PIM_FULL_NAME = 1;
    final static int PIM_FIRST_NAME = 2;
    final static int PIM_LAST_NAME = 3;
    final static int PIM_ADDRESS = 4;
    final static int PIM_MOBILE = 5;
    final static int PIM_MOBILE_HOME = 6;
    final static int PIM_MOBILE_WORK = 7;
    final static int PIM_HOME = 8;
    final static int PIM_WORK = 9;
    final static int PIM_FAX = 10;
    final static int PIM_OTHER = 11;
    final static int PIM_PREFERRED = 12;
    final static int PIM_EMAIL = 13;
    final static int PIM_PHOTO_URL = 14;
    final static int PIM_LAST = 15;
    
    final static int ADD_FIELD = 1;
    final static int SET_FIELD = 2;

    
//    final static String[] s_label = { "FIRST", "FULL_NAME", "FIRST_NAME",
//            "LAST_NAME", "ADDRESS", "MOBILE", "MOBILE_HOME", "MOBILE_WORK",
//            "HOME", "WORK", "FAX", "OTHER", "PREFERRED ", "EMAIL ", "LAST " };

    static int openContacts() {
//#ifdef api.pim
        s_contacts = new JSContact[s_nbContacts + 64];
        s_nbContacts = 0;
        try {
            s_PIMList = PIM.getInstance().openPIMList(PIM.CONTACT_LIST,
                    PIM.READ_WRITE);
            Enumeration itemEnum = s_PIMList.items();
            while (itemEnum.hasMoreElements()) {
                try {
                    addContact(s_PIMList, (Contact) itemEnum.nextElement());
                } catch (Exception e) {
                    Logger.println("Exception while opening contacts: " + e);
                }
            }
        } catch (Exception e) {
            Logger.println("Exception while opening list: " + e);
        }
//#endif
        return s_nbContacts;
    }

    static void closeContacts() {
//#ifdef api.pim
        Logger.println("DEBUG : closeContacts IN");
        for (int i = 0; i < s_nbContacts; i++) {
            s_contacts[i] = null;
        }
        Logger.println("DEBUG : closeContacts init null for");
        s_contacts = null;
        Logger.println("DEBUG : closeContacts s_contacts null. Now try s_PIMList.close()");
        try {
            s_PIMList.close();
        } catch (Exception e) {
            Logger.println("Exception while closing list: " + e);
        }
        Logger.println("DEBUG : closeContacts OUT");
//#endif
    }

//#ifdef api.pim
    static private void addContact(PIMList list, Contact c) {
        s_nbContacts++;
        if (s_contacts.length < s_nbContacts) {
            JSContact[] tmp = new JSContact[s_nbContacts + 64];
            System.arraycopy(s_contacts, 0, tmp, 0, s_nbContacts - 1);
            s_contacts = tmp;
        }
        s_contacts[s_nbContacts - 1] = new JSContact(list, c);
    }

//#endif

    static String getContactInfo(int id, int info) {
//#ifdef api.pim
        if (id >= 0 && id < s_nbContacts && info > 0 && info <= PIM_LAST) {
            return s_contacts[id].m_info[info];
        }
//#endif
        return ("");
    }

    // Create a new contact and returns a contactId used to add new fields by
    // using setInfo.
    static int createContact() {
//#ifdef api.pim
          Contact contact = ((ContactList)s_PIMList).createContact();

          String[] name = new String[s_PIMList.stringArraySize(Contact.NAME)];
          name[Contact.NAME_GIVEN] = "";
          name[Contact.NAME_FAMILY] = "";

          Logger.println("DEBUG createContact : init");
          String[] addr = new String[s_PIMList.stringArraySize(Contact.ADDR)];
          addr[Contact.ADDR_COUNTRY] = "";
          addr[Contact.ADDR_LOCALITY] = "";
          addr[Contact.ADDR_POSTALCODE] = "";
          addr[Contact.ADDR_STREET] = "";
          if (s_PIMList.isSupportedField(Contact.FORMATTED_NAME))
                  contact.addString(Contact.FORMATTED_NAME, PIMItem.ATTR_NONE, "");
          else Logger.println("DEBUG createContact not supported : FORMATTED_NAME");
             if (s_PIMList.isSupportedArrayElement(Contact.NAME, Contact.NAME_FAMILY))
                  name[Contact.NAME_FAMILY] = "";
              else Logger.println("DEBUG createContact not supported : NAME_FAMILY");
             if (s_PIMList.isSupportedArrayElement(Contact.NAME, Contact.NAME_GIVEN))
                  name[Contact.NAME_GIVEN] = "";
              else Logger.println("DEBUG createContact not supported : NAME_GIVEN");
             contact.addStringArray(Contact.NAME, PIMItem.ATTR_NONE, name);
             if (s_PIMList.isSupportedArrayElement(Contact.ADDR, Contact.ADDR_COUNTRY))
                  addr[Contact.ADDR_COUNTRY] = "";
              else Logger.println("DEBUG createContact not supported : ADDR_COUNTRY");
             if (s_PIMList.isSupportedArrayElement(Contact.ADDR, Contact.ADDR_LOCALITY))
                  addr[Contact.ADDR_LOCALITY] = "";
              else Logger.println("DEBUG createContact not supported : ADDR_LOCALITY");
             if (s_PIMList.isSupportedArrayElement(Contact.ADDR, Contact.ADDR_POSTALCODE))
                  addr[Contact.ADDR_POSTALCODE] = "";
              else Logger.println("DEBUG createContact not supported : ADDR_POSTALCODE");
             if (s_PIMList.isSupportedArrayElement(Contact.ADDR, Contact.ADDR_STREET))
                  addr[Contact.ADDR_STREET] = "";
              else Logger.println("DEBUG createContact not supported : ADDR_STREET");
             if (s_PIMList.isSupportedField(Contact.ADDR))
                    contact.addStringArray(Contact.ADDR, Contact.ATTR_HOME, addr);
              else Logger.println("DEBUG createContact not supported : ADDR ATTR_HOME");
             if (s_PIMList.isSupportedField(Contact.ADDR))
                    contact.addStringArray(Contact.ADDR, Contact.ATTR_WORK, addr);
              else Logger.println("DEBUG createContact not supported : ADDR ATTR_WORK");
             if (s_PIMList.isSupportedField(Contact.FORMATTED_ADDR))
                    contact.addString(Contact.FORMATTED_ADDR, Contact.ATTR_HOME, "");    
              else Logger.println("DEBUG createContact not supported : FORMATTED_ADDR ATTR_HOME");    
             if (s_PIMList.isSupportedField(Contact.FORMATTED_ADDR))
                    contact.addString(Contact.FORMATTED_ADDR, Contact.ATTR_WORK, "");
              else Logger.println("DEBUG createContact not supported : FORMATTED_ADDR ATTR_WORK");
//             if (s_PIMList.isSupportedField(Contact.TEL))
//                    contact.addString(Contact.TEL, Contact.ATTR_HOME, "");
             if (s_PIMList.isSupportedAttribute(Contact.TEL,Contact.ATTR_HOME))
                    contact.addString(Contact.TEL, Contact.ATTR_HOME, "");    
              else Logger.println("DEBUG createContact not supported : TEL ATTR_HOME");
             if (s_PIMList.isSupportedAttribute(Contact.TEL,Contact.ATTR_WORK))
                    contact.addString(Contact.TEL, Contact.ATTR_WORK, "");    
              else Logger.println("DEBUG createContact not supported : TEL ATTR_WORK");
             if (s_PIMList.isSupportedAttribute(Contact.TEL,Contact.ATTR_FAX))
                    contact.addString(Contact.TEL, Contact.ATTR_FAX, "");    
              else Logger.println("DEBUG createContact not supported : TEL ATTR_FAX");
             if (s_PIMList.isSupportedAttribute(Contact.TEL,Contact.ATTR_MOBILE | Contact.ATTR_HOME))
                    contact.addString(Contact.TEL, Contact.ATTR_MOBILE | Contact.ATTR_HOME, "");
              else Logger.println("DEBUG createContact not supported : TEL ATTR_MOBILE ATTR_HOME");    
             if (s_PIMList.isSupportedAttribute(Contact.TEL,Contact.ATTR_MOBILE | Contact.ATTR_WORK))
                    contact.addString(Contact.TEL, Contact.ATTR_MOBILE | Contact.ATTR_WORK, "");
              else Logger.println("DEBUG createContact not supported : TEL ATTR_MOBILE ATTR_WORK");    
             if (s_PIMList.isSupportedField(Contact.EMAIL)) 
                    contact.addString(Contact.EMAIL, PIMItem.ATTR_NONE, "");
              else Logger.println("DEBUG createContact not supported : EMAIL");
             if (s_PIMList.isSupportedField(Contact.PHOTO_URL)) 
                    contact.addString(Contact.PHOTO_URL, PIMItem.ATTR_NONE, "");
              else Logger.println("DEBUG createContact not supported : PHOTO_URL");
//              Logger.println("DEBUG createContact : NOTE");
             if (s_PIMList.isSupportedField(Contact.NOTE)) 
                    contact.addString(Contact.NOTE, PIMItem.ATTR_NONE, "");
              else Logger.println("DEBUG createContact not supported : NOTE");
             

          try {
//             contact.addString(Contact.FORMATTED_NAME, PIMItem.ATTR_NONE, "");
//             contact.addStringArray(Contact.NAME, PIMItem.ATTR_NONE, name);
//             contact.addStringArray(Contact.ADDR, Contact.ATTR_HOME, addr);
//             contact.addStringArray(Contact.ADDR, Contact.ATTR_WORK, addr);
//             contact.addString(Contact.FORMATTED_ADDR, Contact.ATTR_WORK, "");
//             contact.addString(Contact.FORMATTED_ADDR, Contact.ATTR_HOME, "");
//             contact.addString(Contact.TEL, Contact.ATTR_HOME, "");
//             contact.addString(Contact.TEL, Contact.ATTR_WORK, "");
//             contact.addString(Contact.TEL, Contact.ATTR_FAX, "");
//             contact.addString(Contact.TEL, Contact.ATTR_MOBILE | Contact.ATTR_HOME, "");
//             contact.addString(Contact.TEL, Contact.ATTR_MOBILE | Contact.ATTR_WORK, "");
//             contact.addString(Contact.EMAIL, PIMItem.ATTR_NONE, "");
//             contact.addString(Contact.NOTE, PIMItem.ATTR_NONE, "");
//             contact.addString(Contact.PHOTO_URL, PIMItem.ATTR_NONE, "");
//             contact.addToCategory("");  // todo : catcher exception pour passer outre en cas d'exception ca dépend des plateformes supportés pour "" vide.
//             
//             String [] cat = contact.getCategories();
//             for (int i=0; i< cat.length; i++) {
//                 Logger.println("DEBUG categories "+i+"-"+cat[i]);
//             }
//             
             contact.commit();
//              
//          } catch (UnsupportedFieldException e) {
//            // In this case, we choose not to save the contact at all if any of the
//            // fields are not supported on this platform.
//            Logger.println("createContact() tests Contact not saved :"+e);
//            return 0;
          } catch (javax.microedition.pim.PIMException e) {
              Logger.println("createContacttests PIMException");
              return 0;
          }

          addContact((ContactList)s_PIMList, contact);
//#endif
        return s_nbContacts;
    }

    // Delete a contact given by contactId (between 0 and max contacts). Returns
    // 1 if success otherwish returns 0. 
    static int deleteContact(int contactId) {
//#ifdef api.pim
        if (contactId >= 0 && contactId < s_contacts.length) {
            try {                
                ContactList list = (ContactList) s_contacts[contactId].m_contact.getPIMList();            
                list.removeContact((s_contacts[contactId].m_contact));
                s_contacts[contactId] = null;
                s_nbContacts--;
                for (int i = contactId; i < s_nbContacts; i++) {
                    s_contacts[i] = s_contacts[i + 1];
                }
                return 1;
            } catch (Exception e) {
                Logger.println("Exception while deleting contact: " + e);
            }            
        }
//#endif        
        return 0;
    }

    // Modify info_string in a contact given by contactId (between 0 and
    // max contacts). The info_constant can be one of the constants.
    static void setInfo(int contactId, int info_constant, String info_string) {
//#ifdef api.pim
        if (contactId >= 0 && contactId < s_contacts.length && info_constant > 0 && info_constant <= PIM_LAST) {            
            s_contacts[contactId].m_info[info_constant]=new String(info_string);
             Logger.println("DEBUG : setInfo (id:"+contactId+", info:"+info_constant+", string:"+info_string+")");
            realizeField(s_contacts[contactId].m_contact, info_constant, info_string, SET_FIELD);        
        }
//#endif    
    }
     // Modify info_string in a contact given by contactId (between 0 and
     // max contacts). The info_constant can be one of the constants.
     static void addInfo(int contactId, int info_constant, String info_string) {
//#ifdef api.pim
         if (contactId >= 0 && contactId < s_nbContacts && info_constant > 0 && info_constant <= PIM_LAST) {            
             s_contacts[contactId].m_info[info_constant]=new String(info_string);
             Logger.println("DEBUG : addInfo (id:"+contactId+", info:"+info_constant+", string:"+info_string+")");
             realizeField(s_contacts[contactId].m_contact, info_constant, info_string, ADD_FIELD);        
         }
//#endif         
     }   
//#ifdef api.pim     
     static private void realizeStringField(Contact contact, int field, int index, int attributes, java.lang.String value, int action) throws PIMException {
        if (s_PIMList.isSupportedAttribute(field, attributes)) {
            Logger.println("DEBUG : realizeStringField "+field+" and "+attributes+" supported");
            inputStringField(contact, field, index, attributes, value, action);
        }
        else
            Logger.println("DEBUG : realizeStringField "+field+" and "+attributes+ "not supported");         
     }
     static private int realizeStringField(Contact contact, int field, int index, java.lang.String value, int action) throws PIMException{
        if (s_PIMList.isSupportedField(field)) {
            Logger.println("DEBUG : realizeStringField "+field+" supported");                        
            inputStringField(contact, field, index, PIMItem.ATTR_NONE, value, action);
            return 1;
        }
        else
            {
            Logger.println("DEBUG : realizeStringField "+field+" not supported");         
            return -1;
            }
     }     
     static private void inputStringField(Contact contact, int field, int index, int attributes, java.lang.String value, int action) throws PIMException {
        if(action == ADD_FIELD) {
            contact.addString(field, attributes, value);
        } else {
            contact.setString(field, index, attributes, value);
        }                            
        contact.commit();
    }     
     static private void realizeStringArrayField(Contact contact, int field, int indexField, int index, int attributes, java.lang.String value, int action) throws PIMException{
        if (s_PIMList.isSupportedArrayElement(field, indexField)) {
            Logger.println("DEBUG : realizeStringArrayField "+field+" and "+attributes+" supported");
            String[] values = new String[s_PIMList.stringArraySize(field)];    
            for (int i=0; i<values.length; i++) {
                values[i] = "";
            }
            values[indexField] = value;            
            if(action == ADD_FIELD) {
                contact.addStringArray(field, attributes, values);
            } else {
                contact.setStringArray(field, index, attributes, values);
            }                            
            contact.commit();
        }
        else
            Logger.println("DEBUG : realizeStringArrayField "+field+" and "+attributes+ "not supported");         
     }     
    static private void realizeField(Contact contact, int info_constant, String info_string, int action) {
        Logger.println("DEBUG : addField " + info_string);
        try {
            switch (info_constant) {        
                case PIM_FULL_NAME: 
                    realizeStringField(contact, Contact.FORMATTED_NAME, 0, info_string, action);
                    break;               
                case PIM_FIRST_NAME: 
                    realizeStringArrayField(contact, Contact.NAME, Contact.NAME_GIVEN, 0, PIMItem.ATTR_NONE, info_string, action);
                    break;   
                case PIM_LAST_NAME: 
                    realizeStringArrayField(contact, Contact.NAME, Contact.NAME_FAMILY, 0, PIMItem.ATTR_NONE, info_string, action);
                    break; 
                case PIM_ADDRESS:
                    int state = realizeStringField(contact, Contact.FORMATTED_ADDR, 0, info_string, action);
                    if(state == -1) {
                        realizeStringField(contact, Contact.ADDR, 0, info_string, action);
                    }
                    break;
                case PIM_MOBILE:   
                    realizeStringField(contact, Contact.TEL, 0, Contact.ATTR_MOBILE, info_string, action);
                    break;
                case PIM_MOBILE_HOME:     
                    realizeStringField(contact, Contact.TEL, 0, Contact.ATTR_MOBILE & Contact.ATTR_HOME, info_string, action);
                    break;
                case PIM_MOBILE_WORK:       
                    realizeStringField(contact, Contact.TEL, 0, Contact.ATTR_MOBILE & Contact.ATTR_WORK, info_string, action);
                    break;
                case PIM_HOME:       
                    realizeStringField(contact, Contact.TEL, 0, Contact.ATTR_HOME, info_string, action);
                    break; 
                case PIM_WORK:   
                    realizeStringField(contact, Contact.TEL, 0, Contact.ATTR_WORK, info_string, action);
                    break;
                case PIM_FAX: 
                    realizeStringField(contact, Contact.TEL, 0, Contact.ATTR_FAX, info_string, action);
                    break;
                case PIM_OTHER:
                    realizeStringField(contact, Contact.NOTE, 0, info_string, action);
                    break;
                case PIM_PREFERRED: 
                    // TODO : return Contact.ATTR_PREFERRED; NOT YET IMPLEMENTED
                    break;
                case PIM_EMAIL: 
                    realizeStringField(contact, Contact.EMAIL, 0, info_string, action);
                    break;
                case PIM_PHOTO_URL: 
                    realizeStringField(contact, Contact.PHOTO_URL, 0, info_string, action);
                    break;                    
                case PIM_LAST: 
                default:
                        break;
                }
          } catch (PIMException e) {
              // An error occured
            Logger.println("DEBUG exception realizeField : "+e);
          }    
    }     

    // as Contact.getString returns an exception is teh field is not set,
    // provide a secure one
    private String getString(Contact c, int field, int attr) {
        try {
            return c.getString(field, attr);
        } catch (Exception e) {
        }
        return "";
    }

    private String getMultiString(Contact c, int field, int attr) {
        try {
            String result = "";
            String[] array = c.getStringArray(field, attr);
            for (int j = 0; j < array.length; j++) {
                if (array[j] != null) {
                    result += array[j] + " ";
                }
            }
            return result;
        } catch (Exception e) {
        }
        return "";
    }

    private String getFieldValue(PIMList list, Contact c, int formattedName, int name) {
        if (list.isSupportedField(formattedName)) {
            return getString(c, formattedName, Contact.ATTR_NONE);
        } else if (list.isSupportedField(name)) {
            return getMultiString(c, name, Contact.ATTR_NONE);
        }
        return "";
    }

    private void getName(PIMList list, Contact c) {
        if (list.isSupportedField(Contact.NAME)) {

            if (c.countValues(Contact.NAME) != 0) {
            String[] array = c.getStringArray(Contact.NAME, Contact.ATTR_NONE);
            if (list.isSupportedArrayElement(Contact.NAME, Contact.NAME_FAMILY)) {
                if (array[Contact.NAME_FAMILY] != null) {
                    m_info[PIM_LAST_NAME] = array[Contact.NAME_FAMILY];
                }
            }
            if (list.isSupportedArrayElement(Contact.NAME, Contact.NAME_GIVEN)) {
                if (array[Contact.NAME_GIVEN] != null) {
                    m_info[PIM_FIRST_NAME] = array[Contact.NAME_GIVEN];
                }
            }
            }
        }
        if (list.isSupportedField(Contact.FORMATTED_NAME)) {
            m_info[PIM_FULL_NAME] = getString(c, Contact.FORMATTED_NAME,
                    Contact.ATTR_NONE);
        } else {
            m_info[PIM_FULL_NAME] = m_info[PIM_LAST_NAME] + " "
                    + m_info[PIM_FIRST_NAME];
        }
    }

    private void getAddress(PIMList list, Contact c) {
        if (list.isSupportedField(Contact.FORMATTED_ADDR)) {
            m_info[PIM_ADDRESS] = getString(c, Contact.FORMATTED_ADDR,
                    Contact.ATTR_NONE);
        } else if (list.isSupportedField(Contact.ADDR)) {
            m_info[PIM_ADDRESS] = getMultiString(c, Contact.ADDR,
                    Contact.ATTR_NONE);
        }
    }

    private boolean hasAttribute(int a, int m) {
        return (a & m) == m;
    }

    
//     void printAttr (Contact c) { 
//         int n = c.countValues (Contact.TEL);
//     Logger.println ("phone numbers count: "+n); for (int i = 0; i < n; i++) {
//     int a = c.getAttributes (Contact.TEL, i); Logger.print ("-> "+ getString
//     (c, Contact.TEL, i)+"/"+a); if (hasAttribute (a, Contact.ATTR_MOBILE)) {
//     Logger.print (" MOBILE"); } if (hasAttribute (a, Contact.ATTR_HOME)) {
//     Logger.print (" HOME"); } if (hasAttribute (a, Contact.ATTR_WORK)) {
//     Logger.print (" WORK"); } if (hasAttribute (a, Contact.ATTR_FAX)) {
//     Logger.print (" FAX"); } if (hasAttribute (a, Contact.ATTR_OTHER)) {
//     Logger.print (" OTHER"); } if (hasAttribute (a, Contact.ATTR_PREFERRED))
//     { Logger.print (" PREFERRED"); } Logger.println (""); } 
//     }
    

    private void getPhoneNumbers(PIMList list, Contact c) {
        // printAttr (c);
        int nbValues = c.countValues(Contact.TEL);
        for (int i = 0; i < nbValues; i++) {
            String s = getString(c, Contact.TEL, i);
            int a = c.getAttributes(Contact.TEL, i);
            if (hasAttribute(a, Contact.ATTR_MOBILE)) {
                if (hasAttribute(a, Contact.ATTR_HOME)) {
                    m_info[PIM_MOBILE_HOME] = s;
                } else if (hasAttribute(a, Contact.ATTR_WORK)) {
                    m_info[PIM_MOBILE_WORK] = s;
                } else {
                    m_info[PIM_MOBILE] = s;
                }
            } else if (hasAttribute(a, Contact.ATTR_HOME)) {
                m_info[PIM_HOME] = s;
            } else if (hasAttribute(a, Contact.ATTR_WORK)) {
                m_info[PIM_WORK] = s;
            } else if (hasAttribute(a, Contact.ATTR_FAX)) {
                m_info[PIM_FAX] = s;
            } else {
                m_info[PIM_OTHER] = s;
            }
            if (hasAttribute(a, Contact.ATTR_PREFERRED)) {
                m_info[PIM_PREFERRED] = s;
            }
        }
    }

    private void getEmail(PIMList list, Contact c) {
        if (list.isSupportedField(Contact.EMAIL)) {
            m_info[PIM_EMAIL] = getString(c, Contact.EMAIL, Contact.ATTR_NONE);
        }
    }
    
    private void getPhoto(PIMList list, Contact c) {
        if (list.isSupportedField(Contact.EMAIL)) {
            m_info[PIM_PHOTO_URL] = getString(c, Contact.PHOTO_URL, Contact.ATTR_NONE);
        }
    }
        
//#endif

//#ifdef api.pim
    JSContact(PIMList list, Contact c) {
        m_info = new String[PIM_LAST];
        for (int i = 0; i < PIM_LAST; i++) {
            m_info[i] = "";
        }

        m_contact = c;
        
        //print(m_contact);
        
        //Logger.println("getName");
        getName(list, c);
        //Logger.println("getAddress");
        getAddress(list, c);
        //Logger.println("getPhoneNumbers");
        getPhoneNumbers(list, c);
        //Logger.println("getEmail");
        getEmail(list, c);
        getPhoto(list, c);
        //Logger.println ("---- New Contact ----");
        // for (int i = 1; i < PIM_LAST; i++) {
        // Logger.println ("    "+s_label[i]+" = "+m_info [i]);
        // }
    }
//#endif
    
    
// for tests
//    static void tests() {
//         ContactList contacts = null;
//          try {
//            contacts = (ContactList) PIM.getInstance().openPIMList(PIM.CONTACT_LIST, PIM.READ_WRITE);
//          } catch (PIMException e) {
//              // An error occurred
//              return;
//          }
//          Contact contact = contacts.createContact();
//
//          String[] name = new String[contacts.stringArraySize(Contact.NAME)];
//          name[Contact.NAME_GIVEN] = "John";
//          name[Contact.NAME_FAMILY] = "Public";
//
//          String[] addr = new String[contacts.stringArraySize(Contact.ADDR)];
//          addr[Contact.ADDR_COUNTRY] = "USA";
//          addr[Contact.ADDR_LOCALITY] = "Coolsville";
//          addr[Contact.ADDR_POSTALCODE] = "91921-1234";
//          addr[Contact.ADDR_STREET] = "123 Main Street";
//
//          try {
//             contact.addString(Contact.FORMATTED_NAME, PIMItem.ATTR_NONE, "Mr. John Q. Public, Esq.");
//             contact.addStringArray(Contact.NAME, PIMItem.ATTR_NONE, name);
//             contact.addStringArray(Contact.ADDR, Contact.ATTR_HOME, addr);
//             contact.addString(Contact.TEL, Contact.ATTR_HOME, "613-123-4567");
//             contact.addString(Contact.TEL, Contact.ATTR_WORK, "123-123-1234");
//             contact.addString(Contact.TEL, Contact.ATTR_FAX, "613-123-4567");
//             contact.addString(Contact.TEL, Contact.ATTR_MOBILE | Contact.ATTR_HOME, "613-123-4567");
//             contact.addString(Contact.TEL, Contact.ATTR_MOBILE | Contact.ATTR_WORK, "613-123-4567");
//             contact.addToCategory("Friends");
//             contact.addString(Contact.EMAIL, Contact.ATTR_HOME | Contact.ATTR_PREFERRED, "jqpublic@xyz.dom1.com");
//
//          } catch (UnsupportedFieldException e) {
//            // In this case, we choose not to save the contact at all if any of the
//            // fields are not supported on this platform.
//            Logger.println("tests Contact not saved");
//            return;
//          } catch (javax.microedition.pim.PIMException e) {
//              Logger.println(" tests PIMException");
//              return;
//          }
//
//          try {
//              contact.commit();
//          } catch (PIMException e) {
//              // An error occured
//          }
//          try {
//              contacts.close();
//          } catch (PIMException e) {
//          }
//
//    }
 
}
