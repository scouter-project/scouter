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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.counter.CounterBasket;
import scouter.agent.counter.anotation.Counter;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.trace.AlertProxy;
import scouter.agent.trace.TraceContext;
import scouter.agent.trace.TraceContextManager;
import scouter.agent.util.DumpUtil;
import scouter.lang.AlertLevel;
import scouter.lang.TextTypes;
import scouter.lang.pack.AlertPack;
import scouter.lang.value.MapValue;
import scouter.util.DateUtil;
import scouter.util.FileUtil;
import scouter.util.HashUtil;
import scouter.util.Hexa32;
import scouter.util.SysJMX;
import scouter.util.ThreadUtil;

public class DebugService {
	
	static final String PREFIX_SERVICE = "service.";
	static final String PREFIX_TIME = "time.";
	static final String PREFIX_INTERVAL = "interval.";
	
	Configure conf = Configure.getInstance();
	HashMap<Integer, DelayedCondition> conditionMap = new HashMap<Integer, DelayedCondition>();
	long lastModifiedTime;
	long lastCheckStuckTime;
	
	@Counter
	public void checkService(CounterBasket pw) {
		loadDelayedConfigureFile();
		PrintWriter stuckOut = null;
		try {
			boolean checkStuck = false;
			long now = System.currentTimeMillis();
			if (now - lastCheckStuckTime >= conf.autodump_stuck_check_interval_ms) {
				checkStuck = true;
				lastCheckStuckTime = now;
			}
			StringBuilder stuckMsg = new StringBuilder();
			HashSet<Integer> sentService = new HashSet<Integer>();
			Enumeration<TraceContext> en = TraceContextManager.getContextEnumeration();
			while (en.hasMoreElements()) {
				TraceContext ctx = en.nextElement();
				if (checkStuck) {
					checkStcukService(ctx, stuckOut, stuckMsg);
				}
				alertDelayedService(ctx, sentService);
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
	
	private void alertDelayedService(TraceContext ctx, HashSet<Integer> sentService) {
		if (conditionMap.size() < 1) return;
		if (sentService.contains(ctx.serviceHash)) return;
		for (DelayedCondition condition : conditionMap.values()) {
			if (condition.time <= 0) continue;
			if (condition.service == 0) continue;
			long now = System.currentTimeMillis();
			long etime = now - ctx.startTime;
			if (condition.service == ctx.serviceHash && etime >= condition.time * 1000) {
				if ((now - condition.lastSentTime) < condition.interval * 1000) continue;
				condition.lastSentTime = now;
				StringBuilder msg = new StringBuilder();
				msg.append(ctx.serviceName + System.getProperty("line.separator"));
				msg.append(etime + " ms" + System.getProperty("line.separator"));
				msg.append(ctx.thread.getName() + ":");
				msg.append(ctx.thread.getState().name());
				MapValue tags = new MapValue();
				int stackId = DataProxy.sendError(ThreadUtil.getThreadStack(ctx.threadId));
				tags.put(AlertPack.HASH_FLAG + TextTypes.ERROR + "_stack", stackId);
				AlertProxy.sendAlert(AlertLevel.WARN, "DELAYED_SERVICE", msg.toString(), tags);
				sentService.add(ctx.serviceHash);
			}
		}
	}
	
	private void loadDelayedConfigureFile() {
		File dir = conf.getPropertyFile().getParentFile();
		String filename = conf.trace_delayed_service_mgr_filename;
		File file = new File(dir, filename);
		if (file.lastModified() == lastModifiedTime) {
			return;
		}
		Logger.println("SA-2001", "Load delayed service configure file : " + file.getAbsolutePath());
		lastModifiedTime = file.lastModified();
		Properties properties = new Properties();
		if (file.canRead()) {
			FileInputStream fis = null; 
			try {
				fis = new FileInputStream(file);
				properties.load(fis);
			} catch (Exception e) {
				Logger.println("SA-2002", "Load error delayed service property file", e);
			} finally {
				FileUtil.close(fis);
			}
		}
		HashMap<Integer, DelayedCondition> indexMap = new HashMap<Integer, DelayedCondition>();
		for (Object key : properties.keySet()) {
			try {
				String name = key.toString();
				if (name.startsWith(PREFIX_SERVICE)) {
					int index = Integer.valueOf(name.substring(PREFIX_SERVICE.length()));
					DelayedCondition condition = indexMap.get(index);
					if (condition == null) {
						condition = new DelayedCondition();
						indexMap.put(index, condition);
					}
					condition.service = HashUtil.hash(properties.getProperty(name).trim());
				} else if (name.startsWith(PREFIX_TIME)) {
					int index = Integer.valueOf(name.substring(PREFIX_TIME.length()));
					DelayedCondition condition = indexMap.get(index);
					if (condition == null) {
						condition = new DelayedCondition();
						indexMap.put(index, condition);
					}
					condition.time = Integer.valueOf(properties.getProperty(name));
				} else if (name.startsWith(PREFIX_INTERVAL)) {
					int index = Integer.valueOf(name.substring(PREFIX_INTERVAL.length()));
					DelayedCondition condition = indexMap.get(index);
					if (condition == null) {
						condition = new DelayedCondition();
						indexMap.put(index, condition);
					}
					condition.interval = Integer.valueOf(properties.getProperty(name));
				}
			} catch (Exception e) { }
		}
		conditionMap.clear();
		for (DelayedCondition condition : indexMap.values()) {
			if (condition.service != 0) {
				conditionMap.put(condition.service, condition);
			}
		}
	}
	
	private PrintWriter open() throws IOException {
		File file = new File(conf.dump_dir, "longtx_" + conf.obj_name + "_" + DateUtil.timestampFileName()+".txt");
		return new PrintWriter(new FileWriter(file));
	}
	
	private static class DelayedCondition {
		public int service;
		public int time;
		public int interval = 30;
		public long lastSentTime;
	}
}
