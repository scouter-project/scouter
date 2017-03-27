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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.*;
import org.eclipse.ui.part.ViewPart;
import scouter.client.Images;
import scouter.client.model.AgentDataProxy;
import scouter.client.model.RefreshThread;
import scouter.client.model.RefreshThread.Refreshable;
import scouter.client.model.TextProxy;
import scouter.client.model.ThreadData;
import scouter.client.server.ServerManager;
import scouter.client.sorter.TableLabelSorter;
import scouter.client.util.ColorUtil;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.lang.counters.CounterEngine;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.DecimalValue;
import scouter.lang.value.ListValue;
import scouter.util.CastUtil;
import scouter.util.FormatUtil;
import scouter.util.Hexa32;
import scouter.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ObjectActiveServiceListView extends ViewPart implements Refreshable {
	
	public static final String ID = ObjectActiveServiceListView.class.getName();

	private int serverId;
	private String objType;
	private int objHash = 0;
	CounterEngine counterEngine;
	
	private TableViewer tableViewer;
	private TableColumnLayout tableColumnLayout;
	private Label errorLbl;
	
	RefreshThread thread;
	boolean autoRefresh = false;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String ids[] = secId.split("&");
		this.serverId = CastUtil.cint(ids[0]);
		this.objType = ids[1];
		if (ids.length > 2) {
			this.objHash = CastUtil.cint(ids[2]);
		}
	}

	public void createPartControl(Composite parent) {
		counterEngine = ServerManager.getInstance().getServer(serverId).getCounterEngine();
		if (this.objHash == 0) {
			this.setPartName("Active Service List[" + counterEngine.getDisplayNameObjectType(objType) + "]");
		} else {
			this.setPartName("Active Service List[" + TextProxy.object.getText(this.objHash) + "]");
		}
		initialLayout(parent);
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("reload", ImageUtil.getImageDescriptor(Images.refresh)) {
			public void run() {
				manulRefresh = true;
				thread.interrupt();
			}
		});
		man.add(new Separator());
		
	    Action actAutoRefresh = new Action("Auto Refresh in 5 sec.", IAction.AS_CHECK_BOX){ 
	        public void run() {
	        	autoRefresh = isChecked();
	        	if (autoRefresh) {
	        		thread.interrupt();
	        	}
	        }
	    };  
	    actAutoRefresh.setImageDescriptor(ImageUtil.getImageDescriptor(Images.refresh_auto));
	    man.add(actAutoRefresh);
	    
		thread = new RefreshThread(this, 5000);
		thread.start();
	}
	
	private void initialLayout(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, true));
		errorLbl = new Label(composite, SWT.NONE);
		errorLbl.setForeground(ColorUtil.getInstance().getColor(SWT.COLOR_RED));
		GridData gr = new GridData(SWT.FILL, SWT.FILL, true, false);
		gr.exclude = true;
		errorLbl.setLayoutData(gr);
		errorLbl.setVisible(false);
		Composite tableComposite = new Composite(composite, SWT.NONE);
		tableComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		tableComposite.setLayout(new GridLayout(1, true));
		createTableViewer(tableComposite);
	}

	private void createTableViewer(Composite composite) {
		tableViewer = new TableViewer(composite, SWT.MULTI  | SWT.FULL_SELECTION | SWT.BORDER);
		tableColumnLayout = new TableColumnLayout();
		composite.setLayout(tableColumnLayout);
		createColumns();
		final Table table = tableViewer.getTable();
	    table.setHeaderVisible(true);
	    table.setLinesVisible(true);
	    tableViewer.setContentProvider(new ArrayContentProvider());
	    tableViewer.setLabelProvider(new LabelProvider());
	    tableViewer.setComparator(new TableLabelSorter(tableViewer));
	    GridData gridData = new GridData(GridData.FILL, GridData.FILL, true, true);
	    tableViewer.getControl().setLayoutData(gridData);
	    tableViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				StructuredSelection sel = (StructuredSelection) event.getSelection();
				Object o = sel.getFirstElement();
				if (o instanceof ThreadData) {
					ThreadData data = (ThreadData) o;
					try {
						IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
						ObjectThreadDetailView view = (ObjectThreadDetailView) win.getActivePage().showView(ObjectThreadDetailView.ID, serverId + "&" + data.objHash, IWorkbenchPage.VIEW_ACTIVATE);
						view.setInput(data.id, Hexa32.toLong32(data.txid));
					} catch (Exception d) {
					}
				}
			}
		});
	}
	
	boolean manulRefresh = true;
	
	public void refresh() {
		if (manulRefresh == false && autoRefresh == false) {
			return;
		}
		manulRefresh = false;
		load();
	}
	
	private void load() {
		List<Pack> packList = AgentDataProxy.getActiveThreadList(objType, objHash, serverId);
		final ArrayList<ThreadData> datas = new ArrayList<ThreadData>();
		final DecimalValue count = new DecimalValue();
		final StringBuilder error = new StringBuilder();
		for (Pack pack : packList) {
			MapPack mpack = (MapPack) pack;
			boolean complete = mpack.getBoolean("complete");
			int objHash = mpack.getInt("objHash");
			if (complete == false) {
				String objName = TextProxy.object.getText(objHash);
				if (objName != null) {
					error.append(objName + " ");
				}
			}
			ListValue idLv = mpack.getList("id");
			ListValue nameLv = mpack.getList("name");
			ListValue statLv = mpack.getList("stat");
			ListValue cpuLv = mpack.getList("cpu");
			ListValue txidLv = mpack.getList("txid");
			ListValue elapsedLv = mpack.getList("elapsed");
			ListValue serviceLv = mpack.getList("service");
			ListValue ipLv = mpack.getList("ip");
			ListValue sqlLv = mpack.getList("sql");
			ListValue subcallLv = mpack.getList("subcall");
			
			if (idLv != null) {
				int size = idLv.size();
				count.value = count.value + size;
				
				for (int i = 0; i < size; i++) {
					ThreadData data = new ThreadData();
					data.id = idLv.getLong(i);
					data.objHash = objHash;
					data.name = nameLv.getString(i);
					data.state = statLv.getString(i);
					data.cpu = cpuLv.getLong(i);
					data.txid = txidLv.getString(i);
					data.elapsed = elapsedLv.getLong(i);
					data.serviceName = serviceLv.getString(i);
					String sql = sqlLv.getString(i);
					if (StringUtil.isNotEmpty(sql)) {
						data.note = sql;
					} else {
						data.note = subcallLv.getString(i);
					}
					if (ipLv != null)
						data.ip = ipLv.getString(i);
					datas.add(data);
				}
			}
			Collections.sort(datas, new Comparator<ThreadData>() {
				public int compare(ThreadData o1, ThreadData o2) {
					return o1.elapsed > o2.elapsed ? -1 : 1;
				}
			});
		}
		if (error.length() > 0) {
			error.append("may be not loaded.");
		}
		ExUtil.exec(tableViewer.getTable(), new Runnable() {
			public void run() {
				ObjectActiveServiceListView.this.setContentDescription("Count = " + count.value);
				if (error.length() > 0) {
					GridData gr = (GridData) errorLbl.getLayoutData();
					gr.exclude = false;
					errorLbl.setVisible(true);
					errorLbl.setText(error.toString());
				} else {
					GridData gr = (GridData) errorLbl.getLayoutData();
					gr.exclude = true;
					errorLbl.setVisible(false);
					errorLbl.setText("");
				}
				errorLbl.getParent().layout(false);
				tableViewer.setInput(datas);
			}
		});
	}
	
	private void createColumns() {
		for (ColumnEnum column : ColumnEnum.values()) {
			createTableViewerColumn(column.getTitle(), column.getWidth(), column.getAlignment(), column.isResizable(), column.isMoveable(), column.isNumber());
		}
	}
	
	private TableViewerColumn createTableViewerColumn(String title, int width, int alignment,  boolean resizable, boolean moveable, final boolean isNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(tableViewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setAlignment(alignment);
		column.setMoveable(moveable);
		tableColumnLayout.setColumnData(column, new ColumnWeightData(width, width, resizable));
		column.setData("isNumber", isNumber);
		column.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				TableLabelSorter sorter = (TableLabelSorter) tableViewer.getComparator();
				TableColumn selectedColumn = (TableColumn) e.widget;
				sorter.setColumn(selectedColumn);
			}
		});
		return viewerColumn;
	}
	
	class LabelProvider implements ITableLabelProvider, IColorProvider {

		public void addListener(ILabelProviderListener listener) {
		}

		public void dispose() {
		}

		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		public void removeListener(ILabelProviderListener listener) {
		}

		public Color getBackground(Object element) {
			return null;
		}

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
		
		public Color getForeground(Object element) {
			if (element instanceof ThreadData) {
				ThreadData t = (ThreadData) element;
				if (t.elapsed > 8000) {
					return ColorUtil.getInstance().getColor(SWT.COLOR_RED);
				} else if (t.elapsed > 3000) {
					return ColorUtil.getInstance().getColor(SWT.COLOR_MAGENTA);
				} else {
					return ColorUtil.getInstance().getColor(SWT.COLOR_BLUE);
				}
			}
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof ThreadData) {
				if (columnIndex == ColumnEnum.NO.getIndex()) {
					return FormatUtil.print(((ThreadData) element).id, "000");
				} else if (columnIndex == ColumnEnum.OBJNAME.getIndex()) {
					return TextProxy.object.getText(((ThreadData) element).objHash);
				} else if (columnIndex == ColumnEnum.NAME.getIndex()) {
					return ((ThreadData) element).name;
				} else if (columnIndex == ColumnEnum.STATE.getIndex()) {
					return ((ThreadData) element).state;
				} else if (columnIndex == ColumnEnum.CPU.getIndex()) {
					return FormatUtil.print(((ThreadData) element).cpu, "#,##0");
				} else if (columnIndex == ColumnEnum.ELAPSED.getIndex()) {
					return FormatUtil.print(((ThreadData) element).elapsed, "#,##0");
				} else if (columnIndex == ColumnEnum.TXID.getIndex()) {
					return ((ThreadData) element).txid;
				} else if (columnIndex == ColumnEnum.SERVICE.getIndex()) {
					return ((ThreadData) element).serviceName;
				} else if (columnIndex == ColumnEnum.NOTE.getIndex()) {
					return ((ThreadData) element).note;
				} else if (columnIndex == ColumnEnum.IP.getIndex()) {
					return ((ThreadData) element).ip;
				}
			}
			return null;
		}
	}

	public void setFocus() {
	}

	enum ColumnEnum {
		OBJNAME("ObjectName", 150, SWT.LEFT, true, true, false, 0),
		SERVICE("Service", 200, SWT.LEFT, true, true, false, 1),
		ELAPSED("Elapsed", 60, SWT.RIGHT, true, true, true, 2),
		NOTE("Note", 200, SWT.LEFT, true, true, false, 3),
		CPU("Cpu", 60, SWT.RIGHT, true, true, true, 4),
		IP("IP", 100, SWT.LEFT, true, true, false, 5),
		STATE("State", 100, SWT.LEFT, true, true, false, 6),
		NAME("Name", 250, SWT.LEFT, true, true, false, 7),
		NO("No", 40, SWT.RIGHT, true, true, true, 8),
	    TXID("TxId", 70, SWT.LEFT, true, true, false, 9);

	    private final String title;
	    private final int width;
	    private final int alignment;
	    private final boolean resizable;
	    private final boolean moveable;
	    private final boolean isNumber;
	    private final int index;

	    private ColumnEnum(String text, int width, int alignment, boolean resizable, boolean moveable, boolean isNumber, int index) {
	        this.title = text;
	        this.width = width;
	        this.alignment = alignment;
	        this.resizable = resizable;
	        this.moveable = moveable;
	        this.isNumber = isNumber;
	        this.index = index;
	    }
	    
	    public String getTitle(){
	        return title;
	    }

	    public int getAlignment(){
	        return alignment;
	    }

	    public boolean isResizable(){
	        return resizable;
	    }

	    public boolean isMoveable(){
	        return moveable;
	    }

		public int getWidth() {
			return width;
		}
		
		public boolean isNumber() {
			return this.isNumber;
		}
		
		public int getIndex() {
			return this.index;
		}
	}

}

