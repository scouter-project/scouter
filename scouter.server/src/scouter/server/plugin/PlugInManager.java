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
package scouter.server.plugin;

import scouter.lang.pack.*;
import scouter.lang.plugin.PluginConstants;
import scouter.server.plugin.builtin.BuiltInPluginManager;

public class PlugInManager {

	static IXLog xlog;
	static IXLog xlogdb;
	static IXLogProfile xlogProfiles;
	static IObject objects;
	static IAlert alerts;
	static ICounter counters;
	static ISummary summary;

	public static void xlog(XLogPack m) {
        BuiltInPluginManager.invokeAllPlugins(PluginConstants.PLUGIN_SERVER_XLOG, m);

		if (xlog != null) {
			try {
				xlog.process(m);
			} catch (Throwable t) {
			}
		}
	}

	public static void xlogdb(XLogPack m) {
		if (xlogdb != null) {
			try {
				xlogdb.process(m);
			} catch (Throwable t) {
			}
		}
	}

	public static void profile(XLogProfilePack m) {
        BuiltInPluginManager.invokeAllPlugins(PluginConstants.PLUGIN_SERVER_PROFILE, m);

		if (xlogProfiles != null) {
			try {
				xlogProfiles.process(m);
			} catch (Throwable t) {
			}
		}

	}

	public static void active(ObjectPack p) {
        BuiltInPluginManager.invokeAllPlugins(PluginConstants.PLUGIN_SERVER_OBJECT, p);

		if (objects != null) {
			try {
				objects.process(p);
			} catch (Throwable t) {
			}
		}

	}

	public static void alert(AlertPack p) {
        BuiltInPluginManager.invokeAllPlugins(PluginConstants.PLUGIN_SERVER_ALERT, p);

		if (alerts != null) {
			try {
				alerts.process(p);
			} catch (Throwable t) {
			}
		}
	}

	public static void counter(PerfCounterPack p) {
		BuiltInPluginManager.invokeAllPlugins(PluginConstants.PLUGIN_SERVER_COUNTER, p);

		if (counters != null) {
			try {
				counters.process(p);
			} catch (Throwable t) {
			}
		}
	}

	public static void summary(SummaryPack p) {
        BuiltInPluginManager.invokeAllPlugins(PluginConstants.PLUGIN_SERVER_SUMMARY, p);

		if (summary != null) {
			try {
				summary.process(p);
			} catch (Throwable t) {
			}
		}
	}

	public static void load() {
		PlugInLoader.getInstance();
	}
}
