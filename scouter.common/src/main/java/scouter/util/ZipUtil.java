package scouter.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtil {
	static public byte [] compress(byte [] data){
		if(data == null){
			return null;
		}
		ZipOutputStream zipOut = null;
		ByteArrayOutputStream byteOut = null;
		try {
			byteOut = new ByteArrayOutputStream(); 
			zipOut = new ZipOutputStream(byteOut);
			ZipEntry entry = new ZipEntry("B");
			zipOut.putNextEntry(entry);
			zipOut.flush();
		}catch(IOException ex){
			return data;
		}finally {
			if(zipOut != null){
				try { zipOut.closeEntry(); }catch(Exception ex){}
				try { zipOut.close(); }catch(Exception ex){}
			}
		}
		return byteOut.toByteArray();
	}
	
	static public byte [] decompress(byte [] data){
		if(data == null || data.length < 4){
			return data;
		}
		
		if(data[0] != 80 || data[1] != 75 || data[2] != 3 || data[3] != 4){ //check Zip Header
			return data;
		}
		
		ZipInputStream zipIn = null;
		ByteArrayOutputStream byteOut = null;
		try{
			zipIn = new ZipInputStream(new ByteArrayInputStream(data));
			ZipEntry entry = zipIn.getNextEntry();
			if(entry == null){
				return data;
			}
			byteOut = new ByteArrayOutputStream();
			int value;
			while((value = zipIn.read()) != -1){
				byteOut.write(value);
			}
		}catch(IOException ex){
			return data;
		}finally{
			if(zipIn != null){
				try { zipIn.closeEntry(); }catch(Exception ex){}
				try { zipIn.close(); }catch(Exception ex){}				
			}
		}
		
		return byteOut.toByteArray();
	}
}
