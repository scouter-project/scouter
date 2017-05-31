/*
 *  Copyright 2015 Scouter Project.
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
package scouter.agent;

import scouter.agent.counter.CounterExecutingManager;
import scouter.agent.netio.request.ReqestHandlingProxy;
import scouter.agent.plugin.PluginLoader;
import scouter.util.Hexa32;
import scouter.util.KeyGen;
import scouter.util.SysJMX;

public class AgentBoot implements Runnable {
	public void run() {
		boot();
	}

	private static boolean booted = false;

	public synchronized static void boot() {
		if (booted)
			return;
		booted = true;
			
		CounterExecutingManager.load();
		ReqestHandlingProxy.load(ReqestHandlingProxy.class);
		
		Configure.getInstance().printConfig();
		
		long seed =System.currentTimeMillis() ^ (((long)SysJMX.getProcessPID())<<32);
		KeyGen.setSeed(seed);
		Logger.println("A100", "agent boot seed="+Hexa32.toString32(seed));
		PluginLoader.getInstance();
	}
	public static void main(String[] args) {
	   boot();
	}
}
