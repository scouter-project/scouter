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
package scouter.client.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import scouter.client.model.TextProxy;
import scouter.client.net.TcpProxy;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.ScouterUtil;
import scouter.client.util.TimeUtil;
import scouter.lang.pack.MapPack;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.StringUtil;

public class ObjectFileSocketView extends ViewPart {

	public final static String ID = ObjectFileSocketView.class.getName();
	private StyledText text;
	private int objHash;
	private int serverId;
	String secondId;
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		secondId = site.getSecondaryId();
		
	}
	
	public void setInput(int serverId){
		this.serverId = serverId;
		if (secondId != null) {
			String[] tokens = StringUtil.tokenizer(secondId, "&");
			this.objHash = CastUtil.cint(tokens[0]);
			this.setPartName("List OpenFile[" + TextProxy.object.getLoadText(DateUtil.yyyymmdd(TimeUtil.getCurrentTime(serverId)), objHash, serverId) + "]");
		}
		load();
	}

	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		text = new StyledText(parent, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		text.setFont(new Font(null, "Courier New", 10, SWT.NORMAL));
	}

	public void load() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				MapPack mpack = null;
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				try {
					MapPack param = new MapPack();
					param.put("objHash", objHash);
					mpack = (MapPack) tcp.getSingle(RequestCmd.OBJECT_FILE_SOCKET, param);
					
					
				} catch(Exception e){
					ConsoleProxy.errorSafe(e.toString());
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				if (mpack != null) {
					final int status = mpack.getInt("status");
					final String error = mpack.getText("error");
					final String exception = mpack.getText("exception");
					final String data = mpack.getText("data");
					ExUtil.exec(text, new Runnable() {
						public void run() {
							if (status != 0) {
								StringBuilder sb = new StringBuilder();
								sb.append("Error Code : " + status);
								if (error != null) {
									sb.append("\nError : " + error);
								}
								if (exception != null) {
									sb.append("\n" + exception);
									if (exception.toLowerCase().contains("no such file")) {
										sb.append("\n");
										sb.append("\n# yum install lsof");
										sb.append("\n# apt–get install lsof (in Debian or Ubuntu)");
									}
								}
								if (data != null) {
									sb.append("\n" + data);
								}
								String msg = sb.toString();
								text.setText(msg);
								StyleRange styleRange = new StyleRange(0, msg.length(), Display.getCurrent().getSystemColor(SWT.COLOR_RED), null);
								text.setStyleRange(styleRange);
							} else {
								text.setText(data);
								int firstLine = data.indexOf('\n');
								StyleRange styleRange = new StyleRange(0, firstLine, Display.getCurrent().getSystemColor(SWT.COLOR_BLUE), null);
								text.setStyleRange(styleRange);
							}
						}
					});
				}
			}
		});
	}

	public void setFocus() {
	}

}
