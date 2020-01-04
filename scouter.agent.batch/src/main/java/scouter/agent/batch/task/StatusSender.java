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
package scouter.agent.batch.task;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import scouter.agent.batch.Configure;
import scouter.agent.batch.Logger;
import scouter.agent.batch.Main;
import scouter.agent.batch.netio.data.net.UdpAgent;
import scouter.agent.batch.netio.data.net.UdpLocalServer;
import scouter.agent.batch.counter.CounterBasket;
import scouter.io.DataOutputX;
import scouter.lang.TimeTypeEnum;
import scouter.lang.counters.CounterConstants;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.PerfCounterPack;
import scouter.lang.value.DecimalValue;
import scouter.net.NetCafe;

public class StatusSender {
	private Configure conf;
	private InetAddress server;
    private CounterBasket cb;
    private long lastCheckTime = 0L;
    
	public StatusSender(){
		conf =  Configure.getInstance();
		cb = new CounterBasket();
		try {
			server =  InetAddress.getByName(conf.net_collector_ip);
		}catch(Exception ex){
			ex.printStackTrace();
			server = null;
		}
	}
	
	public void sendBatchService(long currentTime){
		if(server == null){
			return;
		}
		
		checkBatchService(currentTime);
		updateBatchService();
		PerfCounterPack[] pks = cb.getList();
		sendCounter(pks);
	}
	
	private void updateBatchService(){
		PerfCounterPack pack = cb.getPack(conf.getObjName(), TimeTypeEnum.REALTIME);
		UdpLocalServer localServer = UdpLocalServer.getInstance();
		pack.put(CounterConstants.BATCH_SERVICE, new DecimalValue(Main.batchMap.size()));
		pack.put(CounterConstants.BATCH_START, new DecimalValue(localServer.getStartBatchs()));
		pack.put(CounterConstants.BATCH_END, new DecimalValue(localServer.getEndBatchs()));
		pack.put(CounterConstants.BATCH_ENDNOSIGNAL, new DecimalValue(localServer.getEndNoSignalBatchs()));
	}
	
	private void checkBatchService(long currentTime){
		boolean isCheck = false;

		if((currentTime - lastCheckTime) >= conf.sfa_dump_interval_ms){
			isCheck = true;
			lastCheckTime = currentTime;
		}
		if(!isCheck){
			return;
		}
		try{
			MapPack map;
			long stdTime = conf.sfa_dump_interval_ms * 5;
			long gapTime;
			for(String key : Main.batchMap.keySet()){
				map = Main.batchMap.get(key);
				gapTime = currentTime - (map.getLong("startTime") + map.getLong("elapsedTime"));
				if(gapTime >= stdTime){
					Main.batchMap.remove(key);
					UdpLocalServer.getInstance().addEndNoSignalBatchs();
					Logger.println("remove " + key + " in batchMap");
				}
			}
		}catch(Throwable th){
			Logger.println("E002","Exception to check batchMap", th);
		}
	}
	
	private void sendCounter(PerfCounterPack[] p) {
		try {
			List<byte[]> buff = new ArrayList<byte[]>();
			int bytes = 0;
			for (int k = 0; k < p.length; k++) {
				byte[] b = new DataOutputX().writePack(p[k]).toByteArray();
				if (bytes + b.length >= conf.net_udp_packet_max_bytes) {
					sendDirect(buff); // buff.size媛� 0�씪�닔�룄 �엳�떎.
					bytes = 0;// bytes 媛� 珥덇린�솕..
					buff.clear();
				}
				bytes += b.length;
				buff.add(b);
			}
			sendDirect(buff);
		} catch (Exception e) {
		}
	}
	
	private void sendDirect(List<byte[]> buff) {
		switch (buff.size()) {
		case 0:  return;
		case 1:
			write(buff.get(0));
			break;
		default:
			write(buff);
			break;
		}
	}

	private boolean write(byte[] p) {
		try {
			if (p.length > conf.net_udp_packet_max_bytes) {
				return UdpAgent.sendMTU(server, conf.net_collector_udp_port, p, conf.net_udp_packet_max_bytes);
			}
			DataOutputX out = new DataOutputX();
			out.write(NetCafe.CAFE);
			out.write(p);
			
			UdpAgent.sendUdp(server, conf.net_collector_udp_port, out.toByteArray());
			return true;
		} catch (IOException e) {
			Logger.println("A120", "UDP", e);
			return false;
		}
	}
	
	private boolean write(List<byte[]> p) {
		try {
			DataOutputX buffer = new DataOutputX();
			int bufferCount = 0;
			for (int i = 0; i < p.size(); i++) {
				byte[] b = p.get(i);
				if (b.length > conf.net_udp_packet_max_bytes) {
					UdpAgent.sendMTU(server, conf.net_collector_udp_port, b, conf.net_udp_packet_max_bytes);
				} else if (b.length + buffer.getWriteSize() > conf.net_udp_packet_max_bytes) {
					sendList(bufferCount, buffer.toByteArray());
					buffer = new DataOutputX();
					bufferCount = 0;
				} else {
					bufferCount++;
					buffer.write(b);
				}
			}
			if (buffer.getWriteSize() > 0) {
				sendList(bufferCount, buffer.toByteArray());
			}
			return true;
		} catch (IOException e) {
			Logger.println("A123", "UDP", e);
			return false;
		}
	}
	
	private void sendList(int bufferCount, byte[] buffer) throws IOException {
		DataOutputX out = new DataOutputX();
		out.write(NetCafe.CAFE_N);
		out.writeShort(bufferCount);
		out.write(buffer);
		
		UdpAgent.sendUdp(server, conf.net_collector_udp_port, out.toByteArray());
	}
}
