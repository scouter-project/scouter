package scouter.agent.error;

import java.sql.SQLException;

public class CONNECTION_OPEN_FAIL extends SQLException {

	public CONNECTION_OPEN_FAIL() {
	}

	public CONNECTION_OPEN_FAIL(String reason) {
		super(reason);
	}

	public CONNECTION_OPEN_FAIL(Throwable cause) {
		super(cause);
	}

	public CONNECTION_OPEN_FAIL(String reason, String SQLState) {
		super(reason, SQLState);
	}

	public CONNECTION_OPEN_FAIL(String reason, Throwable cause) {
		super(reason, cause);
	}

	public CONNECTION_OPEN_FAIL(String reason, String SQLState, int vendorCode) {
		super(reason, SQLState, vendorCode);
	}

	public CONNECTION_OPEN_FAIL(String reason, String sqlState, Throwable cause) {
		super(reason, sqlState, cause);
		// TODO Auto-generated constructor stub
	}

	public CONNECTION_OPEN_FAIL(String reason, String sqlState, int vendorCode, Throwable cause) {
		super(reason, sqlState, vendorCode, cause);
	}

}
