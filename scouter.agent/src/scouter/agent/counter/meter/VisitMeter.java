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

package scouter.agent.counter.meter;

import java.util.Enumeration;

import scouter.agent.Logger;
import scouter.util.DateUtil;
import scouter.util.LongLongLinkedMap;

public class VisitMeter {

	private static final int MAX_VISITORS = 70000;
	protected static LongLongLinkedMap visitors = new LongLongLinkedMap().setMax(MAX_VISITORS);
	protected static MeterResource newVisotors = new MeterResource();

	public static void add(long visitor) {
		if (visitor == 0)
			newVisotors.add(1);
		else {
			visitors.putLast(visitor, System.currentTimeMillis());
		}
	}

	public synchronized static int getVisitors() {
		int v = 0;
		long now = System.currentTimeMillis();
		try {
			Enumeration en = visitors.entries();
			while (en.hasMoreElements()) {
				LongLongLinkedMap.ENTRY e = (LongLongLinkedMap.ENTRY) en.nextElement();
				if (now - e.getValue() > DateUtil.MILLIS_PER_FIVE_MINUTE) {
				   visitors.remove(e.getKey());
				} else {
					v++;
				}
			}
		} catch (Throwable t) {
			Logger.println("SA09", "VISIT-METER" + t.toString());
		}
		return v;
	}

	public synchronized static int getNewVisitors() {
		return (int) newVisotors.getSum(300);
	}

	public static void main(String[] args) throws InterruptedException {
		for (int i = 1; i <= 100; i++) {
			if(i==50)
				Thread.sleep(1000);
			add(i);
		}
		Thread.sleep(1000);
		System.out.println(getVisitors());
	}
}