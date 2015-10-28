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
 *
 */
package scouter.client.util;

import scouter.lang.step.Step;
import scouter.lang.step.StepSingle;

public class StepWrapper {
	public long time; 
	public long cpu; 
	public int  indent;
	public int sSummaryIdx;
	public Step step;
	public StepWrapper(long time, long cpu, int indent, int sSummaryIdx, Step step) {
		super();
		this.time = time;
		this.cpu = cpu;
		this.indent = indent;
		this.step = step;
		this.sSummaryIdx = sSummaryIdx;
	}
	
	public int getLastIndex(){
		if(step instanceof StepSingle){
			return ((StepSingle)step).index;
		}else{
			return sSummaryIdx;
		}
	}
}
