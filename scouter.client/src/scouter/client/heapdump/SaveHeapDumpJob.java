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
package scouter.client.heapdump;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import scouter.client.Images;
import scouter.client.actions.OpenWorkspaceExplorerAction;
import scouter.client.model.XLogProxy;
import scouter.client.server.ServerManager;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.RCPUtil;

public class SaveHeapDumpJob extends Job{

	public static String hprofDirName = "heapdump";
	String workingDir = "";
	
	public static String hprofExtension = ".hprof";
	public static String hprofFileName = "";
	
	private int objHash;
	private String fileName;
	private String yyyymmdd;
	private int serverId;
	
	int maxBlock;
	
	IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	
	public SaveHeapDumpJob(String name, int objHash, String fileName, String objName, int serverId) {
		super(name);
		this.objHash = objHash;
		this.fileName = fileName;
		this.serverId = serverId;
		yyyymmdd = fileName.substring(0, 8);
		
		String serverName = ServerManager.getInstance().getServer(serverId).getName();
		workingDir = RCPUtil.getWorkingDirectory()+"/"+serverName+"/"+yyyymmdd+"/"+objName+"/"+hprofDirName;
		hprofFileName = fileName;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask("Saveing Heapdump file to... "+workingDir+"/"+fileName, IProgressMonitor.UNKNOWN);
		
		checkAndCreateDir();
		boolean result = downloadHeapdump();
		monitor.done();
		
		if(!result){
			ConsoleProxy.errorSafe("file writing error occured.");
			return Status.OK_STATUS;
		}
		
		ExUtil.exec(PlatformUI.getWorkbench().getDisplay(), new Runnable(){
			public void run() {
				Action act = new OpenWorkspaceExplorerAction(window, "Workspace Explorer", Images.explorer, true, serverId);
				act.run();
			}
		});
		return Status.OK_STATUS;
	}
	
	private void checkAndCreateDir() {
		File hprofDir = new File(workingDir);
		if (hprofDir.exists() == false) {
			hprofDir.mkdirs();
		}
	}
	
	private boolean downloadHeapdump(){
		return XLogProxy.getHeapdumpByteArray(objHash, fileName, workingDir + "/" + hprofFileName, serverId);
	}

//	private void saveHeapdumpToFile(byte[] p) {
//		try {
//			FileOutputStream fileOuputStream = new FileOutputStream(workingDir + "/" + hprofFileName);
//			fileOuputStream.write(p);
//			fileOuputStream.close();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
	
}
