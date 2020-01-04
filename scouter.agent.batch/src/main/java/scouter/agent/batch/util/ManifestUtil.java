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

package scouter.agent.batch.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import scouter.agent.batch.JavaAgent;

public class ManifestUtil {
	public static void print() throws IOException {
		InputStream manifestStream = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("META-INF/MANIFEST.MF");
		try {
			Manifest manifest = new Manifest(manifestStream);
			Attributes attributes = manifest.getMainAttributes();
			String impVersion = attributes.getValue("Implementation-Version");
			System.out.println(attributes);
		} catch (IOException ex) {
			ex.printStackTrace();
		}

	}

	public static File getToolsFile() throws Exception {
		String java_home = System.getProperty("java.home");
		File tools = new File(java_home, "../lib/tools.jar");
		if (tools.canRead() == false) {
			java_home = System.getenv("JAVA_HOME");
			if (java_home == null) {
				throw new Exception("The JAVA_HOME environment variable is not defined correctly");
			}
			tools = new File(java_home, "./lib/tools.jar");
			if (tools.canRead() == false) {
				throw new Exception("The JAVA_HOME environment variable is not defined correctly");
			}
		}
		return tools;
	}

	public static String getThisJarName() {
		String path = "";
		ClassLoader cl = ManifestUtil.class.getClassLoader();
		if (cl == null) {
			path = ""
					+ ClassLoader.getSystemClassLoader().getResource(
							JavaAgent.class.getName().replace('.', '/') + ".class");
		} else {
			path = "" + cl.getResource(JavaAgent.class.getName().replace('.', '/') + ".class");
		}
		path = path.substring("jar:file:/".length(), path.indexOf("!"));
		if (path.indexOf(':') > 0)
			return path;
		else
			return "/" + path;
	}
}