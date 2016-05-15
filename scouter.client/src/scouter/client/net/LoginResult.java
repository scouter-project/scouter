package scouter.client.net;

import scouter.util.StringUtil;

public class LoginResult {
	
	public boolean success;
	public String errorMessage;
	
	public String getErrorMessage() {
		if (!success && StringUtil.isEmpty(errorMessage)) {
			return "Failure to unknown causes";
		}
		return errorMessage;
	}

}
