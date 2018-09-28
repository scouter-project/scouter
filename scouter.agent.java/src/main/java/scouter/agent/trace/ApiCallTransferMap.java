/*
 *  Copyright 2015 the original author or authors.
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package scouter.agent.trace;

import scouter.lang.step.ApiCallStep2;
import scouter.util.IntKeyLinkedMap;

public class ApiCallTransferMap {

	public static class ID {
		public TraceContext ctx;
		public ApiCallStep2 step;

		public ID(TraceContext ctx, ApiCallStep2 step) {
			this.ctx = ctx;
			this.step = step;
		}
	}

	private static IntKeyLinkedMap<ID> map = new IntKeyLinkedMap<ID>().setMax(2001);

	public static void put(int hash, TraceContext ctx, ApiCallStep2 step) {
		map.put(hash, new ID(ctx,step));
	}

	public static void remove(int hash) {
		map.remove(hash);
	}

	public static ID get(int hash) {
		return map.get(hash);
	}


}
