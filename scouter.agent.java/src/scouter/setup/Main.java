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
package scouter.setup;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;

import scouter.agent.Logger;
import scouter.agent.util.ManifestUtil;

public class Main {

	public static void main(String[] args) throws Throwable {

		File tools = ManifestUtil.getToolsFile();
		File setup = new File(ManifestUtil.getThisJarName());
		URLClassLoader myloader = new URLClassLoader(new URL[] { tools.toURI().toURL(), setup.toURI().toURL() }, null);

		Class c = Class.forName(InternalMain.class.getName(), true, myloader);
		Class[] argc = { String[].class };
		Object[] argo = { args };

		java.lang.reflect.Method method = c.getDeclaredMethod("main", argc);
		try {
			method.invoke(null, argo);
		} catch (InvocationTargetException e) {
			throw e.getTargetException();
		}
	}

}