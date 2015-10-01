/*
 *  Copyright 2015 the original author or authors.
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
package scouter.setup;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import scouter.agent.Logger;
import scouter.agent.util.ManifestUtil;
import scouter.util.ShellArg;
import scouter.util.StringUtil;
import scouter.util.SysJMX;
public class InternalMain {
	public static void main(String[] args) throws Exception {
		ShellArg arg = new ShellArg(args);
		if (arg.hasKey("-pid") == false && arg.hasKey("-name") == false) {
			Logger.info("Please check options");
			Logger.info("ex) java -cp " + ManifestUtil.getThisJarName() + " " + Main.class.getName()
					+ " -name <word>  -opt \"options\"");
			Logger.info("    java -cp " + ManifestUtil.getThisJarName() + " " + Main.class.getName()
					+ " -pid <pid> -opt \"options\"");
			return;
		}
		String opts = arg.get("-opt");
		String agentjar = null;
		try {
			agentjar = ManifestUtil.getThisJarName();
			Logger.info("AgentJar: " + agentjar);
			if (StringUtil.isNotEmpty(opts)) {
				Logger.info("opt: " + opts);
			}
			process(arg, agentjar, opts);
		} catch (ClassNotFoundException c) {
			Logger.info("This module needs the tools.jar, please check  ${java.home}/lib");
			Logger.info("java.home=" + System.getProperty("java.home"));
		}
	}
	private static void process(ShellArg arg, String agentjar, String option) throws ClassNotFoundException, Exception {
		Class classVM = Class.forName("com.sun.tools.attach.VirtualMachine");
		Class classVMD = Class.forName("com.sun.tools.attach.VirtualMachineDescriptor");
		String pid = arg.get("-pid");
		String name = arg.get("-name");
		String thisPid = Integer.toString(SysJMX.getProcessPID());
		List vmdList = (List) Proxy.invoke(classVM, null, "list");
		Set<String> usedNames = null;
		String prefix = arg.get("-scouter.name");
		if (prefix != null) {
			usedNames = new HashSet<String>();
			for (int i = 0; i < vmdList.size(); i++) {
				Object vmd = vmdList.get(i);
				String id = (String) Proxy.invoke(classVMD, vmd, "id");
				if (thisPid.equals(id))
					continue;
				String loadedName = getScouterName(classVM, agentjar, id);
				if (loadedName != null) {
					usedNames.add(loadedName);
				}
			}
		}
		for (int i = 0; i < vmdList.size(); i++) {
			Object vmd = vmdList.get(i);
			String id = (String) Proxy.invoke(classVMD, vmd, "id");
			String desc = (String) Proxy.invoke(classVMD, vmd, "displayName");
			if (thisPid.equals(id))
				continue;
			desc = trimDesc(desc);
			if (desc.length() == 0)
				continue;
			if (StringUtil.isNotEmpty(pid)) {
				if (pid.equals(id)) {
					loadAgent(classVM, agentjar, id, desc, option);
				}
			} else {
				if (desc.indexOf(name) >= 0) {
					if (prefix != null) {
						int n = 0;
						String newname = prefix + n;
						while (usedNames.contains(newname)) {
							newname = prefix + (++n);
						}
						usedNames.add(newname);
						String option2;
						if (StringUtil.isEmpty(option)) {
							option2 = "scouter.name=" + newname;
						} else {
							option2 = option + ",scouter.name=" + newname;
						}
						loadAgent(classVM, agentjar, id, desc, option2);
					} else {
						loadAgent(classVM, agentjar, id, desc, option);
					}
				} else {
					Logger.println("A150", "Not target : [" + id + "] " + desc);
				}
			}
		}
	}
	private static String trimDesc(String desc) {
		if (desc == null) {
			return "";
		}
		desc = desc.trim();
		int index = desc.indexOf(" ");
		if (index > 0) {
			return desc.substring(0, index);
		} else {
			return desc;
		}
	}
	private static void loadAgent(Class classVM, String agentjar, String pid, String desc, String opts)
			throws Exception {
		Object vm = Proxy.attach(classVM, pid);
		Properties p = (Properties) Proxy.invoke(vm.getClass(), vm, "getSystemProperties");
		if (p.containsKey("scouter.enabled") == false) {
			Proxy.invoke(vm.getClass(), vm, "getSystemProperties");
			if (opts == null) {
				Proxy.loadagent(vm, agentjar);
			} else {
				Proxy.loadagent(vm, agentjar, opts);
			}
			Logger.println("A151", "Load agent : [" + pid + "] " + desc);
		} else {
			String scouter_name = p.getProperty("scouter.name");
			Logger.println("A152", "Already loaded : [" + pid + "] scouter.name=" + scouter_name + " " + desc);
		}
		Proxy.invoke(classVM, vm, "detach");
	}
	private static String getScouterName(Class classVM, String agentjar, String pid) throws Exception {
		String scouter_name = null;
		Object vm = Proxy.attach(classVM, pid);
		Properties p = (Properties) Proxy.invoke(vm.getClass(), vm, "getSystemProperties");
		if (p.containsKey("scouter.enabled")) {
			scouter_name = p.getProperty("scouter.name");
		}
		Proxy.invoke(classVM, vm, "detach");
		return scouter_name;
	}
}
