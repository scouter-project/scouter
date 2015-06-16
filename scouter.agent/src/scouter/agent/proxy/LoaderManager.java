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
package scouter.agent.proxy;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import scouter.agent.Configure;
import scouter.agent.JavaAgent;
import scouter.agent.Logger;
import scouter.agent.util.ManifestUtil;
import scouter.util.FileUtil;
import scouter.util.IntKeyLinkedMap;
import scouter.util.StringUtil;
public class LoaderManager {
	private static ClassLoader pluginClassLoader;
	private static ClassLoader toolsClassLoader;
	
	private static IntKeyLinkedMap<ClassLoader> loaders = new IntKeyLinkedMap<ClassLoader>().setMax(10);
	public synchronized static ClassLoader getToolsLoader() {
		if (toolsClassLoader == null) {
			try {
				File tools = ManifestUtil.getToolsFile();
				File tempFile = deployJar("scouter.tools");
				toolsClassLoader = new URLClassLoader(new URL[] { tools.toURI().toURL(), tempFile.toURI().toURL() },
						null);
			} catch (Throwable e) {
				Logger.println("A134", e);
			}
		}
		return toolsClassLoader;
	}
	public synchronized static ClassLoader getHttpLoader(ClassLoader parent) {
		if (parent == null)
			return null;
		ClassLoader loader  = loaders.get(System.identityHashCode(parent));
		
		if (loader == null) {
			try {
				File tempFile = deployJar("scouter.http");
				loader = new URLClassLoader(new URL[] { tempFile.toURI().toURL() }, parent);
				loaders.put(System.identityHashCode(parent), loader);
			} catch (Throwable e) {
				Logger.println("A135", "SUBLOADER " + e);
			}
		}
		return loader;
	}
	public synchronized static ClassLoader getDB2Loader(ClassLoader parent) {
		ClassLoader loader  = loaders.get(System.identityHashCode(parent));
		if (loader == null) {
			try {
				File tempFile = deployJar("scouter.jdbc");
				loader = new URLClassLoader(new URL[] { tempFile.toURI().toURL() }, parent);
				loaders.put(System.identityHashCode(parent), loader);
			} catch (Throwable e) {
				Logger.println("A136", "SUBLOADER " + e);
			}
		}
		return loader;
	}
	public synchronized static ClassLoader getHttpClient(ClassLoader parent) {
		if (parent == null)
			return null;
		ClassLoader loader  = loaders.get(System.identityHashCode(parent));
		if (loader == null) {
			try {
				File tempFile = deployJar("scouter.httpclient");
				loader = new URLClassLoader(new URL[] { tempFile.toURI().toURL() }, parent);
				loaders.put(System.identityHashCode(parent), loader);
			} catch (Throwable e) {
				Logger.println("A137", "SUBLOADER " + e);
			}
		}
		return loader;
	}
	private static File deployJar(String jarname) {
		try {
			File target = new File(Configure.getInstance().subagent_dir, jarname + ".jar");
			if (target.canRead() == false) {
				InputStream is = JavaAgent.class.getResourceAsStream("/" + jarname + ".jar");
				byte[] newBytes = FileUtil.readAll(is);
				is.close();
				FileUtil.save(target, newBytes);
				return target;
			}
			long oldLength = target.length();
			InputStream is = JavaAgent.class.getResourceAsStream("/" + jarname + ".jar");
			byte[] newBytes = FileUtil.readAll(is);
			is.close();
			if ( newBytes.length != oldLength) {
				FileUtil.save(target, newBytes);
			}
			return target;
		} catch (Exception e) {
			Logger.println("A138", "fail to deploy " + jarname);
			return null;
		}
	}
	public static ClassLoader getPlugInLoader() {
		return pluginClassLoader;
	}
	public synchronized static void createPlugInLoader(String file) {
		if (StringUtil.isEmpty(file) || new File(file).canRead() == false) {
			pluginClassLoader = null;
			return;
		}
		try {
			URL[] urls = toURLS(file);
			if (urls.length > 0) {
				pluginClassLoader = new URLClassLoader(urls, LoaderManager.class.getClassLoader());
			}
		} catch (Throwable e) {
			Logger.println("A139", e);
		}
	}
	private static URL[] toURLS(String file) {
		List<URL> urls = new ArrayList<URL>();
		StringTokenizer nizer = new StringTokenizer(file, ";");
		while (nizer.hasMoreTokens()) {
			String n = StringUtil.trimToEmpty(nizer.nextToken());
			if (n.length() > 0) {
				try {
					urls.add(new File(n).toURI().toURL());
				} catch (Exception e) {
				}
			}
		}
		return urls.toArray(new URL[0]);
	}
}
