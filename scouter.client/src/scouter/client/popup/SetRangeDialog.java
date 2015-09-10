package scouter.client.popup;

import org.csstudio.swt.xygraph.figures.Axis;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import scouter.util.CastUtil;

public class SetRangeDialog extends Dialog {
	
	Button autoRange;
	Axis axis;
	Text maxTxt;
	Text minTxt;

	public SetRangeDialog(Shell parentShell, Axis axis) {
		super(parentShell);
		this.axis = axis;
	}

	protected Control createDialogArea(Composite parent) {
		Composite comp =  (Composite) super.createDialogArea(parent);
		FillLayout fillLayout = new FillLayout();
		fillLayout.marginWidth = 5;
		comp.setLayout(fillLayout);
		Group container = new Group(comp, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = false;
		layout.marginWidth = 5;
		layout.marginHeight = 5;
		container.setLayout(layout);
		autoRange = new Button(container, SWT.CHECK);
		autoRange.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		autoRange.setText("Auto Range");
		autoRange.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (autoRange.getSelection()) {
					maxTxt.setEnabled(false);
					minTxt.setEnabled(false);
				} else {
					maxTxt.setEnabled(true);
					minTxt.setEnabled(true);
				}
			}
		});
		Label maxLbl = new Label(container, SWT.NONE);
		maxLbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		maxLbl.setAlignment(SWT.RIGHT);
		maxLbl.setText("Max : ");
		maxTxt = new Text(container, SWT.BORDER);
		maxTxt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		Label minLbl = new Label(container, SWT.NONE);
		minLbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		minLbl.setAlignment(SWT.RIGHT);
		minLbl.setText("Min : ");
		minTxt = new Text(container, SWT.BORDER);
		minTxt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		if (axis.isAutoScale()) {
			autoRange.setSelection(true);
			maxTxt.setText(CastUtil.cString(axis.getRange().getUpper()));
			minTxt.setText(CastUtil.cString(axis.getRange().getLower()));
			maxTxt.setEnabled(false);
			minTxt.setEnabled(false);
		} else {
			maxTxt.setText(CastUtil.cString(axis.getRange().getUpper()));
			minTxt.setText(CastUtil.cString(axis.getRange().getLower()));
		}
		return container;
	}
	
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Set Range");
	}

	protected boolean isResizable() {
		return false;
	}

	protected void okPressed() {
		if (autoRange.getSelection()) {
			axis.setAutoScale(true);
			super.okPressed();
		} else {
			try {
				double max = CastUtil.cdouble(maxTxt.getText().trim());
				double min = CastUtil.cdouble(minTxt.getText().trim());
				if (max > min) {
					axis.setRangeDirect(min, max);
				} else {
					axis.setRangeDirect(max, min);
				}
				axis.setAutoScale(false);
				super.okPressed();
			} catch (Exception e) {
				MessageDialog.openError(getShell(), "Error", e.toString());
			}
		}
	}
}
