/*
 *  Copyright 2015 LG CNS.
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
package scouter.client.xlog.views;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.model.TextProxy;
import scouter.client.model.XLogData;
import scouter.client.preferences.PManager;
import scouter.client.preferences.PreferenceConstants;
import scouter.client.sorter.TableLabelSorter;
import scouter.client.threads.ObjectSelectManager;
import scouter.client.threads.ObjectSelectManager.IObjectCheckListener;
import scouter.client.util.ColorUtil;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.UIUtil;
import scouter.client.xlog.views.XLogViewPainter.ITimeChange;
import scouter.util.FormatUtil;
import scouter.util.LongKeyLinkedMap;
import scouter.util.StrMatch;


abstract public class XLogViewCommon extends ViewPart implements ITimeChange, IObjectCheckListener {
	private static final int MIN_HEIGHT_UP = 260;
	private static final int MIN_HEIGHT_DOWN = 50;
	private static final int DETAIL_TABLE_MAX_ROW = 200;

	protected XLogViewPainter viewPainter;
	protected XLogViewMouse mouse;
	protected Canvas canvas;
	protected LongKeyLinkedMap<XLogData> twdata  = new LongKeyLinkedMap<XLogData>();
	protected String objType;
	
	public String yyyymmdd;
	
	long lastRedraw = 0L;
	
	String[] titles = {"Url", "Err", "Cnt", "Avg", "Sum" };
	
	protected SashForm sashForm;
	protected Action showFilters;
	
	TableViewer viewer;
	XLogSnapshotData selectedItem;
	
	protected Combo objFilter;
	protected Text serviceFilter, ipFilter;
	protected Button onlySqlBtn, onlyApicallBtn, onlyErrorBtn, clearBtn, loadPastBtn, applyBtn, loadBtn;
	protected Display display;
	protected Shell shell;
	protected ToolTip toolTip;
	
	Action onlySqlAction, onlyApicallAction, onlyErrorAction;
	
	
	public void create(Composite parent, IToolBarManager man) {
		onlySqlAction = new Action("Only SQL", IAction.AS_CHECK_BOX) {
			public void run() {
				onlySqlBtn.setSelection(isChecked());
				if (sashForm.getMaximizedControl() == canvas) {
					setFilters();
				}
			}
		};
		onlySqlAction.setImageDescriptor(ImageUtil.getImageDescriptor(Images.database_go));
		man.add(onlySqlAction);
		onlyApicallAction = new Action("Only ApiCall", IAction.AS_CHECK_BOX) {
			public void run() {
				onlyApicallBtn.setSelection(isChecked());
				if (sashForm.getMaximizedControl() == canvas) {
					setFilters();
				}
			}
		};
		onlyApicallAction.setImageDescriptor(ImageUtil.getImageDescriptor(Images.link));
		man.add(onlyApicallAction);
		onlyErrorAction = new Action("Only Error", IAction.AS_CHECK_BOX) {
			public void run() {
				onlyErrorBtn.setSelection(isChecked());
				if (sashForm.getMaximizedControl() == canvas) {
					setFilters();
				}
			}
		};
		onlyErrorAction.setImageDescriptor(ImageUtil.getImageDescriptor(Images.error));
		man.add(onlyErrorAction);
		man.add(new Separator());
		
		// PARENT SASHFORM
		sashForm = new SashForm(parent, SWT.HORIZONTAL);
		sashForm.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_GRAY));
		sashForm.SASH_WIDTH = 1;
		canvas = new Canvas(sashForm, SWT.DOUBLE_BUFFERED);
		canvas.setLayout(new GridLayout());
		// RIGHT SASHFORM
		final SashForm innerSashForm = new SashForm(sashForm, SWT.VERTICAL);
		innerSashForm.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event arg0) {
				int height = innerSashForm.getClientArea().height;
				int[] weights = innerSashForm.getWeights();
				if (height >= MIN_HEIGHT_UP + MIN_HEIGHT_DOWN) {
					weights[0] = 1000000 * MIN_HEIGHT_UP / height;
					weights[1] = 1000000 - weights[0];
				} else {
					weights[0] = 1000000 * MIN_HEIGHT_UP
							/ (MIN_HEIGHT_UP + MIN_HEIGHT_DOWN);
					weights[1] = 1000000 * MIN_HEIGHT_DOWN
							/ (MIN_HEIGHT_UP + MIN_HEIGHT_DOWN);
				}
				innerSashForm.setWeights(weights);
			}
		});
		innerSashForm.SASH_WIDTH = 1;
		innerSashForm.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
		
		TraverseListener enterKeyListener = new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_RETURN) {
					setFilters();
				}
			}
		};
		final Composite filterComp = new Composite(innerSashForm, SWT.NONE);
		filterComp.setLayout(UIUtil.formLayout(5, 5));

		Group filterGroup = new Group(filterComp, SWT.NONE);
	    filterGroup.setText("Filter");
		filterGroup.setLayout(UIUtil.formLayout(5, 5));
		filterGroup.setLayoutData(UIUtil.formData(0, 5, 0, 0, 100, -5, null, -1));

		// FILTER - OBJECT
		Label objLabel = new Label(filterGroup, SWT.NONE);
		objLabel.setText("Object : ");
		objLabel.setLayoutData(UIUtil.formData(null, -1, 0, 10, null, -1, null, -1, 70));		
		
		objFilter = new Combo(filterGroup, SWT.VERTICAL| SWT.BORDER |SWT.H_SCROLL);
	    objFilter.setText("");
	    objFilter.setEnabled(true);
		objFilter.setLayoutData(UIUtil.formData(objLabel, 10, 0, 8, 100, -5, null, -1));
		objFilter.addTraverseListener(enterKeyListener);
		objFilter.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if(viewPainter.objName.equals(objFilter.getText())){
					objFilter.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_BLUE));
				}else{
					objFilter.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
				}
			}
		});

		// FILT ER - SERVICE
		Label serviceLabel = new Label(filterGroup, SWT.NONE);
		serviceLabel.setText("Service : ");
		serviceLabel.setLayoutData(UIUtil.formData(null, -1, 0, 40, null, -1, null, -1, 70));		
		
		serviceFilter = new Text(filterGroup, SWT.BORDER);
		serviceFilter.setEnabled(true);
		serviceFilter.setLayoutData(UIUtil.formData(serviceLabel, 10, 0, 38, 100, -5, null, -1));
		serviceFilter.addTraverseListener(enterKeyListener);
		serviceFilter.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if(viewPainter.service.equals(serviceFilter.getText())){
					serviceFilter.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
				}else{
					serviceFilter.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
				}
			}
		});
		
		// FILTER - IP
		Label ipLabel = new Label(filterGroup, SWT.NONE);
		ipLabel.setText("IP : ");
		ipLabel.setLayoutData(UIUtil.formData(null, -1, 0, 70, null, -1, null, -1, 70));		
		
		ipFilter = new Text(filterGroup, SWT.BORDER);
		ipFilter.setEnabled(true);
		ipFilter.setLayoutData(UIUtil.formData(serviceLabel, 10, 0, 68, 100, -5, null, -1));
		ipFilter.addTraverseListener(enterKeyListener);
		ipFilter.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if(viewPainter.ip.equals(ipFilter.getText())){
					ipFilter.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
				}else{
					ipFilter.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
				}
			}
		});

		Composite buttonComp = new Composite(filterGroup, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
	    gridLayout.numColumns = 6;
	    gridLayout.makeColumnsEqualWidth = true;
	    gridLayout.marginHeight = 0;
	    gridLayout.marginWidth = 0;
	    buttonComp.setLayout(gridLayout);
		buttonComp.setLayoutData(UIUtil.formData(0, 0, 0, 98, 100, -5, null, -1));
	    
		GridData gridData;
		
		onlySqlBtn = new Button(buttonComp, SWT.CHECK);
		onlySqlBtn.setText("SQL");
		onlySqlBtn.setEnabled(true);
		gridData = new GridData(SWT.LEFT, SWT.FILL, true, true);
		gridData.horizontalSpan = 2;
		onlySqlBtn.setLayoutData(gridData);
		onlySqlBtn.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					onlySqlAction.setChecked(onlySqlBtn.getSelection());
					break;
				}
			}
		});
		
		onlyApicallBtn = new Button(buttonComp, SWT.CHECK);
		onlyApicallBtn.setText("ApiCall");
		onlyApicallBtn.setEnabled(true);
		gridData = new GridData(SWT.LEFT, SWT.FILL, true, true);
		gridData.horizontalSpan = 2;
		onlyApicallBtn.setLayoutData(gridData);
		onlyApicallBtn.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					onlyApicallAction.setChecked(onlyApicallBtn.getSelection());
					break;
				}
			}
		});
		
		onlyErrorBtn = new Button(buttonComp, SWT.CHECK);
		onlyErrorBtn.setText("Error");
		onlyErrorBtn.setEnabled(true);
		gridData = new GridData(SWT.LEFT, SWT.FILL, true, true);
		gridData.horizontalSpan = 2;
		onlyErrorBtn.setLayoutData(gridData);
		onlyErrorBtn.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					onlyErrorAction.setChecked(onlyErrorBtn.getSelection());
					break;
				}
			}
		});
		
		clearBtn = new Button(buttonComp, SWT.PUSH);
		clearBtn.setEnabled(true);
		clearBtn.setImage(Images.table_delete);
		clearBtn.setText("Clear");
		gridData = new GridData(GridData.FILL, GridData.FILL, true, true);
		gridData.horizontalSpan = 3;
	    clearBtn.setLayoutData(gridData);
		clearBtn.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					objFilter.setText("");
					serviceFilter.setText("");
					ipFilter.setText("");
					onlySqlBtn.setSelection(false);
					onlyApicallBtn.setSelection(false);
					onlyErrorBtn.setSelection(false);
					onlySqlAction.setChecked(false);
					onlyApicallAction.setChecked(false);
					onlyErrorAction.setChecked(false);
					break;
				}
			}
		});

		loadPastBtn = new Button(buttonComp, SWT.PUSH);
		loadPastBtn.setEnabled(true);
		loadPastBtn.setImage(Images.previous);
		loadPastBtn.setText("Previous");
		gridData = new GridData(GridData.FILL, GridData.FILL, true, true);
		gridData.horizontalSpan = 3;
	    loadPastBtn.setLayoutData(gridData);
		loadPastBtn.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					objFilter.setText(viewPainter.objName);
					serviceFilter.setText(viewPainter.service);
					ipFilter.setText(viewPainter.ip);
					onlySqlBtn.setSelection(viewPainter.onlySql);
					onlyApicallBtn.setSelection(viewPainter.onlyApicall);
					onlyErrorBtn.setSelection(viewPainter.onlyError);
					break;
				}
			}
		});
		
		applyBtn = new Button(buttonComp, SWT.PUSH);
		applyBtn.setEnabled(true);
		applyBtn.setImage(Images.filter);
		applyBtn.setText("Apply Filter");
		gridData =  new GridData(GridData.FILL, GridData.FILL, true, true);
		gridData.horizontalSpan = 6;
	    applyBtn.setLayoutData(gridData);
		applyBtn.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					setFilters();
					break;
				}
			}
		});
		loadBtn = new Button(filterComp, SWT.PUSH);
		loadBtn.setEnabled(true);
		loadBtn.setImage(Images.download);
		loadBtn.setText("Load Transactions ▼");
	    loadBtn.setLayoutData(UIUtil.formData(0, 5, filterGroup, 10, 100, -5, null, -1));
		loadBtn.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					refreshDetailTable();
					break;
				}
			}
		});
				
		Composite tableComp = new Composite(innerSashForm, SWT.NONE);
		viewer = new TableViewer(tableComp, SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		TableColumnLayout layout = new TableColumnLayout();
		tableComp.setLayout( layout );
		createTableViewerColumn(layout, titles[0], 30, SWT.LEFT, false);
		createTableViewerColumn(layout, titles[1], 15, SWT.RIGHT, true);
		createTableViewerColumn(layout, titles[2], 15, SWT.RIGHT, true);
		createTableViewerColumn(layout, titles[3], 18, SWT.RIGHT, true);
		createTableViewerColumn(layout, titles[4], 22, SWT.RIGHT, true);
		viewer.setLabelProvider(new TableItemProvider());
		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setComparator(new TableLabelSorter(viewer));
		table.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				StructuredSelection sel = (StructuredSelection) viewer.getSelection();
				Object o = sel.getFirstElement();
				if (o instanceof XLogSnapshotData) {
					XLogSnapshotData d = (XLogSnapshotData) o;
					TableItem item = (TableItem) event.item;
					if (getCurrentUrlHashValue() == d.urlHash) {
						setUrlHashValue(0);
						d.selected = false;
					} else {
						setUrlHashValue(d.urlHash);
						item.setText(0, TextProxy.service.getLoadText(yyyymmdd, d.urlHash, d.defaultServerId));
						if (selectedItem != null) {
							selectedItem.selected = false;
						}
						d.selected = true;
						selectedItem = d;
					}
					viewer.refresh(true);
					viewPainter.build();
					canvas.redraw();
				}
			}
		});
	    
		sashForm.setWeights(new int[]{2, 1});
		sashForm.setMaximizedControl(canvas);
				
		mouse = new XLogViewMouse(twdata, canvas);
		viewPainter = new XLogViewPainter(twdata, mouse, this);
		viewPainter.set(canvas.getClientArea());
		mouse.setPainter(viewPainter);
		
		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				try {
					viewPainter.drawImageBuffer(e.gc);
					viewPainter.drawSelectArea(e.gc);
				} catch (Throwable t) {
				}
			}
		});
		canvas.addMouseListener(mouse);
		canvas.addMouseMoveListener(mouse);
		canvas.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {
			}
			public void keyPressed(KeyEvent e) {
				final int key = e.keyCode;
				ExUtil.asyncRun(new Runnable() {
					public void run() {
						viewPainter.keyPressed(key);
						viewPainter.build();
						ExUtil.exec(canvas, new Runnable() {
							public void run() {
								canvas.redraw();
							}
						});
					}
				});
			}
		});
		ObjectSelectManager.getInstance().addObjectCheckStateListener(this);
	}
	
	public int getMaxCount() {
		return PManager.getInstance().getInt(PreferenceConstants.P_XLOG_MAX_COUNT);
	}

	public void setObjType(String objType){
		mouse.setObjType(objType);
	}
	
	public void setDate(String yyyymmdd){
		this.yyyymmdd = yyyymmdd;
		viewPainter.yyyymmdd = yyyymmdd;
		mouse.yyyymmdd = yyyymmdd;
	}
	
	public void setUrlHashValue(int hash){
		viewPainter.selectedUrlHash = hash;
		viewPainter.createFilterHash();
	}
	
	public int getCurrentUrlHashValue(){
		return viewPainter.selectedUrlHash;
	}
	
	public void setFilters(){
		final String objName = objFilter.getText();
		final String service = serviceFilter.getText();
		final String ip = ipFilter.getText();
		final boolean onlySql = onlySqlBtn.getSelection();
		final boolean onlyApicall = onlyApicallBtn.getSelection();
		final boolean onlyError = onlyErrorBtn.getSelection();
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				viewPainter.objName = objName;
				viewPainter.service = service;
				viewPainter.ip = ip;
				viewPainter.objNameMat = new StrMatch(objName);
				viewPainter.serviceMat = new StrMatch(service);
				viewPainter.ipMat = new StrMatch(ip);
				viewPainter.onlySql = onlySql;
				viewPainter.onlyApicall = onlyApicall;
				viewPainter.onlyError = onlyError;
				viewPainter.createFilterHash();
				viewPainter.build();
				ExUtil.exec(canvas, new Runnable() {
					public void run() {
						objFilter.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
						serviceFilter.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
						ipFilter.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
						canvas.redraw();
					}
				});
			}
		});
	}
	
	public void clearFilters(){
		viewPainter.objName = "";
		objFilter.setText("");
		viewPainter.service = "";
		serviceFilter.setText("");
		viewPainter.ip = "";
		ipFilter.setText("");
		viewPainter.onlyApicall = false;
		onlyApicallBtn.setSelection(false);
		viewPainter.onlySql = false;
		onlySqlBtn.setSelection(false);
		viewPainter.onlyError = false;
		onlyErrorBtn.setSelection(false);
		viewer.getTable().removeAll();
		viewPainter.selectedUrlHash = 0;
		viewPainter.createFilterHash();
		
		onlySqlAction.setChecked(false);
		onlyApicallAction.setChecked(false);
		onlyErrorAction.setChecked(false);
	}

	public void setFocus() {
	}

	public void dispose() {
		super.dispose();
		ObjectSelectManager.getInstance().removeObjectCheckStateListener(this);
		if (this.viewPainter != null) {
			viewPainter.dispose();
		}
	}
	
	private void createTableViewerColumn(TableColumnLayout tableColumnLayout, String title, int weight, int alignment, final boolean isNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setAlignment(alignment);
		column.setMoveable(true);
		tableColumnLayout.setColumnData(column, new ColumnWeightData(weight, 5, false));
		column.setData("isNumber", isNumber);
		column.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				TableLabelSorter sorter = (TableLabelSorter) viewer.getComparator();
				TableColumn selectedColumn = (TableColumn) e.widget;
				sorter.setColumn(selectedColumn);
			}
		});
	}
	
	protected void settingFilterInputs(){
		if(twdata == null)
			return;
		
		Set<String> objNames = new HashSet<String>();
		
		Enumeration<XLogData> en = twdata.values();
		while (en.hasMoreElements()) {
			XLogData data = en.nextElement();
			String objName = data.objName;
			if (objName == null)
				objName = TextProxy.object.getLoadText(yyyymmdd, data.p.objHash, data.serverId);
			objNames.add(objName);
		}
		// filter setting
		objFilter.setItems(objNames.toArray(new String[objNames.size()]));
	}
	
	static class CountComparator implements Comparator<XLogSnapshotData> {
        public int compare(XLogSnapshotData a, XLogSnapshotData b) {
        	int x1 = a.cnt;
        	int x2 = b.cnt;
        	if (x1 == x2) {
        		return a.sum > b.sum ? -1 : 1;
        	}
        	return x1 > x2 ? -1 : 1;
        }
    }
	
	private void refreshDetailTable(){
    	setUrlHashValue(0);
    	viewPainter.build();
    	settingValuesToTable();
    	canvas.redraw();
	}
	
	ObjectSelectManager objSelMgr = ObjectSelectManager.getInstance();
	
	private void settingValuesToTable() {
		if (twdata.size() < 1) {
			return;
		}

		TreeSet<XLogSnapshotData> sortSet = new TreeSet<XLogSnapshotData>(new CountComparator());
		HashMap<Integer, XLogSnapshotData> serviceMap = new HashMap<Integer, XLogSnapshotData>();
		
		Enumeration<XLogData> en = twdata.values();
		while (en.hasMoreElements()) {
			XLogData data = en.nextElement();
			if (data.filter_ok == false|| objSelMgr.isUnselectedObject(data.p.objHash)) {
				continue;
			}
			int serviceHash = data.p.service;
			XLogSnapshotData snapshot = serviceMap.get(serviceHash);
			if(snapshot != null){
				boolean isErr = data.p.error != 0;
				snapshot.addElapsed(data.p.elapsed, isErr);
			}else{
				serviceMap.put(serviceHash, new XLogSnapshotData(data.p.service, data.p.elapsed, data.p.error!=0, data.serverId));
			}
		}
		// sort
		sortSet.addAll(serviceMap.values());
		
		int rowCnt = (sortSet.size() > DETAIL_TABLE_MAX_ROW)? DETAIL_TABLE_MAX_ROW : sortSet.size();
		int rowNum = 0;
		
		ArrayList<XLogSnapshotData> list = new ArrayList<XLogSnapshotData>();
		for (XLogSnapshotData data : sortSet) {
			if (rowNum >= rowCnt) {
				break;
			}
			list.add(data);
			rowNum++;
		}
		viewer.setInput(list);
	}
	
	class TableItemProvider implements ITableLabelProvider, IColorProvider, IFontProvider {
		public Color getForeground(Object element) {
			if (element instanceof XLogSnapshotData) {
				XLogSnapshotData d = (XLogSnapshotData) element;
				if (d.errorCnt > 0) {
					return ColorUtil.getInstance().getColor("red");
				}
			}
			return null;
		}

		public Color getBackground(Object element) {
			return null;
		}

		public void addListener(ILabelProviderListener listener) {
			
		}

		public void dispose() {
			
		}

		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		public void removeListener(ILabelProviderListener listener) {
		}

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof XLogSnapshotData == false) {
				return null;
			} 
			XLogSnapshotData d = (XLogSnapshotData) element;
			switch (columnIndex) {
				case 0 :
					return TextProxy.service.getText(d.urlHash);
				case 1 :
					return FormatUtil.print(d.errorCnt, "#,##0");
				case 2 :
					return FormatUtil.print(d.cnt, "#,##0");
				case 3 :
					return FormatUtil.print(d.getAvg(), "#,##0");
				case 4 :
					return FormatUtil.print(d.sum, "#,##0");
				}
			return null;
		}

		public Font getFont(Object element) {
			if (element instanceof XLogSnapshotData == false) {
				return null;
			}
			XLogSnapshotData d = (XLogSnapshotData) element;
			if (d.selected) {
				FontData fd = viewer.getControl().getDisplay().getSystemFont().getFontData()[0];
				fd.setStyle(SWT.BOLD);
				return new Font(viewer.getControl().getDisplay(), fd);
			}
			return null;
		}
	}

	public void timeRangeChanged(long time_start, long time_end) {
		XLogData first = twdata.getFirstValue();
		XLogData last = twdata.getLastValue();
		
		if (first == null || last == null) {
			loadAdditinalData(time_start, time_end, false);
		} else if (first.p.endTime > time_end ) {
			loadAdditinalData(time_start, time_end, true);
		} else if (last.p.endTime < time_start) {
			loadAdditinalData(time_start, time_end, false);
		} else {
			long firstTime = first.p.endTime;
			long lastTime = last.p.endTime;
			if (firstTime > time_start && firstTime < time_end) {
				// time_start----------firstTime------------time_end
				loadAdditinalData(time_start, firstTime, true);
			} 
			if (lastTime > time_start && lastTime < time_end) {
				// time_start---------lastTime---------time_end
				loadAdditinalData(lastTime, time_end, false);
			} 
		}
	}
	public abstract void loadAdditinalData(long stime, long etime, boolean reverse);

	public void notifyChangeState() {
		viewPainter.build();
		canvas.redraw();
	}
}
