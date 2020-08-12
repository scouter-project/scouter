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

package scouter.test;

import scouter.AnyTrace;
import scouter.agent.AgentBoot;
import scouter.agent.Configure;
import scouter.agent.netio.data.net.TcpRequestMgr;
import scouter.agent.trace.TraceContext;
import scouter.agent.trace.TraceContextManager;
import scouter.agent.trace.TraceMain;
import scouter.io.DataOutputX;
import scouter.lang.pack.XLogPack;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.HashUtil;
import scouter.util.IPUtil;
import scouter.util.KeyGen;
import scouter.util.ShellArg;
import scouter.util.SysJMX;
import scouter.util.ThreadUtil;

import java.util.Random;

public class TpsRush {
	public static void main2(String[] args) {

		ShellArg sh = new ShellArg(args);
		String server = sh.get("-h", "127.0.0.1");
		String port = sh.get("-p", "6100");
		int tps =CastUtil.cint(sh.get("-tps","1000"));
		String type = sh.get("-type", "java");
		String name = sh.get("-name", "java"+SysJMX.getProcessPID());
		System.setProperty("obj_type", type);
		System.setProperty("obj_name", name);
		
		System.setProperty("server.addr", server);
		System.setProperty("server.port", port);

		
		System.out.println("Scouter Test Simulation!!");
		System.out.println("  server = " + server + ":" +port);
		System.out.println("  tcp = " + tps);
		
		AgentBoot.boot();
		//RequestAgent.getInstance();
		TcpRequestMgr.getInstance();
		
		double interval = 1000.0/tps;
		
		long now = System.currentTimeMillis();
		
		Random r = new Random();

		long txcount = 0 ;
		double tm = 0;
		long last_unit = 0;
		while (true) {
			txcount++;
			
			String serviceName = "fdafafdafdafdafdfdsafaafda" + (next(r, 100000));
			int service_hash = HashUtil.hash(serviceName);
			long txid = KeyGen.next();
			profile(txid,service_hash);
			long endtime = System.currentTimeMillis();
			
			
			int elapsed =next(r, 10000);
			int cpu = next(r, 10000);
			int sqlCount = next(r, 100);
			int sqlTime = next(r, 1000);
			String remoteAddr = IPUtil.toString(DataOutputX.toBytes(next(r,255)));
			String error = null;
			long visitor = KeyGen.next();
			
			
			
			XLogPack pack = TraceMain.txperf(endtime, txid, service_hash,serviceName, elapsed, cpu, sqlCount, sqlTime, remoteAddr, error, visitor);
			TraceMain.metering(pack);
			
		    long unit=endtime/5000;
		    if(last_unit!=unit){
		    	last_unit = unit;
		    	System.out.println(DateUtil.timestamp(endtime) + "  exe-tx=" + txcount+ "  " + Configure.getInstance().getObjName());
		    }
			tm = tm+interval;
			if(tm>1){
				ThreadUtil.sleep((int)tm);
			     tm = tm - ((int)tm);
			}
			long x = System.currentTimeMillis();
			if(x-now >1000000)
				break;
		}
		ThreadUtil.sleep(100000);
	}

	private static void profile(long txid, int serviceHash) {
		TraceContext ctx = new TraceContext(false);
		ctx.txid=txid;
		ctx.serviceHash=serviceHash;
		ctx.startTime=System.currentTimeMillis();
		ctx.thread = Thread.currentThread();
		
		TraceContextManager.start(ctx);
		
		AnyTrace.message("profile 1");
		AnyTrace.message("profile 2");
		
		ctx.profile.close(true);
		TraceContextManager.end(ctx);
	}

	private static int next(Random r, int max) {
		return Math.abs(r.nextInt() % max);
	}
}
