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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.net.TcpProxy;
import scouter.client.server.ServerManager;
import scouter.client.sorter.ColumnLabelSorter;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.ScouterUtil;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.lang.value.TextValue;
import scouter.lang.value.Value;
import scouter.lang.value.ValueEnum;
import scouter.net.RequestCmd;
import scouter.util.DateUtil;

public class ServerLogsView extends ViewPart {

	public static final String ID = ServerLogsView.class.getName();

	public TableViewer viewer;
	public Table table;

	private int serverId;

	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		if (secId != null) {
			serverId = Integer.parseInt(secId);
		}
	}

	public void createPartControl(Composite parent) {
		this.setPartName("Logs["
				+ ServerManager.getInstance().getServer(serverId).getName()
				+ "]");
		parent.setLayout(new GridLayout(1, true));
		Composite tableComp = new Composite(parent, SWT.BORDER);
		tableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table = new Table(tableComp, SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.FULL_SELECTION);
		table.setHeaderVisible(true);
		viewer = new TableViewer(table);
		table.setLinesVisible(true);
		final TableColumn nameColumn = new TableColumn(table, SWT.LEFT);
		nameColumn.setAlignment(SWT.LEFT);
		nameColumn.setText("Name");
		nameColumn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ColumnLabelSorter sorter = (ColumnLabelSorter) viewer.getComparator();
				sorter.setColumn(nameColumn);
			}
		});
		final TableColumn sizeColumn = new TableColumn(table, SWT.RIGHT);
		sizeColumn.setAlignment(SWT.RIGHT);
		sizeColumn.setText("Size");
		sizeColumn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ColumnLabelSorter sorter = (ColumnLabelSorter) viewer.getComparator();
				sorter.setColumn(sizeColumn);
			}
		});
		final TableColumn timeColumn = new TableColumn(table, SWT.CENTER);
		timeColumn.setAlignment(SWT.CENTER);
		timeColumn.setText("Last Modified");
		timeColumn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ColumnLabelSorter sorter = (ColumnLabelSorter) viewer.getComparator();
				sorter.setColumn(timeColumn);
			}
		});
		TableColumnLayout layout = new TableColumnLayout();
		tableComp.setLayout(layout);
		layout.setColumnData(nameColumn, new ColumnWeightData(30));
		layout.setColumnData(sizeColumn, new ColumnWeightData(20));
		layout.setColumnData(timeColumn, new ColumnWeightData(20));
		viewer.setContentProvider(new ContentProvider());
		viewer.setLabelProvider(new LabelProvider());
		viewer.setComparator(new ColumnLabelSorter(viewer).setCustomCompare(new ColumnLabelSorter.ICustomCompare() {
			public int doCompare(TableColumn col, int index, Object o1, Object o2) {
				if (o1 instanceof LogFileObject && o2 instanceof LogFileObject) {
					LogFileObject dirObj1 = (LogFileObject) o1;
					LogFileObject dirObj2 = (LogFileObject) o2;
					switch (index) {
					case 0:
						return dirObj1.name.compareTo(dirObj2.name);
					case 1:
						long gap = dirObj1.size - dirObj2.size;
						if (gap > 0) {
							return 1;
						} else if (gap < 0) {
							return -1;
						} else {
							return 0;
						}
					case 2:
						gap = dirObj1.lastModified - dirObj2.lastModified;
						if (gap > 0) {
							return 1;
						} else if (gap < 0) {
							return -1;
						} else {
							return 0;
						}
					}
				}
				return 0;
			}
		}));
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("Reload", ImageUtil.getImageDescriptor(Images.refresh)) {
			public void run() {
				load();
			}
		});
		createContextMenu(viewer, new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager){
                fillTreeViewerContextMenu(manager);
            }
        });
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				StructuredSelection sel = (StructuredSelection) viewer.getSelection();
				if (sel != null) {
					Object o = sel.getFirstElement();
					if (o instanceof LogFileObject) {
						LogFileObject lfo = (LogFileObject) o;
						new FileOpenJob(lfo.name).schedule();
					}
				}
			}
		});
		load();
	}
	
	private void load() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				TcpProxy tcpProxy = TcpProxy.getTcpProxy(serverId);
				try {
					MapPack pack = (MapPack) tcpProxy.getSingle(RequestCmd.SERVER_LOG_LIST, null);
					if (pack == null) {
						ConsoleProxy.errorSafe("Cannot load logs");
						return;
					}
					final ArrayList<LogFileObject> fileList = new ArrayList<LogFileObject>();
					ListValue nameLv = pack.getList("name");
					ListValue sizeLv = pack.getList("size");
					ListValue lastModifiedLv = pack.getList("lastModified");

					for (int i = 0; i < nameLv.size(); i++) {
						LogFileObject fileObj = new LogFileObject();
						fileObj.name = nameLv.getString(i);
						fileObj.size = sizeLv.getLong(i);
						fileObj.lastModified = lastModifiedLv.getLong(i);
						fileList.add(fileObj);
					}
					ExUtil.exec(table, new Runnable() {
						public void run() {
							viewer.setInput(fileList);
						}
					});
				} catch (Throwable th) {
					th.printStackTrace();
				} finally {
					TcpProxy.putTcpProxy(tcpProxy);
				}
			}
		});
	}

	public void setFocus() {

	}

	private void createContextMenu(Viewer viewer, IMenuListener listener) {
		MenuManager contextMenu = new MenuManager();
		contextMenu.setRemoveAllWhenShown(true);
		contextMenu.addMenuListener(listener);
		Menu menu = contextMenu.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(contextMenu, viewer);
	}
	
	private void fillTreeViewerContextMenu(IMenuManager mgr){
		ISelection selection = viewer.getSelection();
		if (selection instanceof IStructuredSelection) {
            IStructuredSelection sel = (IStructuredSelection)selection;
			Object obj = sel.getFirstElement();
			if (obj instanceof LogFileObject) {
				LogFileObject lfo = (LogFileObject) obj;
				Action action = new OpenAction(lfo.name);
				mgr.add(action);
			}
		}
    }
	
	public class OpenAction extends Action {
		
		String name;
		
		public OpenAction(String name) {
			super("&Open");
			this.name = name;
		}
		public void run() {
        	new FileOpenJob(name).schedule();
		}
	}
	
	public class FileOpenJob extends Job {

		String name;
		
		public FileOpenJob(String name) {
			super("Open Files");
			this.name = name;
		}
		
		protected IStatus run(IProgressMonitor monitor) {
			TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
			try {
				monitor.beginTask("Open " + name + "...", IProgressMonitor.UNKNOWN);
				MapPack param = new MapPack();
				param.put("name", name);
				Value v = tcp.getSingleValue(RequestCmd.SERVER_LOG_DETAIL, param);
				if (v != null && v.getValueType() == ValueEnum.TEXT) {
					final TextValue tv = (TextValue) v;
					ExUtil.exec(Display.getDefault(), new Runnable() {
						public void run() {
							try {
								WhiteBoardView view =  (WhiteBoardView) getSite().getWorkbenchWindow().getActivePage().showView(WhiteBoardView.ID);
								if (view != null) {
									view.setInput(name, tv.value);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
				}
				monitor.done();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				TcpProxy.putTcpProxy(tcp);
			}
			return Status.OK_STATUS;
		}
	}

	class LogFileObject {
		String name;
		long size;
		long lastModified;
	}

	class ContentProvider implements IStructuredContentProvider {

		List<LogFileObject> objList;
		
		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			objList = (List<LogFileObject>) newInput;
		}

		public Object[] getElements(Object inputElement) {
			if (objList == null) return null;
			return objList.toArray(new LogFileObject[objList.size()]);
		}

	}

	class LabelProvider implements ITableLabelProvider {

		public void addListener(ILabelProviderListener listener) {
		}

		public void dispose() {
		}

		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		public void removeListener(ILabelProviderListener listener) {
		}

		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof LogFileObject) {
				LogFileObject obj = (LogFileObject) element;
				switch (columnIndex) {
				case 0:
					return obj.name;
				case 1:
					return ScouterUtil.humanReadableByteCount(obj.size, true);
				case 2:
					return DateUtil.format(obj.lastModified,
							"yyyy-MM-dd HH:mm:ss");
				}
			}
			return null;
		}
	}
}
