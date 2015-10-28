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
package scouter.client.group;

import scouter.util.DateUtil;


public enum DatePeriodUnit {
	A_MONTH ("30 days", 30 * DateUtil.MILLIS_PER_DAY),
	A_WEEK ("7 days",  7 * DateUtil.MILLIS_PER_DAY),
	A_DAY ("1 day", DateUtil.MILLIS_PER_DAY);
	
	private String label;
	private long time;
	
	private DatePeriodUnit(String label,  long time) {
		this.label = label;
		this.time = time;
	}
	
	public String getLabel() {
		return this.label;
	}
	
	public long getTime() {
		return this.time;
	}
}
