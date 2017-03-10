package scouter.client.notice;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

public class NoticeDialog extends Dialog {

	String message; 
	
	protected NoticeDialog(Shell parentShell, String message) {
		super(parentShell);
		this.message = message;
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite comp =  (Composite) super.createDialogArea(parent);
		FillLayout fillLayout = new FillLayout();
		fillLayout.marginWidth = 5;
		fillLayout.marginHeight = 5;
		comp.setLayout(fillLayout);
		Browser webview = new Browser(comp, SWT.BORDER);
		webview.setText(message);
		return webview;
	}
	
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Notice");
	}
	
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Point getInitialLocation(Point initialSize) {
		Monitor primaryMonitor = Display.getDefault().getPrimaryMonitor();
		Rectangle bounds = primaryMonitor.getBounds();
		int x = bounds.x + (bounds.width) / 2 - getShell().getSize().x / 2;
		int y = bounds.y + (bounds.height) / 2 - getShell().getSize().y / 2;
		return new Point(x, y);
	}
	
	@Override
	protected Point getInitialSize() {
		return getShell().computeSize(500, 400);
	}
	
	@Override
	protected Control createButtonBar(Composite parent){
		return null;
	}
	
}
