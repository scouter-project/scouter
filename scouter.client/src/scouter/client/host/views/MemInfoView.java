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
package scouter.client.host.views;

import java.util.ArrayList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.model.TextProxy;
import scouter.client.net.TcpProxy;
import scouter.client.util.ColoringWord;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.CustomLineStyleListener;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.ScouterUtil;
import scouter.lang.pack.MapPack;
import scouter.net.RequestCmd;

public class MemInfoView extends ViewPart {

	public final static String ID = MemInfoView.class.getName();
	private StyledText text;
	private int objHash;
	private int serverId;
	private static ArrayList<ColoringWord> reserved = new ArrayList<ColoringWord>();
	
	static {
		reserved.add(new ColoringWord("Total ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("Available ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("Percent ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("Used ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("Free ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("Active ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("Inactive ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("Buffers ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("Cached ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("Sin ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("Sout ", SWT.COLOR_DARK_GREEN, true));
	}
	
	public void setInput(int serverId, int objHash){
		this.serverId = serverId;
		this.objHash = objHash;
		this.setPartName("Mem Info[" + TextProxy.object.getText(objHash) + "]");
		load();
	}

	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		text = new StyledText(parent, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		text.setFont(new Font(null, "Courier New", 10, SWT.NORMAL));
		text.addLineStyleListener(new CustomLineStyleListener(false, reserved, true));
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("Reload", ImageUtil.getImageDescriptor(Images.refresh)) {
			public void run() {
				load();
			}
		});
	}

	public void load() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				MapPack mpack = null;
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				try {
					MapPack param = new MapPack();
					param.put("objHash", objHash);
					mpack = (MapPack) tcp.getSingle(RequestCmd.HOST_MEMINFO, param);
				} catch(Exception e){
					ConsoleProxy.errorSafe(e.toString());
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				if (mpack != null) {
					final String result = mpack.getText("result");
					ExUtil.exec(text, new Runnable() {
						public void run() {
								text.setText(result);
						}
					});
				}
			}
		});
	}

	public void setFocus() {
	}

}
