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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.util.ChartUtil;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.SortUtil;
import scouter.client.util.TableControlAdapter;
import scouter.client.util.UIUtil;
import scouter.client.util.UIUtil.ViewWithTable;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.util.CastUtil;
import scouter.util.FormatUtil;


public class ClientThreadListView extends ViewPart implements ViewWithTable{
	public static final String ID = ClientThreadListView.class.getName();

	private Table table = null;
	IToolBarManager man;
	
	public void createPartControl(Composite parent) {
		parent.setLayout(ChartUtil.gridlayout(1));

		final Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(ChartUtil.gridlayout(1));
		comp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL));

		table = build(comp);
		table.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL));

		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.F5) {
					reload();
				}
			}
		});
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				TableItem[] item = table.getSelection();
				if (item == null || item.length == 0)
					return;
				long threadId = CastUtil.clong(item[0].getText(0));
				try {
					IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
					ClientThreadDetailView view = (ClientThreadDetailView) win.getActivePage().showView(ClientThreadDetailView.ID, "" + threadId, IWorkbenchPage.VIEW_ACTIVATE);
					view.setInput("[" + threadId + "]", threadId);
				} catch (Exception d) {
					d.printStackTrace();
				}
			}
		});

		comp.addControlListener(new TableControlAdapter(table, cols, new int[]{20, -1, 10, 10}));
		
		man = getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("reload", ImageUtil.getImageDescriptor(Images.refresh)) {
			public void run() {
				reload();
			}
		});
		
	}

	public void setInput(){
		ExUtil.exec(new Runnable(){
			public void run() {
				reload();	
			}
		});
	}
	
	public void reload() {
		if (table == null)
			return;
		table.removeAll();

		MapPack mpack = getThreadList();
		ListValue idLv = mpack.getList("id");
		ListValue nameLv = mpack.getList("name");
		ListValue statLv = mpack.getList("stat");
		ListValue cpuLv = mpack.getList("cpu");
		
		int rows = idLv == null ? 0 : idLv.size();
		for (int i = 0; i < rows; i++) {
			TableItem t = new TableItem(table, SWT.NONE, i);
			t.setText(new String[] { //
			//
					FormatUtil.print(idLv.get(i), "000"), //
					CastUtil.cString(nameLv.get(i)),//
					CastUtil.cString(statLv.get(i)),//
					FormatUtil.print(cpuLv.get(i), "#,##0"),//

			});

		}
		sortTable();
	}

	public static MapPack getThreadList() {
		ThreadMXBean tmb = ManagementFactory.getThreadMXBean();

		long[] thread = tmb.getAllThreadIds();
		MapPack pack = new MapPack();
		ListValue id = pack.newList("id");
		ListValue name = pack.newList("name");
		ListValue stat = pack.newList("stat");
		ListValue cpu = pack.newList("cpu");

		for (int i = 0; i < thread.length; i++) {
			ThreadInfo fo = tmb.getThreadInfo(thread[i]);
			id.add(fo.getThreadId());
			name.add(fo.getThreadName());
			stat.add(fo.getThreadState().toString());
			cpu.add(tmb.getThreadCpuTime(thread[i]) / 1000000);
		}

		return pack;
	}
	
	TableColumn[] cols;
	private Table build(Composite parent) {
		final Table table = new Table(parent, SWT.BORDER | SWT.WRAP | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		cols = new TableColumn[4];
		cols[0] = UIUtil.create(table, SWT.CENTER, "No", cols.length, 0, true, 40, this);
		cols[1] = UIUtil.create(table, SWT.LEFT, "Name", cols.length, 1, false, 400, this);
		cols[2] = UIUtil.create(table, SWT.CENTER, "Stat", cols.length, 2, false, 100, this);
		cols[3] = UIUtil.create(table, SWT.RIGHT, "Cpu", cols.length, 3, true, 60, this);
	
		return table;
	}

	public void setFocus() {
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	boolean asc;
	int col_idx;
	boolean isNum;
	
	public void setSortCriteria(boolean asc, int col_idx, boolean isNum) {
		this.asc = asc;
		this.col_idx = col_idx;
		this.isNum = isNum;
		
	}

	public void setTableItem(TableItem t) {
	}

	public void sortTable(){
		int col_count = table.getColumnCount();
		TableItem[] items = table.getItems();
		if (isNum) {
			new SortUtil(asc).sort_num(items, col_idx, col_count);
		} else {
			new SortUtil(asc).sort_str(items, col_idx, col_count);
		}
	}
	
}