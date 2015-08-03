/*
 *  Copyright 2015 LG CNS.
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

package scouter.agent.counter.task;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Enumeration;

import scouter.Version;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.counter.CounterBasket;
import scouter.agent.counter.anotation.Counter;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.netio.data.net.TcpWorker;
import scouter.lang.pack.ObjectPack;
import scouter.util.FileUtil;
import scouter.util.StringKeyLinkedMap;
import scouter.util.SysJMX;

public class AgentHeartBeat {
	public AgentHeartBeat() {
		Logger.info("objType:" + Configure.getInstance().scouter_type);
		Logger.info("objName:" + Configure.getInstance().objName);
	}

	private static StringKeyLinkedMap<ObjectPack> objects = new StringKeyLinkedMap<ObjectPack>();

	public static void addObject(String objType, int objHash, String objName) {
		if (objName == null)
			return;
		if (objName.equals(Configure.getInstance().objName))
			return;

		ObjectPack old = objects.get(objName);

		if (old != null && objType.equals(old.objType)) {
			return;
		}
		ObjectPack p = new ObjectPack();
		p.objType = objType;
		p.objHash = objHash;
		p.objName = objName;
		objects.put(objName, p);
	}

	@Counter
	public void alive(CounterBasket pw) {
		DataProxy.sendHeartBeat(getMainObject());
		Enumeration<ObjectPack> en = objects.values();
		while (en.hasMoreElements()) {
			DataProxy.sendHeartBeat(en.nextElement());
		}
	}

	private ObjectPack getMainObject() {
		Configure conf = Configure.getInstance();
		ObjectPack p = new ObjectPack();
		p.objType = conf.scouter_type;
		p.objHash = conf.objHash;
		p.objName = conf.objName;

		p.version = Version.getAgentFullVersion();
		p.address = TcpWorker.localAddr;
		return p;
	}

	public static void clearSubObjects() {
		objects.clear();
	}

	@Counter
	public void regist(CounterBasket pw) {
		Configure conf = Configure.getInstance();
		try {
			int pid = SysJMX.getProcessPID();
			File dir = new File(conf.object_registry);
			File file = new File(dir, Integer.toString(pid));
			if (file.canWrite()) {
				FileUtil.save(file, conf.objName.getBytes());
			}
		} catch (Exception e) {           
		}
	}
}