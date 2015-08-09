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
package scouter.client.stack.base;

 import java.io.File;
import java.util.ArrayList;

import javax.swing.filechooser.FileFilter;
 
 public class SelectFileFilter extends FileFilter {
	 private String m_caption = null;
	 private ArrayList<String> m_searchList = null;
	 private int m_searchSize = 0;
	 
	 public SelectFileFilter(String caption, ArrayList<String> list){
		 m_caption = caption;
		 m_searchSize = 0;
		 
		 if(list == null){
			 return;
		 }
		 
		 m_searchSize = list.size();
		 if(m_searchSize == 0){
			 return;
		 }
		 m_searchList  = list;
	 }
	 
	public boolean accept(File f) {
		if (f.isDirectory() || m_searchList == null) {
			return true;
		}
		
		String extension = getFileExtension(f.getName());
		if(extension == null)
			return false;
		
		int i;
		extension = extension.toLowerCase();
		for(i = 0; i < m_searchSize; i++ ){
			if(m_searchList.get(i).equals(extension))
				return true;
		}
		return false;
	} 
	public String getDescription() {
		return m_caption; 
	}
	
	public String getFileExtension(String filename){
		for(int i=filename.length()-1; i >= 0; i--){
			if( filename.charAt(i) == '.'){
				return filename.substring(i+1);
			}
		}
		return null;
	}
 }

