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
 *
 */
package scouter.client.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ConsoleProxy;
import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.util.FileUtil;


public class ClientTCP{
	Socket socket;
	DataInputX in;
	DataOutputX out;

	public void open(int serverId) {
		close();
		Server server = ServerManager.getInstance().getServer(serverId);
		if (server == null) {
			return;
		}
		try {
			socket = new Socket();
			///
			socket.setKeepAlive(true); 
			socket.setTcpNoDelay(true);
			//socket.setSoLinger(true, 0); 
			///
			socket.connect(new InetSocketAddress(server.getIp(), server.getPort()));
			socket.setTcpNoDelay(true);
			socket.setSoTimeout(4000);
			in = new DataInputX(new BufferedInputStream(socket.getInputStream()));
			out = new DataOutputX(new BufferedOutputStream(socket.getOutputStream()));
			if (server.isConnected() == false) {
				ConsoleProxy.infoSafe("Success to connect " + server.getIp() + ":" + server.getPort());
				server.setConnected(true);
			}
		} catch (Throwable t) {
			ConsoleProxy.errorSafe(t.toString());
			close();
			server.setConnected(false);
		}
	}
	
	public DataOutputX getOutput() {
		return out;
	}

	public DataInputX getInput() {
		return in;
	}

	public boolean isSessionOk() {
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