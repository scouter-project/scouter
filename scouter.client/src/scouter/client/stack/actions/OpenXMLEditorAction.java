package scouter.client.stack.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;

import scouter.client.stack.views.XMLEditorView;

public class OpenXMLEditorAction extends Action {
	public final static String ID = OpenXMLEditorAction.class.getName();

	private IWorkbenchWindow m_window;
	
	public OpenXMLEditorAction(IWorkbenchWindow window, String label, ImageDescriptor imageDescriptor) {
		super(label, imageDescriptor);
		this.setId(ID);
		m_window = window;
	}
	
	public void run(){
		if (m_window != null) {
			try {
				m_window.getActivePage().showView(XMLEditorView.ID);
			} catch (PartInitException ex) {
				ex.printStackTrace();
			}
		}
	}
}