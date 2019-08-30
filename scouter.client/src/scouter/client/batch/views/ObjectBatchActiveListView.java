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
package scouter.client.batch.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
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
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.batch.actions.OpenBatchActiveStackJob;
import scouter.client.model.AgentDataProxy;
import scouter.client.model.BatchData;
import scouter.client.model.RefreshThread;
import scouter.client.model.RefreshThread.Refreshable;
import scouter.client.model.TextProxy;
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
import scouter.util.DateUtil;
import scouter.util.FormatUtil;

public class ObjectBatchActiveListView extends ViewPart implements Refreshable {
	
	public static final String ID = ObjectBatchActiveListView.class.getName();

	private int serverId;
	private String objType;
	private int objHash = 0;
	CounterEngine counterEngine;
	private String key;
	
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
			this.setPartName("Batch Active List[" + counterEngine.getDisplayNameObjectType(objType) + "]");
		} else {
			this.setPartName("Batch Active List[" + TextProxy.object.getText(this.objHash) + "]");
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
		
	    Action actAutoRefresh = new Action("Auto Refresh in 10 sec.", IAction.AS_CHECK_BOX){ 
	        public void run() {
	        	autoRefresh = isChecked();
	        	if (autoRefresh) {
	        		thread.interrupt();
	        	}
	        }
	    }; 
	    actAutoRefresh.setImageDescriptor(ImageUtil.getImageDescriptor(Images.refresh_auto));
	    man.add(actAutoRefresh);
	    
		thread = new RefreshThread(this, 10000);
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
				if (o instanceof BatchData) {
					BatchData data = (BatchData) o;
					if(!data.lastStack){
						return;
					}
					key = data.key;
					openThreadDumpDialog.run();
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
		List<Pack> packList = AgentDataProxy.getBatchActiveList(objType, objHash, serverId);
		final ArrayList<BatchData> datas = new ArrayList<BatchData>();
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
			
			ListValue keys = mpack.getList("key");
			ListValue batchJobId = mpack.getList("batchJobId");
			ListValue args = mpack.getList("args");
			ListValue pID = mpack.getList("pID");
			ListValue startTime = mpack.getList("startTime");
			ListValue elapsedTime = mpack.getList("elapsedTime");
			ListValue cPUTime = mpack.getList("cPUTime");
			ListValue sqlTotalTime = mpack.getList("sqlTotalTime");
			ListValue sqlTotalRows = mpack.getList("sqlTotalRows");
			ListValue sqlTotalRuns = mpack.getList("sqlTotalRuns");
			ListValue lastStack = mpack.getList("lastStack");
	
			if (keys != null) {
				int size = keys.size();
				count.value = count.value + size;
				BatchData data;
				for (int i = 0; i < size; i++) {
					data = new BatchData();
					data.key = keys.getString(i);
					data.objHash = objHash;
					data.batchJobId = batchJobId.getString(i);
					data.args = args.getString(i);
					data.pID = (int)pID.getLong(i);
					data.startTime = startTime.getLong(i);
					data.elapsedTime = elapsedTime.getLong(i);
					data.cPUTime = cPUTime.getLong(i);
					data.sqlTotalTime = sqlTotalTime.getLong(i);
					data.sqlTotalRows = sqlTotalRows.getLong(i);
					data.sqlTotalRuns = sqlTotalRuns.getLong(i);
					data.lastStack = lastStack.getBoolean(i);
					datas.add(data);
				}
			}
			Collections.sort(datas, new Comparator<BatchData>() {
				public int compare(BatchData o1, BatchData o2) {
					return o1.elapsedTime > o2.elapsedTime ? -1 : 1;
				}
			});
			
			int index = 1;
			for(BatchData data : datas){
				data.id = index;
				index++;
			}
		}
		if (error.length() > 0) {
			error.append("may be not loaded.");
		}
		ExUtil.exec(tableViewer.getTable(), new Runnable() {
			public void run() {
				ObjectBatchActiveListView.this.setContentDescription("Count = " + count.value);
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
			if (element instanceof BatchData) {
				BatchData t = (BatchData) element;
				if (t.elapsedTime > 3600000) {
					return ColorUtil.getInstance().getColor(SWT.COLOR_RED);
				} else if (t.elapsedTime > 1800000) {
					return ColorUtil.getInstance().getColor(SWT.COLOR_MAGENTA);
				} else {
					return ColorUtil.getInstance().getColor(SWT.COLOR_BLUE);
				}
			}
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof BatchData) {
				if (columnIndex == ColumnEnum.NO.getIndex()) {
					return FormatUtil.print(((BatchData) element).id, "000");
				} else if (columnIndex == ColumnEnum.OBJNAME.getIndex()) {
					return TextProxy.object.getText(((BatchData) element).objHash);
				} else if (columnIndex == ColumnEnum.BATCHJOBID.getIndex()) {
					return ((BatchData) element).batchJobId;
				} else if (columnIndex == ColumnEnum.ARGS.getIndex()) {
					return ((BatchData) element).args;
				} else if (columnIndex == ColumnEnum.PID.getIndex()) {
					return ((BatchData) element).pID + "";
				} else if (columnIndex == ColumnEnum.STARTTIME.getIndex()) {
					return DateUtil.yyyymmdd(((BatchData) element).startTime) + " " + DateUtil.hhmmss(((BatchData) element).startTime);
				} else if (columnIndex == ColumnEnum.ELAPSEDTIME.getIndex()) {
					return FormatUtil.print(((BatchData) element).elapsedTime, "#,##0");
				} else if (columnIndex == ColumnEnum.CPUTIME.getIndex()) {
					return FormatUtil.print((((BatchData) element).cPUTime/1000000L), "#,##0");
				} else if (columnIndex == ColumnEnum.SQLTOTALTIME.getIndex()) {
					return FormatUtil.print((((BatchData) element).sqlTotalTime / 1000000L), "#,##0");
				} else if (columnIndex == ColumnEnum.SQLTOTALROWS.getIndex()) {
					return FormatUtil.print(((BatchData) element).sqlTotalRows, "#,##0");
				} else if (columnIndex == ColumnEnum.SQLTOTALRUNS.getIndex()) {
					return FormatUtil.print(((BatchData) element).sqlTotalRuns, "#,##0");
				} else if (columnIndex == ColumnEnum.LASTSTAK.getIndex()) {
					if(((BatchData) element).lastStack){
						return "O";
					}else{
						return "";
					}
				}
			}
			return null;
		}
	}

	public void setFocus() {
	}

	enum ColumnEnum {
		NO("NO", 60, SWT.RIGHT, true, true, true, 0),
		OBJNAME("Object Name", 150, SWT.LEFT, true, true, false, 1),
		BATCHJOBID("Bath Job ID", 150, SWT.LEFT, true, true, false, 2),
		ARGS("Arguments", 200, SWT.LEFT, true, true, false, 3),
		PID("PID", 80, SWT.LEFT, true, true, true, 4),
		STARTTIME("Start Time", 150, SWT.RIGHT, true, true, false, 5),
		ELAPSEDTIME("Elapsed Time", 100, SWT.RIGHT, true, true, true, 6),
		CPUTIME("CPU Time", 100, SWT.RIGHT, true, true, true, 7),
		SQLTOTALTIME("SQL Time", 100, SWT.RIGHT, true, true, true, 8),
		SQLTOTALROWS("SQL Rows", 100, SWT.RIGHT, true, true, true, 9),
	    SQLTOTALRUNS("SQL Runs", 100, SWT.RIGHT, true, true, true, 10),
	    LASTSTAK("Stack", 70, SWT.CENTER, true, true, false, 11);

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

	Action openThreadDumpDialog = new Action("Batch Thread Dump View", ImageUtil.getImageDescriptor(Images.thread)) {
		public void run() {
			new OpenBatchActiveStackJob(key, objHash, serverId).schedule();
		}
	};
	
}