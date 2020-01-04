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

import java.io.IOException;
import java.util.Properties;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

public class JVM {

	public JVM(String pid) {
		this.pid = pid;
	}

	private String pid;
	private VirtualMachine vm = null;
	private String desc;

	public boolean isConnected() {
		return vm != null;
	}

	public String getPid() {
		return pid;
	}

	public boolean connect() throws AttachNotSupportedException, IOException {
		this.vm = VirtualMachine.attach(pid);
		this.desc = vm.getSystemProperties().getProperty("sun.java.command");
		return true;
	}

	public void close() {
		if (this.vm != null) {
			try {
				this.vm.detach();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		this.pid = null;
		this.vm = null;
	}

	public Properties getSystemProperties() throws IOException {
		if (vm == null)
			throw new RuntimeException("Not connected to jvm");
		return vm.getSystemProperties();
	}

	public VirtualMachine getVM() {
		return vm;
	}

	public String getDesc() {
		return this.desc;
	}

}