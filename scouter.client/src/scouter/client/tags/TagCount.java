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
package scouter.client.tags;

import java.util.ArrayList;
import java.util.List;


public class TagCount {
	
	String tagName;
	String value;
	float count;
	List<TagCount> childs = null;
	
	protected synchronized void addChild(TagCount child) {
		if (childs == null) {
			childs = new ArrayList<TagCount>();
		}
		childs.add(child);
	}
	
	protected TagCount[] getChildArray() {
		if (childs == null) return new TagCount[0];
		return childs.toArray(new TagCount[childs.size()]);
	}
	
	protected int getChildSize() {
		if (childs == null) return 0;
		return childs.size();
	}

	@Override
	public String toString() {
		return "TagCount [tagName=" + tagName + ", value=" + value + ", count="
				+ count + ", childs size = " + (childs == null ? "null" : childs.size()) + "]";
	}
}
