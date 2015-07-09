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

package scouter.agent.counter.task;

import scouter.agent.counter.CounterBasket;
import scouter.agent.counter.anotation.Counter;
import scouter.agent.counter.meter.VisitMeter;
import scouter.lang.TimeTypeEnum;
import scouter.lang.counters.CounterConstants;
import scouter.lang.pack.PerfCounterPack;
import scouter.lang.value.DecimalValue;

public class CountingVisitor {

	@Counter
	public void visitor(CounterBasket pw) {

		PerfCounterPack p = pw.getPack(TimeTypeEnum.REALTIME);
		
		int visit5m =VisitMeter.getVisitors();
		p.put(CounterConstants.WAS_USER_5M, new DecimalValue(visit5m));

		p = pw.getPack(TimeTypeEnum.FIVE_MIN);
		p.put(CounterConstants.WAS_USER_5M, new DecimalValue(visit5m));
	}
}