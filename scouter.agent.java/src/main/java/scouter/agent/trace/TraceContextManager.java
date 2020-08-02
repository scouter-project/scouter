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
import scouter.agent.Logger;
import scouter.util.KeyGen;
import scouter.util.LongKeyLinkedMap;
import scouter.util.LongKeyMap;

import java.util.Enumeration;

import static scouter.agent.trace.TraceContext.GetBy.*;

public class TraceContextManager {
	private static Configure conf = Configure.getInstance();

	private static LongKeyMap<TraceContext> entryByThreadId = new LongKeyMap<TraceContext>();
	private static LongKeyLinkedMap<TraceContext> entryByTxid = new LongKeyLinkedMap<TraceContext>();

	private static ThreadLocal<TraceContext> local = new ThreadLocal<TraceContext>();
	private static ThreadLocal<Long> txidLocal = new ThreadLocal<Long>();
	public static ThreadLocal<Long> txidByCoroutine = new ThreadLocal<Long>();

	private static CoroutineDebuggingLocal<TraceContext> coroutineDebuggingLocal = new CoroutineDebuggingLocal<TraceContext>();


	private static LongKeyMap<TraceContext> deferredEntry = new LongKeyMap<TraceContext>();

	//pass = 1, discard = 2, end-processing-with-path = -1, end-processing-with-path = -2
	private static ThreadLocal<Integer> forceDiscard = new ThreadLocal<Integer>();

	static {
		entryByTxid.setMax(20000);
	}

	public static int size() {
		return entryByThreadId.size();
	}

	public static int[] getActiveCount() {
		int[] act = new int[3];
		try {
			long now = System.currentTimeMillis();
			Enumeration<TraceContext> en = entryByThreadId.values();
			while (en.hasMoreElements()) {
				TraceContext ctx = en.nextElement();
				long tm = now - ctx.startTime;
				if (tm < conf.trace_activeserivce_yellow_time) {
					act[0]++;
				} else if (tm < conf.trace_activeservice_red_time) {
					act[1]++;
				} else {
					act[2]++;
				}
			}

			Enumeration<TraceContext> enDeferred = deferredEntry.values();
			while (enDeferred.hasMoreElements()) {
				TraceContext ctx = enDeferred.nextElement();
				long tm = now - ctx.startTime;
				if (tm < conf.trace_activeserivce_yellow_time) {
					act[0]++;
				} else if (tm < conf.trace_activeservice_red_time) {
					act[1]++;
				} else {
					act[2]++;
				}
			}
		} catch (Throwable t) {
		}
		return act;
	}

	public static Enumeration<TraceContext> getContextEnumeration() {
		return entryByThreadId.values();
	}

	public static Enumeration<TraceContext> getDeferredContextEnumeration() {
		return deferredEntry.values();
	}

	public static TraceContext getContext() {
		Long txid = txidLocal.get();
		TraceContext traceContext = txid == null ? null : entryByTxid.get(txid);

		if (traceContext != null) {
			traceContext.getBy = ThreadLocalTxid;
			return traceContext;
		}

		txid = txidByCoroutine.get();
		traceContext = txid == null ? null : entryByTxid.get(txid);

		if (traceContext != null) {
			traceContext.getBy = ThreadLocalTxidByCoroutine;
			return traceContext;
		}

		traceContext = local.get();
		if (traceContext != null) {
			traceContext.getBy = ThreadLocal;
			return traceContext;
		}

		traceContext = getCoroutineContext();
		if (traceContext != null) {
			traceContext.getBy = CoroutineLocal;
			return traceContext;
		}

		return null;
	}

	public static TraceContext getContext(long key) {
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

	public static void startByCoroutine(TraceContext o) {
		txidByCoroutine.set(o.txid);
	}

	public static void end(TraceContext o) {
		clearAllContext(o);
	}

	public static void setTxidLocal(Long txid) {
		txidLocal.set(txid);
	}

	public static void asCoroutineDebuggingMode(Long coroutineId, TraceContext o) {
		CoroutineDebuggingLocal.setCoroutineDebuggingId(coroutineId);
		coroutineDebuggingLocal.put(o);
		local.set(null);
	}

	public static void toDeferred(TraceContext o) {
		deferredEntry.put(o.txid, o);
	}

	public static void completeDeferred(TraceContext o) {
		deferredEntry.remove(o.txid);
	}

	public static void clearAllContext(TraceContext o) {
		local.set(null);
		coroutineDebuggingLocal.clear(); //it should be prev of txidLocal clear

		entryByTxid.remove(o.txid);
		if (!o.isReactiveStarted) {
			entryByThreadId.remove(o.threadId);
		}

		txidByCoroutine.set(null);
		Long localTxid = txidLocal.get();
		txidLocal.set(null);

		if (localTxid != null && o.txid != localTxid) {
			Logger.println("G243", "unintended status on ending trace. o.txid is not localTxid : " + o.txid + " : " + localTxid);
		}
		clearForceDiscard();
	}
}
