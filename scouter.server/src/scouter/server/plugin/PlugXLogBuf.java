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

package scouter.server.plugin;

import scouter.lang.pack.XLogPack;
import scouter.server.Logger;
import scouter.server.core.CoreRun;
import scouter.util.RequestQueue;
import scouter.util.ThreadUtil;

public class PlugXLogBuf extends Thread {

	private static PlugXLogBuf instance = null;

	public final static synchronized PlugXLogBuf getInstance() {
		if (instance == null) {
			instance = new PlugXLogBuf();
			instance.setDaemon(true);
			instance.setName(ThreadUtil.getName(instance));
			instance.start();
		}
		return instance;
	}

	protected PlugXLogBuf() {
	}

	private RequestQueue<XLogPack> queue = new RequestQueue<XLogPack>(CoreRun.MAX_QUE_SIZE());

	public int getQueueSize() {
		return queue.size();
	}

	public boolean add(XLogPack p) {
		p.endTime = System.currentTimeMillis();

		Object ok = queue.put(p);
		if (ok == null) {
			Logger.println("XLogPlugIn", 10, "queue exceeded!!");
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
			XLogPack m = (XLogPack) queue.get();
			PlugInManager.xlog(m);
		}
	}

}