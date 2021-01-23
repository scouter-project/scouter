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
package scouter.client.heapdump.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import scouter.client.Images;
import scouter.client.heapdump.actions.HeapDumpDeleteAction;
import scouter.client.heapdump.actions.HeapDumpDownloadAction;
import scouter.client.net.TcpProxy;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ChartUtil;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.SortUtil;
import scouter.client.util.TableControlAdapter;
import scouter.client.util.UIUtil;
import scouter.client.util.UIUtil.ViewWithTable;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.FormatUtil;
import scouter.util.StringUtil;


public class HeapDumpListView extends ViewPart implements ViewWithTable{
	public static final String ID = HeapDumpListView.class.getName();

	private Table table = null;
	private int objHash;
	private String objName;
	
	private IMemento memento;
	Display display = null;
	
	boolean asc = true;
	int col_idx;
	boolean isNum;
	private int serverId;
	
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		this.memento = memento;
	}

	public void createPartControl(Composite parent) {
		
		display = Display.getCurrent();
		parent.setLayout(ChartUtil.gridlayout(1));

		table = build(parent);
		table.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL));

		table.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.F5) {
					reload();
				}
			}
		});
		
		table.addMouseListener(new MouseListener() {
			public void mouseUp(MouseEvent e) {
			}

			public void mouseDown(MouseEvent event) {
				if (event.button == 3) {
					TableItem[] tebleItems = table.getSelection();
					final String fileName = tebleItems[0].getText();
					
					Menu menu = new Menu(table.getShell(), SWT.POP_UP);
					
					MenuItem downloadItem = new MenuItem(menu, SWT.PUSH);
					downloadItem.setText("Download");
					downloadItem.setImage(Images.download);
					downloadItem.addListener(SWT.Selection, new Listener() {
						public void handleEvent(Event event) {
							IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
							Action act = new HeapDumpDownloadAction(window, "Download Binary Dump", fileName, objName, objHash, Images.heap, serverId);
							act.run();
						}
					});
					
					MenuItem deleteItem = new MenuItem(menu, SWT.PUSH);
					deleteItem.setText("Delete");
					deleteItem.setImage(Images.table_delete);
					deleteItem.addListener(SWT.Selection, new Listener() {
						public void handleEvent(Event event) {
							IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
							Action act = new HeapDumpDeleteAction(window, "Delete Binary Dump", fileName, objHash, fileName, Images.heap, serverId);
							act.run();
							reload();
						}
					});
					
					Point pt = new Point(event.x, event.y);
					pt = table.toDisplay(pt);
					menu.setLocation(pt.x, pt.y);
					menu.setVisible(true);
				}
			}

			public void mouseDoubleClick(MouseEvent e) {
			}
		});
		
		parent.addControlListener(new TableControlAdapter(table, cols, new int[]{-1, 6}));
		
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("Reload", ImageUtil.getImageDescriptor(Images.refresh)) {
			public void run() {
				reload();
			}
		});
		
	    restoreState();
	}

	public void setInput(int objHash, String objName, int serverId) {
		this.objHash = objHash;
		this.objName = objName;
		this.serverId = serverId;
		
		Server server = ServerManager.getInstance().getServer(serverId);
		String svrName = server.getName();
		
		setContentDescription("ⓢ"+svrName+" | Heap Dump files in \'"+objName+"\'");
		ExUtil.exec(new Runnable() {
			public void run() {
				reload();
			}
		});
		
	}

	public void reload() {
		if (table == null)
			return;

		MapPack mpack = null;
		
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			MapPack param = new MapPack();
			param.put("objHash", objHash);
			param.put("objName", objName);
			mpack = (MapPack) tcp.getSingle(RequestCmd.OBJECT_LIST_HEAP_DUMP, param);
		} catch(Exception e){
			ConsoleProxy.errorSafe(e.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		
		if(mpack == null){
			table.removeAll();
			return;
		}
		ListValue nameLv = mpack.getList("name");
		ListValue sizeLv = mpack.getList("size");

		// 2013-08-20 BY jonghun@lgcns.com
		// FOR SCROLL MAINTAINCE, DO NOT CALL 'removeAll();' METHOD.
		// ALTERNATIVELY, GETTING TABLE ITEMS, THEN UPDATE EACH DATA.
		TableItem[] tItem = table.getItems();
		if(tItem.length != nameLv.size()){
			table.removeAll();
			tItem = table.getItems();
		}
		
		int rows = nameLv == null ? 0 : nameLv.size();
		for (int i = 0; i < rows; i++) {
			TableItem t = null;
			if(tItem != null && tItem.length > 0 && i < tItem.length && tItem[i] != null){
				t = tItem[i];
			}else{
				t = new TableItem(table, SWT.NONE, i);
			}
			
			double size = CastUtil.clong(sizeLv.get(i));
			String fileSize = "";
			if(size  > 1024*1024){
				fileSize = FormatUtil.print(size/(1024*1024), "#,##0.0") + " MB";
			}else if(size > 1024){
				fileSize = FormatUtil.print(size/(1024), "#,##0.0") + " KB";
			}
			String[] datas = new String[] {
					CastUtil.cString(nameLv.get(i)),
					fileSize,
			};
			for(int inx = 0 ; inx < datas.length ; inx++){
				datas[inx] = StringUtil.trimToEmpty(datas[inx]);
			}
			t.setText(datas);
		}
		sortTable();
	}

	TableColumn[] cols;
	private Table build(Composite parent) {
		final Table table = new Table(parent, SWT.BORDER | SWT.WRAP | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		cols = new TableColumn[2];
		cols[0] = UIUtil.create(table, SWT.LEFT, "Name", cols.length, 0, false, 350, this);
		cols[1] = UIUtil.create(table, SWT.RIGHT, "Size", cols.length, 1, true, 100, this);

		return table;
	}

	public void setSortCriteria(boolean asc, int col_idx, boolean isNum) {
		this.asc = asc;
		this.col_idx = col_idx;
		this.isNum = isNum;
	}

	public void setTableItem(TableItem t){
	}
	
	public void setFocus() {
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

	public void saveState(IMemento memento) {
		super.saveState(memento);
		memento = memento.createChild(ID);
		memento.putInteger("objHash", objHash);
		memento.putString("objName", objName);
		memento.putInteger("serverId", serverId);
	}

	private void restoreState() {
		if (memento == null)
			return;
		IMemento m = memento.getChild(ID);
		if(m == null)
			return;
		int objHash = m.getInteger("objHash");
		String objName = m.getString("objName");
		int serverId = CastUtil.cint(m.getInteger("serverId"));
		setInput(objHash, objName, serverId);
	}

	
	
}
