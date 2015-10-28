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
package scouter.client.model;

import scouter.util.ThreadUtil;

public class RefreshThread extends Thread {
	public static interface Refreshable {
		public void refresh();
	}

	private int interval;

	public RefreshThread(Refreshable view, int interval) {
		this.view = view;
		this.setDaemon(true);
		this.interval = interval;
		this.setName(view.toString());
	}

	final private Refreshable view;

	public void setThreadName(String name){
		this.setName(name);
	}
	
	public void run() {

		while (brun) {
			view.refresh();
			ThreadUtil.sleep(interval);
//			for (int i = 0; i < interval / 100 && brun; i++) {
//				ThreadUtil.sleep(100);
//			}
		}
	}

	private boolean brun = true;

	public void shutdown() {
		brun = false;
	}
}
