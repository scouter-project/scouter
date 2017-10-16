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

package scouter.util;

public class StopWatch {
	private long stime = System.currentTimeMillis();
	private long etime;

	public void start() {
		stime = System.currentTimeMillis();
		etime = 0;
	}

	public long getTime() {
		if (etime == 0) {
			long now = System.currentTimeMillis();
			return now - stime;
		} else {
			return etime - stime;
		}
	}

	public long stop() {
		if (etime == 0)
			etime = System.currentTimeMillis();

		return etime - stime;
	}
}
