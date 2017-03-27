package scouter.client.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 2. 15.
 */
public class ClientFileUtil {
	public static final String GROUP_FILE = "groupfile";
	public static final String XLOG_COLUMN_FILE = "xlogcolumnfile";

	public static boolean saveObjectFile(Object obj, String fileName) {
		File f = new File(RCPUtil.getWorkingDirectory(), fileName);

		try (FileOutputStream fileOut = new FileOutputStream(f);
		     ObjectOutputStream out = new ObjectOutputStream(fileOut)){
			out.writeObject(obj);
			return true;
		} catch(IOException ie) {
			ie.printStackTrace();
			try {f.delete();} catch(Exception e) {}
		}
		return false;
	}

	public static <T> T readObjectFile(String fileName, Class<T> type) {
		File f = new File(RCPUtil.getWorkingDirectory(), fileName);

		try (FileInputStream fileIn = new FileInputStream(f);
		     ObjectInputStream in = new ObjectInputStream(fileIn)){

			T o = (T)in.readObject();
			return o;

		} catch(Throwable t) {
			try {f.delete();} catch(Exception e) {}
			t.printStackTrace();
		}

		return null;
	}
}
