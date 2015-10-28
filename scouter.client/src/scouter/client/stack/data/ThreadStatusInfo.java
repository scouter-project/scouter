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

public class ThreadStatusInfo {
	ArrayList<Integer> m_value = null;
	
	public ThreadStatusInfo(){
		m_value = new ArrayList<Integer>();
	}
	
	public void checkStatusCount(ArrayList<String> statusList, int index, String line) {
		String newLine = line.substring(index);
		if(newLine == null || newLine.length() == 0)
			return;

		String status = null;
		int start = -1;
		int end = -1;
		int inx = 0;
		int length = newLine.length();
		char ch;
		
		while(inx < length ){
			ch = newLine.charAt(inx);
			if(ch >= 0x21 && ch <= 0x7E){   // !(33 - 0x21) ~ ~(126 - 0x7E)
				if(start == -1){
					start = inx;
				}
			}else{
				if(start >=0){
					end = inx;
					break;
				}
			}
			inx++;
		}
		if(end == -1 && start >= 0){
			end = inx;
		}
		
		if(end > start){
			status = newLine.substring(start, end);
		}
		
		if(status == null)
			return;
		
		inx = -1;
		start = 0;
		int size = statusList.size();
		while(start < size){
			if(status.equals(statusList.get(start))){
				inx = start;
				break;
			}
			start++;
		}
		
		if(m_value.size() == 0 && size > 0){
			for(int i = 0; i < size; i++){
				m_value.add(new Integer(0));
			}			
		}

		if(inx == -1){
			statusList.add(status);
			m_value.add(new Integer(1));
			inx = statusList.size() - 1;
			return;
		}
		
		m_value.set(inx, new Integer(m_value.get(inx).intValue() + 1));
	}
	
	public int getValue(int inx){
		if(inx >= m_value.size()){
			return 0;
		}
		
		return m_value.get(inx).intValue();
	}
	
	public int geSize(){
		return m_value.size();
	}
}
