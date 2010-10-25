//#condition MM.Upload
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
import java.io.OutputStream;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.Enumeration;
import java.util.Hashtable;

import java.util.Random;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

public class Upload extends Node {
    File stream ;
    String url;
    String header;

    boolean m_activateChanged=false;

    private final int IDX_URL_VIDEO      = 0;
    private final int IDX_URL_SERVER     = 1;
    private final int IDX_ACTIVATE       = 2;
    private final int IDX_IS_ACTIVE      = 3;
    private final int IDX_RESPONSE_CODE  = 4;
    private final int IDX_PROGRESS       = 5;
    private final int IDX_ARRAY_NAMES    = 6;
    private final int IDX_ARRAY_VALUES   = 7;
    private final int IDX_ARRAY_TYPES    = 8;
    private final int IDX_ARRAY_FILES    = 9;

    Upload () {
        super(10);
        m_field[IDX_URL_VIDEO]    = new MFString(this); //url of the video file to upload
        m_field[IDX_URL_SERVER]   = new MFString(this); //url of the server
        m_field[IDX_ACTIVATE]     = new SFBool (false, this); // activate
        m_field[IDX_IS_ACTIVE]    = new SFBool (false); // isActive
        m_field[IDX_RESPONSE_CODE]= new SFInt32(0); //responseCode (of the HTTP POST request)
        m_field[IDX_PROGRESS]     = new SFFloat(0); //progess (of the download)
        m_field[IDX_ARRAY_NAMES]  = new MFString(this); // array of custom param names
        m_field[IDX_ARRAY_VALUES] = new MFString(this); // array of custom param values
        m_field[IDX_ARRAY_TYPES]  = new MFString(this); // array of custom param types
        m_field[IDX_ARRAY_FILES]  = new MFString(this); // array of custom param file urls
    }

    public boolean compose (Context c, Region clip, boolean forceUpdate) {
        boolean updated = m_isUpdated | forceUpdate;
        //PB version JDK
        if ( m_activateChanged ) {
            m_activateChanged = false;
            if( ((SFBool)m_field[IDX_ACTIVATE]).getValue() == true ) {
                try {
                    postHttpMultiPart();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        } else  if (m_isUpdated) {
            m_isUpdated = false;
            prepareRequest();
        }
        //updated |= specificCompose (c, clip, updated);
        return updated;
    }

    public void fieldChanged (Field field) {
        if (field == m_field[IDX_URL_VIDEO]) {
            m_isUpdated = true;
        } else if (field == m_field[IDX_ACTIVATE]) {
            m_activateChanged = true;
        }
    }

    public void launchHTTPRequest() {
        if (stream !=null) {
            try {
                int toNext = 6144;//number of octet in one pool
                byte [] data = stream.readBytes(stream.getLen());
                //      Logger.println("url : "+url.substring(30,url.length()));
                HttpConnection conn; 
                int i = 0;
                int rc = 0;
                String ss;
                int s = 0;
                OutputStream os ;
                int responseCode = HttpConnection.HTTP_OK;
                while (i < data.length)  {
                    conn = (HttpConnection) Connector.open( url );
                    conn.setRequestMethod( HttpConnection.POST );
                    conn.setRequestProperty( "User-Agent", "Profile/MIDP-2.0 Configuration/CLDC-1.1" );
                    conn.setRequestProperty( "Content-Type", "application/octet-stream" );
                    s = Math.min(toNext,(data.length-i));
                    ss = String.valueOf(s+(header.getBytes().length));

                    conn.setRequestProperty( "Content-Length", ss)  ;
                    //Logger.println("CONTENT LENGTH : "+ss);
                    os = conn.openOutputStream();
                    //      Logger.println("Test i = "+i+" min = "+Math.min(toNext,(data.length-i)));
                    os.write(header.getBytes());
                    os.write( data, i, s );
                    i = i + toNext;
                    //      Logger.println("Fin i = "+i);
                    os.close();
                    rc = conn.getResponseCode();                                    
                    if( rc != HttpConnection.HTTP_OK ){
                        //      Logger.println("Requete "+i+" POST ERROR : "+rc+" : "+conn.getResponseMessage()+", pour : "+conn.getQuery());
                        responseCode = rc;
                    } /*else {
                        Logger.println("Requete "+i+" POST OK");
                        }*/
                    conn.close();
                }
                ((SFInt32)m_field[IDX_RESPONSE_CODE]).setValue(responseCode);
            }
            catch( IOException ioe ){
                Logger.println("UploadData launchHTTPRequest : \nIOException : "+ioe.toString());
                ioe.printStackTrace();
                ((SFInt32)m_field[IDX_RESPONSE_CODE]).setValue(-1);
            } 
        }
        else Logger.println("Impossible de recuperer video");    
    }

//     boolean specificCompose (Context c, Region clip, boolean forceUpdate) {
//         return (false);
//     }

    private void prepareRequest() {
        if( ((SFFloat)m_field[IDX_PROGRESS]).getValue() != 0 ) {
        }
        String file_path = ((MFString)m_field[IDX_URL_VIDEO]).getValue(0);

        url = ((MFString)m_field[IDX_URL_SERVER]).getValue(0);

        if (!file_path.equalsIgnoreCase("")) {
            //the video file containing the camera capture
            if (file_path.equalsIgnoreCase("local")) {
                //      Logger.println("On passe dans local");
                file_path = System.getProperty("fileconn.dir.videos")+"video.3gp";
            }
            String ext = file_path.substring(file_path.lastIndexOf('.'));
            Random rnd = new Random (System.currentTimeMillis());
            header = ""+rnd.nextInt(65536)+ext+"&";
            //      Logger.println("filepath : "+file_path+", ext : "+ext+", url : "+url.substring(15,url.length())+", header : "+header);
            stream = new File (file_path);
            Logger.println ("File ok!");
            if(stream!=null && !url.equalsIgnoreCase("")) {
                launchHTTPRequest();
                //      Logger.println ("on peut y aller!");
            }
        } else  {
            Logger.println ("UploadData :: filepath==null");
        }
    }

    private void postHttpMultiPart( ) throws IOException {
        Register r=new Register();

        // get element count
        int size = ((MFString)m_field[IDX_ARRAY_NAMES]).m_size;

        // preparing http message

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        // write beginning boundary
        bos.write(("--"+BOUNDARY+"\r\n").getBytes());

        // write form data
        for(int i=0; i<size; i++) {

            String key      = ((MFString)m_field[IDX_ARRAY_NAMES]).getValue(i);
            String value    = ((MFString)m_field[IDX_ARRAY_VALUES]).getValue(i);
            String type     = ((MFString)m_field[IDX_ARRAY_TYPES]).getValue(i);
            String filename = ((MFString)m_field[IDX_ARRAY_FILES]).getValue(i);

            if( (type==null) || (type.length()==0 ) ) {
                bos.write(("Content-Disposition: form-data; name=\""+key+"\"\r\n").getBytes());
                bos.write(("\r\n"+value+"\r\n").getBytes());
                bos.write(("--"+BOUNDARY+"\r\n").getBytes());
            } else {
                if( (filename!=null) && (filename.length()>0)) {
                    File dataFile = new File(filename);
                    byte[] fileData = dataFile.getData();
                    if( fileData==null ) {
                        fileData = dataFile.readAllBytes();
                        dataFile.close(File.CLOSED);
                    }
                    if( (fileData!=null) && (fileData.length>0) ) {
                        bos.write(("Content-Disposition: form-data; name=\""+key+"\"; filename=\""+value+"\"\r\n").getBytes());
                        bos.write(("Content-Type: "+type+"\r\n\r\n").getBytes());
                        bos.write(fileData);
                        bos.write(("--"+BOUNDARY+"\r\n").getBytes());
                        bos.write(("--"+BOUNDARY+"\r\n").getBytes());
                    }
                } else {
                    bos.write(("Content-Disposition: form-data; name=\""+key+"\"\r\n").getBytes());
                    bos.write(("\r\n"+value+"\r\n").getBytes());
                    bos.write(("Content-Type: "+type+"\r\n").getBytes());
                    bos.write(("--"+BOUNDARY+"\r\n").getBytes());
                }
            }
        }

        // write ending boundary
        bos.write(("\r\n--" + BOUNDARY + "--\r\n").getBytes());

        // sending the http message
        HttpConnection hc = null;
        InputStream is = null;
        byte[] res = null;
        url = ((MFString)m_field[IDX_URL_SERVER]).getValue(0);
        Logger.println("Connecting to: "+url);

        try {
            // open http conection: multipart/form-data and POST
            hc = (HttpConnection) Connector.open(url);
            hc.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
            hc.setRequestMethod(HttpConnection.POST);

            // write the http message
            OutputStream dout = hc.openOutputStream();
            dout.write(bos.toByteArray());
            bos.close();
            dout.close();

            // read the response
            int ch;

               bos = new ByteArrayOutputStream();
            is = hc.openInputStream();
            while ((ch = is.read()) != -1)
            {
                bos.write(ch);
            }
            res = bos.toByteArray();
            bos.close();

        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if(bos != null)
                    bos.close();

                if(is != null)
                    is.close();

                if(hc != null)
                    hc.close();
            } catch(Exception e2) {
                e2.printStackTrace();
            }
        }

        ((SFFloat)m_field[IDX_PROGRESS]).setValue(FixFloat.float2fix(100));

    }

    static final String BOUNDARY = "----------V2ymHFg03ehbqgZCaKO6jy";

    byte[] postBytes = null;

    private void prepareHttpMultipartRequest( Hashtable params, String fileField, String fileName, String fileType, byte[] fileBytes) throws Exception
    {
        String boundaryMessage = getBoundaryMessage(BOUNDARY, params, fileField, fileName, fileType);

        String endBoundary = "\r\n--" + BOUNDARY + "--\r\n";

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        bos.write(boundaryMessage.getBytes());

        bos.write(fileBytes);

        bos.write(endBoundary.getBytes());

        this.postBytes = bos.toByteArray();

        bos.close();
    }

    String getBoundaryMessage(String boundary, Hashtable params, String fileField, String fileName, String fileType)
    {
        StringBuffer res = new StringBuffer("--").append(boundary).append("\r\n");

        Enumeration keys = params.keys();

        while(keys.hasMoreElements())
        {
            String key = (String)keys.nextElement();
            String value = (String)params.get(key);

            res.append("Content-Disposition: form-data; name=\"").append(key).append("\"\r\n")
                .append("\r\n").append(value).append("\r\n")
                .append("--").append(boundary).append("\r\n");
        }
        res.append("Content-Disposition: form-data; name=\"").append(fileField).append("\"; filename=\"").append(fileName).append("\"\r\n")
            .append("Content-Type: ").append(fileType).append("\r\n\r\n");

        return res.toString();
    }

    public byte[] send() throws Exception
    {
        HttpConnection hc = null;

        InputStream is = null;

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        byte[] res = null;

        try
        {
            hc = (HttpConnection) Connector.open(url);

            hc.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);

            hc.setRequestMethod(HttpConnection.POST);

            OutputStream dout = hc.openOutputStream();

            dout.write(postBytes);

            dout.close();

            int ch;

            is = hc.openInputStream();

            while ((ch = is.read()) != -1)
            {
                bos.write(ch);
            }
            res = bos.toByteArray();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if(bos != null)
                    bos.close();

                if(is != null)
                    is.close();

                if(hc != null)
                    hc.close();
            }
            catch(Exception e2)
            {
                e2.printStackTrace();
            }
        }
        return res;
    }
}
