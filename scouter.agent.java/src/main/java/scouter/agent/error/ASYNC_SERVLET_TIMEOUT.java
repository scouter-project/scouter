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

@SuppressWarnings("serial")
public class ASYNC_SERVLET_TIMEOUT extends RuntimeException {

	public ASYNC_SERVLET_TIMEOUT() {
	}

	public ASYNC_SERVLET_TIMEOUT(String message) {
		super(message);
	}

	public ASYNC_SERVLET_TIMEOUT(Throwable cause) {
		super(cause);
	}

	public ASYNC_SERVLET_TIMEOUT(String message, Throwable cause) {
		super(message, cause);
	}

}
