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

import scouter.client.stack.utils.StringUtils;

public class UniqueStackValue extends StackAnalyzedValue {
	private ArrayList<String> m_stack = null;
	
	public UniqueStackValue(String value, int count, int intPct, int extPct){
		super(value, count, intPct, extPct);		
	}

	public UniqueStackValue(ArrayList<String> list){
		setValue(StringUtils.makeStackValue(list.get(0), false));
		setStack(list);
	}
	
	public void setValues(int count, int intPct, int extPct){
		setCount(count);
		setIntPct(intPct);
		setExtPct(extPct);
	}
	
	public ArrayList<String> getStack(){
		return m_stack;
	}
	
	public void setStack( ArrayList<String> list){
		m_stack = list;
	}
}
