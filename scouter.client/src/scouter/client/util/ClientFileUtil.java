package scouter.client.util;

import java.io.*;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 2. 15.
 */
public class ClientFileUtil {
    public static final String GROUP_FILE = "groupfile";
    public static final String XLOG_COLUMN_FILE = "xlogcolumnfile";
    public static final String WORKSPACE_METADATA_DIR = ".metadata";
    public static final String WORKSPACE_LOG_FILE = ".metadata/.log";

    public static boolean saveObjectFile(Object obj, String fileName) {
        File f = new File(RCPUtil.getWorkingDirectory(), fileName);

        try (FileOutputStream fileOut = new FileOutputStream(f);
             ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(obj);
            return true;
        } catch (IOException ie) {
            ie.printStackTrace();
            try {
                f.delete();
            } catch (Exception e) {
            }
        }
        return false;
    }

    public static <T> T readObjectFile(String fileName, Class<T> type) {
        File f = new File(RCPUtil.getWorkingDirectory(), fileName);

        try (FileInputStream fileIn = new FileInputStream(f);
             ObjectInputStream in = new ObjectInputStream(fileIn)) {

            T o = (T) in.readObject();
            return o;

        } catch (Throwable t) {
            try {
                f.delete();
            } catch (Exception e) {
            }
            t.printStackTrace();
        }

        return null;
    }

    public static boolean copy(File sourceLocation, File targetLocation) throws IOException {
        if (sourceLocation.exists() == false) {
            return false;
        }
        if (sourceLocation.isDirectory()) {
            copyDirectory(sourceLocation, targetLocation);
        } else {
            copyFile(sourceLocation, targetLocation);
        }
        return true;
    }

    private static void copyDirectory(File source, File target) throws IOException {
        if (!target.exists()) {
            target.mkdir();
        }

        for (String f : source.list()) {
            copy(new File(source, f), new File(target, f));
        }
    }

    private static void copyFile(File source, File target) throws IOException {
        try (
                InputStream in = new FileInputStream(source);
                OutputStream out = new FileOutputStream(target)
        ) {
            byte[] buf = new byte[1024];
            int length;
            while ((length = in.read(buf)) > 0) {
                out.write(buf, 0, length);
            }
        }
    }

    public static void deleteFile(File source) {
        if (source.exists()) {
            source.delete();
        }
    }

    public static boolean deleteDirectory(File path) {
        if (!path.exists()) {
            return false;
        }
        File[] files = path.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                deleteDirectory(file);
            } else {
                file.delete();
            }
        }
        return path.delete();
    }


}
