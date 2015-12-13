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

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import scouter.client.Images;
import scouter.client.model.AgentDailyListProxy;
import scouter.client.model.TextProxy;
import scouter.client.popup.CalendarDialog.ILoadCalendarDialog;
import scouter.client.server.ServerManager;
import scouter.client.util.ColorUtil;
import scouter.client.util.UIUtil;
import scouter.lang.counters.CounterEngine;
import scouter.lang.pack.ObjectPack;
import scouter.lang.value.ListValue;
import scouter.util.CastUtil;
import scouter.util.DateUtil;

public class ObjectCounterDialog {
	
	
	public static final String[] HOURLY_TIMES = {"00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24"};
	
	Display display;
	int serverId;
	String objType;
	String from = DateUtil.yyyymmdd(), to = DateUtil.yyyymmdd();
	Text fromTxt, toTxt;
	Combo fromTime, toTime;
	Table counterTable, objTable;
	ICounterObjectCallback callback;
	
	AgentDailyListProxy agentProxy = new AgentDailyListProxy();
	
	public ObjectCounterDialog(Display display, int serverId, String objType) {
		this.display = display;
		this.serverId = serverId;
		this.objType = objType;
	}
	
	public void setCallback(ICounterObjectCallback callback) {
		this.callback = callback;
	}
	
	public void show() {
		final CounterEngine counterEngine = ServerManager.getInstance().getServer(serverId).getCounterEngine();
		final Shell dialog = new Shell(display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL | SWT.RESIZE);
		UIUtil.setDialogDefaultFunctions(dialog);
		dialog.setText("Objects & Counters");
		dialog.setLayout(new GridLayout(1, true));
		Composite mainComp = new Composite(dialog, SWT.NONE);
		GridData gr = new GridData(SWT.FILL, SWT.FILL, true, true);
		gr.widthHint = 700;
		gr.heightHint = 500;
		mainComp.setLayoutData(gr);
		mainComp.setLayout(new GridLayout(1, true));
		
		
		Composite upperComp = new Composite(mainComp, SWT.NONE);
		upperComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		upperComp.setLayout(new GridLayout(9, false));
		
		CLabel objTypeLbl = new CLabel(upperComp, SWT.NONE);
		objTypeLbl.setLayoutData(new GridData(SWT.TOP, SWT.FILL, true, false));
		objTypeLbl.setAlignment(SWT.LEFT);
		objTypeLbl.setFont(new Font(null, "Arial", 10, SWT.BOLD));
		objTypeLbl.setImage(Images.getObjectIcon(objType, true, serverId));
		objTypeLbl.setText(counterEngine.getDisplayNameObjectType(objType));
		
		fromTxt = new Text(upperComp, SWT.READ_ONLY | SWT.BORDER);
		gr = new GridData(SWT.FILL, SWT.FILL, true, false);
		gr.widthHint = 100;
		fromTxt.setLayoutData(gr);
		fromTxt.setBackground(ColorUtil.getInstance().getColor("white"));
		fromTxt.setText(from.substring(0, 4) + "-" + from.substring(4, 6) + "-" + from.substring(6, 8));
		
		Button fromCalBtn = new Button(upperComp, SWT.PUSH);
		fromCalBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		fromCalBtn.setImage(Images.CTXMENU_RDC);
		fromCalBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				new CalendarDialog(display, new ILoadCalendarDialog() {
					public void onPressedOk(String date) {
						from = date;
						fromTxt.setText(date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8));
					}
					public void onPressedOk(long startTime, long endTime) {}
					public void onPressedCancel() {}
				}).show(-1, -1, DateUtil.getTime(from, "yyyyMMdd"));
			}
		});
		
		fromTime = new Combo(upperComp, SWT.VERTICAL | SWT.BORDER | SWT.READ_ONLY);
		gr = new GridData(SWT.FILL, SWT.FILL, true, false);
		gr.widthHint = 50;
		fromTime.setLayoutData(gr);
		fromTime.setItems(HOURLY_TIMES);
		fromTime.select(0);
		
		Label label = new Label(upperComp, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		label.setAlignment(SWT.CENTER);
		label.setText("~");
		
		toTxt = new Text(upperComp, SWT.READ_ONLY | SWT.BORDER);
		gr = new GridData(SWT.FILL, SWT.FILL, true, false);
		gr.widthHint = 100;
		toTxt.setLayoutData(gr);
		toTxt.setBackground(ColorUtil.getInstance().getColor("white"));
		toTxt.setText(to.substring(0, 4) + "-" + to.substring(4, 6) + "-" + to.substring(6, 8));
		
		Button toCalBtn = new Button(upperComp, SWT.PUSH);
		toCalBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		toCalBtn.setImage(Images.CTXMENU_RDC);
		toCalBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				new CalendarDialog(display, new ILoadCalendarDialog() {
					public void onPressedOk(String date) {
						to = date;
						toTxt.setText(date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8));
					}
					public void onPressedOk(long startTime, long endTime) {}
					public void onPressedCancel() {}
				}).show(-1, -1, DateUtil.getTime(to, "yyyyMMdd"));
			}
		});
		
		toTime = new Combo(upperComp, SWT.VERTICAL | SWT.BORDER | SWT.READ_ONLY);
		gr = new GridData(SWT.FILL, SWT.FILL, true, false);
		gr.widthHint = 50;
		toTime.setLayoutData(gr);
		toTime.setItems(HOURLY_TIMES);
		toTime.select(HOURLY_TIMES.length - 1);
		
		Button getBtn = new Button(upperComp, SWT.PUSH);
		gr = new GridData(SWT.FILL, SWT.FILL, true, false);
		gr.widthHint = 120;
		getBtn.setLayoutData(gr);
		getBtn.setText("Get Object List");
		getBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				queryObjSet();
			}
		});
		
		Composite tableComp = new Composite(mainComp, SWT.NONE);
		tableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tableComp.setLayout(new GridLayout(2, true));
		
		final Button counterBtn = new Button(tableComp, SWT.CHECK);
		gr = new GridData(SWT.LEFT, SWT.FILL, true, false);
		gr.horizontalIndent = 8;
		counterBtn.setLayoutData(gr);
		counterBtn.setText(" Counter List");
		counterBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				TableItem[] items =counterTable.getItems();
				if (items != null) {
					for (TableItem item : items) {
						item.setChecked(counterBtn.getSelection());
					}
				}
			}
		});
		
		final Button objBtn = new Button(tableComp, SWT.CHECK);
		gr = new GridData(SWT.LEFT, SWT.FILL, true, false);
		gr.horizontalIndent = 8;
		objBtn.setLayoutData(gr);
		objBtn.setText(" Object List");
		objBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				TableItem[] items =objTable.getItems();
				if (items != null) {
					for (TableItem item : items) {
						item.setChecked(objBtn.getSelection());
					}
				}
			}
		});
		
		counterTable = new Table(tableComp, SWT.CHECK | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		counterTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		ArrayList<String> counters = counterEngine.getAllCounterList(objType);
		if (counters != null) {
			Collections.sort(counters);
			for (String counter : counters) {
				TableItem item = new TableItem(counterTable, SWT.NONE);
				item.setText(counterEngine.getCounterDisplayName(objType, counter));
				item.setData(counter);
			}
		}
		
		objTable = new Table(tableComp, SWT.CHECK | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		objTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Button okBtn = new Button(mainComp, SWT.PUSH);
		gr = new GridData(SWT.RIGHT, SWT.FILL, true, false);
		gr.widthHint = 100;
		okBtn.setLayoutData(gr);
		okBtn.setText("&Ok");
		okBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (callback != null) {
					TableItem[] items = objTable.getItems();
					List<ObjectPack> objList = new ArrayList<ObjectPack>();
					for (TableItem item : items) {
						if (item.getChecked()) {
							ObjectPack pack = new ObjectPack();
							pack.objHash = (Integer) item.getData();
							pack.objName = item.getText();
							pack.objType = objType;
							objList.add(pack);
						}
					}
					items = counterTable.getItems();
					List<String> counterList = new ArrayList<String>();
					for (TableItem item : items) {
						if (item.getChecked()) {
							counterList.add((String) item.getData());
						}
					}
					long stime = DateUtil.yyyymmdd(from) + (CastUtil.clong(fromTime.getText()) * DateUtil.MILLIS_PER_HOUR);
					long etime = DateUtil.yyyymmdd(to) + (CastUtil.clong(toTime.getText()) * DateUtil.MILLIS_PER_HOUR);
					callback.completeSelection(stime, etime, objList, counterList);
				}
				dialog.close();
			}
		});
		
		getBtn.notifyListeners(SWT.Selection, new Event());
		
		dialog.setDefaultButton(okBtn);
		dialog.pack();
		dialog.open();
	}
	
	private void queryObjSet() {
		long stime = DateUtil.yyyymmdd(from);
		long etime = DateUtil.yyyymmdd(to);
		HashMap<String, ListValue> valueMap = new HashMap<String, ListValue>();
		while (stime <= etime) {
			String date = DateUtil.yyyymmdd(stime);
			ListValue lv = agentProxy.getObjHashLv(date, serverId, objType);
			valueMap.put(date, lv);
			stime += DateUtil.MILLIS_PER_DAY;
		}
		objTable.removeAll();
		Set<String> dateSet = valueMap.keySet();
		HashSet<Integer> alreadySet = new HashSet<Integer>();
		for (String date : dateSet) {
			ListValue lv = valueMap.get(date);
			for (int i = 0; i < lv.size(); i++) {
				int objHash = (int) lv.getLong(i);
				if (alreadySet.contains(objHash) == false) {
					alreadySet.add(objHash);
					String objName = TextProxy.object.getLoadText(date, objHash, serverId);
					TableItem item = new TableItem(objTable, SWT.NONE);
					item.setText(objName);
					item.setData(objHash);
				}
			}
		}
		sortTable(objTable);
	}
	
	private void sortTable(Table table) {
		TableItem[] items = table.getItems();
		Collator collator = Collator.getInstance(Locale.getDefault());
		for (int i = 1; i < items.length; i++) {
			String value1 = items[i].getText(0);
			for (int j = 0; j < i; j++) {
				String value2 = items[j].getText(0);
				if (collator.compare(value1, value2) < 0) {
					String text = items[i].getText(0);
					boolean checked = items[i].getChecked();
					Object data = items[i].getData();
					items[i].dispose();
					TableItem item = new TableItem(table, SWT.NONE, j);
					item.setText(text);
					item.setChecked(checked);
					item.setData(data);
					items = table.getItems();
					break;
				}
			}
		}
	}
	
	public interface ICounterObjectCallback {
		public void completeSelection(long from, long to, List<ObjectPack> objList, List<String> counterList);
	}
}
