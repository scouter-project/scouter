package scouter.lang.pack;

public class XLogTypes {
	public final static byte WEB_SERVICE = 0;
	public final static byte APP_SERVICE = 1;
	public final static byte BACK_THREAD = 2;
	public final static byte ASYNCSERVLET_DISPATCHED_SERVICE = 3;
	public final static byte BACK_THREAD2 = 4;

	public final static byte UNKNOWN = 99;

	public enum Type {
		WEB_SERVICE(XLogTypes.WEB_SERVICE),
		APP_SERVICE(XLogTypes.APP_SERVICE),
		BACK_THREAD(XLogTypes.BACK_THREAD),
		ASYNCSERVLET_DISPATCHED_SERVICE(XLogTypes.ASYNCSERVLET_DISPATCHED_SERVICE),
		BACK_THREAD2(XLogTypes.BACK_THREAD2),
		UNKNOWN(XLogTypes.UNKNOWN),
		;

		byte value;

		Type(byte value) {
			this.value = value;
		}

		public static Type of(byte value) {
			for (Type type : Type.values()) {
				if (type.value == value) {
					return type;
				}
			}
			return UNKNOWN;
		}
	}
}

