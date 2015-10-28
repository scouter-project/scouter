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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import scouter.client.sorter.ObjectTreeSorter;

public abstract class HierarchyObject {
	
	protected Map<String, HierarchyObject> childMap = new HashMap<String, HierarchyObject>();
	private TreeMap<String, HierarchyObject> sortedMap = new TreeMap<String, HierarchyObject>(new ObjectTreeSorter(childMap));
	private HierarchyObject parent;
	
	public HierarchyObject getChild(String key) {
		return this.childMap.get(key);
	}
	
	public HierarchyObject putChild(String key, HierarchyObject child) {
		this.childMap.put(key, child);
		return this.sortedMap.put(key, child);
	}
	
	public void setParent(HierarchyObject obj) {
		this.parent = obj;
	}
	
	public HierarchyObject getParent() {
		return parent;
	}
	
	public int getChildSize() {
		return this.childMap.size();
	}
	
	public Set<String> keySet() {
		return sortedMap.keySet();
	}
	
	public Object[] getSortedChildArray() {
		return sortedMap.values().toArray();
	}
	
	public abstract String getName();
	public abstract String getDisplayName();
	
	public String toString() {
		return getName() ;
	}

	public static void main(String[] args) {
		HashMap<String, HierarchyObject> childMap = new HashMap<String, HierarchyObject>();
		 TreeMap<String, HierarchyObject> sortedSet = new TreeMap<String, HierarchyObject>(new ObjectTreeSorter(childMap));
		 
		 DummyObject obj1 = new DummyObject("kim");
		 DummyObject obj2 = new DummyObject("lee");
		 
		 System.out.println(childMap.put("abc", obj1));
		 System.out.println(childMap.put("abc", obj2));
		 System.out.println(sortedSet.put("abc", obj1));
		 System.out.println(sortedSet.put("abc", obj2));
		 System.out.println(childMap.size());
		 System.out.println(sortedSet.size());
	}
}
