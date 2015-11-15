package scouter.agent.trace;

import scouter.util.IntKeyLinkedMap;

public class TransferMap {
	public static class ID {
		public ID(long gxid, long caller, long callee) {
			this.gxid = gxid;
			this.caller = caller;
			this.callee = callee;
		}

		public long gxid;
		public long caller;
		public long callee;

	}

	private static IntKeyLinkedMap<ID> map = new IntKeyLinkedMap<ID>().setMax(5001);

	public static void put(int hash, long gxid, long caller, long callee) {
		map.put(hash, new ID(gxid, caller, callee));
	}

	public static ID get(int hash) {
		return map.get(hash);
	}

}
