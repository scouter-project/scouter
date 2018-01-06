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
 */

package scouter.lang;

import scouter.util.StringKeyLinkedMap;

import java.util.Enumeration;
import java.util.List;

public class ObjectType {
	private String name;
	private String displayName;
	private Family family;
	private String icon;
	private boolean subObject;
	private StringKeyLinkedMap<Counter> counterMap = new StringKeyLinkedMap<Counter>();
	private StringKeyLinkedMap<String> attrMap = new StringKeyLinkedMap<String>();
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public Family getFamily() {
		return family;
	}
	public void setFamily(Family family) {
		this.family = family;
	}
	
	public String getIcon() {
		return icon;
	}
	public void setIcon(String icon) {
		this.icon = icon;
	}
	
	public boolean isSubObject() {
		return this.subObject;
	}
	
	public void setSubObject(boolean isSubObject) {
		this.subObject = isSubObject;
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
	
	public Counter[] listCounters() {
		if(family == null) return null;
		List<Counter> list = family.listCounters();
		Enumeration<Counter> en = counterMap.values();
		while (en.hasMoreElements()) {
			list.add(en.nextElement());
		}
		return list.toArray(new Counter[list.size()]);
	}
	
	public Counter[] listObjectTypeCounters() {
		Counter[] counters = new Counter[counterMap.size()];
		Enumeration<Counter> en = counterMap.values();
		int i = 0;
		while (en.hasMoreElements()) {
			counters[i] = en.nextElement();
			i++;
		}
		return counters;
	}
	
	public Counter getCounter(String name) {
		Counter c = counterMap.get(name);
		if (c == null) {
			c = family.getCounter(name);
		}
		return c;
	}
	
	public void addCounter(Counter c) {
		this.counterMap.put(c.getName(), c);
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
		ObjectType other = (ObjectType) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
