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

import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.counter.anotation.Counter;
import scouter.agent.counter.anotation.InteractionCounter;
import scouter.agent.netio.data.DataProxy;
import scouter.lang.pack.InteractionPerfCounterPack;
import scouter.lang.pack.PerfCounterPack;
import scouter.util.ThreadUtil;
import scouter.util.scan.Scanner;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
public class CounterExecutingManager extends Thread {

	Configure conf = Configure.getInstance();
	private static CounterExecutingManager instance;
	private List<CountStat> countStatList = new ArrayList<CountStat>();
	private List<CountStat> interactionCountStatList = new ArrayList<CountStat>();

	private CounterExecutingManager() {
	}

	public final static synchronized CounterExecutingManager getInstance() {
		if (instance == null) {
			instance = new CounterExecutingManager();
			instance.setDaemon(true);
			instance.setName(ThreadUtil.getName(instance));
			instance.start();
		}
		return instance;
	}

	public void run() {
		while (true) {
			ThreadUtil.sleep(1000);
			if (conf.counter_enabled == false) {
				continue;
			}
			long now = System.currentTimeMillis();
			gatherAndSendCounter(now);
			gatherAndSendInteractionCounter(now);
		}
	}

	private void gatherAndSendCounter(long now) {
		CounterBasket basket = new CounterBasket();
		for (int i = 0; i < countStatList.size(); i++) {
			CountStat stat = countStatList.get(i);
			try {
				if (stat.counter.interval() <= now - stat.xtime) {
					stat.xtime = now;
					stat.counter.process(basket);
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		PerfCounterPack[] packs = basket.getList();
		DataProxy.sendCounter(packs);
	}

	private void gatherAndSendInteractionCounter(long now) {
		InteractionCounterBasket basket = new InteractionCounterBasket();
		for (int i = 0; i < interactionCountStatList.size(); i++) {
			CountStat stat = interactionCountStatList.get(i);
			try {
				if (stat.counter.interval() <= now - stat.xtime) {
					stat.xtime = now;
					stat.counter.process(basket);
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		InteractionPerfCounterPack[] packs = basket.geAllAsArray();
		if (packs != null && packs.length > 0) {
			DataProxy.sendCounter(packs);
		}
	}

	public void putCounter(Invocation counter) {
		countStatList.add(new CountStat(counter));
	}

	public void putInteractionCounter(Invocation counter) {
		interactionCountStatList.add(new CountStat(counter));
	}

	public static void load() {
		Set<String> defaultTasks = new Scanner("scouter.agent.counter.task").process();
		Set<String> customTasks = new Scanner(System.getProperty("scouter.task")).process();
		defaultTasks.addAll(customTasks);

		int counterCount = 0;
		int interactionCounterCount = 0;
		Iterator<String> itr = defaultTasks.iterator();
		while (itr.hasNext()) {
			try {
				Class c = Class.forName(itr.next());
				if (Modifier.isPublic(c.getModifiers()) == false)
					continue;
				Method[] m = c.getDeclaredMethods();
				for (int i = 0; i < m.length; i++) {
					Counter cntAn = m[i].getAnnotation(Counter.class);
					if (cntAn != null) {
						int interval=cntAn.interval();
						CounterExecutingManager.getInstance().putCounter(new Invocation(c.newInstance(), m[i], interval));
						counterCount++;
					}

					InteractionCounter icntAnot = m[i].getAnnotation(InteractionCounter.class);
					if (icntAnot != null) {
						int interval=icntAnot.interval();
						CounterExecutingManager.getInstance().putInteractionCounter(new Invocation(c.newInstance(), m[i], interval));
						interactionCounterCount++;
					}
				}
			} catch (Throwable t) {
				scouter.agent.Logger.println("A112", ThreadUtil.getStackTrace(t));
			}
		}
		scouter.agent.Logger.println("A113", "Counter Collector Started (#" + counterCount + ")");
		scouter.agent.Logger.println("A113", "InteractionCounter Collector Started (#" + counterCount + ")");
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
				Logger.println("A111", object.getClass() + " " + method + " " + e.getMessage(), e);
				e.printStackTrace();
			}
		}

		public void process(InteractionCounterBasket pw) throws Throwable {
			try {
				method.invoke(object, pw);
			} catch (Exception e) {
				Logger.println("A111-1", object.getClass() + " " + method + " " + e);
			}
		}

		public long interval() {
			return this.time;
		}
	}

	static class CountStat {
		Invocation counter;
		long xtime;

		CountStat(Invocation counter) {
			this.counter = counter;
		}
	}
}

















