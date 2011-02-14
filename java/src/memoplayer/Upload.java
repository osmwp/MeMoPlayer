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
    boolean m_doUpload=false;

    private final int IDX_URL_SERVER     = 0;
    private final int IDX_HTTP_ANSWER    = 1;
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
        m_field[IDX_URL_SERVER]   = new MFString(); // url of the request
        m_field[IDX_HTTP_ANSWER]  = new MFString(); // will contain the answer of the request
        m_field[IDX_ACTIVATE]     = new SFBool (false, this); // activate
        m_field[IDX_IS_ACTIVE]    = new SFBool (false); // isActive
        m_field[IDX_RESPONSE_CODE]= new SFInt32(0); //responseCode (of the HTTP POST request)
        m_field[IDX_PROGRESS]     = new SFFloat(0); // progress (of the upload)
        m_field[IDX_ARRAY_NAMES]  = new MFString(); // array of custom param names
        m_field[IDX_ARRAY_VALUES] = new MFString(); // array of custom param values
        m_field[IDX_ARRAY_TYPES]  = new MFString(); // array of custom param types
        m_field[IDX_ARRAY_FILES]  = new MFString(); // array of custom param file urls
    }

    public boolean compose (Context c, Region clip, boolean forceUpdate) {
        boolean updated = forceUpdate;

        if ( m_activateChanged ) {
            m_activateChanged = false;
            if( ((SFBool)m_field[IDX_ACTIVATE]).getValue() == true ) {
                new Thread () {
                    public void run () {
                    	m_doUpload = true;
                try {
                    postHttpMultiPart();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        m_doUpload = false;
                }
                }.start ();
            }
        }
        //updated |= specificCompose (c, clip, updated);
        return updated;
    }

    public void fieldChanged (Field field) {
        if (field == m_field[IDX_ACTIVATE]) {
        	boolean val = ((SFBool)m_field[IDX_ACTIVATE]).getValue();
        	if ( val == false ) {
            	// ask to cancel upload
        		m_doUpload = false;
        	} else {
        		// ask to initiate upload
        		if ( m_doUpload == false ) {
            m_activateChanged = true;
        		} else {
        			Logger.println("Upload already activated");
        }
    }
        }
        }

    static final String BOUNDARY = "----------V2ymHFg03ehbqgZCaKO6jy";

    private void postHttpMultiPart( ) throws IOException {
        ((SFFloat)m_field[IDX_PROGRESS]).setValue(FixFloat.float2fix(0));

        int i=0;
        float dataSize=0;
        long dataWritten=0;

        // get element count
        int size = ((MFString)m_field[IDX_ARRAY_NAMES]).m_size;
        // compute data size
        for (i=0; i<size; i++) {

            String key      = ((MFString)m_field[IDX_ARRAY_NAMES]).getValue(i);
            String value    = ((MFString)m_field[IDX_ARRAY_VALUES]).getValue(i);
            String type     = ((MFString)m_field[IDX_ARRAY_TYPES]).getValue(i);
            String filename = ((MFString)m_field[IDX_ARRAY_FILES]).getValue(i);

            if( (type==null) || (type.length()==0 ) ) {
            	dataSize += (key.length()+value.length());
            } else {
                if( (filename!=null) && (filename.length()>0)) {
                    File dataFile = new File(filename);
                    if ( dataFile!=null ) {
                    	dataSize += dataFile.getLen();
                        dataFile.close(File.CLOSED);
                    }
                } else {
                	dataSize += (key.length()+value.length()+type.length());
                }
            }
        }

        if (dataSize<1) {
        	// error nothing to send
            ((SFFloat)m_field[IDX_PROGRESS]).setValue(FixFloat.float2fix(1));
            ((SFInt32)m_field[IDX_RESPONSE_CODE]).setValue(-1);
        	return;
        }

        // sending the http message
        HttpConnection hc = null;
        ByteArrayOutputStream bos=null;
        InputStream is = null;
        byte[] res = null;
        url = ((MFString)m_field[IDX_URL_SERVER]).getValue(0);
        Logger.println("Upload, connecting to: "+url);

        try {
            // open http conection: multipart/form-data and POST
            hc = (HttpConnection) Connector.open(url);
            if (hc!=null) {
	            Logger.println("Connected, uploading ...");

            hc.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
            hc.setRequestMethod(HttpConnection.POST);

            // write the http message
	            OutputStream os = hc.openOutputStream();

	            // write beginning boundary
	            os.write(("--"+BOUNDARY+"\r\n").getBytes());

	            float progress = (float)0.2;

	            ((SFFloat)m_field[IDX_PROGRESS]).setValue(FixFloat.float2fix(progress));

	            // write form data
	            for (i=0; i<size; i++) {

	                String key      = ((MFString)m_field[IDX_ARRAY_NAMES]).getValue(i);
	                String value    = ((MFString)m_field[IDX_ARRAY_VALUES]).getValue(i);
	                String type     = ((MFString)m_field[IDX_ARRAY_TYPES]).getValue(i);
	                String filename = ((MFString)m_field[IDX_ARRAY_FILES]).getValue(i);

	                if ( (type==null) || (type.length()==0 ) ) {
	                	os.write(("Content-Disposition: form-data; name=\""+key+"\"\r\n").getBytes());
	                	os.write(("\r\n"+value+"\r\n").getBytes());
	                	os.write(("--"+BOUNDARY+"\r\n").getBytes());
	                	dataWritten += (key.length()+value.length());
	                } else {
	                    if ( (filename!=null) && (filename.length()>0)) {
	                        File dataFile = new File(filename);
	                        byte[] fileData = dataFile.getData();
	                        if ( fileData==null ) {
	                            fileData = dataFile.readAllBytes();
	                            dataFile.close(File.CLOSED);
            }
	                        if ( (fileData!=null) && (fileData.length>0) ) {
	                        	os.write(("Content-Disposition: form-data; name=\""+key+"\"; filename=\""+value+"\"\r\n").getBytes());
	                        	os.write(("Content-Type: "+type+"\r\n\r\n").getBytes());
		                        int block = fileData.length / 10;
		                        int left=0;
		                        for ( int j=0; j<=10; j++ ) {
		                        	left = fileData.length - (j*block);
		                        	if (left>block ) {
		                        		os.write(fileData,j*block,block);
		                        		dataWritten+=block;
		                        	} else {
		                        		if (left>0) {
			                        		os.write(fileData,j*block,left);
			                        		dataWritten+=left;
        }
    }
		        	                progress = (float)(0.2 + (0.6*(dataWritten/dataSize)));
		        		            ((SFFloat)m_field[IDX_PROGRESS]).setValue(FixFloat.float2fix(progress));
    }
	                        	os.write(("--"+BOUNDARY+"\r\n").getBytes());
	                        	os.write(("--"+BOUNDARY+"\r\n").getBytes());
	                        }
	                    } else {
	                    	os.write(("Content-Disposition: form-data; name=\""+key+"\"\r\n").getBytes());
	                    	os.write(("\r\n"+value+"\r\n").getBytes());
	                        os.write(("Content-Type: "+type+"\r\n").getBytes());
	                        os.write(("--"+BOUNDARY+"\r\n").getBytes());
		                	dataWritten += (key.length()+value.length()+type.length());
        }
    }

	                progress = (float)(0.2 + (0.6*(dataWritten/dataSize)));
		            ((SFFloat)m_field[IDX_PROGRESS]).setValue(FixFloat.float2fix(progress));
	            }

	            // write ending boundary
	            os.write(("\r\n--" + BOUNDARY + "--\r\n").getBytes());

	            os.close();

	            progress = (float)0.9; 
	            ((SFFloat)m_field[IDX_PROGRESS]).setValue(FixFloat.float2fix(progress));

	            Logger.println("Upload done waiting answer ...");

	            // read the response
            int ch;

	            bos = new ByteArrayOutputStream();
            is = hc.openInputStream();

            while ((ch = is.read()) != -1)
            {
                bos.write(ch);
            }
	            
	            bos.close();
	            Logger.println("Answer received");
	            
	            ((MFString)m_field[IDX_URL_SERVER]).setValue(1,bos.toString());
                ((SFInt32)m_field[IDX_RESPONSE_CODE]).setValue(hc.getResponseCode());
            } else {
	            Logger.println("Error connecting to: "+url);
                ((SFInt32)m_field[IDX_RESPONSE_CODE]).setValue(-1);
        }
            

        } catch(Exception e) {
            e.printStackTrace();
            ((SFInt32)m_field[IDX_RESPONSE_CODE]).setValue(-1);
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

        ((SFFloat)m_field[IDX_PROGRESS]).setValue(FixFloat.float2fix(1));

    }
    
}
