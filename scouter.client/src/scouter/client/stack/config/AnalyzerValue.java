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
package scouter.client.stack.config;

import java.util.ArrayList;

public class AnalyzerValue {
	public static final int FILTER_ALL = 1;
	public static final int FILTER_EACH = 2;

	public static final int READER_FIRST = 1;
	public static final int READER_LAST = 2;
	public static final int READER_NEXT = 3;
	
	private String m_name = null;
	private String m_extension = null;
	private int m_filter = 0;
	private int m_reader = 0;
	
	ArrayList<String> m_list = null;
	ArrayList<String> m_listMain = null;
	
	public String getName(){
		return m_name;
	}
	public String getExtension(){
		return m_extension;
	}
	public int getFilter(){
		return m_filter;
	}
	public int getReader(){
		return m_reader;
	}
	
	public void setName(String name){
		m_name = name;
	}
	
	public void setExtension(String extension){
		m_extension = extension;
	}
	
	public void setFilter(int filter){
		m_filter = filter;
	}
	
	public void setReader(int reader){
		m_reader = reader;
	}	

	public void setList(ArrayList<String> list){
		m_list = list;
	}	

	public void setListMain(ArrayList<String> list){
		m_listMain = list;
	}	
	
	public ArrayList<String> getList(){
		return m_list;
	}	

	public ArrayList<String> getListMain(){
		return m_listMain;
	}	
	
	public void setFilter(String filter){
		if(filter == null || filter.length() == 0)
			throw new RuntimeException("analyzeStack filter attribute is null");
		
		if("ALL".equals(filter.toUpperCase()))
			m_filter = FILTER_ALL;
		else if("EACH".equals(filter.toUpperCase()))
			m_filter = FILTER_EACH;
		else
			throw new RuntimeException("analyzeStack filter attribute must in (all,each) - " + filter);
	}
	
	public void setReader(String reader){
		if(reader == null || reader.length() == 0)
			throw new RuntimeException("analyzeStack reader attribute is null");
		
		if("FIRST".equals(reader.toUpperCase()))
			m_reader = READER_FIRST;
		else if("LAST".equals(reader.toUpperCase()))
			m_reader = READER_LAST;
		else if("NEXT".equals(reader.toUpperCase()))
			m_reader = READER_NEXT;
		else
			throw new RuntimeException("analyzeStack reader attribute must in (first,last,next) - " + reader);
	}	

	public boolean isValid(){
		if(m_name == null || m_name.length() == 0)
			return false;

		if(m_extension == null || m_extension.length() == 0)
			return false;
		
		String search = "[" + m_extension + "]";
		if("[LOG] [SVC] [TOP] [SQL] [WS]".indexOf(search)>=0)
			return false;
		
		
		if(m_filter <= 0 || m_filter > 2)
			return false;
		
		if(m_reader <= 0 || m_filter > 3)
			return false;

		if(m_list == null || m_list.size() == 0)
			return false;
		
		if(m_filter == FILTER_ALL && (m_listMain == null || m_listMain.size() == 0))
			return false;
		
		return true;
	}
}
