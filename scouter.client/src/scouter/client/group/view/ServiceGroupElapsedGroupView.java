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

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;

import scouter.client.net.TcpProxy;
import scouter.client.util.ScouterUtil;
import scouter.client.util.TimeUtil;
import scouter.client.views.AbstractServiceGroupElapsedView;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;

public class ServiceGroupElapsedGroupView extends AbstractServiceGroupElapsedView {
	
	public final static String ID = ServiceGroupElapsedGroupView.class.getName();
	
	String grpName;
	private Map<Integer, ListValue> serverObjMap = new HashMap<Integer, ListValue>();
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		grpName = secId;
	}

	public void createPartControl(Composite parent) {
		this.setPartName("Service[Elapsed] - " + grpName);
		super.createPartControl(parent);
	}

	@Override
	public MapPack fetch() {
		ScouterUtil.collectGroupObjcts(grpName, serverObjMap);
		HashMap<String, PerfStat> valueMap = new HashMap<String, PerfStat>();
		Iterator<Integer> itr = serverObjMap.keySet().iterator();
		while (itr.hasNext()) {
			int serverId = itr.next();
			TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
			try {
				MapPack param = new MapPack();
				param.put("objHash", serverObjMap.get(serverId));
				MapPack p = (MapPack) tcp.getSingle(RequestCmd.REALTIME_SERVICE_GROUP, param);
				if (p != null) {
					ListValue nameLv = p.getList("name");
					ListValue countLv = p.getList("count");
					ListValue elapsedLv = p.getList("elapsed");
					ListValue errorLv = p.getList("error");
					for (int i = 0, max = (nameLv == null ? 0 : nameLv.size()) ; i < max; i++) {
						String name = nameLv.getString(i);
						PerfStat perf = valueMap.get(name);
						if (perf == null) {
							perf = new PerfStat();
							valueMap.put(name, perf);
						}
						perf.count += CastUtil.cint(countLv.get(i));
						perf.elapsed += CastUtil.clong(elapsedLv.get(i));
						perf.error += CastUtil.cint(errorLv.get(i));
					}
				}
			} catch (Throwable th) {
				th.printStackTrace();
			} finally {
				TcpProxy.putTcpProxy(tcp);
			}
		}
		MapPack m = null;
		if (valueMap.size() > 0) {
			m = new MapPack();
			ListValue nameLv = m.newList("name");
			ListValue countLv = m.newList("count");
			ListValue elapsedLv = m.newList("elapsed");
			ListValue errorLv = m.newList("error");
			Iterator<String> itrr = valueMap.keySet().iterator();
			while (itrr.hasNext()) {
				String name = itrr.next();
				PerfStat perf = valueMap.get(name);
				nameLv.add(name);
				countLv.add(perf.count);
				elapsedLv.add(perf.elapsed);
				errorLv.add(perf.error);
			}
			long time = TimeUtil.getCurrentTime();
			m.put("time", time);
		}
		return m;
	}
	
	public static class PerfStat {
		public int count;
		public int error;
		public long elapsed;

		public void add(PerfStat o) {
			this.count += o.count;
			this.error += o.error;
			this.elapsed += o.elapsed;
		}
	}
}
