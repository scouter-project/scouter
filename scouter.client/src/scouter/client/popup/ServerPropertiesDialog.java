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
package scouter.client.popup;

import java.util.ArrayList;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import scouter.client.model.PropertyData;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.UIUtil;

public class ServerPropertiesDialog {
	
	private final Display display;
	private Shell dialog;
	private int serverId;
	
	private TableViewer propertyTableViewer;
	private TableColumnLayout tableColumnLayout;
	ArrayList<PropertyData> propertyList = new ArrayList<PropertyData>();
	
	public ServerPropertiesDialog(Display display, int serverId) {
		this.display = display;
		this.serverId = serverId;
	}
	
	public void show() {
		dialog = setDialogLayout();
		UIUtil.setDialogDefaultFunctions(dialog);
		makeTableContents();
	}

	private Shell setDialogLayout() {
		Shell dialog = new Shell(display, SWT.DIALOG_TRIM | SWT.RESIZE);
		dialog.setLayout (new FillLayout());
		dialog.setText("Properties");
		Composite tableComposite = new Composite(dialog, SWT.NONE);
		initialTable(tableComposite);
		return dialog;
	}
	
	private void makeTableContents() {
		Server server = ServerManager.getInstance().getServer(serverId);
		propertyList.clear();
		propertyList.add(new PropertyData("name", server.getName()));
		propertyList.add(new PropertyData("ip", server.getIp()));
		propertyList.add(new PropertyData("port", ""+server.getPort()));
		propertyList.add(new PropertyData("version", ""+server.getVersion()));
		propertyList.add(new PropertyData("user id", server.getUserId()));
		propertyList.add(new PropertyData("group", server.getGroup()));
		propertyList.add(new PropertyData("timezone", server.getTimezone()));
		propertyList.add(new PropertyData("session", server.getSession()));
		propertyList.add(new PropertyData("time delta(ms)", server.getDelta()));
		propertyList.add(new PropertyData("secure_mode", server.isSecureMode()));
		propertyTableViewer.refresh();
		dialog.pack();
		dialog.open();
	}
	
	private void initialTable(Composite composite) {
		propertyTableViewer = new TableViewer(composite, SWT.MULTI  | SWT.BORDER);
		tableColumnLayout = new TableColumnLayout();
		composite.setLayout(tableColumnLayout);
		TableViewerColumn c = createTableViewerColumn("Property", 1, 150, SWT.FILL, true, true);
		c.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof PropertyData) {
					return ((PropertyData) element).property;
				}
				return null;
			}
		});
		c = createTableViewerColumn("Value", 2, 300, SWT.FILL, true, true);
		c.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof PropertyData) {
					Object value = ((PropertyData) element).value;
					if (value instanceof String) {
						return (String) value;
					} 
				}
				return null;
			}
		});
		final Table table = propertyTableViewer.getTable();
	    table.setHeaderVisible(true);
	    table.setLinesVisible(true);
	    propertyTableViewer.setContentProvider(new ArrayContentProvider());
	    propertyTableViewer.setInput(propertyList);
	}
	
	private TableViewerColumn createTableViewerColumn(String title, int weight, int width, int alignment,  boolean resizable, boolean moveable) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(propertyTableViewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setAlignment(alignment);
		column.setMoveable(moveable);
		tableColumnLayout.setColumnData(column, new ColumnWeightData(weight, width, resizable));
		return viewerColumn;
	}
}
