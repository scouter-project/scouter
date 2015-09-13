package scouter.agent.counter.task;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;

import scouter.agent.counter.CounterBasket;
import scouter.agent.counter.anotation.Counter;
import scouter.agent.counter.meter.MeterResource;
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
					}
				}
			} catch (Throwable th) { }
		}
		if (permGenBean == null) return;
		MemoryUsage usage = permGenBean.getUsage();
		long used = usage.getUsed();
		meter.add(used);
		float usedM = (used / 1024.f / 1024.f);
		float max = (usage.getMax() / 1024.f / 1024.f);
		
		PerfCounterPack p = pw.getPack(TimeTypeEnum.REALTIME);
		p.put(CounterConstants.JAVA_PERM_USED, new FloatValue(usedM));
		p.put(CounterConstants.JAVA_PERM_PERCENT, new FloatValue(usedM * 100 / max));
		
		p = pw.getPack(TimeTypeEnum.FIVE_MIN);
		p.put(CounterConstants.JAVA_PERM_USED, new FloatValue((float)(meter.getAvg(300) / 1024.f / 1024.f)));
	}
	
}
