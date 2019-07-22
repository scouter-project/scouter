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
package scouter.agent.batch.netio.request;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import scouter.agent.batch.Logger;
import scouter.agent.batch.netio.request.anotation.RequestHandler;
import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.lang.pack.Pack;
import scouter.util.scan.Scanner;

public class ReqestHandlingProxy {
	protected static HashMap<String, Invocation> handlers = new HashMap<String, Invocation>();

	protected static class Invocation {
		Object object;
		Method method;
		Class[] pTypes;

		public Invocation(Object object, Method method, Class[] paramter) {
			this.object = object;
			this.method = method;
			this.pTypes = paramter;
		}

		public Pack exec(Pack p, DataInputX in, DataOutputX out) {
			try {
				switch (pTypes.length) {
				case 3:
					return (Pack) method.invoke(object, new Object[] { p, in, out });
				default:
					return (Pack) method.invoke(object, new Object[] { p });
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public String toString() {
			return object.getClass().getName() + "." + method.getName();
		}
	}

	public static void load(Class cls) {
		String pkg = Scanner.cutOutLast(cls.getName(), ".");
		Set<String> classes = new Scanner(pkg).process();
		Set<String> custom = new Scanner(System.getProperty("scouter.handler")).process();
		classes.addAll(custom);
		
		Iterator<String> itr = classes.iterator();
		while (itr.hasNext()) {
			try {
				Class c = Class.forName(itr.next());
				if (Modifier.isPublic(c.getModifiers()) == false)
					continue;
				try {
					Method[] m = c.getDeclaredMethods();
					for (int i = 0; i < m.length; i++) {
						RequestHandler mapAn = (RequestHandler) m[i].getAnnotation(RequestHandler.class);
						if (mapAn == null)
							continue;
						String key = mapAn.value();
						Invocation news = new Invocation(c.newInstance(), m[i], m[i].getParameterTypes());
						Invocation olds = handlers.get(key);
						if (olds != null) {
							Logger.println("A129", "Warning duplicated Handler key=" + key + " " + olds + " <-> " + news);
						}
						handlers.put(key, news);
					}
				} catch (Exception x) {
					x.printStackTrace();
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	public static Pack process(String cmd, Pack req, DataInputX in, DataOutputX out) {
		if ("KEEP_ALIVE".equals(cmd))
			return null;
		Invocation handler = handlers.get(cmd);
		if (handler != null) {
			return handler.exec(req, in, out);
		} else {
			Logger.println("A130", "TCP unknown cmd=" + cmd);
		}
		return null;
	}

	public static void main(String[] args) {
		load(ReqestHandlingProxy.class);
	}

}
