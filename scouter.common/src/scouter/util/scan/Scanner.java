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

package scouter.util.scan;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import scouter.util.StringUtil;

public class Scanner {
	private String prefix;

	public Scanner(String prefix) {
		if (StringUtil.isEmpty(prefix))
			this.prefix = null;
		else
			this.prefix = prefix.replace('.', '/');
	}

	public Set<String> process() {
		return process(Thread.currentThread().getContextClassLoader());
	}

	public Set<String> process(ClassLoader loader) {
		Set<String> mainSet = new TreeSet<String>();
		if (this.prefix == null)
			return mainSet;

		try {
			Set<File> files = getRoot(loader);
			Iterator<File> itr = files.iterator();
			while (itr.hasNext()) {
				Set<String> classes = listUp(itr.next());
				mainSet.addAll(classes);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return mainSet;
	}

	public Set<File> getRoot() throws IOException {
		Set<File> files = new HashSet<File>();
		if (this.prefix != null) {
			Enumeration<URL> en = Thread.currentThread().getContextClassLoader().getResources(prefix);
			while (en.hasMoreElements()) {
				File file = parse(en.nextElement());
				files.add(file);
			}
		}
		return files;
	}

	public Set<File> getRoot(ClassLoader loader) throws IOException {
		Set<File> files = new HashSet<File>();
		if (this.prefix != null) {
			Enumeration<URL> en = loader.getResources(prefix);
			while (en.hasMoreElements()) {
				File file = parse(en.nextElement());
				files.add(file);
			}
		}
		return files;
	}

	private File parse(URL res) {
		String file = res.getFile();
		int x = file.indexOf("!");
		if (x > 0)
			return new File(file.substring(file.indexOf("/"), x));
		else
			return new File(file.substring(file.indexOf("/"), file.length() - prefix.length()));
	}

	public Set<String> listUp(File root) {
		Set<String> classes = new HashSet<String>();
		if (this.prefix != null) {
			if (root.isDirectory()) {
				listUp(classes, new File(root, prefix), root.getAbsolutePath());
			} else {
				try {
					listUp(classes, new JarFile(root));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return classes;
	}

	public static Set<String> getClassesInJar(String jarName) {

		Set<String> classes = new HashSet<String>();
		try {
			if (jarName == null)
				return classes;
			JarFile file = new JarFile(jarName);
			Enumeration<JarEntry> en = file.entries();
			while (en.hasMoreElements()) {
				JarEntry entry = en.nextElement();
				if (entry.getName().toLowerCase().endsWith(".class")) {
					classes.add(getClassName(entry.getName()));
				}
			}
		} catch (Exception e) {
		}
		return classes;
	}

	public void listUp(Set<String> classes, JarFile file) {
		if (this.prefix == null)
			return;

		Enumeration<JarEntry> en = file.entries();
		while (en.hasMoreElements()) {
			JarEntry entry = en.nextElement();
			if (entry.getName().startsWith(prefix) == false) {
				continue;
			}
			if (entry.getName().toLowerCase().endsWith(".class")) {
				classes.add(getClassName(entry.getName()));
			}
		}
	}

	private void listUp(Set<String> classes, File file, String root) {
		if (file.isDirectory() == false)
			return;
		File[] sub = file.listFiles();
		for (int i = 0; i < sub.length; i++) {
			if (sub[i].getName().toLowerCase().endsWith(".class")) {
				String name = sub[i].getAbsolutePath().substring(root.length());
				name = name.replace('\\', '/');
				if (name.startsWith("/"))
					name = name.substring(1);
				classes.add(getClassName(name));
			}
			if (sub[i].getName().startsWith("."))
				continue;
			if (sub[i].isDirectory()) {
				listUp(classes, sub[i], root);
			}
		}

	}

	public static String getClassName(String name) {
		return name.substring(0, name.length() - 6).replace('/', '.');
	}

	public static String cutOutLast(String name, String seperator) {
		int x = name.lastIndexOf(seperator);
		if (x < 0)
			return name;
		return name.substring(0, x);
	}
}