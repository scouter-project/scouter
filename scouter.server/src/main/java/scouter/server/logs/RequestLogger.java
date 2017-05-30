/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 */

package scouter.server.logs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;

import scouter.server.Configure;
import scouter.server.LoginManager;
import scouter.server.LoginUser;
import scouter.net.RequestCmd;
import scouter.util.DateUtil;
import scouter.util.FileUtil;
import scouter.util.RequestQueue;
import scouter.util.ThreadUtil;

public class RequestLogger extends Thread {

	private static final String FILE_PREFIX = "request";
	private static final String DIRECTORY = Configure.getInstance().log_dir;
	private static RequestLogger instance;
	public static Set<String> cmdSet = new HashSet<String>();
	public static Set<String> demandSet = new HashSet<String>();

	static {
		cmdSet.add(RequestCmd.SERVER_DB_DELETE);
		cmdSet.add(RequestCmd.REMOTE_CONTROL);
		cmdSet.add(RequestCmd.REMOTE_CONTROL_ALL);
		cmdSet.add(RequestCmd.TRANX_LOAD_TIME_GROUP);
		demandSet.add(RequestCmd.ACTIVE_QUERY_LIST);
	}

	public static synchronized RequestLogger getInstance() {
		if (instance == null) {
			instance = new RequestLogger();
			instance.setDaemon(true);
			instance.setName(ThreadUtil.getName(instance));
			instance.start();
		}
		return instance;
	}

	private RequestQueue<RequestInfo> queue = new RequestQueue<RequestInfo>(1000);

	public boolean add(String cmd, long session) {
		if (cmdSet.contains(cmd)) {
			LoginUser loginUser = LoginManager.getUser(session);
			if (loginUser != null) {
				queue.put(new RequestInfo(System.currentTimeMillis(), loginUser.id(), loginUser.ip(), cmd));
				return true;
			} else {
				queue.put(new RequestInfo(System.currentTimeMillis(), "unknown" + session, "", cmd));
				return false;
			}
		}
		return false;
	}

	boolean running = true;

	public void run() {
		while (running) {
			RequestInfo r = queue.get();
			BufferedWriter bw = null;
			try {
				File file = getFile();

				bw = new BufferedWriter(new FileWriter(file, true));
				bw.write(r.toString());
				bw.newLine();
				// bw.flush();
			} catch (Throwable e) {
				outFile=null;
				e.printStackTrace();
			} finally {
				FileUtil.close(bw);
			}
		}
	}

	private long dateUnit;
	private File outFile;

	private File getFile() {
		if (outFile == null || dateUnit != DateUtil.getDateUnit()) {
			String filename = FILE_PREFIX + "-" + DateUtil.yyyymmdd()+ ".log";
			outFile = new File(DIRECTORY, filename);
			File parentDir = new File(DIRECTORY);
			if (parentDir.exists() == false) {
				parentDir.mkdirs();
			}
		}
		return outFile;
	}

	public synchronized void registerCmd(String cmd) {
		if (cmdSet.contains(cmd) || demandSet.contains(cmd)) {
			return;
		}
		cmdSet.add(cmd);
		queue.put(new RequestInfo(System.currentTimeMillis(), "unknown", "register", cmd));
	}

	class RequestInfo {
		long time;
		String user;
		String ip;
		String cmd;

		RequestInfo(long time, String user, String ip, String cmd) {
			this.time = time;
			this.user = user;
			this.ip = ip;
			this.cmd = cmd;
		}

		public String toString() {
			return DateUtil.timestamp(this.time) + " " + this.user + "(" + this.ip + ") "
					+ cmd;
		}
	}
}