package scouter.lang.step;

public class SqlXType {
	public final static byte STMT = 0;
	public final static byte PREPARED = 1;
	public final static byte DYNA = 2;

	public static String toString(byte xtype) {
		switch (xtype) {
		case STMT:
			return "ST)";
		case PREPARED:
			return "PS)";
		case DYNA:
			return "PD)";
		}
		return "ST)";
	}
}
