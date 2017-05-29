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

package scouter.agent.netio.data;

import java.util.ArrayList;
import java.util.List;

import scouter.agent.Configure;
import scouter.agent.netio.data.net.DataUdpAgent;
import scouter.io.DataOutputX;
import scouter.lang.pack.Pack;
import scouter.util.Queue;
import scouter.util.ThreadUtil;

public class UDPDataSendThread extends Thread {

	private static UDPDataSendThread instance = null;

	public final static synchronized UDPDataSendThread getInstance() {
		if (instance == null) {
			instance = new UDPDataSendThread();
			instance.setDaemon(true);
			instance.setName(ThreadUtil.getName(instance));
			instance.start();
		}
		return instance;
	}

	protected UDPDataSendThread() {
	}

	private Queue<byte[]> queue = new Queue<byte[]>(1024);

	public int getQueueSize() {
		return queue.size();
	}

	public boolean add(Pack p) {
		try {
			byte[] b = new DataOutputX().writePack(p).toByteArray();
			Object ok = queue.push(b);
			return ok != null;
		} catch (Exception e) {
		}
		return false;
	}

	public boolean isQueueOk() {
		return queue.size() < 1000;
	}

	public void shutdown() {
		running = false;
	}

	private boolean running = true;

	public void run() {
		DataUdpAgent udp = DataUdpAgent.getInstance();

		while (running) {
			int size = queue.size();
			switch (size) {
			case 0:
				ThreadUtil.sleep(conf.net_udp_collection_interval_ms);
				break;
			case 1:
				udp.write(queue.pop());
				break;
			default:
				send(udp, size);
				break;
			}

		}
	}

	private Configure conf = Configure.getInstance();
	private void send(DataUdpAgent udp, int size) {
		List<byte[]> buff = new ArrayList<byte[]>();
		int bytes = 0;
		for (int k = 0; k < size; k++) {
			byte[] b = queue.pop();
			if (bytes + b.length >= conf.net_udp_packet_max_bytes) {
				send(udp, buff); //buff.size가 0일수도 있다.
				bytes = 0;// bytes 값 초기화..
				buff.clear();
			}
			bytes += b.length;
			buff.add(b);
		}

		send(udp, buff);

	}

	public void send(DataUdpAgent udp, List<byte[]> buff) {
		switch (buff.size()) {
		case 0:
			break;
		case 1:
			udp.write(buff.get(0));
			break;
		default:
			udp.write(buff);
			break;
		}
	}

}