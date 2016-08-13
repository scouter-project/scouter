package scouter.server.db;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import scouter.util.DateUtil;

public class BatchZipDB {
	static public void write(long time, String objName, String filename, long fileSize, InputStream in) throws IOException{
		String path  = getDBPath(time, objName);
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
        buffer.append("/").append(DateUtil.yyyymmdd(time)).append(objName);
		return buffer.toString();
	}
}
