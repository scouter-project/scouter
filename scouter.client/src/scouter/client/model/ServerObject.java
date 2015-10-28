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
package scouter.client.model;

import scouter.util.StringUtil;

public class ServerObject extends HierarchyObject {
	
	private int id;
	private String name;
	private long usedMemory;
	private long totalMemory;
	
	public ServerObject(int id, String name) {
		this.id = id;
		this.name = name;
	}
	
	public int getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}

	public void setUsedMemory(long used) {
		this.usedMemory = used;
	}
	
	public long getUsedMemory() {
		return usedMemory;
	}
	
	public long getTotalMemory() {
		return totalMemory;
	}

	public void setTotalMemory(long totalMemory) {
		this.totalMemory = totalMemory;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}
	
		@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ServerObject other = (ServerObject) obj;
		if (StringUtil.isEmpty(name)) {
			if (StringUtil.isEmpty(other.getName())) {
				return true;
			} else {
				return false;
			}
		} else {
			return StringUtil.trim(name).equals(StringUtil.trim(other.getName()));
		}
	}

	public String toString() {
		return "ServerObject [name=" + name + "]" + keySet();
	}

	public String getDisplayName() {
		return name;
	}
}
