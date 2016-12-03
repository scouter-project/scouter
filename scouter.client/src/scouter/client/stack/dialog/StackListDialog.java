/*
 *  Copyright 2016 the original author or authors. 
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
 */
package scouter.client.stack.dialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.client.stack.actions.FetchSingleStackJob;
import scouter.client.stack.actions.FetchStackJob;
import scouter.client.util.ExUtil;
import scouter.io.DataInputX;
import scouter.lang.pack.MapPack;
import scouter.net.RequestCmd;
import scouter.util.DateUtil;

public class StackListDialog extends Dialog {
	
	int serverId;
	String objName;
	String date;
	
	Table table;
	Text rangeText;
	
	public StackListDialog(int serverId, String objName) {
		this(serverId, objName, DateUtil.yyyymmdd());
	}

	public StackListDialog(int serverId, String objName, String date) {
		super(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
		this.serverId = serverId;
		this.objName = objName;
		this.date = date;
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite container =  (Composite) super.createDialogArea(parent);
		Label label1 = new Label(container, SWT.NONE);
		label1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		label1.setAlignment(SWT.RIGHT);
		label1.setFont(new Font(null, "Airal", 10, SWT.BOLD));
		label1.setText("Select range to analyze (using shift key) and click \"OK\".");
		Label label2 = new Label(container, SWT.NONE);
		label2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		label2.setAlignment(SWT.RIGHT);
		label2.setFont(new Font(null, "Airal", 10, SWT.NONE));
		label2.setText("Double click to view a single thread dump as raw text.");
		Composite tableComposite = new Composite(container, SWT.NONE);
		tableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table = new Table(tableComposite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.FULL_SELECTION);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				TableItem[] items = table.getSelection();
				if (items != null && items.length > 0) {
					TableItem first = items[0];
					TableItem last = items[items.length - 1];
					rangeText.setText(first.getText(1) + " ~ " + last.getText(1));
				}
			}
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		table.addMouseListener(new MouseListener(){
			public void mouseDoubleClick(MouseEvent e) {
				TableItem[] items = table.getSelection();
				if(items == null){
					return;
				}				
				long from = (Long) items[0].getData();
				new FetchSingleStackJob(serverId, objName, from, getTableList(), null).schedule(500);
				cancelProessed();
			}

			public void mouseDown(MouseEvent arg0) {
			}
			public void mouseUp(MouseEvent arg0) {
			}			
		});
		
		TableColumn indexColumn = new TableColumn(table, SWT.NONE);
		TableColumn timeColumn = new TableColumn(table, SWT.NONE);
		TableColumnLayout tableColumnLayout = new TableColumnLayout();
		tableColumnLayout.setColumnData(indexColumn, new ColumnWeightData(20));
		tableColumnLayout.setColumnData(timeColumn, new ColumnWeightData(80));
		tableComposite.setLayout(tableColumnLayout);
		rangeText = new Text(container, SWT.BORDER | SWT.CENTER | SWT.SINGLE | SWT.READ_ONLY);
		rangeText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		load();
		return container;
	}
	
	private void load() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				final List<Long> timeList = new ArrayList<Long>();
				try {
					MapPack param = new MapPack();
					param.put("objName", objName);
					long from = DateUtil.yyyymmdd(date);
					param.put("from", from);
					param.put("to", from + DateUtil.MILLIS_PER_DAY - 1);
					tcp.process(RequestCmd.GET_STACK_INDEX, param, new INetReader() {
						public void process(DataInputX in) throws IOException {
							long time = in.readLong();
							timeList.add(new Long(time));
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				Collections.sort(timeList);
				ExUtil.exec(table, new Runnable() {
					public void run() {
						for (int i = 0; i < timeList.size(); i++) {
							TableItem item = new TableItem(table, SWT.NONE);
							item.setText(0, String.valueOf(i+1));
							long time = timeList.get(i).longValue();
							item.setText(1, DateUtil.format(time, "yyyy-MM-dd HH:mm:ss"));
							item.setData(time);
						}
					}
				});
			}
		});
	}
	
	@Override
	protected Point getInitialSize() {
		return new Point(500, 500);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(objName);
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected void okPressed() {
		TableItem[] items = table.getSelection();
		if (items != null && items.length > 0) {
			TableItem first = items[0];
			TableItem last = items[items.length - 1];
			long from = (Long) first.getData();
			long to = (Long) last.getData() + 1;
			new FetchStackJob(serverId, objName, from, to, items.length).schedule(500);
		}
		super.okPressed();
	}
	
	protected void cancelProessed(){
		super.cancelPressed();
	}
	
	private List<Long> getTableList(){
		TableItem [] items = table.getItems();
		if(items == null){
			return null;
		}
		List<Long> list = new ArrayList<Long>();
		for(TableItem item : items){
			list.add((Long)item.getData());
		}
		return list;
	}
}
