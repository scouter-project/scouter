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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;

import scouter.client.model.AgentModelThread;
import scouter.client.model.AgentObject;
import scouter.client.net.TcpProxy;
import scouter.client.server.ServerManager;
import scouter.lang.counters.CounterConstants;
import scouter.lang.counters.CounterEngine;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.net.RequestCmd;

public class CounterMapStackView extends CounterStackCommonView {
	
	public static final String ID = CounterMapStackView.class.getName();

	int serverId;
	int objHash;
	String objType;
	String title;
	List<String> counters = new ArrayList<String>();
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String[] ids = secId.split("&");
		serverId = Integer.valueOf(ids[0]);
		objHash = Integer.valueOf(ids[1]);
		title = ids[2];
		for (int i = 3; i < ids.length; i++) {
			counters.add(ids[i]);
		}
	}
	
	public void createPartControl(Composite parent) {
		AgentObject agent = AgentModelThread.getInstance().getAgentObject(objHash);
		if (agent == null) {
			this.objType = CounterConstants.TOMCAT;
			this.setPartName(title + "[" + objHash + "]");
		} else {
			this.objType = agent.getObjType();
			this.setPartName(title + "[" + agent.getObjName() + "]");
		}
		super.createPartControl(parent);
	}

	protected MapValue fetch() {
		Pack p = null; 
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			MapPack param = new MapPack();
			param.put("objHash", objHash);
			ListValue counterLv = param.newList("counter");
			for (String counter : counters) {
				counterLv.add(counter);
			}
			p = tcp.getSingle(RequestCmd.COUNTER_REAL_TIME_MULTI, param);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		
		if (p == null) return null;
		CounterEngine engine = ServerManager.getInstance().getServer(serverId).getCounterEngine();
		
		MapPack pack = (MapPack)p;
		ListValue counterLv = pack.getList("counter");
		ListValue valueLv = pack.getList("value");
		MapValue mapValue = new MapValue();
		for (int i = 0; i < counterLv.size(); i++) {
			String counter = engine.getCounterDisplayName(objType, counterLv.getString(i));
			mapValue.put(counter, valueLv.get(i));
		}
		return mapValue;
	}
}
