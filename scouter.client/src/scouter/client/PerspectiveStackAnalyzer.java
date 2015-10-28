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
package scouter.client;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import scouter.client.group.view.GroupNavigationView;
import scouter.client.stack.views.StackAnalyzerView;
import scouter.client.views.ObjectDailyListView;
import scouter.client.views.ObjectNavigationView;
import scouter.client.views.WorkspaceExplorer;

public class PerspectiveStackAnalyzer implements IPerspectiveFactory  {
	
	public static final String ID = PerspectiveStackAnalyzer.class.getName();
	
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(false);
		
		IFolderLayout agentLayout = layout.createFolder(IConstants.LAYOUT_WASSERVICE_OBJECT_NAVIGATION, IPageLayout.LEFT, 0.20f, editorArea);
		agentLayout.addPlaceholder(ObjectNavigationView.ID + ":*");
		agentLayout.addPlaceholder(ObjectDailyListView.ID + ":*");
		agentLayout.addPlaceholder(GroupNavigationView.ID);
		agentLayout.addView(ObjectNavigationView.ID);
		layout.getViewLayout(ObjectNavigationView.ID).setCloseable(false); 
		
		IFolderLayout mainLayout = layout.createFolder("perspective.stack.main", IPageLayout.LEFT, 1.0f, editorArea);
		mainLayout.addView(StackAnalyzerView.ID);
		layout.getViewLayout(StackAnalyzerView.ID).setCloseable(false); 
		
		IFolderLayout explorerFolder = layout.createFolder("perspective.stack.explorer", IPageLayout.BOTTOM, 0.5f, IConstants.LAYOUT_WASSERVICE_OBJECT_NAVIGATION);
		explorerFolder.addView(WorkspaceExplorer.ID);
		
		layout.addPerspectiveShortcut(getId());
	}
	
	public static String getId() {
		return ID;
	}
}
