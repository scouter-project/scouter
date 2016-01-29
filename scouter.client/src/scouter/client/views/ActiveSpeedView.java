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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;

import scouter.client.model.RefreshThread;
import scouter.client.net.TcpProxy;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ConsoleProxy;
import scouter.lang.pack.MapPack;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.StringUtil;


public class ActiveSpeedView extends ActiveSpeedCommonView {

	public static final String ID = ActiveSpeedView.class.getName();
	
	private String objType;
	private int serverId;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String[] ids = StringUtil.split(secId, "&");
		this.serverId = CastUtil.cint(ids[0]);
		this.objType = ids[1];
	}

	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		Server server = ServerManager.getInstance().getServer(serverId);
		String objTypeDisplay = "";
		if(server != null){
			objTypeDisplay = server.getCounterEngine().getDisplayNameObjectType(objType);
		}
		
		this.setPartName("ActiveSpeed - " + objTypeDisplay);
		thread = new RefreshThread(this, 150);
		thread.start();
		thread.setName(this.toString() + " - " + "objType:"+objType+", serverId:"+serverId);
	}
	
	public void fetch() {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			MapPack param = new MapPack();
			param.put("objType", objType);
			MapPack p = (MapPack) tcp.getSingle(RequestCmd.ACTIVESPEED_REAL_TIME_GROUP, param);
			ActiveSpeedData a = new ActiveSpeedData();
			activeSpeedData = a;
			if(p == null){
				return;
			} else {
				a.act1 =CastUtil.cint(p.get("act1"));
				a.act2 =CastUtil.cint(p.get("act2"));
				a.act3 =CastUtil.cint(p.get("act3"));
				a.tps =CastUtil.cfloat(p.get("tps"));
			}
		} catch(Exception e){
			ConsoleProxy.errorSafe(e.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
	}
}
