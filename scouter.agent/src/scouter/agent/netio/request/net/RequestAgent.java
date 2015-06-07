/*
 *  Copyright 2015 LG CNS.
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
package scouter.agent.netio.request.net;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.net.SocketAddr;
import scouter.util.FileUtil;
import scouter.util.ThreadUtil;


public class RequestAgent extends Thread {

	private static RequestAgent instance = null;

	public static synchronized RequestAgent getInstance() {
		if (instance == null) {
			instance = new RequestAgent();
			instance.setDaemon(true);
			instance.setName(ThreadUtil.getName(instance));
			instance.start();
		}
		return instance;
	}

	ExecutorService threadPool = ThreadUtil.createExecutor(ThreadUtil.getName(RequestWorker.class), 2, 20, 30000, true);

	public void run() {
		while (true) {
			Configure conf = Configure.getInstance();
			String addrStr = conf.tcp_addr;

			InetAddress addr = null;
			if (addrStr != null) {
				try {
					addr = InetAddress.getByName(addrStr);
				} catch (UnknownHostException e) {
				}
			}
			int port = conf.tcp_port;
			int max_port = conf.tcp_port_max;

			ServerSocket server = null;
			for (int i = port; i < max_port; i++) {
				try {
					server = new ServerSocket(i, 50, addr);
					break;
				} catch (Exception e) {
					//Logger.println("A39" + i + " " +e);		
					server = null;
				}
			}

			if (server != null) {
				serverAddr = new SocketAddr(server.getInetAddress().getHostAddress(), server.getLocalPort());				
				Logger.info("tcp listen " + serverAddr.getPort() + " for request");
				try {
					while (true) {
						Socket client = server.accept();
						if (RequestWorker.getActiveCount() >= conf.max_concurrent_server_request) {
							client.close();
							Logger.println("A130", "SOCKET:Too many concurrent requests");
						} else {
							threadPool.execute(new RequestWorker(client));
						}
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
			serverAddr = null;
			server = FileUtil.close(server);
			ThreadUtil.sleep(3000);
		}
	}

	private SocketAddr serverAddr;

	public SocketAddr getSocketAddr() {
		return serverAddr;
	}
}
