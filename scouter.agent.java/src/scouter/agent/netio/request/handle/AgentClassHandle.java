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
package scouter.agent.netio.request.handle;
import java.io.File;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import scouter.agent.JavaAgent;
import scouter.agent.Logger;
import scouter.agent.netio.request.anotation.RequestHandler;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.BlobValue;
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.lang.value.Value;
import scouter.net.RequestCmd;
import scouter.util.ClassUtil;
import scouter.util.FileUtil;
public class AgentClassHandle {
	@RequestHandler(RequestCmd.OBJECT_LOAD_CLASS_BY_STREAM)
	public Pack loadClassAsStream(Pack param) {
		MapPack p = (MapPack) param;
		String className = p.getText("class");
		InputStream is = null;
		try {
			Class clazz = getClass(className);
			if (clazz == null) {
				p.put("error", "Not found class " + className);
				return p;
			}
			String clsAsResource = "/" + className.replace('.', '/').concat(".class");
			is = clazz.getResourceAsStream(clsAsResource);
			p.put("class", new BlobValue(FileUtil.readAll(is)));
		} catch (Throwable th) {
			Logger.println("A126", th);
			p.put("error", th.getMessage());
			return p;
		} finally {
			FileUtil.close(is);
		}
		return p;
	}
	@RequestHandler(RequestCmd.OBJECT_CLASS_DESC)
	public Pack getClassInfo(Pack param) {
		MapPack p = (MapPack) param;
		String className = p.getText("class");
		try {
			Class clazz = getClass(className);
			if (clazz == null) {
				p.put("error", "Not found class " + className);
				return p;
			}
			p.put("class", ClassUtil.getClassDescription(clazz));
		} catch (Throwable th) {
			Logger.println("A904", th);
			p.put("error", th.getMessage());
			return p;
		}
		return p;
	}
	private ListValue toValue(Class[] inf) {
		ListValue v = new ListValue();
		for (int i = 0; i < inf.length; i++) {
			v.add(inf[i].getName());
		}
		return v;
	}
	private Class getClass(String className) {
		Class[] loadedClasses = JavaAgent.getInstrumentation().getAllLoadedClasses();
		for (Class c : loadedClasses) {
			if (c.getName().equals(className)) {
				return c;
			}
		}
		return null;
	}
	@RequestHandler(RequestCmd.OBJECT_CHECK_RESOURCE_FILE)
	public Pack checkJarFile(Pack param) {
		MapPack p = (MapPack) param;
		String resource = p.getText("resource");
		MapPack m = new MapPack();
		try {
			URL url = new URL(resource);
			JarURLConnection connection = (JarURLConnection) url.openConnection();
			File file = new File(connection.getJarFileURL().toURI());
			if (file.exists() == false) {
				m.put("error", "Cannot find jar file.");
			} else {
				if (file.canRead()) {
					m.put("name", file.getName());
					m.put("size", file.length());
				} else {
					m.put("error", "Cannot read jar file.");
				}
			}
		} catch (Exception e) {
			m.put("error", e.toString());
		}
		return m;
	}
	@RequestHandler(RequestCmd.OBJECT_DOWNLOAD_JAR)
	public Pack downloadJar(Pack param) {
		MapPack p = (MapPack) param;
		String resource = p.getText("resource");
		MapPack m = new MapPack();
		try {
			URL url = new URL(resource);
			JarURLConnection connection = (JarURLConnection) url.openConnection();
			File file = new File(connection.getJarFileURL().toURI());
			if (file.exists() == false) {
				m.put("error", "Cannot find jar file.");
			} else {
				if (file.canRead()) {
					m.put("jar", new BlobValue(FileUtil.readAll(file)));
				} else {
					m.put("error", "Cannot read jar file.");
				}
			}
		} catch (Exception e) {
			m.put("error", e.toString());
		}
		return m;
	}
}
