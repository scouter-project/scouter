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
package scouter.client.views;

import java.util.ArrayList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.net.TcpProxy;
import scouter.client.server.ServerManager;
import scouter.client.sorter.ColumnLabelSorter;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;

public class LoginUserView extends ViewPart {
	
	public final static String ID = LoginUserView.class.getName();
	
	private int serverId;
	
	private TableViewer viewer;
	private TableColumnLayout tableColumnLayout;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		this.serverId = Integer.valueOf(site.getSecondaryId());
	}

	public void createPartControl(Composite parent) {
		this.setPartName("Login List[" + ServerManager.getInstance().getServer(serverId).getName() + "]");
		tableColumnLayout = new TableColumnLayout();
		parent.setLayout(tableColumnLayout);
		viewer = new TableViewer(parent, SWT.FULL_SELECTION | SWT.BORDER);
		createColumns();
		final Table table = viewer.getTable();
	    table.setHeaderVisible(true);
	    table.setLinesVisible(true);
	    viewer.setContentProvider(new ArrayContentProvider());
	    viewer.setComparator(new ColumnLabelSorter(viewer));
	    IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
	    man.add(new Action("Reload", ImageUtil.getImageDescriptor(Images.refresh)) {
			public void run() {
				load();
			}
		});
	    load();
	}
	
	public void setFocus() {
		
	}
	
	private void load() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				MapPack p = null;
				try {
					p = (MapPack) tcp.getSingle(RequestCmd.GET_LOGIN_LIST, null);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				final ArrayList<UserData> dataList = new ArrayList<UserData>();
				if (p != null) {
					ListValue sessionLv = p.getList("session");
					ListValue userLv = p.getList("user");
					ListValue ipLv = p.getList("ip");
					ListValue timeLv = p.getList("logintime");
					ListValue verLv = p.getList("ver");
					ListValue hostLv = p.getList("host");
					for (int i = 0; i < sessionLv.size(); i++) {
						UserData data = new UserData();
						dataList.add(data);
						data.session = sessionLv.getLong(i);
						data.user = userLv.getString(i);
						data.ip = ipLv.getString(i);
						data.logintime = timeLv.getLong(i);
						data.version = verLv.getString(i);
						data.host = hostLv.getString(i);
					}
				}
				ExUtil.exec(viewer.getTable(), new Runnable() {
					public void run() {
						viewer.setInput(dataList);
					}
				});
			}
		});
	}
	
	private void createColumns() {
		for (UserEnum column : UserEnum.values()) {
			TableViewerColumn c = createTableViewerColumn(column.getTitle(), column.getWidth(), column.getAlignment(), column.isResizable(), column.isMoveable(), column.isNumber());
			ColumnLabelProvider labelProvider = null;
			switch (column) {
			case USER :
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof UserData) {
							return ((UserData)element).user;
						}
						return null;
					}
				};
				break;
			case IP :
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof UserData) {
							return ((UserData)element).ip;
						}
						return null;
					}
				};
				break;
			case LOGINTIME :
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof UserData) {
							return String.valueOf(((UserData)element).logintime);
						}
						return null;
					}
				};
				break;
			case VERSION :
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof UserData) {
							return ((UserData)element).version;
						}
						return null;
					}
				};
				break;
			case HOST :
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof UserData) {
							return ((UserData)element).host;
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
		tableColumnLayout.setColumnData(column, new ColumnWeightData(width, 10, resizable));
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
	
	enum UserEnum {
	    USER("User", 50, SWT.CENTER, true, true, false), //
	    IP("Ip", 50, SWT.LEFT, true, true, false), //
	    LOGINTIME("Login Time(Sec)", 50, SWT.RIGHT, true, true, true),
	    VERSION("Ver.", 50, SWT.LEFT, true, true, false),
	    HOST("Host", 50, SWT.LEFT, true, true, false);

	    private final String title;
	    private final int width;
	    private final int alignment;
	    private final boolean resizable;
	    private final boolean moveable;
	    private final boolean isNumber;

	    private UserEnum(String text, int width, int alignment, boolean resizable, boolean moveable, boolean isNumber) {
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
	
	static class UserData {
		public long session;
		public String user;
		public String ip;
		public long logintime;
		public String version;
		public String host;
	}
}
