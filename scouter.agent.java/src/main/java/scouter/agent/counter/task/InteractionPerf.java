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
		if (!conf.counter_interaction_enabled) {
			return;
		}

		int periodSec = 30;
		String interactionType = CounterConstants.INTR_API_INCOMING;
		LinkedMap<MeterInteractionManager.Key, MeterInteraction> apiIncomingMeterMap = MeterInteractionManager.getInstance().getApiIncomingMeterMap();
		addInteractionsToBasket(basket, interactionType, apiIncomingMeterMap, periodSec);
	}

	@InteractionCounter(interval = 5000)
	public void collectNormalIncomingInteractionCounter(InteractionCounterBasket basket) {
		if (!conf.counter_interaction_enabled) {
			return;
		}

		int periodSec = 30;
		String interactionType = CounterConstants.INTR_NORMAL_INCOMING;
		LinkedMap<MeterInteractionManager.Key, MeterInteraction> normalIncomingMeterMap = MeterInteractionManager.getInstance().getNormalIncomingMeterMap();
		addInteractionsToBasket(basket, interactionType, normalIncomingMeterMap, periodSec);
	}

	@InteractionCounter(interval = 5000)
	public void collectApiOutgoingInteractionCounter(InteractionCounterBasket basket) {
		if (!conf.counter_interaction_enabled) {
			return;
		}

		int periodSec = 30;
		String interactionType = CounterConstants.INTR_API_OUTGOING;
		LinkedMap<MeterInteractionManager.Key, MeterInteraction> apiOutgoingMeterMap = MeterInteractionManager.getInstance().getApiOutgoingMeterMap();
		addInteractionsToBasket(basket, interactionType, apiOutgoingMeterMap, periodSec);
	}

	@InteractionCounter(interval = 5000)
	public void collectNormalOutgoingInteractionCounter(InteractionCounterBasket basket) {
		if (!conf.counter_interaction_enabled) {
			return;
		}

		int periodSec = 30;
		String interactionType = CounterConstants.INTR_NORMAL_OUTGOING;
		LinkedMap<MeterInteractionManager.Key, MeterInteraction> normalOutgoingMeterMap = MeterInteractionManager.getInstance().getNormalOutgoingMeterMap();
		addInteractionsToBasket(basket, interactionType, normalOutgoingMeterMap, periodSec);
	}

	@InteractionCounter(interval = 5000)
	public void collectDbCallInteractionCounter(InteractionCounterBasket basket) {
		if (!conf.counter_interaction_enabled) {
			return;
		}

		int periodSec = 30;
		String interactionType = CounterConstants.INTR_DB_CALL;
		LinkedMap<MeterInteractionManager.Key, MeterInteraction> dbCallMeterMap = MeterInteractionManager.getInstance().getDbCallMeterMap();
		addInteractionsToBasket(basket, interactionType, dbCallMeterMap, periodSec);
	}

	@InteractionCounter(interval = 5000)
	public void collectRedisCallInteractionCounter(InteractionCounterBasket basket) {
		if (!conf.counter_interaction_enabled) {
			return;
		}

		int periodSec = 30;
		String interactionType = CounterConstants.INTR_REDIS_CALL;
		LinkedMap<MeterInteractionManager.Key, MeterInteraction> redisCallMeterMap = MeterInteractionManager.getInstance().getRedisCallMeterMap();
		addInteractionsToBasket(basket, interactionType, redisCallMeterMap, periodSec);
	}

	@InteractionCounter(interval = 5000)
	public void collectKafkaCallInteractionCounter(InteractionCounterBasket basket) {
		if (!conf.counter_interaction_enabled) {
			return;
		}

		int periodSec = 30;
		String interactionType = CounterConstants.INTR_KAFKA_CALL;
		LinkedMap<MeterInteractionManager.Key, MeterInteraction> kafkaCallMeterMap = MeterInteractionManager.getInstance().getKafkaCallMeterMap();
		addInteractionsToBasket(basket, interactionType, kafkaCallMeterMap, periodSec);
	}

	@InteractionCounter(interval = 5000)
	public void collectRabbitmqCallInteractionCounter(InteractionCounterBasket basket) {
		if (!conf.counter_interaction_enabled) {
			return;
		}

		int periodSec = 30;
		String interactionType = CounterConstants.INTR_RABBITMQ_CALL;
		LinkedMap<MeterInteractionManager.Key, MeterInteraction> rabbitmqCallMeterMap = MeterInteractionManager.getInstance().getRabbitmqCallMeterMap();
		addInteractionsToBasket(basket, interactionType, rabbitmqCallMeterMap, periodSec);
	}

	@InteractionCounter(interval = 5000)
	public void collectElasticSearchCallInteractionCounter(InteractionCounterBasket basket) {
		if (!conf.counter_interaction_enabled) {
			return;
		}

		int periodSec = 30;
		String interactionType = CounterConstants.INTR_ELASTICSEARCH_CALL;
		LinkedMap<MeterInteractionManager.Key, MeterInteraction> esMeterMap = MeterInteractionManager.getInstance().getElasticSearchCallMeterMap();
		addInteractionsToBasket(basket, interactionType, esMeterMap, periodSec);
	}

	@InteractionCounter(interval = 5000)
	public void collectMongoDbCallInteractionCounter(InteractionCounterBasket basket) {
		if (!conf.counter_interaction_enabled) {
			return;
		}

		int periodSec = 30;
		String interactionType = CounterConstants.INTR_MONGODB_CALL;
		LinkedMap<MeterInteractionManager.Key, MeterInteraction> meterMap = MeterInteractionManager.getInstance().getMongoDbCallMeterMap();
		addInteractionsToBasket(basket, interactionType, meterMap, periodSec);
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
