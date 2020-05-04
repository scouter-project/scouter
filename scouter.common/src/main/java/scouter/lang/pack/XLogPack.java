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
 * Object that contains one transaction information
 */
public class XLogPack implements Pack {

	/**
	 * Transaction endtime
	 */
	public long endTime;
	/**
	 * Object ID
	 */
	public int objHash;
	/**
	 * Transaction name Hash
	 */
	public int service;
	/**
	 * Transaction ID
	 */
	public long txid;
	/**
	 * thread name hash
	 */
	public int threadNameHash;
	/**
	 * Caller ID
	 */
	public long caller;
	/**
	 * Global transaction ID
	 */
	public long gxid;
	/**
	 * Elapsed time(ms)
	 */
	public int elapsed;
	/**
	 * Error hash
	 */
	public int error;
	/**
	 * Cpu time(ms)
	 */
	public int cpu;
	/**
	 * SQL count
	 */
	public int sqlCount;
	/**
	 * SQL time(ms)
	 */
	public int sqlTime;
	/**
	 * Remote ip address
	 */
	public byte[] ipaddr;
	/**
	 * Allocated memory(kilo byte)
	 */
	public int kbytes;
	/**
	 * Http status
	 */
	@Deprecated
	public int status;
	/**
	 * User ID
	 */
	public long userid;
	/**
	 * User-agent hash
	 */
	public int userAgent;
	/**
	 * Referer hash
	 */
	public int referer;
	/**
	 * Group hash
	 */
	public int group;
	/**
	 * ApiCall count
	 */
	public int apicallCount;
	/**
	 * ApiCall time(ms)
	 */
	public int apicallTime;
	/**
	 * Country code
	 */
	public String countryCode; // CountryCode.getCountryName(countryCode);
	/**
	 * City hash
	 */
	public int city;
	/**
	 * XLog type. WebService:0, AppService:1, BackgroundThread:2
	 */
	public byte xType; // see XLogTypes
	/**
	 * Login hash
	 */
	public int login;
	/**
	 * Description hash
	 */
	public int desc;
	/**
	 * WebServer object ID
	 */
	@Deprecated
	public int webHash; // WEB서버의 ObjectHash
	/**
	 * WebServer -> WAS time(ms)
	 */
	@Deprecated
	public int webTime; // WEB서버 --> WAS 시작 시점까지의 시간
	/**
	 * has Thread Dump ? No:0, Yes:1
	 */
	public byte hasDump;

	/**
	 * any text (not use dic)
	 */
	public String text1;
	public String text2;

	/**
	 * queuing host and time
	 */
	public int queuingHostHash;
	public int queuingTime;
	public int queuing2ndHostHash;
	public int queuing2ndTime;

	/**
	 * any text (not use dic)
	 */
	public String text3;
	public String text4;
	public String text5;

	public int profileCount;
	public boolean b3Mode;
	public int profileSize;
	public byte discardType;
	public boolean ignoreGlobalConsequentSampling;

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

	public boolean isDriving() {
		return (gxid == txid) || gxid == 0;
	}

	public boolean isDropped() {
		return service == 0;
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
		o.writeDecimal(kbytes);
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

		o.writeByte(hasDump);

		o.writeDecimal(threadNameHash);
		o.writeText(text1);
		o.writeText(text2);

		o.writeDecimal(queuingHostHash);
		o.writeDecimal(queuingTime);
		o.writeDecimal(queuing2ndHostHash);
		o.writeDecimal(queuing2ndTime);

		o.writeText(text3);
		o.writeText(text4);
		o.writeText(text5);

		o.writeDecimal(profileCount);
		o.writeBoolean(b3Mode);
		o.writeDecimal(profileSize);
		o.writeByte(discardType);
		o.writeBoolean(ignoreGlobalConsequentSampling);

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
		this.kbytes = (int) d.readDecimal();
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
		if (d.available() >0) {
			this.hasDump = d.readByte();
		}
		if (d.available() >0) {
			this.threadNameHash = (int) d.readDecimal();
		}
		if (d.available() >0) {
			this.text1 = d.readText();
			this.text2 = d.readText();
		}
		if (d.available() >0) {
			this.queuingHostHash = (int) d.readDecimal();
			this.queuingTime = (int) d.readDecimal();
			this.queuing2ndHostHash = (int) d.readDecimal();
			this.queuing2ndTime = (int) d.readDecimal();
		}
		if (d.available() >0) {
			this.text3 = d.readText();
			this.text4 = d.readText();
			this.text5 = d.readText();
		}
		if (d.available() >0) {
			this.profileCount = (int) d.readDecimal();
		}
		if (d.available() >0) {
			this.b3Mode = d.readBoolean();
		}
		if (d.available() >0) {
			this.profileSize = (int) d.readDecimal();
			this.discardType = d.readByte();
			this.ignoreGlobalConsequentSampling = d.readBoolean();
		}

		return this;
	}

}
