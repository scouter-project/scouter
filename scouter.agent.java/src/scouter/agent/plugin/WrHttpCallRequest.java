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
import java.net.HttpURLConnection;
import scouter.agent.Logger;
import scouter.agent.proxy.IHttpClient;
import scouter.util.ObjectUtil;
public class WrHttpCallRequest extends Wrapper {
	protected static Class[] arg_c_ss = { String.class, String.class };
	private Object reqObject;
	private java.lang.reflect.Method addHeader;
	private HttpURLConnection urlCon;
	private IHttpClient httpclient;
    
	private boolean enabled = true;
	private Throwable _error;
	
	public WrHttpCallRequest(HttpURLConnection req) {
		this.urlCon = req;
	}
	public WrHttpCallRequest(Object req) {
		this.reqObject = req;
	}
	public WrHttpCallRequest(IHttpClient httpclient, Object req) {
		this.httpclient = httpclient;
		this.reqObject = req;
	}
	public void header(Object key, Object value) {
		if (enabled == false || key == null || value == null)
			return;
		try {
			if (this.urlCon != null) {
				this.urlCon.setRequestProperty(toString(key), toString(value));
				return;
			}
			if(this.httpclient!=null){
				this.httpclient.addHeader(reqObject, toString(key), toString(value));
				return;
			}
			if (addHeader == null) {
				addHeader = this.reqObject.getClass().getMethod("addHeader", arg_c_ss);
				addHeader.setAccessible(true);
			}
			addHeader.invoke(reqObject, new Object[] { toString(key), toString(value) });
		} catch (Throwable e) {
			enabled = false;
			_error = e;
			Logger.println("A900", e);
		}
	}
	private String toString(Object value) {
		String s = ObjectUtil.toString(value);
		if (s.length() > 80)
			return s.substring(0, 80);
		return s;
	}
	public Object inner() {
		return this.reqObject;
	}
	public boolean isOk() {
		return enabled;
	}
	public Throwable error() {
		return _error;
	}
}
