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
package scouter.agent.batch.proxy;

import scouter.agent.batch.JavaAgent;
import scouter.agent.batch.Logger;
import scouter.agent.batch.util.ManifestUtil;
import scouter.util.ArrayUtil;
import scouter.util.BytesClassLoader;
import scouter.util.FileUtil;
import scouter.util.HashUtil;
import scouter.util.IntKeyLinkedMap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;

/**
 * @author Paul S.J. Kim(sjkim@whatap.io)
 * @author Gun Lee(gunlee01@gmail.com)
 */

public class LoaderManager {
	private static ClassLoader toolsLoader;
	private static IntKeyLinkedMap<ClassLoader> loaders = new IntKeyLinkedMap<ClassLoader>().setMax(10);

	public synchronized static ClassLoader getToolsLoader() {
		if (toolsLoader == null) {
			try {
				if (JavaAgent.isJava9plus()) {
					toolsLoader = ClassLoader.getSystemClassLoader();
				} else {
					File tools = ManifestUtil.getToolsFile();
					toolsLoader = new URLClassLoader(new URL[] { tools.toURI().toURL() });
				}
			} catch (Throwable e) {
				Logger.println("A134", e);
			}
		}

		return createLoader(toolsLoader, "scouter.tools");
	}

	public static ClassLoader getHttpLoader(ClassLoader parent) {
		return createLoader(parent, "scouter.http");
	}

	//TODO del
	public static ClassLoader getJdbcLoader(ClassLoader parent) {
		return createLoader(parent, "scouter.jdbc");
	}

	public static ClassLoader getDB2Loader(ClassLoader parent) {
		return createLoader(parent, "scouter.jdbc");
	}

	public static ClassLoader getHttpClient(ClassLoader parent) {
		return createLoader(parent, "scouter.httpclient");
	}

	public static ClassLoader getKafkaClient(ClassLoader parent) {
		return createLoader(parent, "scouter.kafka");
	}

	public static ClassLoader getRedisClient(ClassLoader parent) {
		return createLoader(parent, "scouter.redis");
	}

	private synchronized static ClassLoader createLoader(ClassLoader parent, String key) {

		int hashKey = (parent == null ? 0 : System.identityHashCode(parent));
		hashKey = hashKey ^ HashUtil.hash(key);
		ClassLoader loader = loaders.get(hashKey);
		if (loader == null) {
			try {
				byte[] bytes = deployJarBytes(key);
				if (bytes != null) {
					loader = new BytesClassLoader(bytes, parent);
					loaders.put(hashKey, loader);
				}
			} catch (Throwable e) {
				Logger.println("A137", "SUBLOADER " + key + " " + e);
			}
		}
		return loader;
	}

	public static ClassLoader appendToSystemOrBootLoader(String key) {
		if (JavaAgent.isJava9plus()) {
			appendToSystemLoader(JavaAgent.getInstrumentation(), key);
			return ClassLoader.getSystemClassLoader();
		} else {
			appendToBootLoader(JavaAgent.getInstrumentation(), key);
			return null;
		}
	}

	private static void appendToSystemLoader(Instrumentation inst, String key) {
		byte[] bytes = deployJarBytes(key);
		try {
			File tempJar = FileUtil.saveAsTemp(key, ".jar", bytes);
			inst.appendToSystemClassLoaderSearch(new JarFile(tempJar));
		} catch (IOException e) {
			Logger.println("A138", "Error on load " + key + ".jar " + e);
			throw new RuntimeException(e);
		}
	}

	private static void appendToBootLoader(Instrumentation inst, String key) {
		byte[] bytes = deployJarBytes(key);
		try {
			File tempJar = FileUtil.saveAsTemp(key, ".jar", bytes);
			inst.appendToBootstrapClassLoaderSearch(new JarFile(tempJar));
		} catch (IOException e) {
			Logger.println("A138", "Error on load " + key + ".jar " + e);
			throw new RuntimeException(e);
		}
	}

	private static byte[] deployJarBytes(String jarname) {
		try {
			InputStream is = JavaAgent.class.getResourceAsStream("/" + jarname + ".jar");
			byte[] newBytes = FileUtil.readAll(is);
			is.close();
			Logger.println("NONE", "LoadJarBytes " + jarname + " " + ArrayUtil.len(newBytes) + " bytes");
			return newBytes;
		} catch (Exception e) {
			Logger.println("NONE", "fail to load jar bytes " + jarname);
			return null;
		}
	}

}
