package scouter.agent.batch.netio.data.net;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

import scouter.agent.batch.Configure;
import scouter.agent.batch.Main;
import scouter.io.DataInputX;
import scouter.lang.pack.MapPack;

public class UdpLocalServer extends Thread{
	private static UdpLocalServer instance;

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
	
	private void processRunningInfo(byte [] data){
		DataInputX input = new DataInputX(data);
		try{
			MapPack mapPack = new MapPack();
			mapPack.read(input);
			String key = new StringBuilder(50).append(mapPack.getText("bathJobId")).append('-').append(mapPack.getLong("pID")).append('-').append(mapPack.getLong("startTime")).toString();
			Main.batchMap.put(key, mapPack);
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	private void processEndInfo(byte [] data){
		DataInputX input = new DataInputX(data);
		try{
			String key = new StringBuilder(50).append(input.readText()).append('-').append(input.readInt()).append('-').append(input.readLong()).toString();
			Main.batchMap.remove(key);
		}catch(Exception ex){
			ex.printStackTrace();
		}		
	}
}
