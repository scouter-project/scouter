/*
 *  Copyright 2016 the original author or authors. 
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

package scouter.agent.batch.netio.data.net;

import scouter.agent.batch.Configure;
import scouter.agent.batch.trace.TraceContext;

import scouter.io.DataOutputX;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;

public class UdpLocalAgent {
	static public void sendUdpPackToServer(Pack pack){
		Configure conf = Configure.getInstance();
		try {
			UdpAgent.sendUdpPackToServer(conf.net_collector_ip, conf.net_collector_udp_port, pack);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	static public void sendDumpFileInfo(TraceContext traceContext){
		Configure conf = Configure.getInstance();
		try {
			DataOutputX out = new DataOutputX();
			out.writeInt(BatchNetFlag.BATCH_END_DUMPFILE_INFO);
			out.writeLong(traceContext.startTime);
			out.writeText(conf.getObjName());
			out.writeText(traceContext.getLogFullFilename());
			UdpAgent.sendUdp("127.0.0.1", conf.net_local_udp_port, out.toByteArray());
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	static public void sendRunningInfo(TraceContext traceContext){
		MapPack mapPack = traceContext.caculateTemp();
		Configure conf = Configure.getInstance();
		try {
			DataOutputX output = new DataOutputX();
			output.writeInt(BatchNetFlag.BATCH_RUNNING_INFO);
			mapPack.write(output);
			UdpAgent.sendUdp("127.0.0.1", conf.net_local_udp_port, output.toByteArray());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static public void sendEndInfo(TraceContext traceContext){
		Configure conf = Configure.getInstance();
		try {
			DataOutputX out = new DataOutputX();
			out.writeInt(BatchNetFlag.BATCH_END_INFO);
			out.writeText(traceContext.batchJobId);
			out.writeInt(traceContext.pID);
			out.writeLong(traceContext.startTime);
			UdpAgent.sendUdp("127.0.0.1", conf.net_local_udp_port, out.toByteArray());
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}	
}