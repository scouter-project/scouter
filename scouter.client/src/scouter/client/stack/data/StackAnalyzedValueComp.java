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
import java.util.Collections;
import java.util.Comparator;

public class StackAnalyzedValueComp  implements Comparator<StackAnalyzedValue>{
	private boolean m_isCount;

	public StackAnalyzedValueComp(){
		m_isCount = true;
	}
	
	public StackAnalyzedValueComp(boolean isCount){
		m_isCount = isCount;
	}

	public int compare(StackAnalyzedValue o1, StackAnalyzedValue o2) {
		if(m_isCount){
			if(o1.getCount() > o2.getCount())
				return -1;
			else if(o1.getCount() < o2.getCount())
				return 1;
			
			return 0;
		}else{
			return o1.getValue().compareTo(o2.getValue());
		}
	}

	public static ArrayList<StackAnalyzedValue> sortClone(ArrayList<StackAnalyzedValue> list, boolean isCount){
		int size = list.size();
		ArrayList<StackAnalyzedValue> target = new ArrayList<StackAnalyzedValue>(size);
		for(int i = 0; i < size; i++){
			target.add(list.get(i));
		}
		Collections.sort(target, new StackAnalyzedValueComp(isCount));
		return target;
	}
}
