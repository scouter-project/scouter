package scouter.agent;

import java.io.File;

import scouter.Version;
import scouter.agent.counter.CounterExecutingManager;
import scouter.agent.netio.request.ReqestHandlingProxy;
import scouter.util.SysJMX;
import scouter.util.ThreadUtil;
import scouter.util.logo.Logo;

public class HostMain {
	public static void main(String[] args) {
		Logo.print(true);
		Logger.println("A01", "Scouter Server Version " + Version.getServerFullVersion());
		CounterExecutingManager.load();
		ReqestHandlingProxy.load();

		Configure.getInstance().printConfig();

		File exit = new File(SysJMX.getProcessPID() + ".scouter");
		try {
			exit.createNewFile();
		} catch (Exception e) {
			String tmp = System.getProperty("user.home", "/tmp");
			exit = new File(tmp, SysJMX.getProcessPID() + ".scouter.run");
			try {
				exit.createNewFile();
			} catch (Exception k) {
				System.exit(1);
			}
		}
		exit.deleteOnExit();
		System.out.println("System JRE version : " + System.getProperty("java.version"));
		while (true) {
			if (exit.exists() == false) {
				System.exit(0);
			}
			ThreadUtil.sleep(1000);
		}

	}

}
