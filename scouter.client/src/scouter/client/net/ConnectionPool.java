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

import java.util.LinkedList;

public class ConnectionPool {
	
	private final static int POOL_SIZE = 3;

	private LinkedList<TcpProxy> pool = new LinkedList<TcpProxy>();
	
	public int size() {
		return pool.size();
	}

	TcpProxy removeFirst() {
		return pool.removeFirst();
	}
	
	void put(TcpProxy t) {
		while (pool.size() >= POOL_SIZE) {
			pool.removeFirst().close();
		}
		pool.add(t);
	}
	
	public void closeAll() {
		while (pool.size() > 0) {
			pool.removeFirst().close();
		}
	}
}
