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
 *
 */
package scouter.client.views;

import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;

import scouter.client.model.AgentModelThread;
import scouter.client.model.AgentObject;
import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.client.server.ServerManager;
import scouter.util.StringUtil;
import scouter.lang.counters.CounterEngine;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.lang.value.Value;
import scouter.io.DataInputX;
import scouter.net.RequestCmd;

public class CounterMapStackView extends CounterStackCommonView {
	
	public static final String ID = CounterMapStackView.class.getName();

	int serverId;
	int objHash;
	String counter;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String[] ids = secId.split("&");
		serverId = Integer.valueOf(ids[0]);
		objHash = Integer.valueOf(ids[1]);
		counter = ids[2];
	}
	
	public void createPartControl(Composite parent) {
		AgentObject agent = AgentModelThread.getInstance().getAgentObject(objHash);
		CounterEngine counterEngine = ServerManager.getInstance().getServer(serverId).getCounterEngine();
		if (agent == null) {
			this.setPartName(counter + "[" + objHash + "]");
		} else {
			String display = counterEngine.getCounterDisplayName(agent.getObjType(), counter);
			if (StringUtil.isEmpty(display)) {
				display = counter;
			}
			this.setPartName(display + "[" + agent.getObjName() + "]");
		}
		super.createPartControl(parent);
	}

	protected MapValue fetch() {
		final ArrayList<MapValue> list = new ArrayList<MapValue>(); 
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			MapPack param = new MapPack();
			ListValue objHashLv = param.newList("objHash");
			objHashLv.add(objHash);
			param.put("counter", counter);
			tcp.process(RequestCmd.COUNTER_MAP_REAL_TIME, param, new INetReader() {
				public void process(DataInputX in) throws IOException {
					Value v = in.readValue();
					if (v != null) {
						list.add((MapValue)v);
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		
		return list.size() > 0 ? list.get(0) : null;
	}
}
