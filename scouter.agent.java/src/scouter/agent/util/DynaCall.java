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
package scouter.agent.util;

import java.lang.reflect.Method;

public class DynaCall {
	boolean enable = true;
	Class dynaClass;
	Method method;

	public DynaCall(Object o, String name, Class... arg) {
		try {
			this.dynaClass = o.getClass();
			this.method = dynaClass.getMethod(name, arg);
		} catch (Exception e) {
			enable = false;
		}
	}

	public boolean isEnabled(){
		return enable;
	}
	public void disabled(){
		this.enable=false;
	}
	public Object call(Object o , Object ... args) throws Exception{
		return method.invoke(o, args);
	}
}
