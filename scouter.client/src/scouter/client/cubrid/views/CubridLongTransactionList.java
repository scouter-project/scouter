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
import java.util.LinkedHashMap;
import java.util.Map;

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
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
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
import scouter.client.sorter.ColumnLabelSorter;
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

public class CubridLongTransactionList extends ViewPart implements Refreshable {
	
	public static final String ID = CubridLongTransactionList.class.getName();
	
	static long TIME_RANGE = DateUtil.MILLIS_PER_FIVE_MINUTE;
	static int REFRESH_INTERVAL = (int) (DateUtil.MILLIS_PER_SECOND * 5);
	
	String[] maxListArray = {"10","20","50","100","1000"};
	
	static int DEFAULT_MAX_LIST = 1000;
	
	private Combo dbListCombo;
	private Label MaxLabel;
	private Combo MaxListCombo;
	private int MaxListValue = 50;
	
	private TableViewer viewer;
	private TableColumnLayout tableColumnLayout;
	
	private Clipboard clipboard;
	private int serverId;
	
	RefreshThread thread;

	String date;
	long stime, etime;
	
	Map<String, TransactionObject> saveData;
	
	ArrayList<TransactionObject> transactionList = new ArrayList<>();

	String selectionDB = "";
	boolean isDefaultView = false;
	int lastListIndex = 0;
	int prvActiveDBHash = -1;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String ids[] = secId.split("&");
		serverId = CastUtil.cint(ids[0]);
		selectionDB = CastUtil.cString(ids[1]);
		
		if (selectionDB.equals("default")) {
			isDefaultView = true;
		}
		
		saveData = new LinkedHashMap<String, TransactionObject>() {
			private static final long serialVersionUID = 1L;

			@Override
		    protected boolean removeEldestEntry(Map.Entry<String,TransactionObject> eldest) {
		        return size() > DEFAULT_MAX_LIST;
		    }
		};
	}
	
	@Override
	public void createPartControl(Composite parent) {
		initialLayout(parent);
		clipboard = new Clipboard(null);
	}

	private void initialLayout(Composite parent) {
		Server server = ServerManager.getInstance().getServer(serverId);
		
		if (server != null) {
			this.setPartName("Long Transaction List[" + server.getName() + "]");
		} else {
			this.setPartName("Long Transaction List");
		}
		parent.setLayout(new GridLayout(2, true));
		
		dbListCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		if (!isDefaultView) {
			dbListCombo.add(selectionDB);
			dbListCombo.select(0);
			dbListCombo.setEnabled(false);
		}
		
		dbListCombo.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				ExUtil.exec(parent, new Runnable() {
					@Override
					public void run() {
						selectionDB = dbListCombo.getText();
						saveData.clear();
						transactionList.clear();
						viewer.setInput(transactionList.toArray());
						thread.interrupt();
					}
				});

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {

			}
		});
		
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, true));
		GridData gdata = new GridData();
		gdata.horizontalAlignment = GridData.END;
		gdata.verticalAlignment = GridData.CENTER;
		composite.setLayoutData(gdata);
		
		MaxLabel = new Label(composite, SWT.NONE);
		MaxLabel.setText("MaxList : ");
		MaxLabel.setLayoutData(new GridData(SWT.CENTER, SWT.RIGHT, false, false));
		MaxListCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
        ArrayList<String> valueStrList = new ArrayList<String>();
        for (int i = 0; i < maxListArray.length ; i++) {
        	valueStrList.add(maxListArray[i]);
        }
        MaxListCombo.setItems(valueStrList.toArray(new String[maxListArray.length]));
        MaxListCombo.select(2);
        MaxListCombo.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, false, false));
        
        MaxListCombo.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				MaxListValue = Integer.parseInt(maxListArray[MaxListCombo.getSelectionIndex()]);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				
			}
		});
		
		Composite tableComposite = new Composite(parent, SWT.NONE);
		GridData gdataTable = new GridData(GridData.FILL_BOTH);
		gdataTable.horizontalSpan = 2;
		tableComposite.setLayoutData(gdataTable);
		tableComposite.setLayout(new GridLayout(1, true));
		createTableViewer(tableComposite);
		
		thread = new RefreshThread(this, REFRESH_INTERVAL);
		thread.start();
	}

//	private void load() {
//		ExUtil.asyncRun(new Runnable() {
//			public void run() {
//				TcpProxy tcpProxy = TcpProxy.getTcpProxy(serverId);
//				try {
//					MapPack param = new MapPack();
//					param.put("objHash", objHash);
//					MapPack pack = (MapPack) tcpProxy.getSingle(RequestCmd.HOST_TOP, param);
//					if (pack == null) return;
//					String error = pack.getText("error");
//					if (error != null) {
//						ConsoleProxy.errorSafe(error);
//					}
//					ListValue pidLv = pack.getList("PID");
//					ListValue userLv = pack.getList("USER");
//					ListValue cpuLv = pack.getList("CPU");
//					ListValue memLv = pack.getList("MEM");
//					ListValue timeLv = pack.getList("TIME");
//					ListValue nameLv = pack.getList("NAME");
//					
//					transactionList = new TransactionObject[pidLv.size()];
//					for (int i = 0; i < pidLv.size(); i++) {
//						transactionList[lastListIndex] = new TransactionObject();
//						transactionList[lastListIndex].sql_text = (int) pidLv.getLong(i);
//						transactionList[lastListIndex].user = userLv.getString(i);
//						transactionList[lastListIndex].sql_id = (float) cpuLv.getDouble(i);
//						transactionList[lastListIndex].host =  memLv.getLong(i);
//						transactionList[lastListIndex].pid = timeLv.getLong(i);
//						transactionList[lastListIndex].program = nameLv.getString(i);
//						transactionList[lastListIndex].tran_time = nameLv.getString(i);
//						transactionList[lastListIndex].query_time = nameLv.getString(i);
//					}
//					ExUtil.exec(viewer.getTable(), new Runnable() {
//						public void run() {
//							viewer.setInput(transactionList);
//						}
//					});
//				} catch (Throwable th){
//					th.printStackTrace();
//				} finally {
//					TcpProxy.putTcpProxy(tcpProxy);
//				}
//			}
//		});
//	}
	
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
				if (!(o1 instanceof TransactionObject) || !(o2 instanceof TransactionObject)) {
					return 0;
				}
				TransactionObject p1 = (TransactionObject) o1;
				TransactionObject p2 = (TransactionObject) o2;
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
		manager.add(new Action("&Add LongTransaction ListView", ImageDescriptor.createFromImage(Images.add)) {
			public void run() {
				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				AddLongTransactionList dialog = new AddLongTransactionList(window.getShell().getDisplay(), serverId,
						new AddLongTransactionList.IAddLongTransactionList() {
							@Override
							public void onPressedOk(String dbName) {
								try {
									window.getActivePage().showView(CubridLongTransactionList.ID,
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
					TransactionObject data = (TransactionObject) items[i].getData();
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
			case SQL_TEXT:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof TransactionObject) {
							return ((TransactionObject) element).sql_text;
						}
						return null;
					}
				};
				break;
			case USER:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof TransactionObject) {
							return ((TransactionObject) element).user;
						}
						return null;
					}
				};
				break;
			case SQL_ID:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof TransactionObject) {
							return ((TransactionObject) element).sql_id;
						}
						return null;
					}
				};
				break;
			case HOST:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof TransactionObject) {
							return ((TransactionObject) element).host;
						}
						return null;
					}
				};
				break;
			case PID:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof TransactionObject) {
							return String.valueOf(((TransactionObject) element).pid);
						}
						return null;
					}
				};
				break;
			case PROGRAM:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof TransactionObject) {
							return ((TransactionObject) element).program;
						}
						return null;
					}
				};
				break;
			case TRAN_TIME:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof TransactionObject) {
							return String.valueOf(((TransactionObject) element).tran_time);
						}
						return null;
					}
				};
				break;
			case QUERY_TIME:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof TransactionObject) {
							return String.valueOf(((TransactionObject) element).query_time);
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

	class TransactionObject {
		String sql_text;
		String user;
		String sql_id;
		String host;
		String pid;
		String program;
		float tran_time;
		float query_time;
		
		public String getValueByIndex(int index) {
			switch (index) {
			case 0:
				return sql_text;
			case 1:
				return user;
			case 2:
				return sql_id;
			case 3:
				return host;
			case 4:
				return pid;
			case 5:
				return program;
			case 6:
				return String.valueOf(tran_time);
			case 7:
				return String.valueOf(query_time);
			}
			return null;
		}

		public String toString() {
			return sql_text + "\t" + user + "\t" + sql_id + "\t" + host + "\t" 
					+ pid + "\t" + program + "\t" + tran_time + "\t" + query_time + "\n"; 
		}
	}
	
	public enum ColumnEnum {
	    SQL_TEXT("SQL TEXT", 50, SWT.RIGHT, true, true, true),
	    TRAN_TIME("TRAN TIME", 150, SWT.LEFT, true, true, false),
		QUERY_TIME("QUERY_TIME", 150, SWT.LEFT, true, true, false),
		HOST("HOST", 50, SWT.RIGHT, true, true, true),
		PID("PID", 50, SWT.RIGHT, true, true, true),
	    USER("USER", 70, SWT.RIGHT, true, true, false),
	    PROGRAM("PROGRAM", 100, SWT.RIGHT, true, true, true),
	    SQL_ID("SQL_ID", 50, SWT.RIGHT, true, true, true);

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

	@Override
	public void refresh() {
		ActiveDbInfo activeDBList = ActiveDbInfo.getInstance();

		long now = TimeUtil.getCurrentTime(serverId);
		date = DateUtil.yyyymmdd(now);
		stime = now - TIME_RANGE;
		etime = now;

		if (isDefaultView) {
			if (selectionDB.equals("") || selectionDB.equals("default")) {
				checkDBList();
				return;
			} else {
				checkDBList();
			}
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
				if (activeDBList.isEmpty(serverId)) {
					transactionList.clear();
				}

				param.put("objHash", objHashLv);
				param.put("date", date);
				param.put("stime", stime);
				param.put("etime", etime);
				param.put("time", now);
				param.put("key", StatusConstants.CUBRID_DB_TRANSACTION_INFO + selectionDB);
				p = tcp.getSingle(RequestCmd.CUBRID_DB_LONG_TRANSACTION_DATA, param);
				if (p != null) {
					sp = (StatusPack) p;
					mv = sp.data;
				}
				
				if (mv != null) {
					String key;
					TransactionObject tranObj;
					for (int i=0 ; i < mv.getList("user").size() ; i++) {
						tranObj = new TransactionObject();
						tranObj.sql_text = mv.getList("SQL_Text").get(i).toString();
						tranObj.user = mv.getList("user").get(i).toString();
						tranObj.sql_id = mv.getList("SQL_ID").get(i).toString();
						tranObj.host = mv.getList("host").get(i).toString();
						tranObj.pid = mv.getList("pid").get(i).toString();
						tranObj.program = mv.getList("program").get(i).toString();
						tranObj.tran_time = mv.getList("tran_time").getFloat(i);
						tranObj.query_time = mv.getList("query_time").getFloat(i);
						key = tranObj.sql_id + tranObj.host + tranObj.pid + tranObj.user + tranObj.program;
						
						if (saveData.get(key) == null) {
							saveData.put(key, tranObj);
						} else {
							saveData.remove(key);
							saveData.put(key, tranObj);
						}
					}
				}
			}
		} catch (Throwable th) {
			th.printStackTrace();
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		
		if (saveData.size() > 0) {
			updateTableview();
		}
	}
	
	private void updateTableview() {
		int skipCount = saveData.size() - MaxListValue;
		transactionList.clear();
		for(String key : saveData.keySet()) {
			if (skipCount > 0) {
				skipCount--;
			} else {
				transactionList.add(saveData.get(key));
			}
		}

		ExUtil.exec(viewer.getTable(), new Runnable() {
			public void run() {
				viewer.setInput(transactionList.toArray());
			}
		});
	}
	
	public void checkDBList() {

		if (!isDefaultView) {
			return;
		}
		
		if (ActiveDbInfo.getInstance().getActiveDBInfo(serverId) == null) {
			return;
		}
		
		if (ActiveDbInfo.getInstance().getActiveDBInfo(serverId).hashCode() == prvActiveDBHash) {
			return;
		}

		prvActiveDBHash = ActiveDbInfo.getInstance().getActiveDBInfo(serverId).hashCode();

		ExUtil.exec(dbListCombo, new Runnable() {
			public void run() {
				dbListCombo.removeAll();
			}
		});

		if (!ActiveDbInfo.getInstance().isEmpty(serverId)) {
			ExUtil.exec(dbListCombo, new Runnable() {
				public void run() {
					for (String dbName : ActiveDbInfo.getInstance().keySet(serverId)) {
						dbListCombo.add(dbName);
					}
					dbListCombo.setEnabled(true);
				}
			});

		} else {
			ExUtil.exec(dbListCombo, new Runnable() {
				public void run() {
					selectionDB = "";
					dbListCombo.setEnabled(false);
				}
			});
		}

		ExUtil.exec(dbListCombo, new Runnable() {
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
}


