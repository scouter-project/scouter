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
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;

import scouter.agent.Configure;
import scouter.agent.counter.CounterBasket;
import scouter.agent.counter.anotation.Counter;
import scouter.agent.counter.meter.MeterResource;
import scouter.agent.trace.AlertProxy;
import scouter.lang.AlertLevel;
import scouter.lang.TimeTypeEnum;
import scouter.lang.counters.CounterConstants;
import scouter.lang.pack.PerfCounterPack;
import scouter.lang.value.FloatValue;

public class PermGen {

	MemoryPoolMXBean permGenBean;
	public MeterResource meter = new MeterResource();

	@Counter
	public void getPermGen(CounterBasket pw) {
		if (permGenBean == null) {
			try {
				List<MemoryPoolMXBean> beans = ManagementFactory.getMemoryPoolMXBeans();
				for (MemoryPoolMXBean bean : beans) {
					if (bean.getName().toUpperCase().contains("PERM GEN")) {
						permGenBean = bean;
						break;
					} else if(bean.getName().toUpperCase().contains("METASPACE")) {
						permGenBean = bean;
						break;
					}
				}
			} catch (Throwable th) {
			}
		}
		if (permGenBean == null) {
			return;
		}
		MemoryUsage usage = permGenBean.getUsage();
		long used = usage.getUsed();
		meter.add(used);
		float usedM = (used / 1024.f / 1024.f);
		float max = (usage.getMax() / 1024.f / 1024.f);

		PerfCounterPack p = pw.getPack(TimeTypeEnum.REALTIME);
		p.put(CounterConstants.JAVA_PERM_USED, new FloatValue(usedM));
		
		//metaspace의 경우 설정값을 지정했을 경우에만 사용률(%) 수집 및 경고 발생
		if(usage.getMax() != -1) { 
			p.put(CounterConstants.JAVA_PERM_PERCENT, new FloatValue(usedM * 100 / max));
	
			Configure conf = Configure.getInstance();
			float rate = used * 100 / usage.getMax();
	
			// /////////////////////////////////////////////////
			// PermGen Warning 
			if (rate >= conf.alert_perm_warning_pct) {
				AlertProxy.sendAlert(AlertLevel.WARN, "WARNING_MEMORY_HIGH", "warning perm usage used="
						+ (used / 1024 / 1024) + "MB rate=" + rate + "%");
			}
		}
		// /////////////////////////////////////////////////

		p = pw.getPack(TimeTypeEnum.FIVE_MIN);
		p.put(CounterConstants.JAVA_PERM_USED, new FloatValue((float) (meter.getAvg(300) / 1024.f / 1024.f)));
	}
}
