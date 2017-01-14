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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.zip.ZipOutputStream;

import scouter.agent.batch.Configure;
import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.net.NetCafe;
import scouter.net.TcpFlag;
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
	
	public boolean prepare(boolean reConnect) {
		if(reConnect){
			close(false);
		}
		
		Configure conf = Configure.getInstance();
		String host = conf.net_collector_ip;
		int port = conf.net_collector_tcp_port;
		int so_timeout = conf.net_collector_tcp_so_timeout_ms;
		int connection_timeout = conf.net_collector_tcp_connection_timeout_ms;

		socket = new Socket();
		try {
			socket.connect(new InetSocketAddress(host, port), connection_timeout);
			socket.setSoTimeout(so_timeout);
			
			if(!reConnect){
				LIVE.put(this.hashCode(), this);
			}
			
			in = new DataInputX(new BufferedInputStream(socket.getInputStream()));
			out = new DataOutputX(new BufferedOutputStream(socket.getOutputStream()));
			out.writeInt(NetCafe.TCP_AGENT_REQ);
			out.writeInt(objHash);
			out.flush();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public void run() {
		if (socket == null)
			return;
		byte [] job = null;
		try {
			while((job = tcpAgentReqMgr.getJob()) != null){
				process(job);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			prepare(true);
		}
		//System.out.println("TcpStackSzipWorker close: " + this.hashCode());		
		LIVE.remove(this.hashCode());
	}
	
	public void process(byte [] job) throws Exception{
		long startTime = 0L;
		String objName = null;
		String filename = null;
	
        DataInputX reader = new DataInputX(job);
		startTime = reader.readLong();
		objName = reader.readText();
		filename = reader.readText();
		//System.out.println("==>" + startTime + " - " + objName + " : " + filename);
		
		if(startTime == 0L || filename == null){
			return;
		}
		
		File [] files = null;
		boolean isSuccess = false;
		BufferedInputStream bin = null;
		try {
			files = makeZipFile(filename);
			long fileSize = files[0].length();
			
			out.writeInt(NetCafe.TCP_SEND_STACK);
			out.writeLong(startTime);
			out.writeText(objName);
			
			int index = filename.lastIndexOf(File.separator);
			String pureName;
			if(index >= 0){
				pureName = filename.substring(index + 1);
			}else{
				pureName = filename;
			}
			out.writeText(pureName);
			out.writeLong(fileSize);
			out.flush();
			
			bin = new BufferedInputStream(new FileInputStream(files[0]));
			byte [] buffer = new byte[4096];
			OutputStream os = socket.getOutputStream();
			while((index = bin.read(buffer)) != -1){
				os.write(buffer, 0, index);
			}
			os.flush();
			bin.close();
			bin = null;
			
			if(in.readByte() == TcpFlag.OK){
				isSuccess = true;
			}
		}finally{
			if(bin != null){
				try { bin.close(); }catch(Exception ex){}
			}
		}
		
		if(!isSuccess){
			//System.out.println("Send Fail!!!");			
			return;
		}
		//System.out.println("Send Success!!!");

		try {
			for(File file : files){
				if(!file.delete()){
					TcpAgentReqMgr.getInstance().addFile(file);
				}
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
		
	public void close(boolean remove) {
		FileUtil.close(in);
		FileUtil.close(out);
		FileUtil.close(socket);
		socket = null;
		if(remove){
			LIVE.remove(this.hashCode());
		}
	}
	
	public File [] makeZipFile(String filename ){
		File indexFile = null;
		File stackFile = null;
		ZipOutputStream zos = null;
		File zipFile = new File(filename + ".zip");
		
		try {
			zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));
			zos.setLevel(9);
			indexFile = new File(filename + ".inx"); // Index file
			if(!indexFile.exists()){
				return null;
			}
			ZipFileUtil.sendZipFile(zos, indexFile);
			stackFile = new File(filename + ".log"); // stack log file
			if(!stackFile.exists()){
				return null;
			}
			ZipFileUtil.sendZipFile(zos, stackFile);
			zos.flush();
		}catch(Exception ex){
			ex.printStackTrace();
			return null;
		}finally{
			if(zos != null){
				try{ zos.close(); }catch(Exception ex){}
			}
		}
		File [] files = new File[3];
		files[0] = zipFile;
		files[1] = indexFile;
		files[2] = stackFile;
		return files;
	}
}
