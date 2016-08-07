package scouter.agent.batch.netio.req.net;

import java.io.File;
import java.net.Socket;
import java.util.zip.ZipOutputStream;

import scouter.util.ZipFileUtil;

public class TCPStackZipSender {
	private Socket socket;
	private String filename;
	public TCPStackZipSender(Socket socket, String filename){
		this.socket = socket;
		this.filename = filename;
	}
	
	public void process(){
		ZipOutputStream zos = null;
		File file;
		try {
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
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			if(zos != null){
				try{ zos.close(); }catch(Exception ex){}
			}
		}
	}
}
