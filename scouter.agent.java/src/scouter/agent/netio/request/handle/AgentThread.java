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
import scouter.agent.AgentCommonConstant;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.counter.task.MakeStack;
import scouter.agent.netio.request.anotation.RequestHandler;
import scouter.agent.netio.request.worker.DumpOnCpuExceedanceWorker;
import scouter.agent.proxy.ToolsMainFactory;
import scouter.agent.trace.TraceContext;
import scouter.agent.trace.TraceContextManager;
import scouter.agent.util.DumpUtil;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.*;
import scouter.util.CastUtil;
import scouter.util.Hexa32;
import scouter.util.SysJMX;
import scouter.util.ThreadUtil;

import java.io.IOException;
import java.util.Enumeration;

import static scouter.net.RequestCmd.*;

public class AgentThread {
	@RequestHandler(OBJECT_THREAD_DETAIL)
	public Pack threadDetail(Pack param) {
		MapPack paramPack = (MapPack) param;
		long thread = paramPack.getLong("id");
		long txid = paramPack.getLong("txid");

		MapPack p;
		TraceContext ctx;

		if(thread != 0L) {
			p = ThreadUtil.getThreadDetail(thread);
			ctx = TraceContextManager.getContext(thread);
		} else {
			p = new MapPack();
			ctx = TraceContextManager.getDeferredContext(txid);
		}

		if (ctx != null) {
			p.put("Thread Id", new DecimalValue(0L));
			p.put("Thread Name", new TextValue("[No Thread] wait on deferred queue"));
			p.put("State", new TextValue("n/a"));

			p.put("Service Txid", new TextValue(Hexa32.toString32(ctx.txid)));
			p.put("Service Name", new TextValue(AgentCommonConstant.removeSpringRequestMappingPostfixFlag(ctx.serviceName)));
			long etime = System.currentTimeMillis() - ctx.startTime;
			p.put("Service Elapsed", new DecimalValue(etime));
		}
		return p;
	}
	@RequestHandler(OBJECT_THREAD_CONTROL)
	public Pack threadKill(Pack param) {
		long thread = ((MapPack) param).getLong("id");
		String action = ((MapPack) param).getText("action");
		// 쓰레드 상세 화면에서 쓰레드를 제어한다.
		TraceContext ctx = TraceContextManager.getContext(thread);
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
			p.put("Service Name", new TextValue(AgentCommonConstant.removeSpringRequestMappingPostfixFlag(ctx.serviceName)));
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
			TraceContext ctx = TraceContextManager.getContext(tid);
			if (ctx != null) {
				txid.add(new TextValue(Hexa32.toString32(ctx.txid)));
				service.add(new TextValue(AgentCommonConstant.removeSpringRequestMappingPostfixFlag(ctx.serviceName)));
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
		Enumeration<TraceContext> en = TraceContextManager.getContextEnumeration();
		while (en.hasMoreElements()) {
			TraceContext ctx = en.nextElement();
			if (ctx == null) {
				continue;
			}
			id.add(ctx.thread.getId());
			name.add(ctx.thread.getName());
			stat.add(ctx.thread.getState().name());
			txid.add(new TextValue(Hexa32.toString32(ctx.txid)));
			service.add(new TextValue(AgentCommonConstant.removeSpringRequestMappingPostfixFlag(ctx.serviceName)));
			ip.add(ctx.remoteIp);
			long etime = System.currentTimeMillis() - ctx.startTime;
			elapsed.add(new DecimalValue(etime));
			sql.add(ctx.sqltext);
			subcall.add(ctx.apicall_name);
			try {
				cpu.add(SysJMX.getThreadCpuTime(ctx.thread));
			} catch (Throwable th) {
				Logger.println("A128", th);
				cpu.add(0L);
			}
			login.add(ctx.login);
			desc.add(ctx.desc);
		}

		Enumeration<TraceContext> enDeferred = TraceContextManager.getDeferredContextEnumeration();
		while (enDeferred.hasMoreElements()) {
			TraceContext ctx = enDeferred.nextElement();
			if (ctx == null) {
				continue;
			}
			id.add(0L);
			name.add("[No Thread] wait on deferred queue");
			stat.add("n/a");
			txid.add(new TextValue(Hexa32.toString32(ctx.txid)));
			service.add(new TextValue(AgentCommonConstant.removeSpringRequestMappingPostfixFlag(ctx.serviceName)));
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
