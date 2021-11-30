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
package scouter.client.cubrid.views;

import java.util.ArrayList;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import scouter.client.cubrid.ActiveDbInfo;
import scouter.client.model.AgentModelThread;
import scouter.client.model.RefreshThread;
import scouter.client.model.RefreshThread.Refreshable;
import scouter.client.net.TcpProxy;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.sorter.TableLabelSorter;
import scouter.client.util.ExUtil;
import scouter.client.util.TimeUtil;
import scouter.lang.constants.StatusConstants;
import scouter.lang.counters.CounterConstants;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.pack.StatusPack;
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;

public class CubridServerInfoView extends ViewPart implements Refreshable {
	public static final String ID = CubridServerInfoView.class.getName();

	static long TIME_RANGE = DateUtil.MILLIS_PER_FIVE_MINUTE;
	static int REFRESH_INTERVAL = (int) (DateUtil.MILLIS_PER_SECOND * 5);

	int serverId;

	private ArrayList<ServerInfo> dataList;
	private ArrayList<MapValue> responseData;
	private TableViewer tableViewer;
	private TableColumnLayout tableColumnLayout;

	RefreshThread thread;

	String date;
	long stime, etime;

	MapValue prvData;

	@Override
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		serverId = CastUtil.cint(secId);
		dataList = new ArrayList<>();
		responseData = new ArrayList<>();
	}

	@Override
	public void createPartControl(Composite parent) {
		Server server = ServerManager.getInstance().getServer(serverId);
		ActiveDbInfo activeDBList = ActiveDbInfo.getInstance();
		
		if (server != null) {
			this.setPartName("CUBRID ServerInfo" + "[" + server.getName() + "]");
			activeDBList.addServerInfo(serverId);
		} else {
			this.setPartName("CUBRID ServerInfo");
		}

		Composite tableComposite = new Composite(parent, SWT.NONE);
		tableColumnLayout = new TableColumnLayout();
		tableComposite.setLayout(tableColumnLayout);

		tableViewer = new TableViewer(tableComposite, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER);
		tableViewer.setComparator(new TableLabelSorter(tableViewer));
		tableViewer.setUseHashlookup(true);
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.getTable().setHeaderVisible(true);

		createColumns();

		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.setLabelProvider(new ITableLabelProvider() {
			@Override
			public void removeListener(ILabelProviderListener arg0) {
			}

			@Override
			public boolean isLabelProperty(Object arg0, String arg1) {
				return false;
			}

			@Override
			public void dispose() {
			}

			@Override
			public void addListener(ILabelProviderListener arg0) {
			}

			@Override
			public String getColumnText(Object arg0, int arg1) {
				ServerInfo info = (ServerInfo) arg0;
				switch (arg1) {
				case 0:
					return info.dbName;
				case 1:
					return info.ipAddress;
				case 2:
					return info.cpuUsed + "%";
				case 3:
					return info.activeSession;
				case 4:
					return info.lockWaitSession;
				}
				return "";
			}

			@Override
			public Image getColumnImage(Object arg0, int arg1) {
				return null;
			}

		});

		createTableContextMenu();

		thread = new RefreshThread(this, REFRESH_INTERVAL);
		thread.start();
	}

	private void createTableContextMenu() {
		/*
		 * MenuManager manager = new MenuManager();
		 * tableViewer.getControl().setMenu(manager.createContextMenu(
		 * tableViewer.getControl())); manager.add(new
		 * AddRealTimeMultiViewAction(serverId, MultiViewType.DATABASE_IO,
		 * "Add View"));
		 */
	}

	private void createColumns() {
		for (ColumnEnum column : ColumnEnum.values()) {
			createTableViewerColumn(column.getTitle(), column.getWidth(), column.getAlignment(), column.isResizable(),
					column.isMoveable(), column.isNumber());
		}
	}

	private TableViewerColumn createTableViewerColumn(String title, int width, int alignment, boolean resizable,
			boolean moveable, final boolean isNumber) {
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

	private class ServerInfo {

		String dbName;
		String ipAddress;
		String cpuUsed;
		String activeSession;
		String lockWaitSession;

		public ServerInfo(String dbName, String ip, String cpu, String active, String lockWait) {
			super();
			this.dbName = dbName;
			this.ipAddress = ip;
			this.cpuUsed = cpu;
			this.activeSession = active;
			this.lockWaitSession = lockWait;
		}
	}

	@Override
	public void refresh() {

		ActiveDbInfo activeDBList = ActiveDbInfo.getInstance();

		long now = TimeUtil.getCurrentTime(serverId);
		date = DateUtil.yyyymmdd(now);
		stime = now - TIME_RANGE;
		etime = now;

		if (!getDBList()) {
			dataList.clear();
			ExUtil.exec(tableViewer.getTable(), new Runnable() {
				public void run() {
					if (tableViewer != null) {
						tableViewer.setInput(dataList);
					}
				}
			});
			return;
		}

		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		Pack p = null;

		try {
			responseData.clear();
			MapPack param = new MapPack();
			ListValue objHashLv = AgentModelThread.getInstance().getLiveObjHashLV(serverId,
					CounterConstants.CUBRID_AGENT);
			StatusPack sp = null;
			MapValue mv = null;

			if (objHashLv.size() > 0) {
				if (activeDBList.isEmpty(serverId)) {
					dataList.clear();
				}

				for (String dbName : activeDBList.keySet(serverId)) {
					param.put("objHash", objHashLv);
					param.put("date", date);
					param.put("stime", stime);
					param.put("etime", etime);
					param.put("time", now);
					param.put("key", StatusConstants.CUBRID_DB_SERVER_INFO + dbName);
					p = tcp.getSingle(RequestCmd.CUBRID_DB_SERVER_INFO, param);
					if (p != null) {
						sp = (StatusPack) p;
						mv = sp.data;
						responseData.add(mv);
					} else {
						System.out.println("CubridServerInfoView p is null");
					}
				}
			}
		} catch (Throwable th) {
			th.printStackTrace();
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}

		String dbName;
		String ipAddress;
		String cpuUsed;
		String activeSession;
		String lockWaitSession;
		boolean isSearched = false;

		for (int i = 0; i < responseData.size(); i++) {
			isSearched = false;
			dbName = responseData.get(i).getText("db_name");
			ipAddress = responseData.get(i).getText("ip_address");
			cpuUsed = responseData.get(i).getText("cpu_used");
			activeSession = responseData.get(i).getText("active_session");
			lockWaitSession = responseData.get(i).getText("lock_wait_sessions");
			for (int j = 0; j < dataList.size(); j++) {
				if (dataList.get(j).dbName.equals(dbName)) {
					dataList.get(j).ipAddress = ipAddress;
					dataList.get(j).cpuUsed = cpuUsed;
					dataList.get(j).activeSession = activeSession;
					dataList.get(j).lockWaitSession = lockWaitSession;
					isSearched = true;
					break;
				}
			}
			if (!isSearched) {
				dataList.add(new ServerInfo(responseData.get(i).getText("db_name"),
						responseData.get(i).getText("ip_address"), 
						responseData.get(i).getText("cpu_used"),
						responseData.get(i).getText("active_session"),
						responseData.get(i).getText("lock_wait_sessions")));
			}
		}

		ExUtil.exec(tableViewer.getTable(), new Runnable() {
			public void run() {
				if (tableViewer != null) {
					tableViewer.setInput(dataList);
				}
			}
		});
	}

	private boolean getDBList() {
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		Pack p = null;
		ActiveDbInfo activeDBList = ActiveDbInfo.getInstance();
		try {
			MapPack param = new MapPack();
			ListValue objHashLv = AgentModelThread.getInstance().getLiveObjHashLV(serverId,
					CounterConstants.CUBRID_AGENT);
			if (objHashLv.size() > 0) {
				param.put("objHash", objHashLv);
				param.put("key", StatusConstants.CUBRID_ACTIVE_DB_LIST);
				param.put("date", date);
				param.put("time", stime);
				p = tcp.getSingle(RequestCmd.CUBRID_ACTIVE_DB_LIST, param);
			}

			if (p != null) {
				StatusPack sp = (StatusPack) p;
				MapValue mv = (MapValue) sp.data;

				if (mv == null || mv.isEmpty()) {
					if (prvData != null) {
						prvData.clear();
					}
					return false;
				}

				if (prvData != null && prvData.equals(mv)) {
					return true;
				}

				prvData = mv;
				activeDBList.clear(serverId);
				for (String key : mv.keySet()) {
					activeDBList.put(serverId, key, String.valueOf(mv.get(key)));
				}
			} else {

				if (prvData != null)
					prvData.clear();

				activeDBList.clear(serverId);
				return false;
			}

		} catch (Throwable th) {
			th.printStackTrace();
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return true;
	}

	enum ColumnEnum {
		DB_NAME("DB NAME", 25, SWT.CENTER, true, true, false),
		IP_ADDRESS("IP ADDRESS", 40, SWT.CENTER, true, true, false),
		CPU_USED("CPU", 20, SWT.CENTER, true, true, true),
		ACTIVE_SESSION("ACTIVE SESSION", 40, SWT.CENTER, true, true, true),
		LOCK_WAIT_SESSION("LOCK WAIT SESSIONS", 45, SWT.CENTER, true, true, true);

		private final String title;
	    private final int width;
	    private final int alignment;
	    private final boolean resizable;
	    private final boolean moveable;
	    private final boolean isNumber;

		private ColumnEnum(String text, int width, int alignment, boolean resizable, boolean moveable,
				boolean isNumber) {
			this.title = text;
			this.width = width;
			this.alignment = alignment;
			this.resizable = resizable;
			this.moveable = moveable;
			this.isNumber = isNumber;
		}

		public String getTitle() {
			return title;
		}

		public int getAlignment() {
			return alignment;
		}

		public boolean isResizable() {
			return resizable;
		}

		public boolean isMoveable() {
			return moveable;
		}

		public int getWidth() {
			return width;
		}

		public boolean isNumber() {
			return this.isNumber;
		}
	}

	@Override
	public void setFocus() {

	}

}