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
 *
 */
package scouter.client.daemon.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import scouter.client.model.XLogData;
import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.client.server.ServerManager;
import scouter.client.xlog.XLogUtil;
import scouter.io.DataInputX;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.pack.XLogPack;
import scouter.lang.pack.XLogProfilePack;
import scouter.lang.step.Step;
import scouter.net.RequestCmd;
import scouter.util.IntSet;
import scouter.util.ThreadUtil;

public class XLogApi {
	public static List<Pack> getListByGxId(int serverId, String date, long gxid) {
		final List<Pack> list = new ArrayList<Pack>();
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			MapPack param = new MapPack();
			param.put("date", date);
			param.put("gxid", gxid);
			tcp.process(RequestCmd.XLOG_READ_BY_GXID, param, new INetReader() {
				public void process(DataInputX in) throws IOException {
					Pack p = in.readPack();
					list.add(p);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return list;
	}

	public static List<Pack> getListByGxId(int serverId, long stime, long etime, long gxid) {
		final List<Pack> list = new ArrayList<Pack>();
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			MapPack param = new MapPack();
			param.put("stime", stime);
			param.put("etime", etime);
			param.put("gxid", gxid);
			tcp.process(RequestCmd.XLOG_LOAD_BY_GXID, param, new INetReader() {
				public void process(DataInputX in) throws IOException {
					Pack p = in.readPack();
					list.add(p);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return list;
	}

	static Executor pool = Executors.newCachedThreadPool();

	public static List<XLogData> getAllListByGxId(final String date, final long gxid) {
		final List<XLogData> list = new ArrayList<XLogData>();
		Enumeration<Integer> serverList = ServerManager.getInstance().getAllServerList();

		final IntSet worker = new IntSet();
		while (serverList.hasMoreElements()) {
			final int serverId = serverList.nextElement();
			worker.add(serverId);
			pool.execute(new Runnable() {
				public void run() {
					List<Pack> packList = getListByGxId(serverId, date, gxid);
					for (Pack p : packList) {
						try {
							XLogPack xpack = XLogUtil.toXLogPack(p);
							XLogData data = new XLogData(xpack, serverId);
							list.add(data);
						} catch (Throwable e) {
						}
					}
					worker.remove(serverId);
				}
			});
		}
		while (worker.size() > 0) {
			ThreadUtil.qWait();
		}
		Collections.sort(list, new Comparator<XLogData>() {
			public int compare(XLogData o1, XLogData o2) {
				long stime1 = o1.p.endTime - o1.p.elapsed;
				long stime2 = o2.p.endTime - o2.p.elapsed;
				return stime1 >= stime2 ? 1 : -1;
			}
		});
		return list;
	}

	public static List<XLogData> getAllListByGxId(final long stime, final long etime, final long gxid) {
		final List<XLogData> list = new ArrayList<XLogData>();
		Enumeration<Integer> serverList = ServerManager.getInstance().getAllServerList();

		final IntSet worker = new IntSet();
		while (serverList.hasMoreElements()) {
			final int serverId = serverList.nextElement();
			worker.add(serverId);
			pool.execute(new Runnable() {
				public void run() {
					List<Pack> packList = getListByGxId(serverId, stime, etime, gxid);
					for (Pack p : packList) {
						try {
							XLogPack xpack = XLogUtil.toXLogPack(p);
							XLogData data = new XLogData(xpack, serverId);
							list.add(data);
						} catch (Throwable e) {
						}
					}
					worker.remove(serverId);
				}
			});
		}
		while (worker.size() > 0) {
			ThreadUtil.qWait();
		}
		Collections.sort(list, new Comparator<XLogData>() {
			public int compare(XLogData o1, XLogData o2) {
				long stime1 = o1.p.endTime - o1.p.elapsed;
				long stime2 = o2.p.endTime - o2.p.elapsed;
				return stime1 >= stime2 ? 1 : -1;
			}
		});
		return list;
	}

	public static XLogPack getXLog(String date, long txid, int serverId) {
		MapPack param = new MapPack();
		param.put("date", date);
		param.put("txid", txid);
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			Pack p = tcp.getSingle(RequestCmd.XLOG_READ_BY_TXID, param);
			return XLogUtil.toXLogPack(p);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return null;
	}

	public static Step[] getProfile(String date, long txIds, int serverId) {
		MapPack param = new MapPack();
		param.put("date", date);
		param.put("txid", txIds);
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			XLogProfilePack re = (XLogProfilePack) tcp.getSingle(RequestCmd.TRANX_PROFILE, param);
			if (re == null)
				return null;
			return Step.toObjects(re.profile);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return null;
	}
}
