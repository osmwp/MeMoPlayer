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

import java.io.IOException;

/**
 * Not so smart static methods to handle cookies in HTTP headers.
 */
public class SmartHttpCookies {
    private final static int COOKIES_TIMEOUT = 10*60; // 10min timeout
    
    private static ObjLink s_cookies;
    
    public synchronized static String load(String host) throws IOException {
        StringBuffer sb = new StringBuffer();
        
        ObjLink cookie = s_cookies;
        ObjLink prev = null;
        float timeout = System.currentTimeMillis()/1000 - COOKIES_TIMEOUT;
        
        while (cookie != null) {
            // purge timed out cookies
            if (cookie.m_z < timeout) {
                //Logger.println("SmartHttpCookies: removing timeout cookie : "+cookie.m_object+" at "+cookie.m_z+" < "+timeout);
                if (prev != null) {
                    prev.m_next = cookie.m_next;
                } else if (cookie == s_cookies) {
                    s_cookies = cookie.m_next;
                }
            } else { // search matching domain
                String domain = (String)cookie.m_object;
                if (host.endsWith(domain)) {
                    //Logger.println("SmartHttpCookies: loading cookie for "+host+" : "+((String)cookie.m_param));
                    cookie.m_z = System.currentTimeMillis()/1000; // update cookie timeout each time it is used.
                    // Append cookie content
                    if (sb.length() != 0) sb.append(';');
                    sb.append((String)cookie.m_param);
                }
            }
            prev = cookie;
            cookie = cookie.m_next;
        }
        return sb.length() != 0 ? sb.toString() : null;
    }
    
    public static void parseCookieHeader(String cookieHeader, String domain) {
        String[] cookie = split(cookieHeader, domain);
        save(cookie[0], cookie[1]);
    }
    
    public synchronized static void save(String cookieContent, String host) {
        String cookieKey = cookieContent.substring(0, cookieContent.indexOf('='));
        float now = System.currentTimeMillis()/1000;
        
        // Search a cookie matching a domain & cookie key to overwrite
        ObjLink cookie = s_cookies;
        while (cookie != null) {
            String domain = (String)cookie.m_object;
            if (host.equals(domain)) {
                String key = (String)cookie.m_param;
                key = key.substring(0, key.indexOf('='));
                if (cookieKey.equals(key)) {
                    // Logger.println("SmartHttpCookies: overwrite cookie "+cookieContent+" for "+host);
                    cookie.m_param = cookieContent;
                    cookie.m_z = now;
                    return;
                }
            }
            cookie = cookie.m_next;
        }
        // Previous cookie not found, just add it to list
        Logger.println("SmartHttpCookies: add new cookie "+cookieContent+" for "+host);
        s_cookies = ObjLink.create(host, cookieContent, now, s_cookies);
    }
    
    private static String[] split(String content, String domain) { 
        String res=null;
        String val; String[] split;
        while (content != null) {
            split = split(content, ';');
            // First data before ; is the cookie content
            if (res == null) {
                res = split[0];
                content = split[1];
                continue;
            }
            val = split[0];
            content = split[1];
            if (val.length() != 0) {
                split = split(val, '=');
                if (split[1] != null) {
//#ifndef MM.pfs
                    String key = split[0].toLowerCase();
                    if (key.equals("domain")) {
                        domain = split[1];
                    }
//#endif
                    /*} else if (key.equals("path")) {
                        path = split[1];
                    } else if (key.equals("expires")) {
                        expires = split[1];
                    } else {
                        break;
                    }*/
                }
            }
        }
        return new String [] { res, domain /*, path, expires*/ };
    }
    
    private static String[] split(String content, char sep) {
        String a=null, b=null;
        int pos = content.indexOf(sep);
        if (pos != -1) {
            a = content.substring(0, pos).trim();
            if (pos < content.length()-1) {
                b = content.substring(pos+1).trim();    
            }
        } else {
            a = content.trim();
        }
        return new String[] { a, b };
    }
}
