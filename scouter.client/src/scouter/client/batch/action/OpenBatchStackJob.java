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
 *
 */
package scouter.client.batch.action;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipInputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.client.stack.base.MainProcessor;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.RCPUtil;
import scouter.client.util.StackUtil;
import scouter.io.DataInputX;
import scouter.lang.pack.BatchPack;
import scouter.lang.pack.MapPack;
import scouter.net.RequestCmd;
import scouter.util.ZipFileUtil;

public class OpenBatchStackJob extends Job {

	Display display;
	BatchPack pack;
	int serverId;
	String secId = "batchstackview";
	
	public OpenBatchStackJob(Display display, BatchPack pack, int serverId) {
		super("Load Batch History Stack");
		this.display = display;
		this.pack = pack;
		this.serverId = serverId;
	}

	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask("Load Batch Stack....", IProgressMonitor.UNKNOWN);

		final String stackfile = getStackData(serverId, pack);
		if(stackfile == null){
			return Status.CANCEL_STATUS;
		}
		
		ExUtil.exec(Display.getDefault(), new Runnable() {
			public void run() {
				try {
					MainProcessor.instance().processStackFile(stackfile);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});
		return Status.OK_STATUS;
	}
	
	private String getStackData(int serverId, BatchPack input) {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		String dirPath = StackUtil.getStackWorkspaceDir(input.objName);
		String filename = getStackFilename();
		String zipFilename = filename + ".zip";
		File zipFile = new File(dirPath, zipFilename);
		try {
			MapPack param = new MapPack();
			param.put("objHash", input.objHash);
			param.put("time", input.startTime);
			param.put("filename", zipFilename);

			ZipInputStream zis = null;
			final BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(zipFile));
			try {
				tcp.process(RequestCmd.BATCH_HISTORY_STACK, param, new INetReader() {
					public void process(DataInputX in) throws IOException {
						byte [] data = in.readBlob();
						if(data != null && data.length >= 0){
							out.write(data);
						}
					}
				});			
				out.flush();
				
				zis = new ZipInputStream(new FileInputStream(zipFile));
				ZipFileUtil.recieveZipFile(zis, dirPath);
			}finally{
				if(out != null){
					try {out.close(); } catch(Exception ex){}
				}
				if(zis != null){
					try {zis.close(); } catch(Exception ex){}
				}
			}
			zipFile.delete();
		} catch (Throwable th) {
			ConsoleProxy.errorSafe(th.toString());
			return null;
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return dirPath + filename + ".log";
	}
	
	private String getStackFilename(){
		StringBuilder buffer = new StringBuilder(100);
		Date date = new Date(pack.startTime);
		buffer.append(pack.batchJobId).append('_').append(new SimpleDateFormat("yyyyMMdd").format(date)).append('_').append(new SimpleDateFormat("HHmmss.SSS").format(date)).append('_').append(pack.pID);
		return buffer.toString();
	}
}
