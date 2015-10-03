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
package scouter.server.plugin;
import scouter.lang.pack.XLogPack;
import scouter.lang.pack.XLogProfilePack;
import scouter.server.Logger;
import scouter.server.core.CoreRun;
import scouter.util.RequestQueue;
import scouter.util.ThreadUtil;
public class PlugXLogProfileBuf extends Thread {
	private static PlugXLogProfileBuf instance = null;
	public final static synchronized PlugXLogProfileBuf getInstance() {
		if (instance == null) {
			instance = new PlugXLogProfileBuf();
			instance.setDaemon(true);
			instance.setName(ThreadUtil.getName(instance));
			instance.start();
		}
		return instance;
	}
	protected PlugXLogProfileBuf() {
	}
	private RequestQueue<XLogProfilePack> queue = new RequestQueue<XLogProfilePack>(CoreRun.MAX_QUE_SIZE());
	public int getQueueSize() {
		return queue.size();
	}
	public boolean add(XLogProfilePack p) {
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
			XLogProfilePack m = queue.get();
			PlugInManager.profile(m);
		}
	}
}
