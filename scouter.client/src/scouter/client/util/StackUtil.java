package scouter.client.util;

import java.io.File;

public class StackUtil {
	static public String getStackWorkspaceDir(String objName){
		File workDir = RCPUtil.getWorkingDirectory();
		String dirPath = workDir.getAbsolutePath() + objName + "/stack/";
		File dir = new File(dirPath);
		if (dir.exists() == false) {
			dir.mkdirs();
		}
		return dirPath;
	}
}
