package scouter.agent.netio.request.handle;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.util.Util;
import scouter.agent.netio.request.anotation.RequestHandler;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TopRequest {
	static final int SLEEP_TIME = 1000 * 5;

	SystemInfo si = new SystemInfo();
	OperatingSystem os = si.getOperatingSystem();
	HardwareAbstractionLayer hal = si.getHardware();
	static int cpuCores;

	@RequestHandler(RequestCmd.HOST_TOP)
	public Pack getTop(Pack param) {

		if (cpuCores == 0) {
			CentralProcessor ps = hal.getProcessor();
			cpuCores = ps.getLogicalProcessorCount();

		}
		MapPack pack = new MapPack();
		ListValue pidLv = pack.newList("PID");
		ListValue userLv = pack.newList("USER");
		ListValue cpuLv = pack.newList("CPU");
		ListValue memLv = pack.newList("MEM");
		ListValue timeLv = pack.newList("TIME");
		ListValue nameLv = pack.newList("NAME");

		SystemInfo si =  new SystemInfo();
		OperatingSystem os = si.getOperatingSystem();

		Map<Integer, Object[]> prevCpuTime = os.getProcesses().stream()
				.collect(Collectors.toMap(
						p->p.getProcessID(),
						p->new Object[] {p.getKernelTime() + p.getUserTime(), System.nanoTime()},
						(a,b)->a));

		try {
			Thread.sleep(2000);
		}catch (Exception e) {}

		Map<Integer, Object[]> currentProcesses = os.getProcesses().stream()
				.collect(Collectors.toMap(p->p.getProcessID(),
						p->new Object[] {p, System.nanoTime()},
						(a,b)->a));


		Map<OSProcess, Double> pct = currentProcesses.entrySet().stream()
				.filter(e->prevCpuTime.containsKey(e.getKey())) //just a filter to make sure that this key exists in the previous sample
				.collect(Collectors.toMap(
						e->(OSProcess)e.getValue()[0],
						e->{
							OSProcess newProcess = (OSProcess) e.getValue()[0];
							Long newNano = (Long) e.getValue()[1];
							Long prevNano =  (Long) prevCpuTime.get(newProcess.getProcessID())[1];

							long previousTime = (long) prevCpuTime.get(e.getKey())[0];
							long currentTime = newProcess.getKernelTime() + newProcess.getUserTime();

							double elapsed = (newNano-prevNano) / 1000000d;
							long timeDifference = currentTime - previousTime;
							double cpu = (100d * timeDifference / elapsed);
							return cpu;
						},
						(a,b)->a
				));

		pct.entrySet().stream()
				.sorted(Comparator.comparing((Map.Entry<OSProcess, Double> e) -> e.getValue()).reversed())
				.forEach(e -> {
					try {
						OSProcess p = e.getKey();
						Double usage = e.getValue();

						pidLv.add(p.getProcessID());
						userLv.add(p.getUser());
						timeLv.add(p.getStartTime());
						nameLv.add(p.getName());
						memLv.add(p.getResidentSetSize());
						cpuLv.add(usage);
					} catch (Exception ex) {
					}
				});

		return pack;
	}

	public static long previousTime;
	public static long[] oldTicks;

	public static void main(String[] args) {
		for (int i = 0; i < 500; i++) {
			cpuUtilizationPerProcess();
			Util.sleep(2000);
		}
	}

	public static void cpuUtilizationPerProcess() {
		SystemInfo systemInfo = new SystemInfo();
		CentralProcessor processor = systemInfo.getHardware().getProcessor();
		int cpuNumber = processor.getLogicalProcessorCount();

		List<OSProcess> processes = systemInfo.getOperatingSystem().getProcesses(null, OperatingSystem.ProcessSorting.CPU_DESC, 10);
		OSProcess process = processes.stream().filter(p -> p.getName().equals("scouter")).findFirst().get();

		long currentTime = process.getKernelTime() + process.getUserTime();
		long timeDifference = currentTime - previousTime;

		//double processCpu = (100 * (timeDifference / 5000d)) / cpuNumber;
		double processCpu = (100d * (timeDifference / 2000d)) * 300d / 8d; //M1 맥북에서는 보정해줘야 하는 듯. tickNano 곱하고 process 수(efficient process 제외)로 나눠야 하는 것 같다.
		previousTime = currentTime;

		System.out.format("K: %d, U: %d, diff: %d, CPU: %.1f%n", process.getKernelTime(), process.getUserTime(),
				timeDifference, processCpu);
	}

}
