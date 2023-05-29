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

package scouter.agent.counter.task;

import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.counter.CounterBasket;
import scouter.agent.counter.anotation.Counter;
import scouter.agent.trace.AlertProxy;
import scouter.agent.trace.TraceContext;
import scouter.agent.trace.TraceContextManager;
import scouter.agent.util.DumpUtil;
import scouter.lang.AlertLevel;
import scouter.util.DateUtil;
import scouter.util.FileUtil;
import scouter.util.Hexa32;
import scouter.util.SysJMX;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class DebugService {
	
	Configure conf = Configure.getInstance();
	long lastCheckStuckTime;
	
	@Counter
	public void checkService(CounterBasket pw) {
		PrintWriter stuckOut = null;
		try {
			boolean checkStuck = false;
			long now = System.currentTimeMillis();
			if (now - lastCheckStuckTime >= conf.autodump_stuck_check_interval_ms) {
				checkStuck = true;
				lastCheckStuckTime = now;
			}
			StringBuilder stuckMsg = new StringBuilder();
			//TODO reactive support
			for (Map.Entry<Long, TraceContext> e : TraceContextManager.getContextEntries()) {
				TraceContext ctx = e.getValue();
				if (checkStuck) {
					checkStcukService(ctx, stuckOut, stuckMsg);
				}
			}

			if (stuckMsg.length() > 0) {
				AlertProxy.sendAlert(AlertLevel.WARN, "STUCK_SERVICE", stuckMsg.toString());
			}
		} catch (Exception e) { 
			Logger.println("A154", e.toString());
		} finally {
			FileUtil.close(stuckOut);
		}
	}
	
	private void checkStcukService(TraceContext ctx, PrintWriter out, StringBuilder msg) {
		//TODO reactive support
		if (conf.autodump_stuck_thread_ms <= 0) return;
		long etime = System.currentTimeMillis() - ctx.startTime;
		if (etime > conf.autodump_stuck_thread_ms) {
			try {
				if (out == null) {
					out = open();
				}
				out.print(ctx.thread.getId() + ":");
				out.print(ctx.thread.getName() + ":");
				out.println(ctx.thread.getState().name());
				out.println("cpu " + SysJMX.getThreadCpuTime(ctx.thread));
				out.println(Hexa32.toString32(ctx.txid));
				out.print(ctx.serviceName + ":");
				out.println(etime + " ms");
				if (ctx.sqltext != null) {
					out.println("sql=" + ctx.sqltext );
					if(ctx.sqlActiveArgs!=null){
						out.println("[" + ctx.sqlActiveArgs + "]");
					}
				}
				if (ctx.apicall_name != null) {
					out.println("apicall=" + ctx.apicall_name);
				}
				out.println("");
				DumpUtil.printStack(out, ctx.thread.getId());
				out.println("");
				out.flush();
			} catch (Exception e) {
				Logger.println("A155", e.toString());
				FileUtil.close(out);
				return;
			}
			msg.append(ctx.serviceName + System.getProperty("line.separator"));
			msg.append(etime + " ms" + System.getProperty("line.separator"));
			msg.append(ctx.thread.getName() + ":");
			msg.append(ctx.thread.getState().name() + ":");
			msg.append("cpu " + SysJMX.getThreadCpuTime(ctx.thread) + System.getProperty("line.separator"));
			msg.append(Hexa32.toString32(ctx.txid) + System.getProperty("line.separator"));
			msg.append(System.getProperty("line.separator"));
		}
	}
	
	private PrintWriter open() throws IOException {
		File file = new File(conf.dump_dir, "longtx_" + conf.obj_name + "_" + DateUtil.timestampFileName()+".txt");
		return new PrintWriter(new FileWriter(file));
	}
}
