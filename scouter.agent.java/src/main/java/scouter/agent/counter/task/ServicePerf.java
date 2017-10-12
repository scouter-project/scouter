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

package scouter.agent.counter.task;

import scouter.agent.Configure;
import scouter.agent.counter.CounterBasket;
import scouter.agent.counter.anotation.Counter;
import scouter.agent.counter.meter.MeterResource;
import scouter.agent.counter.meter.MeterService;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.summary.EndUserSummary;
import scouter.agent.summary.ServiceSummary;
import scouter.agent.trace.TraceContextManager;
import scouter.agent.util.DumpUtil;
import scouter.lang.SummaryEnum;
import scouter.lang.TimeTypeEnum;
import scouter.lang.counters.CounterConstants;
import scouter.lang.pack.PerfCounterPack;
import scouter.lang.pack.SummaryPack;
import scouter.lang.value.DecimalValue;
import scouter.lang.value.FloatValue;
import scouter.lang.value.ListValue;
import scouter.util.DateUtil;

public class ServicePerf {

	private MeterResource activeCounter = new MeterResource();
	private Configure conf = Configure.getInstance();

	@Counter
	public void getServicePerf(CounterBasket pw) {
		Configure conf = Configure.getInstance();
		MeterService service = MeterService.getInstance();
		int elapsed = service.getElapsedTime(30);
		float tps = service.getTPS(30);
		float errorRate = service.getErrorRate(30);
		int count = service.getServiceCount(60);
		int resp90pct = service.getElapsed90Pct(30);
		int sqlTimeByService = service.getSqlTime(30);
		int apiTimeByService = service.getApiTime(30);
		int queuingTime = service.getQueuingTime(30);

		int[] act = TraceContextManager.getActiveCount();
		int active = act[0] + act[1] + act[2];
		if (conf.autodump_trigger_active_service_cnt <= active) {
			DumpUtil.autoDump();
		}
		activeCounter.add(active);

		// active service 30초 평균으로 변경
		active = (int) Math.round(activeCounter.getAvg(30));

		PerfCounterPack p = pw.getPack(TimeTypeEnum.REALTIME);
		p.put(CounterConstants.WAS_ELAPSED_TIME, new DecimalValue(elapsed));
		p.put(CounterConstants.WAS_SERVICE_COUNT, new DecimalValue(count));
		p.put(CounterConstants.WAS_TPS, new FloatValue(tps));
		p.put(CounterConstants.WAS_ERROR_RATE, new FloatValue(errorRate));
		p.put(CounterConstants.WAS_ACTIVE_SERVICE, new DecimalValue(active));
		p.put(CounterConstants.WAS_ELAPSED_90PCT, new DecimalValue(resp90pct));
		p.put(CounterConstants.WAS_APICALL_ELAPSED_TIME_BY_SERVICE, new DecimalValue(apiTimeByService));
		p.put(CounterConstants.WAS_SQL_ELAPSED_TIME_BY_SERVICE, new DecimalValue(sqlTimeByService));
		p.put(CounterConstants.WAS_QUEUING_TIME, new DecimalValue(queuingTime));

		ListValue activeSpeed = new ListValue();
		activeSpeed.add(act[0]);
		activeSpeed.add(act[1]);
		activeSpeed.add(act[2]);

		p.put(CounterConstants.WAS_ACTIVE_SPEED, activeSpeed);

		// UdpProxy.sendAlert(, message)

		count = service.getServiceCount(300);
		tps = service.getTPS(300);
		elapsed = service.getElapsedTime(300);
		errorRate = service.getErrorRate(300);
		int activeService = (int) activeCounter.getAvg(300);
		resp90pct = service.getElapsed90Pct(300);
        sqlTimeByService = service.getSqlTime(300);
        apiTimeByService = service.getApiTime(300);

		p = pw.getPack(TimeTypeEnum.FIVE_MIN);
		p.put(CounterConstants.WAS_ELAPSED_TIME, new DecimalValue(elapsed));
		p.put(CounterConstants.WAS_SERVICE_COUNT, new DecimalValue(count));
		p.put(CounterConstants.WAS_TPS, new FloatValue(tps));
		p.put(CounterConstants.WAS_ERROR_RATE, new FloatValue(errorRate));
		p.put(CounterConstants.WAS_ACTIVE_SERVICE, new DecimalValue(activeService));
		p.put(CounterConstants.WAS_ELAPSED_90PCT, new DecimalValue(resp90pct));
        p.put(CounterConstants.WAS_APICALL_ELAPSED_TIME_BY_SERVICE, new DecimalValue(apiTimeByService));
        p.put(CounterConstants.WAS_SQL_ELAPSED_TIME_BY_SERVICE, new DecimalValue(sqlTimeByService));
		p.put(CounterConstants.WAS_QUEUING_TIME, new DecimalValue(queuingTime));
	}

	private long last_sent = DateUtil.getMinUnit(System.currentTimeMillis()) / 5;

	@Counter(interval = 500)
	public void summary(CounterBasket pw) {
		long time = System.currentTimeMillis();
		long now = DateUtil.getMinUnit(time) / 5;

		if(conf.getBoolean("_dev_summary_test", false)) {
			now = time / 1000 / 15;
		}

		if (now == last_sent)
			return;
		last_sent = now;
		time = (time - 10000) / DateUtil.MILLIS_PER_FIVE_MINUTE * DateUtil.MILLIS_PER_FIVE_MINUTE;

		SummaryPack p = ServiceSummary.getInstance().getAndClear(SummaryEnum.APP);
		if (p != null) {
			p.time = time;
			DataProxy.send(p);
		}
		p = ServiceSummary.getInstance().getAndClear(SummaryEnum.SQL);
		if (p != null) {
			p.time = time;
			DataProxy.send(p);
		}
		p = ServiceSummary.getInstance().getAndClear(SummaryEnum.APICALL);
		if (p != null) {
			p.time = time;
			DataProxy.send(p);
		}

		p = ServiceSummary.getInstance().getAndClearX(SummaryEnum.IP);
		if (p != null) {
			p.time = time;
			DataProxy.send(p);
		}
		p = ServiceSummary.getInstance().getAndClearX(SummaryEnum.USER_AGENT);
		if (p != null) {
			p.time = time;
			DataProxy.send(p);
		}
		p = ServiceSummary.getInstance().getAndClearError(SummaryEnum.SERVICE_ERROR);
		if (p != null) {
			p.time = time;
			DataProxy.send(p);
		}
	}

	@Counter(interval = 500)
	public void enduser(CounterBasket pw) {
		long time = System.currentTimeMillis();
		long now = DateUtil.getMinUnit(time) / 5;
		if (now == last_sent)
			return;
		last_sent = now;
		time = (time - 10000) / DateUtil.MILLIS_PER_FIVE_MINUTE * DateUtil.MILLIS_PER_FIVE_MINUTE;

		SummaryPack p = EndUserSummary.getInstance().getAndClearNavTable();
		if (p != null) {
			p.time = time;
			DataProxy.send(p);
		}
		p = EndUserSummary.getInstance().getAndClearAjaxTable();
		if (p != null) {
			p.time = time;
			DataProxy.send(p);
		}
		p = EndUserSummary.getInstance().getAndClearErrorTable();
		if (p != null) {
			p.time = time;
			DataProxy.send(p);
		}
	}
}
