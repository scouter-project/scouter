/*
 *  Copyright 2015 LG CNS.
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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import scouter.client.Activator;
import scouter.client.net.LoginMgr;
import scouter.client.preferences.ServerPrefUtil;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.UIUtil;
import scouter.net.NetConstants;
import scouter.util.CipherUtil;
import scouter.util.StringUtil;

public class LoginDialog {
	public static final String ID = LoginDialog.class.getName();

	private final Display display;
	private final Shell shell;
	private final ILoginDialog callback;
	private final int openType;
	public static final int TYPE_STARTUP = 991;
	public static final int TYPE_ADD_SERVER = 992;
	public static final int TYPE_OPEN_SERVER = 993;
	public static final int TYPE_EDIT_SERVER = 994;

	ArrayList<String> storeAddr = new ArrayList<String>();
	FormData data;
	Combo addrCombo;
	Text id, pass;
	Label idLabel, passLabel;

	List list;

	Button autoLoginCheck, showPass;
	boolean autoLogin;

	String address = null;
	boolean showPassword = false;

	public LoginDialog(Display display, ILoginDialog callback, int openType) {
		this(display, callback, openType, null);
	}

	public LoginDialog(Display display, ILoginDialog callback, int openType, String address) {
		this.display = display;
		this.shell = new Shell(display, (SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL) & ~SWT.CLOSE);
		this.callback = callback;
		this.openType = openType;
		this.address = address;
	}

	public void show() {
		switch (openType) {
		case TYPE_STARTUP:
			shell.setText("Scouter Start");
			this.address = ServerPrefUtil.getStoredDefaultServer();
			break;
		case TYPE_ADD_SERVER:
			shell.setText("Add Server");
			break;
		case TYPE_OPEN_SERVER:
			shell.setText("Open Server");
			break;
		case TYPE_EDIT_SERVER:
			shell.setText("Edit Server");
			break;
		default:
			shell.setText("Scouter");
			break;
		}
		shell.setImage(Activator.getImage("icons/h128.png"));
		shell.setLayout(UIUtil.formLayout(5, 5));

		final Group parentGroup = new Group(shell, SWT.NONE);
		parentGroup.setText("Authentication Info");
		parentGroup.setLayout(UIUtil.formLayout(5, 5));
		parentGroup.setLayoutData(UIUtil.formData(null, -1, 0, 0, null, -1, null, -1));

		Label addrLabel = new Label(parentGroup, SWT.RIGHT);
		addrLabel.setText("Server Address :");
		addrLabel.setLayoutData(UIUtil.formData(null, -1, 0, 10, null, -1, null, -1, 100));

		String[] addrs = ServerPrefUtil.getStoredServerList();

		addrCombo = new Combo(parentGroup, SWT.VERTICAL | SWT.BORDER | SWT.H_SCROLL);
		if (addrs != null && addrs.length > 0) {
			addrCombo.setItems(addrs);
		}
		addrCombo.setEnabled(true);
		addrCombo.setLayoutData(UIUtil.formData(addrLabel, 5, 0, 7, 100, -5, null, -1, 150));

		idLabel = new Label(parentGroup, SWT.RIGHT);
		idLabel.setText("ID :");
		idLabel.setLayoutData(UIUtil.formData(null, -1, addrCombo, 10, null, -1, null, -1, 100));

		id = new Text(parentGroup, SWT.SINGLE | SWT.BORDER);
		id.setLayoutData(UIUtil.formData(idLabel, 5, addrCombo, 7, 100, -5, null, -1));
		id.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				id.selectAll();
			}
		});

		passLabel = new Label(parentGroup, SWT.RIGHT);
		passLabel.setText("Password :");
		passLabel.setLayoutData(UIUtil.formData(null, -1, id, 10, null, -1, null, -1, 100));

		createPasswordInput(parentGroup, showPassword, "");

		showPass = new Button(parentGroup, SWT.CHECK);
		showPass.setText("Show password");
		showPass.setSelection(showPassword);
		showPass.setLayoutData(UIUtil.formData(null, -1, passLabel, 10, 100, -5, null, -1));
		showPass.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				String ontyping = pass.getText();
				if (pass != null && !pass.isDisposed()) {
					pass.dispose();
				}
				createPasswordInput(parentGroup, showPass.getSelection(), ontyping);
				parentGroup.layout();
			}
		});

		autoLoginCheck = new Button(parentGroup, SWT.CHECK);
		autoLoginCheck.setText("Auto Login");
		autoLoginCheck.setLayoutData(UIUtil.formData(null, -1, showPass, 10, 100, -5, null, -1));
		autoLoginCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (autoLoginCheck.getSelection()) {
					autoLogin = true;
				} else {
					autoLogin = false;
				}
			}
		});
		autoLoginCheck.setSelection(false);

		list = new List(parentGroup, SWT.NONE);
		list.setLayoutData(UIUtil.formData(0, 5, autoLoginCheck, 10, 100, -5, null, -1, -1, 60));
		list.add("Type your authentication info...");
		list.select(list.getItemCount() - 1);
		list.showSelection();

		Composite footer = new Composite(shell, SWT.NONE);
		footer.setLayoutData(UIUtil.formData(0, 5, parentGroup, 10, 100, -5, null, -1));

		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.makeColumnsEqualWidth = true;
		footer.setLayout(gridLayout);

		Button okButton = new Button(footer, SWT.PUSH);
		okButton.setText("&OK");
		okButton.setEnabled(true);
		okButton.setLayoutData(new GridData(GridData.FILL_BOTH));
		okButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				boolean success = false;
				success = loginInToServer(addrCombo.getText());
				if (success) {
					shell.close();
					shell.dispose();
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		Button cancelButton = new Button(footer, SWT.PUSH);
		cancelButton.setText("&Cancel");
		cancelButton.setLayoutData(new GridData(GridData.FILL_BOTH));
		cancelButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				shell.close();
				shell.dispose();
				if (callback != null) {
					callback.onPressedCancel();
				}
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		if (StringUtil.isNotEmpty(this.address)) {
			addrCombo.setText(address);
			Server server = ServerManager.getInstance().getServer(address);
			if (server != null && StringUtil.isNotEmpty(server.getUserId())) {
				id.setText(server.getUserId());
			}
			autoLoginCheck.setSelection(ServerPrefUtil.isAutoLoginAddress(address));
		} else if (openType == TYPE_STARTUP){
			addrCombo.setText("127.0.0.1:" + NetConstants.DATATCP_SERVER_PORT);
			id.setText("admin");
		}

		shell.setDefaultButton(okButton);

		// Text aboutLabel = new Text(shell, SWT.BORDER | SWT.READ_ONLY |
		// SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL);
		// aboutLabel.setLayoutData(UIUtil.formData(0, 5, footer, 10, 100, -5,
		// null, -1, 100, 70));
		// aboutLabel.setText(ApplicationWorkbenchWindowAdvisor.aboutText.trim());

		shell.pack();

		// POSITION SETTING - SCREEN CENTER
		Monitor primaryMonitor = display.getPrimaryMonitor();
		Rectangle bounds = primaryMonitor.getBounds();
		Rectangle rect = shell.getBounds();
		int x = bounds.x + (bounds.width - rect.width) / 2;
		int y = bounds.y + (bounds.height - rect.height) / 2;
		shell.setLocation(x, y);

		shell.addListener(SWT.Traverse, new Listener() {
			public void handleEvent(Event event) {
				switch (event.detail) {
				case SWT.TRAVERSE_ESCAPE:
					shell.close();
					event.detail = SWT.TRAVERSE_NONE;
					event.doit = false;
					if (callback != null) {
						callback.onPressedCancel();
					}
					break;
				}
			}
		});

		shell.open();
	}

	public void close() {
		if (shell != null) {
			shell.close();
		}
	}

	private void createPasswordInput(Composite parentGroup, boolean showPassword, String ontyping) {
		if (showPassword) {
			pass = new Text(parentGroup, SWT.SINGLE | SWT.BORDER);
		} else {
			pass = new Text(parentGroup, SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);
		}
		pass.setText(ontyping);
		pass.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				pass.selectAll();
			}
		});
		pass.setLayoutData(UIUtil.formData(passLabel, 5, id, 7, 100, -5, null, -1));
	}

	public boolean loginInToServer(String address) {
		Server server = null;
		if (StringUtil.isEmpty(address)) {
			errMsg("Please check server address");
			msg("");
			return false;
		}
		address = address.trim();
		boolean existServer = false;
		try {
			String ip = null;
			String port = null;
			if (address.contains(":") == false) {
				address = address.concat(":" + NetConstants.DATATCP_SERVER_PORT);
			}
			String addr[] = address.split(":");
			ip = addr[0];
			port = addr[1];
			msg("Logging in..." + address);

			ServerManager srvMgr = ServerManager.getInstance();
			if (this.openType != TYPE_EDIT_SERVER && srvMgr.isRunningServer(ip, port)) {
				errMsg("Already running server");
				msg("");
				return false;
			}

			server = new Server(ip, port);
			if (srvMgr.getServer(server.getId()) == null) {
				srvMgr.addServer(server);
			} else {
				existServer = true;
			}

			boolean success = LoginMgr.login(server.getId(), id.getText(), pass.getText());
			if (success) {
				msg("Successfully log in to " + address);
				ServerPrefUtil.addServerAddr(address);
				if (autoLogin) {
					ServerPrefUtil.addAutoLoginServer(address, id.getText(), CipherUtil.md5(pass.getText()));
				} else {
					ServerPrefUtil.removeAutoLoginServer(address);
				}
				msg("Completed !!!");
				msg("");
				if (callback != null) {
					callback.onPressedOk(address, server.getId());
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return true;
			} else {
				if (existServer == false) {
					ServerManager.getInstance().removeServer(server.getId());
				}
				errMsg("Please check your ID/Password or network.");
				msg("");
				return false;
			}
		} catch (Exception e) {
			if (server != null && existServer == false) {
				ServerManager.getInstance().removeServer(server.getId());
			}
			e.printStackTrace();
			MessageDialog.openError(shell, "Error", "error occured:" + e.getMessage());
		}
		return false;
	}

	private void msg(String msg) {
		list.add(msg);
		list.select(list.getItemCount() - 1);
		list.showSelection();
	}

	private void errMsg(String msg) {
		msg(msg);
	}

	public interface ILoginDialog {
		void onPressedOk(String serverAddr, int serverId);

		void onPressedCancel();
	}

}
