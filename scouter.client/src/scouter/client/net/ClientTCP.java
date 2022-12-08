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
 *
 */
package scouter.client.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.net.NetCafe;
import scouter.util.FileUtil;


public class ClientTCP{

	Socket socket;
	DataInputX in;
	DataOutputX out;

	public void open(int serverId, boolean socksLogin) {
		close();
		Server server = ServerManager.getInstance().getServer(serverId);
		if (server == null) {
			return;
		}
		try {
			if (socksLogin) {
				InetSocketAddress proxyAddr = new InetSocketAddress(server.getSocksIp(), server.getSocksPort() );
				socket = new Socket(new Proxy(Proxy.Type.SOCKS, proxyAddr));
			}else {
				socket = new Socket();
			}
			///
			socket.setKeepAlive(true); 
			socket.setTcpNoDelay(true);
			//socket.setSoLinger(true, 0); 
			///
			socket.connect(new InetSocketAddress(server.getIp(), server.getPort()),3000);
			socket.setTcpNoDelay(true);
			socket.setSoTimeout(server.getSoTimeOut());
			in = new DataInputX(new BufferedInputStream(socket.getInputStream()));
			out = new DataOutputX(new BufferedOutputStream(socket.getOutputStream()));
			
			//*************//
			out.writeInt(NetCafe.TCP_CLIENT);
			out.flush();
			//*************//
			if (!server.isConnected()) {
				System.out.println(
						String.format("Success to connect %s:%d (%s)",
								server.getIp(), server.getPort(),
								server.isSocksLogin()?server.getSocksIp()+":"+server.getSocksPort() : "direct"));
			}
			server.setConnected(true);
		} catch (Throwable t) {
			System.out.println(t.getMessage());
			close();
			if (server.getConnectionPool().size() < 1) {
				server.setConnected(false);
			}
		}
	}
	
	public DataOutputX getOutput() {
		return out;
	}

	public DataInputX getInput() {
		return in;
	}

	public boolean isSessionOk() {
		return socket != null && !socket.isClosed();
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