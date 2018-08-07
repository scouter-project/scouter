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

package scouter.agent.counter;

import scouter.agent.Configure;
import scouter.lang.pack.InteractionPerfCounterPack;

import java.util.HashMap;
import java.util.Map;

public class InteractionCounterBasket {

	private Map<String, InteractionPerfCounterPack> table = new HashMap<String, InteractionPerfCounterPack>();

	public InteractionPerfCounterPack getPack(String objName, String interactionType) {
		String key = objName + interactionType;
		InteractionPerfCounterPack p = table.get(key);
		if (p == null) {
			p = new InteractionPerfCounterPack(objName, interactionType);
			table.put(key, p);
		}
		return p;
	}

	public InteractionPerfCounterPack getPack(String interactionType) {
		return getPack(Configure.getInstance().getObjName(), interactionType);
	}

	public InteractionPerfCounterPack[] getList() {
		return (InteractionPerfCounterPack[]) table.values().toArray(new InteractionPerfCounterPack[table.size()]);
	}
}