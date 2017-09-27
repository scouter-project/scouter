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

import lombok.extern.slf4j.Slf4j;
import scouterx.webapp.framework.configure.ConfigureAdaptor;
import scouterx.webapp.framework.configure.ConfigureManager;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

//TODO connection pool wait and timeout

@Slf4j
public class ConnectionPool {
	private static ConfigureAdaptor conf = ConfigureManager.getConfigure();
	private int poolSize;
	public ConnectionPool(int size) {
		this.poolSize = size;
	}

	private LinkedList<TcpProxy> pool = new LinkedList<TcpProxy>();

	TcpProxy getTcpProxy() {
		TcpProxy proxy;
		List<TcpProxy> proxyToClose = null;
		synchronized (this) {
			if(pool.size() == 0) {
				proxy = null;
			} else {
				//try just one more
				proxy = pool.removeFirst();
				if (proxy.getLastUsed() < (System.currentTimeMillis() - (conf.getNetWebappTcpClientPoolTimeout() - 1))) {
					proxyToClose = new ArrayList<>();
					proxyToClose.add(proxy);
					proxy = null;

					if(pool.size() != 0) {
						proxy = pool.removeFirst();
						if (proxy.getLastUsed() < (System.currentTimeMillis() - (conf.getNetWebappTcpClientPoolTimeout() - 1))) {
							proxyToClose.add(proxy);
							proxy = null;
						}

					}
				}
			}
		}
		if (proxyToClose != null) {
			for (TcpProxy p : proxyToClose) {
				try {
					p.realClose();
				} catch (Exception e) { }
			}
		}

		return proxy;
	}

	public synchronized void initPool(int serverId) {
		closeAllInternal();
		createAllInternal(serverId);
	}

	public synchronized int getCurrentPoolSize() {
		return pool.size();
	}

	public void put(TcpProxy t) {
		TcpProxy removed = null;
		t.setLastUsed(System.currentTimeMillis());

		synchronized (this) {
			pool.add(t);
			if (pool.size() > poolSize) {
				removed = pool.removeFirst();
			}
		}

		if (removed != null) {
			removed.realClose();
		}
	}

	public synchronized void closeAll() {
		closeAllInternal();
	}

	private void closeAllInternal() {
		while (pool.size() > 0) {
			pool.removeFirst().realClose();
		}
	}

	private void createAllInternal(int serverId) {
		//default 6
		put(new TcpProxy(serverId));
		put(new TcpProxy(serverId));
		put(new TcpProxy(serverId));
		put(new TcpProxy(serverId));
		put(new TcpProxy(serverId));
		put(new TcpProxy(serverId));
	}
}
