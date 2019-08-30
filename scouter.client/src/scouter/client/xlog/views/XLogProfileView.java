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
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import scouter.client.Activator;
import scouter.client.Images;
import scouter.client.constants.HelpConstants;
import scouter.client.model.XLogData;
import scouter.client.server.GroupPolicyConstants;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.xlog.ProfileText;
import scouter.client.xlog.SaveProfileJob;
import scouter.client.xlog.actions.OpenXLogProfileJob;
import scouter.client.xlog.actions.OpenXLogThreadProfileJob;
import scouter.client.xlog.dialog.XlogSummarySQLDialog;
import scouter.lang.step.Step;
import scouter.util.*;


public class XLogProfileView extends ViewPart {
	public static final String ID = XLogProfileView.class.getName();
	private StyledText text;
	private XLogData xLogData;
	private String txid;
	Menu contextMenu;
	MenuItem sqlSummary;
	MenuItem bindSqlParamMenu;
	MenuItem simplifiedProfileViewMenu;
	
	IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	
	Step[] steps;
	
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
		man.add(openSqlSummaryDialog);
		man.add(helpAction);
		
	    IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();
	    menuManager.add(saveFullProfile);
	    
	    createContextMenu();
	}
	
	private void createContextMenu() {
		contextMenu = new Menu(text);
		sqlSummary = new MenuItem(contextMenu, SWT.PUSH);
		sqlSummary.setText("SQL Statistics");
		sqlSummary.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				openSqlSummaryDialog.run();
			}
		});
		bindSqlParamMenu = new MenuItem(contextMenu, SWT.CHECK);
		bindSqlParamMenu.setText("Bind SQL Parameter");
		bindSqlParamMenu.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				bindSqlParam = bindSqlParamMenu.getSelection();
				setInput(steps, xLogData, serverId);
			}
		});
		simplifiedProfileViewMenu = new MenuItem(contextMenu, SWT.CHECK);
        simplifiedProfileViewMenu.setSelection(true);
		simplifiedProfileViewMenu.setText("Simplified Profile View");
		simplifiedProfileViewMenu.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				isSimplified = simplifiedProfileViewMenu.getSelection();
				setInput(steps, xLogData, serverId);
			}
		});

		MenuItem saveProfile = new MenuItem(contextMenu, SWT.PUSH);
		saveProfile.setText("Save Full Profile");
		saveProfile.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				saveFullProfile.run();
			}
		});
	    text.setMenu(contextMenu);
	}

	public static boolean isSummary;
	
	boolean truncated;
	private int serverId;
	boolean bindSqlParam;
	boolean isSimplified = true;
	
	CacheTable<String, Boolean> preventDupleEventTable = new CacheTable<String, Boolean>().setDefaultKeepTime(700);
	public void setInput(Step[] steps, final XLogData item, int serverId) {
		this.steps = steps;
		this.xLogData = item;
		this.txid = Hexa32.toString32(item.p.txid);
		this.serverId = serverId;
		
		Server server = ServerManager.getInstance().getServer(serverId);
		sqlSummary.setEnabled(server.isAllowAction(GroupPolicyConstants.ALLOW_SQLPARAMETER));
		bindSqlParamMenu.setEnabled(server.isAllowAction(GroupPolicyConstants.ALLOW_SQLPARAMETER));
		
		setPartName(txid);
		text.setText("");
		
		ProfileText.build(DateUtil.yyyymmdd(xLogData.p.endTime), text, this.xLogData, steps, serverId, bindSqlParam, isSimplified);

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
									if (preventDupleEventTable.get("gxid") != null) return;
									preventDupleEventTable.put("gxid", new Boolean(true));
								}
								String[] tokens = StringUtil.tokenizer(fulltxt, " =\n");
								String gxid = tokens[2];
								try {
									XLogFlowView view = (XLogFlowView) window.getActivePage().showView(XLogFlowView.ID, gxid, IWorkbenchPage.VIEW_ACTIVATE);
									if (view != null) {
										view.loadByGxId(DateUtil.yyyymmdd(item.p.endTime), Hexa32.toLong32(gxid));
									}
								} catch (PartInitException e) {
									ConsoleProxy.error(e.toString());
								}
							} else if (fulltxt.startsWith("► txid")) {
								if (preventDupleEventTable.get("txid") != null) return;
								synchronized (preventDupleEventTable) {
									if (preventDupleEventTable.get("txid") != null) return;
									preventDupleEventTable.put("txid", new Boolean(true));
								}
								String[] tokens = StringUtil.tokenizer(fulltxt, " =\n");
								String txid = tokens[tokens.length - 1];
								try {
									XLogFlowView view = (XLogFlowView) window.getActivePage().showView(XLogFlowView.ID, txid, IWorkbenchPage.VIEW_ACTIVATE);
									if (view != null) {
										view.loadByTxId(DateUtil.yyyymmdd(item.p.endTime), Hexa32.toLong32(txid));
									}
								} catch (PartInitException e) {
									ConsoleProxy.error(e.toString());
								}
							} else if (fulltxt.startsWith("► caller")) {
								if (preventDupleEventTable.get("caller") != null) return;
								synchronized (preventDupleEventTable) {
									if (preventDupleEventTable.get("caller") != null) return;
									preventDupleEventTable.put("caller", new Boolean(true));
								}
								String[] tokens = StringUtil.tokenizer(fulltxt, " =\n");
								String txIdStr = tokens[tokens.length - 1];
								long txid = Hexa32.toLong32(txIdStr);
								new OpenXLogProfileJob(XLogProfileView.this.getViewSite().getShell().getDisplay(), DateUtil.yyyymmdd(item.p.endTime), txid, item.p.gxid).schedule();
							} else if (fulltxt.endsWith(">") && fulltxt.contains("call:")) {
								if (preventDupleEventTable.get("call") != null) return;
								synchronized (preventDupleEventTable) {
									if (preventDupleEventTable.get("call") != null) return;
									preventDupleEventTable.put("call", new Boolean(true));
								}
								int startIndex = fulltxt.lastIndexOf("<");
								if (startIndex > -1) {
									int endIndex = fulltxt.lastIndexOf(">");
									String txIdStr = fulltxt.substring(startIndex + 1, endIndex);
									long txid = Hexa32.toLong32(txIdStr);
									new OpenXLogProfileJob(XLogProfileView.this.getViewSite().getShell().getDisplay(), DateUtil.yyyymmdd(item.p.endTime), txid, item.p.gxid).schedule();
								}
							}else if (fulltxt.endsWith(">") && fulltxt.contains("thread:")) {
								if (preventDupleEventTable.get("thread") != null) return;
								synchronized (preventDupleEventTable) {
									if (preventDupleEventTable.get("thread") != null) return;
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
		
		text.redraw();
	}
	
	public void setFocus() {
	}
	
	Action openSqlSummaryDialog = new Action("SQL Statistics", ImageUtil.getImageDescriptor(Images.sum)) {
		public void run() { 
			XlogSummarySQLDialog summberSQLDialog = new XlogSummarySQLDialog(new Shell(Display.getDefault(), SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MAX | SWT.MIN), steps, xLogData);
			summberSQLDialog.open();
		}
	};
	
	Action saveFullProfile = new Action("Save Full Profile") {
		public void run() {
			SaveProfileJob job = new SaveProfileJob("Save Profile...", xLogData.p.endTime, xLogData, txid, serverId, isSummary);
			job.schedule();
		}
    };

	Action helpAction = new Action("help", ImageUtil.getImageDescriptor(Images.help)) {
		public void run() {
			org.eclipse.swt.program.Program.launch(HelpConstants.HELP_URL_XLOG_PROFILE_VIEW);
		}
	};
}
