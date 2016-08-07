package scouter.server.netio.req.net;

import java.net.Socket;

public class TcpAgentReqWorker extends Thread {
	private int objHash;
	private Socket socket;
	public TcpAgentReqWorker(int objHash, Socket socket){
		this.socket = socket;
		this.objHash = objHash;
		this.setName("SCOUTER-TCP-REQ-" + socket.getRemoteSocketAddress().toString());
		this.setDaemon(true);		
	}
	
	@Override
	public void run() {
		try {
			while (true) {
		
			}
		}catch(Throwable ex){
			
		}finally{
			if(socket != null){
				try { socket.close();}catch(Exception ex){}
			}
		}
	}
}
