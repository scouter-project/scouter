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

import java.io.IOException;

import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.util.DateUtil;
import scouter.util.Hexa32;

public class XLogPack implements Pack {

	public long endTime;
	public int objHash;
	public int service;
	public long txid;
	public long caller;
	public long gxid;
	public int elapsed;
	public int error;

	public int cpu;
	public int sqlCount;
	public int sqlTime;
	public byte[] ipaddr;
	public int bytes;
	public int status;
	public long userid;
	public int userAgent;
	public int referer;
	public int group;
	public int apicallCount;
	public int apicallTime;

	// TOP100
	public String countryCode; // CountryCode.getCountryName(countryCode);
	public int city;
	public byte xType; // see XLogTypes

	public int login;
	public int desc;

	// WEB TIME
	public int webHash; // WEB서버의 ObjectHash
	public int webTime; // WEB서버 --> WAS 시작 시점까지의 시간
	
	//just used to control flow
	transient public boolean ignore; 
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("XLOG ");
		sb.append(DateUtil.timestamp(endTime));
		sb.append(" objHash=").append(Hexa32.toString32(objHash));
		sb.append(" service=").append(Hexa32.toString32(service));
		sb.append(" txid=").append(Hexa32.toString32(txid));
		sb.append(" caller=").append(Hexa32.toString32(caller));
		sb.append(" gxid=").append(Hexa32.toString32(gxid));
		sb.append(" elapsed=").append(elapsed);
		sb.append(" error=").append(error);

		return sb.toString();
	}

	public byte getPackType() {
		return PackEnum.XLOG;
	}

	public void write(DataOutputX out) throws IOException {
		DataOutputX o = new DataOutputX();

		o.writeDecimal(endTime);
		o.writeDecimal(objHash);
		o.writeDecimal(service);
		o.writeLong(txid);
		o.writeLong(caller);
		o.writeLong(gxid);
		o.writeDecimal(elapsed);
		o.writeDecimal(error);
		o.writeDecimal(cpu);
		o.writeDecimal(sqlCount);
		o.writeDecimal(sqlTime);
		o.writeBlob(ipaddr);
		o.writeDecimal(bytes);
		o.writeDecimal(status);
		o.writeDecimal(userid);
		o.writeDecimal(userAgent);
		o.writeDecimal(referer);
		o.writeDecimal(group);
		o.writeDecimal(apicallCount);
		o.writeDecimal(apicallTime);
		o.writeText(countryCode);
		o.writeDecimal(city);
		o.writeByte(xType);

		o.writeDecimal(login);
		o.writeDecimal(desc);

		o.writeDecimal(webHash);
		o.writeDecimal(webTime);

		out.writeBlob(o.toByteArray());
	}

	public Pack read(DataInputX din) throws IOException {

		DataInputX d = new DataInputX(din.readBlob());

		this.endTime = d.readDecimal();
		this.objHash = (int) d.readDecimal();
		this.service = (int) d.readDecimal();
		this.txid = d.readLong();
		this.caller = d.readLong();
		this.gxid = d.readLong();
		this.elapsed = (int) d.readDecimal();
		this.error = (int) d.readDecimal();
		this.cpu = (int) d.readDecimal();
		this.sqlCount = (int) d.readDecimal();
		this.sqlTime = (int) d.readDecimal();
		this.ipaddr = d.readBlob();
		this.bytes = (int) d.readDecimal();
		this.status = (int) d.readDecimal();
		this.userid = d.readDecimal();
		this.userAgent = (int) d.readDecimal();
		this.referer = (int) d.readDecimal();
		this.group = (int) d.readDecimal();
		this.apicallCount = (int) d.readDecimal();
		this.apicallTime = (int) d.readDecimal();
		if (d.available() > 0) {
			this.countryCode = d.readText();
			this.city = (int) d.readDecimal();
		}
		if (d.available() > 0) {
			this.xType = d.readByte();
		}
		if (d.available() > 0) {
			this.login = (int) d.readDecimal();
			this.desc = (int) d.readDecimal();
		}
		if (d.available() > 0) {
			this.webHash = (int) d.readDecimal();
			this.webTime = (int) d.readDecimal();
		}
	
		return this;
	}

}