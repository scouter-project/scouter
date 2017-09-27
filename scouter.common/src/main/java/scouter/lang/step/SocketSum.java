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

package scouter.lang.step;

import scouter.io.DataInputX;
import scouter.io.DataOutputX;

import java.io.IOException;

public class SocketSum extends StepSummary {

	public byte[] ipaddr;
	public int port;
	public int count;
	public long elapsed;
	public int error;
	
	public SocketSum() {
	}

	public SocketSum(byte[] ipaddr, int port) {
		this.ipaddr = ipaddr;
		this.port = port;
	}

	public void add(int elapsed, boolean error) {
		this.count++;
		this.elapsed+=elapsed;
		if(error)
			this.error++;
	}

	public byte getStepType() {
		return StepEnum.SOCKET_SUM;
	}

	public byte[] getIpaddr() {
		return ipaddr;
	}

	public int getPort() {
		return port;
	}

	public int getCount() {
		return count;
	}

	public long getElapsed() {
		return elapsed;
	}

	public int getError() {
		return error;
	}

	public void write(DataOutputX out) throws IOException {
		out.writeBlob(ipaddr);
		out.writeDecimal(port);
		out.writeDecimal(count);
		out.writeDecimal(elapsed);
		out.writeDecimal(error);
	}

	public Step read(DataInputX in) throws IOException {
		this.ipaddr = in.readBlob();
		this.port = (int) in.readDecimal();
		this.count = (int) in.readDecimal();
		this.elapsed =  in.readDecimal();
		this.error =  (int)in.readDecimal();
		return this;
	}
}