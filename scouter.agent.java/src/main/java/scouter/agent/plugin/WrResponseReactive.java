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
package scouter.agent.plugin;

import scouter.agent.Logger;

import java.io.PrintWriter;

public class WrResponseReactive extends WrResponse {

	private Object resObject;

	private Throwable _error;

	private boolean enabled = true;

	public WrResponseReactive(Object res) {
		super(res);
		resObject = res;
	}

	public PrintWriter getWriter() {
		if (enabled == false)
			return null;
		try {
			//TODO
			return null;
		} catch (Throwable e) {
			enabled = false;
			_error = e;
			Logger.println("A173", e);
			return null;
		}
	}

	public String getContentType() {
		if (enabled == false)
			return null;
		try {
			//TODO
			return null;
		} catch (Throwable e) {
			enabled = false;
			_error = e;
			Logger.println("A174", e);
			return null;
		}
	}

	public String getCharacterEncoding() {
		if (enabled == false)
			return null;
		try {
			//TODO
			return null;
		} catch (Throwable e) {
			enabled = false;
			_error = e;
			Logger.println("A175", e);
			return null;
		}
	}

	public Object inner() {
		return this.resObject;
	}

	public boolean isOk() {
		return enabled;
	}

	public Throwable error() {
		return _error;
	}

}
