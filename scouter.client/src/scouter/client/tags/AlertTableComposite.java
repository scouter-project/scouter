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
 */
package scouter.client.tags;

import java.util.ArrayList;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import scouter.client.model.TextProxy;
import scouter.client.popup.AlertNotifierDialog;
import scouter.client.sorter.TableLabelSorter;
import scouter.lang.AlertLevel;
import scouter.lang.pack.AlertPack;
import scouter.lang.pack.Pack;
import scouter.util.DateUtil;

public class AlertTableComposite extends Composite {

	Composite parent;

	private TableViewer viewer;
	private TableColumnLayout tableColumnLayout;

	int serverId;
	String yyyymmdd;

	public AlertTableComposite(Composite parent, int style) {
		super(parent, style);
		this.parent = this;
		initLayout();
	}
	
	private void initLayout() {
		parent.setLayout(new FillLayout());
		Composite comp = new Composite(parent, SWT.NONE);
		tableColumnLayout = new TableColumnLayout();
		comp.setLayout(tableColumnLayout);
		viewer = new TableViewer(comp, SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		createColumns();
		final Table table = viewer.getTable();
	    table.setHeaderVisible(true);
	    table.setLinesVisible(true);
	    viewer.setContentProvider(new ArrayContentProvider());
	    viewer.setComparator(new TableLabelSorter(viewer));
	    viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent e) {
				StructuredSelection sel = (StructuredSelection) viewer.getSelection();
				Object o = sel.getFirstElement();
				if (o instanceof AlertPack) {
					String objName = TextProxy.object.getLoadText(yyyymmdd, ((AlertPack) o).objHash, serverId);
					AlertNotifierDialog alertDialog = new AlertNotifierDialog(parent.getDisplay(), serverId);
					alertDialog.setObjName(objName);
					alertDialog.setPack((AlertPack) o);
					alertDialog.show(parent.getBounds());
				} else {
					System.out.println(o);
				}
			}
		});
	}
	
	public void setInput(ArrayList<Pack> packList, int serverId, String date) {
		this.serverId = serverId;
		this.yyyymmdd = date;
		viewer.setInput(packList);
	}
	
	ArrayList<AlertColumnEnum> columnList = new ArrayList<AlertColumnEnum>();
	
	private void createColumns() {
		for (AlertColumnEnum column : AlertColumnEnum.values()) {
			createTableViewerColumn(column.getTitle(), column.getWidth(), column.getAlignment(), column.isResizable(), column.isMoveable(), column.isNumber());
			columnList.add(column);
		}
		viewer.setLabelProvider(new TableItemProvider());
	}
	
	class TableItemProvider implements ITableLabelProvider {

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
			if (element instanceof AlertPack == false) {
				return null;
			} 
			AlertPack p = (AlertPack) element;
			AlertColumnEnum column = columnList.get(columnIndex);
			switch (column) {
			case TIME :
				return DateUtil.format(p.time, "HH:mm:ss.SSS");
			case LEVEL :
				return AlertLevel.getName(p.level);
			case OBJECT :
				return TextProxy.object.getLoadText(yyyymmdd, p.objHash, serverId);
			case TITLE :
				return p.title;
			case MESSAGE :
				return p.message;
			case TAGS :
				return p.tags.toString();
			}
			return null;
		}
	}
	
	private TableViewerColumn createTableViewerColumn(String title, int width, int alignment,  boolean resizable, boolean moveable, final boolean isNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setAlignment(alignment);
		column.setMoveable(moveable);
		tableColumnLayout.setColumnData(column, new ColumnPixelData(width, resizable));
		column.setData("isNumber", isNumber);
		column.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				TableLabelSorter sorter = (TableLabelSorter) viewer.getComparator();
				TableColumn selectedColumn = (TableColumn) e.widget;
				sorter.setColumn(selectedColumn);
			}
		});
		return viewerColumn;
	}
	
	enum AlertColumnEnum {
	    TIME("TIME", 100, SWT.RIGHT, true, true, true), //
	    LEVEL("LEVEL", 70, SWT.CENTER, true, true, false), //
	    OBJECT("OBJECT", 100, SWT.LEFT, true, true, false),
	    TITLE("TITLE", 150, SWT.LEFT, true, true, false),
	    MESSAGE("MESSAGE", 250, SWT.LEFT, true, true, false),
	    TAGS("TAGs", 200, SWT.LEFT, true, true, false);

	    private final String title;
	    private final int weight;
	    private final int alignment;
	    private final boolean resizable;
	    private final boolean moveable;
	    private final boolean isNumber;

	    private AlertColumnEnum(String text, int weight, int alignment, boolean resizable, boolean moveable, boolean isNumber) {
	        this.title = text;
	        this.weight = weight;
	        this.alignment = alignment;
	        this.resizable = resizable;
	        this.moveable = moveable;
	        this.isNumber = isNumber;
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
			return weight;
		}
		
		public boolean isNumber() {
			return this.isNumber;
		}
	}
}
