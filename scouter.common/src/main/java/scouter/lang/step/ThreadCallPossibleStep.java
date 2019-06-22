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

public class ThreadCallPossibleStep extends StepSingle {
	public long txid;
	public int hash;
	public int elapsed;
	//0 - none thread dispatching, 1 - thread dispatching
	public byte threaded;
	public String nameTemp;

	public boolean isIgnoreIfNoThreaded;

	public byte getStepType() {
		return StepEnum.THREAD_CALL_POSSIBLE;
	}

	public long getTxid() {
		return txid;
	}

	public int getHash() {
		return hash;
	}

	public int getElapsed() {
		return elapsed;
	}

	public byte getThreaded() {
		return threaded;
	}

	public String getNameTemp() {
		return nameTemp;
	}

	public void write(DataOutputX out) throws IOException {
		super.write(out);

		out.writeDecimal(txid);
		out.writeDecimal(hash);
		out.writeDecimal(elapsed);
		out.writeByte(threaded);
	}

	public Step read(DataInputX in) throws IOException {
		super.read(in);

		this.txid = in.readDecimal();
		this.hash = (int) in.readDecimal();
		this.elapsed = (int) in.readDecimal();
		this.threaded= in.readByte();
		return this;
	}
}
