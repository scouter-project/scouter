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
package scouter.client.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import scouter.client.model.TextProxy;
import scouter.client.net.TcpProxy;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.TimeUtil;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.StringUtil;

public class ServerStatusView extends ViewPart {

	public final static String ID = ServerStatusView.class.getName();
	
	private int objHash;
	private int serverId;
	Browser browser;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secondId = site.getSecondaryId();
		if (secondId != null) {
			String[] tokens = StringUtil.tokenizer(secondId, "&");
			this.objHash = CastUtil.cint(tokens[0]);
		}
	}
	
	Shell shell;
	boolean loaded = false;

	public void createPartControl(final Composite parent) {
		shell = parent.getShell();

		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout gLayout = new GridLayout(1, true);
		gLayout.horizontalSpacing = 0;
		gLayout.marginHeight = 0;
		gLayout.marginWidth = 0;
		composite.setLayout(gLayout);
		
		Composite textComposite = new Composite(composite, SWT.NONE);
		textComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		textComposite.setLayout(new FillLayout());
		
		browser = new Browser(textComposite, SWT.BORDER);
		
	}

	public void setInput(int serverId){
		this.serverId = serverId;
		this.setPartName("Server Status[" + TextProxy.object.getLoadText(DateUtil.yyyymmdd(TimeUtil.getCurrentTime(serverId)), objHash, serverId) + "]");
		load();
	}
	
	public void load() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				MapPack mpack = null;
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				try {
					MapPack param = new MapPack();
					param.put("objHash", objHash);
					mpack = (MapPack) tcp.getSingle(RequestCmd.APACHE_SERVER_STATUS, param);
					
				} catch(Exception e){
					ConsoleProxy.errorSafe(e.toString());
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				if (mpack != null) {
					String error = mpack.getText("error");
					if (error != null) {
						ConsoleProxy.errorSafe(error);
					}
					final ListValue lv = mpack.getList("serverStatus");
					
					ExUtil.exec(browser, new Runnable() {
						public void run() {
							StringBuilder sb = new StringBuilder();
							for (int i = 0; i < lv.size(); i++) {
								sb.append(lv.getString(i));
							}
							browser.setText(sb.toString());
						}
					});
				}
			}
		});
	}

	public void setFocus() {

	}

}
