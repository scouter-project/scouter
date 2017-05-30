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
package scouter.agent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import scouter.util.ThreadUtil;

public class BackJobs extends Thread {

	private static BackJobs instance = null;

	public final static synchronized BackJobs getInstance() {
		if (instance == null) {
			instance = new BackJobs();
			instance.setDaemon(true);
			instance.setName(ThreadUtil.getName(instance));
			instance.start();
		}
		return instance;
	}

	protected BackJobs() {
	}

	public class JobW {
		Runnable job;
		long lastExTime;
		int interval;

		public JobW(Runnable job, int interval) {
			this.job = job;
			this.interval = interval;
		}
	}

	private Map<String, JobW> jobs = new HashMap<String, JobW>();

	/**
	 * 반복적으로 실행할 잡을 등록한다. 
	 * @param id
	 * @param interval - 1000보다 작으면 1000 이 적용된다.
	 * @param job
	 */
	public void put(String id, int interval, Runnable job) {
		jobs.put(id, new JobW(job, interval));
	}

	public void remove(String id) {
		jobs.remove(id);
	}

	public void shutdown() {
		running = false;
	}

	private boolean running = true;

	public void run() {

		while (running) {
			ThreadUtil.sleep(1000);
			try {
				process();
			} catch (Throwable t) {
				Logger.println("A109", t);
			}
		}

	}

	private void process() {
		Iterator<Map.Entry<String, JobW>> itr = jobs.entrySet().iterator();
		while (itr.hasNext()) {
			Map.Entry<String, JobW> j = itr.next();
			JobW jobw = j.getValue();
			long now = System.currentTimeMillis();
			if (now >= jobw.lastExTime + jobw.interval) {
				jobw.lastExTime = now;
				try {
					jobw.job.run();
				} catch (Exception e) {
					Logger.println("A110", j.getKey() + ":" + e);
				}
			}
		}
	}
}
