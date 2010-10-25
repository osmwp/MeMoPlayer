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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class PackageMemoRimApp extends Task {
    File     appJadFile;
    File     memoLibJadFile;
   
    @Override
    public void execute() throws BuildException {
            validate();
            try {
        		String rimCodUrl  = "RIM-COD-URL";
        		String rimCodSize = "RIM-COD-Size";
        		String rimCodSha1 = "RIM-COD-SHA1";
            	
            	
            		BufferedReader rdr = new BufferedReader(new FileReader(appJadFile));
        			String line = rdr.readLine();
        			int idxJad=-1;
                    while (line!=null) {
                        line = line.trim();
                        if (line.length()>0) {
                        	if( line.startsWith(rimCodUrl) ) {
                        		int nIdxEnd = line.indexOf(":");
                        		String str = line.substring(rimCodUrl.length(), nIdxEnd);
                            	if( str.startsWith("-") ) {
                            		int idx = Integer.parseInt(str.substring(1));
                            		if( idx > idxJad )
                            			idxJad = idx;
                        		} else {
                        			if( idxJad == -1 )
                        				idxJad = 0;
                        		}
                        	}
                        }
                        line = rdr.readLine();
                    }
                    rdr.close();

                    if( idxJad == -1 ) {
                    	throw new BuildException("Invalid jad file, no RIM-COD-URL found.");
                    }
                    
                    idxJad++;
                    
                    rdr = new BufferedReader(new FileReader(memoLibJadFile));
        			line = rdr.readLine();

        			FileWriter fstream = new FileWriter(appJadFile.getAbsolutePath(),true);
                    BufferedWriter out = new BufferedWriter(fstream);

                    while (line!=null) {
                    	if( line.startsWith(rimCodUrl) ) {
                    		writeJad(out, rimCodUrl, line, idxJad);
                    	} else if( line.startsWith(rimCodSize) ) {
                    		writeJad(out, rimCodSize, line, idxJad);
                    	} else if( line.startsWith(rimCodSha1) ) {
                    		writeJad(out, rimCodSha1, line, idxJad);
                    	}
                        line = rdr.readLine();
                    }
                    rdr.close();
                    out.close();
                    
                    
            } catch (Exception e) {
                    throw new BuildException(e, getLocation());
            }
    }

    protected void writeJad(BufferedWriter out, String rimProperty, String rimLibProperty, int idxOffset) throws IOException {
		int nIdxEnd = rimLibProperty.indexOf(":");
		String str = rimLibProperty.substring(rimProperty.length(), nIdxEnd);
    	if( str.startsWith("-") ) {
    		int idx = Integer.parseInt(str.substring(1))+idxOffset;
    		out.write(rimProperty+"-"+idx+rimLibProperty.substring(nIdxEnd));
		} else {
    		out.write(rimProperty+"-"+idxOffset+rimLibProperty.substring(nIdxEnd));
		}
    	out.write("\r\n");
    }

    protected void validate() {
        if (memoLibJadFile==null)   throw new BuildException("You must specify a lib jad file to read.");
        if (appJadFile==null)       throw new BuildException("You must specify a jad file to write.");
        if (!memoLibJadFile.canRead()) throw new BuildException("Can not read file " + memoLibJadFile.getAbsolutePath());
    }
   
   
    public void setAppJadFile(File file) {
        this.appJadFile = file;
    }
    public void setMemoLibJadFile(File file) {
        this.memoLibJadFile = file;
    }
}
