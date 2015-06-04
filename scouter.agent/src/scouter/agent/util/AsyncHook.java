/*
 *  Copyright 2015 LG CNS.
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
import scouter.util.RequestQueue;
import scouter.util.SystemUtil;
import scouter.util.ThreadUtil;

public class AsyncHook extends Thread {

	private static AsyncHook instance = null;

	public final static synchronized AsyncHook getInstance() {
		if (instance == null) {
			instance = new AsyncHook();
			instance.setDaemon(true);
			instance.setName(ThreadUtil.getName(instance));
			instance.start();
		}
		return instance;
	}

	private RequestQueue<Hook> execute = new RequestQueue<Hook>(1024);

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
		execute.put(new Hook(loader, classname, body));
	}

	public void run() {
		while (true) {
			Hook m = (Hook) execute.get();
			if (m == null) {
				ThreadUtil.sleep(1000);
				continue;
			}
			//AIX JDK1.5에서는 Dynamic Hooking을 사용하면 안됨 
			if(SystemUtil.IS_AIX && SystemUtil.IS_JAVA_1_5){
				continue;
			}
			try {
				Class cls = Class.forName(m.classname, false, m.loader);
				ClassDefinition[] cd = new ClassDefinition[1];
				cd[0] = new ClassDefinition(cls, m.body);
				JavaAgent.getInstrumentation().redefineClasses(cd);
			} catch (Throwable t) {
				Logger.println("TA045","async hook fail:" + m.classname + " " + t);
			}
		}
	}

}