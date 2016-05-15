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

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

import com.sun.management.UnixOperatingSystemMXBean;

import scouter.agent.Logger;
import scouter.agent.counter.CounterBasket;
import scouter.agent.counter.anotation.Counter;
import scouter.lang.TimeTypeEnum;
import scouter.lang.counters.CounterConstants;
import scouter.lang.pack.PerfCounterPack;
import scouter.lang.value.ListValue;

public class FDInfo {
	
	public static boolean availableFdInfo = true;
	
	@Counter
	public void process(CounterBasket pw) {
		if (availableFdInfo == false) {
			 return;
		}
		
		// Currently supported only sun jvm on unix platform
		try {
			OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
			if(os instanceof UnixOperatingSystemMXBean){
				UnixOperatingSystemMXBean unixOs = (UnixOperatingSystemMXBean) os;
				long max = unixOs.getMaxFileDescriptorCount();
				long open = unixOs.getOpenFileDescriptorCount();
				
				ListValue fdUsage = new ListValue();
				fdUsage.add(max);
				fdUsage.add(open);
				
				PerfCounterPack p = pw.getPack(TimeTypeEnum.REALTIME);
				p.put(CounterConstants.JAVA_FD_USAGE, fdUsage);
			} else {
				availableFdInfo = false;
			}
		} catch (Throwable th) {
			Logger.println(th.getMessage());
			availableFdInfo = false;
		}
	}
}
