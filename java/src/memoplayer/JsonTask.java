//#condition api.jsonrpc
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
import java.util.*;

/**
 * This task requires the "PFS" property to be set as the default JSON-RPC server. 
 */
public class JsonTask extends Task {

    private String m_url;
    private byte[] m_paramData;
    private Object m_result;
    private boolean m_isRpc;
    
    private SmartHttpConnection shcon = null;
    private DataInputStream dis = null;
    private DataOutputStream dos = null;
    
    JsonTask(String url) {
        m_isRpc = false;
        m_url = url;
        m_paramData = null;
    }
    
    JsonTask(String functionName, Object[] params, int paramsSize, int requestId) {
        m_isRpc = true;
        
        // Discover Url
        int sep = functionName.indexOf("?");
        if (sep > 0) {
            m_url = functionName.substring(0, sep);
            functionName = functionName.substring(sep + 1);
//#ifdef MM.pfs
        } else {
            m_url = MiniPlayer.getPfsBaseUrl() + "JSON-RPC";
//#endif
        }
        // Support for || in function name to handle HTTP headers
        sep = functionName.indexOf("||");
        if (sep > 0) {
            m_url += functionName.substring(sep);
            functionName = functionName.substring(0, sep);
        }
        
        // Serialize args
        String jsonCall = parseArg(functionName, params, paramsSize, requestId);
        try {
            m_paramData = jsonCall.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Logger.println("JsonTask: Could not encode UTF-8, fallback to default encoding.");
            m_paramData = jsonCall.getBytes();
        }
        //Logger.println("JsonTask: "+jsonCall);
    }
    
    void doTheJob() throws Exception {
        shcon = new SmartHttpConnection(m_url,m_isRpc);
        
        // When using RPC, send method and args 
        if (m_isRpc) {
            dos = shcon.openDataOutputStream();
            if (dos == null) throw new Exception("Could not send JSON data");
            dos.write(m_paramData);
            dos.close();

//#ifdef api.traffic
            // Count uploaded data
            Traffic.update(m_paramData.length);
//#endif
            dos = null;
        }
        
        // do not check content length, because it's not pertinent on Alcatel OT800
        //int length = (int)shcon.getLength();
        //if (length <= 0) throw new Exception ("Empty answer !");
        
        // Read content
        dis = shcon.openDataInputStream();
        
        JsonDecoder jsd = new JsonDecoder();
        try {
            m_result = jsd.decode(dis);
        } catch (Exception e) {
            throw new Exception("Could not decode JSON data : "+e);
        }
        jsd = null;
        clean();
       
        if (m_result == null) throw new Exception ("Returned null");
        
        if(m_isRpc && m_result instanceof Hashtable) {
            Hashtable ht = (Hashtable)m_result;
            if (ht.containsKey("result")) {
                m_result = ht.get("result");
            } else if (ht.containsKey("error")) {
                ht = (Hashtable)ht.get("error");
                throw new Exception("remote error: "+ht.get("code")+": "+ht.get("msg")); 
            }
        }
    }
    
    boolean onError (Exception e) {
        Logger.println("JsonRpcTask: Exception: "+e.getMessage());
        e.printStackTrace();
        m_result = new Integer (-1);
        if (shcon != null) m_result = new Integer(shcon.getResponseCode());
        clean();
        return super.onError(e);
    }
    
    Object getResult() {
        return m_result;
    }
    
    private String parseArg(String functionName, Object[] params, int paramsSize, int requestId) {
        StringBuffer callStr = new StringBuffer();
        callStr.append("{\"id\":");
        callStr.append(requestId + 1);
        callStr.append(",\"method\":\"");
        callStr.append(functionName);
        callStr.append("\",\"params\":[");
        for(int i= 0; i<paramsSize; i++) {
            if(i != 0)
                callStr.append(',');
            objToJsonStr(callStr, params[i]);
        }
        callStr.append("]}");
        //Logger.println("JsonRpcRequest: "+callStr);
        return callStr.toString();
    }

    private void objToJsonStr(StringBuffer ret, Object o) {
        if (o == null) {
            // nothing
        } else if (o instanceof Hashtable) {
            ret.append('{');
            for (Enumeration e = ((Hashtable) o).keys(); e.hasMoreElements();) {
                Object key = (Object) e.nextElement();
                Object val = ((Hashtable) o).get(key);
                ret.append('"');
                ret.append(key.toString());
                ret.append('"');
                ret.append(':');
                objToJsonStr(ret, val);
                if(e.hasMoreElements())
                    ret.append(',');
            }
            ret.append('}');
        } else if (o instanceof String) {
            ret.append('"');
            ret.append(escape((String)o));
            ret.append('"');
        } else if (o instanceof Vector) {
            ret.append('[');
            for (Enumeration e = ((Vector) o).elements(); e.hasMoreElements();) {
                objToJsonStr(ret, e.nextElement());
                if(e.hasMoreElements())
                    ret.append(',');
            }
            ret.append(']');
        } else {
            ret.append(o.toString());
        }
    }
    
    private void clean() {
        m_paramData = null;
        if (dos != null) try { dos.close(); dos = null; } catch (Exception e) {}
        if (dis != null) try { dis.close(); dis = null; } catch (Exception e) {}
        if (shcon != null) try { shcon.close(); shcon = null; } catch (Exception e) {}
    }
    
    /**
     * Escape double quotes and backslash characters
     */
    private String escape(String s) {
        char[] chars = s.toCharArray();
        StringBuffer sb = new StringBuffer(s);
        int c = 0, size = chars.length;
        for (int i=0; i<size; i++) {
            switch(chars[i]) {
            case '"':
            case '\\':
                sb.insert(i+c, '\\'); 
                c++; 
                break;
            }
        }
        return sb.toString();
    }
}
