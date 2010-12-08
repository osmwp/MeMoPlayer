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
import java.io.*;

import javax.microedition.io.*;

class SmartHttpConnection implements Connection {
    private final static String HEADERS_SEP = "||";
    private final static int POST_MODE = -1;
    
    private final static String HTTP_COOKIE_FIELD = "cookie";
    private final static String HTTP_SET_COOKIE_FIELD = "set-cookie";
    
    private final static String s_extraUA;
    static {
        String ua = MiniPlayer.getJadProperty ("MeMo-Extra-UA");
        s_extraUA = (ua == "") ? null : ua;
    }
    

    String m_url;
    HttpConnection m_conn;
    int m_responseCode;
    DataOutputStream m_dos;
    DataInputStream m_dis;
    
    boolean m_handleRedirects = true;
    String[] m_headers;
    int m_headersCnt;
    
    public SmartHttpConnection(String url, boolean write) throws IOException {
        m_url = checkForExtensions (url);
        
        if (write) {
            //Logger.println ("SmartHttpConnection: opening url: "+url+" in write mode");
            //MCP: When opening a SmartHttpConnection for write, the POST method is used.
            // The POST sends data when the OutputStream is asked for.
            // Then the response code will be fetch when the InputStream is asked for.
            m_conn = (HttpConnection) Connector.open(m_url, Connector.READ_WRITE);
            m_conn.setRequestMethod(HttpConnection.POST);
            addHttpHeaders();
            m_responseCode = POST_MODE;
            return;
        }
        
        while (m_url != null) {            
            m_conn = (HttpConnection) Connector.open(m_url);
            m_conn.setRequestMethod(HttpConnection.GET);
            addHttpHeaders();
            m_responseCode = getResponse();
            switch (m_responseCode) {
            case HttpConnection.HTTP_MOVED_PERM:
            case HttpConnection.HTTP_MOVED_TEMP:
            case HttpConnection.HTTP_SEE_OTHER:
            case HttpConnection.HTTP_TEMP_REDIRECT:
                if (m_handleRedirects) {
                    m_url = m_conn.getHeaderField("Location");
                    //Logger.println("\n Location :"+m_url);
                    if (m_url != null && m_url.startsWith("/")) {
                        StringBuffer sb = new StringBuffer();
                        sb.append(m_conn.getProtocol()).append("://");
                        sb.append(m_conn.getHost());
                        sb.append(':');
                        sb.append(m_conn.getPort());
                        sb.append(m_url);
                        m_url = sb.toString();
                    }
                    //Logger.println("SmartHttpConnection: redirect to: " + m_url);
                    m_conn.close();
                    break;
                }
            default:
                m_url = null;
                break;
            }
        }
    }

    public DataOutputStream openDataOutputStream() throws IOException {
        if (m_responseCode != POST_MODE || m_conn == null) {
            throw new IOException("Http error: cannot open output stream");
        }
        //m_conn.setRequestProperty("Content-Type", "application/octet-stream");
        return m_dos = m_conn.openDataOutputStream();   
    }
    
    public DataInputStream openDataInputStream() throws IOException {
        if (m_responseCode == POST_MODE) {
            if (m_dos != null) {
                try { m_dos.close(); } catch (Exception e) {}
                m_dos = null;
            }
            // Going back to read mode
            m_responseCode = getResponse();
        }
        if(m_responseCode == HttpConnection.HTTP_OK) {
            return m_dis = m_conn.openDataInputStream();
        }
        throw new IOException("Http error: "+m_responseCode);
    }

    public void close() throws IOException {
        if (m_conn != null) {
            if (m_dos != null) {
                try { m_dos.close(); } catch (Exception e) {}
                m_dos = null;
                // POST was sent without reading the return data
                m_responseCode = getResponse();
            }
            if (m_dis != null) {
                try { m_dis.close(); } catch (Exception e) {}
                m_dis = null;
            }
            try { m_conn.close(); } catch (Exception e) {}
            m_conn = null;
        }
    }
    
    public long getLength() {
        return m_conn != null ? m_conn.getLength() : -1;
    }
    
    private int getResponse() throws IOException {
        int responseCode = m_conn.getResponseCode();
        
//#ifdef MM.pfs
        parseUpdateHeader();
//#endif
        
        parseCookieHeaders(HTTP_SET_COOKIE_FIELD, m_conn.getHost());
        return responseCode;
    }
    
    private void addHttpHeaders() throws IOException {
//#ifdef MM.pfs
    	addTagsHeader ();
//#endif
        
        String cookie = SmartHttpCookies.load(m_conn.getHost());
        if (cookie != null) {
            m_conn.setRequestProperty(HTTP_COOKIE_FIELD, cookie);
        }
        String extraUA = s_extraUA;
        // Set headers found during parse of URL
        for (int i=0; i<m_headersCnt; i+=2) {
            if (extraUA != null && m_headers[i].startsWith ("User-Agent")) {
                m_headers[i+1] = extraUA + m_headers[i+1];
                extraUA = null;
            }
            m_conn.setRequestProperty (m_headers[i], m_headers[i+1]);
        }
        if (extraUA != null) { // force a User Agent anyway
            m_conn.setRequestProperty("User-Agent", extraUA);
        }
    }
    
    private void parseCookieHeaders(String headerKey, String host) throws IOException {
        // Parse all header to save cookies
        int n = 0; 
        while (true) {
            String key = m_conn.getHeaderFieldKey(++n);
            if (key == null) break;
            //Logger.println("HTTP Header: "+key);
            if (key.toLowerCase().equals(headerKey)) {
                SmartHttpCookies.parseCookieHeader(m_conn.getHeaderField(n), host);
            }
        }
    }

    public int getResponseCode() {
        return m_responseCode;
    }
    
    private String checkForExtensions(String url) {
        int end = url.indexOf(HEADERS_SEP);
        if (end != -1) {
            String ext = url.substring(end+2);
            url = url.substring(0, end);
            int start = 0;
            //Logger.println("SmartHttpConnection: Url with extensions: "+url+" : "+ext);
            while ((end = ext.indexOf(HEADERS_SEP, start)) != -1) {
                checkExt(ext, start, end);
                start = end + 2;
                if (end >= ext.length()) {
                    return url;
                }
            }
            checkExt(ext, start, ext.length());
        }
        return url;
    }
    
    private void checkExt(String ext, int start, int end) {
        int sep = ext.indexOf('=', start);
        if (sep != -1 && sep < end) {
            //Logger.println("SmartHttpConnection: Found HTTP header: "+ext.substring(start, end));
            if (m_headers == null || m_headers.length == m_headersCnt) {
                String[] pH = m_headers;
                m_headers = new String[m_headersCnt*2+2];
                if (pH != null) { // copy previously allocated headers
                    System.arraycopy(pH, 0, m_headers, 0, m_headersCnt);
                }
            }
            m_headers[m_headersCnt++] = ext.substring(start, sep);
            m_headers[m_headersCnt++] = ext.substring(sep+1, end);
        } else if (ext.substring(start, end).trim().equals("noredirects")) {
            //Logger.println("SmartHttpConnection: Disabling redirects");
            m_handleRedirects = false;
        }
    }
    
//#ifdef MM.pfs
    
    /**
     * Adds a x-tags header with content of the xtags cookie
     * if the target server is the same as the baseUrl.
     */
    public void addTagsHeader() throws IOException {
    	String baseUrl = CookyManager.get ("baseUrl");
    	if (baseUrl.length() != 0 && m_url.startsWith(baseUrl)) {
    		String tags = CookyManager.get ("xtags");
	    	if (tags.length() != 0) {
	            m_conn.setRequestProperty ("X-Tags", tags);
	        }
    	}
    }
    
    /**
     * Check for the specific x-update HTTP header.
     * Open browser on given url.
     */
    public void parseUpdateHeader() throws IOException {
        String data = m_conn.getHeaderField("x-update");
        if (data != null && data.length() != 0) {
            MiniPlayer.openUrl(data);
        }
    }

//#endif
    
}
