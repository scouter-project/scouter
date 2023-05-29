package scouter.agent.counter.task;

import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.counter.CounterBasket;
import scouter.agent.counter.anotation.Counter;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.proxy.ToolsMainFactory;
import scouter.agent.trace.TraceContext;
import scouter.agent.trace.TraceContextManager;
import scouter.agent.trace.TraceMain;
import scouter.lang.pack.StackPack;
import scouter.lang.step.DumpStep;
import scouter.util.StringUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public class MakeStack {
    static Configure conf = Configure.getInstance();

    public long lastStackTime;

    @Counter
    public void make(CounterBasket pw) {
        if (isPStackEnabled() == false) {
            ToolsMainFactory.activeStack = false;
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
        if (!conf._psts_enabled) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now < lastStackTraceGenTime + conf._psts_dump_interval_ms) {
            return;
        }
        lastStackTraceGenTime = now;
        int minMs = conf._psts_dump_min_ms;
        int maxCount = conf._psts_dump_max_count;

        ThreadMXBean tmxBean = ManagementFactory.getThreadMXBean();

        List<TraceContext> ctxList = new ArrayList<TraceContext>();
        int doCount = 0;
        for (Map.Entry<Long, TraceContext> e : TraceContextManager.getContextEntries()) {
            if (maxCount > 0 && doCount >= maxCount) {
                break;
            }
            doCount++;
            TraceContext ctx = e.getValue();
            if (ctx != null) {
                long elapsed = (System.currentTimeMillis() - ctx.startTime);
                if (minMs <= 0 || elapsed >= minMs) {
                    ctxList.add(ctx);
                }
            }
        }

        for (TraceContext ctx : ctxList) {
            if (ctx.isReactiveStarted) {
                reactiveStepDump(tmxBean, ctx);
            } else {
                stepDump(tmxBean, ctx);
            }
        }
    }

    private void stepDump(ThreadMXBean tmxBean, TraceContext ctx) {
        if (ctx == null || ctx.threadId <= 0) {
            return;
        }

        ThreadInfo tInfo = tmxBean.getThreadInfo(ctx.threadId, 50);
        if (tInfo != null) {
            StackTraceElement[] elements = tInfo.getStackTrace();
            int length = elements.length;
            int[] stacks = new int[length];

            for (int i = 0; i < length; i++) {
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
        } else {
            Thread t = ctx.thread;
            if (t != null) {
                StackTraceElement[] elements = t.getStackTrace();
                int length = Math.min(elements.length, 50);
                int[] stacks = new int[length];

                for (int i = 0; i < length; i++) {
                    stacks[i] = DataProxy.sendStackElement(elements[i]);
                }
                DumpStep dumpStep = new DumpStep();
                dumpStep.start_time = (int) (System.currentTimeMillis() - ctx.startTime);
                dumpStep.stacks = stacks;
                dumpStep.threadId = ctx.threadId;
                dumpStep.threadName = StringUtil.isEmpty(t.getName()) ? t.toString() : t.getName();
                dumpStep.threadState = t.getState().name();

                ctx.temporaryDumpSteps.offer(dumpStep);
                ctx.hasDumpStack = true;
            }
        }
    }

    private void reactiveStepDump(ThreadMXBean tmxBean, TraceContext ctx) {
        if (ctx == null) {
            return;
        }

        long now = System.currentTimeMillis();

        List<Integer> stacks = new ArrayList<Integer>();

        DumpStep dumpStep = new DumpStep();
        dumpStep.start_time = (int) (System.currentTimeMillis() - ctx.startTime);

        long threadId = TraceContextManager.getReactiveThreadId(ctx.txid);
        if (threadId != 0) {
            ThreadInfo tInfo = tmxBean.getThreadInfo(ctx.threadId, 50);
            if (tInfo != null) {
                StackTraceElement[] elements = tInfo.getStackTrace();
                for (StackTraceElement element : elements) {
                    stacks.add(DataProxy.sendStackElement(element));
                }

                dumpStep.threadId = threadId;
                dumpStep.threadName = tInfo.getThreadName();
                dumpStep.threadState = tInfo.getThreadState().toString();
                dumpStep.lockOwnerId = tInfo.getLockOwnerId();
                dumpStep.lockName = tInfo.getLockName();
                dumpStep.lockOwnerName = tInfo.getLockOwnerName();
            }
        }

        if (ctx.scannables != null) {
            Enumeration<TraceContext.TimedScannable> en = ctx.scannables.values();
            stacks.add(DataProxy.sendStackElement("<<<<<<<<<< currently existing subscribes >>>>>>>>>>"));
            while (en.hasMoreElements()) {
                TraceContext.TimedScannable ts = en.nextElement();
                if (ts == null) {
                    return;
                }
                String dumpScannable = TraceMain.reactiveSupport.dumpScannable(ctx, ts, now);
                stacks.add(DataProxy.sendStackElement(dumpScannable));
            }

            dumpStep.stacks = convertIntegers(stacks);
        }
        ctx.temporaryDumpSteps.offer(dumpStep);
        ctx.hasDumpStack = true;
    }

    private static int[] convertIntegers(List<Integer> integers) {
        int[] ret = new int[integers.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = integers.get(i);
        }
        return ret;
    }

}
