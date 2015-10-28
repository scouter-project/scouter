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
package scouter.agent.proxy;
import scouter.agent.Logger;
import scouter.agent.trace.TraceContext;
public class NettyHttpClientFactory {
	private static final String CLIENT = "scouter.xtra.httpclient.NettyHttpClient";
	public static final IHttpClient dummy = new IHttpClient() {
		public String getURI(Object o) {
			return null;
		}
		public String getHost(Object o) {
			return null;
		}
		public String getHeader(Object o, String key) {
			return null;
		}
		public void addHeader(Object o, String key, String value) {
		}
	};
	public static IHttpClient create(ClassLoader parent) {
		try {
			ClassLoader loader = LoaderManager.getHttpClient(parent);
			if (loader == null) {
				return dummy;
			}
			Class c = Class.forName(CLIENT, true, loader);
			return (IHttpClient) c.newInstance();
		} catch (Throwable e) {
			e.printStackTrace();
			Logger.println("A140", "fail to create", e);
			return dummy;
		}
	}
}
