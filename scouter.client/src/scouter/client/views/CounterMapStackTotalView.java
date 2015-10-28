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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;

import scouter.client.model.AgentModelThread;
import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.lang.value.NumberValue;
import scouter.lang.value.Value;
import scouter.io.DataInputX;
import scouter.net.RequestCmd;

public class CounterMapStackTotalView extends CounterStackCommonView {
	
	public static final String ID = CounterMapStackTotalView.class.getName();

	int serverId;
	String objType;
	String counter;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String[] ids = secId.split("&");
		serverId = Integer.valueOf(ids[0]);
		objType =ids[1];
		counter = ids[2];
	}
	
	public void createPartControl(Composite parent) {
		Server server = ServerManager.getInstance().getServer(serverId);
		String displayObjType = server.getCounterEngine().getDisplayNameObjectType(objType);
		String displaycounter = server.getCounterEngine().getCounterDisplayName(objType, counter);
		if (displaycounter == null) {
			displaycounter = counter;
		}
		this.setPartName(displayObjType + " - " + displaycounter + "[" + server.getName() + "]");
		super.createPartControl(parent);
	}

	protected MapValue fetch() {
		final ArrayList<MapValue> list = new ArrayList<MapValue>(); 
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			MapPack param = new MapPack();
			ListValue objHashLv = param.newList("objHash");
			Set<Integer> objSet = AgentModelThread.getInstance().getObjectList(objType);
			for (int hash : objSet) {
				objHashLv.add(hash);
			}
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
		
		return combineMapValue(list);
	}
	
	private MapValue combineMapValue(List<MapValue> list) {
		MapValue mv = new MapValue();
		for (MapValue m : list) {
			Enumeration<String> keys = m.keys();
			while (keys.hasMoreElements()) {
				String key = keys.nextElement();
				Value value = m.get(key);
				if (value instanceof NumberValue == false) {
					continue;
				}
				Value v = mv.get(key);
				if (v == null) {
					mv.put(key, value);
				} else {
					NumberValue nv = (NumberValue) v;
					nv.add((NumberValue) value);
				}
			}
		}
		return mv;
	}

}
