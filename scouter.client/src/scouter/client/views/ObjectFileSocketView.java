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

import java.io.IOException;
import java.util.ArrayList;
import java.util.ResourceBundle;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.model.TextProxy;
import scouter.client.net.TcpProxy;
import scouter.client.popup.EditableMessageDialog;
import scouter.client.sorter.ColumnLabelSorter;
import scouter.client.util.ColoringWord;
import scouter.client.util.CustomLineStyleListener;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.BlobValue;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;
import scouter.util.Hexa32;
import scouter.util.IPUtil;
import scouter.util.StringUtil;

public class ObjectFileSocketView extends ViewPart {

	public final static String ID = ObjectFileSocketView.class.getName();

	static ArrayList<ColoringWord> keyWords = new ArrayList<ColoringWord>();

	static {
		keyWords.add(new ColoringWord("java.lang.Thread.State:", SWT.COLOR_BLUE, false));
		keyWords.add(new ColoringWord("daemon", SWT.COLOR_BLUE, false));
		keyWords.add(new ColoringWord("java.util", SWT.COLOR_BLUE, false));
		keyWords.add(new ColoringWord("java.net.Socket.connect", SWT.COLOR_BLUE, false));
		keyWords.add(new ColoringWord("prio", SWT.COLOR_BLUE, false));
		keyWords.add(new ColoringWord("org.apache", SWT.COLOR_BLUE, false));
		keyWords.add(new ColoringWord("java.lang.Thread.run", SWT.COLOR_DARK_GREEN, false));
		keyWords.add(new ColoringWord("java.lang", SWT.COLOR_DARK_MAGENTA, false));
	}

	ResourceBundle bundle = ResourceBundle.getBundle("scouter.client.views.tcpport");

	private int serverId;
	private int objHash;

	private TableViewer viewer;
	private TableColumnLayout tableColumnLayout;

	public void setInput(int serverId, int objHash) {
		this.serverId = serverId;
		this.objHash = objHash;
		this.setPartName("Socket[" + TextProxy.object.getText(objHash) + "]");
		load(0);
	}

	public void createPartControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		composite.setLayout(new GridLayout(1, true));
		createTableViewer(composite);
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("Reload", ImageUtil.getImageDescriptor(Images.refresh)) {
			public void run() {
				load(0);
			}
		});
	}

	private void createTableViewer(Composite composite) {
		viewer = new TableViewer(composite, SWT.FULL_SELECTION | SWT.BORDER);
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
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				try {
					StructuredSelection sel = (StructuredSelection) event.getSelection();
					SocketObject socketObj = (SocketObject) sel.getFirstElement();
					if (StringUtil.isNotEmpty(socketObj.stack)) {
						new EditableMessageDialog().show("Stack", socketObj.stack, new CustomLineStyleListener(false,
								keyWords, false));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private void load(final long key) {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				Pack p = null;
				try {
					MapPack param = new MapPack();
					param.put("objHash", objHash);
					param.put("key", key);
					p = tcp.getSingle(RequestCmd.OBJECT_SOCKET, param);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				final ArrayList<SocketObject> list = new ArrayList<SocketObject>();
				if (p != null) {
					MapPack m = (MapPack) p;
					ListValue keyLv = m.getList("key");
					ListValue hostLv = m.getList("host");
					ListValue portLv = m.getList("port");
					ListValue countLv = m.getList("count");
					ListValue serviceLv = m.getList("service");
					ListValue txidLv = m.getList("txid");
					ListValue orderLv = m.getList("order");
					ListValue stackLv = m.getList("stack");
					TextProxy.service.load(DateUtil.yyyymmdd(), serviceLv, serverId);
					for (int i = 0; i < keyLv.size(); i++) {
						SocketObject socketObj = new SocketObject();
						socketObj.key = keyLv.getLong(i);
						socketObj.host = ((BlobValue) hostLv.get(i)).value;
						socketObj.port = portLv.getInt(i);
						socketObj.count = countLv.getInt(i);
						socketObj.service = TextProxy.service.getText(serviceLv.getInt(i));
						socketObj.txid = txidLv.getLong(i);
						socketObj.standby = orderLv.getBoolean(i);
						socketObj.stack = stackLv.getString(i);
						list.add(socketObj);
					}
				}
				ExUtil.exec(viewer.getTable(), new Runnable() {
					public void run() {
						viewer.setInput(list);
					}
				});
			}
		});
	}

	private void createTableContextMenu() {
		MenuManager manager = new MenuManager();
		viewer.getControl().setMenu(manager.createContextMenu(viewer.getControl()));
		manager.add(new Action("&Stack Trace", ImageDescriptor.createFromImage(Images.pin)) {
			public void run() {
				try {
					StructuredSelection sel = (StructuredSelection) viewer.getSelection();
					SocketObject socketObj = (SocketObject) sel.getFirstElement();
					if (socketObj != null) {
						load(socketObj.key);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	private void createColumns() {
		for (SocketTableEnum column : SocketTableEnum.values()) {
			TableViewerColumn c = createTableViewerColumn(column.getTitle(), column.getWidth(), column.getAlignment(),
					column.isResizable(), column.isMoveable(), column.isNumber());
			ColumnLabelProvider labelProvider = null;
			switch (column) {
			case HOST:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof SocketObject) {
							SocketObject so = (SocketObject) element;
							return IPUtil.toString(so.host);
						}
						return null;
					}
				};
				break;
			case PORT:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof SocketObject) {
							SocketObject so = (SocketObject) element;
							return CastUtil.cString(so.port);
						}
						return null;
					}
				};
				break;
			case DESC:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof SocketObject) {
							SocketObject so = (SocketObject) element;
							String port =CastUtil.cString(so.port);
							if (bundle.containsKey(port)) {
								return bundle.getString(port);
							}
						}
						return null;
					}
				};
				break;
			case COUNT:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof SocketObject) {
							SocketObject so = (SocketObject) element;
							return FormatUtil.print(so.count, "#,##0");
						}
						return null;
					}
				};
				break;
			case SERVICE:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof SocketObject) {
							SocketObject so = (SocketObject) element;
							return so.service;
						}
						return null;
					}
				};
				break;
			case TXID:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof SocketObject) {
							SocketObject so = (SocketObject) element;
							return Hexa32.toString32(so.txid);
						}
						return null;
					}
				};
				break;
			case STANDBY:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof SocketObject) {
							SocketObject so = (SocketObject) element;
							if (so.standby) {
								return "\u2713";
							}
						}
						return null;
					}
				};
				break;
			case STACK:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof SocketObject) {
							SocketObject so = (SocketObject) element;
							return so.stack;
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

	private TableViewerColumn createTableViewerColumn(String title, int width, int alignment, boolean resizable,
			boolean moveable, final boolean isNumber) {
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

	enum SocketTableEnum {

		HOST("Host", 150, SWT.LEFT, true, true, false), PORT("Port", 50, SWT.LEFT, true, true, true), DESC(
				"Description", 150, SWT.LEFT, true, true, false), COUNT("Count", 50, SWT.RIGHT, true, true, true), SERVICE(
				"Service", 250, SWT.LEFT, true, true, false), TXID("Txid", 100, SWT.LEFT, true, true, false), STANDBY(
				"\u2713", 30, SWT.CENTER, true, true, false), STACK("Stack", 300, SWT.LEFT, true, true, false);

		private final String title;
		private final int width;
		private final int alignment;
		private final boolean resizable;
		private final boolean moveable;
		private final boolean isNumber;

		private SocketTableEnum(String text, int width, int alignment, boolean resizable, boolean moveable,
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

	static class SocketObject {
		long key;
		byte[] host;
		int port;
		long count;
		String service;
		long txid;
		boolean standby;
		String stack;
	}
}
