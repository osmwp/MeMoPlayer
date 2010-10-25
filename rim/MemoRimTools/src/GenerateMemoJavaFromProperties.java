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

public class GenerateMemoJavaFromProperties extends Task {
    File     file;
    File     destFile;
    String   baseClass;
   
    @Override
    public void execute() throws BuildException {
            validate();
            try {
            		String filePath = file.getPath();
            		String javaClass = destFile.getName();
            		if( javaClass.endsWith(".java") == false ) {
            			throw new BuildException("The destination file must have .java extension.");
            		}
            		javaClass = javaClass.substring(0,javaClass.length()-5);
            		
            		BufferedReader rdr = new BufferedReader(new FileReader(file));
                    FileWriter fstream = new FileWriter(destFile.getAbsolutePath());
                    BufferedWriter out = new BufferedWriter(fstream);
                    
        			out.write("class "+javaClass+" extends "+baseClass+" {\n\n");
        			out.write("    protected "+javaClass+" () {}\n\n");
        			out.write("    // Entry point for BB application\n    public static void main( String[] args )\n    {\n");
        			out.write("        self = new "+javaClass+"();\n");
        			out.write("        self.enterEventDispatcher();\n    }\n\n");

        			String line = rdr.readLine();
                    int nbLine=0;
                    while (line!=null) {
                            line = line.trim();
                            if (line.length()>0) {
                            	if( line.startsWith("jad.") && (line.indexOf('=')>0) ) {
                            		int indexEquals = line.indexOf('=');
                            		if(nbLine==0) {
                                        out.write("\n    // jad properties stub\n");
                                        out.write("    protected String getApplicationProperty (String name) {\n");
                            		}
                            		nbLine++;
                        			String property = line.substring(4,indexEquals);
                                    out.write("        if(name.equalsIgnoreCase(\""+property+"\")) {\n"); 
                                    out.write("            return \""+line.substring(indexEquals+1)+"\";\n        }\n");
                            	} 
                            }
                            line = rdr.readLine();
                    }

                    if(nbLine>0) {
                        out.write("        return \"\";\n    }\n");
                    }
                    out.write("\n\n}");
                    
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
        if (file==null)       throw new BuildException("You must specify a property file to read.");
        if (destFile==null)   throw new BuildException("You must specify a destination file to read.");
        if (!file.canRead())      throw new BuildException("Can not read file " + file.getAbsolutePath());
        if (baseClass==null) throw new BuildException("baseClass property is not set.");
        if (baseClass.length()==0) throw new BuildException("baseClass property can not be empty.");
    }
   
   
    public void setFile(File file) {
        this.file = file;
    }
    public void setDestFile(File file) {
        this.destFile = file;
    }
    public void setBaseClass(String string) {
        this.baseClass = string;
    }
}
