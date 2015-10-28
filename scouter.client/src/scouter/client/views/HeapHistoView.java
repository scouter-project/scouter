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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.model.TextProxy;
import scouter.client.net.TcpProxy;
import scouter.client.sorter.ColumnLabelSorter;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.ScouterUtil;
import scouter.client.util.TimeUtil;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.StringUtil;

public class HeapHistoView extends ViewPart {
	
	public final static String ID = HeapHistoView.class.getName();
	
	private TableViewer viewer;
	private TableColumnLayout tableColumnLayout;
	
	private Clipboard clipboard;
	
	private int objHash;
	private int serverId;

	private String secondId;
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		secondId = site.getSecondaryId();
	}
	
	public void createPartControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		composite.setLayout(new GridLayout(1, true));
		createTableViewer(composite);
		clipboard = new Clipboard(null);
	}
	
	public void setInput(int serverId){
		this.serverId = serverId;
		
		if (secondId != null) {
			String[] tokens = StringUtil.tokenizer(secondId, "&");
			this.objHash = CastUtil.cint(tokens[0]);
			long time = CastUtil.clong(tokens[1]);
			this.setPartName("HeapHistogram[" + TextProxy.object.getLoadText(DateUtil.yyyymmdd(TimeUtil.getCurrentTime(serverId)), objHash, serverId) + "][" + DateUtil.format(time, "HH:mm:ss") + "]");
		}
		
		load();
	}
	
	boolean ctrlPressed = false;
	
	private void createTableContextMenu() {
		MenuManager manager = new MenuManager();
		viewer.getControl().setMenu(manager.createContextMenu(viewer.getControl()));
	    manager.add(new Action("&Copy", ImageDescriptor.createFromImage(Images.copy)) {
			public void run() {
				selectionCopyToClipboard();
			}
	    });
	    viewer.getTable().addListener(SWT.KeyDown, new Listener() {
			public void handleEvent(Event e) {
				if (e.keyCode == SWT.CTRL) {
					ctrlPressed = true;
				} else if (e.keyCode == 'c' || e.keyCode == 'C') {
					if (ctrlPressed) {
						selectionCopyToClipboard();
					}
				}
			}
		});
	    
	    viewer.getTable().addListener(SWT.KeyUp, new Listener() {
			public void handleEvent(Event e) {
				if (e.keyCode == SWT.CTRL) {
					ctrlPressed = false;
				} 
			}
		});
	}
	
	private void selectionCopyToClipboard() {
		if (viewer != null) {
			TableItem[] items = viewer.getTable().getSelection();
			if (items != null && items.length > 0) {
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < items.length; i++) {
					HeapHistoData data = (HeapHistoData) items[i].getData();
					sb.append(data.toString());
				}
				clipboard.setContents(new Object[] {sb.toString()}, new Transfer[] {TextTransfer.getInstance()});
			}
		}
	}
	
	private void createTableViewer(Composite composite) {
		viewer = new TableViewer(composite, SWT.MULTI  | SWT.FULL_SELECTION | SWT.BORDER);
		tableColumnLayout = new TableColumnLayout();
		composite.setLayout(tableColumnLayout);
		createColumns();
		final Table table = viewer.getTable();
	    table.setHeaderVisible(true);
	    table.setLinesVisible(true);
	    createTableContextMenu();
	    viewer.setContentProvider(new ArrayContentProvider());
	    viewer.setComparator(new ColumnLabelSorter(viewer));
	    GridData gridData = new GridData(GridData.FILL, GridData.FILL, true, true);
	    viewer.getControl().setLayoutData(gridData);
	}
	
	private void createColumns() {
		for (HeapHistoColumnEnum column : HeapHistoColumnEnum.values()) {
			TableViewerColumn c = createTableViewerColumn(column.getTitle(), column.getWidth(), column.getAlignment(), column.isResizable(), column.isMoveable(), column.isNumber());
			ColumnLabelProvider labelProvider = null;
			switch (column) {
				case NO:
					labelProvider = new ColumnLabelProvider() {
						@Override
						public String getText(Object element) {
							if (element instanceof HeapHistoData) {
								return String.valueOf(((HeapHistoData)element).no);
							}
							return null;
						}
					};
					break;
				case COUNT:
					labelProvider = new ColumnLabelProvider() {
						@Override
						public String getText(Object element) {
							if (element instanceof HeapHistoData) {
								return String.valueOf(((HeapHistoData)element).count);
							}
							return null;
						}
					};
					break;
				case SIZE:
					labelProvider = new ColumnLabelProvider() {
						@Override
						public String getText(Object element) {
							if (element instanceof HeapHistoData) {
								return String.format("%,d", ((HeapHistoData)element).size);
							}
							return null;
						}
					};
					break;
				case CLASSNAME:
					labelProvider = new ColumnLabelProvider() {
						@Override
						public String getText(Object element) {
							if (element instanceof HeapHistoData) {
								return ((HeapHistoData)element).name;
							}
							return null;
						}
					};
					break;
				}
			if (labelProvider != null) {
				c.setLabelProvider(labelProvider);
			}
		}
	}
	
	private TableViewerColumn createTableViewerColumn(String title, int width, int alignment,  boolean resizable, boolean moveable, final boolean isNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setAlignment(alignment);
		column.setMoveable(moveable);
		tableColumnLayout.setColumnData(column, new ColumnWeightData(width, width, resizable));
		column.setData("isNumber", isNumber);
		column.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ColumnLabelSorter sorter = (ColumnLabelSorter) viewer.getComparator();
				TableColumn selectedColumn = (TableColumn) e.widget;
				sorter.setColumn(selectedColumn);
			}
		});
		return viewerColumn;
	}
	
	public void setFocus() {
	}
	
	public void load() {
		ExUtil.asyncRun(new Runnable() {
			
			public void run() {
				MapPack out = null;
				final List<HeapHistoData> datas = new ArrayList<HeapHistoData>();
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				try {
					MapPack param = new MapPack();
					param.put("objHash", objHash);
					out = (MapPack) tcp.getSingle(RequestCmd.OBJECT_HEAPHISTO, param);
					if (out == null) {
						return;
					}
					String error = out.getText("error");
					if (error != null) {
						ConsoleProxy.errorSafe(error);
					}
					ListValue lv = out.getList("heaphisto");
					if (lv == null) {
						return;
					}
					for (int i = 0; i < lv.size(); i++) {
						HeapHistoData data = new HeapHistoData();
						String[] tokens = StringUtil.tokenizer(lv.getString(i)," ");
						if (tokens == null || tokens.length < 4) {
							continue;
						}
						String index = removeNotDigit(tokens[0]);
						data.no = CastUtil.cint(index);
						data.count = CastUtil.cint(tokens[1]);
						data.size = CastUtil.clong(tokens[2]);
						data.name = getCanonicalName(tokens[3]);
						datas.add(data);
					}
				} catch(Exception e){
					ConsoleProxy.errorSafe(e.toString());
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				ExUtil.exec(viewer.getTable(), new Runnable() {
					public void run() {
						viewer.setInput(datas);
					}
				});
			}
		});
	}
	
	private static String getCanonicalName(String className) {
		if (StringUtil.isEmpty(className)) {
			return className;
		}
		int arrayCnt = 0;
		boolean prefix = true;
		char[] arr = className.toCharArray();
		int offset = 0;
		StringBuilder sb = new StringBuilder();
		for (; offset < arr.length && prefix; offset++) {
			if (arr[offset] == '[') {
				arrayCnt++;
				continue;
			} else if(offset == 0 || arr[offset-1] == '[') {
				if ('L' == arr[offset]) {
					if (className.endsWith(";")) {
						sb.append(className.substring(offset + 1, className.length() - 1));
					} else {
						sb.append(className.substring(offset + 1));
					}
				} else if ('V' == arr[offset]) {
					sb.append("void");
				} else if ('Z' == arr[offset]) {
					sb.append("boolean");
				} else if ('C' == arr[offset]) {
					sb.append("char");
				} else if ('B' == arr[offset]) {
					sb.append("byte");
				} else if ('S' == arr[offset]) {
					sb.append("short");
				} else if ('I' == arr[offset]) {
					sb.append("int");
				} else if ('F' == arr[offset]) {
					sb.append("float");
				} else if ('J' == arr[offset]) {
					sb.append("long");
				} else if ('D' == arr[offset]) {
					sb.append("double");
				} else {
					sb.append(className);
				}
				prefix = false;
			}
		}
		while(arrayCnt > 0) {
			sb.append("[]");
			arrayCnt--;
		}
		return sb.toString();
	}
	
	private static String removeNotDigit(String name) {
		StringBuffer sb = new StringBuffer();
		char[] charArray = name.toCharArray();
		for (int i = 0; i < charArray.length; i++) {
			if (Character.isDigit(charArray[i])) {
				sb.append(charArray[i]);
			}
		}
		return sb.toString();
	}
	
	enum HeapHistoColumnEnum {

		NO("No", 50, SWT.RIGHT, true, true, true),
	    COUNT("Count", 100, SWT.RIGHT, true, true, true),
	    SIZE("Size", 150, SWT.RIGHT, true, true, true),
	    CLASSNAME("ClassName", 250, SWT.LEFT, true, true, false);

	    private final String title;
	    private final int width;
	    private final int alignment;
	    private final boolean resizable;
	    private final boolean moveable;
	    private final boolean isNumber;

	    private HeapHistoColumnEnum(String text, int width, int alignment, boolean resizable, boolean moveable, boolean isNumber) {
	        this.title = text;
	        this.width = width;
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
			return width;
		}
		
		public boolean isNumber() {
			return this.isNumber;
		}
	}

	class HeapHistoData {
		public int no;
		public int count;
		public long size;
		public String name;
		
		public String toString() {
			return no + "\t" + count + "\t" + size + "\t" + name + "\n";
		}
	}
}
