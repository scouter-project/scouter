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

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.Value;
import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.net.RequestCmd;
import scouter.net.TcpFlag;

public class TcpProxy {
	private final ClientTCP tcp = new ClientTCP();
	private Server server;
	
	protected TcpProxy() { }

	protected TcpProxy(int serverId) {
		this.server = ServerManager.getInstance().getServer(serverId);
	}
	
	public static synchronized TcpProxy getTcpProxy(int serverId) {
		Server server = ServerManager.getInstance().getServer(serverId);
		if (server == null || server.isOpen() == false || server.isConnected() == false) {
			return new DummyTcpProxy();
		}
		ConnectionPool pool = server.getConnectionPool();
		if (pool.size() == 0)
			return new TcpProxy(serverId);
		else
			return pool.removeFirst();
	}

	public static synchronized void putTcpProxy(TcpProxy t) {
		if (t == null)
			return;
		if (t.isValid() && t.getServer().isConnected()) {
			ConnectionPool pool = t.getServer().getConnectionPool();
			pool.put(t);
		} else {
			t.close();
		}
	}
	
	protected ClientTCP getClientTcp() {
		return tcp;
	}
	
	public Server getServer() {
		return this.server;
	}

	public synchronized void open() {
		if (tcp.isSessionOk() == false) {
			tcp.open(this.server.getId());
		}
	}
	
	public synchronized void close() {
		sendClose();
		tcp.close();
	}
	
	protected void finalize() throws Throwable {
		tcp.close();
	};

	public Pack getSingle(String cmd, Pack param) {
		List<Pack> values = process(cmd, param);
		if (values == null || values.size() == 0)
			return null;
		else
			return values.get(0);
	}

	public List<Pack> process(String cmd, Pack param) {

		final List<Pack> list = new ArrayList<Pack>();
		process(cmd, param, new INetReader() {
			public void process(DataInputX in) throws IOException {
				Pack p = in.readPack();
				list.add(p);
			}
		});
		return list;
	}
	
	public Value getSingleValue(String cmd, Pack param) {
		List<Value> values = processValues(cmd, param);
		if (values == null || values.size() == 0)
			return null;
		else
			return values.get(0);
	}

	public List<Value> processValues(String cmd, Pack param) {
		final List<Value> list = new ArrayList<Value>();
		process(cmd, param, new INetReader() {
			public void process(DataInputX in) throws IOException {
				Value v = in.readValue();
				list.add(v);
			}
		});
		return list;
	}
	
	public boolean isValid() {
		if (this instanceof DummyTcpProxy) {
			return false;
		}
		return tcp.isSessionOk();
	}

	public synchronized void process(String cmd, Object param, INetReader recv) {
		open();
		if (tcp.isSessionOk() == false) {
			return;
		}
		
		long session = this.server.getSession();
		
		if (session != 0) {
			DataOutputX out = tcp.getOutput();
			DataInputX in = tcp.getInput();
			try {
				out.writeText(cmd);
				out.writeLong(session);
				if (param instanceof Value) {
					out.writeValue((Value) param);
				} else if (param instanceof Pack) {
					out.writePack((Pack) param);
				}
				out.flush();
				byte resFlag;
				while ((resFlag = in.readByte()) == TcpFlag.HasNEXT) {
					recv.process(in);
				}
				if (resFlag == TcpFlag.INVALID_SESSION) {
					server.setSession(0); // SessionObserver will relogin
					tcp.close();
				}
			} catch (Throwable e) {
				tcp.close();
			}
		}
	}

	public synchronized void sendClose() {
		if (tcp.isSessionOk() == false) {
			return;
		}
		DataOutputX out = tcp.getOutput();
		try {
			out.writeText(RequestCmd.CLOSE);
			out.flush();
		} catch (Exception e) {
		}
	}
	
	public static MapPack loginProxy(int serverId, MapPack param) throws IOException {
		TcpProxy proxy = new TcpProxy(serverId);
		proxy.open();
		if (proxy.isValid() == false) {
			return null;
		}
		param.put("ip", proxy.getLocalInetAddress().getHostAddress());
		DataOutputX out = proxy.getClientTcp().getOutput();
		DataInputX in = proxy.getClientTcp().getInput();
		try {
			out.writeText(RequestCmd.LOGIN);
			out.writeLong(0);
			out.writePack(param);
			out.flush();
			MapPack pack = null;
			while (in.readByte() == TcpFlag.HasNEXT) {
				pack = (MapPack) in.readPack();
			}
			return pack;
		} finally {
			proxy.close();
		}
	}
	
	public InetAddress getLocalInetAddress() {
		return tcp.socket.getLocalAddress();
	}
}
