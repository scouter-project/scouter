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

import scouter.lang.pack.InteractionPerfCounterPack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InteractionCounterBasket {

	private Map<String, List<InteractionPerfCounterPack>> packListMap = new HashMap<String, List<InteractionPerfCounterPack>>();

	public void add(String interactionType, InteractionPerfCounterPack pack) {
		List<InteractionPerfCounterPack> packList = getList(interactionType);
		if (packList == null) {
			packList = new ArrayList<InteractionPerfCounterPack>();
			packListMap.put(interactionType, packList);
		}
		packList.add(pack);
	}

	public List<InteractionPerfCounterPack> getList(String interactionType) {
		return packListMap.get(interactionType);
	}

	public InteractionPerfCounterPack[] geAllAsArray() {
		Collection<List<InteractionPerfCounterPack>> valueCollection = packListMap.values();

		int size = 0;
		for (List<InteractionPerfCounterPack> list : valueCollection) {
			size += list.size();
		}

		InteractionPerfCounterPack[] packs = new InteractionPerfCounterPack[size];
		int index = 0;
		for (List<InteractionPerfCounterPack> list : valueCollection) {
			for (InteractionPerfCounterPack pack : list) {
				packs[index++] = pack;
			}
		}

		return packs;
	}
}