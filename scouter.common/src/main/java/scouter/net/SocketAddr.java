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

package scouter.net;

import scouter.util.CastUtil;

public class SocketAddr {

	private String addr;
	private int port;

	public SocketAddr(String addr, int port) {
		this.addr = addr;
		this.port = port;
	}

	public SocketAddr(String address) {
		try {
			int x = address.indexOf(":");
			this.addr = address.substring(0, x);
			this.port = CastUtil.cint(address.substring(x + 1));
		} catch (Exception e) {
			// ignore
		}
	}

	public boolean equals(Object obj) {
		if (obj instanceof SocketAddr) {
			SocketAddr a = (SocketAddr) obj;
			return this.addr.equals(a.addr) && this.port == a.port;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.addr.hashCode() ^ this.port;
	}

	public String getAddr() {
		return addr;
	}

	public int getPort() {
		return port;
	}

	public String toString() {
		return addr + ":" + port;
	}

	public boolean isOk() {
		return addr != null && port > 0;
	}
}