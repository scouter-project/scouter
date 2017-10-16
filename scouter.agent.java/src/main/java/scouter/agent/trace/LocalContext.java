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

public class LocalContext {
  	public StepSingle stepSingle;
	public TraceContext context;
	public Object option;
	public boolean service;
	public LocalContext(){
	}
	public LocalContext(TraceContext ctx, StepSingle stepSingle) {
		this.context = ctx;
		this.stepSingle = stepSingle;
	}
	public LocalContext(TraceContext ctx, StepSingle stepSingle, Object option) {
		this.context = ctx;
		this.stepSingle = stepSingle;
		this.option=option;
	}
	public LocalContext (Object stat) {
		option=stat;
		this.service=true;
	}
}