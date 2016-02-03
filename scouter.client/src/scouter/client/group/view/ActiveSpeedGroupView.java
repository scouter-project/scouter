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
package scouter.client.group.view;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;

import scouter.client.group.GroupManager;
import scouter.client.model.AgentModelThread;
import scouter.client.model.AgentObject;
import scouter.client.model.RefreshThread;
import scouter.client.net.TcpProxy;
import scouter.client.util.ConsoleProxy;
import scouter.client.views.ActiveSpeedCommonView;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;


public class ActiveSpeedGroupView extends ActiveSpeedCommonView {

	public static final String ID = ActiveSpeedGroupView.class.getName();
	
	private Map<Integer, ListValue> serverObjMap = new HashMap<Integer, ListValue>();
	private String grpName;
	private String objType;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String[] datas = secId.split("&");
		grpName = datas[0];
		objType = datas[1];
	}
	
	public void createPartControl(Composite parent) {
		this.setPartName("ActiveSpeed - " + grpName);
		super.createPartControl(parent);
		thread = new RefreshThread(this, 150);
		thread.start();
		thread.setName(this.toString() + " - " + "grpName:"+grpName);
	}
	
	private GroupManager manager = GroupManager.getInstance();
	
	private void collectObj() {
		serverObjMap.clear();
		Set<Integer> objHashs = manager.getObjectsByGroup(grpName);
		for (int objHash : objHashs) {
			AgentObject agentObj = AgentModelThread.getInstance().getAgentObject(objHash);
			if (agentObj == null || agentObj.isAlive() == false) {
				continue;
			}
			int serverId = agentObj.getServerId();
			ListValue lv = serverObjMap.get(serverId);
			if (lv == null) {
				lv = new ListValue();
				serverObjMap.put(serverId, lv);
			}
			lv.add(objHash);
		}
	}
	
	public void fetch() {
		collectObj();
		Iterator<Integer> itr = serverObjMap.keySet().iterator();
		ActiveSpeedData a = new ActiveSpeedData();
		while (itr.hasNext()) {
			int serverId = itr.next();
			TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
			try {
				MapPack param = new MapPack();
				param.put("objHash", serverObjMap.get(serverId));
				MapPack p = (MapPack) tcp.getSingle(RequestCmd.ACTIVESPEED_GROUP_REAL_TIME_GROUP, param);
				if(p == null){
					continue;
				}
				if (p != null) {
					a.act1 += CastUtil.cint(p.get("act1"));
					a.act2 += CastUtil.cint(p.get("act2"));
					a.act3 += CastUtil.cint(p.get("act3"));
					a.tps += CastUtil.cint(p.get("tps"));
				}
			} catch(Exception e){
				ConsoleProxy.errorSafe(e.toString());
			} finally {
				TcpProxy.putTcpProxy(tcp);
			}
		}
		activeSpeedData = a;
	}
}
