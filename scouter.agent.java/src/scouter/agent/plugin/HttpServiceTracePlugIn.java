/*
 *  Copyright 2015 LG CNS.
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
import java.util.StringTokenizer;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.proxy.LoaderManager;
import scouter.agent.trace.TraceContext;
import scouter.lang.conf.ConfObserver;
import scouter.lang.pack.XLogPack;
import scouter.util.CompareUtil;
import scouter.util.StringUtil;
public class HttpServiceTracePlugIn {
	static IHttpService dummy = new IHttpService() {
		public boolean reject(TraceContext ctx, Object req, Object res) {
			return false;
		}
		public void start(TraceContext ctx, Object req, Object res) {
		}
		public void end(TraceContext ctx, XLogPack p) {
		}
	};
	static IHttpService plugIn = dummy;
	static String plugin_http_trace;
	private static void loadHttpService(boolean reload) {
		Configure conf = Configure.getInstance();
		if (reload || CompareUtil.equals(plugin_http_trace, conf.plugin_http_trace) == false) {
			plugin_http_trace = conf.plugin_http_trace;
			Logger.info("PLUG-IN: load httpservice-plugin");
			ClassLoader loader = LoaderManager.getPlugInLoader();
			if (loader == null) {
				plugIn = dummy;
				return;
			}
			if (StringUtil.isNotEmpty(conf.plugin_http_trace)) {
				try {
					Class c = Class.forName(conf.plugin_http_trace, false, loader);
					if (IHttpService.class.isAssignableFrom(c)) {
						plugIn = (IHttpService) c.newInstance();
						Logger.println("A131", c.getName());
					}
				} catch (Exception e) {
				}
			}
			if (plugIn == null) {
				plugIn = dummy;
			}
		}
	}
	private static String plugin_subcall_name;
	private static void loadApiCallName(boolean reload) {
		Configure conf = Configure.getInstance();
		if (reload || CompareUtil.equals(plugin_subcall_name, conf.plugin_apicall_name) == false) {
			plugin_subcall_name = conf.plugin_apicall_name;
			ApiCallTracePlugin.reinit();
			Logger.info("PLUG-IN: load subcall-plugin");
			ClassLoader loader = LoaderManager.getPlugInLoader();
			if (loader == null) {
				return;
			}
			if (StringUtil.isNotEmpty(conf.plugin_apicall_name)) {
				StringTokenizer nizer = new StringTokenizer(conf.plugin_apicall_name, ", ");
				while (nizer.hasMoreTokens()) {
					try {
						String name = StringUtil.trim(nizer.nextToken());
						Class c = Class.forName(name, false, loader);
						if (IApiCallTrace.class.isAssignableFrom(c)) {
							ApiCallTracePlugin.put((IApiCallTrace) c.newInstance());
						}
					} catch (Exception e) {
					}
				}
			}
		}
	}
	static {
		final Configure conf = Configure.getInstance();
		reloadClassLoader();
		loadHttpService(true);
		loadApiCallName(true);
		ConfObserver.add(HttpServiceTracePlugIn.class.getName(), new Runnable() {
			public void run() {
				boolean reload = reloadClassLoader();
				loadHttpService(reload);
				loadApiCallName(reload);
			}
		});
	}
	private static String plugin_classpath = null;
	private static boolean reloadClassLoader() {
		Configure conf = Configure.getInstance();
		if (CompareUtil.equals(plugin_classpath, conf.plugin_classpath) == false) {
			LoaderManager.createPlugInLoader(conf.plugin_classpath);
			plugin_classpath = conf.plugin_classpath;
			return true;
		}
		return false;
	}
	public static boolean reject(TraceContext ctx, Object req, Object res) {
		return plugIn.reject(ctx, req, res);
	}
	public static void start(TraceContext ctx, Object req, Object res) {
		plugIn.start(ctx, req, res);
	}
	public static void end(TraceContext ctx, XLogPack p) {
		plugIn.end(ctx, p);
	}
}
