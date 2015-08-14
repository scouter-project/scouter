package scouter.client.stack.views;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import scouter.client.Images;

public class XMLEditorInput implements IEditorInput {
	private String m_fileName;
	
	public XMLEditorInput(String fileName){
		m_fileName = fileName;
	}
	
	public String getFileName(){
		return m_fileName;
	}
	
	@Override
	public Object getAdapter(Class arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean exists() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPersistableElement getPersistable() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getToolTipText() {
		return XMLEditorInput.class.getName();
	}

}
