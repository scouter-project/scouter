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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import scouter.client.net.TcpProxy;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ChartUtil;
import scouter.client.util.ExUtil;
import scouter.client.util.RCPUtil;
import scouter.client.util.UIUtil;
import scouter.util.StringUtil;
import scouter.lang.Account;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.CipherUtil;

public class AccountDialog {

	public static short ADD_MODE = 1;
	public static short EDIT_MODE = 2;
	
	int mode = ADD_MODE;
	Account targetAccount;
	int serverId;
	Shell dialog;
	Text idText;
	Button dupCheckBtn;
	Text passText;
	Text rePassText;
	Text emailText;
	Button okBtn;
	String selectedGroup = null;

	List<Button> radiobuttons = new ArrayList<Button>();
	boolean confirmId = false;

	public AccountDialog(int serverId) {
		this.serverId = serverId;
	}
	
	public AccountDialog(int serverId, Account account) {
		this.serverId = serverId;
		this.targetAccount = account;
	}
	
	public void show(short mode) {
		this.mode = mode;
		dialog = new Shell(Display.getDefault(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		UIUtil.setDialogDefaultFunctions(dialog);
		if (mode == ADD_MODE) {
			dialog.setText("Add Account");
		} else if (mode == EDIT_MODE) {
			dialog.setText("Edit Account");
		}
		
		initialLayout();
		
		if (mode == ADD_MODE) {
			okBtn.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (validateOk()) {
						okBtn.setEnabled(false);
						addAccount();
					}
				}
			});
		} else if (mode == EDIT_MODE) {
			String group = null;
			if (targetAccount == null) {
				Server server = ServerManager.getInstance().getServer(serverId);
				idText.setText(server.getUserId());
				emailText.setText(server.getEmail());
				group = server.getGroup();
			} else {
				idText.setText(targetAccount.id);
				emailText.setText(targetAccount.email);
				group = targetAccount.group;
			}
			idText.setEnabled(false);
			dupCheckBtn.setEnabled(false);
			if (StringUtil.isNotEmpty(group)) {
				for (Button btn : radiobuttons) {
					if (btn.getText().equals(group)) {
						selectedGroup = group;
						btn.setSelection(true);
						break;
					}
				}
			}
			okBtn.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					confirmId = true;
					if (validateOk()) {
						okBtn.setEnabled(false);
						editAccount();
					}
				}
			});
		}
		dialog.pack();
		dialog.open();
	}

	private void initialLayout() {
		GridLayout layout = new GridLayout(1, true);
		layout.verticalSpacing = 10;
		dialog.setLayout(new GridLayout(1, true));
		Composite comp = new Composite(dialog, SWT.NONE);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		comp.setLayout(new FillLayout());
		Group profileGrp = new Group(comp, SWT.NONE);
		profileGrp.setText("Profile");
		profileGrp.setLayout(ChartUtil.gridlayout(3));

		GridData data = null;
		Label label = new Label(profileGrp, SWT.NONE);
		label.setText("ID");
		label.setAlignment(SWT.RIGHT);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = 100;
		label.setLayoutData(data);

		idText = new Text(profileGrp, SWT.SINGLE | SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		data.widthHint = 150;
		idText.setLayoutData(data);

		dupCheckBtn = new Button(profileGrp, SWT.PUSH);
		dupCheckBtn.setText("&Check");
		data = new GridData(SWT.FILL, SWT.CENTER, false, false);
		data.widthHint = 80;
		dupCheckBtn.setLayoutData(data);
		dupCheckBtn.setEnabled(false);
		
		idText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String id = idText.getText();
				confirmId = false;
				if (StringUtil.isEmpty(id)) {
					dupCheckBtn.setEnabled(false);
				} else {
					dupCheckBtn.setEnabled(true);
				}
			}
		});

		dupCheckBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String id = idText.getText();
				boolean avaliable = avaliableId(id);
				if (avaliable) {
					MessageDialog.openInformation(dialog, "Confirm", id
							+ " is available");
					confirmId = true;
				} else {
					MessageDialog.openWarning(dialog, "Duplicated", id
							+ " is exist");
					confirmId = false;
				}
			}
		});

		label = new Label(profileGrp, SWT.NONE);
		label.setText("Password");
		label.setAlignment(SWT.RIGHT);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		label.setLayoutData(data);

		passText = new Text(profileGrp, SWT.SINGLE | SWT.BORDER
				| SWT.PASSWORD);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		passText.setLayoutData(data);

		label = new Label(profileGrp, SWT.NONE);
		label.setText("");
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		label = new Label(profileGrp, SWT.NONE);
		label.setText("Re-Password");
		label.setAlignment(SWT.RIGHT);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		label.setLayoutData(data);

		rePassText = new Text(profileGrp, SWT.SINGLE | SWT.BORDER
				| SWT.PASSWORD);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		rePassText.setLayoutData(data);

		label = new Label(profileGrp, SWT.NONE);
		label.setText("");
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		label = new Label(profileGrp, SWT.NONE);
		label.setText("E-mail");
		label.setAlignment(SWT.RIGHT);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		label.setLayoutData(data);

		emailText = new Text(profileGrp, SWT.SINGLE | SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false);
		emailText.setLayoutData(data);

		label = new Label(profileGrp, SWT.NONE);
		label.setText("");
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		ListValue groupList = getGroupList();
		if (groupList != null && groupList.size() > 0) {
			Group typeGrp = new Group(profileGrp, SWT.NONE);
			typeGrp.setText("Group");
			data = new GridData(SWT.FILL, SWT.FILL, true, false);
			data.horizontalSpan = 3;
			typeGrp.setLayoutData(data);
			typeGrp.setLayout(new GridLayout((groupList.size() % 4) + 1, true));
			for (int i = 0; i < groupList.size(); i++) {
				final Button btn = new Button(typeGrp, SWT.RADIO);
				btn.setText(CastUtil.cString(groupList.get(i)));
				btn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				btn.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						selectedGroup = btn.getText();
					}
				});
				radiobuttons.add(btn);
			}
		}

		okBtn = new Button(dialog, SWT.PUSH);
		GridData gd = new GridData(SWT.RIGHT, SWT.FILL, false, false);
		gd.widthHint = 100;
		okBtn.setLayoutData(gd);
		okBtn.setText("&Ok");
	}

	private boolean avaliableId(String id) {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		MapPack p = new MapPack();
		try {
			MapPack param = new MapPack();
			param.put("id", id);
			p = (MapPack) tcp.getSingle(RequestCmd.CHECK_ACCOUNT_ID, param);

		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return p.getBoolean("result");
	}
	
	private boolean validateOk() {
		if (confirmId == false) {
			MessageDialog.openWarning(dialog, "Confirm ID", "Please check ID");
			return false;
		}
		String password = passText.getText();
		String repassword = rePassText.getText();
		if (StringUtil.isEmpty(password)) {
			MessageDialog.openWarning(dialog, "Confirm Password", "Please check password");
			return false;
		}
		if (password.equals(repassword) == false) {
			MessageDialog.openWarning(dialog, "Confirm Password", "Please check re-password");
			return false;
		}
		if (selectedGroup == null) {
			MessageDialog.openWarning(dialog, "Confirm Group", "Please check group");
			return false;
		}
		return true;
	}
	
	private void addAccount() {
		final String id = idText.getText();
		final String password = passText.getText();
		final String email = emailText.getText();
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				boolean result = false;
				try {
					MapPack param = new MapPack();
					param.put("id", id);
					param.put("pass", CipherUtil.sha256(password));
					param.put("email", email);
					param.put("group", selectedGroup);
					MapPack p = (MapPack) tcp.getSingle(RequestCmd.ADD_ACCOUNT, param);
					result = p.getBoolean("result");
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				final boolean finalResult = result;
				ExUtil.exec(dialog, new Runnable() {
					public void run() {
						if (finalResult) {
							MessageDialog.openInformation(dialog, "Success[Add Account]", "Your registration has been successful");
							dialog.close();
						} else {
							MessageDialog.openError(dialog, "Failed[Add Account]", "Your registration failed");
							okBtn.setEnabled(true);
						}
					}
				});
			}
		});
		
	}
	
	private void editAccount() {
		final String id = idText.getText();
		final String password = passText.getText();
		final String email = emailText.getText();
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				boolean result = false;
				try {
					MapPack param = new MapPack();
					param.put("id", id);
					param.put("pass", CipherUtil.sha256(password));
					param.put("email", email);
					param.put("group", selectedGroup);
					MapPack p = (MapPack) tcp.getSingle(RequestCmd.EDIT_ACCOUNT, param);
					result = p.getBoolean("result");
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				final boolean finalResult = result;
				ExUtil.exec(dialog, new Runnable() {
					public void run() {
						if (finalResult) {
							if (ServerManager.getInstance().getServer(serverId).getUserId().equals(id)) {
								MessageDialog.openInformation(dialog, "Success[Edit Account]", "Your modification has been successful.\nYou will be restart.");
								RCPUtil.restart();
							} else {
								MessageDialog.openInformation(dialog, "Success[Edit Account]", "Your modification has been successful.");
								dialog.close();
							}
						} else {
							MessageDialog.openError(dialog, "Failed[Edit Account]", "Your modification failed");
							okBtn.setEnabled(true);
						}
					}
				});
			}
		});
		
	}

	private ListValue getGroupList() {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		ListValue lv = null;
		try {
			MapPack p = (MapPack) tcp.getSingle(RequestCmd.LIST_ACCOUNT_GROUP,
					null);
			lv = p.getList("group_list");
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return lv;
	}
}
