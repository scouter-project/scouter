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

import java.util.Map;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;

import scouter.client.model.AgentModelThread;
import scouter.client.model.AgentObject;
import scouter.client.net.TcpProxy;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.TimeUtil;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;

public class ServiceGroupTPSView extends AbstractServiceGroupTPSView {
	
	public final static String ID = ServiceGroupTPSView.class.getName();
	
	int serverId;
	String objType;
	String displayObjType;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String ids[] = secId.split("&");
		serverId = Integer.valueOf(ids[0]);
		objType = ids[1];
	}

	public void createPartControl(Composite parent) {
		Server server = ServerManager.getInstance().getServer(serverId);
		if (server != null ) displayObjType = server.getCounterEngine().getDisplayNameObjectType(objType);
		this.setPartName("Service[Throughput] - " + displayObjType);
		super.createPartControl(parent);
	}

	public MapPack fetch() {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		Pack pack = null;
		try {
			MapPack param = new MapPack();
			ListValue objLv = param.newList("objHash");
			Map<Integer, AgentObject> agentMap = AgentModelThread.getInstance().getAgentObjectMap();
			for (AgentObject p : agentMap.values()) {
				if (p.getObjType().equals(objType)) {
					objLv.add(p.getObjHash());
				}
			}
			pack = tcp.getSingle(RequestCmd.REALTIME_SERVICE_GROUP, param);
		} catch (Throwable th) {
			th.printStackTrace();
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		MapPack m = null;
		if (pack != null) {
			m = (MapPack) pack;
			long time = TimeUtil.getCurrentTime(serverId);
			m.put("time", time);
		}
		return m;
	}
}
