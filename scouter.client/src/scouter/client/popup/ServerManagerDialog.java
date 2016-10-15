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
package scouter.client.popup;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import scouter.client.Images;
import scouter.client.popup.AddServerAddressDialog.ServerAddressAddition;
import scouter.client.popup.LoginDialog2.ILoginDialog;
import scouter.client.preferences.ServerPrefUtil;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ColorUtil;
import scouter.client.util.ExUtil;
import scouter.client.util.UIUtil;

public class ServerManagerDialog implements ServerAddressAddition, ILoginDialog {
	Table table;
	Set<String> addrSet = new HashSet<String>();
	
	public void show() {
		final Shell dialog = new Shell(Display.getDefault(), SWT.DIALOG_TRIM);
		UIUtil.setDialogDefaultFunctions(dialog);
		dialog.setLayout(new GridLayout(2, false));
		dialog.setText("Server");
		CLabel title = new CLabel(dialog, SWT.NONE);
		GridData gr = new GridData(SWT.LEFT, SWT.CENTER, true, true, 2, 1);
		title.setLayoutData(gr);
		title.setFont(new Font(null, "Arial", 10, SWT.BOLD));
		title.setImage(Images.SERVER_ACT);
		title.setText("Server List");
		
		table = new Table(dialog,  SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		gr = new GridData(SWT.FILL, SWT.FILL, true, true);
		gr.widthHint = 250;
		gr.heightHint = 350;
		table.setLayoutData(gr);
		
		Composite buttonComp = new Composite(dialog, SWT.NONE);
		gr = new GridData(SWT.FILL, SWT.FILL, false, true);
		buttonComp.setLayoutData(gr);
		buttonComp.setLayout(UIUtil.formLayout(3, 3));
		
		Button addBtn = new Button(buttonComp, SWT.PUSH);
		addBtn.setLayoutData(UIUtil.formData(null, -1, 0, 5, null, -1, null, -1, 100));
		addBtn.setText("&Add");
		addBtn.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				new AddServerAddressDialog(e.widget.getDisplay(), ServerManagerDialog.this, addrSet).show(e.widget.getDisplay().getBounds());
			}
		});
		
		Button importBtn = new Button(buttonComp, SWT.PUSH);
		importBtn.setLayoutData(UIUtil.formData(null, -1, addBtn, 5, null, -1, null, -1, 100));
		importBtn.setText("&Import");
		importBtn.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				Shell shell = new Shell(e.widget.getDisplay());
			    FileDialog fileDialog = new FileDialog(shell);
			    fileDialog.setFilterExtensions(new String[] { "*.tns" });
			    fileDialog.setFilterNames(new String[] { "Server List File(*.tns)"});
			    final String path = fileDialog.open();
			    if(path != null && !"".equals(path)){
			    	ExUtil.asyncRun(new Runnable() {
						public void run() {
							try {
								BufferedReader br = new BufferedReader(new FileReader(path));
								String s = null;
								final Set<String> addedSet = new HashSet<String>();
								while ((s = br.readLine()) != null) {
									if (addrSet.contains(s)) continue;
									try {
										String str[] = s.split(":");
										if (str != null && str.length == 2) {
											addedSet.add(s);
										}
									} catch (Exception e) { }
								}
								ExUtil.exec(Display.getDefault(), new Runnable() {
									public void run() {
										for (String newAddress : addedSet) {
											addServerAddress(newAddress);
										}
									}
								});
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
			    }
			}
		});
		
		final Button authBtn = new Button(buttonComp, SWT.PUSH);
		authBtn.setLayoutData(UIUtil.formData(null, -1, importBtn, 10, null, -1, null, -1, 100));
		authBtn.setText("&Auth.");
		authBtn.setEnabled(false);
		authBtn.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				TableItem items[] = table.getSelection();
				if (items != null && items.length > 0) {
					String addr = (String) items[0].getData();
					if (items[0].getForeground().getRGB().equals(ColorUtil.getInstance().getColor(SWT.COLOR_RED).getRGB())) {
						LoginDialog2 loginDialog  = new LoginDialog2(dialog, ServerManagerDialog.this, LoginDialog2.TYPE_ADD_SERVER, addr);
						loginDialog.open();
					} else {
						LoginDialog2 loginDialog  = new LoginDialog2(dialog, ServerManagerDialog.this, LoginDialog2.TYPE_EDIT_SERVER, addr);
						loginDialog.open();
					}
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
				TableItem[] items = table.getItems();
				for (TableItem item : items) {
					if (item.getForeground().getRGB().equals(ColorUtil.getInstance().getColor(SWT.COLOR_RED).getRGB())) {
						String addr = (String) item.getData();
						ServerPrefUtil.addServerAddr(addr);
						String addrs[] = addr.split(":");
						Server server = new Server(addrs[0], addrs[1]);
						ServerManager.getInstance().addServer(server);
					}
				}
				dialog.close();
			}
		});
		
		table.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				if (e.item instanceof TableItem) {
					authBtn.setEnabled(true);
					return;
				}
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		Enumeration<Integer> allServerSet = ServerManager.getInstance().getAllServerList();
		while( allServerSet.hasMoreElements()) {
			int serverId =allServerSet.nextElement();
			Server server = ServerManager.getInstance().getServer(serverId);
			if (server == null) continue;
			TableItem item = new TableItem(table, SWT.NONE);
			String str = server.getIp() + ":" + server.getPort();
			item.setData(str);
			addrSet.add(str);
			if (server.isOpen()) {
				str += " (" + server.getName() + ")";
				if (server.isConnected()) {
					str += " - Running";
				} else {
					str += " - Disconnected";
				}
			} else {
				str += " - Closed";
			}
			item.setText(str);
		}
		
		dialog.pack();
		dialog.open();
	}

	public void addServerAddress(String newAddress) {
		TableItem item = new TableItem(table, SWT.NONE);
		item.setForeground(ColorUtil.getInstance().getColor(SWT.COLOR_RED));
		item.setData(newAddress);
		item.setText(newAddress);
		addrSet.add(newAddress);
	}

	@Override
	public void loginSuccess(String serverAddr, int serverId) {
		TableItem items[] = table.getSelection();
		if (items[0].getData().equals(serverAddr)) {
			items[0].setForeground(null);
			Server server = ServerManager.getInstance().getServer(serverId);
			if (server.isOpen()) {
				items[0].setText(serverAddr + " (" + server.getName() + ") - Running");
			}
		}
	}
}
