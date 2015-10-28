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
package scouter.client.context.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;

import scouter.client.net.TcpProxy;
import scouter.client.util.UIUtil;
import scouter.lang.pack.MapPack;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;

public class OpenCxtmenuDbChangeIntervalAction extends Action {
	public final static String ID = OpenCxtmenuDbChangeIntervalAction.class.getName();

	private final IWorkbenchWindow win;
	private int objHash;
	private int serverId;

	public OpenCxtmenuDbChangeIntervalAction(IWorkbenchWindow win, String label,
			int objHash, int serverId) {
		this.win = win;
		this.objHash = objHash;
		this.serverId = serverId;
		setText(label);
	}

	public void run() {
		if (win != null) {
			TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
			MapPack m = null;
			try {
				MapPack param = new MapPack();
				param.put("objHash", objHash);
				m = (MapPack) tcp.getSingle(RequestCmd.GET_QUERY_INTERVAL, param);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				TcpProxy.putTcpProxy(tcp);
			}
			if (m == null) {
				new ChangeIntervalDailog(2).show();
			} else {
				long current = m.getLong("interval");
				new ChangeIntervalDailog(current).show();
			}
		}
	}
	
	class ChangeIntervalDailog {
		
		final long[] intervals = {DateUtil.MILLIS_PER_SECOND * 5
				,DateUtil.MILLIS_PER_SECOND * 10
				,DateUtil.MILLIS_PER_SECOND * 30
				,DateUtil.MILLIS_PER_SECOND * 60};
		long currentInterval;
		
		ChangeIntervalDailog(long currentInterval) {
			this.currentInterval = currentInterval;
		}
		
		public void show() {
			final Shell dialog = new Shell(win.getShell().getDisplay(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			UIUtil.setDialogDefaultFunctions(dialog);
			dialog.setText("Set Interval");
			dialog.setLayout(new GridLayout(1, true));
			Composite upperComp = new Composite(dialog, SWT.NONE);
			upperComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			upperComp.setLayout(new RowLayout());
			final Button enableBtn = new Button(upperComp, SWT.CHECK);
			enableBtn.setText("Enable data gathering");
			final Group radioGroup = new Group(dialog, SWT.NONE);
			radioGroup.setText("Interval");
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.widthHint = 400;
			radioGroup.setLayoutData(gd);
			radioGroup.setLayout(new GridLayout(intervals.length, true));
			for (int i = 0; i < intervals.length; i++) {
				Button button = new Button(radioGroup, SWT.RADIO);
				button.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				button.setText((intervals[i] / DateUtil.MILLIS_PER_SECOND) + " sec");
				button.setData(intervals[i]);
				if (currentInterval == intervals[i]) {
					button.setSelection(true);
					enableBtn.setSelection(true);
				}
			}
			enableBtn.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (enableBtn.getSelection()) {
						for (Control c : radioGroup.getChildren()) {
							c.setEnabled(true);
						}
					} else {
						for (Control c : radioGroup.getChildren()) {
							c.setEnabled(false);
						}
					}
				}
			});
			enableBtn.notifyListeners(SWT.Selection, new Event());
			Button okBtn = new Button(dialog, SWT.PUSH);
			gd = new GridData(SWT.RIGHT, SWT.FILL, true, false);
			gd.widthHint = 80;
			okBtn.setLayoutData(gd);
			okBtn.setText("&OK");
			okBtn.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
					try {
						MapPack param = new MapPack();
						param.put("objHash", objHash);
						if (enableBtn.getSelection()) {
							for (Control c : radioGroup.getChildren()) {
								if (c instanceof Button) {
									if (((Button) c).getSelection()) {
										param.put("interval", CastUtil.clong(c.getData()));
										break;
									}
								}
							}
						} else {
							param.put("interval", 0);
						}
						tcp.getSingle(RequestCmd.SET_QUERY_INTERVAL, param);
					} catch (Exception ee) {
						ee.printStackTrace();
					} finally {
						TcpProxy.putTcpProxy(tcp);
					}
					dialog.close();
				}
				public void widgetDefaultSelected(SelectionEvent e) {}
			});
			dialog.pack();
			dialog.open();
		}
	}
}
