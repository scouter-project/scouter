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
package scouter.client.configuration.views;

import java.util.ArrayList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.net.TcpProxy;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ChartUtil;
import scouter.client.util.ColoringWord;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.CustomLineStyleListener;
import scouter.client.util.UIUtil;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;

public class ConfigureServerView extends ViewPart {
	public final static String ID = ConfigureServerView.class.getName();
	
	private ArrayList<ColoringWord> defaultHighlightings;
	
	private StyledText text;
	private String serverConfig;
	private int serverId;
	
	Composite listComp;
	Table table;
	
	private Clipboard clipboard = new Clipboard(null);
	
	CustomLineStyleListener listener;
	
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		SashForm sashForm = new SashForm(parent, SWT.HORIZONTAL);
		sashForm.SASH_WIDTH = 1;
		initialStyledText(sashForm);
		listComp = new Composite(sashForm, SWT.NONE);
		listComp.setLayout(ChartUtil.gridlayout(1));
		table = new Table(listComp,SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		UIUtil.create(table, SWT.LEFT, "Key", 3, 0, false, 200);
		UIUtil.create(table, SWT.CENTER, "Value", 3, 1, false, 100);
		UIUtil.create(table, SWT.CENTER, "Def.", 3, 2, false, 100);
		
		table.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if(e.stateMask == SWT.CTRL){
					if (e.keyCode == 'c' || e.keyCode == 'C') {
						TableItem[] items = table.getSelection();
						if (items == null || items.length < 1) {
							return;
						}
						StringBuffer sb = new StringBuffer();
						for (TableItem item : items) {
							sb.append(item.getText(0));
							sb.append("=");
							sb.append(item.getText(1));
							sb.append("\n");
						}
						clipboard.setContents(new Object[] {sb.toString()}, new Transfer[] {TextTransfer.getInstance()});
					}
				}
			}
		});
		
		sashForm.setWeights(new int[] {1, 1});
		sashForm.setMaximizedControl(null);
		initialToolBar();
	}

	public void setInput(int serverId){
		this.serverId = serverId;
		Server server = ServerManager.getInstance().getServer(serverId);
		if (server != null) {
			setPartName("Config Server[" + server.getName() + "]");
			loadConfig();
			loadConfigList();
		}
	}
	
	private void initialStyledText(Composite parent) {
		text = new StyledText(parent, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		listener = new CustomLineStyleListener(true, defaultHighlightings, false);
		text.addLineStyleListener(listener);
		text.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {
			}
			public void keyPressed(KeyEvent e) {
				if(e.stateMask == SWT.CTRL){
					if(e.keyCode == 's'){
						saveConfigurations();
					}else if(e.keyCode == 'a'){
						text.selectAll();
					}
				}
			}
		});
	}

	private void saveConfigurations(){
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			MapPack param = new MapPack();
			param.put("setConfig", text.getText());
			MapPack out = (MapPack) tcp.getSingle(RequestCmd.SET_CONFIGURE_SERVER, param);
			
			if (out != null) {
				String config = out.getText("result");
				if("true".equalsIgnoreCase(config)) {
					MessageDialog.open(MessageDialog.INFORMATION
							, PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()
							, "Success"
							, "Configuration saving is done."
							, SWT.NONE);
					loadConfigList();
				} else {
					MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()
							,"Error"
							, "Configuration saving is failed.");
				}
			}
		} catch(Exception e){
			ConsoleProxy.errorSafe(e.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
	}
	
	private void initialToolBar() {
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("Save", ImageUtil.getImageDescriptor(Images.save)) {
			public void run() {
				saveConfigurations();
			}
		});
	}

	private void loadConfig() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				MapPack mpack = null;
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				try {
					mpack = (MapPack) tcp.getSingle(RequestCmd.GET_CONFIGURE_SERVER, null);
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				if (mpack != null) {
					ListValue configKey = mpack.getList("configKey");
					
					defaultHighlightings = new ArrayList<ColoringWord>();
					for(int inx = 0 ; configKey != null && inx < configKey.size(); inx++){
						defaultHighlightings.add(new ColoringWord(configKey.getString(inx), SWT.COLOR_BLUE, true));
					}
					defaultHighlightings.add(new ColoringWord(";", SWT.COLOR_RED, true));
					
					serverConfig = mpack.getText("serverConfig");
				}
				ExUtil.exec(text, new Runnable() {
					public void run() {
						listener.setKeywordArray(defaultHighlightings);
						text.setText(serverConfig);
					}
				});
			}
		});
	}
	
	private void loadConfigList() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				MapPack pack = null;
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				try {
					pack = (MapPack) tcp.getSingle(RequestCmd.LIST_CONFIGURE_SERVER, null);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				if (pack != null) {
					final ListValue keyList = pack.getList("key");
					final ListValue valueList = pack.getList("value");
					final ListValue defaultList = pack.getList("default");
					ExUtil.exec(listComp, new Runnable() {
						public void run() {
							table.removeAll();
							for (int i = 0; i < keyList.size(); i++) {
								TableItem item = new TableItem(table, SWT.NONE);
								item.setText(new String[] {CastUtil.cString(keyList.get(i)), 
										CastUtil.cString(valueList.get(i)),
										CastUtil.cString(defaultList.get(i)) });
							}
							listComp.layout();
						}
					});
				}
			}
		});
	}
	
	public void setFocus() {
		IStatusLineManager slManager= getViewSite().getActionBars().getStatusLineManager();
		slManager.setMessage("CTRL + S : save configurations, CTRL + A : select all text");
	}

}
