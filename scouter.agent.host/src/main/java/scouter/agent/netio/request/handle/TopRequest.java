package scouter.agent.netio.request.handle;

import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.ProcCredName;
import org.hyperic.sigar.ProcMem;
import org.hyperic.sigar.ProcTime;
import org.hyperic.sigar.ProcUtil;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;
import org.hyperic.sigar.SigarProxyCache;

import scouter.agent.Logger;
import scouter.agent.netio.request.anotation.RequestHandler;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.SysJMX;

public class TopRequest {
	static final int SLEEP_TIME = 1000 * 5;
	static Sigar sigar = new Sigar();
	static SigarProxy proxy = SigarProxyCache.newInstance(sigar, SLEEP_TIME);
	static int cpuCores;

	@RequestHandler(RequestCmd.HOST_TOP)
	public Pack getTop(Pack param) throws SigarException {

		if (cpuCores == 0) {
			cpuCores = getCpuCore();
		}
		MapPack pack = new MapPack();
		ListValue pidLv = pack.newList("PID");
		ListValue userLv = pack.newList("USER");
		ListValue cpuLv = pack.newList("CPU");
		ListValue memLv = pack.newList("MEM");
		ListValue timeLv = pack.newList("TIME");
		ListValue nameLv = pack.newList("NAME");

		long[] pids = proxy.getProcList();
		for (int i = 0; i < pids.length; i++) {
			try {
				pidLv.add(pids[i]);
				try {
					ProcCredName cred = sigar.getProcCredName(pids[i]);
					userLv.add(cred.getUser());
				} catch (Exception e) {
					userLv.add("unknown");
				}

				try {
					ProcTime time = sigar.getProcTime(pids[i]);
					timeLv.add(time.getStartTime());
				} catch (Exception e) {
					timeLv.add(0);
				}
				try {
					String name = ProcUtil.getDescription(sigar, pids[i]);
					nameLv.add(name);
				} catch (Exception e) {
					nameLv.add("");
				}
				try {
					ProcMem mem = sigar.getProcMem(pids[i]);
					memLv.add(mem.getResident());
				} catch (Exception e) {
					memLv.add(0);
				}
				try {
					ProcCpu cpu = sigar.getProcCpu(pids[i]);
					cpuLv.add((float)(cpu.getPercent() * 100 /cpuCores));
				} catch (Exception e) {
					cpuLv.add(0);
				}
			} catch (Exception e) {
			}
		}

		return pack;
	}
	
	private int getCpuCore() {
		try {
			return sigar.getCpuList().length;
		} catch (SigarException e) {
			return 1;
		}
	}
	public static void main(String[] args) throws SigarException {
		ProcMem mem = sigar.getProcMem(SysJMX.getProcessPID());
		System.out.println(Sigar.formatSize(mem.getSize()));
		System.out.println(Sigar.formatSize(mem.getResident()));
		System.out.println(Sigar.formatSize(mem.getRss()));
		System.out.println(Sigar.formatSize(mem.getShare()));
		System.out.println(Sigar.formatSize(mem.getVsize()));
	}
}
