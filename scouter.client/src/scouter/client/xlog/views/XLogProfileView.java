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
package scouter.client.xlog.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Activator;
import scouter.client.Images;
import scouter.client.model.XLogData;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ImageUtil;
import scouter.client.util.MyKeyAdapter;
import scouter.client.xlog.ProfileText;
import scouter.client.xlog.SaveProfileJob;
import scouter.client.xlog.actions.OpenXLogProfileJob;
import scouter.client.xlog.actions.OpenXLogThreadProfileJob;
import scouter.client.xlog.dialog.XlogSummarySQLDialog;
import scouter.lang.step.Step;
import scouter.util.CacheTable;
import scouter.util.DateUtil;
import scouter.util.Hexa32;
import scouter.util.StringUtil;
import scouter.util.SystemUtil;


public class XLogProfileView extends ViewPart {
	public static final String ID = XLogProfileView.class.getName();
	private StyledText text;
	private XLogData xLogData;
	private String txid;
	
	IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	
	Step[] steps;
	
	private int spaceCnt = 1;
	MyKeyAdapter adapter;

	public void createPartControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, true));
		
		//createUpperMenu(composite);
		
		adapter = new MyKeyAdapter(parent.getShell(), "SelectedLog.txt");
		
		text = new StyledText(composite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		text.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		text.setText("");
		if(SystemUtil.IS_MAC_OSX){
		    text.setFont(new Font(null, "Courier New", 12, SWT.NORMAL));		
		}else{
		    text.setFont(new Font(null, "Courier New", 10, SWT.NORMAL));
		}
		text.setBackgroundImage(Activator.getImage("icons/grid.jpg"));
		text.addKeyListener(adapter);
		
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();

		man.add( new Action("SQL Statistics", ImageUtil.getImageDescriptor(Images.sum)) {
			public void run() {
				XlogSummarySQLDialog summberSQLDialog = new XlogSummarySQLDialog(new Shell(getViewSite().getShell().getDisplay(), SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN ), steps, xLogData);
				summberSQLDialog.open();
			}
		});
		
		Action gridBackAct = new Action("Grid Background", IAction.AS_CHECK_BOX) {
			public void run() {
				if (isChecked()) {
					text.setBackgroundImage(Activator.getImage("icons/grid.jpg"));
				} else {
					text.setBackgroundImage(null);
				}
			}
		};
		gridBackAct.setImageDescriptor(ImageUtil.getImageDescriptor(Images.grid));
		gridBackAct.setChecked(true);
		man.add(gridBackAct);
	    
	    IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();
	    menuManager.add(new Action("Save Full Profile") {
			public void run() {
				makeFullProfileToFile();
			}
	    });
	}

	public static boolean isSummary;
	
	private void makeFullProfileToFile() {
		SaveProfileJob job = new SaveProfileJob("Save Profile...", xLogData.p.endTime, xLogData, txid, serverId, isSummary);
		job.schedule();
	}

	boolean truncated;
	private int serverId;
	
	CacheTable<String, Boolean> preventDupleEventTable = new CacheTable<String, Boolean>().setDefaultKeepTime(500);
	public void setInput(Step[] steps, final XLogData item, int serverId) {
		this.steps = steps;
		this.xLogData = item;
		this.txid = Hexa32.toString32(item.p.txid);
		this.serverId = serverId;
		
		setPartName(txid);
		text.setText("");
		
		ProfileText.build(DateUtil.yyyymmdd(xLogData.p.endTime), text, this.xLogData, steps, spaceCnt,  serverId);
		text.addListener(SWT.MouseUp, new Listener(){
			public void handleEvent(Event event) {
				try {
					int offset = text.getOffsetAtLocation(new Point (event.x, event.y));
					StyleRange style = text.getStyleRangeAtOffset(offset);
					if (style != null && style.underline && style.underlineStyle == SWT.UNDERLINE_LINK) {
						int line = text.getLineAtOffset(offset);
						String fulltxt = text.getLine(line);
						if (StringUtil.isNotEmpty(fulltxt)) {
							if (fulltxt.startsWith("► gxid")) {
								if (preventDupleEventTable.get("gxid") != null) return;
								synchronized (preventDupleEventTable) {
									preventDupleEventTable.put("gxid", new Boolean(true));
								}
								String[] tokens = StringUtil.tokenizer(fulltxt, " =\n");
								String gxid = tokens[tokens.length - 1];
								try {
									XLogDependencyView view = (XLogDependencyView) PlatformUI.getWorkbench()
									.getActiveWorkbenchWindow()
									.getActivePage().showView(XLogDependencyView.ID, "*", IWorkbenchPage.VIEW_ACTIVATE);
									if (view != null) {
										view.loadByGxId(DateUtil.yyyymmdd(item.p.endTime), Hexa32.toLong32(gxid));
									}
								} catch (PartInitException e) {
									ConsoleProxy.error(e.toString());
								}
							} else if (fulltxt.startsWith("► txid")) {
								if (preventDupleEventTable.get("txid") != null) return;
								synchronized (preventDupleEventTable) {
									preventDupleEventTable.put("txid", new Boolean(true));
								}
								String[] tokens = StringUtil.tokenizer(fulltxt, " =\n");
								String txid = tokens[tokens.length - 1];
								try {
									XLogDependencyView view = (XLogDependencyView) PlatformUI.getWorkbench()
									.getActiveWorkbenchWindow()
									.getActivePage().showView(XLogDependencyView.ID, "*", IWorkbenchPage.VIEW_ACTIVATE);
									if (view != null) {
										view.loadByTxId(DateUtil.yyyymmdd(item.p.endTime), Hexa32.toLong32(txid));
									}
								} catch (PartInitException e) {
									ConsoleProxy.error(e.toString());
								}
							} else if (fulltxt.startsWith("► caller")) {
								if (preventDupleEventTable.get("caller") != null) return;
								synchronized (preventDupleEventTable) {
									preventDupleEventTable.put("caller", new Boolean(true));
								}
								String[] tokens = StringUtil.tokenizer(fulltxt, " =\n");
								String txIdStr = tokens[tokens.length - 1];
								long txid = Hexa32.toLong32(txIdStr);
								new OpenXLogProfileJob(DateUtil.yyyymmdd(item.p.endTime), txid).schedule();
							} else if (fulltxt.endsWith(">") && fulltxt.contains("call:")) {
								if (preventDupleEventTable.get("call") != null) return;
								synchronized (preventDupleEventTable) {
									preventDupleEventTable.put("call", new Boolean(true));
								}
								int startIndex = fulltxt.lastIndexOf("<");
								if (startIndex > -1) {
									int endIndex = fulltxt.lastIndexOf(">");
									String txIdStr = fulltxt.substring(startIndex + 1, endIndex);
									long txid = Hexa32.toLong32(txIdStr);
									new OpenXLogProfileJob(DateUtil.yyyymmdd(item.p.endTime), txid).schedule();
								}
							}else if (fulltxt.endsWith(">") && fulltxt.contains("thread:")) {
								if (preventDupleEventTable.get("thread") != null) return;
								synchronized (preventDupleEventTable) {
									preventDupleEventTable.put("thread", new Boolean(true));
								}
								int startIndex = fulltxt.lastIndexOf("<");
								if (startIndex > -1) {
									int endIndex = fulltxt.lastIndexOf(">");
									String txIdStr = fulltxt.substring(startIndex + 1, endIndex);
									long threadTxid = Hexa32.toLong32(txIdStr);
									new OpenXLogThreadProfileJob(xLogData, threadTxid).schedule();
								}
							}
						}
					}
				} catch (IllegalArgumentException e) {
					// no character under event.x, event.y
				}
			}
		});
		adapter.setFileName("profile_"+txid+".txt");
	}
	
	public void setFocus() {
	}

	@Override
	public void dispose() {
		super.dispose();
	}
	
	public static void main(String[] args) {
		String fulltxt = "call: UCON:http://127.0.0.1:8080/e2end.jsp 117 ms <z2bcfcg3hfcm0h>";
		int startIndex = fulltxt.lastIndexOf("<");
		if (startIndex > -1) {
			int endIndex = fulltxt.lastIndexOf(">");
			String txIdStr = fulltxt.substring(startIndex + 1, endIndex);
			System.out.println(txIdStr);
		}
	}
}
