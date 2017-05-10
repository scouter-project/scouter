
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package scouter.agent.counter.task;

import org.hyperic.sigar.*;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.counter.CounterBasket;
import scouter.agent.counter.anotation.Counter;
import scouter.agent.counter.meter.MeterResource;
import scouter.agent.netio.data.DataProxy;
import scouter.lang.AlertLevel;
import scouter.lang.TimeTypeEnum;
import scouter.lang.counters.CounterConstants;
import scouter.lang.pack.PerfCounterPack;
import scouter.lang.value.DecimalValue;
import scouter.lang.value.FloatValue;
import scouter.util.FormatUtil;
import scouter.util.LongEnumer;
import scouter.util.LongKeyLinkedMap;

public class HostPerf {
	static int SLEEP_TIME = 2000;
	static Sigar sigarImpl = new Sigar();
	static SigarProxy sigar = SigarProxyCache.newInstance(sigarImpl, SLEEP_TIME);
	
	MeterResource cpuMeter = new MeterResource();
	MeterResource sysCpuMeter = new MeterResource();
	MeterResource userCpuMeter = new MeterResource();

	@Counter
	public void process(CounterBasket pw) {
		try {
			if (Configure.getInstance().counter_netstat_enabled) {
				netstat();
			}
			domain(pw);
		} catch (Exception e) {
			Logger.println("A140", 10, "HostPerf", e);
		}
	}

	void domain(CounterBasket pw) throws SigarException {

		Configure conf = Configure.getInstance();

		CpuPerc cpuPerc = sigar.getCpuPerc();
		float cpu = (float) ((1.0D - cpuPerc.getIdle()) * 100);
		cpuMeter.add(cpu);
		float sysCpu = (float) cpuPerc.getSys() * 100;
		sysCpuMeter.add(sysCpu);
		float userCpu = (float) cpuPerc.getUser() * 100;
		userCpuMeter.add(userCpu);
		
		cpu = (float) cpuMeter.getAvg(conf._cpu_value_avg_sec);
		sysCpu = (float) sysCpuMeter.getAvg(conf._cpu_value_avg_sec);
		userCpu = (float) userCpuMeter.getAvg(conf._cpu_value_avg_sec);

		alertCpu(cpu);
		
		Mem m = sigar.getMem();
		alertMem(m);

		long tmem = m.getTotal();
		long fmem = m.getActualFree();
		long umem = m.getActualUsed();
		float memrate = (float) m.getUsedPercent();

		Swap sw = sigar.getSwap();
		long pagein = sw.getPageIn();
		long pageout = sw.getPageOut();
		long tswap = sw.getTotal();
		long uswap = sw.getUsed();
		float swaprate = (tswap == 0) ? 0 : uswap * 100.0f / tswap;

		PerfCounterPack p = pw.getPack(conf.getObjName(), TimeTypeEnum.REALTIME);
		p.put(CounterConstants.HOST_CPU, new FloatValue(cpu));
		p.put(CounterConstants.HOST_SYSCPU, new FloatValue(sysCpu));
		p.put(CounterConstants.HOST_USERCPU, new FloatValue(userCpu));
		p.put(CounterConstants.HOST_MEM, new FloatValue(memrate));
		p.put(CounterConstants.HOST_MEM_TOTAL, new DecimalValue(tmem / 1024 / 1024));
		p.put(CounterConstants.HOST_MEM_USED, new DecimalValue(umem / 1024 / 1024));
		p.put(CounterConstants.HOST_MEM_AVALIABLE, new DecimalValue(fmem / 1024 / 1024));
		p.put(CounterConstants.HOST_SWAP_PAGE_IN, new DecimalValue(pagein));
		p.put(CounterConstants.HOST_SWAP_PAGE_OUT, new DecimalValue(pageout));
		p.put(CounterConstants.HOST_SWAP, new FloatValue(swaprate));
		p.put(CounterConstants.HOST_SWAP_TOTAL, new DecimalValue(tswap / 1024 / 1024));
		p.put(CounterConstants.HOST_SWAP_USED, new DecimalValue(uswap / 1024 / 1024));

		p.put(CounterConstants.HOST_NET_IN, new DecimalValue(net_in));
		p.put(CounterConstants.HOST_NET_OUT, new DecimalValue(net_out));
		p.put(CounterConstants.HOST_TCPSTAT_CLS, new DecimalValue(tcpstat_close));
		p.put(CounterConstants.HOST_TCPSTAT_FIN, new DecimalValue(tcpstat_fin1 + tcpstat_fin2));
		p.put(CounterConstants.HOST_TCPSTAT_TIM, new DecimalValue(tcpstat_time));
		p.put(CounterConstants.HOST_TCPSTAT_EST, new DecimalValue(tcpstat_est));

		p.put(CounterConstants.HOST_NET_RX_BYTES, new DecimalValue(HostNetDiskPerf.getRxTotalBytesPerSec()));
		p.put(CounterConstants.HOST_NET_TX_BYTES, new DecimalValue(HostNetDiskPerf.getTxTotalBytesPerSec()));

		p.put(CounterConstants.HOST_DISK_READ_BYTES, new DecimalValue(HostNetDiskPerf.getReadTotalBytesPerSec()));
		p.put(CounterConstants.HOST_DISK_WRITE_BYTES, new DecimalValue(HostNetDiskPerf.getWriteTotalBytesPerSec()));

//		System.out.println("rx:" + HostNetDiskPerf.getRxTotalBytesPerSec());
//		System.out.println("tx:" + HostNetDiskPerf.getTxTotalBytesPerSec());
//		System.out.println("read:" + HostNetDiskPerf.getReadTotalBytesPerSec());
//		System.out.println("write:" + HostNetDiskPerf.getWriteTotalBytesPerSec());

		p = pw.getPack(conf.getObjName(), TimeTypeEnum.FIVE_MIN);
		p.put(CounterConstants.HOST_CPU, new FloatValue(cpu));
		p.put(CounterConstants.HOST_SYSCPU, new FloatValue(sysCpu));
		p.put(CounterConstants.HOST_USERCPU, new FloatValue(userCpu));
		p.put(CounterConstants.HOST_MEM, new FloatValue(memrate));
		p.put(CounterConstants.HOST_MEM_TOTAL, new DecimalValue(tmem / 1024 / 1024));
		p.put(CounterConstants.HOST_MEM_USED, new DecimalValue(umem / 1024 / 1024));
		p.put(CounterConstants.HOST_MEM_AVALIABLE, new DecimalValue(fmem / 1024 / 1024));
		p.put(CounterConstants.HOST_SWAP_PAGE_IN, new DecimalValue(pagein));
		p.put(CounterConstants.HOST_SWAP_PAGE_OUT, new DecimalValue(pageout));
		p.put(CounterConstants.HOST_SWAP_TOTAL, new DecimalValue(tswap / 1024 / 1024));
		p.put(CounterConstants.HOST_SWAP_USED, new DecimalValue(uswap / 1024 / 1024));

		p.put(CounterConstants.HOST_NET_IN, new DecimalValue(net_in));
		p.put(CounterConstants.HOST_NET_OUT, new DecimalValue(net_out));
		p.put(CounterConstants.HOST_TCPSTAT_CLS, new DecimalValue(tcpstat_close));
		p.put(CounterConstants.HOST_TCPSTAT_FIN, new DecimalValue(tcpstat_fin1 + tcpstat_fin2));
		p.put(CounterConstants.HOST_TCPSTAT_TIM, new DecimalValue(tcpstat_time));
		p.put(CounterConstants.HOST_TCPSTAT_EST, new DecimalValue(tcpstat_est));

		SigarProxyCache.clear(sigar);
	}

	long mem_last_fatal;
	long mem_last_warning;
	private void alertMem(Mem m) {
		Configure conf = Configure.getInstance();
		if(conf.mem_alert_enabled==false)
			return;
		
		long fmem = m.getActualFree();
		float memrate = (float) m.getUsedPercent();

		long now = System.currentTimeMillis();

		if (memrate >= conf.mem_fatal_pct) {
			if (now >= mem_last_fatal + conf.mem_alert_interval_ms) {
				DataProxy.sendAlert(AlertLevel.FATAL, "FATAL_MEMORY_HIGH", "fatal mem usage free=" + prt(fmem)
						+ " rate=" + memrate + "%", null);
				mem_last_fatal = now;
			}
			return;
		}
		if (memrate >= conf.mem_warning_pct) {
			if (now >= mem_last_warning + conf.mem_alert_interval_ms) {
				DataProxy.sendAlert(AlertLevel.WARN, "WARNING_MEMORY_HIGH", "warning mem usage free=" + prt(fmem)
						+ " rate=" + memrate + "%", null);
				mem_last_warning = now;
			}
			return;
		}
	}

	LongKeyLinkedMap<Integer> oldCpu = new LongKeyLinkedMap<Integer>().setMax(1000);
	long cpu_last_fatal;
	long cpu_last_warning;

	private void alertCpu(float nextCpu) {
		Configure conf = Configure.getInstance();
		if(conf.cpu_alert_enabled==false)
			return;


		if (nextCpu < conf.cpu_warning_pct)
			return;

		long now = System.currentTimeMillis();

		int w = 0, f = 0;
		long stime = System.currentTimeMillis() - conf.cpu_check_period_ms;
		LongEnumer en = oldCpu.keys();
		while (en.hasMoreElements()) {
			long tm = en.nextLong();
			if (tm < stime)
				continue;

			int cpu = oldCpu.get(tm);
			if (cpu >= conf.cpu_fatal_pct) {
				f++;
			} else if (cpu >= conf.cpu_warning_pct) {
				w++;
			}
		}
		oldCpu.put(System.currentTimeMillis(), new Integer((int) nextCpu));

		if (nextCpu >= conf.cpu_fatal_pct) {
			if (f >= conf.cpu_fatal_history) {
				if (now >= cpu_last_fatal + conf.cpu_alert_interval_ms) {
					DataProxy.sendAlert(AlertLevel.FATAL, "FATAL_CPU_HIGH", "cpu high " + nextCpu + "%", null);
					cpu_last_fatal = now;
				}
				return;
			}
		}
		if (nextCpu >= conf.cpu_warning_pct) {
			if (f + w >= conf.cpu_warning_history) {
				if (now >= cpu_last_warning + conf.cpu_alert_interval_ms) {
					DataProxy.sendAlert(AlertLevel.WARN, "WARNING_CPU_HIGH", "cpu high " + nextCpu + "%", null);
					cpu_last_warning = now;
				}
				return;
			}
		}
	}

	long last_time = 0;
	private int net_in;
	private int net_out;
	private int tcpstat_close;
	private int tcpstat_fin1;
	private int tcpstat_fin2;
	private int tcpstat_time;
	private int tcpstat_est;

	void netstat() throws SigarException {
		long now = System.currentTimeMillis();
		if (now - last_time < 10000) {
			return;
		}
		last_time = now;

		NetStat net = sigar.getNetStat();
		this.net_in = net.getAllInboundTotal();
		this.net_out = net.getAllOutboundTotal();
		this.tcpstat_close = net.getTcpCloseWait();
		this.tcpstat_fin1 = net.getTcpFinWait1();
		this.tcpstat_fin2 = net.getTcpFinWait2();
		this.tcpstat_time = net.getTcpTimeWait();
		this.tcpstat_est = net.getTcpEstablished();
	}

	@Counter(interval = 3600000)
	public void disk(CounterBasket pw) {
		Configure conf = Configure.getInstance();
		if (conf.disk_alert_enabled == false)
			return;

		StringBuffer fatal = new StringBuffer();
		StringBuffer warn = new StringBuffer();

		try {
			FileSystem[] fslist = sigar.getFileSystemList();
			for (int i = 0; i < fslist.length; i++) {

				FileSystem fs = fslist[i];
				FileSystemUsage usage;
				if (fs instanceof NfsFileSystem) {
					NfsFileSystem nfs = (NfsFileSystem) fs;
					if (!nfs.ping()) {
						continue;
					}
				}
				String dir = fs.getDirName();
				if (conf.disk_ignore_names.hasKey(dir))
					continue;

				try {
					usage = sigar.getFileSystemUsage(dir);
				} catch (SigarException e) {
					continue;
				}

				float pct = (float) (usage.getUsePercent() * 100);
				if (pct >= conf.disk_fatal_pct && fatal.length() < 32756) {
					long avail = usage.getAvail();
					long total = usage.getTotal();
					if (fatal.length() > 0) {
						fatal.append("\n");
					}
					fatal.append(dir).append(" usage ").append((int) pct).append("% total=")
							.append(FormatUtil.print(total / 1024.0 / 1024, "#0.0#")).append("GB.. available=").append(prt(avail * 1024));
				} else if (pct >= conf.disk_warning_pct && warn.length() < 32756) {
					long avail = usage.getAvail();
					long total = usage.getTotal();
					if (warn.length() > 0) {
						warn.append("\n");
					}
					warn.append(dir).append(" usage ").append((int) pct).append("% total=")
							.append(FormatUtil.print(total / 1024.0 / 1024, "#0.0#")).append("GB.. available=").append(prt(avail * 1024));
				}

			}
			if (fatal.length() > 0) {
				DataProxy.sendAlert(AlertLevel.FATAL, "FATAL_DISK_USAGE", fatal.toString(), null);
			}
			if (warn.length() > 0) {
				DataProxy.sendAlert(AlertLevel.WARN, "WARNING_DISK_USAGE", warn.toString(), null);
			}

		} catch (Throwable t) {
			Logger.println("DISK", 60, "disk perf error", t);
		}
	}

	private String prt(long free) {
		if (free < 1024)
			return free + " B";
		free /= 1024;
		if (free < 1024)
			return free + " KB";
		free /= 1024;
		if (free < 1024)
			return free + " MB";
		free /= 1024;
		return FormatUtil.print(free, "#,##0") + " GB";
	}
	
	public static void main(String[] args) {
		long total = 9126805504L / 1024 / 1204 / 1024;
		System.out.println(total);
	}
}
