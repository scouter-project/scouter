package scouter.agent.trace;

import scouter.util.IntKeyLinkedMap;

public class TransferMap {
	public static class ID {
		public ID(long gxid, long caller, long callee, byte xType) {
			this.gxid = gxid;
			this.caller = caller;
			this.callee = callee;
			this.xType=xType;
		}

		public long gxid;
		public long caller;
		public long callee;
        public byte xType; 
	}

	private static IntKeyLinkedMap<ID> map = new IntKeyLinkedMap<ID>().setMax(5001);

	public static void put(int hash, long gxid, long caller, long callee, byte xType) {
		map.put(hash, new ID(gxid, caller, callee, xType));
	}

	public static ID get(int hash) {
		return map.get(hash);
	}

}
