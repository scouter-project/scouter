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
package scouter.client.util;

import scouter.client.server.Server;
import scouter.client.server.ServerManager;

public class TimeUtil {
	
	public static long getCurrentTime() {
		Server defServer = ServerManager.getInstance().getDefaultServer();
		if (defServer != null) {
			return getCurrentTime(defServer.getId());
		} else {
			return System.currentTimeMillis();
		}
	}
	
	public static long getCurrentTime(int serverId) {
		Server server = ServerManager.getInstance().getServer(serverId);
		if (server == null) {
			server = ServerManager.getInstance().getDefaultServer();
			if (server == null) {
				return System.currentTimeMillis();
			}
		}
		return System.currentTimeMillis() + server.getDelta();
	}
}
