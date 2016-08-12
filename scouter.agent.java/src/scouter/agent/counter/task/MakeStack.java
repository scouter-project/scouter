package scouter.agent.counter.task;

import scouter.agent.Configure;
import scouter.agent.counter.CounterBasket;
import scouter.agent.counter.anotation.Counter;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.proxy.ToolsMainFactory;
import scouter.agent.trace.TraceContext;
import scouter.agent.trace.TraceContextManager;
import scouter.lang.pack.StackPack;
import scouter.lang.step.DumpStep;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Enumeration;

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
		p.objHash = Configure.getInstance().getObjHash();
		p.setStack(stack);

		DataProxy.sendDirect(p);
	}
	
	public static long pstack_requested;
	private boolean isPStackEnabled() {
		return Configure.getInstance().sfa_dump_enabled || System.currentTimeMillis() < pstack_requested;
	}
	private long getInterval() {
		return Configure.getInstance().sfa_dump_interval_ms;
	}


	long lastRegularDumpTime = 0;
	@Counter
	public void regularDump(CounterBasket pw) {

		long now = System.currentTimeMillis();
		if (now < lastRegularDumpTime + 10000) {
			return;
		}
		lastRegularDumpTime = now;

		ThreadMXBean tmxBean = ManagementFactory.getThreadMXBean();
		Enumeration<TraceContext> en = TraceContextManager.getContextEnumeration();
		while (en.hasMoreElements()) {
			TraceContext ctx = en.nextElement();
			if(ctx == null || ctx.threadId <= 0) {
				continue;
			}

			ThreadInfo tInfo = tmxBean.getThreadInfo(ctx.threadId, 50);
			if (tInfo == null) continue;

			StackTraceElement[] elements = tInfo.getStackTrace();
			int length = elements.length;
			int[] stacks = new int[length];

			for(int i=0; i<length; i++) {
				stacks[i] = DataProxy.sendStackElement(elements[i].toString());
			}
			DumpStep dumpStep = new DumpStep();
			dumpStep.stacks = stacks;
			dumpStep.threadId = ctx.threadId;
			dumpStep.threadName = tInfo.getThreadName();
			dumpStep.threadState = tInfo.getThreadState().toString();
			dumpStep.lockOwnerId = tInfo.getLockOwnerId();
			dumpStep.lockName = tInfo.getLockName();
			dumpStep.lockOwnerName = tInfo.getLockOwnerName();

			ctx.temporaryDumpStep = dumpStep;
		}
	}
}
