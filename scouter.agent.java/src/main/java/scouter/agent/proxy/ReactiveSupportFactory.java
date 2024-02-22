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
package scouter.agent.proxy;

import scouter.agent.Logger;
import scouter.agent.trace.TraceContext;

public class ReactiveSupportFactory {
	private static final String REACTIVE_SUPPORT = "scouter.xtra.reactive.ReactiveSupport";
	private static final String REACTIVE_SUPPORT_W_COROUTINE = "scouter.xtra.reactive.ReactiveSupportWithCoroutine";

	public static final IReactiveSupport dummy = new IReactiveSupport() {
		public Object subscriptOnContext(Object mono0, TraceContext traceContext) {
			return mono0;
		}
		public void contextOperatorHook() {
		}

		@Override
		public Object monoCoroutineContextHook(Object coroutineContext, TraceContext traceContext) {
			return coroutineContext;
		}

		@Override
		public String dumpScannable(TraceContext traceContext, TraceContext.TimedScannable timedScannable, long now) {
			return null;
		}

		@Override
		public boolean isReactor34() {
			return false;
		}
	};

	public static IReactiveSupport create(ClassLoader parent) {
		try {
			ClassLoader loader = LoaderManager.getReactiveClient(parent);
			if (loader == null) {
				return dummy;
			}
			IReactiveSupport reactiveSupport = null;
			try {
				Class c = Class.forName(REACTIVE_SUPPORT_W_COROUTINE, true, loader);
				reactiveSupport = (IReactiveSupport) c.newInstance();
			} catch (Throwable e) {
				Logger.println("A133-0", "fail to create reactive support: REACTIVE_SUPPORT_W_COROUTINE", e);
				Class c = Class.forName(REACTIVE_SUPPORT, true, loader);
				reactiveSupport = (IReactiveSupport) c.newInstance();
				Logger.println("success to create reactive support without coroutine support");
			}
			return reactiveSupport;

		} catch (Throwable e) {
			Logger.println("A133-2", "fail to create", e);
			return dummy;
		}
	}
}
