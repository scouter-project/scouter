package scouter.agent.batch;

import java.io.File;
import java.util.Hashtable;

import scouter.Version;
import scouter.agent.batch.netio.data.net.UdpLocalAgent;
import scouter.agent.batch.netio.data.net.UdpLocalServer;
import scouter.agent.batch.netio.request.ReqestHandlingProxy;
import scouter.agent.batch.netio.service.net.TcpRequestMgr;
import scouter.agent.batch.task.StatusSender;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.ObjectPack;
import scouter.util.SysJMX;
import scouter.util.ThreadUtil;
import scouter.util.logo.Logo;

public class Main {
	static public Hashtable<String, MapPack> batchMap = new Hashtable<String, MapPack>();
	
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
		
		StatusSender statusSender = new StatusSender();
		while (true) {
			currentTime = System.currentTimeMillis();
			if((currentTime - startTime) >= 10000){
				UdpLocalAgent.sendUdpPackToServer(getObjectPack());
				startTime = currentTime;
			}
			if (exit.exists() == false) {
				System.exit(0);
			}
			statusSender.sendBatchService();
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
		pack.version = Version.getAgentFullVersion();
		return pack;
	}
}
