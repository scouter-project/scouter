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
import scouter.agent.counter.meter.MeterResource;
import scouter.lang.TimeTypeEnum;
import scouter.lang.counters.CounterConstants;
import scouter.lang.pack.PerfCounterPack;
import scouter.lang.value.FloatValue;
import scouter.lang.value.ListValue;

public class HeapUsage {

	public MeterResource heapmin = new MeterResource();

	@Counter
	public void getHeapUsage(CounterBasket pw) {
		long total = Runtime.getRuntime().totalMemory();
		long free = Runtime.getRuntime().freeMemory();

		float tatalMb = (float) (total / 1024. / 1024.);
		float usedMb = (float) ((total - free) / 1024. / 1024.);

		heapmin.add(total - free);
		float used5MinAvgMb = (float) (heapmin.getAvg(300) / 1024. / 1024.);

		ListValue heapValues = new ListValue();
		heapValues.add(tatalMb);
		heapValues.add(usedMb);
		
		PerfCounterPack p = pw.getPack(TimeTypeEnum.REALTIME);
		p.put(CounterConstants.JAVA_HEAP_TOT_USAGE, heapValues);
		p.put(CounterConstants.JAVA_HEAP_USED, new FloatValue(usedMb));
		p.put(CounterConstants.JAVA_HEAP_TOTAL, new FloatValue(tatalMb));

		p = pw.getPack(TimeTypeEnum.FIVE_MIN);
		p.put(CounterConstants.JAVA_HEAP_USED, new FloatValue(used5MinAvgMb));
		p.put(CounterConstants.JAVA_HEAP_TOTAL, new FloatValue(tatalMb));

	}
}
