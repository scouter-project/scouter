package scouter.client.stack.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import scouter.client.context.actions.CloseServerAction;
import scouter.client.stack.views.XMLEditor;
import scouter.client.stack.views.XMLEditorInput;

public class OpenXMLEditorAction extends Action {
	public final static String ID = CloseServerAction.class.getName();

	private String m_fileName;

	public OpenXMLEditorAction(String label, ImageDescriptor imageDescriptor, String fileName) {
		super(label, imageDescriptor);
		m_fileName = fileName;
	}
	
	public void run(){
		IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (win != null) {
			try {
				win.getActivePage().openEditor(new XMLEditorInput("Default"), XMLEditor.ID);
			} catch (PartInitException ex) {
				ex.printStackTrace();
			}
		}
	}
}