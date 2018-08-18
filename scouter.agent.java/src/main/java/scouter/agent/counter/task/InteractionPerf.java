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
import scouter.lang.counters.CounterConstants;
import scouter.lang.pack.InteractionPerfCounterPack;
import scouter.util.LinkedMap;

import java.util.Enumeration;

public class InteractionPerf {

	private Configure conf = Configure.getInstance();

	@InteractionCounter(interval = 5000)
	public void collectApiIncomingInteractionCounter(InteractionCounterBasket basket) {

		int periodSec = 30;
		String interactionType = CounterConstants.INTR_API_INCOMING;
		LinkedMap<MeterInteractionManager.Key, MeterInteraction> apiIncomingMeterMap = MeterInteractionManager.getInstance().getApiIncomingMeterMap();
		addInteractionsToBasket(basket, interactionType, apiIncomingMeterMap, periodSec);
	}

	@InteractionCounter(interval = 5000)
	public void collectDbCallInteractionCounter(InteractionCounterBasket basket) {

		int periodSec = 30;
		String interactionType = CounterConstants.INTR_DB_CALL;
		LinkedMap<MeterInteractionManager.Key, MeterInteraction> dbCallMeterMap = MeterInteractionManager.getInstance().getDbCallMeterMap();
		addInteractionsToBasket(basket, interactionType, dbCallMeterMap, periodSec);
	}

	@InteractionCounter(interval = 5000)
	public void collectRedisCallInteractionCounter(InteractionCounterBasket basket) {

		int periodSec = 30;
		String interactionType = CounterConstants.INTR_REDIS_CALL;
		LinkedMap<MeterInteractionManager.Key, MeterInteraction> redisCallMeterMap = MeterInteractionManager.getInstance().getRedisCallMeterMap();
		addInteractionsToBasket(basket, interactionType, redisCallMeterMap, periodSec);
	}

	private void addInteractionsToBasket(InteractionCounterBasket basket, String interactionType, LinkedMap<MeterInteractionManager.Key, MeterInteraction> apiIncomingMeterMap, int periodSec) {
		Enumeration<LinkedMap.LinkedEntry<MeterInteractionManager.Key, MeterInteraction>> entries = apiIncomingMeterMap.entries();

		while (entries.hasMoreElements()) {
			LinkedMap.LinkedEntry<MeterInteractionManager.Key, MeterInteraction> entry = entries.nextElement();
			MeterInteractionManager.Key key = entry.getKey();
			MeterInteraction meterInteraction = entry.getValue();

			InteractionPerfCounterPack pack = new InteractionPerfCounterPack(conf.getObjName(), interactionType);
			pack.fromHash = key.fromHash;
			pack.toHash = key.toHash;
			pack.period = periodSec;
			pack.count = meterInteraction.getCount(periodSec);
			pack.errorCount = meterInteraction.getErrorCount(periodSec);
			pack.totalElapsed = meterInteraction.getSumTime(periodSec);

			basket.add(interactionType, pack);
		}
	}
}
