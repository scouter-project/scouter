package scouter.client.stack.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import scouter.client.stack.views.XMLEditorView;

public class OpenXMLEditorAction extends Action {
	public final static String ID = OpenXMLEditorAction.class.getName();

	private String m_fileName;
	IWorkbenchWindow m_window;

	public OpenXMLEditorAction(IWorkbenchWindow window, String label, ImageDescriptor imageDescriptor, String fileName) {
		super(label, imageDescriptor);
		this.setId(ID);
		m_fileName = fileName;
		m_window = window;
	}
	
	public void run(){
		if (m_window != null) {
			try {
				XMLEditorView v = (XMLEditorView)m_window.getActivePage().showView(XMLEditorView.ID);
//				if(v!= null)
//					v.setInput(serverId);
			} catch (PartInitException ex) {
				ex.printStackTrace();
			}
		}
	}
}