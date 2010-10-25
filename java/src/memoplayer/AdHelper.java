//#condition api.ad
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

import javax.microedition.io.HttpConnection;

import ft.owl.adv.AdClient;
import ft.owl.adv.AdsRemoteResponse;
import ft.owl.adv.Pub;

public class AdHelper {
    
    static AdClient s_adClient = new AdClient();
    
    public static final String AD_URL = MiniPlayer.getJadProperty("AD_URL");
    
    public static final String AD_CLICK_URL = MiniPlayer.getJadProperty("AD_CLICK_URL");
    
    public static AdClient getAdClient() {
        return s_adClient;
    }
    
    public static byte[] getAd(String mail, String zoneId, String purge) {
        // delete all ads
        if (purge != null && purge.equals("true")) AdHelper.getAdClient().deleteAllAds();
        
        try {
            AdsRemoteResponse response = AdHelper.getAdClient().updateAds(AdHelper.AD_URL, mail, zoneId);
            if (response == null) {                     
                throw new Exception("No response or timeout from Ads Server");
            } else if (response.getResponseCode() != HttpConnection.HTTP_OK
                    && response.getResponseCode() != HttpConnection.HTTP_MOVED_TEMP) {
                throw new Exception("HTTP ERROR, code: " + response.getResponseCode());
            }
        } catch (Exception e) {
            Logger.println("File.open: update ads exception "+e.getMessage());
        }                    
        Pub pub = AdHelper.getAdClient().serveAd(mail, zoneId);
        byte[] data = null;
        if ((pub != null) && (pub.getPubImage() != null)) {
            data = pub.getPubImage().getContent();
        }
        
        return data;
    }

}
