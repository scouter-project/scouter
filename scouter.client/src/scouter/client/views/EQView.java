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
package scouter.client.views;

import java.io.IOException;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;

import scouter.client.model.AgentModelThread;
import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ScouterUtil;
import scouter.lang.counters.CounterEngine;
import scouter.lang.pack.MapPack;
import scouter.io.DataInputX;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;

public class EQView extends EQCommonView {
	public static final String ID = EQView.class.getName();
	
	private int serverId;
	protected String objType;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String[] ids = site.getSecondaryId().split("&");
		serverId = Integer.valueOf(ids[0]);
		objType = ids[1];
	}

	public void createPartControl(Composite parent) {
		Server server = ServerManager.getInstance().getServer(serverId);
		if (server != null) {
			CounterEngine counterEngine = server.getCounterEngine();
			String displayName = counterEngine.getDisplayNameObjectType(objType);
			this.setPartName("Active Service EQ - " + displayName);
		}
		super.createPartControl(parent);
	}

	public void fetch() {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			MapPack param = new MapPack();
			param.put("objType", objType);
			tcp.process(RequestCmd.ACTIVESPEED_REAL_TIME, param, new INetReader() {
				public void process(DataInputX in) throws IOException {
					MapPack m = (MapPack) in.readPack();
					ActiveSpeedData asd = new ActiveSpeedData();
					asd.act1 = CastUtil.cint(m.get("act1"));
					asd.act2 = CastUtil.cint(m.get("act2"));
					asd.act3 = CastUtil.cint(m.get("act3"));
					int objHash = CastUtil.cint(m.get("objHash"));
					EqData data = new EqData();
					data.objHash = objHash;
					data.asd = asd;
					data.displayName = ScouterUtil.getFullObjName(objHash);
					data.isAlive = AgentModelThread.getInstance().getAgentObject(objHash).isAlive();
					valueSet.add(data);
				}
			});
		} catch (Throwable t) {
			ConsoleProxy.errorSafe(t.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
	}
}
