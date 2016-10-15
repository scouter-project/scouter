package scouter.agent.counter.task;

import scouter.agent.Configure;
import scouter.agent.Logger;
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
	static Configure conf = Configure.getInstance();

	public long lastStackTime;
	@Counter
	public void make(CounterBasket pw) {
		if (isPStackEnabled()== false){
			ToolsMainFactory.activeStack=false;
			return;
		}
		long now = System.currentTimeMillis();
		if (now < lastStackTime + getSFAInterval())
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
		p.objHash = conf.getObjHash();
		p.setStack(stack);

		DataProxy.sendDirect(p);

		long elapsed = (System.currentTimeMillis() - now);
		Logger.trace("[SFA Counter Elasped]" + elapsed);
	}
	
	public static long pstack_requested;
	private boolean isPStackEnabled() {
		return conf.sfa_dump_enabled || System.currentTimeMillis() < pstack_requested;
	}
	private long getSFAInterval() {
		return conf.sfa_dump_interval_ms;
	}


	long lastStackTraceGenTime = 0;
	@Counter
	public void stackTraceStepGenerator(CounterBasket pw) {
		if (!conf._psts_enabled){
			return;
		}

		long now = System.currentTimeMillis();
		if (now < lastStackTraceGenTime + conf._psts_dump_interval_ms) {
			return;
		}
		lastStackTraceGenTime = now;

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
				stacks[i] = DataProxy.sendStackElement(elements[i]);
			}
			DumpStep dumpStep = new DumpStep();
			dumpStep.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
			dumpStep.stacks = stacks;
			dumpStep.threadId = ctx.threadId;
			dumpStep.threadName = tInfo.getThreadName();
			dumpStep.threadState = tInfo.getThreadState().toString();
			dumpStep.lockOwnerId = tInfo.getLockOwnerId();
			dumpStep.lockName = tInfo.getLockName();
			dumpStep.lockOwnerName = tInfo.getLockOwnerName();

			ctx.temporaryDumpSteps.offer(dumpStep);
			ctx.hasDumpStack = true;
		}

		long elapsed = (System.currentTimeMillis() - now);
		Logger.trace("[ASTS Elasped]" + elapsed);
	}
}
