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

package scouter.agent.trace;

import scouter.agent.Configure;
import scouter.agent.util.SimpleLru;
import scouter.lang.AlertLevel;
import scouter.util.KeyGen;
import scouter.util.LongLongLinkedMap;

import java.util.Map;
import java.util.Set;

import static scouter.agent.trace.TraceContext.GetBy.CoroutineLocal;
import static scouter.agent.trace.TraceContext.GetBy.ThreadLocal;
import static scouter.agent.trace.TraceContext.GetBy.ThreadLocalTxid;
import static scouter.agent.trace.TraceContext.GetBy.ThreadLocalTxidByCoroutine;

public class TraceContextManager {
	private static Configure conf = Configure.getInstance();

//	private static LongKeyMap<TraceContext> entryByThreadId = new LongKeyMap<TraceContext>();
//	private static LongKeyLinkedMap<TraceContext> entryByTxid = new LongKeyLinkedMap<TraceContext>().setMax(10000);

	private static SimpleLru<Long, TraceContext> entryByThreadId = new SimpleLru<Long, TraceContext>(10000);
	private static SimpleLru<Long, TraceContext> entryByTxid = new SimpleLru<Long, TraceContext>(10000);
	private static SimpleLru<Long, TraceContext> deferredEntry = new SimpleLru<Long, TraceContext>(10000);

	private static SimpleLru<String, Long> customTxidMap = new SimpleLru<String, Long>(10000);

	private static final ThreadLocal<TraceContext> local = new ThreadLocal<TraceContext>();
	private static final ThreadLocal<Long> txidLocal = new ThreadLocal<Long>();
	public static final ThreadLocal<Long> txidByCoroutine = new ThreadLocal<Long>();

	private static CoroutineDebuggingLocal<TraceContext> coroutineDebuggingLocal = new CoroutineDebuggingLocal<TraceContext>();

	//pass = 1, discard = 2, end-processing-with-path = -1, end-processing-with-path = -2
	private static ThreadLocal<Integer> forceDiscard = new ThreadLocal<Integer>();
	private static boolean coroutineEnabled;
	private static boolean coroutineDebuggingEnabled;

	public static int size() {
		return entryByTxid.size();
	}

	public static int[] getActiveCount() {
		int[] act = new int[3];
		try {
			long now = System.currentTimeMillis();
			for (Map.Entry<Long, TraceContext> e : entryByTxid.entrySet()) {
				TraceContext ctx = e.getValue();
				long tm = now - ctx.startTime;
				if (tm < conf.trace_activeserivce_yellow_time) {
					act[0]++;
				} else if (tm < conf.trace_activeservice_red_time) {
					act[1]++;
				} else {
					act[2]++;
				}

				if (conf.profile_force_end_stuck_service && tm > conf.profile_force_end_stuck_millis) {
					if (conf.profile_force_end_stuck_ignore_metering) {
						ctx.skipMetering = true;
					}
					Throwable th = new RuntimeException("Stuck service. finish xlog but actually may be processing.");
					if (ctx.http != null) {
						TraceMain.Stat stat = new TraceMain.Stat(ctx);
						TraceMain.endHttpService(stat, th);
					} else {
						LocalContext lctx = new LocalContext(ctx, null);
						TraceMain.endService(lctx, null, th);
					}
					String msg = String.format("service: %s, elapsed: %d, start thread: %s, txid: %s", ctx.serviceName, tm,
							ctx.threadName, ctx.txid);
					if (conf.profile_force_end_stuck_alert) {
						AlertProxy.sendAlert(AlertLevel.ERROR, "STUCK", msg);
					} else {
						//AlertProxy.sendAlert(AlertLevel.WARN, "STUCK", msg);
					}
				}

			}
		} catch (Throwable t) {
		}
		return act;
	}

	public static Set<Map.Entry<Long, TraceContext>> getContextEntries() {
		return entryByTxid.entrySet();
	}

	public static Set<Map.Entry<Long, TraceContext>> getThreadingContextEntries() {
		return entryByThreadId.entrySet();
	}

	public static Set<Map.Entry<Long, TraceContext>> getDeferredContextEntries() {
		return deferredEntry.entrySet();
	}

	public static TraceContext getContext() {
		return getContext(false);
	}

	public static TraceContext getContext(boolean force) {
		if (!force && conf.profile_off) {
			return null;
		}
		Long txid = txidLocal.get();
		TraceContext traceContext = txid == null ? null : entryByTxid.get(txid);

		if (traceContext != null) {
			traceContext.getBy = ThreadLocalTxid;
			return traceContext;
		}

		if (coroutineEnabled) {
			txid = txidByCoroutine.get();
			traceContext = txid == null ? null : entryByTxid.get(txid);

			if (traceContext != null) {
				traceContext.getBy = ThreadLocalTxidByCoroutine;
				return traceContext;
			}
		}

		traceContext = local.get();
		if (traceContext != null) {
			traceContext.getBy = ThreadLocal;
			return traceContext;
		}

		if (coroutineDebuggingEnabled) {
			traceContext = getCoroutineContext();
			if (traceContext != null) {
				traceContext.getBy = CoroutineLocal;
				return traceContext;
			}
		}

		return null;
	}

	public static TraceContext getContextByTxid(long txid) {
		return entryByTxid.get(txid);
	}

	public static TraceContext getContextByThreadId(long key) {
		return entryByThreadId.get(key);
	}

	public static TraceContext getDeferredContext(long key) {
		return deferredEntry.get(key);
	}

	public static TraceContext getCoroutineContext() {
		return  coroutineDebuggingLocal.get();
	}

	public static TraceContext getCoroutineContext(long id) {
		return  coroutineDebuggingLocal.get(id);
	}

	public static TraceContext getContextByCustomTxid(String customTxid) {
		Long txid = customTxidMap.get(customTxid);
		if (txid == null) {
			return null;
		}
		return getContextByTxid(txid);
	}

	public static void linkCustomTxid(String customTxid, long txid) {
		TraceContext ctx = getContextByTxid(txid);
		if (ctx == null) {
			return;
		}
		ctx.ctxid = customTxid;
		customTxidMap.put(customTxid, txid);
	}

	public static Long getLocalTxid() {
		return txidLocal.get();
	}

	public static void clearForceDiscard() {
		if(!conf._xlog_hard_sampling_enabled) {
			return;
		}
		Integer num = forceDiscard.get();
		if(num == null) {
			forceDiscard.set(-1);
		} else {
			if(num == 1) {
				forceDiscard.set(-1);
			} else if(num == 2) {
				forceDiscard.set(-2);
			}
		}
	}

	public static boolean isForceDiscarded() {
		if(!conf._xlog_hard_sampling_enabled) {
			return false;
		}

		boolean discard = false;
		Integer num = forceDiscard.get();
		if(num == null) {
			return false;
		}
		if(num == 2 || num == -2) {
			discard = true;
		}
		return discard;
	}

	public static boolean startForceDiscard() {
		if(!conf._xlog_hard_sampling_enabled) {
			return false;
		}

		boolean discard = false;
		Integer num = forceDiscard.get();
		if(num == null || num == -1 || num == -2) {
			if(Math.abs(KeyGen.next()%100) >= conf._xlog_hard_sampling_rate_pct) {
				discard = true;
				forceDiscard.set(2); //discard
			} else {
				forceDiscard.set(1); //pass
			}
		} else {
			if(num == 2) { //discard
				discard = true;
			}
		}
		return discard;
	}

	public static void start(TraceContext o) {
		local.set(o);
		txidLocal.set(o.txid);
		entryByTxid.put(o.txid, o);

		if (!o.isReactiveStarted) {
			entryByThreadId.put(o.threadId, o);
		}
	}

	public static void takeoverTxid(TraceContext o, long oldTxid) {
		txidLocal.set(o.txid);
		entryByTxid.remove(oldTxid);
		entryByTxid.put(o.txid, o);
	}

	public static void startByCoroutine(TraceContext o) {
		coroutineEnabled = true;
		txidByCoroutine.set(o.txid);
	}

	public static void end(TraceContext o) {
		clearAllContext(o);
	}

	private static LongLongLinkedMap threadTxidMap = new LongLongLinkedMap().setMax(2000);
	private static LongLongLinkedMap txidThreadMap = new LongLongLinkedMap().setMax(2000);

	public static void setTxidLocal(Long txid) {
		txidLocal.set(txid);
		if (txid != null && conf._psts_enabled && conf._psts_progressive_reactor_thread_trace_enabled) {
			long threadId = Thread.currentThread().getId();
			txidThreadMap.put(txid, threadId);
			threadTxidMap.put(threadId, txid);
		}
	}

	public static long getReactiveThreadId(long txid) {
		if (!conf._psts_progressive_reactor_thread_trace_enabled) {
			return 0;
		}
		long threadId = txidThreadMap.get(txid);
		if (threadId == 0) {
			return 0;
		}
		long txid0 = threadTxidMap.get(threadId);
		if (txid0 == txid) {
			return threadId;
		}
		return 0;
	}

	public static void asCoroutineDebuggingMode(Long coroutineId, TraceContext o) {
		CoroutineDebuggingLocal.setCoroutineDebuggingId(coroutineId);
		coroutineDebuggingLocal.put(o);
		local.set(null);
		coroutineDebuggingEnabled = true;
	}

	public static void toDeferred(TraceContext o) {
		deferredEntry.put(o.txid, o);
	}

	public static void completeDeferred(TraceContext o) {
		deferredEntry.remove(o.txid);
	}

	public static void clearAllContext(TraceContext o) {
		o._req = null;
		o._res = null;
		local.set(null);
		coroutineDebuggingLocal.clear(); //it should be prev of txidLocal clear

		if (o.ctxid != null) {
			customTxidMap.remove(o.ctxid);
		}

		entryByTxid.remove(o.txid);
		if (conf._psts_progressive_reactor_thread_trace_enabled) {
			txidThreadMap.remove(o.txid);
		}

		txidByCoroutine.set(null);
		if (!o.isReactiveStarted) { //do not clear txidLocal in reactive
			txidLocal.set(null);
			entryByThreadId.remove(o.threadId);
		}

		clearForceDiscard();
	}
}
