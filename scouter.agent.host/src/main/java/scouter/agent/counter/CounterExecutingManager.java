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
package scouter.agent.counter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.counter.anotation.Counter;
import scouter.agent.netio.data.HostAgentDataProxy;
import scouter.lang.pack.PerfCounterPack;
import scouter.util.ThreadUtil;
import scouter.util.scan.Scanner;
public class CounterExecutingManager extends Thread {
	private static CounterExecutingManager instance;
	public final static synchronized CounterExecutingManager getInstance() {
		if (instance == null) {
			instance = new CounterExecutingManager();
			instance.setDaemon(true);
			instance.setName(ThreadUtil.getName(instance));
			instance.start();
		}
		return instance;
	}
	
	Configure conf = Configure.getInstance();
	public void run() {
		while (true) {
			ThreadUtil.sleep(1000);
			if (conf.counter_enabled == false) {
				continue;
			}
			long now = System.currentTimeMillis();
			CounterBasket pw = new CounterBasket();
			for (int i = 0; i < taskSec.size(); i++) {
				CountStat r = taskSec.get(i);
				try {
					if (r.counter.interval() <= now - r.xtime) {
						r.xtime = now;
						r.counter.process(pw);
					}
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
			//
			PerfCounterPack[] pks = pw.getList();
			HostAgentDataProxy.sendCounter(pks);
		}
	}
	private CounterExecutingManager() {
	}
	private List<CountStat> taskSec = new ArrayList<CountStat>();
	static class CountStat {
		Invocation counter;
		long xtime;
		CountStat(Invocation counter) {
			this.counter = counter;
		}
	}
	public void put(Invocation counter) {
		taskSec.add(new CountStat(counter));
	}
	protected static class Invocation {
		Object object;
		Method method;
		long time;
		public Invocation(Object object, Method method, long interval) {
			this.object = object;
			this.method = method;
			this.time=interval;
		}
		public void process(CounterBasket pw) throws Throwable {
			try {
				method.invoke(object, pw);
			} catch (Exception e) {
				Logger.println("A111", object.getClass() + " " + method + " " + e);
			}
		}
		public long interval() {
			return this.time;
		}
	}
	public static void load() {
		Set<String> defaultTasks = new Scanner("scouter.agent.counter.task").process();
		Set<String> customTasks = new Scanner(System.getProperty("scouter.task")).process();
		defaultTasks.addAll(customTasks);
		
		int n = 0;
		Iterator<String> itr = defaultTasks.iterator();
		while (itr.hasNext()) {
			try {
				Class c = Class.forName(itr.next());
				if (Modifier.isPublic(c.getModifiers()) == false)
					continue;
				Method[] m = c.getDeclaredMethods();
				for (int i = 0; i < m.length; i++) {
					Counter mapAn = (Counter) m[i].getAnnotation(Counter.class);
					if (mapAn == null)
						continue;
					int interval=mapAn.interval();
					CounterExecutingManager.getInstance().put(new Invocation(c.newInstance(), m[i], interval));
					n++;
				}
			} catch (Throwable t) {
				scouter.agent.Logger.println("A112", ThreadUtil.getStackTrace(t));
			}
		}
		scouter.agent.Logger.println("A113", "Counter Collector Started (#" + n + ")");
	}
	public static void main(String[] args) {
		load();
	}
}
