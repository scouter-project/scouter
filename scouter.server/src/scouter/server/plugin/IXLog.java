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

package scouter.server.plugin;

import scouter.lang.TextTypes;
import scouter.lang.pack.ObjectPack;
import scouter.lang.pack.XLogPack;
import scouter.server.Configure;
import scouter.server.core.AgentManager;
import scouter.server.core.app.XLogGroupUtil;
import scouter.server.db.TextPermRD;
import scouter.server.db.TextPermWR;
import scouter.server.db.TextRD;
import scouter.server.geoip.GeoIpUtil;
import scouter.util.DateUtil;
import scouter.util.HashUtil;
import scouter.util.IntLinkedSet;

public class IXLog extends IPlugIn {
	public void process(XLogPack p) {
	}

	public void setAutoGroup(XLogPack p) {
		XLogGroupUtil.process(p);
	}

	public void setLocation(XLogPack p) {
		if (Configure.getInstance().geoip_enabled) {
			GeoIpUtil.setNationAndCity(p);
		}
	}
	public String objName(XLogPack p) {
		return AgentManager.getAgentName(p.objHash);
	}
	public String objType(XLogPack p) {
		ObjectPack a = AgentManager.getAgent(p.objHash);
		if (a != null) {
			return a.objType;
		}
		return null;
	}
	public String service(XLogPack p) {
		return TextRD.getString(DateUtil.yyyymmdd(p.endTime), TextTypes.SERVICE, p.service);
	}

	public String error(XLogPack p) {
		return TextRD.getString(DateUtil.yyyymmdd(p.endTime), TextTypes.ERROR, p.error);
	}

	public String userAgent(XLogPack p) {
		return TextPermRD.getString(TextTypes.USER_AGENT, p.userAgent);
	}

	public String desc(XLogPack p) {
		return TextPermRD.getString(TextTypes.DESC, p.desc);
	}

	public String referer(XLogPack p) {
		return TextPermRD.getString(TextTypes.REFERER, p.desc);
	}

	public String login(XLogPack p) {
		return TextPermRD.getString(TextTypes.LOGIN, p.login);
	}

	public String group(XLogPack p) {
		return TextPermRD.getString(TextTypes.GROUP, p.group);
	}

	private static IntLinkedSet saved = new IntLinkedSet().setMax(1000);

	public int addGroup(String groupName) {
		int grpHash = HashUtil.hash(groupName);
		if (saved.contains(grpHash) == false) {
			TextPermWR.add(TextTypes.GROUP, grpHash, groupName);
		}
		return grpHash;
	}

}