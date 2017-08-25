package scouterx.webapp.api.exception;
/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 */

import javax.ws.rs.core.Response;

/**
 * Created by Gun Lee(gunlee01@gmail.com) on 2017. 8. 25.
 */
public enum ErrorState {
	INTERNAL_SERVER_ERRROR(Response.Status.INTERNAL_SERVER_ERROR, 500, "internal server error"),
	;

	private Response.Status status;
	private int errorCode;
	private String errorMessage;

	ErrorState(Response.Status status, int errorCode, String errorMessage) {
		this.status = status;
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}

	public ErrorStateException newException() {
		return new ErrorStateException(this);
	}

	public ErrorStateException newException(String message) {
		return new ErrorStateException(this);
	}

	public ErrorStateException newException(String message, Throwable t) {
		return new ErrorStateException(this, message, t);
	}

	public ErrorStateBizException newBizException() {
		return new ErrorStateBizException(this);
	}

	public ErrorStateBizException newBizException(String message) {
		return new ErrorStateBizException(this);
	}

	public ErrorStateBizException newBizException(String message, Throwable t) {
		return new ErrorStateBizException(this, message, t);
	}
}








