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
package scouter.agent.netio.data;

import scouter.agent.Configure;
import scouter.agent.Logger;
import scouter.agent.netio.data.net.DataUdpAgent;
import scouter.agent.trace.TraceContext;
import scouter.io.DataOutputX;
import scouter.lang.TextTypes;
import scouter.lang.pack.AlertPack;
import scouter.lang.pack.DroppedXLogPack;
import scouter.lang.pack.ObjectPack;
import scouter.lang.pack.Pack;
import scouter.lang.pack.SummaryPack;
import scouter.lang.pack.TextPack;
import scouter.lang.pack.XLogDiscardTypes;
import scouter.lang.pack.XLogPack;
import scouter.lang.pack.XLogProfilePack2;
import scouter.lang.step.Step;
import scouter.lang.value.MapValue;
import scouter.util.HashUtil;
import scouter.util.IntIntLinkedMap;
import scouter.util.IntLinkedSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
public class DataProxy {
	private static UDPDataSendThread udpCollect = UDPDataSendThread.getInstance();
	private static IntIntLinkedMap sqlHash = new IntIntLinkedMap().setMax(5000);
	private static int getSqlHash(String sql) {
		if (sql.length() < 100)
			return HashUtil.hash(sql);
		int id = sql.hashCode();
		int hash = sqlHash.get(id);
		if (hash == 0) {
			hash = HashUtil.hash(sql);
			sqlHash.put(id, hash);
		}
		return hash;
	}
	private static IntLinkedSet sqlText = new IntLinkedSet().setMax(10000);
	public static int sendSqlText(String sql) {
		int hash = getSqlHash(sql);
		if (sqlText.contains(hash)) {
			return hash;
		}
		sqlText.put(hash);
		// udp.add(new TextPack(TextTypes.SQL, hash, sql));
		sendDirect(new TextPack(TextTypes.SQL, hash, sql));
		return hash;
	}
	private static IntLinkedSet serviceName = new IntLinkedSet().setMax(10000);
	public static int sendServiceName(String service) {
		int hash = HashUtil.hash(service);
		sendServiceName(hash,service);
		return hash;
	}
	public static void sendServiceName(int hash, String service) {
		if (serviceName.contains(hash)) {
			return ;
		}
		serviceName.put(hash);
		udpCollect.add(new TextPack(TextTypes.SERVICE, hash, service));
	}
	private static IntLinkedSet objNameSet = new IntLinkedSet().setMax(10000);
	public static int sendObjName(String objName) {
		int hash = HashUtil.hash(objName);
		sendObjName(hash,objName);
		return hash;
	}
	public static void sendObjName(int hash, String objName) {
		if (objNameSet.contains(hash)) {
			return ;
		}
		objNameSet.put(hash);
		udpCollect.add(new TextPack(TextTypes.OBJECT, hash, objName));
	}
	private static IntLinkedSet referer = new IntLinkedSet().setMax(1000);
	public static int sendReferer(String text) {
		int hash = HashUtil.hash(text);
		if (referer.contains(hash)) {
			return hash;
		}
		referer.put(hash);
		sendDirect(new TextPack(TextTypes.REFERER, hash, text));
		return hash;
	}
	private static IntLinkedSet userAgent = new IntLinkedSet().setMax(1000);
	public static int sendUserAgent(String text) {
		int hash = HashUtil.hash(text);
		
		if (userAgent.contains(hash)) {
			return hash;
		}
		userAgent.put(hash);
		udpCollect.add(new TextPack(TextTypes.USER_AGENT, hash, text));
		return hash;
	}
	private static IntLinkedSet methodName = new IntLinkedSet().setMax(10000);
	public static int sendMethodName(String name) {
		int hash = HashUtil.hash(name);
		if (methodName.contains(hash)) {
			return hash;
		}
		methodName.put(hash);
		udpCollect.add(new TextPack(TextTypes.METHOD, hash, name));
		return hash;
	}
	private static IntLinkedSet apicall = new IntLinkedSet().setMax(10000);
	public static int sendApicall( String name) {
		int hash = HashUtil.hash(name);
		if (apicall.contains(hash)) {
			return hash;
		}
		apicall.put(hash);
		udpCollect.add(new TextPack(TextTypes.APICALL, hash, name));
		return hash;
	}
	static Configure conf = Configure.getInstance();
	public static void sendAlert(byte level, String title, String message, MapValue tags) {
		AlertPack p = new AlertPack();
		p.objType = conf.obj_type;
		p.objHash = conf.getObjHash();
		p.level = level;
		p.title = title;
		p.message = message;
		if (tags != null) {
			p.tags = tags;
		}
		sendDirect(p);
	}
	private static IntLinkedSet errText = new IntLinkedSet().setMax(10000);
	public static int sendError( String message) {
		int hash = HashUtil.hash(message);
		if (errText.contains(hash)) {
			return hash;
		}
		errText.put(hash);
		udpCollect.add(new TextPack(TextTypes.ERROR, hash, message));
		return hash;
	}
	private static IntLinkedSet descTable = new IntLinkedSet().setMax(1000);
	public static int sendDesc( String desc) {
		int hash = HashUtil.hash(desc);
		if (descTable.contains(hash)) {
			return hash;
		}
		descTable.put(hash);
		udpCollect.add(new TextPack(TextTypes.DESC, hash, desc));
		return hash;
	}
	private static IntLinkedSet loginTable = new IntLinkedSet().setMax(10000);
	public static int sendLogin( String loginName) {
		int hash = HashUtil.hash(loginName);
		if (loginTable.contains(hash)) {
			return hash;
		}
		loginTable.put(hash);
		udpCollect.add(new TextPack(TextTypes.LOGIN, hash, loginName));
		return hash;
   }
	public static void reset() {
		serviceName.clear();
		errText.clear();
		apicall.clear();
		methodName.clear();
		sqlText.clear();
		referer.clear();
		userAgent.clear();
		descTable.clear();
		loginTable.clear();
		webNameTable.clear();
		groupAgent.clear();
		hashMessage.clear();
		stackElement.clear();

	}
	public static void sendXLog(XLogPack p) {
		p.objHash = conf.getObjHash();
		p.ignoreGlobalConsequentSampling = conf.ignore_global_consequent_sampling;
		sendDirect(p);
		if (conf._log_udp_xlog_enabled) {
			Logger.println(p.toString());
		}
	}
	public static void sendDroppedXLog(DroppedXLogPack p) {
		sendDirect(p);
		if (conf._log_udp_xlog_enabled) {
			Logger.println(p.toString());
		}
	}
	public static void send(SummaryPack p) {
		p.objHash = conf.getObjHash();
		p.objType = conf.obj_type;
		sendDirect(p);
	}
	static DataUdpAgent udpNet = DataUdpAgent.getInstance();
	public static void sendDirect(Pack p) {
		try {
			udpNet.write(new DataOutputX().writePack(p).toByteArray());
		} catch (IOException e) {
		}
	}
	private static void sendDirect(List<byte[]> buff) {
		switch (buff.size()) {
		case 0:  return;
		case 1:
			udpNet.write(buff.get(0));
			break;
		default:
			udpNet.write(buff);
			break;
		}
	}

	public static void sendProfile(Step[] p, TraceContext context) {
		if (p == null || p.length == 0)
			return;

		int bulkSize = conf.profile_step_max_count;
		int count = p.length / bulkSize;

		if (count == 0) {
			sendProfile0(p, context);
			return;
		}

		int remainder = p.length % bulkSize;
		for (int i = 0; i < count; i++) {
			Step[] parts = new Step[bulkSize];
			System.arraycopy(p, i * bulkSize, parts, 0, bulkSize);
			sendProfile0(parts, context);
		}
		if (remainder > 0) {
			Step[] parts = new Step[remainder];
			System.arraycopy(p, count * bulkSize, parts, 0, remainder);
			sendProfile0(parts, context);
		}
	}

	public static void sendProfile0(Step[] p, TraceContext context) {
		if (p == null || p.length == 0)
			return;

		XLogProfilePack2 pk = new XLogProfilePack2();
		pk.ignoreGlobalConsequentSampling = conf.ignore_global_consequent_sampling;
		pk.txid = context.txid;
		pk.gxid = context.gxid;
		pk.xType = context.xType;
		pk.discardType = context.discardType == null ? XLogDiscardTypes.DISCARD_NONE : context.discardType.byteFlag;
		pk.objHash = conf.getObjHash();
		pk.profile = Step.toBytes(p);
		pk.service = context.serviceHash;
		pk.elapsed = (int) (System.currentTimeMillis() - context.startTime);
		context.profileCount += p.length;
		context.profileSize += pk.profile.length;
		sendDirect(pk);
	}

	public static void sendProfile(List<Step> p, TraceContext x) {
		if (p == null || p.size() == 0)
			return;
		XLogProfilePack2 pk = new XLogProfilePack2();
		pk.ignoreGlobalConsequentSampling = conf.ignore_global_consequent_sampling;
		pk.txid = x.txid;
		pk.gxid = x.gxid;
		pk.xType = x.xType;
		pk.discardType = x.discardType == null ? XLogDiscardTypes.DISCARD_NONE : x.discardType.byteFlag;
		pk.objHash = conf.getObjHash();
		pk.profile = Step.toBytes(p);
		x.profileCount += p.size();
		x.profileSize += pk.profile.length;
		// udp.add(pk);
		sendDirect(pk);
	}

	//only for counterPack & interactionCounterPack
	public static void sendCounter(Pack[] p) {
		// udp.add(p);
		try {
			List<byte[]> buff = new ArrayList<byte[]>();
			int bytes = 0;
			for (int k = 0; k < p.length; k++) {
				if (conf._log_udp_counter_enabled) {
					Logger.println(p[k].toString());
				}
				byte[] b = new DataOutputX().writePack(p[k]).toByteArray();
				if (bytes + b.length >= conf.net_udp_packet_max_bytes) {
					sendDirect(buff); // buff.size가 0일수도 있다.
					bytes = 0;// bytes 값 초기화..
					buff.clear();
				}
				bytes += b.length;
				buff.add(b);
			}
			sendDirect(buff);
		} catch (Exception e) {
		}
	}

	public static void sendHeartBeat(ObjectPack p) {
		udpCollect.add(p);
		if (conf._log_udp_object_enabled) {
			Logger.println(p.toString());
		}
	}
	private static IntLinkedSet webNameTable = new IntLinkedSet().setMax(1000);
	public static int sendWebName( String web) {
		int hash = HashUtil.hash(web);
		if (webNameTable.contains(hash)) {
			return hash;
		}
		webNameTable.put(hash);
		udpCollect.add(new TextPack(TextTypes.WEB, hash, web));
		return hash;
	}
	
	private static IntLinkedSet groupAgent = new IntLinkedSet().setMax(500);
	public static int sendGroup(String text) {
		int hash = HashUtil.hash(text);	
		if (groupAgent.contains(hash)) {
			return hash;
		}
		groupAgent.put(hash);
		udpCollect.add(new TextPack(TextTypes.GROUP, hash, text));
		return hash;
	}
	private static IntLinkedSet hashMessage = new IntLinkedSet().setMax(10000);
	public static int sendHashedMessage(String text) {
		int hash = HashUtil.hash(text);
		if (hashMessage.contains(hash)) {
			return hash;
		}
		hashMessage.put(hash);
		udpCollect.add(new TextPack(TextTypes.HASH_MSG, hash, text));
		return hash;
	}

	private static IntLinkedSet stackElement = new IntLinkedSet().setMax(20000);
	public static int sendStackElement(StackTraceElement ste) {
		int hash = ste.hashCode();
		if (stackElement.contains(hash)) {
			return hash;
		}
		stackElement.put(hash);
		udpCollect.add(new TextPack(TextTypes.STACK_ELEMENT, hash, ste.toString()));
		return hash;
	}
	public static int sendStackElement(String ste) {
		int hash = ste.hashCode();
		if (stackElement.contains(hash)) {
			return hash;
		}
		stackElement.put(hash);
		udpCollect.add(new TextPack(TextTypes.STACK_ELEMENT, hash, ste));
		return hash;
	}
}
