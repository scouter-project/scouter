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

import scouter.agent.Configure;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.util.SimpleLru;
import scouter.io.DataInputX;
import scouter.lang.SummaryEnum;
import scouter.lang.pack.SummaryPack;
import scouter.lang.pack.XLogPack;
import scouter.lang.step.ApiCallStep;
import scouter.lang.step.SqlStep;
import scouter.lang.value.ListValue;
import scouter.util.BitUtil;
import scouter.util.IPUtil;

import java.util.Map;

public class ServiceSummary {

	private static ServiceSummary instance = null;

	private Configure conf = Configure.getInstance();
	//	private LongKeyLinkedMap<ErrorData> errorMaster = new LongKeyLinkedMap<ErrorData>().setMax(conf._summary_error_max_count);
	private SimpleLru<Long, ErrorData> errorMaster = new SimpleLru<Long, ErrorData>(conf._summary_error_max_count);

	//	private IntKeyLinkedMap<SummaryData> sqlMaster = new SimpleLru<SummaryData>().setMax(conf._summary_sql_max_count);
	private SimpleLru<Integer, SummaryData> sqlMaster = new SimpleLru<Integer, SummaryData>(conf._summary_sql_max_count);

	//	private IntKeyLinkedMap<SummaryData> apiMaster = new IntKeyLinkedMap<SummaryData>().setMax(conf._summary_api_max_count);
	private SimpleLru<Integer, SummaryData> apiMaster = new SimpleLru<Integer, SummaryData>(conf._summary_api_max_count);

	//	private IntKeyLinkedMap<SummaryData> serviceMaster = new IntKeyLinkedMap<SummaryData>().setMax(conf._summary_service_max_count);
	private SimpleLru<Integer, SummaryData> serviceMaster = new SimpleLru<Integer, SummaryData>(conf._summary_service_max_count);

	//	private IntIntLinkedMap ipMaster = new IntIntLinkedMap().setMax(conf._summary_ip_max_count);
	private SimpleLru<Integer, Integer> ipMaster = new SimpleLru<Integer, Integer>(conf._summary_ip_max_count);

	//	private IntIntLinkedMap uaMaster = new IntIntLinkedMap().setMax(conf._summary_useragent_max_count);
	private SimpleLru<Integer, Integer> uaMaster = new SimpleLru<Integer, Integer>(conf._summary_useragent_max_count);

	public final static synchronized ServiceSummary getInstance() {
		if (instance == null) {
			instance = new ServiceSummary();
		}
		return instance;
	}

	public void process(XLogPack p) {
		if (conf.summary_enabled == false)
			return;
		// service summary
		SummaryData d = getSummaryMap(serviceMaster, p.service);
		d.count++;
		d.elapsed += p.elapsed;
		if (p.error != 0) {
			d.error_cnt++;
		}
		d.cpu += p.cpu;
		d.mem += p.kbytes;

		// ip summary
		if (IPUtil.isOK(p.ipaddr) && p.ipaddr[0] != 0 && p.ipaddr[0] != 127) {
			int ip = DataInputX.toInt(p.ipaddr, 0);
			Integer v = ipMaster.get(ip);
			if (v == null) {
				v = 0;
			}
			ipMaster.put(ip, v + 1);
		}
		// user-agent summary
		if (p.userAgent != 0) {
			Integer v = uaMaster.get(p.userAgent);
			if (v == null) {
				v = 0;
			}
			uaMaster.put(p.userAgent, v + 1);
		}
	}

	public ErrorData process(Throwable thr, int message, int service, long txid, int sql, int api) {
		if (conf.summary_enabled == false)
			return null;
		if (thr == null)
			return null;

		String errName = thr.getClass().getName();

		int errHash = DataProxy.sendError(errName);
		ErrorData errData = getSummaryError(errorMaster, BitUtil.composite(errHash, service));
		errData.error = errHash;
		errData.service = service;
		errData.message = (message == 0 ? errHash : message);
		errData.count++;
		errData.txid = txid;

		if (sql != 0)
			errData.sql = sql;
		if (api != 0)
			errData.apicall = api;
		return errData;
	}

	public void process(SqlStep sqlStep) {
		if (conf.summary_enabled == false)
			return;
		SummaryData d = getSummaryMap(sqlMaster, sqlStep.hash);
		d.count++;
		d.elapsed += sqlStep.elapsed;
		if (sqlStep.error != 0) {
			d.error_cnt++;
		}
	}

	public void process(ApiCallStep apiStep) {
		if (conf.summary_enabled == false)
			return;
		SummaryData d = getSummaryMap(apiMaster, apiStep.hash);
		d.count++;
		d.elapsed += apiStep.elapsed;
		if (apiStep.error != 0) {
			d.error_cnt++;
		}
	}

	private synchronized SummaryData getSummaryMap(SimpleLru<Integer, SummaryData> table, int hash) {
		SimpleLru<Integer, SummaryData> tempTable = table;
		SummaryData d = tempTable.get(hash);
		if (d == null) {
			d = new SummaryData();
			tempTable.put(hash, d);
		}
		return d;
	}

	private synchronized ErrorData getSummaryError(SimpleLru<Long, ErrorData> table, long key) {

		SimpleLru<Long, ErrorData> tempTable = table;
		ErrorData d = tempTable.get(key);
		if (d == null) {
			d = new ErrorData();
			tempTable.put(key, d);
		}
		return d;
	}

	public SummaryPack getAndClear(byte type) {
		SimpleLru<Integer, SummaryData> temp;
		switch (type) {
			case SummaryEnum.APP:
				if (serviceMaster.size() == 0)
					return null;
				temp = serviceMaster;
				serviceMaster = new SimpleLru<Integer, SummaryData>(conf._summary_service_max_count);
				break;
			case SummaryEnum.SQL:
				if (sqlMaster.size() == 0)
					return null;
				temp = sqlMaster;
				sqlMaster = new SimpleLru<Integer, SummaryData>(conf._summary_sql_max_count);
				break;
			case SummaryEnum.APICALL:
				if (apiMaster.size() == 0)
					return null;
				temp = apiMaster;
				apiMaster = new SimpleLru<Integer, SummaryData>(conf._summary_api_max_count);
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
		for (Map.Entry<Integer, SummaryData> ent : temp.entrySet()) {
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
		SimpleLru<Integer, Integer> temp;
		switch (type) {
			case SummaryEnum.IP:
				if (ipMaster.size() == 0)
					return null;
				temp = ipMaster;
				ipMaster = new SimpleLru<Integer, Integer>(conf._summary_ip_max_count);
				break;
			case SummaryEnum.USER_AGENT:
				if (uaMaster.size() == 0)
					return null;
				temp = uaMaster;
				uaMaster = new SimpleLru<Integer, Integer>(conf._summary_useragent_max_count);
				break;
			default:
				return null;
		}

		SummaryPack p = new SummaryPack();
		p.stype = type;

		int cnt = temp.size();
		ListValue id = p.table.newList("id");
		ListValue count = p.table.newList("count");

		for (Map.Entry<Integer, Integer> ent : temp.entrySet()) {
			int key = ent.getKey();
			int value = ent.getValue();
			id.add(key);
			count.add(value);
		}
		return p;
	}

	public SummaryPack getAndClearError(byte type) {
		if (errorMaster.size() == 0)
			return null;

		SimpleLru<Long, ErrorData> temp = errorMaster;
		errorMaster = new SimpleLru<Long, ErrorData>(conf._summary_error_max_count);

		SummaryPack p = new SummaryPack();
		p.stype = type;

		int cnt = temp.size();
		ListValue id = p.table.newList("id");
		ListValue error = p.table.newList("error");
		ListValue service = p.table.newList("service");
		ListValue message = p.table.newList("message");

		ListValue count = p.table.newList("count");
		ListValue txid = p.table.newList("txid");
		ListValue sql = p.table.newList("sql");
		ListValue apicall = p.table.newList("apicall");
		ListValue fullstack = p.table.newList("fullstack");

		for (Map.Entry<Long, ErrorData> ent : temp.entrySet()) {
			long key = ent.getKey();
			ErrorData data = ent.getValue();
			id.add(key);
			error.add(data.error);
			service.add(data.service);
			message.add(data.message);
			count.add(data.count);
			txid.add(data.txid);
			sql.add(data.sql);
			apicall.add(data.apicall);
			fullstack.add(data.fullstack);
		}
		return p;
	}
}
