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

import scouter.agent.Logger;
import scouter.agent.proxy.IHttpTrace;

import java.util.Enumeration;
import java.util.Set;

public class WrRequestReactive extends WrRequest {
	private IHttpTrace http;
	private Object req;

	private boolean enabled = true;
	private Throwable _error = null;

	public WrRequestReactive(Object req, IHttpTrace http) {
		super(req);
	}
	public String getCookie(String key) {
		if (enabled == false)
			return null;
		try {
			return http.getCookie(req, key);
		} catch (Throwable e) {
			enabled = false;
			_error = e;
		}
		return null;
	}
	public String getRequestURI() {
		if (enabled == false)
			return null;
		try {
			return http.getRequestURI(req);
		} catch (Throwable e) {
			enabled = false;
			_error = e;
			Logger.println("A164", e);
			return null;
		}
	}
	public String getRemoteAddr() {
		if (enabled == false)
			return null;
		try {
			return http.getRemoteAddr(req);
		} catch (Throwable e) {
			enabled = false;
			_error = e;
			Logger.println("A165", e);
			return null;
		}
	}
	public String getMethod() {
		if (enabled == false)
			return null;
		try {
			return http.getMethod(req);
		} catch (Throwable e) {
			enabled = false;
			_error = e;
			Logger.println("A166", e);
			return null;
		}
	}
	public String getQueryString() {
		if (enabled == false)
			return null;
		try {
			return http.getQueryString(req);
		} catch (Throwable e) {
			enabled = false;
			_error = e;
			Logger.println("A167", e);
			return null;
		}
	}
	public String getParameter(String key) {
		if (enabled == false)
			return null;
		try {
			return http.getParameter(req, key);
		} catch (Throwable e) {
			enabled = false;
			_error = e;
			Logger.println("A168", e);
			return null;
		}
	}
	public Object getAttribute(String key) {
		if (enabled == false)
			return null;
		try {
			return http.getAttribute(req, key);
		} catch (Throwable e) {
			enabled = false;
			Logger.println("A908", e);
			return null;
		}
	}
	public String getHeader(String key) {
		if (enabled == false)
			return null;
		try {
			return http.getHeader(req, key);
		} catch (Throwable e) {
			enabled = false;
			_error = e;
			Logger.println("A169", e);
			return null;
		}
	}
	public Enumeration getParameterNames() {
		if (enabled == false)
			return null;
		try {
			return http.getParameterNames(req);
		} catch (Throwable e) {
			enabled = false;
			_error = e;
			Logger.println("A170", e);
			return null;
		}
	}
	public Enumeration getHeaderNames() {
		if (enabled == false)
			return null;
		try {
			return http.getHeaderNames(req);
		} catch (Throwable e) {
			enabled = false;
			_error = e;
			return null;
		}
	}
	public WrSession getSession() {
		if (enabled == false)
			return null;
		try {
			return null;
		} catch (Throwable e) {
			enabled = false;
			_error = e;
			Logger.println("A171", e);
			return null;
		}
	}
	public Set getSessionNames() {
		if (enabled == false)
			return null;
		try {
			return null;
		} catch (Throwable e) {
			enabled = false;
			_error = e;
			Logger.println("A909", e);
			return null;
		}
	}
	public Object getSessionAttribute(String key) {
		if (enabled == false)
			return null;
		try {
			return null;
		} catch (Throwable e) {
			enabled = false;
			_error = e;
			Logger.println("A172", e);
			return null;
		}
	}
	public Object inner() {
		return this.req;
	}
	public boolean isOk() {
		return enabled;
	}
	public Throwable error() {
		return _error;
	}
}
