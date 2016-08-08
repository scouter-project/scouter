package scouter.server.netio.req.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class TcpAgentReqWorker extends Thread {
	private int objHash;
	private Socket socket;
	private DataInputStream in;
	private DataOutputStream out;
	
	public TcpAgentReqWorker(int objHash, Socket socket){
		this.socket = socket;
		this.objHash = objHash;
		this.setName("SCOUTER-TCP-REQ-" + socket.getRemoteSocketAddress().toString());
		this.setDaemon(true);
	}
	
	@Override
	public void run() {
		int cmd;
		try {
			in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));		
			
			ReqCommand command;
			while (true) {
				cmd = in.readInt();
				command = ReqCommandFactory.makeReqCommand(cmd);
				command.process(in, out);
			}
		}catch(Throwable ex){
			ex.printStackTrace();
		}finally{
			if(socket != null){
				try { socket.close();}catch(Exception ex){}
			}
		}
	}
}
