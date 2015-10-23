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
		long time = sql.getAvgTime(30);
		float count = sql.getCountPerSec(30);
		float err = sql.getErrorPerSec(30);

		p.put(CounterConstants.WAS_SQL_TIME, new DecimalValue(time));
		p.put(CounterConstants.WAS_SQL_COUNT, new FloatValue(count));
		p.put(CounterConstants.WAS_SQL_ERROR, new FloatValue(err));

		time = api.getAvgTime(30);
		count = api.getCountPerSec(30);
		err = api.getErrorPerSec(30);

		p.put(CounterConstants.WAS_APICALL_TIME, new DecimalValue(time));
		p.put(CounterConstants.WAS_APICALL_COUNT, new FloatValue(count));
		p.put(CounterConstants.WAS_APICALL_ERROR, new FloatValue(err));

		p = pw.getPack(TimeTypeEnum.FIVE_MIN);
		
		time = api.getAvgTime(300);
		count = api.getCountPerSec(300);
		err = api.getErrorPerSec(300);
		
		p.put(CounterConstants.WAS_SQL_TIME, new DecimalValue(time));
		p.put(CounterConstants.WAS_SQL_COUNT, new FloatValue(count));
		p.put(CounterConstants.WAS_SQL_ERROR, new FloatValue(err));

		time = api.getAvgTime(300);
		count = api.getCountPerSec(300);
		err = api.getErrorPerSec(300);

		p.put(CounterConstants.WAS_APICALL_TIME, new DecimalValue(time));
		p.put(CounterConstants.WAS_APICALL_COUNT, new FloatValue(count));
		p.put(CounterConstants.WAS_APICALL_ERROR, new FloatValue(err));

	}

}