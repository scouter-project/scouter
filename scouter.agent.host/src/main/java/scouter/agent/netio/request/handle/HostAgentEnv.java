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
package scouter.agent.netio.request.handle;

import scouter.agent.Configure;
import scouter.agent.netio.request.anotation.RequestHandler;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.TextValue;
import scouter.net.RequestCmd;

import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class HostAgentEnv {

	private static Configure conf = Configure.getInstance();

	@RequestHandler(RequestCmd.OBJECT_ENV)
	public Pack getAgentEnv(Pack param) {
		MapPack m = new MapPack();
		Properties p = System.getProperties();
		@SuppressWarnings("rawtypes")
		Enumeration en = p.keys();
		while (en.hasMoreElements()) {
			String key = (String) en.nextElement();
			String value = p.getProperty(key);
			m.put(key, new TextValue(value));
		}
		Map<String, String> envs = System.getenv();
		Set<String> keys = envs.keySet();
		for (String key : keys) {
			m.put(key, envs.get(key));
		}
		return m;
	}

	@RequestHandler(RequestCmd.OBJECT_SET_KUBE_SEQ)
	public Pack setKubePodSeq(Pack param) {
		long seq = ((MapPack)param).getLong("seq");
		if (seq != conf.getSeqNoForKube()) {
			conf.setSeqNoForKube(seq);
			conf.resetObjInfo();
		}

		return new MapPack();
	}

}
