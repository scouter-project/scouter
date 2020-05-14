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

package scouter.lang.pack;

import scouter.io.DataInputX;
import scouter.io.DataOutputX;

import java.io.IOException;

/**
 * Object that contains multiple counter information
 */
public class DroppedXLogPack implements Pack {

	public long gxid;
	public long txid;

	public DroppedXLogPack() {
	}

	public DroppedXLogPack(long gxid, long txid) {
		this.gxid = gxid;
		this.txid = txid;
	}

	public boolean isDriving() {
		return (gxid == txid) || gxid == 0;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append("DroppedXLogPack:");
		buf.append(" g:").append(gxid);
		buf.append(" t:").append(txid);
		return buf.toString();
	}

	public byte getPackType() {
		return PackEnum.DROPPED_XLOG;
	}

	public void write(DataOutputX dout) throws IOException {
		dout.writeLong(gxid);
		dout.writeLong(txid);
	}

	public Pack read(DataInputX din) throws IOException {
		this.gxid = din.readLong();
		this.txid = din.readLong();
		return this;
	}
}
