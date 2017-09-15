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

/**
 * Created by Gun Lee(gunlee01@gmail.com) on 2017. 8. 25.
 */
@Getter
public class ErrorStateBizException extends ErrorStateException {
	public ErrorStateBizException(ErrorState errorState) {
		super(errorState);
	}

	public ErrorStateBizException(ErrorState errorState, String message) {
		super(errorState, message);
	}

	public ErrorStateBizException(ErrorState errorState, String message, Throwable cause) {
		super(errorState, message, cause);
	}
}
