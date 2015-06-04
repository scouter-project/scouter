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
 */

package scouter.lang;

import java.util.Enumeration;

import scouter.util.StringKeyLinkedMap;

public class Family {
	private String name;
	private String master;
	private StringKeyLinkedMap<String> attrMap = new StringKeyLinkedMap<String>();
	private StringKeyLinkedMap<Counter> counterMap = new StringKeyLinkedMap<Counter>();
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getMaster() {
		return master;
	}
	public void setMaster(String master) {
		this.master = master;
	}
	
	public String setAttribute(String key, String value) {
		return attrMap.put(key, value);
	}
	
	public String getAttribute(String key) {
		return attrMap.get(key);
	}
	
	public boolean isTrueAttribute(String key) {
		String value = attrMap.get(key);
		if (value == null) {
			return false;
		}
		return Boolean.valueOf(value);
	}
	
	public Counter addCounter(Counter counter) {
		return counterMap.put(counter.getName(), counter);
	}
	
	public Counter getCounter(String counter) {
		return counterMap.get(counter);
	}
	
	public Counter[] listCounters() {
		Counter[] array= new Counter[counterMap.size()];
		Enumeration<Counter> en = counterMap.values();
		for(int i = 0 ; i < array.length;i++)
			array[i] = en.nextElement();
		return array;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Family other = (Family) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}