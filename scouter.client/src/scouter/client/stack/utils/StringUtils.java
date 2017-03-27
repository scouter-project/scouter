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
package scouter.client.stack.utils;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;

public class StringUtils {
	static private final String STARTSTACK = "at ";
	
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

	static public String makeSimpleLine(String input, boolean isRemoveLine){
		int index = input.indexOf(STARTSTACK);
		if(index >=0){
			if(isRemoveLine){
				int end = input.indexOf('(');
				if((index +2 )< end ){
					return input.substring(index+2, end);						
				}else{
					return input.substring(index+2);					
				}
			}else{
				return input.substring(index+2);
			}
		}
		return input;
	}
	
	static public ArrayList<String> makeStackToSimpe(ArrayList<String> list, int stackStartLine, ArrayList<String> singleList){
		ArrayList<String> simpleList = new ArrayList<String>();
		if(list == null || list.size() < stackStartLine){
			return simpleList;
		}
		if(singleList == null || singleList.size() == 0){
			return list;
		}
		
		String line;
		int ii;
		int size = list.size();
		int singleSize = singleList.size();
		int startInx = 0;
		for(int i = stackStartLine; i < size; i++){
			line = list.get(i);
			for(ii = 0; ii < singleSize; ii++){
				if(line.indexOf(singleList.get(ii)) >= 0){
					startInx = line.indexOf(STARTSTACK);
					if(startInx >= 0){
						line = new StringBuilder(50).append(line.subSequence(0, startInx + 5)).append(singleList.get(ii)).append("(modified stack)").toString();
						
					}else{
						startInx = line.indexOf('-');
						if(startInx >=0){
							line = new StringBuilder(50).append(line.subSequence(0, startInx)).append(singleList.get(ii)).append("(modified stack)").toString();
						}else{
							line = new StringBuilder(50).append(singleList.get(ii)).append("(modified stack)").toString();							
						}
					}
				}
			}
			simpleList.add(line);
		}
		
		return simpleList;
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
	
	static public int hashCode(ArrayList<String> list){
		int hashCode = 0;
		for(int i = 0; i < list.size(); i++){
			hashCode +=  list.get(i).hashCode();
		}
		return hashCode;
	}
	
	static public String makeStackValue(String value, boolean isRemoveLine){
        int sIndex = getStartIindex(value);
        if(sIndex < 0){
        	throw new RuntimeException(value + " is not stack!");
        }
        int eIndex = 0;
        if(isRemoveLine){
        	eIndex = value.indexOf('(');
        }
        
        if(eIndex > 0){
        	return value.substring(sIndex, eIndex);
        }else{
        	return value.substring(sIndex);                	
        }		
	}
	
	static int getStartIindex(String line){
		int sIndex = line.indexOf(STARTSTACK);
       	if(sIndex >=0){
       		return sIndex + 3;
       	}
       	
		int length = line.length();
		char ch;
		for(int i = 0; i < length; i++){
			ch = line.charAt(i);
			if(ch != ' ' && ch != '\t'){
				return i;
			}
		}
		return -1;
	}
}
