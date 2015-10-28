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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
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
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.model.TextProxy;
import scouter.client.net.TcpProxy;
import scouter.client.sorter.ColumnLabelSorter;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;

public class TopView extends ViewPart {
	
	public static final String ID = TopView.class.getName();
	
	private TableViewer viewer;
	private TableColumnLayout tableColumnLayout;
	
	private Clipboard clipboard;
	private int serverId;
	private int objHash;
	
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
		this.setPartName("Top[" + TextProxy.object.getText(objHash) + "]");
		load();
	}
	
	ProcessObject[] procList;
	
	private void load() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				TcpProxy tcpProxy = TcpProxy.getTcpProxy(serverId);
				try {
					MapPack param = new MapPack();
					param.put("objHash", objHash);
					MapPack pack = (MapPack) tcpProxy.getSingle(RequestCmd.HOST_TOP, param);
					if (pack == null) return;
					String error = pack.getText("error");
					if (error != null) {
						ConsoleProxy.errorSafe(error);
					}
					ListValue pidLv = pack.getList("PID");
					ListValue userLv = pack.getList("USER");
					ListValue cpuLv = pack.getList("CPU");
					ListValue memLv = pack.getList("MEM");
					ListValue timeLv = pack.getList("TIME");
					ListValue nameLv = pack.getList("NAME");
					
					procList = new ProcessObject[pidLv.size()];
					for (int i = 0; i < pidLv.size(); i++) {
						procList[i] = new ProcessObject();
						procList[i].pid = (int) pidLv.getLong(i);
						procList[i].user = userLv.getString(i);
						procList[i].cpu = (float) cpuLv.getDouble(i);
						procList[i].mem =  memLv.getLong(i);
						procList[i].time = timeLv.getLong(i);
						procList[i].name = nameLv.getString(i);
					}
					ExUtil.exec(viewer.getTable(), new Runnable() {
						public void run() {
							viewer.setInput(procList);
						}
					});
				} catch (Throwable th){
					th.printStackTrace();
				} finally {
					TcpProxy.putTcpProxy(tcpProxy);
				}
			}
		});
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
//	    table.addMouseListener(new MouseAdapter() {
//			public void mouseDoubleClick(MouseEvent e) {
//				TableItem[] item = table.getSelection();
//				if (item == null || item.length == 0)
//					return;
//				int pid = CastUtil.cint(item[0].getText(0));
//				IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
//				try {
//					ProcessDetailView view = (ProcessDetailView) win.getActivePage().showView(
//							ProcessDetailView.ID, "" + pid + objHash, IWorkbenchPage.VIEW_ACTIVATE);
//					view.setInput(serverId, objHash, pid);
//				} catch (PartInitException e1) {}
//			}
//		});
	    viewer.setContentProvider(new ArrayContentProvider());
	    viewer.setComparator(new ColumnLabelSorter(viewer).setCustomCompare(new ColumnLabelSorter.ICustomCompare() {
			public int doCompare(TableColumn col, int index, Object o1, Object o2) {
				if (!(o1 instanceof ProcessObject) || !(o2 instanceof ProcessObject)) {
					return 0;
				}
				ProcessObject p1 = (ProcessObject) o1;
				ProcessObject p2 = (ProcessObject) o2;
				Boolean isNumber = (Boolean) col.getData("isNumber");
				if (isNumber != null && isNumber.booleanValue()) {
					String v1 = ColumnLabelSorter.numonly(p1.getValueByIndex(index));
					String v2 = ColumnLabelSorter.numonly(p2.getValueByIndex(index));
					if (v1 == null) v1 = "0";
					if (v2 == null) v2 = "0";
					if (v1.contains(".") || v2.contains(".")) {
						double d1 = Double.valueOf(v1);
						double d2 = Double.valueOf(v2);
						if (d1 > d2) {
							return 1;
						} else if (d2 > d1) {
							return -1;
						} else {
							return 0;
						}
					} else {
						long i1 = Long.valueOf(v1);
						long i2 = Long.valueOf(v2);
						if (i1 > i2) {
							return 1;
						} else if (i2 > i1) {
							return -1;
						} else {
							return 0;
						}
					}
				} else {
					String v1 = p1.getValueByIndex(index);
					String v2 = p2.getValueByIndex(index);
					if (v1 == null) v1 = "";
					if (v2 == null) v2 = "";
					return v1.compareTo(v2);
				}
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
					ProcessObject data = (ProcessObject) items[i].getData();
					sb.append(data.toString());
				}
				clipboard.setContents(new Object[] {sb.toString()}, new Transfer[] {TextTransfer.getInstance()});
			}
		}
	}

	private void createColumns() {
		for (ColumnEnum column : ColumnEnum.values()) {
			TableViewerColumn c = createTableViewerColumn(column.getTitle(), column.getWidth(), column.getAlignment(), column.isResizable(), column.isMoveable(), column.isNumber());
			ColumnLabelProvider labelProvider = null;
			switch (column) {
			case PID:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof ProcessObject) {
							return String.valueOf(((ProcessObject) element).pid);
						}
						return null;
					}
				};
				break;
			case USER:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof ProcessObject) {
							return ((ProcessObject) element).user;
						}
						return null;
					}
				};
				break;
			case CPU:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof ProcessObject) {
							return FormatUtil.print(((ProcessObject) element).cpu, "#,##0.0")+"%";
						}
						return null;
					}
				};
				break;
			case MEM:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof ProcessObject) {
							return FormatUtil.printMem(((ProcessObject) element).mem);
						}
						return null;
					}
				};
				break;
			case TIME:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof ProcessObject) {
							return DateUtil.format(((ProcessObject) element).time, "mm:ss.SSS");
						}
						return null;
					}
				};
				break;
			case NAME:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof ProcessObject) {
							return ((ProcessObject) element).name;
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

	class ProcessObject {
		int pid;
		String user;
		float cpu;
		long mem;
		long time;
		String name;
		
		public String getValueByIndex(int index) {
			switch (index) {
			case 0:
				return String.valueOf(pid);
			case 1:
				return user;
			case 2:
				return String.valueOf(cpu);
			case 3:
				return String.valueOf(mem);
			case 4:
				return String.valueOf(time);
			case 5:
				return name;
			}
			return null;
		}

		public String toString() {
			return pid + "\t" + user   + "\t" + + cpu + "\t" + mem + "\t" + DateUtil.format(time, "mm:ss.SSS") + "\t" + name + "\n"; 
		}
	}
	
	public enum ColumnEnum {

	    PID("PID", 50, SWT.RIGHT, true, true, true), //
	    USER("USER", 70, SWT.RIGHT, true, true, false), //
	    CPU("CPU%", 50, SWT.RIGHT, true, true, true),
	    MEM("MEM", 50, SWT.RIGHT, true, true, true),
	    TIME("TIME", 100, SWT.RIGHT, true, true, true),
	    NAME("NAME", 150, SWT.LEFT, true, true, false);

	    private final String title;
	    private final int width;
	    private final int alignment;
	    private final boolean resizable;
	    private final boolean moveable;
	    private final boolean isNumber;

	    private ColumnEnum(String text, int width, int alignment, boolean resizable, boolean moveable, boolean isNumber) {
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
