package scouter.lang.pack;

public class XLogDiscardTypes {
	public final static byte DISCARD_NONE = 1;
	public final static byte DISCARD_ALL = 2;
	public final static byte DISCARD_PROFILE = 3;
	public final static byte DISCARD_ALL_FORCE = 4;
	public final static byte DISCARD_PROFILE_FORCE = 5;

	public static byte from(XLogDiscard xLogDiscard) {
		return xLogDiscard.byteFlag;
	}

	public static boolean isAliveXLog(byte n) {
		return n == DISCARD_NONE || n == DISCARD_PROFILE || n == DISCARD_PROFILE_FORCE;
	}

	public static boolean isAliveProfile(byte n) {
		return n == DISCARD_NONE;
	}

	public enum XLogDiscard {
		NONE((byte) 1),
		DISCARD_ALL((byte) 2),
		DISCARD_PROFILE((byte) 3),
		DISCARD_ALL_FORCE((byte) 4),
		DISCARD_PROFILE_FORCE((byte) 5)
		;

		public byte byteFlag;

		XLogDiscard(byte byteFlag) {
			this.byteFlag = byteFlag;
		}

		public boolean isForceDiscard() {
			return this == DISCARD_ALL_FORCE || this == DISCARD_PROFILE_FORCE;
		}

		public XLogDiscard toForce() {
			if (this == DISCARD_ALL) {
				return DISCARD_ALL_FORCE;
			}
			if (this == DISCARD_PROFILE) {
				return DISCARD_PROFILE_FORCE;
			}
			return this;
		}
	}
}

