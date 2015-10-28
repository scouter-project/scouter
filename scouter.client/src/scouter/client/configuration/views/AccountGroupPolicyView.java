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
package scouter.client.configuration.views;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.client.server.GroupPolicyConstants;
import scouter.client.server.ServerManager;
import scouter.client.util.ColorUtil;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.UIUtil;
import scouter.util.StringUtil;
import scouter.lang.pack.MapPack;
import scouter.lang.value.BooleanValue;
import scouter.lang.value.MapValue;
import scouter.lang.value.Value;
import scouter.io.DataInputX;
import scouter.net.RequestCmd;
import scouter.util.ClassUtil;

public class AccountGroupPolicyView extends ViewPart {

	public final static String ID = AccountGroupPolicyView.class.getName();
	
	public static final String CHECK = "\u2713";
	public static final String UNCHECK = "X";
	
	int serverId;
	Composite comp;
	Table policyTable;
	private TableColumnLayout tableColumnLayout = new TableColumnLayout();
	static Set<Object> totalSet = ClassUtil.getPublicFinalValueMap(GroupPolicyConstants.class, String.class).keySet();
	ArrayList<String> groupList = new ArrayList<String>();
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		this.serverId = Integer.valueOf(site.getSecondaryId());
	}
	
	public void createPartControl(final Composite parent) {
		this.setPartName("Policy[" + ServerManager.getInstance().getServer(serverId).getName() + "]");
		comp = new Composite(parent, SWT.NONE);
		policyTable = new Table(comp, SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		policyTable.setHeaderVisible(true);
		policyTable.setLinesVisible(true);
		
		comp.setLayout(tableColumnLayout);
		TableColumn column = new TableColumn(policyTable, SWT.CENTER);
		tableColumnLayout.setColumnData(column, new ColumnPixelData(170, true));
		column.setText("");
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		if (ServerManager.getInstance().getServer(serverId).isAllowAction(GroupPolicyConstants.ALLOW_EDITGROUPPOLICY)) {
			policyTable.addListener(SWT.MouseDown, new Listener() {
				public void handleEvent(Event event) {
					Point pt = new Point(event.x, event.y);
		            TableItem item = policyTable.getItem(pt);
		            if(item != null) {
		            	for (int col = 1; col < policyTable.getColumnCount(); col++) {
		            		Rectangle rect = item.getBounds(col);
		                    if (rect.contains(pt)) {
		                    	String text = item.getText(col);
		                    	if (StringUtil.isEmpty(text)) {
		                    		item.setForeground(col, ColorUtil.getInstance().getColor("red"));
		                    		item.setText(col, CHECK);
		                    	} else if (CHECK.equals(text)) {
		                    		if (item.getForeground(col).getRGB().red == 255) {
		                    			item.setText(col, "");
		                    		} else {
		                    			item.setForeground(col, ColorUtil.getInstance().getColor("red"));
			                    		item.setText(col, UNCHECK);
		                    		}
		                    	} else if (UNCHECK.equals(text)) {
		                    		item.setForeground(col, ColorUtil.getInstance().getColor(SWT.COLOR_BLACK));
		                    		item.setText(col, CHECK);
		                    	}
		                    	break;
		                    }
		                }
		            }
				}
			});
			man.add(new Action("Add AccountGroup", ImageUtil.getImageDescriptor(Images.add)) {
				public void run() {
					new NewAccountGroupDailog().show();
				}
			});
			man.add(new Action("Save", ImageUtil.getImageDescriptor(Images.save)) {
				public void run() {
					if (MessageDialog.openConfirm(parent.getShell(), "Save Account Group Polies", "These polices will be applied all clients. Continue?")) {
						new PolicySaveJob(makePolicyMap()).schedule();
					}
				}
			});
			man.add(new Separator());
		}
		man.add(new Action("Reload", ImageUtil.getImageDescriptor(Images.refresh)) {
			public void run() {
				load();
			}
		});
		load();
	}
	
	private void load() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				MapPack p = null;
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				try {
					p = (MapPack) tcp.getSingle(RequestCmd.GET_GROUP_POLICY_ALL, null);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				final MapPack pack = p;
				ExUtil.exec(comp, new Runnable() {
					public void run() {
						policyTable.setRedraw(false);
						policyTable.removeAll();
						while ( policyTable.getColumnCount() > 1) {
						    policyTable.getColumns()[policyTable.getColumnCount() -1].dispose();
						}
						groupList.clear();
						Set<String> groupSet = new TreeSet<String>(pack.keySet());
						for (String name : groupSet) {
							TableColumn column = new TableColumn(policyTable, SWT.CENTER);
							column.setText(name);
							tableColumnLayout.setColumnData(column, new ColumnPixelData(100, true));
							groupList.add(name);
						}
						
						String[] policyArray = totalSet.toArray(new String[totalSet.size()]);
						for (int i = 0; i < policyArray.length; i++) {
							TableItem item = new TableItem(policyTable, SWT.CENTER);
							item.setText(0, policyArray[i]);
							int j = 1;
							for (String group : groupSet) {
								MapValue mv = (MapValue) pack.get(group);
								Value v = mv.get(policyArray[i]);
								if (v != null) {
									BooleanValue bv = (BooleanValue) v;
									if (bv.value) {
										item.setText(j, CHECK);
									}
								}
								j++;
							}
						}
						policyTable.setRedraw(true);
						comp.layout(true, true);
					}
				});
			}
		});
	}

	public void setFocus() {
		
	}
	
	class PolicySaveJob extends Job {
		
		private MapPack param;
		
		public PolicySaveJob(MapPack param) {
			super("Account Group Policy Saving");
			this.param = param;
		}

		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask("Editing Policy....... ", IProgressMonitor.UNKNOWN);
			TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
			try {
				tcp.process(RequestCmd.EDIT_GROUP_POLICY, param, new INetReader() {
					public void process(DataInputX in) throws IOException {
						final Value v = in.readValue();
						ExUtil.exec(comp, new Runnable() {
							public void run() {
								if (v == null) {
									MessageDialog.openError(getSite().getShell(), "Problem occured"  , "Can't receive response. Please try again or contact administrator.");
								} else {
									boolean result = ((BooleanValue) v).value;
									if (result) {
										TableItem[] items = policyTable.getItems();
										int colCount = policyTable.getColumnCount();
										for (TableItem item : items) {
											for (int i = 1; i < colCount; i++) {
												if (CHECK.equals(item.getText(i))) {
													item.setForeground(i, ColorUtil.getInstance().getColor(SWT.COLOR_BLACK));
													item.setText(i, CHECK);
												} else {
													item.setText(i, "");
												}
											}
										}
										MessageDialog.openInformation(getSite().getShell(), "Success"  , "Save successfully. You should restart client to apply new polices.");
									} else {
										MessageDialog.openError(getSite().getShell(), "Failed"  , "Save Failed. Please try again or contact administrator.");
									}
								}
							}
						});
					}
				});
			} catch(Throwable th) {
				ConsoleProxy.errorSafe(th.getMessage());
				return Status.CANCEL_STATUS;
			} finally {
				TcpProxy.putTcpProxy(tcp);
			}
			return Status.OK_STATUS;
		}
	}
	
	private MapPack makePolicyMap() {
		MapPack pack = new MapPack();
		TableItem[] items = policyTable.getItems();
		for (TableItem item : items) {
			String policy = item.getText(0);
			if (StringUtil.isEmpty(policy)) {
				continue;
			}
			int groupIndex = 1;
			for (String group : groupList) {
				Value v = pack.get(group);
				if (v == null) {
					v = new MapValue();
					pack.put(group, v);
				}
				MapValue mv = (MapValue) v;
				String value = item.getText(groupIndex);
				if (CHECK.equals(value)) {
					mv.put(policy, new BooleanValue(true));
				} else {
					mv.put(policy, new BooleanValue(false));
				}
				groupIndex++;
			}
		}
		return pack;
	}
	
	class NewAccountGroupDailog {
		
		Table table;
		
		void show() {
			final Shell dialog = new Shell(getSite().getShell().getDisplay(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			UIUtil.setDialogDefaultFunctions(dialog);
			dialog.setLayout(new GridLayout(2, false));
			dialog.setText("Add Account Group");
			Composite nameComp = new Composite(dialog, SWT.NONE);
			GridData gr = new GridData(SWT.LEFT, SWT.CENTER, true, true, 2, 1);
			nameComp.setLayoutData(gr);
			nameComp.setLayout(new RowLayout());
//			Label label = new Label(nameComp, SWT.NONE);
//			label.setText("Name : ");
			final Text nameTxt = new Text(nameComp, SWT.SINGLE  | SWT.BORDER);
			nameTxt.setLayoutData(new RowData(150, SWT.DEFAULT));
			
			table = new Table(dialog,  SWT.CHECK | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
			gr = new GridData(SWT.FILL, SWT.FILL, true, true);
			gr.widthHint = 250;
			gr.heightHint = 350;
			table.setLayoutData(gr);
			
			String[] policyArray = totalSet.toArray(new String[totalSet.size()]);
			for (String policy : policyArray) {
				TableItem item = new TableItem(table, SWT.NONE);
				item.setText(policy);
			}
			
			Composite buttonComp = new Composite(dialog, SWT.NONE);
			gr = new GridData(SWT.FILL, SWT.FILL, false, true);
			buttonComp.setLayoutData(gr);
			buttonComp.setLayout(UIUtil.formLayout(3, 3));
			
			Button selectAllBtn = new Button(buttonComp, SWT.PUSH);
			selectAllBtn.setLayoutData(UIUtil.formData(null, -1, 0, 5, null, -1, null, -1, 100));
			selectAllBtn.setText("&Select All");
			selectAllBtn.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					TableItem[] items = table.getItems();
					for (TableItem item : items) {
						item.setChecked(true);
					}
				}
			});
			
			Button deselectAllBtn = new Button(buttonComp, SWT.PUSH);
			deselectAllBtn.setLayoutData(UIUtil.formData(null, -1, selectAllBtn, 5, null, -1, null, -1, 100));
			deselectAllBtn.setText("&Deselect All");
			deselectAllBtn.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					TableItem[] items = table.getItems();
					for (TableItem item : items) {
						item.setChecked(false);
					}
				}
			});
			
			Composite bottomComp = new Composite(dialog, SWT.NONE);
			bottomComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
			bottomComp.setLayout(UIUtil.formLayout(3, 3));
			final Button cancelBtn = new Button(bottomComp, SWT.PUSH);
			cancelBtn.setLayoutData(UIUtil.formData(null, -1, null, -1, 100, -5, null, -1, 100));
			cancelBtn.setText("&Cancel");
			cancelBtn.addListener(SWT.Selection, new Listener(){
				public void handleEvent(Event event) {
					dialog.close();
				}
			});
			
			final Button okBtn = new Button(bottomComp, SWT.PUSH);
			okBtn.setLayoutData(UIUtil.formData(null, -1, null, -1, cancelBtn, -5, null, -1, 100));
			okBtn.setText("&Ok");
			okBtn.addListener(SWT.Selection, new Listener(){
				public void handleEvent(Event event) {
					String name = nameTxt.getText();
					if (StringUtil.isEmpty(name)) {
						MessageDialog.openWarning(dialog, "Required Name", "Name is required.");
						nameTxt.setFocus();
						return;
					}
					if (groupList.contains(name)) {
						MessageDialog.openWarning(dialog, "Duplicated Name", "Name is duplicated.");
						nameTxt.setFocus();
						nameTxt.selectAll();
						return;
					}
					MapPack param = new MapPack();
					MapValue mv = new MapValue();
					param.put("name", name);
					param.put("policy",  mv);
					TableItem[] items = table.getItems();
					for (TableItem item : items) {
						String policy = item.getText();
						BooleanValue bv = new BooleanValue(item.getChecked());
						mv.put(policy, bv);
					}
					new AddAccountGroupJob(param).schedule();
					dialog.close();
				}
			});
			
			nameTxt.setFocus();
			
			dialog.pack();
			dialog.open();
		}
	}
	
	class AddAccountGroupJob extends Job {
		
		private MapPack param;
		
		public AddAccountGroupJob(MapPack param) {
			super("Add Account Group");
			this.param = param;
		}

		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask("Add Account Group....... ", IProgressMonitor.UNKNOWN);
			TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
			try {
				tcp.process(RequestCmd.ADD_ACCOUNT_GROUP, param, new INetReader() {
					public void process(DataInputX in) throws IOException {
						final Value v = in.readValue();
						ExUtil.exec(comp, new Runnable() {
							public void run() {
								if (v == null) {
									MessageDialog.openError(getSite().getShell(), "Problem occured"  , "Can't receive response. Please try again or contact administrator.");
								} else {
									boolean result = ((BooleanValue) v).value;
									if (result) {
										TableColumn column = new TableColumn(policyTable, SWT.CENTER);
										String name = param.getText("name");
										MapValue mv = (MapValue) param.get("policy");
										column.setText(name);
										tableColumnLayout.setColumnData(column, new ColumnPixelData(100, true));
										int colIndex = policyTable.getColumnCount() - 1;
										groupList.add(name);
										TableItem[] items = policyTable.getItems();
										for (TableItem item : items) {
											String policy = item.getText(0);
											Value v = mv.get(policy);
											if (v == null) {
												item.setText(colIndex, "");
												continue;
											}
											BooleanValue bv = (BooleanValue) v;
											if (bv.value) {
												item.setText(colIndex, CHECK);
											} else {
												item.setText(colIndex, "");
											}
										}
										policyTable.setRedraw(true);
										comp.layout(true, true);
										MessageDialog.openInformation(getSite().getShell(), "Success"  , "Add successfully.");
									} else {
										MessageDialog.openError(getSite().getShell(), "Failed"  , "Add Failed. Please try again or contact administrator.");
									}
								}
							}
						});
					}
				});
			} catch(Throwable th) {
				ConsoleProxy.errorSafe(th.getMessage());
				return Status.CANCEL_STATUS;
			} finally {
				TcpProxy.putTcpProxy(tcp);
			}
			return Status.OK_STATUS;
		}
	}
}
