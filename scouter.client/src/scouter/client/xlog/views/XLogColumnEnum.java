package scouter.client.xlog.views;

import org.eclipse.swt.SWT;

import java.io.Serializable;

/**
 * @author Gun Lee (gunlee01@gmail.com) on 2017. 8. 15.
 */
public enum XLogColumnEnum implements Serializable {
    OBJECT("Object", 80, SWT.LEFT, true, true, false, true, "XLOG_COL_OBJECT"),
    ELAPSED("Elapsed", 50, SWT.RIGHT, true, true, true, true, "XLOG_COL_ELAPSED"),
    SERVICE("Service", 100, SWT.LEFT, true, true, false, true, "XLOG_COL_SERVICE"),
    END_TIME("EndTime", 70, SWT.CENTER, true, true, true, true, "XLOG_COL_END_TIME"),
    CPU("Cpu", 40, SWT.RIGHT, true, true, true, true, "XLOG_COL_CPU"),
    SQL_COUNT("SQL Count", 50, SWT.RIGHT, true, true, true, true, "XLOG_COL_SQL_COUNT"),
    SQL_TIME("SQL Time", 50, SWT.RIGHT, true, true, true, true, "XLOG_COL_SQL_TIME"),
    API_COUNT("API Count", 50, SWT.RIGHT, true, true, true, true, "XLOG_COL_SQL_COUNT"),
    API_TIME("API Time", 50, SWT.RIGHT, true, true, true, true, "XLOG_COL_SQL_TIME"),
    KBYTES("KBytes", 60, SWT.RIGHT, true, true, true, true, "XLOG_COL_KBYTES"),
    IP("IP", 90, SWT.LEFT, true, true, false, true, "XLOG_COL_IP"),
    LOGIN("Login", 50, SWT.LEFT, true, true, false, true, "XLOG_COL_LOGIN"),
    DESC("Desc", 50, SWT.LEFT, true, true, false, true, "XLOG_COL_DESC"),
    TEXT1("Text1", 50, SWT.LEFT, true, true, false, true, "XLOG_COL_TEXT1"),
    TEXT2("Text2", 50, SWT.LEFT, true, true, false, true, "XLOG_COL_TEXT2"),
    TEXT3("Text3", 50, SWT.LEFT, true, true, false, false, "XLOG_COL_TEXT3"),
    TEXT4("Text4", 50, SWT.LEFT, true, true, false, false, "XLOG_COL_TEXT4"),
    TEXT5("Text5", 50, SWT.LEFT, true, true, false, false, "XLOG_COL_TEXT5"),
    ERROR("Error", 50, SWT.LEFT, true, true, false, true, "XLOG_COL_ERROR"),
    DUMP("Dump", 40, SWT.CENTER, true, true, false, true, "XLOG_COL_DUMP"),
    TX_ID("Txid", 30, SWT.LEFT, true, true, false, true, "XLOG_COL_TX_ID"),
    GX_ID("Gxid", 30, SWT.LEFT, true, true, false, true, "XLOG_COL_GX_ID"),
    START_TIME("StartTime", 70, SWT.CENTER, true, true, true, true, "XLOG_COL_START_TIME"),
    UA("UA", 70, SWT.LEFT, true, true, false, false, "XLOG_COL_UA"),
    COUNTRY("Country", 40, SWT.LEFT, true, true, false, false, "XLOG_COL_COUNTRY"),
    CITY("City", 40, SWT.LEFT, true, true, false, false, "XLOG_COL_CITY"),
    GROUP("Group", 40, SWT.LEFT, true, true, false, false, "GROUP"),
    ;

    private final String title;
    private int width;
    private final int alignment;
    private final boolean resizable;
    private final boolean moveable;
    private final boolean isNumber;
    private final boolean defaultVisible;
    private final String internalID;

    private static final long serialVersionUID = -1477341833201236951L;

    XLogColumnEnum(String text, int width, int alignment, boolean resizable, boolean moveable, boolean isNumber, boolean defaultVisible, String internalID) {
        this.title = text;
        this.width = width;
        this.alignment = alignment;
        this.resizable = resizable;
        this.moveable = moveable;
        this.isNumber = isNumber;
        this.defaultVisible = defaultVisible;
        this.internalID = internalID;
    }

    public String getTitle(){
        return title;
    }

    public int getAlignment(){
        return alignment;
    }

    public boolean isResizable(){
        return resizable;
    }

    public boolean isMoveable(){
        return moveable;
    }

    public int getWidth() {
        return width;
    }

    public boolean isNumber() {
        return this.isNumber;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public boolean isDefaultVisible() {
        return defaultVisible;
    }

    public String getInternalID() {
        return internalID;
    }

    public static XLogColumnEnum findByTitle(String title) {
        for (XLogColumnEnum columnEnum : XLogColumnEnum.values()) {
            if (columnEnum.getTitle().equals(title)) {
                return columnEnum;
            }
        }
        throw new RuntimeException(String.format("[FATAL] Invalid XLogColumn title : <%s>", title));
    }
}
