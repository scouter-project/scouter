/*
 *  Copyright 2015 LG CNS.
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
package scouter.server;


import java.util.List;
import java.util.Vector;
import scouter.util.IShutdown;



public class ShutdownManager {

	private static List<IShutdown> instances = new Vector<IShutdown>();

	public static void add(IShutdown instance) {
		instances.add(instance);
	}

	public synchronized static void shutdown() {
		if (instances.size() == 0) {
			return;
		}
		Logger.println("S180", "Server Shutdown");
		for (int i = 0; i < instances.size(); i++) {		
			instances.get(i).shutdown();
		   Logger.println("S181", "Shutdown " + instances.get(i) + " ...");
		}
		instances.clear();
	}
}
