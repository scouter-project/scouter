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
import scouter.util.DateUtil;
import scouter.util.Hexa32;

import java.io.IOException;

/**
 * Object that contains a part of full profile
 */
public class XLogProfilePack2 extends XLogProfilePack implements Pack {

	public long gxid;
	public byte xType;
	public byte discardType;
	public boolean ignoreGlobalConsequentSampling;

	public byte getPackType() {
		return PackEnum.XLOG_PROFILE2;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Profile2 ");
		sb.append(DateUtil.timestamp(time));
		sb.append(" objHash=").append(Hexa32.toString32(objHash));
		sb.append(" txid=").append(Hexa32.toString32(txid));
		sb.append(" gxid=").append(Hexa32.toString32(gxid));
		sb.append(" profile=").append(profile == null ? null : profile.length);
		return sb.toString();
	}

	public boolean isDriving() {
		return (gxid == txid) || gxid == 0;
	}

	public void write(DataOutputX dout) throws IOException {
		super.write(dout);
		dout.writeLong(gxid);
		dout.writeByte(xType);
		dout.writeByte(discardType);
		dout.writeBoolean(ignoreGlobalConsequentSampling);
	}

	public Pack read(DataInputX din) throws IOException {
		super.read(din);
		this.gxid = din.readLong();
		this.xType = din.readByte();
		this.discardType = din.readByte();
		this.ignoreGlobalConsequentSampling = din.readBoolean();

		return this;
	}

}
