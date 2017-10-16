/*
 *  Copyright 2015 Scouter Project.
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

import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.ListValue;
import scouter.util.SystemUtil;
import scouter.util.ThreadUtil;

import java.io.PrintWriter;
import java.util.List;

public class ToolsMainFactory {
	private static final String TOOLS_MAIN = "scouter.xtra.tools.ToolsMain";

	public static MapPack heaphisto(Pack param) throws Throwable {
		
		MapPack m = new MapPack();
		if (SystemUtil.IS_JAVA_1_5) {
			m.put("error", "Not supported java version : " + SystemUtil.JAVA_VERSION);
			return m;
		}
		if (SystemUtil.JAVA_VENDOR.startsWith("IBM")) {
			m.put("error", "Not supported java vendor : " + SystemUtil.JAVA_VENDOR);
			return m;
		}
		ClassLoader loader = LoaderManager.getToolsLoader();
		if (loader == null) {
			return null;
		}
		if (SystemUtil.JAVA_VENDOR.startsWith("IBM")) {
			m.put("error", "Not supported java vendor : " + SystemUtil.JAVA_VENDOR);
			return m;
		}
		try {
			Class c = Class.forName(TOOLS_MAIN, true, loader);
			IToolsMain toolsMain = (IToolsMain) c.newInstance();
			List<String> out = toolsMain.heaphisto(0, 100000, "all");
			ListValue lv = m.newList("heaphisto");
			for (int i = 0; i < out.size(); i++) {
				lv.add(out.get(i));
			}
		} catch (Exception e) {
			m.put("error", e.getMessage());
		}
		return m;
	}

	public static void heaphisto(PrintWriter out) throws Throwable {
		
		if (SystemUtil.IS_JAVA_1_5) {
			return;
		}
		if (SystemUtil.JAVA_VENDOR.startsWith("IBM")) {
			return;
		}
		ClassLoader loader = LoaderManager.getToolsLoader();
		if (loader == null) {
			return;
		}
		try {
			Class c = Class.forName(TOOLS_MAIN, true, loader);
			IToolsMain toolsMain = (IToolsMain) c.newInstance();
			toolsMain.heaphisto(out);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    /**
     * get thread dump
     * @param param
     * @return
     * @throws Throwable
     */
    public static Pack threadDump(Pack param) throws Throwable {
		
		MapPack m = new MapPack();

        //Java 1.5 or IBM JDK
		if (SystemUtil.IS_JAVA_1_5||SystemUtil.JAVA_VENDOR.startsWith("IBM")) {
			List<String> out =  ThreadUtil.getThreadDumpList();
			ListValue lv = m.newList("threadDump");
			for (int i = 0; i < out.size(); i++) {
				lv.add(out.get(i));
			}
			return m;
		}

		ClassLoader loader = LoaderManager.getToolsLoader();
		if (loader == null) {
			List<String> out =  ThreadUtil.getThreadDumpList();
			ListValue lv = m.newList("threadDump");
			for (int i = 0; i < out.size(); i++) {
				lv.add(out.get(i));
			}
			return m;
		}
		
		try {
			Class c = Class.forName(TOOLS_MAIN, true, loader);
			IToolsMain toolsMain = (IToolsMain) c.newInstance();
			List<String> out = (List<String>) toolsMain.threadDump(0, 100000);
			ListValue lv = m.newList("threadDump");
			for (int i = 0; i < out.size(); i++) {
				lv.add(out.get(i));
			}
		} catch (Exception e) {
			m.put("error", e.getMessage());
		}
		return m;

	}

	public static boolean activeStack = false;

	public static synchronized void threadDump(PrintWriter out) throws Throwable {
		activeStack = true;
		if (SystemUtil.IS_JAVA_1_5 || SystemUtil.JAVA_VENDOR.startsWith("IBM")) {
			out.print(ThreadUtil.getThreadDump());
			return;
		}
		ClassLoader loader = LoaderManager.getToolsLoader();
		if (loader == null) {
			out.print(ThreadUtil.getThreadDump());
			return;
		}
		try {
			Class c = Class.forName(TOOLS_MAIN, true, loader);
			IToolsMain toolsMain = (IToolsMain) c.newInstance();
			toolsMain.threadDump(out);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}