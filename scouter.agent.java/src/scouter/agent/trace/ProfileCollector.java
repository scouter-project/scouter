/*
 *  Copyright 2015 LG CNS.
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
import scouter.agent.netio.data.DataProxy;
import scouter.lang.step.Step;
import scouter.lang.step.StepSingle;

public class ProfileCollector implements IProfileCollector {
	private Configure conf=Configure.getInstance();
	private TraceContext context;
	protected Step[] buffer = new Step[conf.profile_step_max];
	protected int position = 0;

	public int this_index = 0;
	public int parent_index = -1;

	public ProfileCollector(TraceContext context) {
		this.context = context;
	}

	public  void push(StepSingle stepSingle) {
		stepSingle.index = this_index;
		stepSingle.parent = parent_index;
		parent_index = this_index;
		this_index++;
	}

	protected   void process(StepSingle stepSingle) {
		buffer[position++] = stepSingle;
		if (position >= buffer.length) {
			Step[] o = buffer;
			buffer = new Step[conf.profile_step_max];
			position = 0;
			DataProxy.sendProfile(o, context);
		}
	}

	public   void add(StepSingle stepSingle) {
		stepSingle.index = this_index;
		stepSingle.parent = parent_index;
		this_index++;
		process(stepSingle);
	}

	public   void pop(StepSingle stepSingle) {
		parent_index = stepSingle.parent;
		process(stepSingle);
	}

	public void close(boolean ok) {
		if (ok && position > 0) {
			StepSingle[] buff = new StepSingle[position];
			System.arraycopy(buffer, 0, buff, 0, position);
			DataProxy.sendProfile(buff, context);
		}
	}
}