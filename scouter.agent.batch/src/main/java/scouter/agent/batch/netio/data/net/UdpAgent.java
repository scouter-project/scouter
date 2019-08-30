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

import scouter.agent.batch.Logger;
import scouter.agent.batch.Configure;

import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.lang.pack.Pack;
import scouter.net.NetCafe;
import scouter.util.KeyGen;

public class UdpAgent {
	static public boolean sendUdpPackToServer(String IPAddress, int port, Pack pack){
		InetAddress server = null;
		Configure conf = Configure.getInstance();
		try {
			server = InetAddress.getByName(IPAddress);
			byte [] byteArray = new DataOutputX().writePack(pack).toByteArray();
			
			if(byteArray.length > conf.net_udp_packet_max_bytes){
				return sendMTU(server, port, byteArray, conf.net_udp_packet_max_bytes);			
			}else{
				return sendUdpDirect(server, port, new DataOutputX().write(NetCafe.CAFE).write(byteArray).toByteArray());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}	
	
	static public boolean sendUdp(String ip, int port, byte [] byteArray){
		InetAddress server;
		try {
			server = InetAddress.getByName(ip);
			return sendUdp(server, port, byteArray);	
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	static public boolean sendUdp(InetAddress server, int port, byte [] byteArray){
		Configure conf = Configure.getInstance();
		try {		
			if(byteArray.length > conf.net_udp_packet_max_bytes){
				return sendMTU(server, port, byteArray, conf.net_udp_packet_max_bytes);			
			}else{
				return sendUdpDirect(server, port, byteArray);
			}
		} catch (Exception e) {
			e.printStackTrace();
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
				isSuccess = SendMTUDirect(IPAddress, port, pkid, totalPacketCnt, num, availPacketSize, DataInputX.get(data, num * availPacketSize, availPacketSize));
			}
			if (isSuccess && remainder > 0) {
				isSuccess = SendMTUDirect(IPAddress, port, pkid, totalPacketCnt, num, remainder, DataInputX.get(data, data.length - remainder, remainder));
			}
			return isSuccess;
		} catch (IOException e) {
			Logger.println("A121","UDP", e);
		}
		return false;
	}
	
	static public boolean sendUdpDirect(InetAddress IPAddress, int port, byte [] byteArray){
		DatagramSocket datagram = null;
		try {
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
	
	static private boolean SendMTUDirect(InetAddress IPAddress, int port, long pkid, int total, int num, int packetSize, byte[] data) throws IOException {
		Configure conf = Configure.getInstance();
		DataOutputX out = new DataOutputX();
		out.write(NetCafe.CAFE_MTU);
		out.writeInt(conf.getObjHash());
		out.writeLong(pkid);
		out.writeShort(total);
		out.writeShort(num);
		out.writeBlob(data);
				
		return sendUdpDirect(IPAddress, port, out.toByteArray());
	}
}