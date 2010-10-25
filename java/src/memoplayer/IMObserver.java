//#condition api.im
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

import java.util.Vector;

import javax.microedition.io.ConnectionNotFoundException;

import imps.message.handler.Contact;
import imps.message.handler.IMPSHandler;
import imps.message.handler.IMPSResponse;
import imps.message.handler.LoginResponse;
import imps.message.handler.Message;
import imps.message.handler.Result;
import imps.message.processing.Observable;
import imps.message.processing.Observer;

public class IMObserver implements Observer {

    private final String IM_URL = "IM_SIGNAL";

    /**
     * called when a gateway response is received
     */
    public void update(Observable arg0, Object arg1) {
        try {
            IMPSResponse impsResponse = (IMPSResponse) arg1;
            Vector res = new Vector();
            Vector tempContact = new Vector();
            switch (impsResponse.getResponseType()) {
            case IMPSHandler.CONTACTS:
                Logger.println("IMObserver update IMPSHandler.CONTACTS");
                LoginResponse loginResponseContact = (LoginResponse) impsResponse.getResponseObject();
                res.addElement("CONTACTS");
                res.addElement(loginResponseContact.getAlias());
                addContactsToResponse(res,loginResponseContact.getContactList());
                buildResponse(res);
                break;
            case IMPSHandler.LOGIN:
                Logger.println("IMObserver update IMPSHandler.LOGIN");
                LoginResponse loginResponseLogin = (LoginResponse) impsResponse.getResponseObject();
                res.addElement("LOGIN");
                addContactsPresenceToResponse(res,loginResponseLogin.getContactList());
                buildResponse(res);
                break;
            case IMPSHandler.LOGOUT:
                Logger.println("IMObserver update IMPSHandler.LOGOUT");
                res.addElement("LOGOUT");
                buildResponse(res);
                break;
            case IMPSHandler.DISCONNECT:
                Logger.println("IMObserver update IMPSHandler.DISCONNECT");
                res.addElement("DISCONNECT");
                buildResponse(res);
                break;
            case IMPSHandler.NOTIF_PRESENCE:
                Logger.println("IMObserver update IMPSHandler.NOTIF_PRESENCE");
                Vector newContactList = (Vector) impsResponse.getResponseObject();
                res.addElement("NOTIF_PRESENCE");
                Contact contact;
                for (int i = 0 ; i < newContactList.size() ; i++) {
                    contact = (Contact) newContactList.elementAt(i);
                    tempContact.addElement(contact.getUserID());
                    tempContact.addElement(contact.getAlias());
                    tempContact.addElement(contact.getPresence());                    
                    if (contact.getPresenceStatus() != -1) tempContact.addElement(String.valueOf(contact.getPresenceStatus()));
                    else tempContact.addElement("");
                    tempContact.addElement(String.valueOf(contact.isBlocked()));                    
                    tempContact.addElement(contact.getType());
                    tempContact.addElement(getCorrectGroup(contact.getGroup()));
                    tempContact.addElement(String.valueOf(false));                    
                    res.addElement(tempContact);
                    tempContact = new Vector();
                }
                buildResponse(res);
                break;
            case IMPSHandler.UPDATE_ALIAS:
                Logger.println("IMObserver update IMPSHandler.UPDATE_ALIAS");
                res.addElement("UPDATE_ALIAS");
                buildResponse(res);
                break;
            case IMPSHandler.UPDATE_PRESENCE:
                Logger.println("IMObserver update IMPSHandler.UPDATE_PRESENCE");
                res.addElement("UPDATE_PRESENCE");
                buildResponse(res);
                break;
            case IMPSHandler.NEW_MESSAGE:
                Logger.println("IMObserver update IMPSHandler.NEW_MESSAGE");
//#ifdef MM.pause
                MiniPlayer.wakeUpCanvas();
//#endif
                Message newMessage = (Message) impsResponse.getResponseObject();
                String sender = newMessage.getSender();
                String message = newMessage.getContent();
                res.addElement("NEW_MESSAGE");
                res.addElement(sender);
                res.addElement(message);
                buildResponse(res);
                break;
            case IMPSHandler.BLOCK_CONTACT:
                Logger.println("IMObserver update IMPSHandler.BLOCK_CONTACT");
                Contact blockedContact = (Contact) impsResponse.getResponseObject();
                res.addElement("BLOCK_CONTACT");
                res.addElement(blockedContact.getUserID());
                buildResponse(res);
                break;
            case IMPSHandler.UNBLOCK_CONTACT:
                Logger.println("IMObserver update IMPSHandler.UNBLOCK_CONTACT");
                Contact unblockedContact = (Contact) impsResponse.getResponseObject();
                res.addElement("UNBLOCK_CONTACT");
                res.addElement(unblockedContact.getUserID());
                buildResponse(res);
                break;
            case IMPSHandler.ADD_CONTACT:
                Logger.println("IMObserver update IMPSHandler.ADD_CONTACT");
                Contact addedContact = (Contact) impsResponse.getResponseObject();
                res.addElement("ADD_CONTACT");
                tempContact.addElement(addedContact.getUserID());
                tempContact.addElement(addedContact.getAlias());
                tempContact.addElement(addedContact.getPresence());
                tempContact.addElement(String.valueOf(addedContact.getPresenceStatus()));
                tempContact.addElement(String.valueOf(addedContact.isBlocked()));
                tempContact.addElement(addedContact.getType());
                tempContact.addElement(getCorrectGroup(addedContact.getGroup()));
                tempContact.addElement(String.valueOf(false));
                res.addElement(tempContact);
                buildResponse(res);
                break;
            case IMPSHandler.REMOVE_CONTACT:
                Logger.println("IMObserver update IMPSHandler.REMOVE_CONTACT");
                Contact removedContact = (Contact) impsResponse.getResponseObject();
                res.addElement("REMOVE_CONTACT");
                res.addElement(removedContact.getUserID());
                buildResponse(res);
                break;
            case IMPSHandler.CONTACT_INVITATION:
                Logger.println("IMObserver update IMPSHandler.CONTACT_INVITATION");
                Contact invitationContact = (Contact) impsResponse.getResponseObject();
                res.addElement("CONTACT_INVITATION");
                res.addElement(invitationContact.getUserID());
                buildResponse(res);
                break;
            case IMPSHandler.ERROR:
                Logger.println("IMObserver update IMPSHandler.ERROR");
                Result error = (Result) impsResponse.getResponseObject();
                res.addElement("ERROR");
                res.addElement(error.getCode());
                res.addElement(error.getDescription());
                buildResponse(res);
                break;
            default:
                break;
            }
        } catch (Exception e) {
            Logger.println("IMObserver update Exception "+e.getClass()+" "+e.getMessage());
            Vector res = new Vector();
            res.addElement("ERROR");
            res.addElement(new Integer(-1));
            res.addElement("IM_UNKNOWN_ERROR");
            buildResponse(res);
        }

    }

    /**
     * Method called to handle error from IMPS Stack
     */
    public void updateError(Observable arg0, Object arg1) {
        Exception e = (Exception) arg1;
        Logger.println("IMObserver updateError "+e.getClass()+" "+e.getMessage());
        Vector res = new Vector();
        res.addElement("ERROR");
        res.addElement(new Integer(-1));
        if (e.getClass().equals(ConnectionNotFoundException.class)) res.addElement("IM_UNKNOWN_ERROR_CONN_NOT_FOUND");
        else res.addElement("IM_UNKNOWN_ERROR");
        buildResponse(res);
    }

    private void addContactsToResponse(Vector res, Vector contactList) {
        if (contactList.size() > 0) {
            Vector list = new Vector();
            Vector tempContact;
            Vector tempExistingContact;
            int j = 0;
            
            for (int i = 0 ; i < contactList.size() ; i++) {
                Contact contact = (Contact) contactList.elementAt(i);
                
                for (j = 0; j < list.size(); j++) {
                    tempExistingContact = (Vector) list.elementAt(j);
                    if (contact.getUserID().equals(tempExistingContact.elementAt(0))) break;
                }
                
                if (j == list.size()) {                
                    tempContact = new Vector();
                    tempContact.addElement(contact.getUserID());
                    tempContact.addElement(contact.getAlias());
                    tempContact.addElement("F");
                    tempContact.addElement("6");
                    tempContact.addElement(String.valueOf(contact.isBlocked()));                
                    tempContact.addElement("OTHER");
                    tempContact.addElement(getCorrectGroup(contact.getGroup()));
                    tempContact.addElement(String.valueOf(false));
                    list.addElement(tempContact);
                }
            }
            res.addElement(list);
        }
    }
    
    private void addContactsPresenceToResponse(Vector res, Vector contactList) {
        Vector list = new Vector();
        if (contactList.size() > 0) {
            Vector tempContact;
            for (int i = 0 ; i < contactList.size() ; i++) {
                Contact contact = (Contact) contactList.elementAt(i);
                tempContact = new Vector();
                tempContact.addElement(contact.getUserID());                
                tempContact.addElement(contact.getPresence());
                tempContact.addElement(String.valueOf(contact.getPresenceStatus()));                                
                tempContact.addElement(contact.getType());
                list.addElement(tempContact);
            }
            res.addElement(list);
        }
    }

    private void buildResponse(Vector res) {
        memoplayer.Message.sendMessage(IM_URL, Integer.toString(JSArray.addArray(res)));
    }
    
    private String getCorrectGroup(String groupName) {
        String result = groupName;
        int t = result.indexOf("%2520");
        while (t != -1) {
            result = result.substring(0, t).concat(" ").concat(result.substring(t+5));
            t = result.indexOf("%2520");
        }
        return result;
    }

}
