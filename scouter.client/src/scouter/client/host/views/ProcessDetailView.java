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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import scouter.client.net.TcpProxy;
import scouter.client.util.ColoringWord;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.CustomLineStyleListener;
import scouter.client.util.ExUtil;
import scouter.client.util.ScouterUtil;
import scouter.lang.pack.MapPack;
import scouter.net.RequestCmd;

public class ProcessDetailView extends ViewPart {

	public final static String ID = ProcessDetailView.class.getName();
	private StyledText text;
	private int objHash;
	private int serverId;
	private int pid;
	private static ArrayList<ColoringWord> reserved = new ArrayList<ColoringWord>();
	
	static {
		reserved.add(new ColoringWord("pid ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("name ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("exe ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("parent ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("cmdline ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("started ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("user ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("uids ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("gids ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("terminal ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("cwd ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("memory ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("cpu ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("status ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("niceness ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("num threads ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("I/O ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("children ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("open files ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("running threads ", SWT.COLOR_DARK_GREEN, true));
		reserved.add(new ColoringWord("open connections ", SWT.COLOR_DARK_GREEN, true));
	}
	
	public void setInput(int serverId, int objHash, int pid){
		this.serverId = serverId;
		this.objHash = objHash;
		this.pid = pid;
		this.setPartName("Process Detail[" + pid + "]");
		load();
	}

	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		text = new StyledText(parent, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		text.setFont(new Font(null, "Courier New", 10, SWT.NORMAL));
		text.addLineStyleListener(new CustomLineStyleListener(false, reserved, true));
	}

	public void load() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				MapPack mpack = null;
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				try {
					MapPack param = new MapPack();
					param.put("objHash", objHash);
					param.put("pid", pid);
					mpack = (MapPack) tcp.getSingle(RequestCmd.HOST_PROCESS_DETAIL, param);
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
