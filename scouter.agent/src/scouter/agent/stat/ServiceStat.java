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
 */

package scouter.agent.stat;

import java.io.IOException;

import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.io.DataOutputX;
import scouter.lang.SummaryEnum;
import scouter.lang.pack.MapPack;
import scouter.lang.step.ApiCallStep;
import scouter.lang.step.SqlStep;
import scouter.lang.value.ListValue;
import scouter.net.TcpFlag;
import scouter.util.BitUtil;
import scouter.util.DateUtil;
import scouter.util.IntKeyLinkedMap;
import scouter.util.LongKeyLinkedMap;

public class ServiceStat {

	private static ServiceStat instance = null;

	public final static synchronized ServiceStat getInstance() {
		if (instance == null) {
			instance = new ServiceStat();
		}
		return instance;
	}

	private Configure conf = Configure.getInstance();

	public void process(int service, SqlStep sqlStep) {
		if (conf.enable_statistics == false)
			return;

		StatData sql = getStatMap(sqlMaster, sqlStep.hash);

		sql.count++;
		sql.elapsed += sqlStep.elapsed;
		if (sqlStep.error != 0) {
			sql.error_cnt++;
		}
		// /////////////////////
		StatData sqlType = getStatMap(appSqlMaster, service, sqlStep.hash);
		sqlType.count++;
		sqlType.elapsed += sqlStep.elapsed;
		if (sqlStep.error != 0) {
			sqlType.error_cnt++;
		}
	}

	public void process(int service, ApiCallStep apiStep) {
		if (conf.enable_statistics == false)
			return;
		StatData api = getStatMap(apiMaster, apiStep.hash);
		api.count++;
		api.elapsed += apiStep.elapsed;
		if (apiStep.error != 0) {
			api.error_cnt++;
		}
		// /////////////////////
		StatData apiType = getStatMap(appApiMaster, service, apiStep.hash);
		apiType.count++;
		apiType.elapsed += apiStep.elapsed;
		if (apiStep.error != 0) {
			apiType.error_cnt++;
		}
	}

	private StatData getStatMap(IntKeyLinkedMap<StatData> table, int sqlHash) {
		StatData d = table.get(sqlHash);
		if (d == null) {
			d = new StatData();
			table.put(sqlHash, d);
		}
		return d;
	}

	private StatData getStatMap(LongKeyLinkedMap<StatData> table, int serviceHash, int sqlHash) {
		long key = BitUtil.compsite(serviceHash, sqlHash);
		StatData d = table.get(key);
		if (d == null) {
			d = new StatData();
			table.put(key, d);
		}
		return d;
	}

	private IntKeyLinkedMap<StatData> sqlMaster = new IntKeyLinkedMap<StatData>().setMax(conf.stat_sql_max);
	private IntKeyLinkedMap<StatData> apiMaster = new IntKeyLinkedMap<StatData>().setMax(conf.stat_api_max);
	private LongKeyLinkedMap<StatData> appSqlMaster = new LongKeyLinkedMap<StatData>().setMax(conf.stat_app_sql_max);
	private LongKeyLinkedMap<StatData> appApiMaster = new LongKeyLinkedMap<StatData>().setMax(conf.stat_app_api_max);

	
	long last_flush_time=System.currentTimeMillis();
	public void flush(DataOutputX out) {

		IntKeyLinkedMap<StatData> sqlTemp = sqlMaster;
		sqlMaster = new IntKeyLinkedMap<StatData>().setMax(conf.stat_sql_max);

		IntKeyLinkedMap<StatData> apiTemp = apiMaster;
		apiMaster = new IntKeyLinkedMap<StatData>().setMax(conf.stat_api_max);

		LongKeyLinkedMap<StatData> appSqlTemp = appSqlMaster;
		appSqlMaster = new LongKeyLinkedMap<StatData>().setMax(conf.stat_app_sql_max);

		LongKeyLinkedMap<StatData> appApiTemp = appApiMaster;
		appApiMaster = new LongKeyLinkedMap<StatData>().setMax(conf.stat_app_api_max);

		//너무 오랜시간동안 데이터를 조회해 가지 않다가 새로 조회 요청이 들어오면 첫번데이터는 버린다.
		long now = System.currentTimeMillis();
		if(now - last_flush_time > DateUtil.MILLIS_PER_HOUR){
			last_flush_time=now;
			return;
		}
		last_flush_time=now;
		
		try {
			//
			flush(SummaryEnum.APP_SQL, "sql", out, appSqlTemp);
			flush(SummaryEnum.APP_APICALL, "api", out, appApiTemp);
			flush(SummaryEnum.SQL, "sql", out, sqlTemp);
			flush(SummaryEnum.APICALL, "api", out, apiTemp);
			//
		} catch (Exception e) {
			Logger.println("STAT", e.toString());
		}
	}

	private void flush(byte type, String name, DataOutputX out, IntKeyLinkedMap<StatData> table) throws IOException {
		if (table.size() == 0)
			return;

		MapPack p = new MapPack();
		p.put("_pack_", "stat");
		p.put("objHash", Configure.getInstance().objHash);
		p.put("type", type);
		ListValue sqlHash = p.newList(name);
		ListValue count = p.newList("count");
		ListValue errorCnt = p.newList("errorCnt");
		ListValue elapsedSum = p.newList("elapsedSum");

		while (table.size() > 0) {
			int key = table.getFirstKey();
			StatData data = table.remove(key);
			sqlHash.add(key);
			count.add(data.count);
			errorCnt.add(data.error_cnt);
			elapsedSum.add(data.elapsed);
		}
		out.writeByte(TcpFlag.HasNEXT);
		out.writePack(p);
		out.flush();
	}

	private void flush(byte type, String name, DataOutputX out, LongKeyLinkedMap<StatData> table) throws IOException {
		if (table.size() == 0)
			return;

		MapPack p = new MapPack();
		p.put("_pack_", "stat");
		p.put("objHash", Configure.getInstance().objHash);
		p.put("type", type);
		ListValue service = p.newList("app");
		ListValue toHash = p.newList(name);
		ListValue count = p.newList("count");
		ListValue errorCnt = p.newList("errorCnt");
		ListValue elapsedSum = p.newList("elapsedSum");

		while (table.size() > 0) {
			long key = table.getFirstKey();
			StatData data = table.remove(key);
			service.add(BitUtil.getHigh(key));
			toHash.add(BitUtil.getLow(key));
			count.add(data.count);
			errorCnt.add(data.error_cnt);
			elapsedSum.add(data.elapsed);
		}

		out.writeByte(TcpFlag.HasNEXT);
		out.writePack(p);
		out.flush();
	}

}