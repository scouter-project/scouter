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

package scouter.boot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarUtil {

	public static File getThisJarFile() {
		String path = "";
		ClassLoader cl = JarUtil.class.getClassLoader();
		if (cl == null) {
			path = ""
					+ ClassLoader.getSystemClassLoader().getResource(Boot.class.getName().replace('.', '/') + ".class");
		} else {
			path = "" + cl.getResource(JarUtil.class.getName().replace('.', '/') + ".class");
		}
		path = path.substring("jar:file:/".length(), path.indexOf("!"));
		if (path.indexOf(':') > 0)
			return new File(path);
		else
			return new File("/" + path);
	}

	public static void unJar(File jarFile, File toDir) throws IOException {
		if (jarFile == null)
			return;
		JarFile jar = new JarFile(jarFile);
		try {
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = (JarEntry) entries.nextElement();
				if (!entry.isDirectory() && isOk(entry.getName())) {
					InputStream in = jar.getInputStream(entry);
					try {
						File file = new File(toDir, entry.getName());
						file.getParentFile().mkdirs();
						OutputStream out = new FileOutputStream(file);
						try {
							byte[] buf = new byte[8192];
							int n = in.read(buf);
							while (n >= 0) {
								out.write(buf, 0, n);
								n = in.read(buf);
							}
						} finally {
							out.close();
						}
					} finally {
						in.close();
					}
				}
			}
		} finally {
			jar.close();
		}
	}

	private static boolean isOk(String name) {
		if (name.startsWith("META-INF"))
			return false;
		if (name.startsWith("scouter/boot"))
			return false;
		if (name.endsWith(".class"))
			return false;
		return true;
	}
}