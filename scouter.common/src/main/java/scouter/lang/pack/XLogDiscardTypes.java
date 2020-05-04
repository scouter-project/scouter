package scouter.lang.pack;

public class XLogDiscardTypes {
	public final static byte DISCARD_NONE = 1;
	public final static byte DISCARD_ALL = 2;
	public final static byte DISCARD_PROFILE = 3;

	public static byte from(XLogDiscard xLogDiscard) {
		return xLogDiscard.byteFlag;
	}

	public static boolean isAliveXLog(byte n) {
		return n == DISCARD_NONE || n == DISCARD_PROFILE;
	}

	public static boolean isAliveProfile(byte n) {
		return n == DISCARD_NONE;
	}

	public enum XLogDiscard {
		NONE((byte) 1),
		DISCARD_ALL((byte) 2),
		DISCARD_PROFILE((byte) 3)
		;

		public byte byteFlag;

		XLogDiscard(byte byteFlag) {
			this.byteFlag = byteFlag;
		}
	}
}

