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

import scouter.client.net.TcpProxy;
import scouter.client.popup.PopupMessageDialog;
import scouter.client.remote.RemoteCmd;
import scouter.client.remote.RemoteHandler;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.lang.counters.CounterEngine;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.BlobValue;
import scouter.lang.value.Value;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;

public class ActionControl {
	
	@RemoteHandler(RemoteCmd.POPUP_MESSAGE)
	public void popUpMessage(int serverId, final MapPack param) throws Exception {
		ExUtil.exec(Display.getDefault(), new Runnable() {
			public void run() {
				Value message = param.get("message");
				Value from = param.get("from");
				Value keepTime = param.get("keeptime");
				if (message != null) {
					if (keepTime == null) {
						new PopupMessageDialog().show(CastUtil.cString(from), CastUtil.cString(message));
					} else {
						new PopupMessageDialog().show(CastUtil.cString(from), CastUtil.cString(message), CastUtil.cint(keepTime));
					}
				}
			}
		});
	}
	
	@RemoteHandler(RemoteCmd.REFETCH_COUNTER_XML)
	public void refetchCounterXml(int serverId, final MapPack param) throws Exception {
		Server server = ServerManager.getInstance().getServer(serverId);
		if (server == null) {
			return;
		}
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		Pack p = null;
		try {
			p = tcp.getSingle(RequestCmd.GET_XML_COUNTER, null);
		} catch (Exception e) {
			ConsoleProxy.errorSafe(e.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		if (p != null) {
			ConsoleProxy.infoSafe("***********************************");
			ConsoleProxy.infoSafe("Refetch counter.xml is successfully read from " + server.getName());
			CounterEngine engine = server.getCounterEngine();
			MapPack m = (MapPack) p;
			engine.clear();
			Value v1 = m.get("default");
			engine.parse(((BlobValue)v1).value);
			v1 = m.get("custom");
			if (v1 != null) {
				engine.parse(((BlobValue)v1).value);
			}
			server.setDirty(true);
			ConsoleProxy.infoSafe("Applied Complete.");
			ConsoleProxy.infoSafe("***********************************");
		}
	}
}
