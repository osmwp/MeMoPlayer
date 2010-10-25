//#condition api.traffic
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

final class Traffic {
    private final static String TRAFFIC_KEY = "TRAFFIC";
    private final static String TOTAL_KEY = "TOTAL";
    
    private static int s_sessionTraffic;
    private static int s_totalTraffic;
    
    public static synchronized void update(int length) {
        s_sessionTraffic += length;
    }
    
    public static synchronized int getSession() {
        return s_sessionTraffic;
    }
    
    public static synchronized int getTotal() {
        return s_totalTraffic + s_sessionTraffic;
    }
    
    public static synchronized void saveTotal() {
        if (s_sessionTraffic != 0) {
            CacheManager cm = CacheManager.createManager (TRAFFIC_KEY);
            cm.setRecord(TOTAL_KEY, String.valueOf(s_totalTraffic+s_sessionTraffic));
            cm.close();
            Logger.println("Session traffic: "+s_sessionTraffic);
            Logger.println("Total traffic: "+(s_totalTraffic+s_sessionTraffic));
        }
    }
    
    public static synchronized void loadTotal() {
        CacheManager cm = CacheManager.createManager (TRAFFIC_KEY);
        String s = cm.getRecord(TOTAL_KEY);
        if (s.length() != 0) {
            s_totalTraffic = Integer.parseInt(s);
        }
        cm.close();
    }
    
    public static synchronized void reset() {
        CacheManager cm = CacheManager.createManager (TRAFFIC_KEY);
        cm.setRecord(TOTAL_KEY, "0");
        cm.close();
        s_totalTraffic = 0;
        s_sessionTraffic = 0;
    }
}
