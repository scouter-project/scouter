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
package scouter.client.batch.views;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Activator;
import scouter.client.Images;
import scouter.client.batch.actions.OpenBatchStackJob;
import scouter.client.util.ImageUtil;
import scouter.lang.pack.BatchPack;
import scouter.lang.value.MapValue;
import scouter.util.SystemUtil;
import scouter.util.TimeFormatUtil;

public class BatchDetailView extends ViewPart {
	public static final String ID = BatchDetailView.class.getName();
	private StyledText text;
	private BatchPack pack;
	private int serverId;
	
	Menu contextMenu;
	
	IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		
	public void createPartControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, true));
		
		text = new StyledText(composite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		text.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		text.setText("");
		if(SystemUtil.IS_MAC_OSX){
		    text.setFont(new Font(null, "Courier New", 12, SWT.NORMAL));		
		}else{
		    text.setFont(new Font(null, "Courier New", 10, SWT.NORMAL));
		}
		text.setBackgroundImage(Activator.getImage("icons/grid.jpg"));

		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(openThreadDumpDialog);
		man.add(openSFADialog);
		//createContextMenu();
	}
		
	private void createContextMenu() {
		contextMenu = new Menu(text);
		MenuItem menu = new MenuItem(contextMenu, SWT.PUSH);
		menu.setText("Stack Frequency Analyzer");
		menu.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if(pack == null || !pack.isStack){
					return;
				}
				openSFADialog.run();
			}
		});

		menu = new MenuItem(contextMenu, SWT.PUSH);
		menu.setText("Batch Thread Dump View");
		menu.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if(pack == null || !pack.isStack){
					return;
				}
				openThreadDumpDialog.run();
			}
		});
		
	    text.setMenu(contextMenu);
	}
	
	public void setInput(BatchPack pack, int serverId) {
		this.pack = pack;
		this.serverId = serverId;
		setPartName(pack.objName + " - " + pack.batchJobId);

		StringBuilder buffer = new StringBuilder(10240);		
		String lineSeparator = System.getProperty("line.separator");
			
		buffer.append("-[").append(pack.batchJobId).append("]----------------------------------------------").append(lineSeparator);
		buffer.append("PID         : ").append(pack.pID).append(lineSeparator);
		buffer.append("Run  Command: ").append(pack.args).append(lineSeparator);
		if(pack.isStack){
			
			buffer.append("Stack   Dump: O").append(lineSeparator);
			createContextMenu();
		}else{
			buffer.append("Stack   Dump: X").append(lineSeparator);
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		buffer.append("Start   Time: ").append(sdf.format(new Date(pack.startTime))).append(lineSeparator);
		buffer.append("Stop    Time: ").append(sdf.format(new Date(pack.startTime + pack.elapsedTime))).append(lineSeparator);
		buffer.append("Elapsed Time: ").append(String.format("%,13d",pack.elapsedTime)).append(" ms ");
		buffer.append(TimeFormatUtil.elapsedTime(pack.elapsedTime));
		buffer.append(lineSeparator);
		if(pack.cpuTime > 0){
			buffer.append("CPU     Time: ").append(String.format("%,13d",pack.cpuTime/1000000L)).append(" ms").append(lineSeparator);
		}
		if(pack.gcCount > 0){
			buffer.append("GC     Count: ").append(String.format("%,13d",pack.gcCount)).append(lineSeparator);
			buffer.append("GC      Time: ").append(String.format("%,13d",pack.gcTime)).append(" ms ");
			if(pack.elapsedTime > 0){
				buffer.append(String.format("%.2f", ((float)(pack.gcTime * 100F)/pack.elapsedTime))).append(" %");
			}
			buffer.append(lineSeparator);
		}
		if(pack.sqlTotalCnt > 0){
			buffer.append("SQL     Time: ").append(String.format("%,13d",(pack.sqlTotalTime/1000000L))).append(" ms ");
			if(pack.elapsedTime > 0){
				buffer.append(String.format("%.2f", ((float)((pack.sqlTotalTime / 1000000F) * 100F)/pack.elapsedTime))).append(" %");
			}
			buffer.append(lineSeparator);
			buffer.append("SQL     Type: ").append(String.format("%,13d",pack.sqlTotalCnt)).append(lineSeparator);
			buffer.append("SQL     Runs: ").append(String.format("%,13d",pack.sqlTotalRuns)).append(lineSeparator);
		}
		if(pack.threadCnt > 0){
			buffer.append("Thread Count: ").append(String.format("%,13d",pack.threadCnt)).append(lineSeparator);
		}
			
		if(pack.sqlTotalCnt > 0){
			buffer.append(lineSeparator).append("<SQLs>").append(lineSeparator);
			int index = 0;
			buffer.append("Index          Runs     TotalTime        Rate       MinTime       AvgTime       MaxTime          Rows (Measured) StartTime               EndTime").append(lineSeparator);
			buffer.append("----------------------------------------------------------------------------------------------------------------------------------------------------------------");
			List<MapValue> stats = pack.sqlStats;
			for(MapValue mapValue : stats){
				index++;
				buffer.append(lineSeparator);
				buffer.append(String.format("%5s", index)).append(' ');
				buffer.append(String.format("%,13d", mapValue.getLong("runs"))).append(' ');
				buffer.append(String.format("%,13d", (mapValue.getLong("totalTime")/1000000L))).append(' ');
				if((mapValue.getLong("totalTime")/1000000L) == 0){
					buffer.append(String.format("%,10.2f", 0F)).append("% ");					
				}else{
					buffer.append(String.format("%,10.2f", ((100F * (mapValue.getLong("totalTime")/1000000L))/pack.elapsedTime))).append("% ");					
				}
				if(mapValue.getLong("runs") == 0 && mapValue.getLong("minTime") == Long.MAX_VALUE){
					buffer.append(String.format("%,13d", 0)).append(' ');
				}else{
					buffer.append(String.format("%,13d", (mapValue.getLong("minTime")/1000000L))).append(' ');
				}
				if(mapValue.getLong("runs") == 0){
					buffer.append(String.format("%,13d", 0)).append(' ');
				}else{
					buffer.append(String.format("%,13d", ((mapValue.getLong("totalTime")/mapValue.getLong("runs"))/1000000L))).append(' ');
				}
				buffer.append(String.format("%,13d", (mapValue.getLong("maxTime")/1000000L))).append(' ');
				buffer.append(String.format("%,13d", mapValue.getLong("processedRows"))).append(' ').append(String.format("%10s", mapValue.getBoolean("rowed"))).append(' ');
				buffer.append(sdf.format(new Date(mapValue.getLong("startTime")))).append(' ');
				buffer.append(sdf.format(new Date(mapValue.getLong("endTime"))));
			}
			buffer.append(lineSeparator).append("----------------------------------------------------------------------------------------------------------------------------------------------------------------").append(lineSeparator);

			buffer.append(lineSeparator).append("<SQL Texts>").append(lineSeparator);
			index = 0;
			Map<Integer, String> sqls = pack.uniqueSqls;
			for(MapValue mapValue : stats){
				index++;
				buffer.append("-----------------").append(lineSeparator);
				buffer.append("#SQLINX-").append(index).append(lineSeparator);
				buffer.append(sqls.get((int)mapValue.getLong("hashValue"))).append(lineSeparator);
			}
		}
		text.setText(buffer.toString());
	}

	public void setFocus() {
	}
	
	Action openSFADialog = new Action("Stack Frequency Analyzer", ImageUtil.getImageDescriptor(Images.page_white_stack)) {
		public void run() { 
			new OpenBatchStackJob(pack, serverId, true).schedule();
		}
	};

	Action openThreadDumpDialog = new Action("Batch Thread Dump View", ImageUtil.getImageDescriptor(Images.thread)) {
		public void run() { 
			new OpenBatchStackJob(pack, serverId, false).schedule();
		}
	};
}
