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
		Configure conf = Configure.getInstance();
		if (conf.pstack_enabled == false)
			return;
		long now = System.currentTimeMillis();
		if (now < lastStackTime + conf.pstack_interval)
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
		p.objHash = conf.objHash;
		p.setStack(stack);

		DataProxy.sendDirect(p);
	}
}
