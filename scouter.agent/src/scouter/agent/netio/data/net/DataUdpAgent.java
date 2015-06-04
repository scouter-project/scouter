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

package scouter.agent.netio.data.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.net.NetCafe;
import scouter.util.CastUtil;
import scouter.util.CompareUtil;
import scouter.util.KeyGen;
import scouter.util.ShellArg;
import scouter.util.ThreadUtil;

public class DataUdpAgent {

	private static DataUdpAgent inst;

	InetAddress server_host;
	int server_port;

	String local_addr;
	int local_port;

	private DatagramSocket datagram;

	private DataUdpAgent() {
		setTarget();
		openDatagramSocket();
		Configure.getInstance().addObserver(DataUdpAgent.class.getName(), new Runnable() {
			public void run() {
				setTarget();
				openDatagramSocket();
			}

		});
	}

	private void setTarget() {
		Configure conf = Configure.getInstance();
		String host = conf.server_addr;
		int port = conf.server_port;
		try {
			server_host = InetAddress.getByName(host);
			server_port = port;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void close(DatagramSocket d) {
		if (d != null) {
			try {
				d.close();
			} catch (Exception e) {
			}
		}
	}

	private void openDatagramSocket() {
		try {
			Configure conf = Configure.getInstance();
			String host = conf.local_addr;
			int port = conf.local_port;
			if (datagram == null || CompareUtil.equals(host, local_addr) == false || local_port != port) {
				close(datagram);
				local_addr = host;
				local_port = port;
				if (host != null) {
					datagram = new DatagramSocket(port, InetAddress.getByName(host));
					Logger.println("TA036","Agent UDP local.addr=" + host + " local.port=" + port);
				} else {
					datagram = new DatagramSocket(port);
					Logger.println("TA037","Agent UDP local.port=" + port);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static synchronized DataUdpAgent getInstance() {
		if (inst == null) {
			inst = new DataUdpAgent();
		}
		return inst;
	}

	private Configure conf = Configure.getInstance();
	public boolean write(byte[] p) {
		try {
			if (server_host == null)
				return false;
			
			if(p.length > conf.udp_packet_max){
				return writeMTU(p, conf.udp_packet_max);
			}
			
			DataOutputX out = new DataOutputX();
			out.write(NetCafe.JAVA);
			out.write(p);

			byte[] buff = out.toByteArray();
			DatagramPacket packet = new DatagramPacket(buff, buff.length);
			packet.setAddress(server_host);
			packet.setPort(server_port);
			datagram.send(packet);
			return true;
		} catch (IOException e) {
			Logger.println("TA012", "UDP", e);
			return false;
		}
	}

	private boolean writeMTU(byte[] data, int packetSize) {
		try {
			if (server_host == null)
				return false;

			long pkid = KeyGen.next();
			int total = data.length / packetSize;
			int remainder = data.length % packetSize;
			if (remainder > 0)
				total++;
	
			int num = 0;
			for (num = 0; num < data.length / packetSize; num++) {
                   writeMTU(pkid, total, num, packetSize, DataInputX.get(data, num* packetSize, packetSize));
			}
			if (remainder > 0) {
		         writeMTU(pkid, total, num, remainder, DataInputX.get(data, data.length - remainder, remainder));			
			}
			return true;
		} catch (IOException e) {
			Logger.println("TA014", "UDP", e);
			return false;
		}
	}

	private void writeMTU(long pkid, int total, int num, int packetSize, byte[] data) throws IOException{
		DataOutputX out = new DataOutputX();
		out.write(NetCafe.JMTU);
		out.writeInt(conf.objHash);
		out.writeLong(pkid);
		out.writeShort(total);
		out.writeShort(num);
		out.writeBlob(data);
		byte[] buff = out.toByteArray();
		DatagramPacket packet = new DatagramPacket(buff, buff.length);
		packet.setAddress(server_host);
		packet.setPort(server_port);
		datagram.send(packet);
	}
//	public boolean write(byte[][] p) {
//		try {
//			if (server_host == null)
//				return false;
//
//			DataOutputX out = new DataOutputX();
//			out.write(NetCafe.JAVAN);
//			out.writeShort((short) p.length);
//			for (int i = 0; i < p.length; i++) {
//				out.write(p[i]);
//			}
//
//			byte[] buff = out.toByteArray();
//
//			DatagramPacket packet = new DatagramPacket(buff, buff.length);
//			packet.setAddress(server_host);
//			packet.setPort(server_port);
//			datagram.send(packet);
//			return true;
//		} catch (IOException e) {
//			Logger.println("UDP", e.toString());
//			return false;
//		}
//	}

	public void close() {
		if (datagram != null)
			datagram.close();
		datagram = null;
	}

	public boolean write(List<byte[]> p) {
		try {
			if (server_host == null)
				return false;

			DataOutputX out = new DataOutputX();
			out.write(NetCafe.JAVAN);
			out.writeShort((short) p.size());
			for (int i = 0; i < p.size(); i++) {
				out.write(p.get(i));
			}

			byte[] buff = out.toByteArray();

			DatagramPacket packet = new DatagramPacket(buff, buff.length);
			packet.setAddress(server_host);
			packet.setPort(server_port);
			datagram.send(packet);
			return true;
		} catch (IOException e) {
			Logger.println("TA013", "UDP", e);
			return false;
		}

	}

	public boolean debugWrite(String ip, int port, int length) {
		try {
			DataOutputX out = new DataOutputX();
			out.write("TEST".getBytes());
			if (length > 4) {
				out.write(new byte[length - 4]);
			}
			byte[] buff = out.toByteArray();
			DatagramPacket packet = new DatagramPacket(buff, buff.length);
			packet.setAddress(InetAddress.getByName(ip));
			packet.setPort(port);
			datagram.send(packet);
			Logger.println("TA057", "Sent " + length + " bytes to " + ip + ":" + port);
			return true;
		} catch (IOException e) {
			Logger.println("TA001", "UDP " + e.toString());
			return false;
		}
	}

	public static void main(String[] args) {
		ShellArg param = new ShellArg(args);
		String host = param.get("-h");
		int port = CastUtil.cint(param.get("-p"));
		int length = CastUtil.cint(param.get("-l"));
		if (length == 0) {
			System.out.println("Incorrect args\nex) -h 127.0.0.1 -p 6100 -l 32767");
			return;
		}
		for (int i = 0; i < 100; i++) {
			DataUdpAgent.getInstance().debugWrite(host, port, length);
			ThreadUtil.sleep(2000);
		}
	}
}