package scouter.agent.batch.task;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import scouter.agent.batch.Configure;
import scouter.agent.batch.Logger;
import scouter.agent.batch.Main;
import scouter.agent.batch.netio.data.net.UdpAgent;
import scouter.agent.counter.CounterBasket;
import scouter.agent.netio.data.DataProxy;
import scouter.io.DataOutputX;
import scouter.lang.TimeTypeEnum;
import scouter.lang.counters.CounterConstants;
import scouter.lang.pack.PerfCounterPack;
import scouter.lang.value.DecimalValue;
import scouter.net.NetCafe;

public class StatusSender {
	private Configure conf;
	private InetAddress server;
    private CounterBasket cb;
    
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
	
	public void sendBatchService(){
		if(server == null){
			return;
		}
		updateBatchService();
		PerfCounterPack[] pks = cb.getList();
		sendCounter(pks);
	}
	
	private void updateBatchService(){
		PerfCounterPack pack = cb.getPack(conf.getObjName(), TimeTypeEnum.REALTIME);
		pack.put(CounterConstants.BATCH_SERVICE, new DecimalValue(Main.batchMap.size()));
	}
	
	private void sendCounter(PerfCounterPack[] p) {
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
