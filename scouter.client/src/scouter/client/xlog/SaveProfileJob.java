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
package scouter.client.xlog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Button;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import scouter.client.Images;
import scouter.client.actions.OpenWorkspaceExplorerAction;
import scouter.client.model.TextProxy;
import scouter.client.model.XLogData;
import scouter.client.model.XLogProxy;
import scouter.client.server.ServerManager;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.RCPUtil;
import scouter.client.util.StepWrapper;
import scouter.lang.pack.XLogPack;
import scouter.lang.step.Step;
import scouter.lang.step.StepSingle;
import scouter.lang.step.StepSummary;
import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.util.DateUtil;
import scouter.util.SortUtil;

public class SaveProfileJob extends Job{

	public static String xLogDirName = "xlog";
	String workingDir = "";
	
	public static String xLogFileName          = "xlog.xlog";
	public static String profileFileName        = "xlog.prof";
	public static String profileSummaryFileName = "xlog_summary.prof";
	
	long date;
	String yyyymmdd;
	XLogData xLogData;
	String txid;
	int maxBlock;
	private int serverId;
	boolean isSummary;
	
	IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	
	public SaveProfileJob(String name, long date, XLogData xLogData, String txid, int serverId, boolean isSummary) {
		super(name);
		this.date = date; 
		this.yyyymmdd =  DateUtil.yyyymmdd(date);
		this.xLogData = xLogData;
		this.txid = txid;
		this.serverId = serverId;
		this.isSummary = isSummary;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		checkDir();

		monitor.beginTask("Saveing full Profile to... "+workingDir, IProgressMonitor.UNKNOWN);
		
		createDirAndFile();
		saveFullProfile(downloadFullProfile());
		saveXLogData();

		monitor.done();
		
		ExUtil.exec(PlatformUI.getWorkbench().getDisplay(), new Runnable(){
			public void run() {
				Action act = new OpenWorkspaceExplorerAction(window, "Workspace Explorer", Images.explorer, true, serverId);
				act.run();
			}
		});
		
		
		return Status.OK_STATUS;
	}
	
	
	private void checkDir() {
		String service = TextProxy.service.getText(xLogData.p.service).replaceAll("/", "_");
		if(service.length() > 80){
			service = service.substring(0, 80);
		}
		String serverName = ServerManager.getInstance().getServer(serverId).getName();
		String dir = RCPUtil.getWorkingDirectory()+"/"+serverName+"/"+yyyymmdd+xLogData.objName+"/"+xLogDirName;
		
		File rootDir = new File(dir);
		File[] fs = rootDir.listFiles();
		if(fs != null){
			workingDir = dir+"/"+"["+String.format("%03d-", (fs.length+1))+DateUtil.format(date, "HHmmss")+"]"+txid+"_"+service+"/";
			for(File f : fs){
				String dirPath = f.getAbsolutePath();
				if(f.isDirectory() && dirPath != null && dirPath.indexOf(txid+"_"+service) != -1){
					workingDir = dirPath+"/";
					break;
				}
			}
		}else{
			workingDir = dir+"/"+"[001-"+DateUtil.format(date, "HHmmss")+"]"+txid+"_"+service+"/";
		}
	}

	private void saveXLogData() {
		try {
			FileOutputStream fileOuputStream = new FileOutputStream(workingDir + xLogFileName);
			DataOutputX outX = new DataOutputX();
			fileOuputStream.write(outX.writePack(xLogData.p).toByteArray());
			fileOuputStream.close();
		} catch (Exception e) {
			ConsoleProxy.errorSafe(e.toString());
		} finally{
		}
	}

	private void createDirAndFile() {
		File xlogDir = new File(workingDir);
		if (xlogDir.exists() == false) {
			xlogDir.mkdirs();
		}
		
		File xlogFile = new File(xlogDir, xLogFileName);
		if (xlogFile.exists()) {
			xlogFile.delete();
		}
		File profFile = new File(xlogDir, profileFileName);
		if (profFile.exists()) {
			profFile.delete();
		}
		File profsumFile = new File(xlogDir, profileSummaryFileName);
		if (profsumFile.exists()) {
			profsumFile.delete();
		}
	}
	
	private byte[] downloadFullProfile(){
		return XLogProxy.getFullProfileByteArray(DateUtil.yyyymmdd(xLogData.p.endTime), xLogData.p.txid, maxBlock, serverId);
	}

	private void saveFullProfile(byte[] p) {
		try {
			FileOutputStream fileOuputStream;
			if(!isSummary){
				fileOuputStream = new FileOutputStream(workingDir + profileFileName);
			}else{
				fileOuputStream = new FileOutputStream(workingDir + profileSummaryFileName);
			}
			fileOuputStream.write(p);
			fileOuputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static XLogPack getTranxData(File xlogDir, StyledText header, int serverId) {
		String xlogPath = xlogDir.getAbsolutePath()+"/"+xLogFileName;
		File xlogFile = new File(xlogPath);
		if(!xlogFile.canRead()){
			return null;
		}
		byte[] xlogb = new byte[(int) xlogFile.length()];
		
		try {
			FileInputStream xlogInputStream = new FileInputStream(xlogFile);
			xlogInputStream.read(xlogb);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		DataInputX inX = new DataInputX(xlogb);
		XLogPack p = null;
		try {
			p = (XLogPack) inX.readPack();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String date = DateUtil.yyyymmdd(p.endTime);
		
		if(p != null){
			ProfileTextFull.buildXLogData(date, header, p, serverId);
		}
		return p;
	}
	
	public static StepWrapper[] getProfileData(XLogPack pack, File xLogDir, boolean isSummary) {
		String profilePath;
		if(!isSummary){
			profilePath = xLogDir.getAbsolutePath()+"/"+profileFileName;
		}else{
			profilePath = xLogDir.getAbsolutePath()+"/"+profileSummaryFileName;
		}
		File profileFile = new File(profilePath);
		if(!profileFile.canRead()){
			return null;
		}
		byte[] profb = new byte[(int) profileFile.length()];
		
		try {
			FileInputStream profileInputStream = new FileInputStream(profileFile);
			profileInputStream.read(profb);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		Step[] profile = null;
		try {
			profile = Step.toObjects(profb);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		profile = SortUtil.sort(profile);
		
		HashMap<Integer, Integer> indent = new HashMap<Integer, Integer>();
		long stime = pack.endTime - pack.elapsed;
		
		StepWrapper[] profWr = new StepWrapper[profile.length];
		
		int expectedIdx = 0;
		
		int sSummaryIdx = 1;
		
		for(int inx = 0 ; inx < profile.length ; inx++){
			
			if(profile[inx] instanceof StepSingle){
				StepSingle sSingle = (StepSingle) profile[inx];
				int space = 0;
				if (indent.containsKey(sSingle.parent)) {
					space = indent.get(sSingle.parent) + 2;
				}
				indent.put(sSingle.index, space);
				
				profWr[inx] = new StepWrapper(sSingle.start_time + stime, sSingle.start_cpu, space, -1, sSingle);
				
				expectedIdx = sSingle.index + 1;
			}else if(profile[inx] instanceof StepSummary){
				StepSummary sSummary = (StepSummary) profile[inx];
				profWr[inx] = new StepWrapper(-1, -1, -1, sSummaryIdx, sSummary);
				sSummaryIdx++;
			}
			
		}
		
		return profWr;
	}
	
	public static void setProfileData(XLogPack pack, StepWrapper[] profiles, StyledText text, int pageNum, int rowPerPage, Button prevBtn, Button nextBtn, Button startBtn, Button endBtn, int length, int serverId, int searchLineIndex, boolean isSummary){
		String date = DateUtil.yyyymmdd(pack.endTime);
		
		int total = (profiles.length / rowPerPage) + 1;
		if(profiles.length % rowPerPage == 0){
			total = total - 1;	
		}
		if(pageNum > total){
			pageNum = total - 1;
		}
		
		ProfileTextFull.buildProfile(date, text, pack, profiles, pageNum, rowPerPage, prevBtn, nextBtn, startBtn, endBtn, length, serverId, searchLineIndex, isSummary);
	}
	
	
	
	
	
	
}
