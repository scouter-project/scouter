package scouter.agent.counter.task;

import java.lang.management.ManagementFactory;

import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.counter.CounterBasket;
import scouter.agent.counter.anotation.Counter;
import scouter.agent.counter.meter.MeterResource;
import scouter.lang.TimeTypeEnum;
import scouter.lang.counters.CounterConstants;
import scouter.lang.pack.PerfCounterPack;
import scouter.lang.value.DecimalValue;
import scouter.lang.value.FloatValue;
import scouter.util.SystemUtil;

public class HostPerf {

	public MeterResource cpuLoad = new MeterResource();

	private java.lang.management.OperatingSystemMXBean osmxbean;
	boolean ok;

	public HostPerf() {
		try {
			Class c = Class.forName("com.sun.management.OperatingSystemMXBean");
			osmxbean = ManagementFactory.getOperatingSystemMXBean();
			this.ok = c.isAssignableFrom(osmxbean.getClass());
			Logger.println("HOST", "java version : " + SystemUtil.JAVA_VERSION );			
		} catch (Exception e) {
			this.ok = false;
		}
	}

	@Counter
	public void cpuLoad(CounterBasket pw) {

		if (ok == false)
			return;
		try {
			process(pw);
		} catch (Throwable e) {
			Logger.println("HOST", e);
			this.ok = false;
		}
	}

	private void process(CounterBasket pw) {
		Configure conf = Configure.getInstance();
		if (conf.enable_host_agent == false)
			return;

		com.sun.management.OperatingSystemMXBean osm = (com.sun.management.OperatingSystemMXBean) osmxbean;

		AgentHeartBeat.addObject(conf.scouter_host_type, conf.objHostHash, conf.objHostName);
		float cpu = (float) osm.getSystemLoadAverage();

		if (cpu <= 0) {
			cpu = 0;
		}

		cpuLoad.add(cpu);

		long tmem = osm.getTotalPhysicalMemorySize();
		long fmem = osm.getFreePhysicalMemorySize();
		long umem = tmem - fmem;
		long tswap = osm.getTotalSwapSpaceSize();
		long fswap = osm.getFreeSwapSpaceSize();
		long uswap = tswap - fswap;

		PerfCounterPack p = pw.getPack(conf.objHostName, TimeTypeEnum.REALTIME);
		p.put(CounterConstants.HOST_CPU, new FloatValue(cpu));
		p.put(CounterConstants.HOST_MEM_TOTAL, new DecimalValue(tmem / 1024 / 1024));
		p.put(CounterConstants.HOST_MEM_USED, new DecimalValue(umem / 1024 / 1024));
		p.put(CounterConstants.HOST_MEM_AVALIABLE, new DecimalValue(fmem / 1024 / 1024));

		p.put(CounterConstants.HOST_SWAP_TOTAL, new DecimalValue(tswap / 1024 / 1024));
		p.put(CounterConstants.HOST_SWAP_USED, new DecimalValue(uswap / 1024 / 1024));

		p = pw.getPack(conf.objHostName, TimeTypeEnum.FIVE_MIN);
		p.put(CounterConstants.HOST_CPU, new FloatValue((float) cpuLoad.getSum(300)));

		// 단순히 메모리는 평균이 큰 의미가 없음으로 평균을 계산하지 않는다.
		p.put(CounterConstants.HOST_MEM_TOTAL, new DecimalValue(tmem / 1024 / 1024));
		p.put(CounterConstants.HOST_MEM_USED, new DecimalValue(umem / 1024 / 1024));
		p.put(CounterConstants.HOST_MEM_AVALIABLE, new DecimalValue(fmem / 1024 / 1024));

		p.put(CounterConstants.HOST_SWAP_TOTAL, new DecimalValue(tswap / 1024 / 1024));
		p.put(CounterConstants.HOST_SWAP_USED, new DecimalValue(uswap / 1024 / 1024));
	}

}
