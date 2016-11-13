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
package scouter.client.stack.actions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.client.stack.base.MainProcessor;
import scouter.client.util.ExUtil;
import scouter.client.util.RCPUtil;
import scouter.client.util.StackUtil;
import scouter.io.DataInputX;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.StackPack;
import scouter.net.RequestCmd;
import scouter.util.DateUtil;

public class FetchStackJob extends Job {

	int serverId;
	String objName;
	long from;
	long to;
	int total;
	
	public FetchStackJob(int serverId, String objName, long from, long to, int total) {
		super(objName + " stack list...");
		this.serverId = serverId;
		this.objName = objName;
		this.from = from;
		this.to = to;
	}

	protected IStatus run(final IProgressMonitor monitor) {
		String dirPath = StackUtil.getStackWorkspaceDir(this.objName);
		StringBuilder sb = new StringBuilder();
		sb.append(DateUtil.format(from, "yyyyMMdd"));
		sb.append("_");
		sb.append(DateUtil.format(from, "HHmmss"));
		sb.append("_");
		sb.append(DateUtil.format(to, "HHmmss"));
		sb.append(".stack");
		final File stackFile = new File(dirPath, sb.toString());
		if (stackFile.canRead() == false) {
			monitor.beginTask("Fetching...", total);
			TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
			try {
				MapPack param = new MapPack();
				param.put("objName", objName);
				param.put("from", from);
				param.put("to", to);
				final StringBuilder content = new StringBuilder();
				tcp.process(RequestCmd.GET_STACK_ANALYZER, param, new INetReader() {
					int count = 0;
					public void process(DataInputX in) throws IOException {
						StackPack sp = (StackPack) in.readPack();
						content.append(sp.getStack());
						monitor.worked(count++);
						if (count % 100 == 0) {
							writeAppendFile(stackFile, content.toString());
							content.setLength(0);
						}
					}
				});
				if (content.length() > 0) {
					writeAppendFile(stackFile, content.toString());
				}
			} catch (Exception e) {
				e.printStackTrace();
				stackFile.delete();
			} finally {
				TcpProxy.putTcpProxy(tcp);
			}
		}
		
		monitor.done();
		ExUtil.exec(Display.getDefault(), new Runnable() {
			public void run() {
				MainProcessor.instance().processStackFile(stackFile.getAbsolutePath());
			}
		});
		
		return Status.OK_STATUS;
	}
	
	private void writeAppendFile(File file, String str) throws IOException {
		FileWriter writer = new FileWriter(file, true);
		BufferedWriter bufferedWriter = new BufferedWriter(writer, 8192);
		bufferedWriter.write(str);
		bufferedWriter.flush();
		bufferedWriter.close();
	}
}
