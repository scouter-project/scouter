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

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import scouter.agent.batch.Configure;
import scouter.agent.batch.Main;
import scouter.io.DataInputX;
import scouter.lang.pack.MapPack;

public class UdpLocalServer extends Thread{
	private static UdpLocalServer instance;

	private int days = 0;
	private int startBatchs = 0;
	private int endBatchs = 0;
	private int endNoSignalBatchs = 0;
	
	public static synchronized UdpLocalServer getInstance() {
		if (instance == null) {
			instance = new UdpLocalServer();
			instance.setName("SCOUTER-UDP");
			instance.setDaemon(true);
			instance.start();
		}
		return instance;
	}
	
	public void run(){
		Configure conf = Configure.getInstance();
		byte[] receiveData = new byte[300000];
		DatagramSocket serverSocket = null;
		try {
	        int flag;
	        int size;
			serverSocket = new DatagramSocket(conf.net_local_udp_port);
	        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
	        while(true){
	        	serverSocket.receive(receivePacket);
				size = receivePacket.getLength() - 4;
	        	byte [] data = new byte[size];
				System.arraycopy(receivePacket.getData(), 4, data, 0, size);
				
				flag = DataInputX.toInt(receiveData, 0);
				switch(flag){
				case BatchNetFlag.BATCH_END_DUMPFILE_INFO:
		        	TcpAgentReqMgr.getInstance().addJob(data);
		        	break;
				case BatchNetFlag.BATCH_RUNNING_INFO:
					processRunningInfo(data);
					break;
				case BatchNetFlag.BATCH_END_INFO:
					processEndInfo(data);
					break;
				}
	        }
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			if(serverSocket !=null){
				try {serverSocket.close();}catch(Exception ex){}
			}
		}
	}
	
	public int getStartBatchs(){
		return startBatchs;
	}
	
	public int getEndBatchs(){
		return endBatchs;
	}
	
	public int getEndNoSignalBatchs(){
		return endNoSignalBatchs;
	}
	
	public void addEndNoSignalBatchs(){
		checkDays();
		endNoSignalBatchs++;
	}
	
	private void checkDays(){
		int currentDays = (int)(System.currentTimeMillis() / 86400000L);
		if(currentDays != days){
			startBatchs = endBatchs = endNoSignalBatchs = 0;
			days = currentDays;
		}
	}
	
	private void processRunningInfo(byte [] data){
		DataInputX input = new DataInputX(data);
		try{
			MapPack mapPack = new MapPack();
			mapPack.read(input);
			String key = new StringBuilder(50).append(mapPack.getText("batchJobId")).append('-').append(mapPack.getLong("pID")).append('-').append(mapPack.getLong("startTime")).toString();
			checkDays();
			if(!Main.batchMap.containsKey(key)){
				startBatchs++;
			}
			Main.batchMap.put(key, mapPack);
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	private void processEndInfo(byte [] data){
		DataInputX input = new DataInputX(data);
		try{
			String key = new StringBuilder(50).append(input.readText()).append('-').append(input.readInt()).append('-').append(input.readLong()).toString();
			checkDays();
			endBatchs++;
			Main.batchMap.remove(key);
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
}
