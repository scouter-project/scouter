package scouter.agent.trace;

import scouter.agent.Configure;
import scouter.io.DataInputX;
import scouter.util.BitUtil;
import scouter.util.LongKeyLinkedMap;
import scouter.util.ThreadUtil;

public class SocketTable {

	public static class Info {
		public int service;
		public long txid;
		public boolean stackOrder = false;
		public String stack;
		public long count = 1;

		public Info(int service, long txid) {
			this.service = service;
			this.txid = txid;
		}
	}

	public static LongKeyLinkedMap<Info> socketMap = new LongKeyLinkedMap<SocketTable.Info>().setMax(1024);

	public static void add(byte[] ipaddr, int port, int serviceHash, long txid) {
		long key = mkey(ipaddr, port);
		Info info = socketMap.get(key);
		if (info != null) {
			if (info.stackOrder) {
				info.service = serviceHash;
				info.txid = txid;
				info.stackOrder = false;
				info.stack = ThreadUtil.getStackTrace(Thread.currentThread().getStackTrace(), 3);
			}
			if (info.service == 0) {
				info.service = serviceHash;
				info.txid = txid;
			}
			info.count++;
		} else {
			info = new Info(serviceHash, txid);
			socketMap.put(key, info);
			if (port == Configure.getInstance()._trace_fullstack_socket_open_port) {
				info.stack = ThreadUtil.getStackTrace(Thread.currentThread().getStackTrace(), 3);
			}
		}

	}

	private static long mkey(byte[] ipaddr, int port) {
		return BitUtil.composite(ipaddr == null ? 0 : DataInputX.toInt(ipaddr, 0), port);
	}
}
