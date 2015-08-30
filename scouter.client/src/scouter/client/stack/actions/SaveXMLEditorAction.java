package scouter.client.stack.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;

import scouter.client.stack.views.XMLEditorView;

public class SaveXMLEditorAction extends Action {
	public final static String ID = OpenXMLEditorAction.class.getName();

	XMLEditorView m_editorView = null;
	
	public SaveXMLEditorAction(XMLEditorView editorView, String label, ImageDescriptor imageDescriptor) {
		super(label, imageDescriptor);
		this.setId(ID);
		m_editorView = editorView;
	}
	
	public void run(){
		if (m_editorView != null) {
			try {
				m_editorView.saveConfigurations();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
}