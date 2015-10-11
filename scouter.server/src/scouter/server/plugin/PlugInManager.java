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
package scouter.server.plugin;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import scouter.lang.pack.AlertPack;
import scouter.lang.pack.ObjectPack;
import scouter.lang.pack.PerfCounterPack;
import scouter.lang.pack.XLogPack;
import scouter.lang.pack.XLogProfilePack;
import scouter.server.ConfObserver;
import scouter.server.Configure;
import scouter.server.Logger;
import scouter.util.CompareUtil;
import scouter.util.StringUtil;
import scouter.util.scan.Scanner;
public class PlugInManager {
	private static List<IServiceGroup> groups = new ArrayList();
	private static List<IXLog> xlogs = new ArrayList();
	private static List<IXLogProfile> xlogProfiles = new ArrayList();
	private static List<IObject> objects = new ArrayList();
	private static List<IAlert> alerts = new ArrayList();
	private static List<ICounter> counters = new ArrayList();
	private static void load(String path) {
		ClassLoader loader = createClassLoader(path);
		if (loader == null) {
			return;
		}
		Set<String> classes = new HashSet<String>();
		List<File> files = getJarNames(path);
		for (int i = 0; i < files.size(); i++) {
			Set<String> one = Scanner.getClassesInJar(files.get(i).getAbsolutePath());
			classes.addAll(one);
		}
		Iterator<String> itr = classes.iterator();
		while (itr.hasNext()) {
			try {
				Class c = Class.forName(itr.next(), false, loader);
				if (c.isAnnotationPresent(Deprecated.class)) {
					Logger.println("S171", "ignore " + c.getName());
					continue;
				}
				if (IXLog.class.isAssignableFrom(c)) {
					xlogs.add((IXLog) c.newInstance());
					Logger.println("S172", "load IXLog=" + c.getName());
				} else if (IXLogProfile.class.isAssignableFrom(c)) {
					xlogProfiles.add((IXLogProfile) c.newInstance());
					Logger.println("S202", "load IXLogProfile=" + c.getName());
				} else if (IAlert.class.isAssignableFrom(c)) {
					alerts.add((IAlert) c.newInstance());
					Logger.println("S173", "load IAlert=" + c.getName());
				} else if (IObject.class.isAssignableFrom(c)) {
					objects.add((IObject) c.newInstance());
					Logger.println("S174", "load IObject=" + c.getName());
				} else if (IServiceGroup.class.isAssignableFrom(c)) {
					Logger.println("S175", "load IServiceGroup=" + c.getName());
					groups.add((IServiceGroup) c.newInstance());
				}
			} catch (Exception e) {
			}
		}
	}
	private static void unload() {
		List old = xlogs;
		xlogs = new ArrayList();
		unload(old);
		old = groups;
		groups = new ArrayList();
		unload(old);
		old = alerts;
		alerts = new ArrayList();
		unload(old);
		old = objects;
		objects = new ArrayList();
		unload(old);
	}
	private static void unload(List old) {
		for (int i = 0; i < old.size(); i++) {
			((IPlugIn) old.get(i)).unload();
		}
	}
	private static String jarName = null;
	private static ClassLoader pluginClassLoader;
	public static void getInstance() {
		final Configure conf = Configure.getInstance();
		jarName = conf.plugin_classpath;
		load(jarName);
		ConfObserver.put(PlugInManager.class.getName(), new Runnable() {
			public void run() {
				if (CompareUtil.equals(jarName, conf.plugin_classpath) == false) {
					createClassLoader(null);
					unload();
					jarName = conf.plugin_classpath;
					load(jarName);
				}
			}
		});
	}
	protected static List<File> getJarNames(String path) {
		ArrayList<File> arr = new ArrayList<File>();
		StringTokenizer nizer = new StringTokenizer(path, ";:");
		while (nizer.hasMoreTokens()) {
			String name = StringUtil.trimToEmpty(nizer.nextToken());
			File f = new File(name);
			if (f.canRead() == false)
				continue;
			arr.add(f);
		}
		return arr;
	}
	public synchronized static ClassLoader createClassLoader(String path) {
		if (StringUtil.isEmpty(path)) {
			pluginClassLoader = null;
			return null;
		}
		if (pluginClassLoader == null) {
			try {
				List<File> files = getJarNames(path);
				ArrayList<URL> arr = new ArrayList<URL>();
				for (int i = 0; i < files.size(); i++) {
					arr.add(files.get(i).toURI().toURL());
				}
				pluginClassLoader = new URLClassLoader((URL[]) arr.toArray(new URL[arr.size()]),
						PlugInManager.class.getClassLoader());
			} catch (Throwable e) {
				Logger.println("S176", e);
			}
		}
		return pluginClassLoader;
	}
	public static void xlog(XLogPack m) {
		for (int i = 0; i < xlogs.size(); i++) {
			xlogs.get(i).process(m);
		}
	}
	public static void profile(XLogProfilePack m) {
		for (int i = 0; i < xlogProfiles.size(); i++) {
			xlogProfiles.get(i).process(m);
		}
	}
	public static void active(ObjectPack p) {
		for (int i = 0; i < objects.size(); i++) {
			objects.get(i).process(p);
		}
	}
	public static void alert(AlertPack p) {
		for (int i = 0; i < alerts.size(); i++) {
			alerts.get(i).process(p);
		}
	}
	public static void counter(PerfCounterPack p) {
		for (int i = 0; i < counters.size(); i++) {
			counters.get(i).process(p);
		}
	}
	public static int grouping(XLogPack p) {
		int x = 0;
		for (int i = 0; i < groups.size(); i++) {
			x |= groups.get(i).grouping(p);
		}
		return x;
	}
}
