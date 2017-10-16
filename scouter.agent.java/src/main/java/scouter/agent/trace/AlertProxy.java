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
package scouter.agent.trace;

import scouter.agent.Configure;
import scouter.agent.netio.data.DataProxy;
import scouter.lang.value.MapValue;
import scouter.util.LinkedMap;
import scouter.util.StringUtil;

public class AlertProxy {
	// 타이틀별로 구분하여 최대 200가지에 대한 10초이내 중복전송을 막는다.
	private static LinkedMap<String, Long> sendTimeTable = new LinkedMap<String, Long>().setMax(200);

	static Configure conf = Configure.getInstance();

	public static void sendAlert(byte level, String title, String emsg) {
		sendAlert(level, title, emsg, null);
	}
	
	public static void sendAlert(byte level, String title, String emsg, MapValue tags) {
		long now = System.currentTimeMillis();

		if (title == null)
			title = "none";

		Long last = sendTimeTable.get(title);

		if (last == null || now - last.longValue() >= conf.alert_send_interval_ms) {
			sendTimeTable.put(title, now);
			DataProxy.sendAlert(level, title, StringUtil.limiting(emsg, conf.alert_message_length), tags);
		}
	}
}