package scouter.agent.counter.task;

import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetStat;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;
import org.hyperic.sigar.SigarProxyCache;
import org.hyperic.sigar.Swap;

import scouter.agent.Configure;
import scouter.agent.counter.CounterBasket;
import scouter.agent.counter.anotation.Counter;
import scouter.lang.TimeTypeEnum;
import scouter.lang.counters.CounterConstants;
import scouter.lang.pack.PerfCounterPack;
import scouter.lang.value.DecimalValue;
import scouter.lang.value.FloatValue;

public class HostPerf {
	static int SLEEP_TIME = 2000;
	static Sigar sigarImpl = new Sigar();
	static SigarProxy sigar = SigarProxyCache.newInstance(sigarImpl, SLEEP_TIME);

	@Counter
	public void process(CounterBasket pw) {
		try {
			netstat();
			domain(pw);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	void domain(CounterBasket pw) throws SigarException {
		
		Configure conf = Configure.getInstance();

		float cpu = (float) ((1.0D - sigar.getCpuPerc().getIdle()) * 100);
		Mem m = sigar.getMem();

		long tmem = m.getTotal();
		long fmem = m.getFree();
		long umem = m.getUsed();
		float memrate = umem * 100.0f / tmem;

		Swap sw = sigar.getSwap();
		long pagein = sw.getPageIn();
		long pageout = sw.getPageOut();
		long tswap = sw.getTotal();
		long uswap = sw.getUsed();

		PerfCounterPack p = pw.getPack(conf.objName, TimeTypeEnum.REALTIME);
		p.put(CounterConstants.HOST_CPU, new FloatValue(cpu));
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

		p = pw.getPack(conf.objName, TimeTypeEnum.FIVE_MIN);
		p.put(CounterConstants.HOST_CPU, new FloatValue(0));
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
		if (now - last_time < 5000) {
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
	
}
