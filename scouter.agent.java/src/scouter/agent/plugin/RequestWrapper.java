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

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

public class RequestWrapper {

	@SuppressWarnings("rawtypes")
	private static Class[] arg_c = {};
	private static Object[] arg_o = {};

	private static Class[] arg_c_s = { String.class };
	private static Class[] arg_c_z = { Boolean.TYPE };

	private Object reqObject;

	private java.lang.reflect.Method getRequestURI;
	private java.lang.reflect.Method getRemoteAddr;
	private java.lang.reflect.Method getMethod;
	private java.lang.reflect.Method getParameterNames;
	private java.lang.reflect.Method getParameter;
	private java.lang.reflect.Method getHeaderNames;
	private java.lang.reflect.Method getHeader;
	private java.lang.reflect.Method getQueryString;
	private java.lang.reflect.Method getSession;
	private java.lang.reflect.Method getCookies;
	private java.lang.reflect.Method getName;
	private java.lang.reflect.Method getValue;

	private boolean enabled = true;

	public RequestWrapper(Object req) {
		reqObject = req;
		HttpServletRequest r;
	}

	public String getCookie(String key) {
		if (enabled == false)
			return null;
		try {
			if (getCookies == null) {
				getCookies = this.reqObject.getClass().getMethod("getCookies", arg_c);
			}
			Object[] c = (Object[]) getCookies.invoke(reqObject, arg_o);
			if (c == null && c.length == 0)
				return null;
			if (getName == null) {
				getName = c[0].getClass().getMethod("getName", arg_c);
			}
			if (getValue == null) {
				getValue = c[0].getClass().getMethod("getValue", arg_c);
			}
			for (int i = 0; i < c.length; i++) {
				if (key.equals(getName.invoke(c[i], arg_o))) {
					return (String) getValue.invoke(c[i], arg_o);
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
			enabled = false;
		}
		return null;
	}

	public String getRequestURI() {
		if (enabled == false)
			return null;
		try {
			if (getRequestURI == null) {
				getRequestURI = this.reqObject.getClass().getMethod("getRequestURI", arg_c);
			}
			return (String) getRequestURI.invoke(reqObject, arg_o);
		} catch (Throwable e) {
			enabled = false;
			return null;
		}
	}

	public String getRemoteAddr() {
		if (enabled == false)
			return null;
		try {
			if (getRemoteAddr == null) {
				getRemoteAddr = this.reqObject.getClass().getMethod("getRemoteAddr", arg_c);
			}
			return (String) getRemoteAddr.invoke(reqObject, arg_o);
		} catch (Throwable e) {
			enabled = false;
			return null;
		}
	}

	public String getMethod() {
		if (enabled == false)
			return null;
		try {
			if (getMethod == null) {
				getMethod = this.reqObject.getClass().getMethod("getMethod", arg_c);
			}
			return (String) getMethod.invoke(reqObject, arg_o);
		} catch (Throwable e) {
			enabled = false;
			return null;
		}
	}

	public String getQueryString() {
		if (enabled == false)
			return null;
		try {
			if (getQueryString == null) {
				getQueryString = this.reqObject.getClass().getMethod("getQueryString", arg_c);
			}
			return (String) getQueryString.invoke(reqObject, arg_o);
		} catch (Throwable e) {
			enabled = false;
			return null;
		}
	}

	public String getParameter(String key) {
		if (enabled == false)
			return null;
		try {
			if (getParameter == null) {
				getParameter = this.reqObject.getClass().getMethod("getParameter", arg_c_s);
			}
			return (String) getParameter.invoke(reqObject, new Object[] { key });
		} catch (Throwable e) {
			enabled = false;
			return null;
		}
	}

	public String getHeader(String key) {
		if (enabled == false)
			return null;
		try {
			if (getHeader == null) {
				getHeader = this.reqObject.getClass().getMethod("getHeader", arg_c_s);
			}
			return (String) getHeader.invoke(reqObject, new Object[] { key });
		} catch (Throwable e) {
			enabled = false;
			return null;
		}
	}

	public Enumeration getParameterNames() {
		if (enabled == false)
			return null;
		try {
			if (getParameterNames == null) {
				getParameterNames = this.reqObject.getClass().getMethod("getParameterNames", arg_c);
			}
			return (Enumeration) getParameterNames.invoke(reqObject, arg_o);
		} catch (Throwable e) {
			enabled = false;
			return null;
		}
	}

	public Enumeration getHeaderNames() {
		if (enabled == false)
			return null;
		try {
			if (getHeaderNames == null) {
				getHeaderNames = this.reqObject.getClass().getMethod("getHeaderNames", arg_c);
			}
			return (Enumeration) getHeaderNames.invoke(reqObject, arg_o);
		} catch (Throwable e) {
			enabled = false;
			return null;
		}
	}

	public SessionWrapper getSession() {
		if (enabled == false)
			return null;
		try {
			if (getSession == null) {
				getSession = this.reqObject.getClass().getMethod("getSession", arg_c_z);
			}
			Object o = getSession.invoke(reqObject, new Object[] { false });
			return new SessionWrapper(o);
		} catch (Throwable e) {
			enabled = false;
			return null;
		}
	}
}
