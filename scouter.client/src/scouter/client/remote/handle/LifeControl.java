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
package scouter.client.remote.handle;

import org.eclipse.swt.widgets.Display;

import scouter.client.remote.RemoteCmd;
import scouter.client.remote.RemoteHandler;
import scouter.client.util.ExUtil;
import scouter.client.util.RCPUtil;
import scouter.lang.pack.MapPack;

public class LifeControl {
	
	@RemoteHandler(RemoteCmd.EXIT_CLIENT)
	public void exitClient(int serverId, MapPack param) throws Exception {
		ExUtil.exec(Display.getDefault(), new Runnable() {
			public void run() {
				RCPUtil.exit();
			}
		});
	}
	
	@RemoteHandler(RemoteCmd.RESTART_CLIENT)
	public void restartClient(int serverId, MapPack param) throws Exception {
		ExUtil.exec(Display.getDefault(), new Runnable() {
			public void run() {
				RCPUtil.restart();
			}
		});
	}
}
