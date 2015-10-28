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
package scouter.client.configuration.actions;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchWindow;

import scouter.client.Activator;
import scouter.client.Images;
import scouter.client.net.TcpProxy;
import scouter.client.remote.RemoteCmd;
import scouter.client.server.ServerManager;
import scouter.client.util.ColorUtil;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageCombo;
import scouter.client.util.UIUtil;
import scouter.util.StringUtil;
import scouter.lang.counters.CounterEngine;
import scouter.lang.pack.MapPack;
import scouter.lang.value.BooleanValue;
import scouter.lang.value.Value;
import scouter.net.RequestCmd;

public class DefineObjectTypeAction extends Action {
	
	public static final int DEFINE_MODE = 0;
	public static final int EDIT_MODE = 1;
	
	private final IWorkbenchWindow window;
	int serverId;
	String objType;
	int mode;
	
	public DefineObjectTypeAction(IWorkbenchWindow window, int serverId, String objType, int mode) {
		this.window = window;
		this.serverId = serverId;
		this.objType = objType;
		this.mode = mode;
		if (mode == DEFINE_MODE) {
			setText("Define Object Type");
		} else if (mode == EDIT_MODE) {
			setText("Edit Object Type");
		}
	}

	public void run() {
		new DefineObjectTypeDialog().show(objType);
	}
	
	class DefineObjectTypeDialog {
		Text objTypeTxt;
		Text displayTxt;
		ImageCombo familyCombo;
		ImageCombo iconCombo;
		Button subObjectCheck;
		CounterEngine counterEngine = ServerManager.getInstance().getServer(serverId).getCounterEngine();
		
		void show(String objType) {
			final Shell dialog = new Shell(window.getShell(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			UIUtil.setDialogDefaultFunctions(dialog);
			if (mode == DEFINE_MODE) {
				dialog.setText("Define Object Type");
			} else if (mode == EDIT_MODE) {
				dialog.setText("Edit Object Type");
			}
			dialog.setLayout(new GridLayout(1, true));
			Composite mainComp = new Composite(dialog, SWT.NONE);
			mainComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			mainComp.setLayout(new GridLayout(2, false));
			GridData gr;
			Label label = new Label(mainComp, SWT.NONE);
			label.setText("Object Type");
			gr = new GridData(SWT.LEFT, SWT.CENTER, false, false);
			gr.widthHint = 100;
			label.setLayoutData(gr);
			objTypeTxt = new Text(mainComp, SWT.SINGLE | SWT.BORDER);
			gr = new GridData(SWT.FILL, SWT.CENTER, true, false);
			objTypeTxt.setLayoutData(gr);
			objTypeTxt.setText(objType);
			objTypeTxt.setEnabled(false);
			
			label = new Label(mainComp, SWT.NONE);
			label.setText("Display Name");
			gr = new GridData(SWT.LEFT, SWT.CENTER, false, false);
			label.setLayoutData(gr);
			displayTxt = new Text(mainComp, SWT.SINGLE | SWT.BORDER);
			gr = new GridData(SWT.FILL, SWT.CENTER, true, false);
			gr.widthHint = 200;
			displayTxt.setLayoutData(gr);
			
			label = new Label(mainComp, SWT.NONE);
			label.setText("Family");
			gr = new GridData(SWT.LEFT, SWT.CENTER, false, false);
			
			familyCombo = new ImageCombo(mainComp, SWT.READ_ONLY | SWT.BORDER);
			familyCombo.setBackground(ColorUtil.getInstance().getColor("white"));
			gr = new GridData(SWT.FILL, SWT.CENTER, true, false);
			familyCombo.setLayoutData(gr);
			String[] familys = counterEngine.getFamilyNames();
			for (String family : familys) {
				familyCombo.add(family, null);
			}
			familyCombo.select(0);
			
			label = new Label(mainComp, SWT.NONE);
			label.setText("Icon");
			gr = new GridData(SWT.LEFT, SWT.CENTER, false, false);
			label.setLayoutData(gr);
			iconCombo = new ImageCombo(mainComp, SWT.READ_ONLY | SWT.BORDER);
			iconCombo.setBackground(ColorUtil.getInstance().getColor("white"));
			gr = new GridData(SWT.FILL, SWT.CENTER, true, false);
			iconCombo.setLayoutData(gr);
			try {
				Enumeration<URL> en = Activator.getDefault().getBundle().findEntries("icons/object/", "*.png", false);
				while (en.hasMoreElements()) {
					URL url = en.nextElement();
					url = FileLocator.resolve(url);
					String name = new File(url.getFile()).getName();
					if (name.contains("_inact.")) {
						continue;
					}
					Image img = Activator.getImage("icons/object/" + name);
					iconCombo.add(name, img);
					iconCombo.setData(name, name.substring(0, name.lastIndexOf(".")));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			subObjectCheck = new Button(mainComp, SWT.CHECK);
			subObjectCheck.setText("Sub-object");
			subObjectCheck.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false, 2 ,1));
			
			CLabel warnLabel = new CLabel(mainComp, SWT.NONE);
			warnLabel.setImage(Images.exclamation);
			warnLabel.setText("This setting will affect all clients");
			gr = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
			warnLabel.setLayoutData(gr);
			
			if (mode == DEFINE_MODE) {
				ArrayList<String> objTypeList = counterEngine.getAllObjectType();
				for (String anotherObjType : objTypeList) {
					if (objType.equals(anotherObjType)) continue;
					if (objType.startsWith(anotherObjType)) {
						subObjectCheck.setSelection(true);
						break;
					}
				}
			} else if (mode == EDIT_MODE) {
				displayTxt.setText(counterEngine.getDisplayNameObjectType(objType));
				familyCombo.setText(counterEngine.getObjectType(objType).getFamily().getName());
				String icon = counterEngine.getObjectType(objType).getIcon();
				iconCombo.setText((icon == null ? objType : icon) + ".png");
				subObjectCheck.setSelection(counterEngine.getObjectType(objType).isSubObject());
			}
			
			Composite bottomComp = new Composite(dialog, SWT.NONE);
			bottomComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
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
					final String display = displayTxt.getText();
					if (StringUtil.isEmpty(display)) {
						MessageDialog.openWarning(dialog, "Required Filed", "DisplayName is required");
						return;
					}
					final String type = objTypeTxt.getText();
					final String displayName = displayTxt.getText();
					final String family = familyCombo.getText();
					final String icon = (String) iconCombo.getData(iconCombo.getText());
					final BooleanValue subObject =  new BooleanValue(subObjectCheck.getSelection());
					ExUtil.asyncRun(new Runnable() {
						public void run() {
							TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
							try {
								MapPack param = new MapPack();
								param.put(CounterEngine.ATTR_NAME, type);
								param.put(CounterEngine.ATTR_DISPLAY, displayName);
								param.put(CounterEngine.ATTR_FAMILY, family);
								param.put(CounterEngine.ATTR_ICON, icon);
								param.put(CounterEngine.ATTR_SUBOBJECT, subObject);
								String requestCmd = (mode == EDIT_MODE) ? RequestCmd.EDIT_OBJECT_TYPE : RequestCmd.DEFINE_OBJECT_TYPE;
								final Value v = tcp.getSingleValue(requestCmd, param);
								ExUtil.exec(window.getShell(), new Runnable() {
									public void run() {
										if (v != null && ((BooleanValue) v).value) {
											MessageDialog.openInformation(window.getShell(), "Success", ((mode == EDIT_MODE) ? "Edit" : "Add") + " successfully.");
											ExUtil.asyncRun(new Runnable() {
												public void run() {
													TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
													try {
														MapPack param = new MapPack();
														param.put("command", RemoteCmd.REFETCH_COUNTER_XML);
														param.put("fromSession", ServerManager.getInstance().getServer(serverId).getSession());
														tcp.getSingle(RequestCmd.REMOTE_CONTROL_ALL, param);
													} catch (Exception e) {
														ConsoleProxy.errorSafe(e.toString());
													} finally {
														TcpProxy.putTcpProxy(tcp);
													}
												}
											});
										} else {
											MessageDialog.openError(window.getShell(), "Failed"  , "Add Failed. Please try again or contact administrator.");
										}
									}
								});
							} finally {
								TcpProxy.putTcpProxy(tcp);
							}
						}
					});
					dialog.close();
				}
			});
			
			dialog.pack();
			dialog.open();
		}
	}
}
