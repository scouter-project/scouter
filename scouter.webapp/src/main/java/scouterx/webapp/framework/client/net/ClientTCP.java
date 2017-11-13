/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */
package scouterx.webapp.framework.client.net;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.net.NetCafe;
import scouter.util.FileUtil;
import scouterx.webapp.framework.client.server.Server;
import scouterx.webapp.framework.client.server.ServerManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

@Slf4j
@Getter
public class ClientTCP {
	private Socket socket;
	private DataInputX in;
	private DataOutputX out;

	public void open(int serverId) {
		close();
		Server server = ServerManager.getInstance().getServer(serverId);
		if (server == null) {
			return;
		}
		try {
			socket = new Socket();
			socket.setKeepAlive(false);
			socket.setTcpNoDelay(true);
			socket.setPerformancePreferences(0, 2, 1);
			socket.setReuseAddress(true);
			socket.setSoLinger(true, 1000);
			socket.connect(new InetSocketAddress(server.getIp(), server.getPort()),3000);
			socket.setSoTimeout(server.getSoTimeOut());
			in = new DataInputX(new BufferedInputStream(socket.getInputStream()));
			out = new DataOutputX(new BufferedOutputStream(socket.getOutputStream()));
			
			out.writeInt(NetCafe.TCP_CLIENT);
			out.flush();

			log.info("Connected {} to {}:{}", this, server.getIp(), server.getPort());

		} catch (Throwable t) {
			log.error(t.getMessage());
			close();
		}
	}
	
	public DataOutputX getOutput() {
		return out;
	}

	public DataInputX getInput() {
		return in;
	}

	public boolean connected() {
		return socket != null && socket.isClosed() == false;
	}

	public void close() {
		FileUtil.close(socket);
		FileUtil.close(in);
		FileUtil.close(out);
		socket = null;
		in = null;
		out = null;
	}
}