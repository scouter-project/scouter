package scouter.agent.error;

import java.sql.SQLException;

public class TOO_MANY_RECORDS extends SQLException {

    public TOO_MANY_RECORDS() {
    }

    public TOO_MANY_RECORDS(String reason) {
        super(reason);
    }

    public TOO_MANY_RECORDS(Throwable cause) {
        super(cause);
    }

    public TOO_MANY_RECORDS(String reason, String SQLState) {
        super(reason, SQLState);
    }

    public TOO_MANY_RECORDS(String reason, Throwable cause) {
        super(reason, cause);
    }

    public TOO_MANY_RECORDS(String reason, String SQLState, int vendorCode) {
        super(reason, SQLState, vendorCode);
    }

    public TOO_MANY_RECORDS(String reason, String sqlState, Throwable cause) {
        super(reason, sqlState, cause);
        // TODO Auto-generated constructor stub
    }

    public TOO_MANY_RECORDS(String reason, String sqlState, int vendorCode, Throwable cause) {
        super(reason, sqlState, vendorCode, cause);
    }

}
