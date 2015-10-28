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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.net.TcpProxy;
import scouter.client.server.ServerManager;
import scouter.client.sorter.TreeLabelSorter;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.ScouterUtil;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.DateUtil;

public class ServerFileManagementView extends ViewPart {

	public static final String ID = ServerFileManagementView.class.getName();

	public TreeViewer viewer;
	public Tree tree;
	Composite labelComp;
	Label totalLabel;

	private int serverId;
	Map<String, DirObject> dirMap = new TreeMap<String, DirObject>();

	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		if (secId != null) {
			serverId = Integer.parseInt(secId);
		}
	}

	public void createPartControl(Composite parent) {
		this.setPartName("DataManagement["
				+ ServerManager.getInstance().getServer(serverId).getName()
				+ "]");
		parent.setLayout(new GridLayout(1, true));
		labelComp = new Composite(parent, SWT.NONE);
		labelComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		labelComp.setLayout(new RowLayout());
		totalLabel = new Label(parent, SWT.NONE);
		totalLabel.setText("                                                ");
		Composite treeComp = new Composite(parent, SWT.BORDER);
		treeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tree = new Tree(treeComp, SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.FULL_SELECTION | SWT.MULTI);
		tree.setHeaderVisible(true);
		viewer = new TreeViewer(tree);
		tree.setLinesVisible(true);
		final TreeColumn nameColumn = new TreeColumn(tree, SWT.LEFT);
		nameColumn.setAlignment(SWT.LEFT);
		nameColumn.setText("Name");
		nameColumn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				TreeLabelSorter sorter = (TreeLabelSorter) viewer.getComparator();
				sorter.setColumn(nameColumn);
			}
		});
		final TreeColumn sizeColumn = new TreeColumn(tree, SWT.RIGHT);
		sizeColumn.setAlignment(SWT.RIGHT);
		sizeColumn.setText("Size");
		sizeColumn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				TreeLabelSorter sorter = (TreeLabelSorter) viewer.getComparator();
				sorter.setColumn(sizeColumn);
			}
		});
		final TreeColumn timeColumn = new TreeColumn(tree, SWT.CENTER);
		timeColumn.setAlignment(SWT.CENTER);
		timeColumn.setText("Last Modified");
		timeColumn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				TreeLabelSorter sorter = (TreeLabelSorter) viewer.getComparator();
				sorter.setColumn(timeColumn);
			}
		});
		TreeColumnLayout layout = new TreeColumnLayout();
		treeComp.setLayout(layout);
		layout.setColumnData(nameColumn, new ColumnWeightData(30));
		layout.setColumnData(sizeColumn, new ColumnWeightData(20));
		layout.setColumnData(timeColumn, new ColumnWeightData(20));
		viewer.setContentProvider(new DirContentProvider());
		viewer.setLabelProvider(new LockLabelProvider());
		viewer.setComparator(new TreeLabelSorter(viewer).setCustomCompare(new TreeLabelSorter.ICustomCompare() {
			public int doCompare(TreeColumn col, int index, Object o1, Object o2) {
				if (o1 instanceof DirObject && o2 instanceof DirObject) {
					DirObject dirObj1 = (DirObject) o1;
					DirObject dirObj2 = (DirObject) o2;
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
		viewer.setInput(dirMap);
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("Collapse All", ImageUtil
				.getImageDescriptor(Images.collapse)) {
			public void run() {
				viewer.collapseAll();
			}
		});
		man.add(new Separator());
		man.add(new Action("Reload", ImageUtil
				.getImageDescriptor(Images.refresh)) {
			public void run() {
				load();
			}
		});
		load();
		createContextMenu(viewer, new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager){
                fillTreeViewerContextMenu(manager);
            }
        });
	}
	
	private void load() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				TcpProxy tcpProxy = TcpProxy.getTcpProxy(serverId);
				try {
					MapPack pack = (MapPack) tcpProxy.getSingle(
							RequestCmd.SERVER_DB_LIST, null);
					if (pack == null) {
						return;
					}
					final long totalSize = pack.getLong("total");
					final long freeSize = pack.getLong("free");
					ListValue nameLv = pack.getList("name");
					ListValue sizeLv = pack.getList("size");
					ListValue lastModifiedLv = pack.getList("lastModified");
					dirMap.clear();

					Map<String, DirObject> tempMap = new HashMap<String, DirObject>();
					for (int i = 0; i < nameLv.size(); i++) {
						DirObject dirObj = new DirObject();
						dirObj.name = nameLv.getString(i);
						dirObj.size = sizeLv.getLong(i);
						dirObj.lastModified = lastModifiedLv.getLong(i);
						tempMap.put(dirObj.name, dirObj);
					}
					Iterator<String> itr = tempMap.keySet().iterator();
					while (itr.hasNext()) {
						String name = itr.next();
						DirObject dirObj = tempMap.get(name);
						int lastIndex = name.lastIndexOf("/");
						if (lastIndex < 0) {
							lastIndex = name.lastIndexOf("\\");
						}
						if (lastIndex > 1) {
							String parentName = name.substring(0, lastIndex);
							DirObject parentObj = tempMap.get(parentName);
							parentObj.addChild(dirObj);
							dirObj.parent = parentObj;
						} else {
							dirMap.put(name, dirObj);
						}
					}
					ExUtil.exec(labelComp, new Runnable() {
						public void run() {
							StringBuffer sb = new StringBuffer();
							sb.append("Size : ");
							sb.append(ScouterUtil.humanReadableByteCount(
									totalSize, true));
							if (freeSize > 0) {
								sb.append(" / Available : ");
								sb.append(ScouterUtil.humanReadableByteCount(
										freeSize, true));
							}
							totalLabel.setText(sb.toString());
							totalLabel.update();
							labelComp.layout(true, true);
						}
					});
					ExUtil.exec(viewer.getTree(), new Runnable() {
						public void run() {
							viewer.refresh();
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
			Object[] elements = sel.toArray();
			if (elements == null || elements.length < 1) {
            	return;
            }
			Action action = new DeleteAction(elements);
			for (Object obj : elements) {
				try {
					DirObject dirObj = (DirObject) obj;
					if (dirObj.name.contains("00000000")) {
						action.setEnabled(false);
						break;
					}
				} catch (Exception e) { }
			}
			mgr.add(action);
		}
    }
	
	public class DeleteAction extends Action {
		
		Object[] elements;
		
		public DeleteAction(Object[] elements) {
			super("&Delete");
			this.elements = elements;
		}
		public void run() {
            int length = elements.length;
            if (MessageDialog.openConfirm(getViewSite().getShell(), "Permanently Delete", length + " directories are deleted permanently with subdirectories. Continue?")) {
            	new DeleteJob("Delete Files", elements).schedule();
            }
		}
	}
	
	public class DeleteJob extends Job {

		Object[] elements;
		
		public DeleteJob(String name, Object[] elements) {
			super(name);
			this.elements = elements;
		}
		
		protected IStatus run(IProgressMonitor monitor) {
			TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
			try {
				monitor.beginTask("Delete Server Files.....", IProgressMonitor.UNKNOWN);
				MapPack param = new MapPack();
				ListValue fileLv = param.newList("file");
				for (int i = 0; i < elements.length; i++) {
					if (elements[i] instanceof DirObject) {
						DirObject obj = (DirObject) elements[i];
						fileLv.add(obj.name);
					}
				}
				MapPack m = (MapPack) tcp.getSingle(RequestCmd.SERVER_DB_DELETE, param);
				monitor.done();
				int size = m.getInt("size");
				ConsoleProxy.infoSafe(size + " directories is deleted.");
				load();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				TcpProxy.putTcpProxy(tcp);
			}
			return Status.OK_STATUS;
		}
		
	}

	class DirObject {
		String name;
		long size;
		long lastModified;
		DirObject parent;
		List<DirObject> childList;

		public void addChild(DirObject child) {
			if (childList == null) {
				childList = new ArrayList<DirObject>();
			}
			childList.add(child);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DirObject other = (DirObject) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (name == null) {
				if (other.name != null)
					return false;
			} else if (!name.equals(other.name))
				return false;
			return true;
		}

		private ServerFileManagementView getOuterType() {
			return ServerFileManagementView.this;
		}
	}

	class DirContentProvider implements ITreeContentProvider {

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@SuppressWarnings("rawtypes")
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof Map) {
				return ((Map) inputElement).values().toArray();
			}
			return new Object[0];
		}

		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof DirObject) {
				if (((DirObject) parentElement).childList != null) {
					return ((DirObject) parentElement).childList.toArray();
				}
			}
			return null;
		}

		public Object getParent(Object element) {
			if (element instanceof DirObject) {
				return ((DirObject) element).parent;
			}
			return null;
		}

		public boolean hasChildren(Object element) {
			if (element instanceof DirObject) {
				return ((DirObject) element).childList != null;
			}
			return false;
		}

	}

	class LockLabelProvider implements ITableLabelProvider {

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
			if (columnIndex == 0 && element instanceof DirObject) {
				return Images.folder;
			}
			return null;
		}

		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof DirObject) {
				DirObject obj = (DirObject) element;
				switch (columnIndex) {
				case 0:
					int lastIndex = obj.name.lastIndexOf("/");
					if (lastIndex < 0) {
						lastIndex = obj.name.lastIndexOf("\\");
					}
					if (lastIndex > -1) {
						return obj.name.substring(lastIndex + 1);
					} else {
						return obj.name;
					}
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
