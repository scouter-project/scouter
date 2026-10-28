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
package scouter.agent.error;

/**
 * Thrown(marked) when a request is rejected by the duplicated client-ip control.
 */
@SuppressWarnings("serial")
public class REQUEST_REJECT_DUPLICATED_IP extends Error {

	public REQUEST_REJECT_DUPLICATED_IP() {
	}

	public REQUEST_REJECT_DUPLICATED_IP(String message) {
		super(message);
	}

	public REQUEST_REJECT_DUPLICATED_IP(Throwable cause) {
		super(cause);
	}

	public REQUEST_REJECT_DUPLICATED_IP(String message, Throwable cause) {
		super(message, cause);
	}
}
