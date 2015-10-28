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

public class ValueObject {
	private String m_key = null;
	private int m_value = 0;
	
	public ValueObject(){
		
	}
	public ValueObject(String key, int value){
		m_key = key;
		m_value = value;
	}
	
	public void setKey(String key){
		m_key = key;
	}
	
	public void setValue(int value){
		m_value = value;
	}

	public String getKey(){
		return m_key;
	}
	
	public int getValue(){
		return m_value;
	}
	
}
