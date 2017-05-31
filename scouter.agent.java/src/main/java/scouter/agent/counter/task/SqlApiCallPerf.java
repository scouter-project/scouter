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

import scouter.agent.counter.CounterBasket;
import scouter.agent.counter.anotation.Counter;
import scouter.agent.counter.meter.MeterAPI;
import scouter.agent.counter.meter.MeterSQL;
import scouter.lang.TimeTypeEnum;
import scouter.lang.counters.CounterConstants;
import scouter.lang.pack.PerfCounterPack;
import scouter.lang.value.DecimalValue;
import scouter.lang.value.FloatValue;

public class SqlApiCallPerf {

	@Counter
	public void sqlApiPerf(CounterBasket pw) {
		MeterSQL sql = MeterSQL.getInstance();
		MeterAPI api = MeterAPI.getInstance();

		PerfCounterPack p = pw.getPack(TimeTypeEnum.REALTIME);
		long elapsed = sql.getAvgTime(30);
		float tps = sql.getTps(30);
		float errRate = sql.getErrorRate(30);

		p.put(CounterConstants.WAS_SQL_TIME, new DecimalValue(elapsed));
		p.put(CounterConstants.WAS_SQL_TPS, new FloatValue(tps));
		p.put(CounterConstants.WAS_SQL_ERROR_RATE, new FloatValue(errRate));

		elapsed = api.getAvgTime(30);
		tps = api.getTps(30);
		errRate = api.getErrorRate(30);

		p.put(CounterConstants.WAS_APICALL_TIME, new DecimalValue(elapsed));
		p.put(CounterConstants.WAS_APICALL_TPS, new FloatValue(tps));
		p.put(CounterConstants.WAS_APICALL_ERROR_RATE, new FloatValue(errRate));

		p = pw.getPack(TimeTypeEnum.FIVE_MIN);
		
		elapsed = sql.getAvgTime(300);
		tps = sql.getTps(300);
		errRate = sql.getErrorRate(300);

		p.put(CounterConstants.WAS_SQL_TIME, new DecimalValue(elapsed));
		p.put(CounterConstants.WAS_SQL_TPS, new FloatValue(tps));
		p.put(CounterConstants.WAS_SQL_ERROR_RATE, new FloatValue(errRate));

		elapsed = api.getAvgTime(300);
		tps = api.getTps(300);
		errRate = api.getErrorRate(300);

		p.put(CounterConstants.WAS_APICALL_TIME, new DecimalValue(elapsed));
		p.put(CounterConstants.WAS_APICALL_TPS, new FloatValue(tps));
		p.put(CounterConstants.WAS_APICALL_ERROR_RATE, new FloatValue(errRate));

	}

}