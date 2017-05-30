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


public class BackJob extends Thread {

	private static BackJob instance = null;

	public final static synchronized BackJob getInstance() {
		if (instance == null) {
			instance = new BackJob();
			instance.setDaemon(true);
			instance.setName(ThreadUtil.getName(instance));
			instance.start();
		}
		return instance;
	}

	private static class Job {
		long interval;
		long nextExecTime;
		Runnable run;

		public Job(long interval, Runnable run) {
			this.interval = interval;
			this.run = run;
		}

	}

	private StringKeyLinkedMap< Job> jobTable = new StringKeyLinkedMap<Job>().setMax(1000);

	public void add(String name, long tm, Runnable r) {
		jobTable.put(name, new Job(tm, r));
	}

	public void remove(String name) {
		jobTable.remove(name);
	}

	public void run() {
		while (true) {
			ThreadUtil.sleep(1000);
			long now = System.currentTimeMillis();
			StringEnumer en = jobTable.keys();
			while (en.hasMoreElements()) {
				String name = en.nextString();
				try {
					Job job = jobTable.get(name);
					if (now >= job.nextExecTime) {
						job.nextExecTime = job.interval + System.currentTimeMillis();
					}
					job.run.run();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		}
	}

}