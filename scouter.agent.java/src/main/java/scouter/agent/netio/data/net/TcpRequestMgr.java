package scouter.agent.netio.data.net;

import java.util.concurrent.Executor;

import scouter.agent.Configure;
import scouter.util.ThreadUtil;

public class TcpRequestMgr extends Thread {

	private static TcpRequestMgr instance;

	public static synchronized TcpRequestMgr getInstance() {
		if (instance == null) {
			instance = new TcpRequestMgr();
			instance.setName("SCOUTER-TCP");
			instance.setDaemon(true);
			instance.start();
		}
		return instance;
	}

	protected Executor pool = ThreadUtil.createExecutor("SCOUTER", 10, 10000, true);

	@Override
	public void run() {

		while (true) {
			int sessionCount = Configure.getInstance().net_collector_tcp_session_count;
			ThreadUtil.sleep(1000);
			try {
				for (int i = 0; i < sessionCount && TcpWorker.LIVE.size() < sessionCount; i++) {
					TcpWorker w = new TcpWorker();
					if (w.prepare()) {
						pool.execute(w);
					} else {
						ThreadUtil.sleep(3000);
					}
				}
				while (TcpWorker.LIVE.size() > sessionCount) {
					TcpWorker w = TcpWorker.LIVE.removeFirst();
					w.close();
				}
			} catch (Throwable t) {
			}
		}
	}
}
