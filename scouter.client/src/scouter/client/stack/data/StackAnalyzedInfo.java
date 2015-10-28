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
package scouter.client.stack.data;

import java.util.ArrayList;

import scouter.client.stack.utils.NumberUtils;


@SuppressWarnings("serial")
public class StackAnalyzedInfo extends AbstractInfo{
	private String m_analyzedName = null;
	private int m_totalCount = 0;
	private ArrayList<StackAnalyzedValue> m_analyzedList = null;
	private StackFileInfo m_stackFileInfo = null;
	private String m_extension = null;
	
	
    public StackAnalyzedInfo(String value, StackFileInfo stackFileInfo, String extension) {
    	m_analyzedName = value;
    	m_stackFileInfo = stackFileInfo;
        setName("StackAnalyzed");
        m_extension = extension;
    }
    
    public String getName() {
        return m_analyzedName;
    }

    public void setTotalCount(int value){
    	m_totalCount = value;
    }
    
    public int getTotalcount(){
    	return m_totalCount;
    }
    
    public ArrayList<StackAnalyzedValue> getAnalyzedList(){
    	return m_analyzedList;
    }
    
    public String getExtension(){
    	return m_extension;
    }
    
    public void setAnaylizedList(ArrayList<StackAnalyzedValue> value){
    	m_analyzedList = value;
    }
    
	public String toString() {
		StringBuilder buffer = new StringBuilder(100);
		if(m_stackFileInfo.getTotalWorkingCount() > 0){
			buffer.append(m_analyzedName).append(" - ").append(m_totalCount).append(" (").append(NumberUtils.intToPercent((10000*m_totalCount)/m_stackFileInfo.getTotalWorkingCount())).append("%)");
		} else{
			buffer.append(m_analyzedName).append(" - ").append(m_totalCount).append(" (0%)");			
		}
        return buffer.toString();
	}
	
	public String [] toTreeInfo() {
		String [] info = new String[3];
		info[0] = m_analyzedName;
		info[1] = new StringBuilder().append(m_totalCount).toString();
		if(m_stackFileInfo.getTotalWorkingCount() > 0){
			info[2] = new StringBuilder().append(NumberUtils.intToPercent((10000*m_totalCount)/m_stackFileInfo.getTotalWorkingCount())).append('%').toString();
		} else{
			info[2] = "0%";		
		}
        return info;
	}	
	
	public StackFileInfo getStackFileInfo(){
		return m_stackFileInfo;
	}
}
