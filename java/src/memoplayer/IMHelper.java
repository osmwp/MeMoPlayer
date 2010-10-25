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

import imps.message.handler.IMPSHandler;
import imps.message.processing.Observer;


public class IMHelper {

    private IMPSHandler impsHandler = null;

    public boolean login(String email, String pwd, String clientID, int initialStatus, Observer obs) {
        
        String GW_URL = MiniPlayer.getJadProperty("GW_URL");
        String MSISDN = MiniPlayer.getJadProperty("FAKE_MSISDN");
        impsHandler = new IMPSHandler(GW_URL,MSISDN,"USER_AGENT",30000);
        impsHandler.addObserver(obs);
        impsHandler.notifyObservers();

        // login request
        try {
            impsHandler.login(email, pwd, clientID, initialStatus);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error :" + e + " could not login");
            return false;
        }
        return true;
    }

    public boolean logout() {
        try {
            impsHandler.logout();
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.err.println("Error :" + e + " could not logout");
            return false;
        }
        return true;
    }

    public boolean updateAlias(String alias) {
        try {
            impsHandler.updateAlias(alias);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error :" + e + " alias could not be updated");
            return false;
        }
        return true;
    }

    public boolean updatePresence(int status) {
        try {
            impsHandler.updatePresence(status);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error :" + e + " presence could not be updated");
            return false;
        }
        return true;
    }

    public boolean sendMessage(String contact, String msg) {
        try {
            impsHandler.sendMessage(contact,msg);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error :" + e + " message could not be sent");
            return false;
        }
        return true;
    }

    public boolean addContact(String contact) {
        try {
            impsHandler.addContact(contact);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error :" + e + " contact could not be added");
            return false;
        }
        return true;
    }

    public boolean removeContact(String contact) {
        try {
            impsHandler.removeContact(contact);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error :" + e + " contact could not be removed");
            return false;
        }
        return true;
    }

    public boolean blockContact(String contact) {
        try {
            impsHandler.blockContact(contact);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error :" + e + " contact could not be blocked");
            return false;
        }
        return true;
    }

    public boolean unblockContact(String contact) {
        try {
            impsHandler.unblockContact(contact);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error :" + e + " contact could not be unblocked");
            return false;
        }
        return true;
    }

    public boolean respondInvitation(String userID, boolean accept) {
        try {
            impsHandler.respondInvitation(userID, accept);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error :" + e + " response to invitation could not be sent");
            return false;
        }
        return true;
    }
}
