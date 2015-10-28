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
import java.util.Iterator;
import java.util.List;

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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.model.AgentDataProxy;
import scouter.client.model.TextProxy;
import scouter.client.popup.EditableMessageDialog;
import scouter.client.sorter.ColumnLabelSorter;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.ScouterUtil;
import scouter.lang.pack.MapPack;
import scouter.util.CastUtil;
import scouter.util.StringUtil;


public class ObjectEnvView extends ViewPart {
	public static final String ID = ObjectEnvView.class.getName();

	private Text filterTxt;
	
	private TableViewer viewer;
	private TableColumnLayout tableColumnLayout;
	
	Composite parent;
	private Clipboard clipboard;
	private int objHash;
	private int serverId;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String[] ids = secId.split("&");
		serverId = Integer.valueOf(ids[0]);
		objHash = Integer.valueOf(ids[1]);
	}

	@Override
	public void createPartControl(Composite parent) {
		this.setPartName(TextProxy.object.getText(objHash));
		this.parent = parent;
		initialLayout();
		clipboard = new Clipboard(null);
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("Reload", ImageUtil.getImageDescriptor(Images.refresh)) {
			public void run() {
				load();
			}
		});
		load();
	}
	
	private void initialLayout() {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, true));
		createUpperMenu(composite);
		Composite tableComposite = new Composite(composite, SWT.NONE);
		tableComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		tableComposite.setLayout(new GridLayout(1, true));
		createTableViewer(tableComposite);
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
				if (e.stateMask == SWT.CTRL) {
					if (e.keyCode == 'c' || e.keyCode == 'C') {
						selectionCopyToClipboard();
					}
				}
			}
		});
	    viewer.getTable().addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				TableItem[] items = viewer.getTable().getSelection();
				if (items != null && items.length > 0) {
					VariableData data = (VariableData) items[0].getData();
					new EditableMessageDialog().show(data.name, data.value);
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
					VariableData data = (VariableData) items[i].getData();
					sb.append(data.toString());
				}
				clipboard.setContents(new Object[] {sb.toString()}, new Transfer[] {TextTransfer.getInstance()});
			}
		}
	}
	
	ArrayList<VariableData> variableList;
	
	private void load() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				MapPack pack = AgentDataProxy.getEnv(objHash, serverId);
				if (pack != null) {
					variableList = new ArrayList<VariableData>();
					Iterator<String> keys = pack.keys();
					while (keys.hasNext()) {
						String name = keys.next();
						String value = CastUtil.cString(pack.get(name));
						VariableData data = new VariableData();
						variableList.add(data);
						data.name = name;
						data.value = value;
					}
					ExUtil.exec(viewer.getTable(), new Runnable() {
						public void run() {
							viewer.setInput(variableList);
						}
					});
				}
			}
		});
	}
	
	private void createUpperMenu(Composite composite) {
		Group parentGroup = new Group(composite, SWT.NONE);
		parentGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		GridLayout layout = new GridLayout(4, true);
		parentGroup.setLayout(layout);
		
		Label dummyLabel = new Label(parentGroup, SWT.NONE);
		dummyLabel = new Label(parentGroup, SWT.NONE);
		dummyLabel = new Label(parentGroup, SWT.NONE);
		dummyLabel.setLayoutData(new GridData());
		
		filterTxt = new Text(parentGroup, SWT.BORDER);
        filterTxt.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
        filterTxt.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String searchText = filterTxt.getText();
				if (StringUtil.isEmpty(searchText)) {
					viewer.setInput(variableList);
				} else {
					searchText = searchText.toLowerCase();
					List<VariableData> tempList = new ArrayList<VariableData>();
					for (VariableData data : variableList) {
						String name = data.name.toLowerCase();
						String value = data.value.toLowerCase();
						if (name.contains(searchText) || value.contains(searchText)) {
							tempList.add(data);
						}
					}
					viewer.setInput(tempList);
				}
			}
		});
	}
	
	private void createColumns() {
		for (VariableEnum column : VariableEnum.values()) {
			TableViewerColumn c = createTableViewerColumn(column.getTitle(), column.getWidth(), column.getAlignment(), column.isResizable(), column.isMoveable(), column.isNumber());
			ColumnLabelProvider labelProvider = null;
			switch (column) {
			case NAME:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof VariableData) {
							return ((VariableData) element).name;
						}
						return null;
					}
				};
				break;
			case VALUE:
				labelProvider = new ColumnLabelProvider() {
					@Override
					public String getText(Object element) {
						if (element instanceof VariableData) {
							return ((VariableData) element).value;
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

	public void setFocus() {
	}

	enum VariableEnum {
		NAME("Name", 50, SWT.LEFT, true, true, false), //
	    VALUE("Value", 50, SWT.LEFT, true, true, false);
	    
	    private final String title;
	    private final int width;
	    private final int alignment;
	    private final boolean resizable;
	    private final boolean moveable;
	    private final boolean isNumber;
	    
	    private VariableEnum(String text, int width, int alignment, boolean resizable, boolean moveable, boolean isNumber) {
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
	
	class VariableData {
		public String name;
		public String value;
		public String toString() {
			return name + "\t" + value + "\n"; 
		}
	}
}