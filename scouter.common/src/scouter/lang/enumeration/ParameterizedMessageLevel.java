package scouter.lang.enumeration;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 4. 24.
 */
public enum ParameterizedMessageLevel {
    DEBUG((byte)0),
    INFO((byte)1),
    WARN((byte)2),
    ERROR((byte)3),
    FATAL((byte)4),
    ;

    private final byte level;

    ParameterizedMessageLevel(byte level) {
        this.level = level;
    }

    public byte getLevel() {
        return this.level;
    }

    public static ParameterizedMessageLevel of(byte level) {
        for (ParameterizedMessageLevel plevel : ParameterizedMessageLevel.values()) {
            if (level == plevel.getLevel()) {
                return plevel;
            }
        }
        throw new IllegalArgumentException("not matched ParameterizedMessageLevel value.");
    }

    public static ParameterizedMessageLevel of(int level) {
        return of((byte) level);
    }
}
