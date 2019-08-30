/*
 *  Copyright 2015 the original author or authors.
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package scouterx.webapp.framework.exception;

import lombok.Getter;

import javax.ws.rs.core.Response;

/**
 * Created by Gun Lee(gunlee01@gmail.com) on 2017. 8. 25.
 */
@Getter
public enum ErrorState {
	//For System
	SESSION_CLOSED(Response.Status.INTERNAL_SERVER_ERROR, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "internal server error"),
	CLIENT_SOCKET_CLOSED(Response.Status.INTERNAL_SERVER_ERROR, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "internal server error"),

	//For User
	INTERNAL_SERVER_ERROR(Response.Status.INTERNAL_SERVER_ERROR, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "internal server error"),
	COLLECTOR_NOT_CONNECTED(Response.Status.INTERNAL_SERVER_ERROR, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "scouter collector not connected"),
	COLLECTOR_INVALID_SESSION(Response.Status.INTERNAL_SERVER_ERROR, Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "internal server error"),

	ILLEGAL_KEY_ACCESS(Response.Status.BAD_REQUEST, Response.Status.BAD_REQUEST.getStatusCode(), "illegal key access error"),

	LOGIN_REQUIRED(Response.Status.UNAUTHORIZED, Response.Status.UNAUTHORIZED.getStatusCode(), "login required."),
	LOGIN_FAIL(Response.Status.NOT_FOUND, Response.Status.NOT_FOUND.getStatusCode(), "id or password is incorrect."),
	SESSION_EXPIRED(Response.Status.UNAUTHORIZED, Response.Status.UNAUTHORIZED.getStatusCode(), "authorization token or session is expired."),
	NOT_IMPLEMENTED(Response.Status.NOT_IMPLEMENTED, Response.Status.NOT_IMPLEMENTED.getStatusCode(), "This API is not yet implemented."),
	VALIDATE_ERROR(Response.Status.BAD_REQUEST, Response.Status.BAD_REQUEST.getStatusCode(), "fail to validate input parameters. : "),
	;

	private final Response.Status status;
	private final int errorCode;
	private final String errorMessage;

	ErrorState(Response.Status status, int errorCode, String errorMessage) {
		this.status = status;
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}

	public ErrorStateException newException() {
		return new ErrorStateException(this);
	}

	public ErrorStateException newException(String message) {
		return new ErrorStateException(this, message);
	}

	public ErrorStateException newException(String message, Throwable t) {
		return new ErrorStateException(this, message, t);
	}

	public ErrorStateBizException newBizException() {
		return new ErrorStateBizException(this);
	}

	public ErrorStateBizException newBizException(String message) {
		return new ErrorStateBizException(this, message);
	}

	public ErrorStateBizException newBizException(String message, Throwable t) {
		return new ErrorStateBizException(this, message, t);
	}

	public static void throwNotImplementedException() {
		throw NOT_IMPLEMENTED.newBizException();
	}
}








