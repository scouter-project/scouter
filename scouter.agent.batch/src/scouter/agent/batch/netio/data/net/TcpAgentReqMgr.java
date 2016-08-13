package scouter.agent.batch.netio.data.net;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

import scouter.agent.batch.Configure;
import scouter.util.ThreadUtil;

public class TcpAgentReqMgr extends Thread{
	private static ConcurrentLinkedQueue<byte []> queue = new ConcurrentLinkedQueue<byte []>();
	
	private static TcpAgentReqMgr instance;

	public static synchronized TcpAgentReqMgr getInstance() {
		if (instance == null) {
			instance = new TcpAgentReqMgr();
			instance.setName("SCOUTER-TCP-REQ-MGR");
			instance.setDaemon(true);
			instance.start();
		}
		return instance;
	}

	protected Executor pool = ThreadUtil.createExecutor("SCOUTER-REQ-STACK", Configure.getInstance().net_tcp_stack_session_count, 10000, true);

	@Override
	public void run() {
		while (true) {
System.out.println("Thread Cnt:" + TCPStackZipWorker.LIVE.size());			
			int sessionCount = Configure.getInstance().net_tcp_stack_session_count;
			ThreadUtil.sleep(1000);
			try {
				for (int i = 0; i < sessionCount && TCPStackZipWorker.LIVE.size() < sessionCount; i++) {
					TCPStackZipWorker w = new TCPStackZipWorker(this);
					if (w.prepare(false)) {
						pool.execute(w);
					} else {
						ThreadUtil.sleep(3000);
					}
				}
				while (TCPStackZipWorker.LIVE.size() > sessionCount) {
					TCPStackZipWorker w = TCPStackZipWorker.LIVE.removeFirst();
					w.close(true);
				}
			} catch (Throwable t) {
			}
		}
	}
	
	public void addJob(byte [] job){
		queue.add(job);
		System.out.println("AddJob");		
		synchronized(queue){
			queue.notify();
		}
	}
	
	public byte [] getJob(){
		byte [] job = null;
		while((job = queue.poll()) == null){
			try{ 
				synchronized(queue){
					queue.wait(3000); 
				}
			}catch(Exception ex){};
		}
System.out.println("GetJob->" + job.length);	
		return job;
	}
}