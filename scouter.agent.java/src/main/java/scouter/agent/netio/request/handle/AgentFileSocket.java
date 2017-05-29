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
import scouter.agent.trace.SocketTable;
import scouter.io.DataOutputX;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.BlobValue;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.BitUtil;
import scouter.util.LongEnumer;
import scouter.util.RunExec;
import scouter.util.StringUtil;
import scouter.util.SysJMX;
import scouter.util.SystemUtil;

public class AgentFileSocket {
	String linux_file = "lsof -Pp " + SysJMX.getProcessPID();

	@RequestHandler(RequestCmd.OBJECT_FILE_SOCKET)
	public Pack getFileList(Pack param) {
		MapPack p = new MapPack();
		if (SystemUtil.IS_LINUX == false && SystemUtil.IS_MAC == false) {
			p.put("status", -2);
			p.put("error", "not supported os " + SystemUtil.OS_NAME);
			return p;
		}
		RunExec re = new RunExec(linux_file);
		int status = re.exec();
		p.put("status", status);
		if (re.getOutput() != null) {
			p.put("data", re.getOutput());
		}
		if (re.getError() != null) {
			p.put("error", re.getError());
		}
		if (re.getException() != null) {
			p.put("exception", re.getException().getMessage());
		}

		return p;
	}

	@RequestHandler(RequestCmd.OBJECT_SOCKET)
	public Pack getSocketList(Pack param) {
		long order = ((MapPack) param).getLong("key");

		MapPack p = new MapPack();
		ListValue keyLv = p.newList("key");
		ListValue hostLv = p.newList("host");
		ListValue portLv = p.newList("port");
		ListValue countLv = p.newList("count");
		ListValue serviceLv = p.newList("service");
		ListValue txidLv = p.newList("txid");
		ListValue orderLv = p.newList("order");
		ListValue stackLv = p.newList("stack");

		LongEnumer en = SocketTable.socketMap.keys();
		while (en.hasMoreElements()) {
			long key = en.nextLong();
			SocketTable.Info fo = SocketTable.socketMap.get(key);
			if (fo == null)
				continue;
			
			if (key == order || order == Long.MAX_VALUE) {
				fo.stackOrder = true;
			}

			keyLv.add(key);
			hostLv.add(new BlobValue(DataOutputX.toBytes(BitUtil.getHigh(key))));
			portLv.add(BitUtil.getLow(key));
			countLv.add(fo.count);
			serviceLv.add(fo.service);
			txidLv.add(fo.txid);
			orderLv.add(fo.stackOrder);
			stackLv.add(StringUtil.trimEmpty(fo.stack));
		}
		return p;
	}

}