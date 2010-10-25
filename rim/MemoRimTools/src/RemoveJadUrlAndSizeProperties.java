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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class RemoveJadUrlAndSizeProperties extends Task {
    File   file;
   
    @Override
    public void execute() throws BuildException {
            validate();
            try {
            		String filePath = file.getPath();
            		BufferedReader rdr = new BufferedReader(new FileReader(file));
                    FileWriter fstream = new FileWriter(filePath+".tmp");
                    BufferedWriter out = new BufferedWriter(fstream);
                    String line = rdr.readLine();
                    while (line!=null) {
                            line = line.trim();
                            if (line.length()>0) {
                            	if( 	(line.startsWith("MIDlet-Jar-Size")==false)
                            		&&	(line.startsWith("MIDlet-Jar-URL") ==false) ) {
                            		out.write(line);
                            		out.write("\r\n");
                            	} 
                            }
                            line = rdr.readLine();
                    }
                    rdr.close();
                    out.close();
                    file.delete();
                    File fileDest = new File(filePath+".tmp");
                    fileDest.renameTo(file);
                    
            } catch (Exception e) {
                    throw new BuildException(e, getLocation());
            }
    }


    protected void validate() {
            if (file==null)      throw new BuildException("You must specify a file to read.");
            if (!file.canRead()) throw new BuildException("Can not read file " + file.getAbsolutePath());
    }
   
   
    public void setFile(File file) {
            this.file = file;
    }
}
