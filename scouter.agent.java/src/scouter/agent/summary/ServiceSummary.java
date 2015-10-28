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
package scouter.agent.summary;

import java.util.Enumeration;

import scouter.agent.Configure;
import scouter.agent.netio.data.DataProxy;
import scouter.io.DataInputX;
import scouter.lang.SummaryEnum;
import scouter.lang.pack.SummaryPack;
import scouter.lang.pack.XLogPack;
import scouter.lang.step.ApiCallStep;
import scouter.lang.step.SqlStep;
import scouter.lang.value.ListValue;
import scouter.util.BitUtil;
import scouter.util.IPUtil;
import scouter.util.IntIntLinkedMap;
import scouter.util.IntKeyLinkedMap;
import scouter.util.LongKeyLinkedMap;

public class ServiceSummary {

	private static ServiceSummary instance = null;

	public final static synchronized ServiceSummary getInstance() {
		if (instance == null) {
			instance = new ServiceSummary();
		}
		return instance;
	}

	private Configure conf = Configure.getInstance();

	public void process(XLogPack p) {
		if (conf.enable_summary == false)
			return;
		// service summary
		SummaryData d = getSummaryMap(serviceMaster, p.service);
		d.count++;
		d.elapsed += p.elapsed;
		if (p.error != 0) {
			d.error_cnt++;
		}
		d.cpu += p.cpu;
		d.mem += p.bytes;

		// ip summary
		if (IPUtil.isOK(p.ipaddr) && p.ipaddr[0] != 0 && p.ipaddr[0] != 127) {
			int ip = DataInputX.toInt(p.ipaddr, 0);
			ipMaster.put(ip, ipMaster.get(ip) + 1);
		}
		// user-agent summary
		if (p.userAgent != 0) {
			uaMaster.put(p.userAgent, uaMaster.get(p.userAgent) + 1);
		}
	}
	
	public ErrorData process(Throwable p, int service, long txid, int sql, int api) {
		if (conf.enable_summary == false)
			return null;
		String exceptionName = p.getClass().getName();
		
		int errHash = DataProxy.sendError(exceptionName);
		ErrorData d = getSummaryError(errorMaster, BitUtil.composite(errHash,service));
		d.error = errHash;
		d.service = service;
		d.count++;
		d.txid = txid;

		if (sql != 0)
			d.sql = sql;
		if (api != 0)
			d.apicall = api;
		return d;
	}

	public void process(SqlStep sqlStep) {
		if (conf.enable_summary == false)
			return;
		SummaryData d = getSummaryMap(sqlMaster, sqlStep.hash);
		d.count++;
		d.elapsed += sqlStep.elapsed;
		if (sqlStep.error != 0) {
			d.error_cnt++;
		}
	}

	public void process(ApiCallStep apiStep) {
		if (conf.enable_summary == false)
			return;
		SummaryData d = getSummaryMap(apiMaster, apiStep.hash);
		d.count++;
		d.elapsed += apiStep.elapsed;
		if (apiStep.error != 0) {
			d.error_cnt++;
		}
	}

	private synchronized SummaryData getSummaryMap(IntKeyLinkedMap<SummaryData> table, int hash) {
		IntKeyLinkedMap<SummaryData> tempTable = table;
		SummaryData d = tempTable.get(hash);
		if (d == null) {
			d = new SummaryData();
			tempTable.put(hash, d);
		}
		return d;
	}

	private synchronized ErrorData getSummaryError(LongKeyLinkedMap<ErrorData> table, long key) {

		LongKeyLinkedMap<ErrorData> tempTable = table;
		ErrorData d = tempTable.get(key);
		if (d == null) {
			d = new ErrorData();
			tempTable.put(key, d);
		}
		return d;
	}

	private LongKeyLinkedMap<ErrorData> errorMaster = new LongKeyLinkedMap<ErrorData>()
			.setMax(conf.summary_service_error_max);

	private IntKeyLinkedMap<SummaryData> sqlMaster = new IntKeyLinkedMap<SummaryData>().setMax(conf.summary_sql_max);
	private IntKeyLinkedMap<SummaryData> apiMaster = new IntKeyLinkedMap<SummaryData>().setMax(conf.summary_api_max);
	private IntKeyLinkedMap<SummaryData> serviceMaster = new IntKeyLinkedMap<SummaryData>()
			.setMax(conf.summary_service_max);
	private IntIntLinkedMap ipMaster = new IntIntLinkedMap().setMax(conf.summary_service_ip_max);
	private IntIntLinkedMap uaMaster = new IntIntLinkedMap().setMax(conf.summary_service_ua_max);

	public SummaryPack getAndClear(byte type) {
		IntKeyLinkedMap<SummaryData> temp;
		switch (type) {
		case SummaryEnum.APP:
			if (serviceMaster.size() == 0)
				return null;
			temp = serviceMaster;
			serviceMaster = new IntKeyLinkedMap<SummaryData>().setMax(conf.summary_service_max);
			break;
		case SummaryEnum.SQL:
			if (sqlMaster.size() == 0)
				return null;
			temp = sqlMaster;
			sqlMaster = new IntKeyLinkedMap<SummaryData>().setMax(conf.summary_sql_max);
			break;
		case SummaryEnum.APICALL:
			if (apiMaster.size() == 0)
				return null;
			temp = apiMaster;
			apiMaster = new IntKeyLinkedMap<SummaryData>().setMax(conf.summary_api_max);
			break;
		default:
			return null;
		}

		SummaryPack p = new SummaryPack();
		p.stype = type;

		int cnt = temp.size();
		ListValue id = p.table.newList("id");
		ListValue count = p.table.newList("count");
		ListValue errorCnt = p.table.newList("error");
		ListValue elapsedSum = p.table.newList("elapsed");

		ListValue cpu = null;
		ListValue mem = null;
		if (SummaryEnum.APP == type) {
			cpu = p.table.newList("cpu");
			mem = p.table.newList("mem");
		}
		Enumeration<IntKeyLinkedMap.ENTRY> en = temp.entries();
		for (int i = 0; i < cnt; i++) {
			IntKeyLinkedMap.ENTRY<SummaryData> ent = en.nextElement();
			int key = ent.getKey();
			SummaryData data = ent.getValue();
			id.add(key);
			count.add(data.count);
			errorCnt.add(data.error_cnt);
			elapsedSum.add(data.elapsed);
			if (SummaryEnum.APP == type) {
				cpu.add(data.cpu);
				mem.add(data.mem);
			}
		}
		return p;
	}

	public SummaryPack getAndClearX(byte type) {
		IntIntLinkedMap temp;
		switch (type) {
		case SummaryEnum.IP:
			if (ipMaster.size() == 0)
				return null;
			temp = ipMaster;
			ipMaster = new IntIntLinkedMap().setMax(conf.summary_service_ip_max);
			break;
		case SummaryEnum.USER_AGENT:
			if (uaMaster.size() == 0)
				return null;
			temp = uaMaster;
			uaMaster = new IntIntLinkedMap().setMax(conf.summary_service_ua_max);
			break;
		default:
			return null;
		}

		SummaryPack p = new SummaryPack();
		p.stype = type;

		int cnt = temp.size();
		ListValue id = p.table.newList("id");
		ListValue count = p.table.newList("count");

		Enumeration<IntIntLinkedMap.ENTRY> en = temp.entries();
		for (int i = 0; i < cnt; i++) {
			IntIntLinkedMap.ENTRY ent = en.nextElement();
			int key = ent.getKey();
			int value = ent.getValue();
			id.add(key);
			count.add(value);
		}
		return p;
	}

	public SummaryPack getAndClearError(byte type) {
		if(errorMaster.size()==0)
			return null;
		
		LongKeyLinkedMap<ErrorData> temp = errorMaster;
		errorMaster = new LongKeyLinkedMap<ErrorData>().setMax(conf.summary_service_error_max);

		SummaryPack p = new SummaryPack();
		p.stype = type;

		int cnt = temp.size();
		ListValue id = p.table.newList("id");
		ListValue error = p.table.newList("error");
		ListValue service = p.table.newList("service");

		ListValue count = p.table.newList("count");
		ListValue txid = p.table.newList("txid");
		ListValue sql = p.table.newList("sql");
		ListValue apicall = p.table.newList("apicall");
		ListValue fullstack = p.table.newList("fullstack");

		Enumeration<LongKeyLinkedMap.ENTRY> en = temp.entries();
		for (int i = 0; i < cnt; i++) {
			LongKeyLinkedMap.ENTRY<ErrorData> ent = en.nextElement();
			long key = ent.getKey();
			ErrorData data = ent.getValue();
			id.add(key);
			error.add(data.error);
			service.add(data.service);
			count.add(data.count);
			txid.add(data.txid);
			sql.add(data.sql);
			apicall.add(data.apicall);
			fullstack.add(data.fullstack);
		}
		return p;
	}
}