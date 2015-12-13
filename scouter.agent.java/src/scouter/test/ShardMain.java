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

import java.util.Random;

import scouter.agent.AgentBoot;
import scouter.agent.Configure;
import scouter.agent.netio.data.net.TcpRequestMgr;
import scouter.agent.trace.TraceMain;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.HashUtil;
import scouter.util.KeyGen;
import scouter.util.ShellArg;
import scouter.util.ThreadUtil;

public class ShardMain {
	public static void main(String[] args) {

		ShellArg sh = new ShellArg(args);
		String server = sh.get("-h", "127.0.0.1");
		String port = sh.get("-p", "6101");

		System.setProperty("server.addr", server);
		System.setProperty("server.port", port);

		int tps = CastUtil.cint(sh.get("-t", "10"));

		System.out.println("Scouter Test Simulation!!");
		System.out.println("  server = " + server + ":" + port);
		System.out.println("  tcp = " + tps);

		AgentBoot.boot();
		// RequestAgent.getInstance();
		TcpRequestMgr.getInstance();

		double interval = 1000.0 / tps;

		Random r = new Random();

		long txcount = 0;
		double tm = 0;
		long last_unit = 0;
		while (true) {
			txcount++;
			long endtime = System.currentTimeMillis();
			long txid = KeyGen.next();
			String serviceName = "service" + (next(r, 10));
			int service_hash = HashUtil.hash(serviceName);
			int elapsed = next(r, 10000);
			int cpu = next(r, 10000);
			int sqlCount = next(r, 100);
			int sqlTime = next(r, 100);
			String remoteAddr = "27.11.11.2";
			String error = null;
			long visitor = KeyGen.next();
			TraceMain.txperf(endtime, txid, service_hash, serviceName, elapsed, cpu, sqlCount, sqlTime, remoteAddr,
					error, visitor);

			long unit = endtime / 5000;
			if (last_unit != unit) {
				last_unit = unit;
				System.out.println(DateUtil.timestamp(endtime) + "  exe-tx=" + txcount + "  "
						+ Configure.getInstance().getObjName());
			}
			tm = tm + interval;
			if (tm > 1) {
				ThreadUtil.sleep((int) tm);
				tm = tm - ((int) tm);
			}
		}
	}

	private static int next(Random r, int max) {
		return Math.abs(r.nextInt() % max);
	}
}