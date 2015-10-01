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

package scouter.agent.util;

import scouter.util.Queue;
import scouter.util.ThreadUtil;

public class AsyncRun extends Thread {

	private static AsyncRun instance = null;

	public final static synchronized AsyncRun getInstance() {
		if (instance == null) {
			instance = new AsyncRun();
			instance.setDaemon(true);
			instance.setName(ThreadUtil.getName(instance));
			instance.start();
		}
		return instance;
	}

	private Queue<Runnable> execute = new Queue<Runnable>(128);

	public void add(Runnable r) {
		execute.enqueue(r);
	}

	public void run() {
		while (true) {
			ThreadUtil.sleep(1000);
			while (execute.size() > 0) {
				try {
					Runnable r = execute.dequeue();
					r.run();
				} catch (Throwable t) {
				}
			}
			//Configure.getInstance().reload();
			//CounterSendProxy.flush();
		}
	}

}