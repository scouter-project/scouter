package scouter.client.listeners;

import org.csstudio.swt.xygraph.figures.Axis;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.swt.widgets.Shell;

import scouter.client.popup.SetRangeDialog;

public class RangeMouseListener extends MouseListener.Stub {
	
	final Shell shell;
	final Axis axis;
	
	public RangeMouseListener(Shell shell, Axis axis) {
		this.shell = shell;
		this.axis = axis;
	}
	
	public void mouseDoubleClicked(MouseEvent me) {
		new SetRangeDialog(shell, axis).open();
	}
}
