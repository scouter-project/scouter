package scouter.client.stack.actions;

import org.eclipse.jface.action.Action;

import scouter.client.Images;
import scouter.client.model.TextProxy;
import scouter.client.stack.dialog.StackListDialog;
import scouter.client.util.ImageUtil;

public class OpenStackDialogAction extends Action {
	public final static String ID = OpenStackDialogAction.class.getName();
	
	int serverId;
	int objHash;

	public OpenStackDialogAction(int serverId, int objHash) {
		this.serverId = serverId;
		this.objHash = objHash;
		this.setText("Thread Stack Analyzer");
		this.setImageDescriptor(ImageUtil.getImageDescriptor(Images.page_white_stack));
	}
	
	public void run(){
		String objName = TextProxy.object.getText(objHash);
		new StackListDialog(serverId, objName).open();
	}
}