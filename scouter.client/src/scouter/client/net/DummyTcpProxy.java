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
 *
 */
package scouter.client.net;

import java.util.ArrayList;
import java.util.List;

import scouter.lang.pack.Pack;
import scouter.lang.value.Value;

public class DummyTcpProxy extends TcpProxy {
	
	public DummyTcpProxy() {}

	public List<Pack> process(String cmd, Pack param) {
		return new ArrayList<Pack>(0);
	}

	public synchronized void process(String cmd, Object param, INetReader recv) {
	}
	
	public Pack getSingle(String cmd, Pack param) {
		return null;
	}

	public Value getSingleValue(String cmd, Pack param) {
		return null;
	}

	public List<Value> processValues(String cmd, Pack param) {
		return new ArrayList<Value>(0);
	}
}
