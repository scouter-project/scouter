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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.cubrid.ActiveDbInfo;
import scouter.client.cubrid.actions.AddLongTransactionList;
import scouter.client.model.AgentModelThread;
import scouter.client.model.RefreshThread;
import scouter.client.model.RefreshThread.Refreshable;
import scouter.client.net.TcpProxy;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
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

public class CubridSpaceDbView extends ViewPart implements Refreshable {
	public static final String ID = CubridSpaceDbView.class.getName();

	static long TIME_RANGE = DateUtil.MILLIS_PER_FIVE_MINUTE;
	static int REFRESH_INTERVAL = (int) (DateUtil.MILLIS_PER_SECOND * 5);

	int serverId;

	private Composite tableComposite;
	private Table table;
	private TableItem tableItem;
	
	private ArrayList<DbSpace> dbSpaceData = new ArrayList<>();
	private Composite composite;
	
	Combo dbListCombo;
	boolean isDefaultView = false;

	RefreshThread thread;

	String date;
	long stime, etime;

	MapValue prvData;
	
	String selectionDB;
	int prvActiveDBHash;
	
    private static final String DBSPACE_PERM_AND_PERM_TOTAL = "perm_and_perm_total";
    private static final String DBSPACE_PERM_AND_PERM_USED = "perm_and_perm_used";
    private static final String DBSPACE_PERM_AND_TEMP_TOTAL = "perm_and_temp_total";
    private static final String DBSPACE_PERM_AND_TEMP_USED = "perm_and_temp_used";
    private static final String DBSPACE_TEMP_AND_TEMP_TOTAL = "temp_and_temp_total";
    private static final String DBSPACE_TEMP_AND_TEMP_USED = "temp_and_temp_used";

	@Override
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String ids[] = secId.split("&");
		serverId = CastUtil.cint(ids[0]);
		selectionDB = CastUtil.cString(ids[1]);
		
		if (selectionDB.equals("default")) {
			isDefaultView = true;
		}
		
		makeDbSpaceData();
	}

	@Override
	public void createPartControl(Composite parent) {
		Server server = ServerManager.getInstance().getServer(serverId);
		if (server != null) {
			this.setPartName("CUBRID DBSpaceInfo" + "[" + server.getName() + "]");
		} else {
			this.setPartName("CUBRID DBSpaceInfo");
		}
		composite = parent;
		GridLayout layout = new GridLayout(3, true);
		parent.setLayout(layout);
		dbListCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
		
		if (!isDefaultView) {
			dbListCombo.add(selectionDB);
			dbListCombo.select(0);
			dbListCombo.setEnabled(false);
		}
		
		dbListCombo.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				ExUtil.exec(composite, new Runnable() {
					@Override
					public void run() {
						selectionDB = dbListCombo.getItem(dbListCombo.getSelectionIndex());
						thread.interrupt();
					}
				});

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {

			}
		});

		tableComposite = new Composite(composite, SWT.NONE);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 3;
		data.verticalSpan = 3;
		tableComposite.setLayoutData(data);
		tableComposite.setLayout(new FillLayout());
		
		table = new Table(tableComposite, SWT.BORDER);
	    table.setHeaderVisible(true);
	    table.setLinesVisible(false);
	    createContextMenu();
	    
	    for (int i = 0; i < ColumnEnum.values().length; i++) {
	      new TableColumn(table, SWT.NONE);
	      table.getColumn(i).setText(ColumnEnum.values()[i].getTitle());
	    }
	    
		tableItem = new TableItem(table, SWT.NONE);
		tableItem.setText(ListTypeEnum.TYPE1.getTypeArray());
		tableItem = new TableItem(table, SWT.NONE);
		tableItem.setText(ListTypeEnum.TYPE2.getTypeArray());
		tableItem = new TableItem(table, SWT.NONE);
		tableItem.setText(ListTypeEnum.TYPE3.getTypeArray());

		table.addListener(SWT.PaintItem, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (event.index == 2) {
					//System.out.println("handleEvent : " + event);
					GC gc = event.gc;
					int percent = 0;
					int total = 0;
					int used = 0;
					TableItem item = (TableItem)event.item;
					//System.out.println("dbSpaceData.size() : " + dbSpaceData.size());
					for (int i=0; i < dbSpaceData.size(); i++) {
						//System.out.println("dbSpaceData.get(i) : " + dbSpaceData.get(i).total);
						//System.out.println("dbSpaceData.get(i) : " + dbSpaceData.get(i).used);
						if (item.getText(0).equals(dbSpaceData.get(i).listType.getPurpose()) 
								&& item.getText(1).equals(dbSpaceData.get(i).listType.getType())) {
							percent = dbSpaceData.get(i).getPercent();
							total = dbSpaceData.get(i).getTotal();
							used = dbSpaceData.get(i).getUsed();
						}
					}
					Color foreground = gc.getForeground();
					Color background = gc.getBackground();
					if (percent < 90) {
						gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN));
						gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
					} else {
						gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED));
						gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
					}
					int width = (table.getColumn(event.index).getWidth() - 1) * percent / 100;
					gc.fillGradientRectangle(event.x, event.y, width, event.height, true);
					Rectangle rect2 = new Rectangle(event.x, event.y, width-1, event.height-1);
					gc.drawRectangle(rect2);
					gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_FOREGROUND));
					String text = percent+"%" + " ( " + used + "MB" + " / " + total + "MB )";
					Point size = event.gc.textExtent(text);
					int offset = Math.max(0, (event.height - size.y) / 2);
					gc.drawText(text, event.x+5, event.y+offset, true);
					gc.setForeground(background);
					gc.setBackground(foreground);
				}
			}
		});
		
//		table.addListener(SWT.Resize, new Listener() {
//			
//			@Override
//			public void handleEvent(Event event) {
//				
//			}
//		});
		
		for (int i = 0; i < ColumnEnum.values().length; i++) {
			table.getColumn(i).pack();
			table.getColumn(i).setToolTipText("aaaa");
			table.getColumn(2).setWidth(table.getBounds().width - 
					table.getColumn(0).getWidth() - table.getColumn(1).getWidth() - 2);
		}

		thread = new RefreshThread(this, REFRESH_INTERVAL);
		thread.start();
	}

	private void createContextMenu() {
		MenuManager manager = new MenuManager();
		table.setMenu(manager.createContextMenu(table));
		manager.add(new Action("&Add DBSpaceInfo", ImageDescriptor.createFromImage(Images.add)) {
			public void run() {
				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				AddLongTransactionList dialog = new AddLongTransactionList(window.getShell().getDisplay(), serverId,
						new AddLongTransactionList.IAddLongTransactionList() {
							@Override
							public void onPressedOk(String dbName) {
								try {
									window.getActivePage().showView(CubridSpaceDbView.ID,
											serverId + "&" + dbName,
											IWorkbenchPage.VIEW_ACTIVATE);
								} catch (PartInitException e) {
									e.printStackTrace();
								}
							}

							@Override
							public void onPressedCancel() {
							}
						});

				dialog.show();
			}
	    });
	}
	
	@Override
	public void refresh() {

		ExUtil.exec(table, new Runnable() {
			public void run() {
		}
	});

		long now = TimeUtil.getCurrentTime(serverId);
		date = DateUtil.yyyymmdd(now);
		stime = now - TIME_RANGE;
		etime = now;

		if (isDefaultView) {
			checkDBList();
		}
		
		if (selectionDB == null || selectionDB.isEmpty()) {
			return;
		}

		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		Pack p = null;

		try {
			MapPack param = new MapPack();
			ListValue objHashLv = AgentModelThread.getInstance().getLiveObjHashLV(serverId,
					CounterConstants.CUBRID_AGENT);
			StatusPack sp = null;
			MapValue mv = null;

			if (objHashLv.size() > 0) {
				param.put("objHash", objHashLv);
				param.put("date", date);
				param.put("stime", stime);
				param.put("etime", etime);
				param.put("time", now);
				param.put("key", StatusConstants.CUBRID_DB_SERVER_INFO + selectionDB);
				p = tcp.getSingle(RequestCmd.CUBRID_DB_SERVER_INFO, param);
				if (p != null) {
					sp = (StatusPack) p;
					mv = sp.data;
				}

				if (mv != null) {
					dbSpaceData.get(ListTypeEnum.TYPE1.ordinal()).total = Integer.parseInt(mv.getText(DBSPACE_PERM_AND_PERM_TOTAL));
					dbSpaceData.get(ListTypeEnum.TYPE1.ordinal()).used = Integer.parseInt(mv.getText(DBSPACE_PERM_AND_PERM_USED));
					dbSpaceData.get(ListTypeEnum.TYPE2.ordinal()).total = Integer.parseInt(mv.getText(DBSPACE_PERM_AND_TEMP_TOTAL));
					dbSpaceData.get(ListTypeEnum.TYPE2.ordinal()).used = Integer.parseInt(mv.getText(DBSPACE_PERM_AND_TEMP_USED));
					dbSpaceData.get(ListTypeEnum.TYPE3.ordinal()).total = Integer.parseInt(mv.getText(DBSPACE_TEMP_AND_TEMP_TOTAL));
					dbSpaceData.get(ListTypeEnum.TYPE3.ordinal()).used = Integer.parseInt(mv.getText(DBSPACE_TEMP_AND_TEMP_USED));
				}
			}
		} catch (Throwable th) {
			th.printStackTrace();
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		
		//table update
		ExUtil.exec(table, new Runnable() {
			public void run() {
				table.setSize(table.getSize().x, table.getSize().y + 1);
				table.setSize(table.getSize().x, table.getSize().y - 1);
				table.getColumn(2).setWidth(table.getBounds().width - 
						table.getColumn(0).getWidth() - table.getColumn(1).getWidth() - 4);
		}
		});
		
	}

	public void checkDBList() {

		if (ActiveDbInfo.getInstance().getActiveDBInfo(serverId) == null) {
			return;
		}
		
		if (ActiveDbInfo.getInstance().getActiveDBInfo(serverId).hashCode() == prvActiveDBHash) {
			return;
		}

		prvActiveDBHash = ActiveDbInfo.getInstance().getActiveDBInfo(serverId).hashCode();

		ExUtil.exec(composite, new Runnable() {
			public void run() {
				dbListCombo.removeAll();
			}
		});

		if (!ActiveDbInfo.getInstance().isEmpty(serverId)) {
			ExUtil.exec(composite, new Runnable() {
				public void run() {
					for (String dbName : ActiveDbInfo.getInstance().keySet(serverId)) {
						dbListCombo.add(dbName);
					}
					dbListCombo.setEnabled(true);
				}
			});

		} else {
			ExUtil.exec(composite, new Runnable() {
				public void run() {
					dbListCombo.setEnabled(false);
				}
			});
		}

		ExUtil.exec(composite, new Runnable() {
			public void run() {
				for (int i = 0; i < dbListCombo.getItemCount(); i++) {
					if (dbListCombo.getItem(i).equals(selectionDB)) {
						dbListCombo.select(i);
						return;
					}
				}

				if (dbListCombo.getItemCount() != 0) {
					dbListCombo.select(0);
					selectionDB = dbListCombo.getItem(dbListCombo.getSelectionIndex());
				}
			}
		});
	}

	enum ColumnEnum {
		PURPOSE("Purpose"),
		TYPE("Type"),
		FREE_PERCENTAGE("Free");

		private final String title;

		private ColumnEnum(String text) {
			this.title = text;
		}

		public String getTitle() {
			return title;
		}
	}

	enum ListTypeEnum {
		TYPE1("PEMANENT", "PEMANENT"),
		TYPE2("PEMANENT", "TEMPORARY"),
		TYPE3("TEMPORARY", "TEMPORARY");

		private final String purpose;
		private final String type;

		private ListTypeEnum(String purpose, String type) {
			this.purpose = purpose;
			this.type = type;
		}

		public String getPurpose() {
			return purpose;
		}
		
		public String getType() {
			return type;
		}
		
		public String[] getTypeArray() {
			return new String[] {purpose, type};
		}
		
	}
	
	class DbSpace {
		ListTypeEnum listType;
		int total;
		int used;
		
		public DbSpace(ListTypeEnum listType, int total, int used) {
			this.listType = listType;
			this.total = total;
			this.used = used;
			
		}
		
		public String getPurpose() {
			return listType.getPurpose();
		}
		
		public String getType() {
			return listType.getType();
		}
		
		public int getTotal() {
			return total;
		}
		
		public int getUsed() {
			return used;
		}
		
		public void setTotal(int value) {
			this.total = value;
		}
		
		public void setUsed(int value) {
			this.used = value;
		}
		
		public int getPercent() {
			if (used != 0 && total != 0) {
				return (int) ((float)used / total * 100);
			}
			return 0;
		}
		
		public String getString() {
			return "listType purpose : " + listType.getPurpose() 
					+ " type : " + listType.getType() 
					+ " total : " + Integer.toString(total) 
					+ " used : " + Integer.toString(used)
					+ " percent : " + getPercent();
		}
		
	}
	
	private void makeDbSpaceData() {
		DbSpace type1 = new DbSpace(ListTypeEnum.TYPE1, 0, 0);
		DbSpace type2 = new DbSpace(ListTypeEnum.TYPE2, 0, 0);
		DbSpace type3 = new DbSpace(ListTypeEnum.TYPE3, 0, 0);
		dbSpaceData.add(type1);
		dbSpaceData.add(type2);
		dbSpaceData.add(type3);
	}
	
	@Override
	public void setFocus() {

	}

}


