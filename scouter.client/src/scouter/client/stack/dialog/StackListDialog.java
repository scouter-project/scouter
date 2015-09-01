package scouter.client.stack.dialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
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
import org.eclipse.ui.PlatformUI;

import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
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
		Label label = new Label(container, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		label.setAlignment(SWT.RIGHT);
		label.setFont(new Font(null, "Airal", 10, SWT.BOLD));
		label.setText("Select range to analyze (using shift key)");
		Composite tableComposite = new Composite(container, SWT.NONE);
		tableComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		table = new Table(tableComposite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.FULL_SELECTION);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		TableColumn column = new TableColumn(table, SWT.NONE);
		TableColumnLayout tableColumnLayout = new TableColumnLayout();
		tableColumnLayout.setColumnData(column, new ColumnWeightData(100));
		tableComposite.setLayout(tableColumnLayout);
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
						for (Long time : timeList) {
							TableItem item = new TableItem(table, SWT.NONE);
							item.setText(DateUtil.format(time.longValue(), "yyyy-MM-dd HH:mm:ss"));
						}
					}
				});
			}
		});
	}
	
	@Override
	protected Point getInitialSize() {
		return new Point(400, 500);
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
}
