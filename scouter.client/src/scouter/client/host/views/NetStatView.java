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

public class NetStatView extends ViewPart {
	
	public static final String ID = NetStatView.class.getName();
	
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
		this.setPartName("Net Stat[" + TextProxy.object.getText(objHash) + "]");
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
	    viewer.setComparator(new ColumnLabelSorter(viewer));
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
					NetStatData data = (NetStatData) items[i].getData();
					sb.append(data.toString());
				}
				clipboard.setContents(new Object[] {sb.toString()}, new Transfer[] {TextTransfer.getInstance()});
			}
		}
	}
	
	private void load() {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		MapPack pack = null;
		try {
			MapPack param = new MapPack();
			param.put("objHash", objHash);
			pack = (MapPack) tcp.getSingle(RequestCmd.HOST_NET_STAT, param);
		} catch (Throwable t) {
			ConsoleProxy.errorSafe(t.toString());
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		if (pack != null) {
			ListValue protoList = pack.getList("Proto");
			ListValue localAddrList = pack.getList("LocalAddr");
			ListValue remoteAddrList = pack.getList("RemoteAddr");
			ListValue statusList = pack.getList("Status");
			ListValue pidList = pack.getList("Pid");
			ListValue nameList = pack.getList("ProgramName");
			final ArrayList<NetStatData> diskList = new ArrayList<NetStatData>();
			if (protoList != null && protoList.size() > 0) {
				for (int i = 0; i < protoList.size(); i++) {
					NetStatData data = new NetStatData();
					diskList.add(data);
					data.proto = protoList.getString(i);
					data.localAddr = localAddrList.getString(i);
					data.remoteAddr = remoteAddrList.getString(i);
					data.status = statusList.getString(i);
					data.pid = pidList.getString(i);
					data.programName = nameList.getString(i);
				}
			}
			ExUtil.exec(viewer.getTable(), new Runnable() {
				public void run() {
					viewer.setInput(diskList);
				}
			});
		}
	}

	private void createColumns() {
		for (DiskColumnEnum column : DiskColumnEnum.values()) {
			TableViewerColumn c = createTableViewerColumn(column.getTitle(), column.getWidth(), column.getAlignment(), column.isResizable(), column.isMoveable(), column.isNumber());
			ColumnLabelProvider labelProvider = null;
			switch (column) {
			case PROTO:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof NetStatData) {
							return ((NetStatData) element).proto;
						}
						return null;
					}
				};
				break;
			case LOCAL_ADDR:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof NetStatData) {
							return ((NetStatData) element).localAddr;
						}
						return null;
					}
				};
				break;
			case REMOTE_ADDR:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof NetStatData) {
							return ((NetStatData) element).remoteAddr;
						}
						return null;
					}
				};
				break;
			case STATUS:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof NetStatData) {
							return ((NetStatData) element).status;
						}
						return null;
					}
				};
				break;
			case PID:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof NetStatData) {
							return ((NetStatData) element).pid;
						}
						return null;
					}
				};
				break;
			case PROGRAM_NAME:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof NetStatData) {
							return ((NetStatData) element).programName;
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
				TableColumn selectedColumn = (TableColumn) e.widget;
				sorter.setColumn(selectedColumn);
			}
		});
		return viewerColumn;
	}

	@Override
	public void setFocus() {
	}

	class NetStatData {
		public String proto;
		public String localAddr;
		public String remoteAddr;
		public String status;
		public String pid;
		public String programName;
		
		public String toString() {
			return proto + "\t" + localAddr	+ "\t" + remoteAddr + "\t" + status + "\t" + pid + "\t" + programName + "\n";
		}
	}
	
	public enum DiskColumnEnum {

	    PROTO("Proto", 50, SWT.LEFT, true, true, false), //
	    LOCAL_ADDR("Local Addr", 150, SWT.LEFT, true, true, false), //
	    REMOTE_ADDR("Remote Addr", 150, SWT.LEFT, true, true, false),
	    STATUS("Status", 100, SWT.CENTER, true, true, false),
	    PID("PID", 50, SWT.CENTER, true, true, true),
	    PROGRAM_NAME("Program Name", 100, SWT.LEFT, true, true, false);

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
