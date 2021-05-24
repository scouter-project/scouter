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

import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.netio.data.net.DataUdpAgent;
import scouter.io.DataOutputX;
import scouter.lang.TextTypes;
import scouter.lang.pack.AlertPack;
import scouter.lang.pack.ObjectPack;
import scouter.lang.pack.Pack;
import scouter.lang.pack.PerfCounterPack;
import scouter.lang.pack.TextPack;
import scouter.lang.value.MapValue;
import scouter.util.IntLinkedSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HostAgentDataProxy {
	private static DataUdpAgent udpCollect = DataUdpAgent.getInstance();

	static Configure conf = Configure.getInstance();

	public static void sendAlert(byte level, String title, String message, MapValue tags) {
		AlertPack p = new AlertPack();
		p.objType = conf.obj_type;
		p.objHash = conf.getObjHash();
		p.level = level;
		p.title = title;
		p.message = message;
		if (tags != null) {
			p.tags = tags;
		}

		sendDirect(p);
	}

	private static IntLinkedSet errText = new IntLinkedSet().setMax(10000);

	public static void sendError(int hash, String message) {
		if (errText.contains(hash)) {
			return;
		}
		errText.put(hash);
		try {
			udpCollect.write(new DataOutputX().writePack(new TextPack(TextTypes.ERROR, hash, message)).toByteArray());
		} catch (Exception e) {
		}
	}

	public static void reset() {
		errText.clear();
	}

	static DataUdpAgent udpNet = DataUdpAgent.getInstance();

	private static void sendDirect(Pack p) {
		try {
			udpNet.write(new DataOutputX().writePack(p).toByteArray());
		} catch (IOException e) {
		}
	}

	private static void sendDirect(List<byte[]> buff) {
		switch (buff.size()) {
		case 1:
			udpNet.write(buff.get(0));
			break;
		default:
			udpNet.write(buff);
			break;
		}
	}

	static DataUdpAgent udpDirect = DataUdpAgent.getInstance();

	public static void sendCounter(PerfCounterPack[] p) {
		// udp.add(p);
		try {
			List<byte[]> buff = new ArrayList<byte[]>();
			int bytes = 0;
			for (int k = 0; k < p.length; k++) {
				byte[] b = new DataOutputX().writePack(p[k]).toByteArray();
				if (bytes + b.length >= conf.net_udp_packet_max_bytes) {
					sendDirect(buff); // buff.size가 0일수도 있다.
					bytes = 0;// bytes 값 초기화..
					buff.clear();
				}
				bytes += b.length;
				buff.add(b);
			}
			sendDirect(buff);
		} catch (Exception e) {
		}
	}

	public static void sendHeartBeat(ObjectPack p) {
		try {
			udpCollect.write(new DataOutputX().writePack(p).toByteArray());
		} catch (Exception e) {
		}
		if (conf.log_udp_object) {
			Logger.info(p.toString());
		}
	}

}
