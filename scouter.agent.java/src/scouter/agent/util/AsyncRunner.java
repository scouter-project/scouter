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
package scouter.agent.util;

import java.lang.instrument.ClassDefinition;

import scouter.agent.JavaAgent;
import scouter.agent.Logger;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.summary.ErrorData;
import scouter.agent.summary.ServiceSummary;
import scouter.util.RequestQueue;
import scouter.util.SystemUtil;
import scouter.util.ThreadUtil;

public class AsyncRunner extends Thread {

	private static AsyncRunner instance = null;

	public final static synchronized AsyncRunner getInstance() {
		if (instance == null) {
			instance = new AsyncRunner();
			instance.setDaemon(true);
			instance.setName(ThreadUtil.getName(instance));
			instance.start();
		}
		return instance;
	}

	private RequestQueue<Object> queue = new RequestQueue<Object>(1024);

	private static class Hook {
		public Hook(ClassLoader loader, String classname, byte[] body) {
			super();
			this.loader = loader;
			this.classname = classname.replace('/', '.');
			this.body = body;
		}

		ClassLoader loader;
		String classname;
		byte[] body;
	}

	public void add(ClassLoader loader, String classname, byte[] body) {
		queue.put(new Hook(loader, classname, body));
	}

	public void add(LeakData data) {
		queue.put(data);
	}

	public void add(Runnable r) {
		queue.put(r);
	}

	public void run() {
		while (true) {
			Object m = queue.get(1000);
			try {
				if (m instanceof Hook) {
					hooking((Hook) m);
				} else if (m instanceof LeakData) {
					alert((LeakData) m);
				} else if (m instanceof Runnable) {
					process((Runnable) m);
				}
			} catch (Throwable t) {
			}
		}
	}

	private void process(Runnable m) {
		m.run();
	}

	private void alert(LeakData m) {
		ServiceSummary summary = ServiceSummary.getInstance();
		if (m.fullstack) {
			ErrorData d = summary.process(m.error, 0, m.service, m.txid, 0, 0);
			Logger.println("A156", m.error + " " + m.inner);
			if (d != null && d.fullstack == 0) {
				String fullstack = ThreadUtil.getStackTrace(m.error.getStackTrace(), 2);
				d.fullstack = DataProxy.sendError(fullstack);
				Logger.println("A157", fullstack);
			}
		} else {
			summary.process(m.error, 0, m.service, m.txid, 0, 0);
			Logger.println("A156", m.error + " " + m.inner);
		}
	}

	private void hooking(Hook m) {
		// AIX JDK1.5에서는 Dynamic Hooking을 사용하면 안됨
		if (SystemUtil.IS_AIX && SystemUtil.IS_JAVA_1_5) {
			return;
		}
		try {
			Class cls = Class.forName(m.classname, false, m.loader);
			ClassDefinition[] cd = new ClassDefinition[1];
			cd[0] = new ClassDefinition(cls, m.body);
			JavaAgent.getInstrumentation().redefineClasses(cd);
		} catch (Throwable t) {
			Logger.println("A149", "async hook fail:" + m.classname + " " + t);
		}
	}

}
