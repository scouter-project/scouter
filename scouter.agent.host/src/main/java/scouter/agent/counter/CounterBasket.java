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

package scouter.agent.counter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import scouter.agent.Configure;
import scouter.lang.TimeTypeEnum;
import scouter.lang.pack.PerfCounterPack;

public class CounterBasket {
	private class Key {
		private String objName;
		private byte timeType;

		public Key(String objName, byte timeType) {
			this.objName = objName;
			this.timeType = timeType;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Key) {
				Key k = (Key) obj;
				return this.objName.equals(k.objName) && this.timeType==k.timeType;
			}
			return false;
		}

		@Override
		public int hashCode() {

			return objName.hashCode() ^ timeType;
		}
	}

	private Map<Key, PerfCounterPack> table = new HashMap<CounterBasket.Key, PerfCounterPack>();

	public PerfCounterPack getPack(String objName, byte timeType) {
		Key key = new Key(objName, timeType);
		PerfCounterPack p = table.get(key);
		if (p == null) {
			p = new PerfCounterPack();
			p.objName = objName;
			p.timetype = timeType;
			table.put(key, p);
		}
		return p;
	}

	public PerfCounterPack getPack(byte timeType) {
		return getPack(Configure.getInstance().getObjName(), timeType);
	}

	public PerfCounterPack[] getList() {
		ArrayList list =  new ArrayList(table.values());
		return (PerfCounterPack[])list.toArray(new PerfCounterPack[list.size()]);
	}
}