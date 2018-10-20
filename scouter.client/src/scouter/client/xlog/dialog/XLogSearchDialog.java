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
package scouter.client.xlog.dialog;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import scouter.client.Images;
import scouter.client.model.XLogData;
import scouter.client.net.TcpProxy;
import scouter.client.popup.CalendarDialog;
import scouter.client.server.ServerManager;
import scouter.client.util.ColorUtil;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageCombo;
import scouter.client.util.TimeUtil;
import scouter.client.util.UIUtil;
import scouter.client.xlog.XLogUtil;
import scouter.client.xlog.views.XLogSelectionView;
import scouter.lang.counters.CounterEngine;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.ObjectPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.Hexa32;
import scouter.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class XLogSearchDialog implements CalendarDialog.ILoadCalendarDialog{
	 
	Shell dialog;
	IWorkbenchWindow win;
	int serverId;
	String objType;
	String requestCmd;
	Group normalSearchGrp;
	Group quickSearchGrp;
	
	Text ragneText;
	ImageCombo objectCombo;
	Text ipText;
	Text loginText;
	Text descText;
	Text text1Text;
	Text text2Text;
	Text text3Text;
	Text text4Text;
	Text text5Text;
	Text serviceText;
	
	Text dateText;
	Text txidText;
	
	ImageCombo  txidCombo;

	public XLogSearchDialog(IWorkbenchWindow win, int serverId, String objType) {
		this.win = win;
		this.serverId = serverId;
		this.objType = objType;
	}
	
	public void show() {
		CounterEngine counterEngine = ServerManager.getInstance().getServer(serverId).getCounterEngine();
		dialog = new Shell(win.getShell(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		UIUtil.setDialogDefaultFunctions(dialog);
		dialog.setText("Search XLog - " + counterEngine.getDisplayNameObjectType(objType));
		dialog.setLayout(new GridLayout(1, true));
		GridData gr;
		
		Button normalRadio = new Button(dialog, SWT.RADIO);
		normalRadio.setText("Normal Search(Max:500)");
		gr = new GridData(SWT.LEFT, SWT.FILL, false, false);
		gr.widthHint = 300;
		normalRadio.setLayoutData(gr);
		normalRadio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				requestCmd = RequestCmd.SEARCH_XLOG_LIST;
				normalSearchGrp.setEnabled(true);
				ragneText.setEnabled(true);
				objectCombo.setEnabled(true);
				ipText.setEnabled(true);
				loginText.setEnabled(true);
				descText.setEnabled(true);
				text1Text.setEnabled(true);
				text2Text.setEnabled(true);
				text3Text.setEnabled(true);
				text4Text.setEnabled(true);
				text5Text.setEnabled(true);
				serviceText.setEnabled(true);
				
				quickSearchGrp.setEnabled(false);
				dateText.setEnabled(false);
				txidCombo.setEnabled(false);
				txidText.setEnabled(false);
			}
		});
		
		normalSearchGrp = new Group(dialog, SWT.NONE);
		gr = new GridData(SWT.FILL, SWT.FILL, true, false);
		normalSearchGrp.setLayoutData(gr);
		normalSearchGrp.setLayout(new GridLayout(3, false));
		normalSearchGrp.setText("Search Condition");
		
		Label label = new Label(normalSearchGrp, SWT.NONE);
		gr = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gr.widthHint = 70;
		label.setLayoutData(gr);
		label.setText("Time Range");
		
		ragneText = new Text(normalSearchGrp, SWT.BORDER | SWT.READ_ONLY);
		ragneText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		ragneText.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		
		Button button = new Button(normalSearchGrp, SWT.PUSH);
		button.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		button.setImage(Images.CTXMENU_RDC);
		button.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					CalendarDialog dialog = new CalendarDialog(win.getShell().getDisplay(), XLogSearchDialog.this);
					dialog.showWithEndTime(stime, etime);
					break;
				}
			}
		});
		
		label = new Label(normalSearchGrp, SWT.NONE);
		gr = new GridData(SWT.FILL, SWT.FILL, false, false);
		gr.widthHint = 70;
		label.setLayoutData(gr);
		label.setText("Object");
		objectCombo = new ImageCombo(normalSearchGrp, SWT.READ_ONLY | SWT.BORDER);
		gr = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		objectCombo.setLayoutData(gr);
		objectCombo.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		objectCombo.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {}
			public void focusGained(FocusEvent e) {
				objectCombo.removeAll();
				if (stime > 0) {
					ArrayList<ObjectPack> list = loadObjectList(DateUtil.yyyymmdd(stime));
					for (int i = 0; i < list.size(); i++) {
						ObjectPack pack = list.get(i);
						objectCombo.add(pack.objName, null);
						objectCombo.setData(pack.objName, pack.objHash);
					}
				}
			}
		});
		
		label = new Label(normalSearchGrp, SWT.NONE);
		gr = new GridData(SWT.FILL, SWT.FILL, false, false);
		gr.widthHint = 70;
		label.setLayoutData(gr);
		label.setText("Service");
		
		serviceText = new Text(normalSearchGrp, SWT.BORDER);
		gr = new GridData(SWT.FILL, SWT.FILL, true, false, 2 , 1);
		serviceText.setLayoutData(gr);
		
		label = new Label(normalSearchGrp, SWT.NONE);
		gr = new GridData(SWT.FILL, SWT.FILL, false, false);
		gr.widthHint = 70;
		label.setLayoutData(gr);
		label.setText("IP");
		
		ipText = new Text(normalSearchGrp, SWT.BORDER);
		gr = new GridData(SWT.FILL, SWT.FILL, true, false, 2 ,1);
		ipText.setLayoutData(gr);

		label = new Label(normalSearchGrp, SWT.NONE);
		gr = new GridData(SWT.FILL, SWT.FILL, false, false);
		gr.widthHint = 70;
		label.setLayoutData(gr);
		label.setText("LOGIN");

		loginText = new Text(normalSearchGrp, SWT.BORDER);
		gr = new GridData(SWT.FILL, SWT.FILL, true, false, 2 ,1);
		loginText.setLayoutData(gr);

		label = new Label(normalSearchGrp, SWT.NONE);
		gr = new GridData(SWT.FILL, SWT.FILL, false, false);
		gr.widthHint = 70;
		label.setLayoutData(gr);
		label.setText("DESC");

		descText = new Text(normalSearchGrp, SWT.BORDER);
		gr = new GridData(SWT.FILL, SWT.FILL, true, false, 2 ,1);
		descText.setLayoutData(gr);

		label = new Label(normalSearchGrp, SWT.NONE);
		gr = new GridData(SWT.FILL, SWT.FILL, false, false);
		gr.widthHint = 70;
		label.setLayoutData(gr);
		label.setText("TEXT1");

		text1Text = new Text(normalSearchGrp, SWT.BORDER);
		gr = new GridData(SWT.FILL, SWT.FILL, true, false, 2 ,1);
		text1Text.setLayoutData(gr);

		label = new Label(normalSearchGrp, SWT.NONE);
		gr = new GridData(SWT.FILL, SWT.FILL, false, false);
		gr.widthHint = 70;
		label.setLayoutData(gr);
		label.setText("TEXT2");

		text2Text = new Text(normalSearchGrp, SWT.BORDER);
		gr = new GridData(SWT.FILL, SWT.FILL, true, false, 2 ,1);
		text2Text.setLayoutData(gr);

		label = new Label(normalSearchGrp, SWT.NONE);
		gr = new GridData(SWT.FILL, SWT.FILL, false, false);
		gr.widthHint = 70;
		label.setLayoutData(gr);
		label.setText("TEXT3");

		text3Text = new Text(normalSearchGrp, SWT.BORDER);
		gr = new GridData(SWT.FILL, SWT.FILL, true, false, 2 ,1);
		text3Text.setLayoutData(gr);

		label = new Label(normalSearchGrp, SWT.NONE);
		gr = new GridData(SWT.FILL, SWT.FILL, false, false);
		gr.widthHint = 70;
		label.setLayoutData(gr);
		label.setText("TEXT4");

		text4Text = new Text(normalSearchGrp, SWT.BORDER);
		gr = new GridData(SWT.FILL, SWT.FILL, true, false, 2 ,1);
		text4Text.setLayoutData(gr);

		label = new Label(normalSearchGrp, SWT.NONE);
		gr = new GridData(SWT.FILL, SWT.FILL, false, false);
		gr.widthHint = 70;
		label.setLayoutData(gr);
		label.setText("TEXT5");

		text5Text = new Text(normalSearchGrp, SWT.BORDER);
		gr = new GridData(SWT.FILL, SWT.FILL, true, false, 2 ,1);
		text5Text.setLayoutData(gr);
		
		Button quickRadio = new Button(dialog, SWT.RADIO);
		quickRadio.setText("Quick Search");
		gr = new GridData(SWT.LEFT, SWT.FILL, false, false);
		quickRadio.setLayoutData(gr);
		quickRadio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				requestCmd = RequestCmd.QUICKSEARCH_XLOG_LIST;
				
				quickSearchGrp.setEnabled(true);
				dateText.setEnabled(true);
				txidCombo.setEnabled(true);
				txidText.setEnabled(true);
				
				normalSearchGrp.setEnabled(false);
				ragneText.setEnabled(false);
				objectCombo.setEnabled(false);
				ipText.setEnabled(false);
				loginText.setEnabled(false);
				descText.setEnabled(false);
				text1Text.setEnabled(false);
				text2Text.setEnabled(false);
				text3Text.setEnabled(false);
				text4Text.setEnabled(false);
				text5Text.setEnabled(false);
				serviceText.setEnabled(false);
			}
		});
		
		
		quickSearchGrp = new Group(dialog, SWT.NONE);
		gr = new GridData(SWT.FILL, SWT.FILL, true, false);
		quickSearchGrp.setLayoutData(gr);
		quickSearchGrp.setLayout(new GridLayout(3, false));
		quickSearchGrp.setText("QuickSearch Condition");
		
		dateText =  new Text(quickSearchGrp, SWT.BORDER | SWT.READ_ONLY);
		gr = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gr.widthHint = 70;
		dateText.setLayoutData(gr);
		dateText.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		dateText.addMouseListener(new MouseAdapter() {
			public void mouseUp(MouseEvent e) {
				CalendarDialog dialog = new CalendarDialog(win.getShell().getDisplay(), XLogSearchDialog.this);
				dialog.show(-1, -1, DateUtil.getTime(date, "yyyyMMdd"));
			}
		});
		
		txidCombo = new ImageCombo(quickSearchGrp, SWT.READ_ONLY | SWT.BORDER);
		gr = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gr.widthHint = 30;
		txidCombo.setLayoutData(gr);
		txidCombo.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		txidCombo.add("txid", null);
		txidCombo.add("gxid", null);
		txidCombo.select(0);
		
		txidText = new Text(quickSearchGrp, SWT.BORDER);
		txidText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		
		Button okBtn = new Button(dialog, SWT.PUSH);
		gr = new GridData(SWT.RIGHT, SWT.FILL, false, false);
		gr.widthHint = 100;
		okBtn.setLayoutData(gr);
		okBtn.setText("&Search");
		okBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (requestCmd.equals(RequestCmd.SEARCH_XLOG_LIST)) {
					searchXLog();
				} else if (requestCmd.equals(RequestCmd.QUICKSEARCH_XLOG_LIST)) {
					quickSearchXLog();
				}
			}
		});
		
		init();
		
		normalRadio.setSelection(true);
		normalRadio.notifyListeners(SWT.Selection, new Event());
		
		dialog.pack();
		dialog.open();
	}
	
	long stime;
	long etime;
	String date;
	
	private void init() {
		long now = TimeUtil.getCurrentTime(serverId);
		long stime = now - DateUtil.MILLIS_PER_FIVE_MINUTE;
		onPressedOk(stime, now);
		onPressedOk(DateUtil.yyyymmdd(now));
	}

	public void onPressedOk(long startTime, long endTime) {
		this.stime = startTime;
		this.etime = endTime;
		String yyyymmdd = DateUtil.yyyymmdd(startTime);
		ragneText.setText("(" + yyyymmdd.substring(0, 4) + "-" + yyyymmdd.substring(4, 6) + "-" + yyyymmdd.substring(6, 8) + ") "
		+ DateUtil.format(startTime, "HH:mm") + "~" + DateUtil.format(endTime, "HH:mm"));
	}
	
	public void onPressedOk(String date) {
		this.date = date;
		dateText.setText(date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8));
	}
	public void onPressedCancel() {}
	
	private ArrayList<ObjectPack> loadObjectList(String date) {
		ArrayList<ObjectPack> objList = new ArrayList<ObjectPack>();
		TcpProxy proxy = TcpProxy.getTcpProxy(serverId);
		try {
			MapPack param = new MapPack();
			param.put("date", date);
			MapPack out = (MapPack) proxy.getSingle(RequestCmd.OBJECT_LIST_LOAD_DATE, param);
			ListValue objTypeLv = out.getList("objType");
			ListValue objHashLv = out.getList("objHash");
			ListValue objNameLv = out.getList("objName");
			
			if (objHashLv == null || objHashLv.size() < 0) {
				return objList;
			}
			for (int i = 0; i < objHashLv.size(); i++) {
				ObjectPack pack = new ObjectPack();
				pack.objType = objTypeLv.getString(i);
				if (pack.objType.equals(objType) == false) {
					continue;
				}
				pack.objHash = (int) objHashLv.getLong(i);
				pack.objName = objNameLv.getString(i);
				objList.add(pack);
			}
			Collections.sort(objList, new Comparator<Pack>() {
				public int compare(Pack o1, Pack o2) {
					ObjectPack m1 = (ObjectPack) o1;
					ObjectPack m2 = (ObjectPack) o2;
					int c = m1.objType.compareTo(m2.objType);
					if (c != 0)
						return c;
					if (m1.objName != null)
						return m1.objName.compareTo(m2.objName);
					else
						return m1.objHash - m2.objHash;
				}
			});
		} finally {
			TcpProxy.putTcpProxy(proxy);
		}
		return objList;
	}
	
	private void searchXLog() {
		MapPack param = new MapPack();
		param.put("stime", stime);
		param.put("etime", etime);
		String objName = objectCombo.getText();
		if (StringUtil.isNotEmpty(objName)) {
			param.put("objHash", CastUtil.clong(objectCombo.getData(objName)));
		}
		String serviceName = serviceText.getText();
		if (StringUtil.isNotEmpty(serviceName)) {
			param.put("service", serviceName);
		}
		String ip = ipText.getText();
		if (StringUtil.isNotEmpty(ip)) {
			param.put("ip", ip);
		}
		if (StringUtil.isNotEmpty(loginText.getText())) {
			param.put("login", loginText.getText());
		}
		if (StringUtil.isNotEmpty(descText.getText())) {
			param.put("desc", descText.getText());
		}
		if (StringUtil.isNotEmpty(text1Text.getText())) {
			param.put("text1", text1Text.getText());
		}
		if (StringUtil.isNotEmpty(text2Text.getText())) {
			param.put("text2", text2Text.getText());
		}
		if (StringUtil.isNotEmpty(text3Text.getText())) {
			param.put("text3", text3Text.getText());
		}
		if (StringUtil.isNotEmpty(text4Text.getText())) {
			param.put("text4", text4Text.getText());
		}
		if (StringUtil.isNotEmpty(text5Text.getText())) {
			param.put("text5", text5Text.getText());
		}
		new SearchXLogJob(param).schedule();
		dialog.close();
	}
	
	private void quickSearchXLog() {
		MapPack param = new MapPack();
		param.put("date", date);
		String id = txidText.getText();
		try {
			long txid = 0L;
			try {
				txid = Long.parseLong(id.trim());
			} catch (NumberFormatException e) {
				txid = Hexa32.toLong32(id.trim());
			}
			param.put(txidCombo.getText(), txid);
			new SearchXLogJob(param).schedule();
			dialog.close();
		} catch (Exception e) {
			MessageDialog.openError(win.getShell(), "Error", e.toString() + " : " + id);
		}
	}
	
	class SearchXLogJob extends Job {
		
		MapPack param;

		public SearchXLogJob(MapPack param) {
			super("Search XLog");
			this.param = param;
		}

		protected IStatus run(IProgressMonitor monitor) {
			TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
			List<Pack> list = null;
			try {
				monitor.beginTask("Search XLog....... ", IProgressMonitor.UNKNOWN);
				list = tcp.process(requestCmd, param);
			} catch (Exception e) {
				return Status.CANCEL_STATUS;
			} finally {
				TcpProxy.putTcpProxy(tcp);
			}
			final ArrayList<XLogData> datas = new ArrayList<XLogData>();
			for (int i = 0, size = (list == null ? 0 : list.size()); i < size; i++) {
				Pack pack = list.get(i);
				datas.add(new XLogData(XLogUtil.toXLogPack(pack), serverId));
			}
			ExUtil.exec(win.getShell().getDisplay(), new Runnable() {
				public void run() {
					try {
						IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
						XLogSelectionView view = (XLogSelectionView) win.getActivePage().showView(XLogSelectionView.ID,
								"" + System.identityHashCode(SearchXLogJob.this), IWorkbenchPage.VIEW_ACTIVATE);
						view.setInput(datas, objType, DateUtil.yyyymmdd(stime));
					} catch (Exception d) {
					}
				}
			});
			return Status.OK_STATUS;
		}
	}
}
