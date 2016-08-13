package scouter.server.netio.req.net;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import scouter.util.DataUtil;

public class TcpAgentReqWorker extends Thread {
	private int objHash;
	private Socket socket;
	private InputStream in;
	private OutputStream out;
	
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
			socket.setSoTimeout(0);
			in = socket.getInputStream();
			out = socket.getOutputStream();		
						
			ReqCommand command;
			while (true) {
				cmd = DataUtil.readInt(in);
System.out.println("CMD==>" + cmd);					
				command = ReqCommandFactory.makeReqCommand(cmd);
				if(command == null){
					break;
				}
				command.process(in, out);
			}
		}catch(Throwable ex){
			ex.printStackTrace();
		}finally{
			if(in != null){	try { in.close();}catch(Exception ex){} }
			if(out != null){ try { out.close();}catch(Exception ex){}}
			if(socket != null){ try { socket.close();}catch(Exception ex){} }
		}
	}
}
