/*
 *  Copyright 2016 the original author or authors. 
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
package scouter.agent.batch.netio.service.net;

import java.util.concurrent.Executor;

import scouter.agent.batch.Configure;
import scouter.util.ThreadUtil;

public class TcpRequestMgr extends Thread {

	private static TcpRequestMgr instance;

	public static synchronized TcpRequestMgr getInstance() {
		if (instance == null) {
			instance = new TcpRequestMgr();
			instance.setName("SCOUTER-TCP");
			instance.setDaemon(true);
			instance.start();
		}
		return instance;
	}

	protected Executor pool = ThreadUtil.createExecutor("SCOUTER", 10, 10000, true);

	@Override
	public void run() {

		while (true) {
			int sessionCount = Configure.getInstance().net_collector_tcp_session_count;
			ThreadUtil.sleep(1000);
			try {
				//System.out.println(sessionCount + ":" + TcpWorker.LIVE.size());
				for (int i = 0; i < sessionCount && TcpWorker.LIVE.size() < sessionCount; i++) {
					TcpWorker w = new TcpWorker();
					if (w.prepare()) {
						pool.execute(w);
					} else {
						ThreadUtil.sleep(3000);
					}
				}
				while (TcpWorker.LIVE.size() > sessionCount) {
					TcpWorker w = TcpWorker.LIVE.removeFirst();
					w.close();
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
}
