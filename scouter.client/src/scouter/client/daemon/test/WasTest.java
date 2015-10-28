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
package scouter.client.daemon.test;

import scouter.client.daemon.api.WasApi;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.lang.counters.CounterConstants;
import scouter.lang.pack.MapPack;
import scouter.util.ThreadUtil;

public class WasTest {

	public static void test() {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					Server server = ServerManager.getInstance().getDefaultServer();
					if (server != null) {
						MapPack p = WasApi.COUNTER_REAL_TIME_ALL(CounterConstants.WAS_SERVICE_COUNT, "tomcat",
								server.getId());
						System.out.println(p);
					}
					ThreadUtil.sleep(2000);
				}
			}
		});
		t.setDaemon(true);
		t.start();
	}
}
