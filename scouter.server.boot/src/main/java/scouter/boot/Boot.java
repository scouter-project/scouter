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
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class Boot {

	public static void main(String[] args) throws Throwable {
		String lib = "./lib";
		if (args.length >= 1) {
			lib = args[0];
		}

		URL[] jarfiles = getURLs(lib);
		URLClassLoader classloader = new URLClassLoader(jarfiles, Boot.class.getClassLoader());
		Thread.currentThread().setContextClassLoader(classloader);

		String mainClass = System.getProperty("main","scouter.server.Main");
		try {
			Class c = Class.forName(mainClass, true, classloader);
			Class[] argc = { String[].class };
			Object[] argo = { args };
			java.lang.reflect.Method method = c.getDeclaredMethod("main", argc);
			method.invoke(null, argo);
		} catch (InvocationTargetException e) {
			throw e.getTargetException();
		} finally {
			usage();
		}
	}

	private static void usage() {
		System.out.println("java -cp ./boot.jar scouter.boot.Boot [./lib] ");
	}

	private static URL[] getURLs(String path) throws IOException {
		//= sub 디렉토리 까지 읽도록 설정 추가
		List<File>  files = new TargetDirectoryRead().read(new File(path));
		Map<String, File> jars = new TreeMap<String, File>();
		for (File file : files) {
			if (file.getName().startsWith("."))
				continue;
			jars.put(file.getName(), file);
		}

		URL[] urls = new URL[jars.size()];
		ArrayList<File> v = new ArrayList<File>(jars.values());
		for (int i = 0; i < urls.length; i++) {
			urls[i] = v.get(i).toURI().toURL();
		}
		return urls;
	}
	static class TargetDirectoryRead {
		private List<File> filePathList = new ArrayList<>();

		public List<File> read(File file) {
			if (file.isFile() ) {
				filePathList.add(file);
			} else if (file.isDirectory()) {
				File[] listOfFiles = file.listFiles();
				if (listOfFiles != null) {
					Arrays.stream(listOfFiles).forEach(v -> read(v));
				} else {
					System.out.println("[ACCESS DENIED]");
				}
			}
			return filePathList;
		}
	}

}
