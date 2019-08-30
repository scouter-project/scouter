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

import scouter.util.ObjectUtil;
import scouter.util.StringKeyLinkedMap;

public class Counter implements Comparable<Counter> {
	public static final int MIN_NORMALIZE_SEC = 4;
	public static final int MAX_NORMALIZE_SEC = 60;

	private String name;
	private String displayName;
	private String unit = "";
	private String icon = "";
	private boolean all = true;
	private boolean total = true;

	private StringKeyLinkedMap<String> attrMap = new StringKeyLinkedMap<String>();

	public Counter() {}

	public Counter(String name) {
		this.name = name;
	}
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
	public String getUnit() {
		return unit;
	}
	public void setUnit(String unit) {
		this.unit = unit;
	}
	public String getIcon() {
		return icon;
	}
	public void setIcon(String icon) {
		this.icon = icon;
	}
	public boolean isAll() {
		return all;
	}
	public void setAll(boolean all) {
		this.all = all;
	}
	public boolean isTotal() {
		return total;
	}
	public void setTotal(boolean total) {
		this.total = total;
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

	public boolean someContentsEquals(Counter other) {
		if (other == null)
			return false;

		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name)) {
			return false;
		}

		if (displayName == null) {
			if (other.displayName != null)
				return false;
		} else if (!displayName.equals(other.displayName)) {
			return false;
		}

		if (unit == null) {
			if (other.unit != null)
				return false;
		} else if (!unit.equals(other.unit)) {
			return false;
		}

		if (total != other.total) {
			return false;
		}

		return true;
	}

	public boolean semanticEquals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Counter counter = (Counter) o;
		return total == counter.total &&
				ObjectUtil.objectEquals(name, counter.name) &&
				ObjectUtil.objectEquals(displayName, counter.displayName) &&
				ObjectUtil.objectEquals(unit, counter.unit) &&
				ObjectUtil.objectEquals(icon, counter.icon);
	}


	public int compareTo(Counter o) {
		return this.name.compareTo(o.getName());
	}

	public Counter clone() {
		Counter clone = new Counter();
		clone.name = this.name;
		clone.displayName = this.displayName;
		clone.unit = this.unit;
		clone.icon = this.icon;
		clone.all = this.all;
		clone.total = this.total;
		//TODO deep clone
		clone.attrMap = this.attrMap;

		return clone;
	}


}