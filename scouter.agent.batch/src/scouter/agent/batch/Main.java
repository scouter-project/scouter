package scouter.agent.batch;

import java.io.File;

import scouter.Version;
import scouter.agent.batch.netio.data.net.UdpAgent;
import scouter.agent.batch.netio.data.net.UdpLocalServer;
import scouter.agent.batch.netio.request.ReqestHandlingProxy;
import scouter.agent.batch.netio.service.net.TcpRequestMgr;
import scouter.lang.pack.ObjectPack;
import scouter.util.SysJMX;
import scouter.util.ThreadUtil;
import scouter.util.logo.Logo;

public class Main {
	public static void main(String[] args) {		
		Logo.print(true);
		System.out.println("Scouter Batch Agent Version " + Version.getServerFullVersion());
		Logger.println("A01", "Scouter Batch Agent Version " + Version.getServerFullVersion());
		
		ReqestHandlingProxy.load(ReqestHandlingProxy.class);
		UdpLocalServer.getInstance();
		TcpRequestMgr.getInstance();
		
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
		long startTime = System.currentTimeMillis();
		long currentTime;
		while (true) {
			currentTime = System.currentTimeMillis();
			if((currentTime - startTime) >= 10000){
				UdpAgent.sendUdpPack(getObjectPack());
				startTime = currentTime;
			}
			if (exit.exists() == false) {
				System.exit(0);
			}
			ThreadUtil.sleep(1000);
			
		}

	}
	
	static public ObjectPack getObjectPack(){
		Configure conf = Configure.getInstance();
		ObjectPack pack = new ObjectPack();
		pack.alive = true;
		pack.objHash = conf.getObjHash();
		pack.objName = conf.getObjName();
		pack.objType = conf.obj_type;
		return pack;
	}
}
