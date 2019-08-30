package scouter.agent.batch.netio.mtu;

import java.io.IOException;
import java.net.InetAddress;

import scouter.agent.batch.Configure;
import scouter.agent.batch.Logger;
import scouter.util.LongEnumer;
import scouter.util.LongKeyLinkedMap;
import scouter.util.ThreadUtil;

public class MultiPacketProcessor extends Thread{
	private static MultiPacketProcessor instance = null;

    static final int MAX_COUNT = 1000;
    private LongKeyLinkedMap<MultiPacket> buffer = new LongKeyLinkedMap<MultiPacket>();
	
	static public MultiPacketProcessor getInstance(){
		if(instance == null){
			instance = new MultiPacketProcessor();
            instance.setDaemon(true);
            instance.setName(ThreadUtil.getName(instance));
            
            instance.start();
 		}
		return instance;
	}
	
	private MultiPacketProcessor(){
		buffer.setMax(MAX_COUNT);
	}
	
	public void run() {
        while (true) {
            ThreadUtil.sleep(1000);
            try {
	            if (buffer.size() > 0) {
	                    checkExpired();
	            }
            } catch(Exception ex) {
            	ex.printStackTrace();
            }
        }
	}
	
    public byte[] add(long pkid, int total, int num, byte [] data, int objHash, InetAddress addr) throws IOException{
    	MultiPacket p;
    	synchronized(buffer){
    		p = buffer.get(pkid);
            if (p == null) {
                    p = new MultiPacket(total, objHash, addr);
                    buffer.put(pkid, p);
            }
        }

        p.set(num, data);
        if (p.isDone()) {
        	buffer.remove(pkid);
            return p.toBytes();
        }
        return null;
    }
    
    private void checkExpired() {
        LongEnumer en = buffer.keys();
        long key;
        MultiPacket p;
        while (en.hasMoreElements() == true) {
            key = en.nextLong();
            p = buffer.get(key);
            if (p.isExpired()) {
                buffer.remove(key);
//                if (Configure.getInstance().log_expired_multipacket) {
//                    Logger.println("S150", p.toString());
//                }
            }
        }
    }	

}
