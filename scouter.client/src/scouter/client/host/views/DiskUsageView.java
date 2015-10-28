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
package scouter.client.host.views;

import java.util.ArrayList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProvider;
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
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.model.TextProxy;
import scouter.client.net.TcpProxy;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.sorter.ColumnLabelSorter;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.ScouterUtil;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.FormatUtil;

public class DiskUsageView extends ViewPart {
	
	public static final String ID = DiskUsageView.class.getName();
	
	private TableViewer viewer;
	private TableColumnLayout tableColumnLayout;
	
	private Clipboard clipboard;
	private int objHash;
	private int serverId;
	
	@Override
	public void createPartControl(Composite parent) {
		initialLayout(parent);
		clipboard = new Clipboard(null);
	}

	private void initialLayout(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, true));
		Composite tableComposite = new Composite(composite, SWT.NONE);
		tableComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		tableComposite.setLayout(new GridLayout(1, true));
		createTableViewer(tableComposite);
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("Reload", ImageUtil.getImageDescriptor(Images.refresh)) {
			public void run() {
				load();
			}
		});
	}

	public void setInput(int serverId, int objHash){
		this.serverId = serverId;
		this.objHash = objHash;
		this.setPartName("Disk Usage[" + TextProxy.object.getText(objHash) + "]");
		Server server = ServerManager.getInstance().getServer(serverId);
		String serverName = null;
		if (server != null) {
			serverName = server.getName();
		}
		this.setContentDescription("ⓢ" + serverName + "|" + TextProxy.object.getText(objHash));
		load();
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
	    viewer.setComparator(new ColumnLabelSorter(viewer).setCustomCompare(new ColumnLabelSorter.ICustomCompare() {
			public int doCompare(TableColumn col, int index, Object o1, Object o2) {
				ILabelProvider labelProvider = (ILabelProvider) viewer.getLabelProvider(index);
				String t1 = labelProvider.getText(o1);
				String t2 = labelProvider.getText(o2);
				Boolean isNumber = (Boolean) col.getData("isNumber");
				if (isNumber != null && isNumber.booleanValue()) {
					String number1 = ColumnLabelSorter.numonly(t1);
					String number2 = ColumnLabelSorter.numonly(t2);
					double n1 = CastUtil.cdouble(number1);
					double n2 = CastUtil.cdouble(number2);
					if (t1.endsWith("G")) {
						n1 *= Math.pow(1024, 3);
					} else if (t1.endsWith("M")) {
						n1 *= Math.pow(1024, 2);
					} else if (t1.endsWith("K")) {
						n1 *= Math.pow(1024, 1);
					}
					if (t2.endsWith("G")) {
						n2 *= Math.pow(1024, 3);
					} else if (t2.endsWith("M")) {
						n2 *= Math.pow(1024, 2);
					} else if (t2.endsWith("K")) {
						n2 *= Math.pow(1024, 1);
					}
					if (n1 == n2) {
						return 0;
					} else {
						return (n1 > n2) ? 1 : -1;
					}
				} else {
					if (t1 == null) t1 = "";
					if (t2 == null) t2 = "";
				}
				return t1.compareTo(t2);
			}
		}));
	    GridData gridData = new GridData(GridData.FILL, GridData.FILL, true, true);
	    viewer.getControl().setLayoutData(gridData);
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
					DiskData data = (DiskData) items[i].getData();
					sb.append(data.toString());
				}
				clipboard.setContents(new Object[] {sb.toString()}, new Transfer[] {TextTransfer.getInstance()});
			}
		}
	}
	
	private void load() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				MapPack pack = null;
				try {
					MapPack param = new MapPack();
					param.put("objHash", objHash);
					pack = (MapPack) tcp.getSingle(RequestCmd.HOST_DISK_USAGE, param);
				} catch (Throwable t) {
					ConsoleProxy.errorSafe(t.toString());
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				if (pack != null) {
					ListValue deviceList = pack.getList("Device");
					ListValue totalList = pack.getList("Total");
					ListValue usedList = pack.getList("Used");
					ListValue freeList = pack.getList("Free");
					ListValue pctList = pack.getList("Pct");
					ListValue typeList = pack.getList("Type");
					ListValue mountList = pack.getList("Mount");
					final ArrayList<DiskData> diskList = new ArrayList<DiskData>();
					if (deviceList != null && deviceList.size() > 0) {
						for (int i = 0; i < deviceList.size(); i++) {
							DiskData data = new DiskData();
							diskList.add(data);
							data.device = deviceList.getString(i);
							data.total = totalList.getLong(i);
							data.used = usedList.getLong(i);
							data.free = freeList.getLong(i);
							data.pct = (float)pctList.getDouble(i);
							data.type = typeList.getString(i);
							data.mount = mountList.getString(i);
						}
					}
					ExUtil.exec(viewer.getTable(), new Runnable() {
						public void run() {
							viewer.setInput(diskList);
						}
					});
				}
			}
		});
	}

	private void createColumns() {
		for (DiskColumnEnum column : DiskColumnEnum.values()) {
			TableViewerColumn c = createTableViewerColumn(column.getTitle(), column.getWidth(), column.getAlignment(), column.isResizable(), column.isMoveable(), column.isNumber());
			ColumnLabelProvider labelProvider = null;
			switch (column) {
			case DEVICE:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof DiskData) {
							return ((DiskData) element).device;
						}
						return null;
					}
				};
				break;
			case TOTAL:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof DiskData) {
							return FormatUtil.printMem(((DiskData) element).total);
						}
						return null;
					}
				};
				break;
			case USED:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof DiskData) {
							return FormatUtil.printMem(((DiskData) element).used);
						}
						return null;
					}
				};
				break;
			case FREE:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof DiskData) {
							return FormatUtil.printMem(((DiskData) element).free);
						}
						return null;
					}
				};
				break;
			case PCT:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof DiskData) {
							return FormatUtil.print(((DiskData) element).pct,"#0.0")+"%";
						}
						return null;
					}
				};
				break;
			case TYPE:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof DiskData) {
							return ((DiskData) element).type;
						}
						return null;
					}
				};
				break;
			case MOUNT:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof DiskData) {
							return ((DiskData) element).mount;
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
		tableColumnLayout.setColumnData(column, new ColumnWeightData(30, width, resizable));
		column.setData("isNumber", isNumber);
		column.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ColumnLabelSorter sorter = (ColumnLabelSorter) viewer.getComparator();
				sorter.setColumn(column);
			}
		});
		return viewerColumn;
	}

	@Override
	public void setFocus() {
	}

	class DiskData {
		public String device;
		public long total;
		public long used;
		public long free;
		public float pct;
		public String type;
		public String mount;
		
		public String toString() {
			return device + "\t" + total	+ "\t" + used + "\t" + free + "\t" + pct + "\t" + type + "\t" + mount + "\n";
		}
	}
	
	public enum DiskColumnEnum {

	    DEVICE("Device", 100, SWT.LEFT, true, true, false), //
	    TOTAL("Total", 70, SWT.RIGHT, true, true, true), //
	    USED("Used", 70, SWT.RIGHT, true, true, true),
	    FREE("Free", 70, SWT.RIGHT, true, true, true),
	    PCT("PCT", 50, SWT.CENTER, true, true, true),
	    TYPE("Type", 70, SWT.CENTER, true, true, false),
	    MOUNT("Mount", 80, SWT.LEFT, true, true, false);

	    private final String title;
	    private final int width;
	    private final int alignment;
	    private final boolean resizable;
	    private final boolean moveable;
	    private final boolean isNumber;

	    private DiskColumnEnum(String text, int width, int alignment, boolean resizable, boolean moveable, boolean isNumber) {
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
}
