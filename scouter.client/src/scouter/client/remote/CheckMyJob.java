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
 *
 */
package scouter.client.remote;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.client.remote.handle.ActionControl;
import scouter.client.remote.handle.LifeControl;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ConsoleProxy;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.pack.TextPack;
import scouter.io.DataInputX;
import scouter.net.RequestCmd;
import scouter.util.ThreadUtil;

public class CheckMyJob extends Thread {
	
	private static volatile CheckMyJob instance;
	
	public static CheckMyJob getInstance() {
		if (instance == null) {
			synchronized (CheckMyJob.class) {
				if (instance == null) {
					instance = new CheckMyJob();
					instance.setDaemon(true);
					instance.setName(ThreadUtil.getName(instance));
					instance.start();
				}
			}
		}
		return instance;
	}
	
	protected static HashMap<String, Invocation> handlers = new HashMap<String, Invocation>();

	protected static class Invocation {
		Object object;
		Method method;

		public Invocation(Object object, Method method) {
			this.object = object;
			this.method = method;
		}

		public void exec(int serverId, MapPack param) {
			try {
				method.invoke(object, new Object[] {serverId, param });
			} catch (InvocationTargetException t) {
				if (t.getCause() != null) {
					t.getCause().printStackTrace();
				} else {
					t.printStackTrace();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public String toString() {
			return object.getClass().getName() + "." + method.getName();
		}
	}
	
	static Set<String> classSet = new HashSet<String>();
	static {
		classSet.add(LifeControl.class.getName());
		classSet.add(ActionControl.class.getName());
	}
	
	private void load() {
//		String pkg = Scaner.cutOutLast(LifeControl.class.getName(), ".");
//		Set<String> classes = new Scaner(pkg).process();
		Iterator<String> itr = classSet.iterator();
		while (itr.hasNext()) {
			try {
				Class c = Class.forName(itr.next());
				if (Modifier.isPublic(c.getModifiers()) == false)
					continue;
				try {
					Method[] m = c.getDeclaredMethods();
					for (int i = 0; i < m.length; i++) {
						RemoteHandler mapAn = (RemoteHandler) m[i].getAnnotation(RemoteHandler.class);
						if (mapAn == null)
							continue;
						String key = mapAn.value();
						Invocation news = new Invocation(c.newInstance(), m[i]);
						handlers.put(key, news);
					}
				} catch (Exception x) {
					x.printStackTrace();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void run() {
		load();
		while (true) {
			try {
				Set<Integer> serverSet = ServerManager.getInstance().getOpenServerList();
				for (Integer id : serverSet) {
					Server server = ServerManager.getInstance().getServer(id);
					TcpProxy tcp = TcpProxy.getTcpProxy(id);
					try {
						MapPack param = new MapPack();
						param.put("session", server.getSession());
						final DoJob job = new DoJob();
						tcp.process(RequestCmd.CHECK_JOB, param, new INetReader() {
							public void process(DataInputX in) throws IOException {
								Pack p = in.readPack();
								if (p instanceof TextPack) {
									job.command = ((TextPack) p).text;
								} else if (p instanceof MapPack) {
									job.param = ((MapPack) p);
								}
							}
						});
						if (job.command != null) {
							ConsoleProxy.infoSafe(job.command  + " cheked");
							Invocation handler = handlers.get(job.command);
							if (handler != null) {
								handler.exec(id, job.param);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						TcpProxy.putTcpProxy(tcp);
					}
				}
			} catch (Throwable th) {
				th.printStackTrace();
			}
			ThreadUtil.sleep(2000);
		}
	}
	
	static class DoJob {
		String command;
		MapPack param;
	}
}
