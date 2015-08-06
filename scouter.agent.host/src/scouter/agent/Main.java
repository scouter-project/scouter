package scouter.agent;

import java.io.File;

import scouter.Version;
import scouter.agent.counter.CounterExecutingManager;
import scouter.agent.netio.data.net.TcpRequestMgr;
import scouter.agent.netio.request.ReqestHandlingProxy;
import scouter.util.SysJMX;
import scouter.util.ThreadUtil;
import scouter.util.logo.Logo;

public class Main {
	public static void main(String[] args) {
		Logo.print(true);
		AgentBoot.boot();

		System.out.println("Scouter Host Agent Version " + Version.getServerFullVersion());
		Logger.println("A01", "Scouter Host Agent Version " + Version.getServerFullVersion());
		
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
