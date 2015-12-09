package scouter.agent.counter.task;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.counter.CounterBasket;
import scouter.agent.counter.anotation.Counter;
import scouter.agent.trace.TraceContext;
import scouter.agent.trace.TraceContextManager;
import scouter.agent.util.DumpUtil;
import scouter.util.DateUtil;
import scouter.util.FileUtil;
import scouter.util.Hexa32;
import scouter.util.SysJMX;
public class Debug {
	@Counter
	public void autoStack(CounterBasket pw) {
		Configure conf = Configure.getInstance();
		if (conf.autodump_stuck_thread_ms <= 0)
			return;
		PrintWriter out = null;
		try {
			Enumeration<TraceContext> en = TraceContextManager.getContextEnumeration();
			while (en.hasMoreElements()) {
				TraceContext ctx = en.nextElement();
				long etime = System.currentTimeMillis() - ctx.startTime;
				if (etime > conf.autodump_stuck_thread_ms) {
					try {
						if (out == null) {
							out = open();
						}
						out.print(ctx.thread.getId() + ":");
						out.print(ctx.thread.getName() + ":");
						out.print(ctx.thread.getState().name() + ":");
						out.print("cpu " + SysJMX.getThreadCpuTime(ctx.thread) + ":");
						out.print(Hexa32.toString32(ctx.txid) + ":");
						out.print(ctx.serviceName + ":");
						out.print(etime + " ms");
						if (ctx.sqltext != null) {
							out.print(":sql=" + ctx.sqltext );
							if(ctx.sqlActiveArgs!=null){
								out.print("[" + ctx.sqlActiveArgs + "]");
							}
							out.print(":");
						}
						if (ctx.apicall_name != null) {
							out.println(":apicall=" + ctx.apicall_name);
						}
						out.println("");
						DumpUtil.printStack(out, ctx.thread.getId());
						out.println("");
					} catch (Exception e) {
						Logger.println("A155", e.toString());
						FileUtil.close(out);
						return;
					}
				}
			}
		} finally {
			FileUtil.close(out);
		}
	}
	public PrintWriter open() throws IOException {
		File file = new File(Configure.getInstance().mgr_dump_dir, "longtx_" +Configure.getInstance().obj_name + "_"+DateUtil.timestampFileName()+".txt");
		return new PrintWriter(new FileWriter(file));
	}
}
