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
package scouter.agent.trace;

public class HookReturn {
	public String class1;
	public String method;
	public String desc;
	public Object this1;
	public Object return1;

	public HookReturn(String className, String methodName, String methodDesc, Object this1, Object return1) {
		this.class1 = className;
		this.method = methodName;
		this.desc = methodDesc;
		this.this1 = this1;
		this.return1 = return1;
	}
    public String getClassName(){
    	return this.class1;
    }
    public String getMethodName(){
    	return this.method;
    }
    public String getMethodDesc(){
    	return this.desc;
    }
    public Object getThis(){
    	return this.this1;
    }
    public Object getReturn(){
    	return this.return1;
    }
}