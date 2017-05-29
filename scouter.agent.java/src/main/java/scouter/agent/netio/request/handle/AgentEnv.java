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
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import scouter.agent.Configure;
import scouter.agent.JavaAgent;
import scouter.agent.Logger;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.netio.request.anotation.RequestHandler;
import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.ListValue;
import scouter.lang.value.TextValue;
import scouter.net.RequestCmd;
import scouter.net.TcpFlag;
import scouter.util.StringUtil;

public class AgentEnv {

	@RequestHandler(RequestCmd.OBJECT_SYSTEM_GC)
	public Pack systemGc(Pack param) {
		MapPack m = new MapPack();
		System.gc();
		Logger.println("A127", RequestCmd.OBJECT_SYSTEM_GC);
		return m;
	}

	@RequestHandler(RequestCmd.OBJECT_ENV)
	public Pack getAgentEnv(Pack param) {
		MapPack m = new MapPack();
		Properties p = System.getProperties();
		@SuppressWarnings("rawtypes")
		Enumeration en = p.keys();
		while (en.hasMoreElements()) {
			String key = (String) en.nextElement();
			String value = p.getProperty(key);
			m.put(key, new TextValue(value));
		}
		return m;
	}

	Configure conf = Configure.getInstance();

	@RequestHandler(RequestCmd.OBJECT_INFO)
	public Pack getAgentInfo(Pack param) {
		MapPack p = new MapPack();
		p.put("objHash", conf.getObjHash());
		p.put("objName", conf.getObjName());
		p.put("java.version", System.getProperty("java.version"));
		p.put("os.name", System.getProperty("os.name"));
		p.put("user.home", System.getProperty("user.home"));
		p.put("work.dir", new File(".").getAbsolutePath());
		return p;
	}

	@RequestHandler(RequestCmd.OBJECT_RESET_CACHE)
	public Pack getAgentCacheReseto(Pack param) {
		DataProxy.reset();
		return param;
	}
	
	@RequestHandler(RequestCmd.OBJECT_DUMP_FILE_LIST)
	public Pack getDumpFileList(Pack param) {
		MapPack result = new MapPack();
		ListValue nameLv = result.newList("name");
		ListValue sizeLv = result.newList("size");
		ListValue lastModifedLv = result.newList("last_modified");
		File dumpDir = Configure.getInstance().dump_dir;
		if (dumpDir.canRead()) {
			for (File file : dumpDir.listFiles()) {
				if (file.isFile()) {
					nameLv.add(file.getName());
					sizeLv.add(file.length());
					lastModifedLv.add(file.lastModified());
				}
			}
		}
		return result;
	}
	
	@RequestHandler(RequestCmd.OBJECT_DUMP_FILE_DETAIL)
	public Pack getDumpFileDetail(Pack param, DataInputX in, DataOutputX out) {
		MapPack p = (MapPack) param;
		String name = p.getText("name");
		File dumpDir = Configure.getInstance().dump_dir;
		File dumpFile = new File(dumpDir, name);
		if (dumpFile.canRead()) {
			try {
				int buff = 4 * 1024;
				InputStream stream = new FileInputStream(dumpFile);
				byte[] buffer = new byte[buff];
				int n;
				while ((n = stream.read(buffer, 0, buffer.length)) != -1) {
					out.writeByte(TcpFlag.HasNEXT);
					out.writeBlob(buffer, 0, n);
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@RequestHandler(RequestCmd.OBJECT_CLASS_LIST)
	public Pack getLoadedClassList(Pack param) {
		final int COUNT_PER_PAGE = 100;
		MapPack mPack = (MapPack) param;
		int page = mPack.getInt("page");
		if (page < 1) {
			page = 1;
		}
		String filter = mPack.getText("filter");
		MapPack p = new MapPack();
		ListValue indexLv = p.newList("index");
		ListValue typeLv = p.newList("type");
		ListValue nameLv = p.newList("name");
		ListValue superClassLv = p.newList("superClass");
		ListValue interfacesLv = p.newList("interfaces");
		ListValue resourceLv = p.newList("resource");
		p.put("page", page);
		p.put("totalPage", 0);

		Class[] loadedClasses = JavaAgent.getInstrumentation().getAllLoadedClasses();
		if (loadedClasses != null) {
			List<Class> classList = new ArrayList<Class>();
			for (int i = 0; i < loadedClasses.length; i++) {
				Class clazz = loadedClasses[i];
				if (StringUtil.isNotEmpty(filter)) {
					String lowerName = clazz.getName().toLowerCase();
					if (lowerName.contains(filter.toLowerCase()) == false) {
						continue;
					}
				}
				classList.add(clazz);
			}
			int totalCount = classList.size();
			p.put("totalPage", (totalCount / COUNT_PER_PAGE) + 1);
			int start = (page - 1) * COUNT_PER_PAGE;
			for (int i = start; i < start + COUNT_PER_PAGE; i++) {
				if (i > totalCount - 1) {
					break;
				}
				try {
					indexLv.add(i + 1);
					Class clazz = classList.get(i);
					typeLv.add(clazz.isInterface() ? "I" : (clazz.isPrimitive() ? "P" : "C"));
					nameLv.add(clazz.getName());
					String superClassName = "";
					Class superClass = clazz.getSuperclass();
					if (superClass != null) {
						superClassName = superClass.getName();
					}
					superClassLv.add(superClassName);
					StringBuffer sb = new StringBuffer();
					Class[] interfaces = clazz.getInterfaces();
					if (interfaces != null) {
						for (int j = 0; j < interfaces.length; j++) {
							sb.append(interfaces[j].getName());
							if (j < interfaces.length - 1) {
								sb.append(",");
							}
						}
					}
					interfacesLv.add(sb.toString());
					ClassLoader classLoader = clazz.getClassLoader();
					if (classLoader == null) {
						classLoader = ClassLoader.getSystemClassLoader();
					}
					String resource = "";
					try{
						URL url = classLoader.getResource(clazz.getName().replace('.', '/') + ".class");
						if (url != null) {
							resource = url.toString();
						}
					}catch(Throwable t){}
					resourceLv.add(resource);
				} catch (Exception e) {
					e.printStackTrace();
					break;
				}
			}
		}
		return p;
	}
}
