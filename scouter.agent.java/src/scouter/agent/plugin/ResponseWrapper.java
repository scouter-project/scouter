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
package scouter.agent.plugin;

import java.io.PrintWriter;

public class ResponseWrapper {

	@SuppressWarnings("rawtypes")
	private static Class[] arg_c = {};
	private static Object[] arg_o = {};

	private Object resObject;
	

	private java.lang.reflect.Method getWriter;
	private java.lang.reflect.Method getContentType;
	private java.lang.reflect.Method getCharacterEncoding;

	private boolean enabled = true;

	public ResponseWrapper(Object res) {
		resObject = res;
	}

	public PrintWriter getWriter() {
		if (enabled == false)
			return null;
		try {
			if (getWriter == null) {
				getWriter = this.resObject.getClass().getMethod("getWriter", arg_c);
			}
			return (java.io.PrintWriter) getWriter.invoke(resObject, arg_o);
		} catch (Throwable e) {
			enabled = false;
			return null;
		}
	}

	public String getContentType() {
		if (enabled == false)
			return null;
		try {
			if (getContentType == null) {
				getContentType = this.resObject.getClass().getMethod("getContentType", arg_c);
			}
			return (String) getContentType.invoke(resObject, arg_o);
		} catch (Throwable e) {
			enabled = false;
			return null;
		}
	}

	public String getCharacterEncoding() {
		if (enabled == false)
			return null;
		try {
			if (getCharacterEncoding == null) {
				getCharacterEncoding = this.resObject.getClass().getMethod("getCharacterEncoding", arg_c);
			}
			return (String) getCharacterEncoding.invoke(resObject, arg_o);
		} catch (Throwable e) {
			enabled = false;
			return null;
		}
	}

}
