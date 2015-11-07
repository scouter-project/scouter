package scouter.agent.error;

import java.sql.SQLException;

public class SLOW_SQL extends SQLException {

	public SLOW_SQL() {
	}

	public SLOW_SQL(String reason) {
		super(reason);
	}

	public SLOW_SQL(Throwable cause) {
		super(cause);
	}

	public SLOW_SQL(String reason, String SQLState) {
		super(reason, SQLState);
	}

	public SLOW_SQL(String reason, Throwable cause) {
		super(reason, cause);
	}

	public SLOW_SQL(String reason, String SQLState, int vendorCode) {
		super(reason, SQLState, vendorCode);
	}

	public SLOW_SQL(String reason, String sqlState, Throwable cause) {
		super(reason, sqlState, cause);
		// TODO Auto-generated constructor stub
	}

	public SLOW_SQL(String reason, String sqlState, int vendorCode, Throwable cause) {
		super(reason, sqlState, vendorCode, cause);
	}

}
