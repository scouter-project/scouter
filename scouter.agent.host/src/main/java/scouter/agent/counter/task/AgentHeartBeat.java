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
import scouter.agent.counter.CounterBasket;
import scouter.agent.counter.anotation.Counter;
import scouter.agent.netio.data.HostAgentDataProxy;
import scouter.agent.netio.data.net.TcpWorker;
import scouter.lang.pack.ObjectPack;
import scouter.lang.value.BooleanValue;
import scouter.util.FileUtil;
import scouter.util.StringKeyLinkedMap;
import scouter.util.StringUtil;

import java.io.File;
import java.util.Enumeration;

import static scouter.lang.constants.ScouterConstants.HOST_NAME;
import static scouter.lang.constants.ScouterConstants.KUBE_SEQ;
import static scouter.lang.constants.ScouterConstants.POD_NAME;
import static scouter.lang.constants.ScouterConstants.TAG_OBJ_DETECTED_TYPE;
import static scouter.lang.constants.ScouterConstants.USE_KUBE_SEQ;

public class AgentHeartBeat {
	public AgentHeartBeat() {
	}

	private static StringKeyLinkedMap<ObjectPack> objects = new StringKeyLinkedMap<ObjectPack>();

	public static void addObject(String objType, int objHash, String objName) {

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
		HostAgentDataProxy.sendHeartBeat(getMainObject());
		Enumeration<ObjectPack> en = objects.values();
		while (en.hasMoreElements()) {
			HostAgentDataProxy.sendHeartBeat(en.nextElement());
		}
	}

	@Counter
	public void writeHostNameForKube(CounterBasket pw) {
		Configure conf = Configure.getInstance();
		if (conf.isUseKubeHostName() && conf.getSeqNoForKube() > -1) {
			long seqNoForKube = conf.getSeqNoForKube();
			File dir = new File(conf.counter_object_registry_path);
			File file = new File(dir, seqNoForKube + ".scouterkubeseq");
			if (dir.canWrite()) {
				deleteAllHostNameFileWithIgnore(dir, seqNoForKube);
				FileUtil.save(file, conf.obj_name.getBytes());
			}

		} else {
			File dir = new File(conf.counter_object_registry_path);
			deleteAllHostNameFileWithIgnore(dir, -1);
		}
	}

	private void deleteAllHostNameFileWithIgnore(File dir, long ignoreSeq) {
		if (dir == null)
			return;

		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory())
				continue;
			String name = files[i].getName();
			if (!name.endsWith(".scouterkubeseq")) {
				continue;
			}

			int kubeSeq = cintErrorMinusOne(name.substring(0, name.lastIndexOf(".")));
			if (kubeSeq < 0)
				continue;
			if (kubeSeq == ignoreSeq) {
				continue;
			}
			if (files[i].canWrite()) {
				files[i].delete();
			}
		}
	}

	public static int cintErrorMinusOne(String value) {
		if (value == null) {
			return -1;
		} else {
			try {
				return Integer.parseInt(value);
			} catch (Exception e) {
				return -1;
			}
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

		if(StringUtil.isNotEmpty(conf.getObjDetectedType())){
			p.tags.put(TAG_OBJ_DETECTED_TYPE, conf.getObjDetectedType());
		}

		p.tags.put(HOST_NAME, conf.host_name);
		p.tags.put(POD_NAME, conf.getPodName());
		p.tags.put(KUBE_SEQ, conf.getSeqNoForKube());
		p.tags.put(USE_KUBE_SEQ, new BooleanValue(conf.isUseKubeHostName()));

		return p;
	}

	public static void clearSubObjects() {
		objects.clear();
	}
}
