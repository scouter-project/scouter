package scouter.agent.batch.netio.data.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.zip.ZipOutputStream;

import scouter.agent.batch.Configure;
import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.net.NetCafe;
import scouter.util.FileUtil;
import scouter.util.IntKeyLinkedMap;
import scouter.util.ZipFileUtil;

public class TCPStackZipWorker implements Runnable {
	private Socket socket;
	private DataInputX in;
	private DataOutputX out;
	private int objHash;
	private TcpAgentReqMgr tcpAgentReqMgr;
	
	public static IntKeyLinkedMap<TCPStackZipWorker> LIVE = new IntKeyLinkedMap<TCPStackZipWorker>();
	
	public TCPStackZipWorker(TcpAgentReqMgr tcpAgentReqMgr){
		this.tcpAgentReqMgr = tcpAgentReqMgr;
		this.objHash = Configure.getInstance().getObjHash();
	}
	
	public boolean prepare() {
		Configure conf = Configure.getInstance();
		String host = conf.net_collector_ip;
		int port = conf.net_collector_tcp_port;
		int so_timeout = conf.net_collector_tcp_so_timeout_ms;
		int connection_timeout = conf.net_collector_tcp_connection_timeout_ms;

		socket = new Socket();
		try {
			socket.connect(new InetSocketAddress(host, port), connection_timeout);
			socket.setSoTimeout(so_timeout);
			LIVE.put(this.hashCode(), this);
		
			in = new DataInputX(new BufferedInputStream(socket.getInputStream()));
			out = new DataOutputX(new BufferedOutputStream(socket.getOutputStream()));
			out.writeInt(NetCafe.TCP_AGENT_REQ);
			out.writeInt(objHash);
			out.flush();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	public void run() {
		if (socket == null)
			return;
		try {
			String job;
			while((job = tcpAgentReqMgr.getJob()) != null){
				process(job);
			}
		} catch (Throwable t) {
		} finally {
			close();
		}
	}
	
	public void process(String job){
		long startTime = 0L;
		String filename = null;
		try {
			int index = job.indexOf(" ");
			startTime = Long.parseLong(job.substring(0, index));
			filename = job.substring(index+1);
		}catch(Throwable ex){
			ex.printStackTrace();
		}
		
		if(startTime == 0L || filename == null){
			return;
		}
System.out.println("Job=>" + startTime + " : " + filename);			
		ZipOutputStream zos = null;
		File file;
		try {
			out.writeInt(NetCafe.TCP_SEND_STACK);
			out.writeLong(startTime);
			out.writeText(filename);
			
			zos = new ZipOutputStream(socket.getOutputStream());
			file = new File(filename + ".inx"); // Index file
			if(!file.exists()){
				return;
			}
			ZipFileUtil.sendZipFile(zos, file);
			file = new File(filename + ".log"); // stack log file
			if(!file.exists()){
				return;
			}
			ZipFileUtil.sendZipFile(zos, file);
			zos.flush();
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			if(zos != null){
				try{ zos.close(); }catch(Exception ex){}
			}
		}
	}
	
	public void close() {
		FileUtil.close(in);
		FileUtil.close(out);
		FileUtil.close(socket);
		socket = null;
		LIVE.remove(this.hashCode());
	}	
}
