package scouter.server.kube;
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

import scouter.lang.pack.MapPack;
import scouter.lang.pack.ObjectPack;
import scouter.net.RequestCmd;
import scouter.server.Logger;
import scouter.server.netio.AgentCall;
import scouter.util.StringUtil;

import java.util.concurrent.ConcurrentHashMap;

import static scouter.lang.constants.ScouterConstants.HOST_NAME;
import static scouter.lang.constants.ScouterConstants.KUBE_SEQ;
import static scouter.lang.constants.ScouterConstants.USE_KUBE_SEQ;

/**
 * Created by Gun Lee(gunlee01@gmail.com) on 2021/10/23
 */
public class PodSeqManager {

	private static final ConcurrentHashMap<String, PodSeqManager> managerMap = new ConcurrentHashMap<>();

	private static final ConcurrentHashMap<String, ObjectPack> initialHostObjMap = new ConcurrentHashMap<>();

	private final Object lock = new Object();
	private final String podName;
	private final ConcurrentHashMap<String, Integer> hostSeqMap;
	private final ConcurrentHashMap<Integer, String> seqHostMap;

	private PodSeqManager(String podName) {
		this.podName = podName;
		hostSeqMap = new ConcurrentHashMap<>();
		seqHostMap = new ConcurrentHashMap<>();
	}

	public static synchronized PodSeqManager getInstance(String podName) {
		return managerMap.computeIfAbsent(podName, PodSeqManager::new);
	}

	public void applyPodSeq(ObjectPack pack) {
		synchronized (lock) {
			String hostName = pack.tags.getText(HOST_NAME);
			int kubeSeq = pack.tags.getInt(KUBE_SEQ);
			boolean useKubeSeq = pack.tags.getBoolean(USE_KUBE_SEQ);
			pack.addKubeProp(podName, hostName, -1);
			if (!useKubeSeq || StringUtil.isEmpty(hostName)) {
				return;
			}

			if (kubeSeq <= -1) {
				Logger.println("S104-0", String.format("Init host agent for hostName: %s, pod: %s, obj: %s", hostName, podName, pack));
				applyNewPodSeqToPack(pack, hostName);

			} else {
				pack.podSeq = kubeSeq;

				String hostInMap = seqHostMap.get(kubeSeq);
				if (hostInMap == null) {
					Logger.println("S104-1", String.format("recover host's pod seq: %s, host: %s, pod: %s", kubeSeq, hostName, podName));
					seqHostMap.put(kubeSeq, hostName);
					hostSeqMap.put(hostName, kubeSeq);

				} else if (!hostInMap.equals(hostName)) {
					Logger.println("S104-2", String.format("already taken the pod seq: %s, it will be init. host: %s, pod: %s",
							kubeSeq, hostName, podName));
					applyNewPodSeqToPack(pack, hostName);

				} else {
					initialHostObjMap.remove(hostName); //seq가 부여되었으니 삭제
				}
			}
		}
	}

	private void applyNewPodSeqToPack(ObjectPack pack, String hostName) {
		pack.pushSeq = true;

		ObjectPack initialPack = initialHostObjMap.get(hostName);
		if (initialPack == null) {
			initialHostObjMap.put(hostName, pack);
			pack.initialTime = System.currentTimeMillis();
			pack.podSeq = getNewPodSeq(hostName);
			pack.allowProceed = false; //Server에 Object 등록을 더이상 진행하지 않는다.

		} else {
			pack.copyKubePropFrom(initialPack);
			if (initialPack.initialTime > System.currentTimeMillis() - 15000) { //등록 15초가 지나지 않았으면 진행을 멈춤
				pack.allowProceed = false;

			} else { //15초가 지난 경우 그냥 넘어간다. initialHostObjMap 에서는 삭제하면 안됨
				pack.pushSeq = false;
				removePodSeq(hostName); //부여받은 pod seq를 제거한다.
			}
		}
	}

	private int getNewPodSeq(String hostName) {
		Integer prevSeq = hostSeqMap.get(hostName);
		if (prevSeq != null) {
			seqHostMap.put(prevSeq, hostName);
			return prevSeq;
		}
		int newSeq = -1;
		for (int i = 0; i < 10000; i++) {
			String hostInMap = seqHostMap.get(i);
			if (hostInMap == null) {
				newSeq = i;
				hostSeqMap.put(hostName, newSeq);
				seqHostMap.put(newSeq, hostName);
				break;
			}
		}
		return newSeq;
	}

	public void objectInactivated(String hostName) {
		if (hostName != null) {
			removePodSeq(hostName);
			removeInitialHostObject(hostName);
		}
	}
	private void removePodSeq(String hostName) {
		synchronized (lock) {
			Integer seq = hostSeqMap.remove(hostName);
			if (seq != null) {
				seqHostMap.remove(seq);
			}
		}
	}

	private void removeInitialHostObject(String hostName) {
		initialHostObjMap.remove(hostName);
	}

	public static void pushSeqToAgent(ObjectPack pack) {
		if (pack.pushSeq) {
			try {
				MapPack param = new MapPack();
				param.put("seq", pack.podSeq);
				AgentCall.call(pack, RequestCmd.OBJECT_SET_KUBE_SEQ, param);
			} catch (Exception e) {
				Logger.println("S104-e1", 10, "error to call agent " + pack + ", err: " + e.getMessage());
			}
		}
	}

}
