package scouter.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class DataUtil {
	static public int readInt(InputStream in) throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));		
	}
	
    static public void readFully(InputStream in, byte buff[]) throws IOException {
        readFully(in, buff, 0, buff.length);
    }

    static public void readFully(InputStream in, byte buff[], int offset, int length) throws IOException {
        if (length < 0)
            throw new IndexOutOfBoundsException();
        int n = 0;
        while (n < length) {
            int count = in.read(buff, offset + n, length - n);
            if (count < 0)
                throw new EOFException();
            n += count;
        }
    }
    
    static public int readUnsignedByte(InputStream in) throws IOException {
        int ch = in.read();
        if (ch < 0)
            throw new EOFException();
        return ch;
    }
    
    static public int readUnsignedShort(InputStream in) throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        if ((ch1 | ch2) < 0)
            throw new EOFException();
        return (ch1 << 8) + (ch2 << 0);
    }  
    
    static public long readLong(InputStream in) throws IOException {
    	byte [] buffer = new byte[8];
        readFully(in, buffer, 0, 8);
        return (((long)buffer[0] << 56) +
                ((long)(buffer[1] & 255) << 48) +
                ((long)(buffer[2] & 255) << 40) +
                ((long)(buffer[3] & 255) << 32) +
                ((long)(buffer[4] & 255) << 24) +
                ((buffer[5] & 255) << 16) +
                ((buffer[6] & 255) <<  8) +
                ((buffer[7] & 255) <<  0));
    } 
    
	static public byte[] readBlob(InputStream in) throws IOException {
		int baselen = readUnsignedByte(in);
		switch (baselen) {
		case 255: {
			int len = readUnsignedShort(in);
			byte[] buffer = read(in, len);
			return buffer;
		}
		case 254: {
			int len = readInt(in);
			byte[] buffer = read(in, len);
			return buffer;
		}
		case 0: {
			return new byte[0];
		}
		default:
			byte[] buffer = read(in, baselen);
			return buffer;
		}
	} 
	
	static public byte[] read(InputStream in, int len) throws IOException {
		byte[] buff = new byte[len];
		readFully(in, buff);
		return buff;
	}
	
	static public String readText(InputStream in) throws IOException {
		byte[] buffer = readBlob(in);
		return new String(buffer, "UTF8");
	}	
}
