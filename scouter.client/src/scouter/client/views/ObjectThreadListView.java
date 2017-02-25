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

import java.util.HashMap;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.model.AgentDataProxy;
import scouter.client.model.DetachedManager;
import scouter.client.model.RefreshThread;
import scouter.client.model.RefreshThread.Refreshable;
import scouter.client.util.ChartUtil;
import scouter.client.util.ColorUtil;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.ScouterUtil;
import scouter.client.util.SortUtil;
import scouter.client.util.UIUtil;
import scouter.client.util.UIUtil.ViewWithTable;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.util.CastUtil;
import scouter.util.FormatUtil;
import scouter.util.StringUtil;


public class ObjectThreadListView extends ViewPart implements Refreshable, ViewWithTable{
	public static final String ID = ObjectThreadListView.class.getName();

	private Table table = null;
	public int objHash;
	public int serverId;
	Action actAutoRefresh, actFilter;

	protected RefreshThread thread = null;
	Display display = null;
	boolean refresh = false;
	
	boolean asc;
	int col_idx;
	boolean isNum;
	
	public HashMap<Long, Long> prevCpuMap = new HashMap<Long, Long>();
	
	public void createPartControl(Composite parent) {
		display = getSite().getShell().getDisplay();
		parent.setLayout(ChartUtil.gridlayout(1));
		Composite area2 = new Composite(parent, SWT.NONE);
		area2.setLayout(ChartUtil.gridlayout(1));
		area2.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL));
		table = build(area2);
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
					ObjectThreadDetailView view = (ObjectThreadDetailView) win.getActivePage().showView(ObjectThreadDetailView.ID, serverId + "&" +  objHash, IWorkbenchPage.VIEW_ACTIVATE);
					view.setInput(threadId, 0L);
				} catch (Exception d) {
				}
			}
		});

		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("reload", ImageUtil.getImageDescriptor(Images.refresh)) {
			public void run() {
				reload();
			}
		});
		man.add(new Separator());
		
	    actAutoRefresh = new Action("Auto Refresh in 5 sec.", IAction.AS_CHECK_BOX){ 
	        public void run(){    
	        	if(actAutoRefresh.isChecked())   	
	        		reload();    
	        }    
	    };  
	    actAutoRefresh.setImageDescriptor(ImageUtil.getImageDescriptor(Images.refresh_auto));
	    man.add(actAutoRefresh);
	}

	public void setInput(int objHash, int serverId) {
		this.objHash = objHash;
		this.serverId = serverId;
		reload();
		
		if (thread == null) {
			thread = new RefreshThread(this, 5000);
			thread.start();
		}
		
		thread.setName(this.toString() + " - " + "objHash:"+objHash+", serverId:"+serverId);
	}

	public void reload() {
		if (table == null)
			return;

		ExUtil.asyncRun(new Runnable() {
			public void run() {
				final MapPack mpack = AgentDataProxy.getThreadList(objHash, serverId);
				
				if(mpack == null){
					refresh = true;
					return;
				}else{
					refresh = false;
				}
				
				ExUtil.exec(table, new Runnable() {
					public void run() {
						TableItem[] tItem = table.getItems();
						
						ListValue idLv = mpack.getList("id");
						ListValue nameLv = mpack.getList("name");
						ListValue statLv = mpack.getList("stat");
						ListValue cpuLv = mpack.getList("cpu");
						ListValue txidLv = mpack.getList("txid");
						ListValue elapsedLv = mpack.getList("elapsed");
						ListValue serviceLv = mpack.getList("service");

						int rows = idLv == null ? 0 : idLv.size();
						for (int i = 0; i < rows; i++) {
							TableItem t = null;
							if(tItem != null && tItem.length > 0 && i < tItem.length && tItem[i] != null){
								t = tItem[i];
							}else{
								t = new TableItem(table, SWT.NONE, i);
							}
							long threadId = idLv.getLong(i);
							long cputime = cpuLv.getLong(i);
							long delta = 0;
							Long prevCpu = prevCpuMap.get(threadId);
							if (prevCpu != null) {
								delta = cputime - prevCpu.longValue();
							}
							prevCpuMap.put(threadId, cputime);
							String[] datas = new String[] {
									FormatUtil.print(threadId, "000"), //
									CastUtil.cString(nameLv.get(i)),//
									CastUtil.cString(statLv.get(i)),//
									FormatUtil.print(cputime, "#,##0"),//
									FormatUtil.print(delta, "#,##0"),//
									CastUtil.cString(txidLv.get(i)),//
									FormatUtil.print(elapsedLv.get(i), "#,##0"),//
									CastUtil.cString(serviceLv.get(i)),//
							};
							for(int inx = 0 ; inx < datas.length ; inx++){
								datas[inx] = StringUtil.trimToEmpty(datas[inx]);
							}
							t.setText(datas);
						}
						sortTable();
					}
				});
			}
		});
		
	}

	public void setColor(TableItem t) {
		if (StringUtil.isNotEmpty(t.getText(7))) {
			int time = CastUtil.cint(StringUtil.strip(t.getText(6), ","));
			if (time > 8000) {
				t.setForeground(ColorUtil.getInstance().getColor(SWT.COLOR_RED));
			} else if (time > 3000) {
				t.setForeground(ColorUtil.getInstance().getColor(SWT.COLOR_MAGENTA));
			} else {
				t.setForeground(ColorUtil.getInstance().getColor(SWT.COLOR_BLUE));
			}
		} else {
			t.setForeground(ColorUtil.getInstance().getColor(SWT.COLOR_BLACK));
		}
	}

	private Table build(Composite parent) {
		final Table table = new Table(parent, SWT.BORDER | SWT.WRAP | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		TableColumn[] cols = new TableColumn[8];
		cols[0] = UIUtil.create(table, SWT.CENTER, "No", cols.length, 0, true, 40, this);
		cols[1] = UIUtil.create(table, SWT.LEFT, "Name", cols.length, 1, false, 250, this);
		cols[2] = UIUtil.create(table, SWT.CENTER, "Stat", cols.length, 2, false, 100, this);
		cols[3] = UIUtil.create(table, SWT.RIGHT, "Cpu", cols.length, 3, true, 60, this);
		cols[4] = UIUtil.create(table, SWT.RIGHT, "\u0394 Cpu", cols.length, 4, true, 60, this);
		cols[5] = UIUtil.create(table, SWT.LEFT, "Txid", cols.length, 5, false, 70, this);
		cols[6] = UIUtil.create(table, SWT.RIGHT, "Elapsed", cols.length, 6, true, 60, this);
		cols[7] = UIUtil.create(table, SWT.LEFT, "Service Name", cols.length, 7, false, 200, this);

		return table;
	}

	public void setSortCriteria(boolean asc, int col_idx, boolean isNum) {
		this.asc = asc;
		this.col_idx = col_idx;
		this.isNum = isNum;
	}

	public void setTableItem(TableItem t) {
		setColor(t);
	}
	
	public void setFocus() {
	}

	@Override
	public void dispose() {
		super.dispose();
		if(thread != null && thread.isAlive()){
			thread.shutdown();
			thread = null;
		}
	}

	public void refresh() {
		if(refresh || actAutoRefresh.isChecked()){
			reload();
		}
	}
	
	public void sortTable(){
		int col_count = table.getColumnCount();
		TableItem[] items = table.getItems();
		if (isNum) {
			new SortUtil(asc).sort_num(items, col_idx, col_count);
		} else {
			new SortUtil(asc).sort_str(items, col_idx, col_count);
		}
		for (int i = 0; i < items.length; i++) {
			setColor(items[i]);
		}
	}

}
