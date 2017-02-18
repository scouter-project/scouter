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

package scouter.agent.batch.netio.data.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import scouter.agent.Logger;
import scouter.agent.batch.Configure;

import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.net.NetCafe;
import scouter.util.KeyGen;

public class UdpAgent {
	static public boolean sendUdp(String IPAddress, int port, byte [] byteArray){
		InetAddress server = null;
		try {
			server = InetAddress.getByName(IPAddress);
			return sendUdp(server, port, byteArray);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	static public boolean sendUdp(InetAddress IPAddress, int port, byte [] byteArray){
		DatagramSocket datagram = null;
		try {
			Configure conf = Configure.getInstance();
			if (byteArray.length > conf.net_udp_packet_max_bytes) {
				return sendMTU(IPAddress, port, byteArray, conf.net_udp_packet_max_bytes);
			}
			datagram = new DatagramSocket();
			DatagramPacket packet = new DatagramPacket(byteArray, byteArray.length);
			packet.setAddress(IPAddress);
			packet.setPort(port);
			datagram.send(packet);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally{
			if(datagram != null){
				try { datagram.close(); } catch(Exception ex){}
			}
		}
		return false;
	}
	
	static public boolean sendMTU(InetAddress IPAddress, int port, byte[] data, int packetSize) {
		try {
			if (IPAddress == null)
				return false;
			long pkid = KeyGen.next();
			
			int availPacketSize = packetSize - 23; // Packet header size is 23
			int totalPacketCnt;
			int total = data.length / availPacketSize;
			int remainder = data.length % availPacketSize;
			int num = 0;
			boolean isSuccess = true;
			
			totalPacketCnt = total;
			if(remainder > 0){
				totalPacketCnt++;
			}
			
			for (num = 0; (isSuccess && num < total); num++) {
				isSuccess = SendMTU(IPAddress, port, pkid, totalPacketCnt, num, availPacketSize, DataInputX.get(data, num * availPacketSize, availPacketSize));
			}
			if (isSuccess && remainder > 0) {
				isSuccess = SendMTU(IPAddress, port, pkid, totalPacketCnt, num, remainder, DataInputX.get(data, data.length - remainder, remainder));
			}
			return isSuccess;
		} catch (IOException e) {
			Logger.println("A121","UDP", e);
		}
		return false;
	}
	
	static private boolean SendMTU(InetAddress IPAddress, int port, long pkid, int total, int num, int packetSize, byte[] data) throws IOException {
		Configure conf = Configure.getInstance();
		DataOutputX out = new DataOutputX();
		out.write(NetCafe.CAFE_MTU);
		out.writeInt(conf.getObjHash());
		out.writeLong(pkid);
		out.writeShort(total);
		out.writeShort(num);
		out.writeBlob(data);
				
		return sendUdp(IPAddress, port, out.toByteArray());
	}
}