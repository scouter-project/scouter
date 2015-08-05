package scouter.agent.netio.request.handle;

import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.ProcCredName;
import org.hyperic.sigar.ProcMem;
import org.hyperic.sigar.ProcTime;
import org.hyperic.sigar.ProcUtil;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;
import org.hyperic.sigar.SigarProxyCache;

import scouter.agent.netio.request.anotation.RequestHandler;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.DateUtil;

public class TopRequest {

	@RequestHandler(RequestCmd.HOST_TOP)
	public Pack getTop(Pack param) throws SigarException {

		MapPack pack = new MapPack();
		ListValue pidLv = pack.newList("PID");
		ListValue userLv = pack.newList("USER");
		ListValue cpuLv = pack.newList("CPU");
		ListValue memLv = pack.newList("MEM");
		ListValue timeLv = pack.newList("TIME");
		ListValue nameLv = pack.newList("NAME");

		Sigar sigar = new Sigar();
		SigarProxy proxy = SigarProxyCache.newInstance(sigar);

		long[] pids = proxy.getProcList();
		for (int i = 0; i < pids.length; i++) {
			pidLv.add(pids[i]);
			ProcCredName cred = sigar.getProcCredName(pids[i]);
			userLv.add(cred.getUser());
			try {
				ProcMem mem = sigar.getProcMem(pids[i]);
				memLv.add(Sigar.formatSize(mem.getSize()));
			} catch (Exception e) {
				memLv.add(0);
			}
			try {
				ProcCpu cpu = sigar.getProcCpu(pids[i]);
				cpuLv.add(cpu.getPercent());
			} catch (Exception e) {
				e.printStackTrace();
				cpuLv.add(0);
			}
			ProcTime time = sigar.getProcTime(pids[i]);
			timeLv.add(System.currentTimeMillis() - time.getStartTime());
			String name = ProcUtil.getDescription(sigar, pids[i]);
			nameLv.add(name);
		}

		return pack;
	}
}
