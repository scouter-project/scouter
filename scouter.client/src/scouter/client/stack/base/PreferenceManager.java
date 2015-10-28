/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package scouter.client.stack.base;
import java.io.File;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class PreferenceManager {
    private final static PreferenceManager preferenceManager = new PreferenceManager();
    public static final String DELIMETER = "\u00A7\u00A7\u00A7\u00A7";
    
    private final Preferences preferences;
    
    private PreferenceManager() {
        preferences = Preferences.userNodeForPackage(this.getClass());
    }
    
    public static PreferenceManager get() {
        return(preferenceManager);
    }

    public String getSelectedPath() {
        return preferences.get("selectedPath", "");
    }
    
    public void setSelectedPath(File directory) {
    	preferences.put("selectedPath", directory.getAbsolutePath());
    }

    public void setMaxLogfileSize(int size) {
    	preferences.putInt("maxlogfilesize", size);
    }

    public int getTopDividerPos() {
        return(preferences.getInt("top.dividerPos", 0));
    }
    
    public void setTopDividerPos(int pos) {
    	preferences.putInt("top.dividerPos", pos);
    }

    public int getDividerPos() {
        return(preferences.getInt("dividerPos", 0));
    }
    
    public void setDividerPos(int pos) {
    	preferences.putInt("dividerPos", pos);
    }

    public void addToStackFiles(String file) {
        String[] currentFiles = getStackFiles();
        
        if(!hasInFiles(file, currentFiles)) {
            int start = currentFiles.length == 10 ? 1 : 0;
            StringBuilder recentFiles = new StringBuilder();
            
            for(int i = start; i < currentFiles.length; i++) {
                recentFiles.append(currentFiles[i]);
                recentFiles.append(DELIMETER);
            }
            
            recentFiles.append(file);
            preferences.put("stackFiles", recentFiles.toString());
        }
    }
    
    public String[] getStackFiles() {
        return(preferences.get("stackFiles", "").split(DELIMETER));
    }

    public void addToAnalyzedStackFiles(String file) {
        String[] currentFiles = getAnalyzedStackFiles();
        
        if(!hasInFiles(file, currentFiles)) {
            int start = currentFiles.length == 10 ? 1 : 0;
            StringBuilder recentFiles = new StringBuilder();
            
            for(int i = start; i < currentFiles.length; i++) {
                recentFiles.append(currentFiles[i]);
                recentFiles.append(DELIMETER);
            }
            
            recentFiles.append(file);
            preferences.put("analyzedStackFiles", recentFiles.toString());
        }
    }
    
    public String[] getAnalyzedStackFiles() {
        return(preferences.get("analyzedStackFiles", "").split(DELIMETER));
    }
   
    public void setCurrentParserConfig(String filename){
    	preferences.put("currentParserConfig", filename);
    }

    public String getCurrentParserConfig(){
    	return preferences.get("currentParserConfig", null);
    }
            
    public void setPreference(String name, String value){
    	preferences.put(name, value);    	
    }
    
    public String getPreference(String name, String defaultValue){
    	return preferences.get(name, defaultValue);
    }
    public void flush() {
        try {
        	preferences.flush();
        } catch (BackingStoreException ex) {
            ex.printStackTrace();
        }
    }

    private boolean hasInFiles(String file, String[] currentFiles) {       
        for(int i = 0; i < currentFiles.length; i++) {
           if(file.equals(currentFiles[i])) {
               return true;
           } 
        }
        return false;
    }
}
