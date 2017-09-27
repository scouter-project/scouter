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
import scouter.util.BitUtil;
import scouter.util.HashUtil;
import scouter.util.IPUtil;

import java.io.IOException;


public class SocketStep extends StepSingle {

	public byte[] ipaddr;
	public int port;
	public int elapsed;
	public int error;

	public SocketStep() {
	}
	public SocketStep(byte[] ipaddr, int port) {
		this.ipaddr=ipaddr;
		this.port=port;
	}
	public byte getStepType() {
		return StepEnum.SOCKET;
	}

	public byte[] getIpaddr() {
		return ipaddr;
	}

	public int getPort() {
		return port;
	}

	public int getElapsed() {
		return elapsed;
	}

	public int getError() {
		return error;
	}

	public String toString() {
		return IPUtil.toString(ipaddr) +":" +port;
	}

	public void write(DataOutputX out) throws IOException {
		super.write(out);
		out.writeBlob(ipaddr);
		out.writeDecimal(port);
		out.writeDecimal(elapsed);
		out.writeDecimal(error);
	}

	public Step read(DataInputX in) throws IOException {
		super.read(in);
		this.ipaddr = in.readBlob();
		this.port = (int)in.readDecimal();
		this.elapsed = (int)in.readDecimal();
		this.error = (int)in.readDecimal();
		return this;
	}
	public long getSocketId() {
		return BitUtil.composite(HashUtil.hash(ipaddr) ,port);
	}
}