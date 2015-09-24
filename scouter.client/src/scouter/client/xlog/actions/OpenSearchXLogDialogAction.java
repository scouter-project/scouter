/*
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */
package scouter.client.xlog.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;

import scouter.client.Images;
import scouter.client.util.ImageUtil;
import scouter.client.xlog.dialog.XLogSearchDialog;

public class OpenSearchXLogDialogAction extends Action {
	public final static String ID = OpenSearchXLogDialogAction.class.getName();

	private final IWorkbenchWindow win;
	private int serverId;
	private String objType;
	
	public OpenSearchXLogDialogAction(IWorkbenchWindow win, int serverId, String objType) {
		this.win = win;
		this.serverId = serverId;
		this.objType = objType;
		setImageDescriptor(ImageUtil.getImageDescriptor(Images.find));
		setText("Search XLog");
	}
	
	public void run() {
		new XLogSearchDialog(win, serverId, objType).show();
	}
}
