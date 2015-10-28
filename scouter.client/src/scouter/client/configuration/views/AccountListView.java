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
package scouter.client.configuration.views;

import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
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
import scouter.client.configuration.actions.EditAccountAction;
import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.client.server.GroupPolicyConstants;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.sorter.ColumnLabelSorter;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.lang.Account;
import scouter.lang.value.BlobValue;
import scouter.lang.value.Value;
import scouter.lang.value.ValueEnum;
import scouter.io.DataInputX;
import scouter.net.RequestCmd;

public class AccountListView extends ViewPart {
	
	public final static String ID = AccountListView.class.getName();
	
	private int serverId;
	
	private TableViewer viewer;
	private TableColumnLayout tableColumnLayout;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		this.serverId = Integer.valueOf(site.getSecondaryId());
	}

	public void createPartControl(Composite parent) {
		this.setPartName("Account List[" + ServerManager.getInstance().getServer(serverId).getName() + "]");
		tableColumnLayout = new TableColumnLayout();
		parent.setLayout(tableColumnLayout);
		viewer = new TableViewer(parent, SWT.FULL_SELECTION | SWT.BORDER);
		createColumns();
		final Table table = viewer.getTable();
	    table.setHeaderVisible(true);
	    table.setLinesVisible(true);
	    viewer.setContentProvider(new ArrayContentProvider());
	    viewer.setComparator(new ColumnLabelSorter(viewer));
	    createTableContextMenu();
	    IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
	    man.add(new Action("Reload", ImageUtil.getImageDescriptor(Images.refresh)) {
			public void run() {
				load();
			}
		});
	    load();
	}
	
	private void createTableContextMenu() {
		MenuManager manager = new MenuManager();
		viewer.getControl().setMenu(manager.createContextMenu(viewer.getControl()));
		Server server = ServerManager.getInstance().getServer(serverId);
		if (server.isAllowAction(GroupPolicyConstants.ALLOW_EDITACCOUNT)) {
			manager.add(new Action("Edit Account") {
				public void run() {
					ISelection selection = viewer.getSelection();
					if (selection instanceof StructuredSelection) {
						Object o = ((StructuredSelection)selection).getFirstElement();
						if (o instanceof Account) {
							new EditAccountAction(getSite().getWorkbenchWindow(), serverId, (Account) o).run();
						}
					}
				}
			});
		}
	}
	
	public void setFocus() {
		
	}
	
	private void load() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				final ArrayList<Account> list = new ArrayList<Account>();
				try {
					tcp.process(RequestCmd.LIST_ACCOUNT, null, new INetReader() {
						public void process(DataInputX in) throws IOException {
							Value v = in.readValue();
							if (v.getValueType() == ValueEnum.BLOB) {
								BlobValue bv = (BlobValue) v;
								Account ac = new Account();
								ac.toObject(bv.value);
								list.add(ac);
							}
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				ExUtil.exec(viewer.getTable(), new Runnable() {
					public void run() {
						viewer.setInput(list);
					}
				});
			}
		});
	}
	
	private void createColumns() {
		for (AccountEnum column : AccountEnum.values()) {
			TableViewerColumn c = createTableViewerColumn(column.getTitle(), column.getWidth(), column.getAlignment(), column.isResizable(), column.isMoveable(), column.isNumber());
			ColumnLabelProvider labelProvider = null;
			switch (column) {
			case ID :
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof Account) {
							return ((Account)element).id;
						}
						return null;
					}
				};
				break;
			case GROUP :
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof Account) {
							return ((Account)element).group;
						}
						return null;
					}
				};
				break;
			case EMAIL :
				labelProvider = new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof Account) {
							return ((Account)element).email;
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
	
	enum AccountEnum {
	    ID("ID", 50, SWT.CENTER, true, true, false), //
	    GROUP("Group", 50, SWT.CENTER, true, true, false), //
	    EMAIL("Email", 50, SWT.LEFT, true, true, false);

	    private final String title;
	    private final int width;
	    private final int alignment;
	    private final boolean resizable;
	    private final boolean moveable;
	    private final boolean isNumber;

	    private AccountEnum(String text, int width, int alignment, boolean resizable, boolean moveable, boolean isNumber) {
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
