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
public class XLogProfilePack implements Pack {

	/**
	 * Profile time
	 */
	public long time;
	/**
	 * Object ID
	 */
	public int objHash;
	/**
	 * Related transaction name hash
	 */
	public int service;
	/**
	 * Related transaction ID
	 */
	public long txid;
	/**
	 * Elapsed time until this step(ms)
	 */
	public int elapsed;
	/**
	 * Byte array of profile steps
	 */
	public byte[] profile;


	public byte getPackType() {
		return PackEnum.XLOG_PROFILE;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Profile ");
		sb.append(DateUtil.timestamp(time));
		sb.append(" objHash=").append(Hexa32.toString32(objHash));
		sb.append(" profile=").append(profile == null ? null : profile.length);
		return sb.toString();
	}

	public String toDebugString(String svcName) {
		StringBuilder sb = new StringBuilder();
		sb.append("Profile ");
		sb.append(" svcName=").append(svcName);
		sb.append(" objHash=").append(Hexa32.toString32(objHash));
		sb.append(" svc=").append(service);
		sb.append(" txid=").append(Hexa32.toString32(txid));
		sb.append(" profile=").append(profile == null ? null : profile.length);
		return sb.toString();
	}

	public void write(DataOutputX dout) throws IOException {
		dout.writeDecimal(time);
		dout.writeDecimal(objHash);
		dout.writeDecimal(service);
		dout.writeLong(txid);
		dout.writeBlob(profile);
	}

	public Pack read(DataInputX din) throws IOException {
		this.time = din.readDecimal();
		this.objHash = (int) din.readDecimal();
		this.service= (int) din.readDecimal();
		this.txid = din.readLong();
		this.profile = din.readBlob();

		return this;
	}

}
