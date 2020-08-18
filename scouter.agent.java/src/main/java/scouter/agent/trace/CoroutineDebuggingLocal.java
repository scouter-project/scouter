package scouter.agent.trace;
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

import scouter.util.LongKeyMap;

/**
 * Created by Gun Lee(gunlee01@gmail.com) on 30/07/2020
 */
public class CoroutineDebuggingLocal<T> {
	private static final ThreadLocal<Long> COROUTINE_DEBUGGING_ID = new ThreadLocal<Long>();

	private static final LongKeyMap CID_TRACE_CONTEXT = new LongKeyMap();

	public static void setCoroutineDebuggingId(Long id) {
		COROUTINE_DEBUGGING_ID.set(id);
	}

	public static Long getCoroutineDebuggingId() {
		return COROUTINE_DEBUGGING_ID.get();
	}

	public static void releaseCoroutineId() {
		COROUTINE_DEBUGGING_ID.remove();
	}


	public T get() {
		Long coroutineId = getCoroutineDebuggingId();
		if (coroutineId == null) {
			return null;
		}
		return (T) CID_TRACE_CONTEXT.get(coroutineId);
	}

	public T get(long id) {
		return (T) CID_TRACE_CONTEXT.get(id);
	}

	public void put(T obj) {
		Long coroutineId = getCoroutineDebuggingId();
		CID_TRACE_CONTEXT.put(coroutineId, obj);
	}

	public void clear() {
		Long coroutineId = getCoroutineDebuggingId();
		if (coroutineId == null) {
			return;
		}
		CID_TRACE_CONTEXT.put(coroutineId, null);
		releaseCoroutineId();
	}
}
