package scouter.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipFileUtil {
	private static final int FILE_BUFFER_SIZE = 8096;
	
	static public void sendZipFile(ZipOutputStream zos, File file) throws Exception {
		String zipFilename = file.getAbsolutePath();
		int index = zipFilename.lastIndexOf(File.separator);
		if(index >= 0){
			zipFilename = zipFilename.substring(index+1);
		}
			  
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
		byte[] buffer = new byte[FILE_BUFFER_SIZE];
		int readSize = 0;
		CRC32 crc = new CRC32();
		while (0 != (readSize = bis.read(buffer))) {
			if(-1 == readSize) break;
			crc.update(buffer, 0, readSize);
		}
		bis.close();
		
		ZipEntry zipEntry = new ZipEntry(zipFilename);
		zipEntry.setMethod(ZipEntry.STORED);
		zipEntry.setCompressedSize(file.length());
		zipEntry.setSize(file.length());
		zipEntry.setCrc(crc.getValue());
		zos.putNextEntry(zipEntry);
		bis = new BufferedInputStream(new FileInputStream(file));
		while (0 != (readSize = bis.read(buffer))) {
			if(-1 == readSize) break;
			zos.write(buffer, 0, readSize);
		}
		bis.close();
	}
	
	
	static public void recieveZipFile(ZipInputStream zis, String relativePath) throws Exception {
		ZipEntry zipEntry = null;
		  		  
		while(null != (zipEntry = zis.getNextEntry())){
			File outFile = new File(relativePath + "/" + zipEntry.getName());
			
			File parentFolder = outFile.getParentFile();
			if(parentFolder.exists() == false){
				parentFolder.mkdirs();
			}
		   
			BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(outFile));
			int fileLength = (int)zipEntry.getSize();
			int remainLength = fileLength;
			int readSize;
			
			byte[] buffer = new byte[FILE_BUFFER_SIZE];
			readSize = caculReadSize(remainLength);
			while((readSize = zis.read(buffer, 0, readSize)) > 0){
				fos.write(buffer, 0, readSize);
				remainLength -= readSize;
				readSize = caculReadSize(remainLength);
			}
			fos.close();
		}
	}
	
	static private int caculReadSize(int remainLength){
		if(remainLength >= FILE_BUFFER_SIZE){
			return FILE_BUFFER_SIZE;
		}
		return remainLength;
	}
}
