/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
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
package scouter.client.heapdump.actions;


import org.eclipse.jface.action.Action;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchWindow;

import scouter.client.heapdump.SaveHeapDumpJob;
import scouter.client.util.ImageUtil;

public class HeapDumpDownloadAction extends Action {
	public final static String ID = HeapDumpDownloadAction.class.getName();

	@SuppressWarnings("unused")
	private final IWorkbenchWindow window;
	private String fileName;
	private String objName;
	private int objHash;
	private int serverId;
	
	public HeapDumpDownloadAction(IWorkbenchWindow window, String label, String fileName, String objName, int objHash, Image image, int serverId) {
		this.window = window;
		this.fileName = fileName;
		this.objName = objName;
		this.objHash = objHash;
		this.serverId = serverId;
		
		setText(label);
		setId(ID);
		setImageDescriptor(ImageUtil.getImageDescriptor(image));

	}

	public void run() {
		SaveHeapDumpJob job = new SaveHeapDumpJob("Save Heap Dump File...", objHash, fileName, objName, serverId);
		job.schedule();
	}
}
