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

package scouter.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

public class RunExec {
	private String cmd;
	private String[] env;
	private File dir;
	private Throwable exception;
	private Process process;
	private StringBuilder error = new StringBuilder();
	private StringBuilder output = new StringBuilder();

	public RunExec(String cmd) {
		this.cmd = cmd;
	}

	public RunExec setEnv(String[] env) {
		this.env = env;
		return this;
	}

	public RunExec setWorkDir(File dir) {
		this.dir = dir;
		return this;
	}

	long timeout = 5000;
	
	public int exec() {
		try {
			process = Runtime.getRuntime().exec(cmd, env, dir);
			output = new StringBuilder();
			Thread t1 = readAndClose("RunExec-Output", process.getInputStream(), output);
			error = new StringBuilder();
			Thread t2 = readAndClose("RunExec-Error", process.getErrorStream(), error);
			process.getOutputStream().close();
			if (timeout > 0) {
				for (int i = 0; (t1.isAlive() || t2.isAlive() ) && i < timeout; i = i + 50) {
					ThreadUtil.sleep(50);
				}
				if (t1.isAlive() || t2.isAlive()) {
					process.destroy();
					setException(new Exception("TimeOutException"));
					return 9;
				}
			} else {
				process.waitFor();
			}
			return process.exitValue();
		} catch (Throwable e) {
			setException(e);
		}
		return -1;
	}

	private void setException(Throwable e) {
		if (this.exception == null) {
			this.exception = e;
		}
	}

	private Thread readAndClose(String name, final InputStream fin, final StringBuilder sb) {
		Thread t = new Thread(new Runnable() {
			public void run() {
				try {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					byte[] buff = new byte[1024];
					int n = fin.read(buff);
					for (int k = 0; n >= 0 && k < 1024 * 1024; k++) {
						if (out.size() < 1024 * 1024) {
							out.write(buff, 0, n);
						}
						n = fin.read(buff);
					}
					sb.append(new String(out.toByteArray()));
				} catch (Throwable t) {
					setException(t);
				} finally {
					FileUtil.close(fin);
				}
			}
		});
		t.setDaemon(true);
		t.setName(name);
		t.start();
		return t;
	}

	public String getError() {
		return this.error.toString();
	}

	public String getOutput() {
		return this.output.toString();
	}

	public Throwable getException() {
		return this.exception;
	}
	
	public void setTimeout(long time) {
		this.timeout = time;
	}
	
	public static void main(String[] args) {
		RunExec re = new RunExec("d:\\jad\\jad.exe" + " -p " + "d:\\jad\\org.jboss.jca.core.connectionmanager.pool.AbstractPool.class");
		//RunExec re = new RunExec("d:\\jad\\jad.exe" + " -p " + "d:\\jad\\scouter.apache.StringUtils.class");
		//RunExec re = new RunExec("\"C:\\Program Files\\Java\\jdk1.6.0_43\\bin\\java\" -cp d:\\sleep.jar Main 2000");
		re.exec();
		System.out.println(re.getOutput());
		System.out.println(re.getError());
		System.out.println(re.getException());
	}

}