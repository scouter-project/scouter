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

package scouter.xtra.tools;

import scouter.agent.batch.proxy.IToolsMain;
import scouter.lang.AlertLevel;
import scouter.util.FileUtil;
import scouter.util.SysJMX;
import sun.tools.attach.HotSpotVirtualMachine;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ToolsMain implements IToolsMain {
	private static JVM jvm = null;

	public synchronized void heaphisto(PrintWriter out) {
		try {
			if (jvm == null) {
				jvm = new JVM(Integer.toString(SysJMX.getProcessPID()));
				jvm.connect();
			}
			if (jvm.isConnected() == false)
				return;

			HotSpotVirtualMachine vm = (HotSpotVirtualMachine) jvm.getVM();
			InputStream in = vm.heapHisto("-all");
			BufferedReader bin = null;
			try {

				bin = new BufferedReader(new InputStreamReader(in));
				String line = null;
				while (true) {
					line = bin.readLine();
					if (line == null)
						break;
					out.println(line);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				FileUtil.close(in);
			}
		} catch (Exception e) {
			jvm = null;
			e.printStackTrace();
//			sendAlert();
			throw new RuntimeException(getJvmErrMsg());
		}
	}

	public synchronized List<String> heaphisto(int skip, int limit, String filter) {
		List<String> out = new ArrayList<String>();
		try {
			if (jvm == null) {
				jvm = new JVM(Integer.toString(SysJMX.getProcessPID()));
				jvm.connect();
			}
			if (jvm.isConnected() == false)
				return out;

			boolean isLive = "live".equalsIgnoreCase(filter);
			HotSpotVirtualMachine vm = (HotSpotVirtualMachine) jvm.getVM();
			InputStream in = vm.heapHisto(isLive ? "-live" : "-all");

			String line = null;
			BufferedReader bin = null;
			try {
				bin = new BufferedReader(new InputStreamReader(in));
				// skip header
				for (int i = 0; i < 3; i++) {
					line = bin.readLine();
				}
				for (int i = 0; i < limit + skip; i++) {
					line = bin.readLine();
					if (line == null)
						break;
					if (i >= skip) {
						out.add(line);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				FileUtil.close(in);
			}
			return out;
		} catch (Exception e) {
			jvm = null;
			e.printStackTrace();
//			sendAlert();
			throw new RuntimeException(getJvmErrMsg());
		}
	}

	public synchronized void threadDump(PrintWriter out) {
		try {
			if (jvm == null) {
				jvm = new JVM(Integer.toString(SysJMX.getProcessPID()));
				jvm.connect();
			}
			if (jvm.isConnected() == false)
				return;

			HotSpotVirtualMachine vm = (HotSpotVirtualMachine) jvm.getVM();
			InputStream in = vm.remoteDataDump();

			String line = null;
			BufferedReader bin = null;
			try {
				bin = new BufferedReader(new InputStreamReader(in));
				while (true) {
					line = bin.readLine();
					if (line == null)
						break;
					out.println(line);
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				FileUtil.close(in);
			}
		} catch (Exception e) {
			jvm = null;
			e.printStackTrace();
	//		sendAlert();
			throw new RuntimeException(getJvmErrMsg());
		}
	}

	public synchronized List<String> threadDump(int skip, int limit) {
		List<String> out = new ArrayList<String>();

		try {
			if (jvm == null) {
				jvm = new JVM(Integer.toString(SysJMX.getProcessPID()));
				jvm.connect();
			}
			if (jvm.isConnected() == false)
				return out;

			HotSpotVirtualMachine vm = (HotSpotVirtualMachine) jvm.getVM();
			InputStream in = vm.remoteDataDump();

			String line = null;
			BufferedReader bin = null;
			try {
				bin = new BufferedReader(new InputStreamReader(in));
				for (int i = 0; i < limit + skip; i++) {
					line = bin.readLine();
					if (line == null)
						break;
					if (i >= skip) {
						out.add(line);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				FileUtil.close(in);
			}
			return out;
		} catch (Exception e) {
			jvm = null;
			e.printStackTrace();
//			sendAlert();
			throw new RuntimeException(getJvmErrMsg());
		}
	}
/*
	private void sendAlert() {
		AlertProxy.sendAlert(AlertLevel.WARN, "Fail to JVM attachment", "Can not attach to the JVM. \n" +
				"Add this command line option for getting thread dump, heap histo and some another abilities " +
				"if your java version is 9+ : -Djdk.attach.allowAttachSelf=true");
	}
*/
	private String getJvmErrMsg() {
		String javaHome = System.getProperty("java.home");
		StringBuilder sb = new StringBuilder();
		sb.append("The JAVA_HOME environment variable is not defined correctly\n");
		sb.append("This environment variable is needed to run this program\n");
		sb.append("NB: JAVA_HOME should point to a JDK not a JRE (" + javaHome + ")");
		return sb.toString();
	}
}