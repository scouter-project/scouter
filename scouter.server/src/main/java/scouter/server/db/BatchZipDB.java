package scouter.server.db;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import scouter.io.DataOutputX;
import scouter.net.TcpFlag;
import scouter.server.Configure;
import scouter.server.Logger;
import scouter.util.DateUtil;

public class BatchZipDB {
	static public void write(long time, String objName, String filename, long fileSize, InputStream in) throws IOException{
		String path  = getDBPath(time, objName);
        if (Configure.getInstance().log_udp_batch) {
            Logger.println(new StringBuilder(100).append("Batch stack path: ").append(path).toString());
        }		
        File f = new File(path);
        if (!f.exists()) {
            f.mkdirs();
        }		
        if (!f.exists()) {
        	throw new IOException("can't create path:" + path);
        }

        BufferedOutputStream out = null;
        try {
        	out = new BufferedOutputStream(new FileOutputStream(new File(new StringBuilder(100).append(path).append('/').append(filename).append(".zip").toString())));
        	int totalSize = 0;
        	int readSize;
        	byte [] buffer = new byte[1024];
        	while((readSize = in.read(buffer)) != -1){
        		out.write(buffer, 0, readSize);
        		totalSize += readSize;
        		if(totalSize == fileSize){
        			break;
        		}
        	}
        }finally{
        	if(out != null){
        		try{out.close(); }catch(Exception ex){}
        	}
        }
    }
	
	static public String getDBPath(long time, String objName){
		StringBuilder buffer = new StringBuilder();
		buffer.append(DBCtr.getRootPath());
        buffer.append('/').append(DateUtil.yyyymmdd(time)).append('/').append(objName).append('/').append(DateUtil.getHour(time));
		return buffer.toString();
	}
	
	static public void read(String objName, long time, String filename, DataOutputX dout) throws IOException {
		String path = getDBPath(time, objName);
        File f = new File(path);
        if (!f.exists()) {
        	return;
        }

        BufferedInputStream in = null;
        try {
        	File file = new File(new StringBuilder(100).append(path).append('/').append(filename).toString());
        	int fileSize = (int)file.length();
        	int readSize;
        	int totalSize = 0;
        	byte [] buffer = new byte[120000];
        	
        	in = new BufferedInputStream(new FileInputStream(file));
        	while((readSize = in.read(buffer)) != -1){
        		if(readSize > 0){
        			writeBytes(dout, buffer, readSize);
              		totalSize += readSize;      			
        		}
        		if(totalSize >= fileSize){
        			break;
        		}
        	}
        }finally{
        	if(in != null){
        		try{in.close(); }catch(Exception ex){}
        	}
        }
	}
	
	static private void writeBytes(DataOutputX dout, byte [] data, int size) throws IOException{
		dout.writeByte(TcpFlag.HasNEXT);
		dout.writeBlob(data, 0, size);
	}
}
