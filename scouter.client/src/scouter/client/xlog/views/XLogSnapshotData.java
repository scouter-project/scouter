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
package scouter.client.xlog.views;

public class XLogSnapshotData {
	int cnt = 1;
	int sum;
	int errorCnt = 0;
	int urlHash = 0;
	int defaultServerId;
	boolean selected = false;

	public XLogSnapshotData(int urlHash, int elapsed, boolean isError, int defaultServerId) {
		super();
		this.urlHash = urlHash;
		this.sum = elapsed;
		this.errorCnt = isError ? 1 : 0;
		this.defaultServerId = defaultServerId;
	}

	public void addElapsed(int elapsed, boolean err) {
		this.sum += elapsed;
		this.cnt++;
		if (err)
			this.errorCnt++;
	}
	
	public float getAvg() {
		return (float) sum / cnt;
	}
}
