package scouter.agent.netio.data.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import scouter.agent.Configure;
import scouter.agent.netio.request.ReqestHandlingProxy;
import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.lang.pack.Pack;
import scouter.net.TcpFlag;
import scouter.util.FileUtil;
import scouter.util.ThreadUtil;

public class TcpRequestMgr extends Thread {

	private static TcpRequestMgr instance;

	public synchronized TcpRequestMgr getInstance() {
		if (instance == null) {
			instance = new TcpRequestMgr();
			instance.setName("SCOUTER-TCP");
			instance.setDaemon(true);
			instance.start();
		}
		return instance;
	}

	Executor pool = Executors.newFixedThreadPool(10);
	public int CNT = 2;

	@Override
	public void run() {
		while (true) {
			ThreadUtil.sleep(1000);
			try {
				for (int i = 0; i <CNT && TcpWorker.ACTIVE.intValue() < CNT; i++) {
					TcpWorker w = new TcpWorker();
					if (w.prepare()) {
						pool.execute(w);
					} else {
						ThreadUtil.sleep(5000);
					}
				}
			} catch (Throwable t) {
			}
		}
	}
}
