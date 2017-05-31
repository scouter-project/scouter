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

import scouter.agent.netio.request.anotation.RequestHandler;
import scouter.agent.proxy.ToolsMainFactory;
import scouter.agent.util.DumpUtil;
import scouter.lang.pack.Pack;
import scouter.net.RequestCmd;

public class AgentHeapHisto {

	@RequestHandler(RequestCmd.OBJECT_HEAPHISTO)
	public Pack heaphisto(Pack param) {
		try {
			return ToolsMainFactory.heaphisto(param);
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return null;
	}

	@RequestHandler(RequestCmd.TRIGGER_HEAPHISTO)
	public Pack tirgger_heaphisto(Pack param) {
		return DumpUtil.triggerHeapHisto();
	}

}