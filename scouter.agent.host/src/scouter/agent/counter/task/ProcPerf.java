package scouter.agent.counter.task;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;
import org.hyperic.sigar.SigarProxyCache;

import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.counter.CounterBasket;
import scouter.agent.counter.anotation.Counter;
import scouter.agent.counter.meter.MeterResource;
import scouter.lang.TimeTypeEnum;
import scouter.lang.conf.ConfObserver;
import scouter.lang.counters.CounterConstants;
import scouter.lang.pack.PerfCounterPack;
import scouter.lang.value.FloatValue;
import scouter.util.CastUtil;
import scouter.util.FileUtil;

public class ProcPerf {

	static int SLEEP_TIME = 2000;
	static Sigar sigarImpl = new Sigar();
	static SigarProxy sigar = SigarProxyCache.newInstance(sigarImpl, SLEEP_TIME);
	
	private static File regRoot = null;
	
	Map<String, MeterResource> meterMap = new HashMap<String, MeterResource>();

	public static void ready() {
		String objReg = Configure.getInstance().counter_object_registry_path;
		File objRegFile = new File(objReg);
		if (objRegFile.canRead() == false) {
			objRegFile.mkdirs();
		}

		if (objRegFile.exists()) {
			regRoot = objRegFile;
		} else {
			regRoot = null;
		}
	}

	static {
		ready();
		ConfObserver.add("ProcPerf", new Runnable() {
			@Override
			public void run() {
				ready();
			}
		});
	}
	 int cpuCores = 0;

	@Counter
	public void process(CounterBasket pw) {
		File dir = regRoot;
		if (dir == null)
			return;

		if (cpuCores == 0) {
			cpuCores = getCpuCore();
			Logger.info("Num of Cpu Cores : " + cpuCores);
		}
		long now = System.currentTimeMillis();
		File[] pids = dir.listFiles();
		for (int i = 0; i < pids.length; i++) {
			if (pids[i].isDirectory())
				continue;
			String name = pids[i].getName();
			if (name.endsWith(".scouter") == false) {
				continue;
			}
			int pid = CastUtil.cint(name.substring(0, name.lastIndexOf(".")));
			if (pid == 0)
				continue;

			if (now > pids[i].lastModified() + 5000) {
				pids[i].delete();
				continue;
			}

			String objname = new String(FileUtil.readAll(pids[i]));

			MeterResource meter = meterMap.get(objname);
			if (meter == null) {
				meter = new MeterResource();
				meterMap.put(objname, meter);
			}
			try {
				ProcCpu cpu = sigar.getProcCpu(pid);
				double value = cpu.getPercent() * 100.0D/cpuCores;
				meter.add(value);
				float procCpu = (float) meter.getAvg(Configure.getInstance()._cpu_value_avg_sec);
				PerfCounterPack p = pw.getPack(objname, TimeTypeEnum.REALTIME);
				p.put(CounterConstants.PROC_CPU, new FloatValue(procCpu));
				p = pw.getPack(objname, TimeTypeEnum.FIVE_MIN);
				p.put(CounterConstants.PROC_CPU, new FloatValue(procCpu));
			} catch (Exception e) {
				// ignore no proc
			}

		}
	}

	private int getCpuCore() {
		try {
			return sigar.getCpuList().length;
		} catch (SigarException e) {
			return 1;
		}
	}
}
