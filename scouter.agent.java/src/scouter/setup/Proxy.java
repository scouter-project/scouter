/*
 *  Copyright 2015 the original author or authors.
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
package scouter.setup;

import java.lang.reflect.Method;

public class Proxy {

	static Object invoke(Class clazz, Object o, String methodName) throws Exception {
		Class[] arg_c = {};
		Object[] arg_o = {};

		Method method = clazz.getMethod(methodName, arg_c);
		return method.invoke(o, arg_o);
	}

	static Object attach(Class clazz, String pid) throws Exception {
		Class[] arg_c = { String.class };
		Object[] arg_o = { pid };

		Method method = clazz.getMethod("attach", arg_c);
		return method.invoke(null, arg_o);
	}

	static void loadagent(Object o, String agentJarPath) throws Exception {
		Class[] arg_c = { String.class };
		Object[] arg_o = { agentJarPath };

		Method method = o.getClass().getMethod("loadAgent", arg_c);
		method.invoke(o, arg_o);
	}
	static void loadagent(Object o, String agentJarPath, String opt) throws Exception {
		Class[] arg_c = { String.class, String.class };
		Object[] arg_o = { agentJarPath,opt };

		Method method = o.getClass().getMethod("loadAgent", arg_c);
		method.invoke(o, arg_o);
	}
}