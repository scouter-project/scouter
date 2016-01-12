package scouter.client.popup;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import scouter.client.summary.modules.AlertSummaryComposite;
import scouter.client.summary.modules.ApicallSummaryComposite;
import scouter.client.summary.modules.ErrorSummaryComposite;
import scouter.client.summary.modules.IpSummaryComposite;
import scouter.client.summary.modules.ServiceSummaryComposite;
import scouter.client.summary.modules.SqlSummaryComposite;
import scouter.client.summary.modules.UserAgentSummaryComposite;
import scouter.lang.pack.MapPack;

public class SummaryDialog {
	
	int serverId;
	MapPack param;
	
	Shell dialog;
	TabFolder tabFolder;
	TabItem serviceTab;
	TabItem sqlTab;
	TabItem apicallTab;
	TabItem ipTab;
	TabItem userAgentTab;
	TabItem errorTab;
	TabItem alertTab;
	
	public SummaryDialog(int serverId, MapPack param) {
		this.serverId = serverId;
		this.param = param;
	}
	
	public void show(String title) {
		dialog = new Shell(Display.getDefault(), SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM | SWT.RESIZE);
		dialog.setText(title);
		dialog.setLayout(new GridLayout(1, true));
		tabFolder = new TabFolder(dialog, SWT.BORDER);
		tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		serviceTab = new TabItem(tabFolder, SWT.NULL);
		serviceTab.setText("Service");
		serviceTab.setControl(getServiceControl(tabFolder));
		sqlTab = new TabItem(tabFolder, SWT.NULL);
		sqlTab.setText("SQL");
		sqlTab.setControl(getSqlControl(tabFolder));
		apicallTab = new TabItem(tabFolder, SWT.NULL);
		apicallTab.setText("API Call");
		apicallTab.setControl(getApicallControl(tabFolder));
		ipTab = new TabItem(tabFolder, SWT.NULL);
		ipTab.setText("IP");
		ipTab.setControl(getIpControl(tabFolder));
		userAgentTab = new TabItem(tabFolder, SWT.NULL);
		userAgentTab.setText("User-Agent");
		userAgentTab.setControl(getUaControl(tabFolder));
		errorTab = new TabItem(tabFolder, SWT.NULL);
		errorTab.setText("Exception");
		errorTab.setControl(getErrorControl(tabFolder));
		
		alertTab = new TabItem(tabFolder, SWT.NULL);
		alertTab.setText("Alert");
		alertTab.setControl(getAlertControl(tabFolder));	
		
		Button closeBtn = new Button(dialog, SWT.PUSH);
		GridData gr = new GridData(SWT.RIGHT, SWT.FILL, false, false);
		gr.widthHint = 100;
		closeBtn.setLayoutData(gr);
		closeBtn.setText("&Close");
		closeBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				dialog.close();
			}
		});
		
		dialog.pack();
		dialog.open();
	}
	
	private Control getServiceControl(Composite parent) {
		ServiceSummaryComposite comp = new ServiceSummaryComposite(parent, SWT.NONE);
		comp.setData(serverId, param);
		
		return comp;
	}
	
	private Control getSqlControl(Composite parent) {
		SqlSummaryComposite comp = new SqlSummaryComposite(parent, SWT.NONE);
		comp.setData(serverId, param);
		return comp;
	}
	
	private Control getApicallControl(Composite parent) {
		ApicallSummaryComposite comp = new ApicallSummaryComposite(parent, SWT.NONE);
		comp.setData(serverId, param);
		return comp;
	}
	
	private Control getIpControl(Composite parent) {
		IpSummaryComposite comp = new IpSummaryComposite(parent, SWT.NONE);
		comp.setData(serverId, param);
		return comp;
	}
	
	private Control getUaControl(Composite parent) {
		UserAgentSummaryComposite comp = new UserAgentSummaryComposite(parent, SWT.NONE);
		comp.setData(serverId, param);
		return comp;
	}
	
	private Control getErrorControl(Composite parent) {
		ErrorSummaryComposite comp = new ErrorSummaryComposite(parent, SWT.NONE);
		comp.setData(serverId, param);
		return comp;
	}
	
	private Control getAlertControl(Composite parent) {
		AlertSummaryComposite comp = new AlertSummaryComposite(parent, SWT.NONE);
		comp.setData(serverId, param);
		return comp;
	}

}
