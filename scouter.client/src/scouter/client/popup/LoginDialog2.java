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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;
import scouter.Version;
import scouter.client.net.LoginMgr;
import scouter.client.net.LoginResult;
import scouter.client.popup.event.AbstractFocusGainedListener;
import scouter.client.preferences.ServerPrefUtil;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.UIUtil;
import scouter.net.NetConstants;
import scouter.util.StringUtil;
import scouter.util.ThreadUtil;

public class LoginDialog2 extends Dialog {
	public static final String ID = LoginDialog2.class.getName();

	private final Shell shell;
	private final ILoginDialog callback;
	private final int openType;
	public static final int TYPE_STARTUP = 991;
	public static final int TYPE_ADD_SERVER = 992;
	public static final int TYPE_OPEN_SERVER = 993;
	public static final int TYPE_EDIT_SERVER = 994;

	public static final int SKIP_LOGIN = 99;

	Combo addrCombo;
	Combo socksAddrCombo;

	Text idText;
	Text passText;

	Label idLabel;
	Label passLabel;

	List messageList;

	Button autoLoginCheck;
    Button secureCheck;
    Button sock5Check;

	boolean autoLogin;
	boolean secureLogin = true;
	boolean sock5Login;

	String address;
	String socksAddress;

	public LoginDialog2(Shell shell, ILoginDialog callback, int openType, String address, String socksAddress) {
		super(shell);
		this.shell = shell;
		this.callback = callback;
		this.openType = openType;
		this.address = address;
		this.socksAddress = socksAddress;
		this.sock5Login = StringUtil.isNotEmpty(socksAddress);
	}


	@Override
	protected Control createDialogArea(Composite parent) {
		Composite comp = (Composite) super.createDialogArea(parent);
		comp.setLayout(UIUtil.formLayout(5, 5));
		final Group parentGroup = new Group(comp, SWT.NONE);
		parentGroup.setText("Authentication Info");
		parentGroup.setLayout(UIUtil.formLayout(5, 5));
		parentGroup.setLayoutData(UIUtil.formData(0, 0, 0, 0, 100, 0, null, -1));

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

		idText = new Text(parentGroup, SWT.SINGLE | SWT.BORDER);
		idText.setLayoutData(UIUtil.formData(idLabel, 5, addrCombo, 7, 100, -5, null, -1));
		idText.addFocusListener(new AbstractFocusGainedListener() {
			public void focusGained(FocusEvent e) {
				idText.selectAll();
			}
		});

		passLabel = new Label(parentGroup, SWT.RIGHT);
		passLabel.setText("Password :");
		passLabel.setLayoutData(UIUtil.formData(null, -1, idText, 10, null, -1, null, -1, 100));

		createPasswordInput(parentGroup);

		autoLoginCheck = new Button(parentGroup, SWT.CHECK);
		autoLoginCheck.setText("Auto Login");
		autoLoginCheck.setLayoutData(UIUtil.formData(null, -1, passLabel, 10, 100, -5, null, -1));
		autoLoginCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
                autoLogin = autoLoginCheck.getSelection();
			}
		});
		autoLoginCheck.setSelection(false);

		// to hash password before transfer, default true
		secureCheck = new Button(parentGroup, SWT.CHECK);
		secureCheck.setText("Secure Login");
		secureCheck.setLayoutData(UIUtil.formData(null, -1, passLabel, 10, autoLoginCheck, -5, null, -1));
		secureCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
                secureLogin = secureCheck.getSelection();
			}
		});
		secureCheck.setSelection(true);

		// console group
		final Group socksGroup = new Group(comp, SWT.NONE);
		socksGroup.setText("SOCKS5");
		socksGroup.setLayout(UIUtil.formLayout(5, 5));
		socksGroup.setLayoutData(UIUtil.formData(0, 0, parentGroup, 0, 100, 0, null, -1));

		// to use SOCKS5
		sock5Check = new Button(socksGroup, SWT.CHECK|SWT.LEFT);
		sock5Check.setText("SOCKS5");
		sock5Check.setLayoutData(UIUtil.formData(0, 0, 0, 5, 100, -5, null, -1));
		sock5Check.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
                sock5Login = sock5Check.getSelection();
				socksAddrCombo.setEnabled(sock5Login);
			}
		});
		sock5Check.setSelection(false);

		// socks5 address
		Label socks5AddrLabel = new Label(socksGroup, SWT.RIGHT);
		socks5AddrLabel.setText("SOCKS5 Address :");
		socks5AddrLabel.setLayoutData(UIUtil.formData(null, -1, sock5Check, 10, null, -1, null, -1, 100));

		// socks5 address combo
		String[] socks5Addrs = ServerPrefUtil.getStoredSocks5ServerList();

		socksAddrCombo = new Combo(socksGroup, SWT.VERTICAL | SWT.BORDER | SWT.H_SCROLL);
		if (socks5Addrs != null && socks5Addrs.length > 0) {
			socksAddrCombo.setItems(socks5Addrs);
		}
		socksAddrCombo.setEnabled(sock5Login);
		socksAddrCombo.setLayoutData(UIUtil.formData(socks5AddrLabel, 5, sock5Check, 10, 100, -5, null, -1, 150));


		// console group
		final Group consoleGroup = new Group(comp, SWT.NONE);
		consoleGroup.setLayout(UIUtil.formLayout(5, 5));
		consoleGroup.setLayoutData(UIUtil.formData(0, 0, socksGroup, 0, 100, 0, null, -1));

		// connection status console
		messageList = new List(consoleGroup, SWT.NONE);
		messageList.setLayoutData(UIUtil.formData(0, 5, 0, 5, 100, -5, null, -1, 250, 60));
		messageList.add("Type your authentication info...");
		messageList.select(messageList.getItemCount() - 1);
		messageList.showSelection();

		if (StringUtil.isNotEmpty(this.address)) {
			addrCombo.setText(address);
			Server server = ServerManager.getInstance().getServer(address);
			if (server != null && StringUtil.isNotEmpty(server.getUserId())) {
				idText.setText(server.getUserId());
				secureCheck.setSelection(server.isSecureMode());
			}
			autoLoginCheck.setSelection(ServerPrefUtil.isAutoLoginAddress(address));
		} else if (openType == TYPE_STARTUP) {
			addrCombo.setText("127.0.0.1:" + NetConstants.SERVER_TCP_PORT);
			idText.setText("admin");
		}

		if (StringUtil.isNotEmpty(this.socksAddress)) {
			socksAddrCombo.setText(socksAddress);
			sock5Check.setSelection(true);
			Server server = ServerManager.getInstance().getServer(address);
			if (server != null && StringUtil.isNotEmpty(server.getUserId())) {
				idText.setText(server.getUserId());
				secureCheck.setSelection(server.isSecureMode());
			}
			autoLoginCheck.setSelection(ServerPrefUtil.isAutoLoginAddress(address));
		}

		return comp;
	}

	@Override
	protected Point getInitialLocation(Point initialSize) {
		Monitor primaryMonitor = Display.getDefault().getPrimaryMonitor();
		Rectangle bounds = primaryMonitor.getBounds();
		int x = bounds.x + (bounds.width) / 2;
		int y = bounds.y + (bounds.height) / 2;
		return new Point(x, y);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		switch (openType) {
		case TYPE_STARTUP:
			newShell.setText(Version.getClientFullVersion());
			this.address = ServerPrefUtil.getStoredDefaultServer();
			break;
		case TYPE_ADD_SERVER:
			newShell.setText("Add Server");
			break;
		case TYPE_OPEN_SERVER:
			newShell.setText("Open Server");
			break;
		case TYPE_EDIT_SERVER:
			newShell.setText("Edit Server");
			break;
		default:
			newShell.setText("Login");
			break;
		}
	}

	@Override
	protected boolean isResizable() {
		return false;
	}

	private void createPasswordInput(Composite parentGroup) {
		passText = new Text(parentGroup, SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);
		passText.addFocusListener(new AbstractFocusGainedListener() {
			public void focusGained(FocusEvent e) {
				passText.selectAll();
			}
		});
		passText.setLayoutData(UIUtil.formData(passLabel, 5, idText, 7, 100, -5, null, -1));
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		if (openType == TYPE_STARTUP) {
			createButton(parent, SKIP_LOGIN, "Skip", false);
		}
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == SKIP_LOGIN) {
			setReturnCode(SKIP_LOGIN);
			close();
			return;
		}
		super.buttonPressed(buttonId);
	}

	@Override
	protected void okPressed() {
		if (loginInToServer(addrCombo.getText(), socksAddrCombo.getText())) {
			super.okPressed();
		}
	}

	public boolean loginInToServer(String address, String socksAddress) {
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
			if (!address.contains(":")) {
				address = address.concat(":" + NetConstants.SERVER_TCP_PORT);
			}
			String[] addr = address.split(":");
			ip = addr[0];
			port = addr[1];

			String socksIp = null;
			String socksPort = null;
			if (this.sock5Login) {
				if (StringUtil.isEmpty(socksAddress) || !socksAddress.contains(":")) {
					errMsg("Check SOCKS Address");
					return false;
				}
				String[] socksAddr = socksAddress.split(":");
				socksIp = socksAddr[0];
				socksPort = socksAddr[1];
			}

			msg("Log in..." + address);

			ServerManager srvMgr = ServerManager.getInstance();
			if (this.openType != TYPE_EDIT_SERVER && srvMgr.isRunningServer(ip, port)) {
				errMsg("Already running server");
				msg("");
				return false;
			}


			server = new Server(ip, port,null, socksIp, socksPort);
			if (srvMgr.getServer(server.getId()) == null) {
				srvMgr.addServer(server);
			} else {
				existServer = true;
				server = srvMgr.getServer(server.getId());
				server.setSocksIp(socksIp);
				server.setSocksPort(socksPort == null ? 0 : Integer.parseInt(socksPort));
			}

			LoginResult result = LoginMgr.login(server.getId(), idText.getText(), passText.getText());
			if (result.success) {
				msg("Successfully log in to " + address);
				ServerPrefUtil.addServerAddr(address);
				if (autoLogin) {
					ServerPrefUtil.addAutoLoginServer(address, idText.getText(), server.getPassword(), socksAddress);
				} else {
					ServerPrefUtil.removeAutoLoginServer(address);
				}
				msg("Completed !!!");
				msg("");
				if (callback != null) {
					callback.loginSuccess(address, server.getId());
				}
				ThreadUtil.sleep(100L);
				return true;
			} else {
				if (!existServer) {
					ServerManager.getInstance().removeServer(server.getId());
				}
				errMsg(result.getErrorMessage());
				msg("");
				return false;
			}
		} catch (Exception e) {
			if (server != null && !existServer) {
				ServerManager.getInstance().removeServer(server.getId());
			}
			e.printStackTrace();
			MessageDialog.openError(shell, "Error", "error occurred:" + e.getMessage());
		}
		return false;
	}

	private void msg(String msg) {
		messageList.add(msg);
		messageList.select(messageList.getItemCount() - 1);
		messageList.showSelection();
	}

	private void errMsg(String msg) {
		msg(msg);
	}

	public interface ILoginDialog {
		void loginSuccess(String serverAddr, int serverId);
	}
}