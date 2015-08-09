/*
 *  Copyright 2015 LG CNS.
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
package scouter.client.stack.utils;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;

public class StringUtils {
	static public boolean checkExist(String line, ArrayList<String> list){
		if(line == null || list == null)
			return false;
		
		int size = list.size();
		for(int i = 0; i < size; i++){
			if(line.indexOf((String)list.get(i)) >= 0) return true;
		}
		return false;
	}
	
	static public boolean isLockStack(String line){
		if(line.indexOf(" waiting ") >=0 || line.indexOf(" locked ") >=0 || line.indexOf(" parking ") >= 0 ){
			return true;
		}
		
		return false;
	}

	static public String makeSimpleLine(String input, boolean isFullFunction){
		int index = input.indexOf("at ");
		if(index >=0){
			if(isFullFunction){
				return input.substring(index+2);
			}else{
				int end = input.indexOf('(');
				if((index +2 )< end ){
					return input.substring(index+2, end);						
				}else{
					return input.substring(index+2);					
				}
			}
		}
		return input;
	}
	
	static public String getFilename(String fullPath){
		if(fullPath == null)
			return null;
		
		int index = fullPath.length()-1;
		char ch;
		while(index >= 0){
			ch = fullPath.charAt(index); 
			if( ch == '\\' || ch == '/'){
				break;
			}
			index--;
		}
			
		if(index < 0){
			return fullPath;
		}
		
		return fullPath.substring(index+1);
	}

	static public String getDiretory(String fullPath){
		if(fullPath == null)
			return null;
		
		int index = fullPath.length()-1;
		char ch;
		while(index >= 0){
			ch = fullPath.charAt(index); 
			if( ch == '\\' || ch == '/'){
				break;
			}
			index--;
		}
			
		if(index < 0){
			return null;
		}
		
		return fullPath.substring(0, index);
	}	
	
	static public void setClipboard(String contents){
		StringSelection ss = new StringSelection(contents);
		Clipboard clboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clboard.setContents(ss, null);
	}
}
