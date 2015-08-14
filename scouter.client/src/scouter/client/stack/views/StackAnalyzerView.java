/*
 *  Copyright 2015 LG CNS.
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
package scouter.client.stack.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.stack.actions.MainFrameAction;
import scouter.client.stack.actions.OpenXMLEditorAction;
import scouter.client.stack.base.MainFrame;
import scouter.client.util.ImageUtil;
import scouter.client.views.ServerFileManagementView;
import scouter.util.SystemUtil;

public class StackAnalyzerView extends ViewPart {
	
	public final static String ID = StackAnalyzerView.class.getName();

	public void createPartControl(Composite parent) {
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();		
		
		man.add(new Action("open local stack log", ImageUtil.getImageDescriptor(Images.folder)) {
			public void run() {
				new MainFrameAction("Open Stack Log").start();
			}
		});
		man.add(new Action("close all stack log", ImageUtil.getImageDescriptor(Images.close_folder)) {
			public void run() {
				new MainFrameAction("Close All").start();
			}
		});
		man.add(new Separator());
		man.add(new Action("select local parser configuration", ImageUtil.getImageDescriptor(Images.config)) {
			public void run() {
				new MainFrameAction("Select Parser Configuration").start();
			}
		});
		
		man.add(new OpenXMLEditorAction(win, "edit parser configuration", ImageUtil.getImageDescriptor(Images.edit_config), "Default"));
		
		Composite swtAwtComponent = new Composite(parent, SWT.EMBEDDED);
		if (SystemUtil.IS_MAC_OSX) {
			SWT_AWT.embeddedFrameClass = "sun.lwawt.macosx.CViewEmbeddedFrame";
		}
		java.awt.Frame baseFrame = SWT_AWT.new_Frame( swtAwtComponent );
		MainFrame frame = MainFrame.instance(true);
		frame.init();
		baseFrame.add(frame);
		frame.setVisible(true); 
	}

	public void setFocus() {
		// TODO Auto-generated method stub
		
	}

}
