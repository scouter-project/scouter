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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.model.RefreshThread;
import scouter.client.model.RefreshThread.Refreshable;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ImageUtil;
import scouter.client.util.SortUtil;
import scouter.util.StringUtil;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.lang.value.Value;
import scouter.util.CastUtil;
import scouter.util.StringUtil;

public class GeneralTableView extends ViewPart implements Refreshable {
	
	public static final String ID = GeneralTableView.class.getName();
	
	private MapPack pack;
	private Table table;
	private TableColumnLayout tableColumnLayout = new TableColumnLayout();
	Action action;
	Composite comp;
	boolean lastIsNum;
	boolean lastIsAsc;
	int lastSortIndex;
	Action actAutoRefresh;
	protected RefreshThread thread = null;
	
	boolean noCols = true;
	
	
	public void createPartControl(Composite parent) {
		comp = new Composite(parent, SWT.NONE);
		table = new Table(comp, SWT.BORDER | SWT.WRAP | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		comp.setLayout(tableColumnLayout);
		table.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {}
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.F5) {
					if (GeneralTableView.this.action != null) {
						GeneralTableView.this.action.run();
					}
				}
			}
		});
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		Action action = new Action("Reload", ImageUtil.getImageDescriptor(Images.refresh)) {
			public void run() {
				if (GeneralTableView.this.action != null) {
					GeneralTableView.this.action.run();
				}
			} 
		};
		man.add(action);
		actAutoRefresh = new Action("Auto Refresh in 5 sec.", IAction.AS_CHECK_BOX){ 
	        public void run(){    
	        	if(actAutoRefresh.isChecked()) { 	
	        		if (GeneralTableView.this.action != null) {
						GeneralTableView.this.action.run();
					}
	        	}
	        }    
	    };
	    actAutoRefresh.setImageDescriptor(ImageUtil.getImageDescriptor(Images.refresh_auto));
	    man.add(actAutoRefresh);
	    
	    if (thread == null) {
			thread = new RefreshThread(this, 5000);
			thread.start();
		}
	}
	
	public void setInput(Action action, final MapPack pack) throws IOException {
		this.action = action;
		if (pack == null || pack.size() < 1) {
			ConsoleProxy.error("empty value");
			return;
		}
		this.pack = pack;
		String name = pack.getText("_name_");
		if (StringUtil.isNotEmpty(name)) {
			setPartName(name);
		}
		table.setRedraw(false);
		process();
		table.setRedraw(true);
		comp.layout(true, true);
		table.redraw();
		
		thread.setName(this.toString() + " - " + "action:"+action+", pack:"+pack);
	}
	
	private void sortTable() {
		int col_count = table.getColumnCount();
		TableItem[] items = table.getItems();
		if (lastIsNum) {
			new SortUtil(lastIsAsc).sort_num(items, lastSortIndex, col_count);
		} else {
			new SortUtil(lastIsAsc).sort_str(items, lastSortIndex, col_count);
		}
	}
	
	private void process() {
		table.removeAll();
//		while ( table.getColumnCount() > 0 ) {
//		    table.getColumns()[0].dispose();
//		}
		try {
			int max = 0;
			List<ListValue> lvList = new ArrayList<ListValue>();
			String error = pack.getText("_error_");
			if (error != null) {
				ConsoleProxy.errorSafe(error);
			}
			String seq = pack.getText("_seq_");
			List<String> keys = null;
			if (seq != null) {
				keys = Arrays.asList(StringUtil.tokenizer(seq, "/"));
			} else {
				keys = new ArrayList<String>(pack.keySet());
			}
			int index = 0;
			for (String key : keys) {
				Value value = pack.get(key);
				if (value != null && value instanceof ListValue) {
					ListValue lv = (ListValue) value;
					if (noCols) {
						final TableColumn column = new TableColumn(table, SWT.LEFT);
						column.setText(key);
						column.setData("index", index);
						boolean isNum = true;
						try {
							for (int i = 0; i < lv.size(); i++) {
								String str = lv.getString(i);
								Double.valueOf(str);
							}
						} catch (Exception e) {
							isNum = false;
						}
						column.setData("isNum", isNum);
						tableColumnLayout.setColumnData(column, new ColumnWeightData(30));
						column.addListener(SWT.Selection, new Listener() {
							public void handleEvent(Event event) {
								TableItem[] items = table.getItems();
								boolean asc = CastUtil.cboolean(column.getData("sort"));
								column.setData("sort", new Boolean(!asc));
								boolean isNum = CastUtil.cboolean(column.getData("isNum"));
								if (isNum) {
									new SortUtil(asc).sort_num(items, CastUtil.cint(column.getData("index")), table.getColumnCount());
								} else {
									new SortUtil(asc).sort_str(items, CastUtil.cint(column.getData("index")), table.getColumnCount());
								}
								lastIsAsc = asc;
								lastIsNum = isNum;
								lastSortIndex =CastUtil.cint(column.getData("index"));
							}
						});
					}
					if (lv.size() > max) {
						max = lv.size();
					}
					lvList.add(lv);
					index++;
				}
			}
			noCols = false;
			List<String> tempStr = new ArrayList<String>();
			for (int i = 0; i < max; i++) {
				tempStr.clear();
				for (ListValue value : lvList) {
					String v = value.getString(i);
					if (v == null) {
						v = "";
					}
					tempStr.add(v);
				}
				TableItem item = new TableItem(table, SWT.NONE, i);
				item.setText(tempStr.toArray(new String[tempStr.size()]));
			}
			sortTable();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void dispose() {
		super.dispose();
		if(thread != null && thread.isAlive()){
			thread.shutdown();
			thread = null;
		}
	}

	public void setFocus() {
		if (table != null) {
			table.setFocus();
		}
	}

	public void refresh() {
		if (actAutoRefresh.isChecked()) {
			if (GeneralTableView.this.action != null) {
				GeneralTableView.this.action.run();
			}
		}
	}
}
