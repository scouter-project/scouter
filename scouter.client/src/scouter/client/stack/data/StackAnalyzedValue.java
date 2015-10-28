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

import scouter.client.stack.utils.NumberUtils;

public class StackAnalyzedValue {
	private String m_value = null;
	private int m_count = 0;
	private int m_intPct = 0;
	private int m_extPct = 0;	
	
	public StackAnalyzedValue(){
		
	}
	public StackAnalyzedValue(String value, int count, int intPct, int extPct){
		m_value = value;
		m_count = count;
		m_intPct = intPct;
		m_extPct = extPct;
	}
	
	public String getValue(){
		return m_value;
	}
	
	public int getCount(){
		return m_count;
	}
	
	public int getIntPct(){
		return m_intPct;
	}
	
	public int getExtPct(){
		return m_extPct;
	}
	
	public void setValue(String value){
		m_value = value;
	}
	
	public void setCount(int value){
		m_count = value;
	}

	public void setIntPct(int value){
		m_intPct = value;
	}	

	public void setExtPct(int value){
		m_extPct = value;
	}
	
	public void addCount(){
		m_count++;
	}
	
	public String [] toTableInfo(){
		String [] info = new String[4];
		info[0] = new StringBuilder().append(m_count).toString();
		info[1] = new StringBuilder().append(NumberUtils.intToPercent(m_intPct)).append('%').toString();
		info[2] = new StringBuilder().append(NumberUtils.intToPercent(m_extPct)).append('%').toString();
		info[3] = m_value;
        return info;
	}
}
