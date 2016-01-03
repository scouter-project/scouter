package scouter.lang.step;

public class SqlXType {
    public final static byte STMT = 0x0;
    public final static byte PREPARED = 0x1;
    public final static byte DYNA = 0x2;


    public final static byte METHOD_KNOWN = 0x00;
    public final static byte METHOD_EXECUTE = 0x10;
    public final static byte METHOD_UPDATE = 0x20;
    public final static byte METHOD_QUERY = 0x30;

    public static String toString(byte xtype) {
        switch (xtype & 0x0f) {
            case STMT:
                return "STM> ";
            case PREPARED:
                return "PRE> ";
            case DYNA:
                return "DYN> ";
        }
        return "STM> ";
    }

    public static byte getMethodType(byte xtype) {
        return (byte) (xtype & 0xf0);
    }
}
