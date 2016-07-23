/*
 *  Copyright 2016 the original author or authors. 
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

package scouter.agent.batch.net;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import scouter.agent.batch.Configure;

import scouter.io.DataOutputX;
import scouter.lang.pack.Pack;
public class UdpAgent {
	static public void sendUdpPack(Pack pack){
		Configure conf = Configure.getInstance();
		DatagramSocket datagram = null;
		InetAddress server = null;
		try {
			server = InetAddress.getByName(conf.net_collector_ip);
			datagram = new DatagramSocket(conf.net_collector_udp_port, InetAddress.getByName(conf.net_collector_ip));

			byte[] buff = new DataOutputX().writePack(pack).toByteArray();
			DatagramPacket packet = new DatagramPacket(buff, buff.length);
			packet.setAddress(server);
			packet.setPort(conf.net_collector_udp_port);
			datagram.send(packet);			
		} catch (Exception e) {
			e.printStackTrace();
			return;
		} finally{
			if(datagram != null){
				try { datagram.close(); } catch(Exception ex){}
			}
		}
	}
}