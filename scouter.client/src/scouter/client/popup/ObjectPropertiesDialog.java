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
import java.util.Date;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import scouter.client.model.AgentColorManager;
import scouter.client.model.AgentModelThread;
import scouter.client.model.PropertyData;
import scouter.client.net.TcpProxy;
import scouter.client.server.ServerManager;
import scouter.client.util.ColorUtil;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.UIUtil;
import scouter.lang.ObjectType;
import scouter.lang.counters.CounterEngine;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.ObjectPack;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;

public class ObjectPropertiesDialog {
	
	private final Display display;
	private Shell dialog;
	private final int objHash;
	private int serverId;
	
	private ObjectPack objectPack;
	private TableViewer propertyTableViewer;
	private TableColumnLayout tableColumnLayout;
	ArrayList<PropertyData> propertyList = new ArrayList<PropertyData>();
	
	public ObjectPropertiesDialog(Display display, int objHash, int serverId) {
		this.display = display;
		this.objHash = objHash;
		this.serverId = serverId;
	}
	
	public void show() {
		dialog = setDialogLayout();
		UIUtil.setDialogDefaultFunctions(dialog);
		findObjectPack();
		makeTableContents();
	}
	
	public void show(final String date) {
		dialog = setDialogLayout();
		UIUtil.setDialogDefaultFunctions(dialog);
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				ObjectPack objectPack = null;
				TcpProxy proxy = TcpProxy.getTcpProxy(serverId);
				try {
					MapPack param = new MapPack();
					param.put("date", date);
					param.put("objHash", objHash);
					objectPack = (ObjectPack) proxy.getSingle(RequestCmd.OBJECT_INFO, param);
					
					CounterEngine counterEngine = ServerManager.getInstance().getServer(serverId).getCounterEngine();
					String code = counterEngine.getMasterCounter(objectPack.objType);
					objectPack.tags.put("main counter", code);
				} catch (Throwable t) {
					ConsoleProxy.errorSafe(t.toString());
				} finally {
					TcpProxy.putTcpProxy(proxy);
				}
				ObjectPropertiesDialog.this.objectPack = objectPack;
				ExUtil.exec(propertyTableViewer.getTable(), new Runnable() {
					public void run() {
						makeTableContents();
					}
				});
			}
		});
	}

	private Shell setDialogLayout() {
		Shell dialog = new Shell(display, SWT.DIALOG_TRIM | SWT.RESIZE);
		dialog.setText("Properties");
		dialog.setLayout(new FillLayout());
		Composite tableComposite = new Composite(dialog, SWT.NONE);
		initialTable(tableComposite);
		return dialog;
	}
	
	private void findObjectPack() {
		ArrayList<ObjectPack> packList= AgentModelThread.getInstance().getAgentPackList();
		for (ObjectPack objectPack : packList) {
			if (objectPack.objHash == this.objHash) {
				CounterEngine counterEngine = ServerManager.getInstance().getServer(serverId).getCounterEngine();
				String code = counterEngine.getMasterCounter(objectPack.objType);
				objectPack.tags.put("main counter", code);
				this.objectPack = objectPack;
				break;
			}
		}
	}
	
	private void makeTableContents() {
		if (this.objectPack == null) {
			return;
		}
		CounterEngine counterEngine = ServerManager.getInstance().getServer(serverId).getCounterEngine();
		ObjectType type = counterEngine.getObjectType(this.objectPack.objType);
		propertyList.clear();
		propertyList.add(new PropertyData("objectName", this.objectPack.objName));
		propertyList.add(new PropertyData("objectType", this.objectPack.objType));
		propertyList.add(new PropertyData("family", type == null ? "undefined" : type.getFamily().getName()));
		propertyList.add(new PropertyData("address", this.objectPack.address));
		propertyList.add(new PropertyData("version", this.objectPack.version));
		propertyList.add(new PropertyData("alive", String.valueOf(this.objectPack.alive)));
		propertyList.add(new PropertyData("wakeUp", FormatUtil.print(new Date(this.objectPack.wakeup), "yyyyMMdd HH:mm:ss.SSS")));
		propertyList.add(new PropertyData("color", AgentColorManager.getInstance().assignColor(this.objectPack.objType, objHash)));
		for (String key : this.objectPack.tags.keySet()) {
			propertyList.add(new PropertyData(key, CastUtil.cString(this.objectPack.tags.get(key))));
		}
		propertyTableViewer.refresh();
		dialog.pack();
		dialog.open();
	}
	
	private void initialTable(Composite composite) {
		propertyTableViewer = new TableViewer(composite, SWT.MULTI  | SWT.BORDER);
		tableColumnLayout = new TableColumnLayout();
		composite.setLayout(tableColumnLayout);

		TableViewerColumn cdummy = createTableViewerDummyColumn("", 0, SWT.FILL, true, true);
		cdummy.setLabelProvider(new ColumnLabelProvider(){
			public String getText(Object element) {
				return "";
			}
			
		});
		
		TableViewerColumn c = createTableViewerColumn("Property", 150, SWT.FILL, true, true);
		c.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof PropertyData) {
					Object property = ((PropertyData) element).property;
					if (property instanceof String) {
						return (String) property;
					}
				}
				return null;
			}

			public Image getImage(Object element) {
				return null;
			}
			
		});
		c = createTableViewerColumn("Value", 300, SWT.FILL, true, true);
		c.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof PropertyData) {
					Object value = ((PropertyData) element).value;
					if (value instanceof String) {
						return (String) value;
					} else if (value instanceof Color) {
						Color color = (Color) value;
						return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
					}
				}
				return null;
			}

			public Image getImage(Object element) {
				if (element instanceof PropertyData) {
					Object value = ((PropertyData) element).value;
					if (value instanceof Color) {
						Image im = new Image(Display.getCurrent(), 20, 12);
						GC gc = new GC(im);
						gc.setBackground((Color) value);
						gc.fillRectangle(8, 1, 10, 10);
						gc.setForeground(ColorUtil.getInstance().getColor(SWT.COLOR_BLACK));
						gc.drawRectangle(8, 1, 10, 10);
						gc.dispose();
						return im;
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
	
	private TableViewerColumn createTableViewerColumn(String title, int width, int alignment,  boolean resizable, boolean moveable) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(propertyTableViewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setAlignment(alignment);
		column.setMoveable(moveable);
		tableColumnLayout.setColumnData(column, new ColumnWeightData(30, width, resizable));
		return viewerColumn;
	}
	private TableViewerColumn createTableViewerDummyColumn(String title, int width, int alignment,  boolean resizable, boolean moveable) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(propertyTableViewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setAlignment(alignment);
		column.setMoveable(moveable);
		tableColumnLayout.setColumnData(column, new ColumnWeightData(0, width, false));
		return viewerColumn;
	}
}
