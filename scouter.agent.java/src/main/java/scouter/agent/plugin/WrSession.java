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
import scouter.agent.Logger;
public class WrSession extends Wrapper {
	private Object session;
	private java.lang.reflect.Method getAttribute;
	private java.lang.reflect.Method getAttributeNames;
	private boolean enabled = true;
	private Throwable _error;
	public WrSession(Object session) {
		this.session = session;
	}
	public Object getAttribute(String key) {
		if (enabled == false || session == null)
			return null;
		try {
			if (getAttribute == null) {
				getAttribute = this.session.getClass().getMethod("getAttribute", arg_c_s);
				getAttribute.setAccessible(true);
			}
			return getAttribute.invoke(session, new Object[] { key });
		} catch (Throwable e) {
			enabled = false;
			_error = e;
			Logger.println("A176", e);
			return null;
		}
	}
	public Enumeration getAttributeNames() {
		if (enabled == false || session == null)
			return null;
		try {
			if (getAttributeNames == null) {
				getAttributeNames = this.session.getClass().getMethod("getAttributeNames", arg_c);
				getAttributeNames.setAccessible(true);
			}
			return (Enumeration) getAttributeNames.invoke(session, arg_o);
		} catch (Throwable e) {
			enabled = false;
			_error = e;
			Logger.println("A177", e);
			return null;
		}
	}
	public Object inner() {
		return this.session;
	}
	public boolean isOk() {
		return enabled;
	}
	public Throwable error() {
		return _error;
	}
}
