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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import scouter.Version;
import scouter.client.net.LoginMgr;
import scouter.client.net.LoginResult;
import scouter.client.preferences.ServerPrefUtil;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.UIUtil;
import scouter.net.NetConstants;
import scouter.util.StringUtil;

public class LoginDialog2 extends Dialog {
	public static final String ID = LoginDialog2.class.getName();

	private final Shell shell;
	private final ILoginDialog callback;
	private final int openType;
	public static final int TYPE_STARTUP = 991;
	public static final int TYPE_ADD_SERVER = 992;
	public static final int TYPE_OPEN_SERVER = 993;
	public static final int TYPE_EDIT_SERVER = 994;

	ArrayList<String> storeAddr = new ArrayList<String>();
	FormData data;
	Combo addrCombo, socksAddrCombo;
	Text id, pass;
	Label idLabel, passLabel;

	List list;

	Button autoLoginCheck, secureCheck, sock5Check;
	boolean autoLogin;
	boolean secureLogin = true;
	boolean sock5Login = false;

	String address = null;
	String socksAddress = null;

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

		createPasswordInput(parentGroup);

		autoLoginCheck = new Button(parentGroup, SWT.CHECK);
		autoLoginCheck.setText("Auto Login");
		autoLoginCheck.setLayoutData(UIUtil.formData(null, -1, passLabel, 10, 100, -5, null, -1));
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

		// to hash password before transfer, default true
		secureCheck = new Button(parentGroup, SWT.CHECK);
		secureCheck.setText("Secure Login");
		secureCheck.setLayoutData(UIUtil.formData(null, -1, passLabel, 10, autoLoginCheck, -5, null, -1));
		secureCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (secureCheck.getSelection()) {
					secureLogin = true;
				} else {
					secureLogin = false;
				}
			}
		});
		secureCheck.setSelection(true);
		
		// console group
		final Group socksGroup = new Group(comp, SWT.NONE);
		socksGroup.setText("SOCKS5");
		socksGroup.setLayout(UIUtil.formLayout(5, 5));
		socksGroup.setLayoutData(UIUtil.formData(null, -1, parentGroup, 0, null, -1, null, -1));

		// to use SOCKS5
		sock5Check = new Button(socksGroup, SWT.CHECK|SWT.LEFT);
		sock5Check.setText("SOCKS5");
		sock5Check.setLayoutData(UIUtil.formData(0, 0, 0, 5, 100, -5, null, -1));
		sock5Check.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (sock5Check.getSelection()) {
					sock5Login = true;
				} else {
					sock5Login = false;
				}
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
		consoleGroup.setLayoutData(UIUtil.formData(null, -1, socksGroup, 0, null, -1, null, -1));
		
		// connection status console
		list = new List(consoleGroup, SWT.NONE);
		list.setLayoutData(UIUtil.formData(0, 5, 0, 5, 100, -5, null, -1, 250, 60));
		//list.setLayoutData(UIUtil.formData(0, 5, socks5AddrLabel, 10, 100, -5, null, -1, -1, 60));
		list.add("Type your authentication info...");
		list.select(list.getItemCount() - 1);
		list.showSelection();

		if (StringUtil.isNotEmpty(this.address)) {
			addrCombo.setText(address);
			Server server = ServerManager.getInstance().getServer(address);
			if (server != null && StringUtil.isNotEmpty(server.getUserId())) {
				id.setText(server.getUserId());
				secureCheck.setSelection(server.isSecureMode());
			}
			autoLoginCheck.setSelection(ServerPrefUtil.isAutoLoginAddress(address));
		} else if (openType == TYPE_STARTUP) {
			addrCombo.setText("127.0.0.1:" + NetConstants.SERVER_TCP_PORT);
			id.setText("admin");
		}
		
		if (StringUtil.isNotEmpty(this.socksAddress)) {
			socksAddrCombo.setText(socksAddress);
			sock5Check.setSelection(true);
			Server server = ServerManager.getInstance().getServer(address);
			if (server != null && StringUtil.isNotEmpty(server.getUserId())) {
				id.setText(server.getUserId());
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
		pass = new Text(parentGroup, SWT.SINGLE | SWT.BORDER | SWT.PASSWORD);
		pass.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
			}

			public void focusGained(FocusEvent e) {
				pass.selectAll();
			}
		});
		pass.setLayoutData(UIUtil.formData(passLabel, 5, id, 7, 100, -5, null, -1));
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
			if (address.contains(":") == false) {
				address = address.concat(":" + NetConstants.SERVER_TCP_PORT);
			}
			String addr[] = address.split(":");
			ip = addr[0];
			port = addr[1];
			
			String socksIp = null;
			String socksPort = null;
			if (socksAddress.contains(":") == false) {
				errMsg("Check SOCKS Address");
			}
			String socksAddr[] = socksAddress.split(":");
			socksIp = socksAddr[0];
			socksPort = socksAddr[1];
			
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

			LoginResult result = LoginMgr.login(server.getId(), id.getText(), pass.getText());
			if (result.success) {
				msg("Successfully log in to " + address);
				ServerPrefUtil.addServerAddr(address);
				if (autoLogin) {
					ServerPrefUtil.addAutoLoginServer(address, id.getText(), server.getPassword(), socksAddress);
				} else {
					ServerPrefUtil.removeAutoLoginServer(address);
				}
				msg("Completed !!!");
				msg("");
				if (callback != null) {
					callback.loginSuccess(address, server.getId());
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
				errMsg(result.getErrorMessage());
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
		void loginSuccess(String serverAddr, int serverId);
	}

}
