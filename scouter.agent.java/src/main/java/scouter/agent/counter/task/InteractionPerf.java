/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package scouter.agent.counter.task;

import scouter.agent.Configure;
import scouter.agent.counter.InteractionCounterBasket;
import scouter.agent.counter.anotation.InteractionCounter;
import scouter.agent.counter.meter.MeterInteraction;
import scouter.agent.counter.meter.MeterInteractionManager;
import scouter.lang.pack.InteractionPerfCounterPack;

public class InteractionPerf {

	private Configure conf = Configure.getInstance();

	@InteractionCounter
	public void getInteractionCounter(InteractionCounterBasket basket) {

		MeterInteraction apiOutgoingMeter = MeterInteractionManager.getApiOutgoingMeter();
		InteractionPerfCounterPack pack = basket.getPack(apiOutgoingMeter.getInteractionName());

		int period = 30;
		int count = apiOutgoingMeter.getCount(period);
		int errorCount = apiOutgoingMeter.getErrorCount(period);
		long totalElapsed = apiOutgoingMeter.getSumTime(period);


	}


}
