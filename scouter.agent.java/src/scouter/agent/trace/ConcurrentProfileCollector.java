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

import scouter.lang.step.StepSingle;

public class ConcurrentProfileCollector implements IProfileCollector {

	private IProfileCollector inner;

	public ConcurrentProfileCollector(IProfileCollector inner) {
		this.inner = inner;
	}

	public synchronized void add(StepSingle step) {
		this.inner.add(step);
	}

	public synchronized void push(StepSingle step) {
		this.inner.push(step);
	}

	public synchronized void pop(StepSingle step) {
		this.inner.pop(step);
	}

	public synchronized void close(boolean ok) {
		this.inner.close(ok);
	}

}
