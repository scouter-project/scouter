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
import java.util.Date;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import scouter.client.model.TextProxy;
import scouter.client.model.XLogData;
import scouter.client.sorter.TableLabelSorter;
import scouter.client.util.ColorUtil;
import scouter.client.xlog.actions.OpenXLogProfileJob;
import scouter.lang.pack.Pack;
import scouter.lang.pack.XLogPack;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;
import scouter.util.Hexa32;
import scouter.util.IPUtil;

public class ServiceTableComposite extends Composite {
	
	Composite parent;
	
	private TableViewer viewer;
	private TableColumnLayout tableColumnLayout;
	
	int serverId;
	String yyyymmdd;

	public ServiceTableComposite(Composite parent, int style) {
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
				if (o instanceof XLogPack) {
					XLogPack data = (XLogPack) o;
					XLogData d = new XLogData(data, serverId);
					d.objName = TextProxy.object.getLoadText(yyyymmdd, data.objHash, serverId);
					d.serviceName = TextProxy.service.getLoadText(yyyymmdd, data.service, serverId);
					new OpenXLogProfileJob(ServiceTableComposite.this.getDisplay(), d, serverId).schedule();
				} else {
					System.out.println(o);
				}
			}
		});
	}
	
	public void setInput(ArrayList<Pack> packList, int serverId, String date) {
		this.serverId = serverId;
		this.yyyymmdd = date;
		ArrayList<Integer> serverHashes = new ArrayList<Integer>(packList.size());
		for (Pack p : packList) {
			XLogPack xp = (XLogPack) p;
			serverHashes.add(xp.service);
		}
		TextProxy.service.load(yyyymmdd, serverHashes, serverId);
		viewer.setInput(packList);
	}
	
	ArrayList<XLogColumnEnum> columnList = new ArrayList<XLogColumnEnum>();
	
	private void createColumns() {
		for (XLogColumnEnum column : XLogColumnEnum.values()) {
			createTableViewerColumn(column.getTitle(), column.getWidth(), column.getAlignment(), column.isResizable(), column.isMoveable(), column.isNumber());
			columnList.add(column);
		}
		viewer.setLabelProvider(new TableItemProvider());
	}
	
	class TableItemProvider implements ITableLabelProvider, IColorProvider {

		public Color getForeground(Object element) {
			if (element instanceof XLogPack) {
				XLogPack d = (XLogPack) element;
				if (d.error != 0) {
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
			if (element instanceof XLogPack == false) {
				return null;
			} 
			XLogPack p = (XLogPack) element;
			XLogColumnEnum column = columnList.get(columnIndex);
			switch (column) {
			case OBJECT :
				return TextProxy.object.getLoadText(yyyymmdd, p.objHash, serverId);
			case ELAPSED :
				return FormatUtil.print(p.elapsed, "#,##0");
			case SERVICE :
				return TextProxy.service.getLoadText(yyyymmdd, p.service, serverId);
			case START_TIME :
				return FormatUtil.print(new Date(p.endTime - p.elapsed), "HH:mm:ss.SSS");
			case END_TIME :
				return FormatUtil.print(new Date(p.endTime), "HH:mm:ss.SSS");
			case TX_ID :
				return Hexa32.toString32(p.txid);
			case CPU :
				return FormatUtil.print(p.cpu, "#,##0");
			case SQL_COUNT :
				return FormatUtil.print(p.sqlCount, "#,##0");
			case SQL_TIME :
					return FormatUtil.print(p.sqlTime, "#,##0");
			case KBYTES :
					return FormatUtil.print(p.kbytes, "#,##0");
			case IP :
					return IPUtil.toString(p.ipaddr);
			case ERROR :
				return p.error == 0 ? "" : TextProxy.error.getLoadText(yyyymmdd, p.error, serverId);
			case GX_ID :
				return Hexa32.toString32(p.gxid);
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
	
	enum XLogColumnEnum {
	    OBJECT("Object", 150, SWT.LEFT, true, true, false),
	    ELAPSED("Elapsed", 50, SWT.RIGHT, true, true, true),
	    SERVICE("Service", 150, SWT.LEFT, true, true, false),
	    START_TIME("StartTime", 100, SWT.CENTER, true, true, true),
	    END_TIME("EndTime", 100, SWT.CENTER, true, true, true),
	    TX_ID("Txid", 30, SWT.LEFT, true, true, false),
	    CPU("Cpu", 50, SWT.RIGHT, true, true, true),
	    SQL_COUNT("SQL Count", 50, SWT.RIGHT, true, true, true),
	    SQL_TIME("SQL Time", 50, SWT.RIGHT, true, true, true),
	    KBYTES("KBytes", 50, SWT.RIGHT, true, true, true),
	    IP("IP", 100, SWT.LEFT, true, true, false),
	    ERROR("Error", 50, SWT.LEFT, true, true, false),
	    GX_ID("Gxid", 30, SWT.LEFT, true, true, false);

	    private final String title;
	    private final int weight;
	    private final int alignment;
	    private final boolean resizable;
	    private final boolean moveable;
	    private final boolean isNumber;

	    private XLogColumnEnum(String text, int width, int alignment, boolean resizable, boolean moveable, boolean isNumber) {
	        this.title = text;
	        this.weight = width;
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
