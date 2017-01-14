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
package scouter.client.stack.actions;

import org.eclipse.jface.action.Action;

import scouter.client.net.TcpProxy;
import scouter.client.util.ExUtil;
import scouter.lang.pack.MapPack;
import scouter.net.RequestCmd;
import scouter.util.DateUtil;

public class TurnOffStackAction extends Action {
	public final static String ID = TurnOffStackAction.class.getName();
	
	int serverId;
	int objHash;

	public TurnOffStackAction(int serverId, int objHash) {
		this.serverId = serverId;
		this.objHash = objHash;
		this.setText("Turn off");
	}
	
	public void run(){
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				try {
					MapPack param = new MapPack();
					param.put("objHash", objHash);
					tcp.getSingle(RequestCmd.PSTACK_ON, param);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				
			}
		});
	}
}