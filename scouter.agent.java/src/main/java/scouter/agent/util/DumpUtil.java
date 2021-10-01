/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 */

package scouter.agent.util;

import scouter.agent.Configure;
import scouter.agent.proxy.ToolsMainFactory;
import scouter.agent.trace.TraceContext;
import scouter.agent.trace.TraceContextManager;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.ListValue;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.FileUtil;
import scouter.util.Hexa32;
import scouter.util.SysJMX;
import scouter.util.ThreadUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Map;

public class DumpUtil extends Thread {

	private static DumpUtil instance = null;

	public final static synchronized DumpUtil getInstance() {
		if (instance == null) {
			instance = new DumpUtil();
			instance.setDaemon(true);
			instance.setName(ThreadUtil.getName(instance));
			instance.start();
		}
		return instance;
	}

	protected DumpUtil() {
	}

	public static File getDumpFile(String prefix) {
		String name = prefix + "." + DateUtil.ymdhms(System.currentTimeMillis()) + ".dump";
		return new File(Configure.getInstance().dump_dir, name);
	}

	public static Pack triggerHeapHisto() {
		PrintWriter out = null;
		MapPack pack = new MapPack();
		try {
			File file = DumpUtil.getDumpFile("scouter.heaphisto");
			out = new PrintWriter(new FileWriter(file));
			ToolsMainFactory.heaphisto(out);
			pack.put("name", file.getName());
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			FileUtil.close(out);
		}
		return pack;
	}

	public static Pack triggerThreadDump() {
		PrintWriter out = null;
		MapPack pack = new MapPack();
		try {
			File file = DumpUtil.getDumpFile("scouter.threaddump");
			out = new PrintWriter(new FileWriter(file));
			ToolsMainFactory.threadDump(out);
			pack.put("name", file.getName());
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			FileUtil.close(out);
		}
		return pack;
	}

	public static Pack triggerThreadList() {
		PrintWriter out = null;
		MapPack pack = new MapPack();
		try {
			File file = DumpUtil.getDumpFile("scouter.threads");
			out = new PrintWriter(new FileWriter(file));
			MapPack mpack = ThreadUtil.getThreadList();
			ListValue ids = mpack.getList("id");
			ListValue name = mpack.getList("name");
			ListValue stat = mpack.getList("stat");
			ListValue cpu = mpack.getList("cpu");

			for (int i = 0; i < ids.size(); i++) {
				long tid = CastUtil.clong(ids.get(i));
				out.print(i + ":");
				out.print(tid + ":");
				out.print(name.get(i) + ":");
				out.print(stat.get(i) + ":");
				out.print("cpu " + cpu.get(i));

				TraceContext ctx = TraceContextManager.getContextByThreadId(tid);
				if (ctx != null) {
					out.print(":service " + Hexa32.toString32(ctx.txid) + ":");
					out.print(ctx.serviceName + ":");
					long etime = System.currentTimeMillis() - ctx.startTime;
					out.print(etime + " ms");
				}
				out.println("");
				printStack(out, tid);
				out.println("");
				pack.put("name", file.getName());
			}

		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			FileUtil.close(out);
		}
		return pack;
	}

	static Configure conf = Configure.getInstance();

	public static Pack triggerActiveService() {
		PrintWriter out = null;
		MapPack pack = new MapPack();
		try {
			File file = DumpUtil.getDumpFile("scouter.activeservice");
			out = new PrintWriter(new FileWriter(file));
			//TODO reactive support
			int n = 0;
			for (Map.Entry<Long, TraceContext> e : TraceContextManager.getContextEntries()) {
				TraceContext ctx = e.getValue();
				out.print(n + ":");
				out.print(ctx.thread.getId() + ":");
				out.print(ctx.thread.getName() + ":");
				out.print(ctx.thread.getState().name() + ":");
				out.print("cpu " + SysJMX.getThreadCpuTime(ctx.thread) + ":");
				out.print(Hexa32.toString32(ctx.txid) + ":");
				out.print(ctx.serviceName + ":");
				long etime = System.currentTimeMillis() - ctx.startTime;
				out.print(etime + " ms");
				if (ctx.sqltext != null) {
					out.print(":sql=" + ctx.sqltext + ":");
				}
				if (ctx.apicall_name != null) {
					out.println(":subcall=" + ctx.apicall_name);
				}
				out.println("");
				printStack(out, ctx.thread.getId());
				out.println("");
				pack.put("name", file.getName());

				n++;
			}
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			FileUtil.close(out);
		}
		return pack;
	}

	public static void printStack(PrintWriter out, long tid) {
		ThreadMXBean tmb = ManagementFactory.getThreadMXBean();
		ThreadInfo f = tmb.getThreadInfo(tid, 500);
		StackTraceElement[] se = f.getStackTrace();
		if (se != null) {
			for (int i = 0; i < se.length; i++) {
				if (se[i] != null) {
					out.println("\t" + se[i]);
				}
			}
		}
	}

	private void trigger() {
		synchronized (this) {
			this.notifyAll();
		}
	}

	public void run() {
		while (true) {

			synchronized (this) {
				ThreadUtil.wait(this);
			}

			switch (conf.autodump_level) {
			case 1:
				DumpUtil.triggerThreadDump();
				break;
			case 2:
				DumpUtil.triggerActiveService();
				break;
			case 3:
				DumpUtil.triggerThreadList();
				break;
			default:
				DumpUtil.triggerThreadDump();
				break;
			}

		}
	}

	private static long last_auto_dump = 0;
    private static boolean stopAutoDumpTemporarily = false;

	public static void autoDump() {
		if (conf.autodump_enabled == false || stopAutoDumpTemporarily == true)
			return;

		long now = System.currentTimeMillis();
		if (now < last_auto_dump + conf.autodump_interval_ms)
			return;
		last_auto_dump = now;

		DumpUtil.getInstance().trigger();
	}

	public static void autoDumpByCpuExceedance() {
		if (conf.autodump_enabled && conf.autodump_interval_ms <= conf.autodump_cpu_exceeded_dump_interval_ms) {
			return;
		}

		if(conf.autodump_cpu_exceeded_enabled == false) {
			return;
		}

        stopAutoDumpTemporarily = true;

        try{
            for(int i=0; i<conf.autodump_cpu_exceeded_dump_cnt; i++) {
                long now = System.currentTimeMillis();
                if(now < last_auto_dump + conf.autodump_cpu_exceeded_dump_interval_ms) {
                    continue;
                }
                last_auto_dump = now;
                DumpUtil.getInstance().trigger();
                Thread.sleep(conf.autodump_cpu_exceeded_dump_interval_ms);
            }
        } catch (Throwable t) {
        } finally {
            stopAutoDumpTemporarily = false;
        }

	}

}
