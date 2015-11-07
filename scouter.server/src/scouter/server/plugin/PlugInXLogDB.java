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
package scouter.server.plugin;
import scouter.lang.pack.XLogPack;
import scouter.server.Logger;
import scouter.server.core.CoreRun;
import scouter.util.RequestQueue;
import scouter.util.ThreadUtil;
public class PlugInXLogDB extends Thread {
	private static PlugInXLogDB instance = null;
	public final static synchronized PlugInXLogDB getInstance() {
		if (instance == null) {
			instance = new PlugInXLogDB();
			instance.setDaemon(true);
			instance.setName(ThreadUtil.getName(instance));
			instance.start();
		}
		return instance;
	}
	protected PlugInXLogDB() {
	}
	private RequestQueue<XLogPack> queue = new RequestQueue<XLogPack>(CoreRun.MAX_QUE_SIZE());
	public int getQueueSize() {
		return queue.size();
	}
	public boolean add(XLogPack p) {
		Object ok = queue.put(p);
		if (ok == null) {
			Logger.println("S203", 10, "profile queue exceeded!!");
			return false;
		}
		return true;
	}
	public boolean isQueueOk() {
		return queue.size() < CoreRun.MAX_QUE_SIZE();
	}
	public void shutdown() {
		running = false;
	}
	private boolean running = true;
	public void run() {
		while (running) {
			XLogPack m = queue.get();
			PlugInManager.xlogdb(m);
		}
	}
}
