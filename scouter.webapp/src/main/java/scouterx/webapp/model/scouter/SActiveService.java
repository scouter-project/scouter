package scouterx.webapp.model.scouter;
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
 */

import lombok.Data;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.ListValue;
import scouter.util.DateUtil;
import scouter.util.Hexa32;
import scouter.util.StringUtil;
import scouterx.webapp.framework.client.model.AgentModelThread;
import scouterx.webapp.framework.client.model.AgentObject;
import scouterx.webapp.model.enums.ActiveServiceMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Gun Lee(gunlee01@gmail.com) on 2017. 9. 11.
 */
@Data
public class SActiveService {
	int objHash;
	String objName;
	long threadId;
	String threadName;
	String threadStatus;
	long threadCpuTime;
	String txidName;

	String activeDate;
	long txid;

	long elapsed;
	String serviceName;
	String ipaddr;
	String note;
	String mode = ActiveServiceMode.NONE.name();

	public static List<SActiveService> ofPackList(List<Pack> activeServicePackList) {
		List<SActiveService> resultList = new ArrayList<>();

		for (Pack pack : activeServicePackList) {
			MapPack mapPack = (MapPack) pack;
			int objHash = mapPack.getInt("objHash");
			AgentObject agentObject = AgentModelThread.getInstance().getAgentObject(objHash);
			if (agentObject == null) {
				continue;
			}
			String objName = agentObject.getObjName();

			ListValue idLv = mapPack.getList("id");
			ListValue nameLv = mapPack.getList("name");
			ListValue statLv = mapPack.getList("stat");
			ListValue cpuLv = mapPack.getList("cpu");
			ListValue txidLv = mapPack.getList("txid");
			ListValue elapsedLv = mapPack.getList("elapsed");
			ListValue serviceLv = mapPack.getList("service");
			ListValue ipLv = mapPack.getList("ip");
			ListValue sqlLv = mapPack.getList("sql");
			ListValue subcallLv = mapPack.getList("subcall");

			if (idLv != null) {
				int size = idLv.size();

				for (int i = 0; i < size; i++) {
					SActiveService activeService = new SActiveService();
					activeService.objHash = objHash;
					activeService.objName = objName;
					activeService.threadId = idLv.getLong(i);
					activeService.threadName = nameLv.getString(i);
					activeService.threadStatus = statLv.getString(i);
					activeService.threadCpuTime = cpuLv.getLong(i);
					activeService.txidName = txidLv.getString(i);
					activeService.elapsed = elapsedLv.getLong(i);
					activeService.serviceName = serviceLv.getString(i);
					String sql = sqlLv.getString(i);
					String api = subcallLv.getString(i);
					if (StringUtil.isNotEmpty(sql)) {
						activeService.note = sql;
						activeService.mode = ActiveServiceMode.SQL.name();
					} else if (StringUtil.isNotEmpty(api)) {
						activeService.note = api;
						activeService.mode = ActiveServiceMode.SUBCALL.name();
					}
					if (ipLv != null) {
						activeService.ipaddr = ipLv.getString(i);
					}
					//- 액티브 추적용  TXID 변환 추가
					activeService.txid = Hexa32.toLong32(activeService.txidName);
					activeService.activeDate = DateUtil.yyyymmdd();
					resultList.add(activeService);
				}
			}
			resultList.sort((s1, s2) -> s1.elapsed > s2.elapsed ? -1 : 1);
		}

		return resultList;
	}

}
