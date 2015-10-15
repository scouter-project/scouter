/*
 *  Copyright 2015 the original author or authors.
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
import scouter.lang.SummaryEnum;
import scouter.lang.pack.SummaryPack;
import scouter.lang.pack.XLogPack;
import scouter.lang.step.ApiCallStep;
import scouter.lang.step.SqlStep;
import scouter.util.IntKeyLinkedMap;

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
		SummaryData d = getSummaryMap(serviceMaster, p.service);
		d.count++;
		d.elapsed += p.elapsed;
		if (p.error!=0) {
			d.error_cnt++;
		}
		d.cpu += p.cpu;
		d.mem += p.bytes;
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

	private SummaryData getSummaryMap(IntKeyLinkedMap<SummaryData> table, int hash) {
		IntKeyLinkedMap<SummaryData> tempTable = table;
		SummaryData d = tempTable.get(hash);
		if (d == null) {
			d = new SummaryData();
			tempTable.put(hash, d);
		}
		return d;
	}

	private IntKeyLinkedMap<SummaryData> sqlMaster = new IntKeyLinkedMap<SummaryData>().setMax(conf.summary_sql_max);
	private IntKeyLinkedMap<SummaryData> apiMaster = new IntKeyLinkedMap<SummaryData>().setMax(conf.summary_api_max);
	private IntKeyLinkedMap<SummaryData> serviceMaster = new IntKeyLinkedMap<SummaryData>()
			.setMax(conf.summary_service_max);

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
		p.id = new int[cnt];
		p.count = new int[cnt];
		p.errorCnt = new int[cnt];
		p.elapsedSum = new long[cnt];
		if (SummaryEnum.APP == type) {
			p.cpuTime = new long[cnt];
			p.memAlloc = new long[cnt];
		}
		Enumeration<IntKeyLinkedMap.ENTRY> en = temp.entries();
		for (int i = 0; i < cnt; i++) {
			IntKeyLinkedMap.ENTRY<SummaryData> ent = en.nextElement();
			int key = ent.getKey();
			SummaryData data = ent.getValue();
			p.id[i] = key;
			p.count[i] = data.count;
			p.errorCnt[i] = data.error_cnt;
			p.elapsedSum[i] = data.elapsed;
			if (SummaryEnum.APP == type) {
				p.cpuTime[i] = data.cpu;
				p.memAlloc[i] = data.mem;
			}
		}
		return p;
	}

}