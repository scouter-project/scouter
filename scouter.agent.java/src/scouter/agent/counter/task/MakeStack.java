package scouter.agent.counter.task;

import java.io.PrintWriter;
import java.io.StringWriter;

import scouter.agent.Configure;
import scouter.agent.counter.CounterBasket;
import scouter.agent.counter.anotation.Counter;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.proxy.ToolsMainFactory;
import scouter.lang.pack.StackPack;

public class MakeStack {

	public long lastStackTime;
	@Counter
	public void make(CounterBasket pw) {
		if (isPStackEnabled()== false){
			ToolsMainFactory.activeStack=false;
			return;
		}
		long now = System.currentTimeMillis();
		if (now < lastStackTime + getInterval())
			return;
		lastStackTime = now;
		StringWriter sw = new StringWriter();
		PrintWriter out = new PrintWriter(sw);
		try {
			ToolsMainFactory.threadDump(out);
		} catch (Throwable e) {
		} finally {
			out.close();
		}

		String stack = sw.getBuffer().toString();

		StackPack p = new StackPack();
		p.time = System.currentTimeMillis();
		p.objHash = Configure.getInstance().objHash;
		p.setStack(stack);

		DataProxy.sendDirect(p);
	}
	
	public static long pstack_requested;
	private boolean isPStackEnabled() {
		return Configure.getInstance().pstack_enabled || System.currentTimeMillis() < pstack_requested;
	}
	private long getInterval() {
		return Configure.getInstance().pstack_interval;
	}
}
