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
package scouter.client.sorter;

import java.util.Comparator;
import java.util.Map;

import scouter.client.model.HierarchyObject;

public class ObjectTreeSorter implements Comparator<String> {
	
	Map<String, HierarchyObject> baseMap;
	
	public ObjectTreeSorter(Map<String, HierarchyObject> baseMap) {
		this.baseMap = baseMap;
	}
	
	public int compare(String s1, String s2) {
		HierarchyObject o1 = baseMap.get(s1);
		HierarchyObject o2 = baseMap.get(s2);
		return o1.getName().compareTo(o2.getName());
	}
}
