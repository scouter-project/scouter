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
package scouter.agent.netio.request.handle;

import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.counter.task.MakeStack;
import scouter.agent.netio.request.anotation.RequestHandler;
import scouter.agent.netio.request.worker.DumpOnCpuExceedanceWorker;
import scouter.agent.proxy.ToolsMainFactory;
import scouter.agent.trace.TraceContext;
import scouter.agent.trace.TraceContextManager;
import scouter.agent.trace.TraceMain;
import scouter.agent.util.DumpUtil;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.BooleanValue;
import scouter.lang.value.DecimalValue;
import scouter.lang.value.ListValue;
import scouter.lang.value.NullValue;
import scouter.lang.value.TextValue;
import scouter.util.*;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Enumeration;
import java.util.Map;

import static scouter.net.RequestCmd.*;

public class AgentThread {
	@RequestHandler(OBJECT_THREAD_DETAIL)
	public Pack threadDetail(Pack param) {
		MapPack paramPack = (MapPack) param;
		long threadId = paramPack.getLong("id");
		long txid = paramPack.getLong("txid");

		MapPack p = new MapPack();
		TraceContext ctx = TraceContextManager.getContextByTxid(txid);
		if (ctx == null) {
			p.put("Thread Name", new TextValue("[No Thread] End"));
			p.put("State", new TextValue("end"));
			return p;
		}

		if (ctx.isReactiveStarted) {
			threadId = TraceContextManager.getReactiveThreadId(txid);
		}

		p.put("Service Txid", new TextValue(Hexa32.toString32(ctx.txid)));
		p.put("Service Name", new TextValue(ctx.serviceName));
		long etime = System.currentTimeMillis() - ctx.startTime;
		p.put("Service Elapsed", new DecimalValue(etime));
		String sql = ctx.sqltext;
		if (sql != null) {
			p.put("SQL", sql);
			if(ctx.currentSqlStep != null){
				p.put("SQLActiveBindVar",ctx.currentSqlStep.param);
			}
		}

		String subcall = ctx.apicall_name;
		if (subcall != null) {
			p.put("Subcall", subcall);
		}

		if(threadId != 0L) {
			ThreadMXBean tmb = ManagementFactory.getThreadMXBean();
			ThreadInfo f = tmb.getThreadInfo(threadId, 500);
			if (f == null) {
				Thread t = ctx.thread;
				if (t != null) {
					p.put("Thread Id", new DecimalValue(t.getId()));
					p.put("State", new TextValue(t.getState().name()));
					String name = t.getName();
					if (StringUtil.isEmpty(name)) {
						name = t.toString();
					}
					p.put("Thread Name", new TextValue(name));
					p.put("Stack Trace", new TextValue(ThreadUtil.getStackTrace(t.getStackTrace())));
				}
			} else {
				p.put("Thread Id", new DecimalValue(f.getThreadId()));
				p.put("Thread Cpu Time", new DecimalValue(tmb.getThreadCpuTime(threadId) / 1000000));
				p.put("Thread User Time", new DecimalValue(tmb.getThreadUserTime(threadId) / 1000000));
				p.put("Blocked Count", new DecimalValue(f.getBlockedCount()));
				p.put("Blocked Time", new DecimalValue(f.getBlockedTime()));
				p.put("Waited Count", new DecimalValue(f.getWaitedCount()));
				p.put("Waited Time", new DecimalValue(f.getWaitedTime()));
				p.put("Lock Owner Id", new DecimalValue(f.getLockOwnerId()));
				p.put("Lock Name", new TextValue(f.getLockName()));
				p.put("Lock Owner Name", new TextValue(f.getLockOwnerName()));
				p.put("Thread Name", new TextValue(f.getThreadName()));
				p.put("Stack Trace", new TextValue(ThreadUtil.getStackTrace(f.getStackTrace())));
				p.put("State", new TextValue(f.getThreadState().toString()));
			}

		} else {
			TraceContext deferredContext = TraceContextManager.getDeferredContext(txid);
			if (deferredContext != null) {
				p.put("Thread Name", new TextValue("[No Thread] wait on deferred queue"));
			} else {
				p.put("Thread Name", new TextValue("No dedicated thread"));
			}
			p.put("Thread Id", new DecimalValue(0L));
			p.put("State", new TextValue("n/a"));
		}

		if (ctx.isReactiveStarted) {
			String stack = p.getText("Stack Trace");
			if (stack == null) {
				stack = "";
			}
			stack = stack + "\n" + getUnfinishedReactiveStepsAsDumpString(ctx);
			p.put("Stack Trace", new TextValue(stack));
		}

		return p;
	}

	private String getUnfinishedReactiveStepsAsDumpString(TraceContext ctx) {
		if (ctx == null) {
			return null;
		}

		long now = System.currentTimeMillis();
		StringBuilder builder = new StringBuilder(200)
				.append("<<<<<<<<<< currently existing subscribes >>>>>>>>>>").append("\n");

		if (ctx.scannables != null) {
			Enumeration<TraceContext.TimedScannable> en = ctx.scannables.values();
			while (en.hasMoreElements()) {
				TraceContext.TimedScannable ts = en.nextElement();
				if (ts == null) {
					break;
				}
				String dumpScannable = TraceMain.reactiveSupport.dumpScannable(ctx, ts, now);
				builder.append(dumpScannable).append("\n");
			}
		}
		return builder.toString();
	}

	@RequestHandler(OBJECT_THREAD_CONTROL)
	public Pack threadKill(Pack param) {
		long thread = ((MapPack) param).getLong("id");
		String action = ((MapPack) param).getText("action");
		// 쓰레드 상세 화면에서 쓰레드를 제어한다.
		TraceContext ctx = TraceContextManager.getContextByThreadId(thread);
		try {
			if (ctx != null) {
				if ("interrupt".equalsIgnoreCase(action)) {
					ctx.thread.interrupt();
				} else if ("stop".equalsIgnoreCase(action)) {
					ctx.thread.stop();
				} else if ("resume".equalsIgnoreCase(action)) {
					ctx.thread.resume();
				} else if ("suspend".equalsIgnoreCase(action)) {
					ctx.thread.suspend();
				}
			}
		} catch (Throwable t) {
		}
		MapPack p = ThreadUtil.getThreadDetail(thread);
		if (ctx != null) {
			p.put("Service Txid", new TextValue(Hexa32.toString32(ctx.txid)));
			p.put("Service Name", new TextValue(ctx.serviceName));
			long etime = System.currentTimeMillis() - ctx.startTime;
			p.put("Service Elapsed", new DecimalValue(etime));
			String sql = ctx.sqltext;
			if (sql != null) {
				p.put("SQL", sql);
			}
			String subcall = ctx.apicall_name;
			if (subcall != null) {
				p.put("Subcall", subcall);
			}
		}
		return p;
	}
	@RequestHandler(OBJECT_THREAD_LIST)
	public Pack threadList(Pack param) {
		MapPack mpack = ThreadUtil.getThreadList();
		ListValue ids = mpack.getList("id");
		ListValue txid = mpack.newList("txid");
		ListValue elapsed = mpack.newList("elapsed");
		ListValue service = mpack.newList("service");
		for (int i = 0; i < ids.size(); i++) {
			long tid = CastUtil.clong(ids.get(i));
			TraceContext ctx = TraceContextManager.getContextByThreadId(tid);
			if (ctx != null) {
				txid.add(new TextValue(Hexa32.toString32(ctx.txid)));
				service.add(new TextValue(ctx.serviceName));
				long etime = System.currentTimeMillis() - ctx.startTime;
				elapsed.add(new DecimalValue(etime));
			} else {
				txid.add(new NullValue());
				elapsed.add(new NullValue());
				service.add(new NullValue());
			}
		}
		return mpack;
	}
	Configure conf = Configure.getInstance();
	@RequestHandler(OBJECT_ACTIVE_SERVICE_LIST)
	public Pack activeThreadList(Pack param) {
		MapPack rPack = new MapPack();
		ListValue id = rPack.newList("id");
		ListValue elapsed = rPack.newList("elapsed");
		ListValue service = rPack.newList("service");
		ListValue stat = rPack.newList("stat");
		ListValue name = rPack.newList("name");
		ListValue cpu = rPack.newList("cpu");
		ListValue txid = rPack.newList("txid");
		ListValue ip = rPack.newList("ip");
		ListValue sql = rPack.newList("sql");
		ListValue subcall = rPack.newList("subcall");
		ListValue login = rPack.newList("login");
		ListValue desc = rPack.newList("desc");

		for (Map.Entry<Long, TraceContext> e : TraceContextManager.getContextEntries()) {
			TraceContext ctx = e.getValue();
			if (ctx == null) {
				continue;
			}
			if (!ctx.isReactiveStarted) {
				id.add(ctx.thread.getId());
				name.add(ctx.thread.getName());
				stat.add(ctx.thread.getState().name());
			} else {
				if (Configure.getInstance()._psts_progressive_reactor_thread_trace_enabled) {
					id.add(TraceContextManager.getReactiveThreadId(ctx.txid));
				} else {
					id.add(0);
				}
				name.add("omit on reactive");
				stat.add("n/a");
			}
			try {
				cpu.add(SysJMX.getThreadCpuTime(ctx.thread));
			} catch (Throwable th) {
				Logger.println("A128", th);
				cpu.add(0L);
			}

			txid.add(new TextValue(Hexa32.toString32(ctx.txid)));
			service.add(new TextValue(ctx.serviceName));
			ip.add(ctx.remoteIp);
			long etime = System.currentTimeMillis() - ctx.startTime;
			elapsed.add(new DecimalValue(etime));
			sql.add(ctx.sqltext);
			subcall.add(ctx.apicall_name);
			login.add(ctx.login);
			desc.add(ctx.desc);
		}

		for (Map.Entry<Long, TraceContext> e : TraceContextManager.getDeferredContextEntries()) {
			TraceContext ctx = e.getValue();
			if (ctx == null) {
				continue;
			}
			id.add(0L);
			name.add("[No Thread] wait on deferred queue");
			stat.add("n/a");
			txid.add(new TextValue(Hexa32.toString32(ctx.txid)));
			service.add(new TextValue(ctx.serviceName));
			ip.add(ctx.remoteIp);
			long etime = System.currentTimeMillis() - ctx.startTime;
			elapsed.add(new DecimalValue(etime));
			sql.add(ctx.sqltext);
			subcall.add("");
			cpu.add(0L);
			login.add(ctx.login);
			desc.add(ctx.desc);
		}

		rPack.put("complete", new BooleanValue(true));
		return rPack;
	}
	@RequestHandler(OBJECT_THREAD_DUMP)
	public Pack threadDump(Pack param) {
		try {
			return ToolsMainFactory.threadDump(param);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}
	@RequestHandler(TRIGGER_ACTIVE_SERVICE_LIST)
	public Pack triggerActiveServiceList(Pack param) {
		return DumpUtil.triggerActiveService();
	}

	@RequestHandler(TRIGGER_THREAD_LIST)
	public Pack triggerThreadList(Pack param) {
		return DumpUtil.triggerThreadList();
	}

	@RequestHandler(TRIGGER_THREAD_DUMP)
	public Pack triggerThreadDump(Pack param) {
		return DumpUtil.triggerThreadDump();
	}

	@RequestHandler(TRIGGER_THREAD_DUMPS_FROM_CONDITIONS)
	public Pack triggerThreadDumpsFromConditions(Pack param) {
		MapPack mpack = (MapPack) param;
		DumpOnCpuExceedanceWorker.getInstance().add(mpack.getText(TRIGGER_DUMP_REASON));
		return null;
	}

	@RequestHandler(PSTACK_ON)
	public Pack turnOn(Pack param) {
		MapPack p = (MapPack) param;
		long time = p.getLong("time");
		if (time <= 0) {
			MakeStack.pstack_requested = 0;
		} else {
			MakeStack.pstack_requested = System.currentTimeMillis() + time;
		}
		return param;
	}
	public static void main(String[] args) throws IOException {
	}
}
