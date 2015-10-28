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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
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
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.model.TextProxy;
import scouter.client.net.TcpProxy;
import scouter.client.sorter.ColumnLabelSorter;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.ScouterUtil;
import scouter.client.util.TimeUtil;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.DateUtil;

public class ObjectDumpFileListView extends ViewPart {

	public static final String ID = ObjectDumpFileListView.class.getName();

	int serverId;
	int objHash;
	TableViewer viewer;
	TableColumnLayout tableColumnLayout;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secondaryId = site.getSecondaryId();
		if (secondaryId != null) {
			String secIds[] = secondaryId.split("_");
			serverId = Integer.parseInt(secIds[0]);
			objHash = Integer.parseInt(secIds[1]);
		}
		
	}

	public void createPartControl(Composite parent) {
		this.setPartName("Dump File List[" + TextProxy.object.getLoadText(DateUtil.yyyymmdd(TimeUtil.getCurrentTime(serverId)), objHash, serverId) + "]");
		parent.setLayout(new GridLayout(1, true));
		Composite tableComp = new Composite(parent, SWT.NONE);
		tableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tableColumnLayout = new TableColumnLayout();
		tableComp.setLayout(tableColumnLayout);
		viewer = new TableViewer(tableComp, SWT.FULL_SELECTION | SWT.BORDER);
		createColumns();
		final Table table = viewer.getTable();
	    table.setHeaderVisible(true);
	    table.setLinesVisible(true);
	    viewer.setContentProvider(new ArrayContentProvider());
	    viewer.setComparator(new ColumnLabelSorter(viewer).setCustomCompare(new ColumnLabelSorter.ICustomCompare() {
			public int doCompare(TableColumn col, int index, Object o1, Object o2) {
				Boolean isNumber = (Boolean) col.getData("isNumber");
				if (isNumber != null && isNumber.booleanValue()) {
					FileData d1 = (FileData) o1;
					FileData d2 = (FileData) o2;
					return (int) (d1.getNumberValue(index) - d2.getNumberValue(index));
				} else {
					ILabelProvider labelProvider = (ILabelProvider) viewer.getLabelProvider(index);
					String t1 = labelProvider.getText(o1);
					String t2 = labelProvider.getText(o2);
					if (t1 == null)
						t1 = "";
					if (t2 == null)
						t2 = "";
					return t1.compareTo(t2);
				}
			}
		}));
	    viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				StructuredSelection sel = (StructuredSelection) event.getSelection();
				FileData data = (FileData) sel.getFirstElement();
				try {
					ObjectDumpFileDetailView view = (ObjectDumpFileDetailView) PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getActivePage()
							.showView(ObjectDumpFileDetailView.ID, serverId + "&" + objHash + "&" + data.name, IWorkbenchPage.VIEW_ACTIVATE);
					if (view != null) {
						view.setInput(data.name);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	    IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("reload", ImageUtil.getImageDescriptor(Images.refresh)) {
			public void run() {
				load();
			}
		});
	    load();
	}
	
	private void load() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				Pack p = null;
				try {
					MapPack param = new MapPack();
					param.put("objHash", objHash);
					p = tcp.getSingle(RequestCmd.OBJECT_DUMP_FILE_LIST, param);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				final List<FileData> dataList = new ArrayList<FileData>();
				if (p != null) {
					MapPack m = (MapPack) p;
					ListValue nameLv = m.getList("name");
					ListValue sizeLv = m.getList("size");
					ListValue lastLv = m.getList("last_modified");
					if (nameLv != null) {
						for (int i = 0; i < nameLv.size(); i++) {
							FileData data = new FileData();
							dataList.add(data);
							data.name = nameLv.getString(i);
							data.size = sizeLv.getLong(i);
							data.lastModifed = lastLv.getLong(i);
						}
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
		for (ColumnEnum column : ColumnEnum.values()) {
			TableViewerColumn c = createTableViewerColumn(column.getTitle(),
					column.getWidth(), column.getAlignment(),
					column.isResizable(), column.isMoveable(),
					column.isNumber());
			ColumnLabelProvider labelProvider = null;
			switch (column) {
			case NAME:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof FileData) {
							FileData data = (FileData) element;
							return data.name;
						}
						return null;
					}
				};
				break;
			case SIZE:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof FileData) {
							FileData data = (FileData) element;
							return ScouterUtil.humanReadableByteCount(data.size, true);
						}
						return null;
					}
				};
				break;
			case LAST_MODIFIED:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof FileData) {
							FileData data = (FileData) element;
							return DateUtil.format(data.lastModifed, "yyyy-MM-dd HH:mm:ss");
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
	
	class FileData {
		String name;
		long size;
		long lastModifed;
		
		public long getNumberValue(int index) {
			switch (index) {
			case 1:
				return size;
			case 2:
				return lastModifed;
			}
			return 0;
		}
	}

	private TableViewerColumn createTableViewerColumn(String title, int width,
			int alignment, boolean resizable, boolean moveable,
			final boolean isNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setAlignment(alignment);
		column.setMoveable(moveable);
		tableColumnLayout.setColumnData(column, new ColumnWeightData(width, 20,
				resizable));
		column.setData("isNumber", isNumber);
		column.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ColumnLabelSorter sorter = (ColumnLabelSorter) viewer.getComparator();
				sorter.setColumn(column);
			}
		});
		return viewerColumn;
	}

	public enum ColumnEnum {

		NAME("Name", 300, SWT.CENTER, true, true, false), //
		SIZE("Size", 100, SWT.RIGHT, true, true, true), //
		LAST_MODIFIED("Last Modified", 200, SWT.CENTER, true, true, true);

		private final String title;
		private final int width;
		private final int alignment;
		private final boolean resizable;
		private final boolean moveable;
		private final boolean isNumber;

		private ColumnEnum(String text, int width, int alignment,
				boolean resizable, boolean moveable, boolean isNumber) {
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
	
	public void setFocus() {
	}
}
