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
package scouter.agent.counter.task;
import scouter.Version;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.counter.CounterBasket;
import scouter.agent.counter.anotation.Counter;
import scouter.agent.netio.data.DataProxy;
import scouter.agent.netio.data.net.TcpWorker;
import scouter.agent.proxy.ToolsMainFactory;
import scouter.lang.pack.ObjectPack;
import scouter.lang.value.BooleanValue;
import scouter.util.FileUtil;
import scouter.util.StringKeyLinkedMap;
import scouter.util.StringUtil;
import scouter.util.SysJMX;

import java.io.File;
import java.util.Enumeration;

import static scouter.lang.constants.ScouterConstants.*;

public class AgentHeartBeat {
	static {
		Logger.println("objType:" + Configure.getInstance().obj_type);
		Logger.println("objName:" + Configure.getInstance().getObjName());
	}

	private static StringKeyLinkedMap<ObjectPack> objects = new StringKeyLinkedMap<ObjectPack>();
	public static void addObject(String objType, int objHash, String objName) {
		if (objName == null)
			return;
		if (objName.equals(Configure.getInstance().getObjName()))
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
		p.objType = conf.obj_type;
		p.objHash = conf.getObjHash();
		p.objName = conf.getObjName();
		p.version = Version.getAgentFullVersion();
		p.address = TcpWorker.localAddr;
		if(StringUtil.isNotEmpty(conf.getObjExtType())){
			p.tags.put(TAG_OBJ_EXT_TYPE, conf.getObjExtType());
		}
		if(StringUtil.isNotEmpty(conf.getObjDetectedType())){
			p.tags.put(TAG_OBJ_DETECTED_TYPE, conf.getObjDetectedType());
		}

		if (ToolsMainFactory.activeStack) {
			p.tags.put(TAG_ACTIVE_STACK, new BooleanValue(true));
		}
		p.tags.put(TAG_AUTODUMP_CPU_ENABLED, new BooleanValue(conf.autodump_cpu_exceeded_enabled));
		if (conf.autodump_cpu_exceeded_enabled) {
            p.tags.put(TAG_AUTODUMP_CPU_THRESHOLD, conf.autodump_cpu_exceeded_threshold_pct);
			p.tags.put(TAG_AUTODUMP_CPU_DURATION, conf.autodump_cpu_exceeded_duration_ms);
		}
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
			File dir = new File(conf.counter_object_registry_path);
			File file = new File(dir, pid + ".scouter");
			if (dir.canWrite()) {
				FileUtil.save(file, conf.getObjName().getBytes());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
