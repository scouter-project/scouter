
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

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.hardware.VirtualMemory;
import oshi.software.os.FileSystem;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.InternetProtocolStats.TcpState;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.counter.CounterBasket;
import scouter.agent.counter.anotation.Counter;
import scouter.agent.counter.meter.MeterResource;
import scouter.agent.netio.data.HostAgentDataProxy;
import scouter.lang.AlertLevel;
import scouter.lang.TimeTypeEnum;
import scouter.lang.counters.CounterConstants;
import scouter.lang.pack.PerfCounterPack;
import scouter.lang.value.DecimalValue;
import scouter.lang.value.FloatValue;
import scouter.util.FormatUtil;
import scouter.util.LongEnumer;
import scouter.util.LongKeyLinkedMap;
import scouter.util.ThreadUtil;

import java.util.List;

public class HostPerf {
	SystemInfo si = new SystemInfo();
	OperatingSystem os = si.getOperatingSystem();
	HardwareAbstractionLayer hal = si.getHardware();

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

	void domain(CounterBasket pw) {

		Configure conf = Configure.getInstance();

		CentralProcessor processor = hal.getProcessor();
		long[] oldTicks = processor.getSystemCpuLoadTicks();
		ThreadUtil.sleep(500);
		long[] ticks = processor.getSystemCpuLoadTicks();

		long total = 0;
		for (int i = 0; i < ticks.length; i++) {
			total += ticks[i] - oldTicks[i];
		}

		long idle = ticks[CentralProcessor.TickType.IDLE.getIndex()] - oldTicks[CentralProcessor.TickType.IDLE.getIndex()];
		long sys = ticks[CentralProcessor.TickType.SYSTEM.getIndex()] - oldTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
		long user = ticks[CentralProcessor.TickType.USER.getIndex()] - oldTicks[CentralProcessor.TickType.USER.getIndex()];
		float cpuUsage = total > 0 ? (float) (total - idle) / total : 0f;
		float sysUsage = total > 0 ? (float) sys / total : 0f;
		float userUsage = total > 0 ? (float) user / total : 0f;
		cpuUsage *= 100;
		sysUsage *= 100;
		userUsage *= 100;

		cpuMeter.add(cpuUsage);
		sysCpuMeter.add(sysUsage);
		userCpuMeter.add(userUsage);

		cpuUsage = (float) cpuMeter.getAvg(conf._cpu_value_avg_sec);
		sysUsage = (float) sysCpuMeter.getAvg(conf._cpu_value_avg_sec);
		userUsage = (float) userCpuMeter.getAvg(conf._cpu_value_avg_sec);

		alertCpu(cpuUsage);

		GlobalMemory memory = hal.getMemory();
		long mAvailable = memory.getAvailable();
		long mTotal = memory.getTotal();
		float mUsageRate = mTotal > 0 ? (float) (mTotal - mAvailable) / mTotal * 100 : 0f;

		alertMem(mAvailable, mUsageRate);

		memory.getPageSize();
		VirtualMemory vm = memory.getVirtualMemory();
		long swapPagesIn = vm.getSwapPagesIn();
		long swapPagesOut = vm.getSwapPagesOut();
		long swapTotal = vm.getSwapTotal();
		long swapUsed = vm.getSwapUsed();
		float swapRate = swapTotal > 0 ? swapUsed * 100.0f / swapTotal: 0f;

		PerfCounterPack p = pw.getPack(conf.getObjName(), TimeTypeEnum.REALTIME);
		p.put(CounterConstants.HOST_CPU, new FloatValue(cpuUsage));
		p.put(CounterConstants.HOST_SYSCPU, new FloatValue(sysUsage));
		p.put(CounterConstants.HOST_USERCPU, new FloatValue(userUsage));
		p.put(CounterConstants.HOST_MEM, new FloatValue(mUsageRate));
		p.put(CounterConstants.HOST_MEM_TOTAL, new DecimalValue(mTotal / 1024 / 1024));
		p.put(CounterConstants.HOST_MEM_USED, new DecimalValue((mTotal - mAvailable) / 1024 / 1024));
		p.put(CounterConstants.HOST_MEM_AVALIABLE, new DecimalValue(mAvailable / 1024 / 1024));
		p.put(CounterConstants.HOST_SWAP_PAGE_IN, new DecimalValue(swapPagesIn));
		p.put(CounterConstants.HOST_SWAP_PAGE_OUT, new DecimalValue(swapPagesOut));
		p.put(CounterConstants.HOST_SWAP, new FloatValue(swapRate));
		p.put(CounterConstants.HOST_SWAP_TOTAL, new DecimalValue(swapTotal / 1024 / 1024));
		p.put(CounterConstants.HOST_SWAP_USED, new DecimalValue(swapUsed / 1024 / 1024));

		p.put(CounterConstants.HOST_NET_IN, new DecimalValue(net_in));
		p.put(CounterConstants.HOST_NET_OUT, new DecimalValue(net_out));
		p.put(CounterConstants.HOST_TCPSTAT_CLS, new DecimalValue(tcpstat_close));
		p.put(CounterConstants.HOST_TCPSTAT_FIN, new DecimalValue(tcpstat_fin1 + tcpstat_fin2));
		p.put(CounterConstants.HOST_TCPSTAT_TIM, new DecimalValue(tcpstat_time));
		p.put(CounterConstants.HOST_TCPSTAT_EST, new DecimalValue(tcpstat_est));

		p = pw.getPack(conf.getObjName(), TimeTypeEnum.FIVE_MIN);
		p.put(CounterConstants.HOST_CPU, new FloatValue(cpuUsage));
		p.put(CounterConstants.HOST_SYSCPU, new FloatValue(sysUsage));
		p.put(CounterConstants.HOST_USERCPU, new FloatValue(userUsage));
		p.put(CounterConstants.HOST_MEM, new FloatValue(mUsageRate));
		p.put(CounterConstants.HOST_MEM_TOTAL, new DecimalValue(mTotal / 1024 / 1024));
		p.put(CounterConstants.HOST_MEM_USED, new DecimalValue(mTotal / 1024 / 1024));
		p.put(CounterConstants.HOST_MEM_AVALIABLE, new DecimalValue(mAvailable / 1024 / 1024));
		p.put(CounterConstants.HOST_SWAP_PAGE_IN, new DecimalValue(swapPagesIn));
		p.put(CounterConstants.HOST_SWAP_PAGE_OUT, new DecimalValue(swapPagesOut));
		p.put(CounterConstants.HOST_SWAP_TOTAL, new DecimalValue(swapTotal / 1024 / 1024));
		p.put(CounterConstants.HOST_SWAP_USED, new DecimalValue(swapUsed / 1024 / 1024));

		p.put(CounterConstants.HOST_NET_IN, new DecimalValue(net_in));
		p.put(CounterConstants.HOST_NET_OUT, new DecimalValue(net_out));
		p.put(CounterConstants.HOST_TCPSTAT_SS, new DecimalValue(tcpstat_ss));
		p.put(CounterConstants.HOST_TCPSTAT_SR, new DecimalValue(tcpstat_sr));
		p.put(CounterConstants.HOST_TCPSTAT_EST, new DecimalValue(tcpstat_est));
		p.put(CounterConstants.HOST_TCPSTAT_FIN, new DecimalValue(tcpstat_fin1 + tcpstat_fin2));
		p.put(CounterConstants.HOST_TCPSTAT_TIM, new DecimalValue(tcpstat_time));
		p.put(CounterConstants.HOST_TCPSTAT_CLS, new DecimalValue(tcpstat_close));

	}

	long mem_last_fatal;
	long mem_last_warning;
	private void alertMem(long available, float usage) {
		Configure conf = Configure.getInstance();
		if(conf.mem_alert_enabled==false)
			return;

		long now = System.currentTimeMillis();

		if (usage >= conf.mem_fatal_pct) {
			if (now >= mem_last_fatal + conf.mem_alert_interval_ms) {
				HostAgentDataProxy.sendAlert(AlertLevel.FATAL, "FATAL_MEMORY_HIGH", "fatal mem usage free=" + prt(available)
						+ " rate=" + usage + "%", null);
				mem_last_fatal = now;
			}
			return;
		}
		if (usage >= conf.mem_warning_pct) {
			if (now >= mem_last_warning + conf.mem_alert_interval_ms) {
				HostAgentDataProxy.sendAlert(AlertLevel.WARN, "WARNING_MEMORY_HIGH", "warning mem usage free=" + prt(available)
						+ " rate=" + usage + "%", null);
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
					HostAgentDataProxy.sendAlert(AlertLevel.FATAL, "FATAL_CPU_HIGH", "cpu high " + nextCpu + "%", null);
					cpu_last_fatal = now;
				}
				return;
			}
		}
		if (nextCpu >= conf.cpu_warning_pct) {
			if (f + w >= conf.cpu_warning_history) {
				if (now >= cpu_last_warning + conf.cpu_alert_interval_ms) {
					HostAgentDataProxy.sendAlert(AlertLevel.WARN, "WARNING_CPU_HIGH", "cpu high " + nextCpu + "%", null);
					cpu_last_warning = now;
				}
				return;
			}
		}
	}

	long last_time = 0;
	private int net_in;
	private int net_out;
	private int tcpstat_ss;
	private int tcpstat_sr;
	private int tcpstat_est;
	private int tcpstat_close;
	private int tcpstat_fin1;
	private int tcpstat_fin2;
	private int tcpstat_time;

	void netstat() {
		long now = System.currentTimeMillis();
		if (now - last_time < 10000) {
			return;
		}
		last_time = now;

		InternetProtocolStats.TcpStats stat = os.getInternetProtocolStats().getTCPv4Stats();
		stat.getConnectionsEstablished();

		List<InternetProtocolStats.IPConnection> connections = os.getInternetProtocolStats().getConnections();

		int tcpSs = 0;
		int tcpSr = 0;
		int tcpEsta = 0;
		int tcpFin1 = 0;
		int tcpFin2 = 0;
		int tcpTimeWait = 0;
		int tcpCloseWait = 0;

		for (int i = 0; i < connections.size(); i++) {
			TcpState state = connections.get(i).getState();
			switch (state) {
				case SYN_SENT:
					tcpSs++;
					break;
				case SYN_RECV:
					tcpSr++;
					break;
				case ESTABLISHED:
					tcpEsta++;
					break;
				case FIN_WAIT_1:
					tcpFin1++;
					break;
				case FIN_WAIT_2:
					tcpFin2++;
					break;
				case TIME_WAIT:
					tcpTimeWait++;
					break;
				case CLOSE_WAIT:
					tcpCloseWait++;
					break;
				default:

			}
		}

		List<NetworkIF> networkIFs = hal.getNetworkIFs();
		long bytesReceived = 0;
		long bytesSent = 0;
		long packetReceived = 0;
		long packetSent = 0;
		for (int i = 0; i < networkIFs.size(); i++) {
			bytesReceived += networkIFs.get(i).getBytesRecv();
			bytesSent += networkIFs.get(i).getBytesSent();
			packetReceived += networkIFs.get(i).getPacketsRecv();
			packetSent += networkIFs.get(i).getPacketsSent();

		}

		this.net_in = (int) bytesReceived;
		this.net_out = (int) bytesSent;
		this.tcpstat_ss = tcpSs;
		this.tcpstat_sr = tcpSr;
		this.tcpstat_est = tcpEsta;
		this.tcpstat_fin1 = tcpFin1;
		this.tcpstat_fin2 = tcpFin2;
		this.tcpstat_time = tcpTimeWait;
		this.tcpstat_close = tcpCloseWait;

	}

	@Counter(interval = 3600000)
	public void disk(CounterBasket pw) {
		Configure conf = Configure.getInstance();
		if (conf.disk_alert_enabled == false)
			return;

		StringBuffer fatal = new StringBuffer();
		StringBuffer warn = new StringBuffer();

		try {
			FileSystem fsm = os.getFileSystem();
			List<OSFileStore> fst = fsm.getFileStores();

			for (int i = 0; i < fst.size(); i++) {
				String mount = fst.get(i).getMount();

				long total = 0;
				long free = 0;
				float usage = 0;
				try {
					total = fst.get(i).getTotalSpace();
					free = fst.get(i).getFreeSpace();
					usage = ((float) total - free) / total * 100.0f;
				} catch (Exception e) {
					Logger.println("A160", 300, "disk:" + mount + ", err:" + e.getMessage());
				}

				if (conf.disk_ignore_names.hasKey(mount))
					continue;

				if (conf.disk_ignore_size_gb < total / 1024 / 1024 / 1024)
					continue;

				if (usage >= conf.disk_fatal_pct && fatal.length() < 32756) {
					if (fatal.length() > 0) {
						fatal.append("\n");
					}
					fatal.append(mount).append(" usage ").append((int) usage).append("% total=")
							.append(FormatUtil.print(total / 1024.0 / 1024 / 1024, "#0.0#")).append("GB.. available=")
							.append(prt(free));

				} else if (usage >= conf.disk_warning_pct && warn.length() < 32756) {
					if (warn.length() > 0) {
						warn.append("\n");
					}
					warn.append(mount).append(" usage ").append((int) usage).append("% total=")
							.append(FormatUtil.print(total / 1024.0 / 1024 / 1024, "#0.0#")).append("GB.. available=")
							.append(prt(free));
				}
			}

			if (fatal.length() > 0) {
				HostAgentDataProxy.sendAlert(AlertLevel.FATAL, "FATAL_DISK_USAGE", fatal.toString(), null);
			}
			if (warn.length() > 0) {
				HostAgentDataProxy.sendAlert(AlertLevel.WARN, "WARNING_DISK_USAGE", warn.toString(), null);
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
