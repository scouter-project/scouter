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

import scouter.client.counter.views.CounterRealTimeAllView;
import scouter.client.counter.views.CounterRealTimeTotalView;
import scouter.client.group.view.GroupNavigationView;
import scouter.client.preferences.PManager;
import scouter.client.preferences.PreferenceConstants;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.views.AlertView;
import scouter.client.views.EQView;
import scouter.client.views.ObjectDailyListView;
import scouter.client.views.ObjectNavigationView;
import scouter.client.views.WorkspaceExplorer;
import scouter.client.xlog.views.XLogRealTimeView;
import scouter.lang.counters.CounterConstants;

public class PerspectiveService implements IPerspectiveFactory  {
	
	public static final String ID = PerspectiveService.class.getName();
	
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(false);
		
		String host = PManager.getInstance().getString(PreferenceConstants.P_PERS_WAS_SERV_DEFAULT_HOST);
    	String objType = PManager.getInstance().getString(PreferenceConstants.P_PERS_WAS_SERV_DEFAULT_WAS);
		
		Server server = ServerManager.getInstance().getDefaultServer();
		int serverId = 0;
		if(server != null){
			serverId = server.getId();
		}
		
		IFolderLayout agentLayout = layout.createFolder(IConstants.LAYOUT_WASSERVICE_OBJECT_NAVIGATION, IPageLayout.LEFT, 0.20f, editorArea);
		agentLayout.addPlaceholder(ObjectNavigationView.ID + ":*");
		agentLayout.addPlaceholder(ObjectDailyListView.ID + ":*");
		agentLayout.addPlaceholder(WorkspaceExplorer.ID);
		agentLayout.addPlaceholder(GroupNavigationView.ID);
		agentLayout.addView(ObjectNavigationView.ID);
		layout.getViewLayout(ObjectNavigationView.ID).setCloseable(false); 
		
		IFolderLayout eqLayout = layout.createFolder(IConstants.LAYOUT_WASSERVICE_EQ, IPageLayout.BOTTOM, 0.5f, IConstants.LAYOUT_WASSERVICE_OBJECT_NAVIGATION);
		eqLayout.addPlaceholder(EQView.ID + ":*");
		eqLayout.addView(EQView.ID + ":" + serverId +"&"+ objType); // 1

		IFolderLayout cpuLayout = layout.createFolder(IConstants.LAYOUT_WASSERVICE_CPU, IPageLayout.BOTTOM, 0.5f, IConstants.LAYOUT_WASSERVICE_EQ);
		cpuLayout.addView(CounterRealTimeAllView.ID + ":" + serverId + "&" + host + "&" + CounterConstants.HOST_CPU);
		
		IFolderLayout alertLayout = layout.createFolder(IConstants.LAYOUT_WASSERVICE_ALERT, IPageLayout.BOTTOM, 0.5f, IConstants.LAYOUT_WASSERVICE_OBJECT_NAVIGATION);
		alertLayout.addPlaceholder(AlertView.ID + ":*");
		alertLayout.addView(AlertView.ID);
		
		IFolderLayout upResLayout = layout.createFolder(IConstants.LAYOUT_WASSERVICE_LEFT_TOP, IPageLayout.LEFT, 0.3f, editorArea);
		upResLayout.addView(CounterRealTimeAllView.ID + ":" + serverId + "&" + objType + "&" + CounterConstants.WAS_RECENT_USER);
		
		IFolderLayout midResLayout = layout.createFolder(IConstants.LAYOUT_WASSERVICE_LEFT_MID1, IPageLayout.BOTTOM, 0.25f, IConstants.LAYOUT_WASSERVICE_LEFT_TOP);
		midResLayout.addView(CounterRealTimeTotalView.ID + ":" + serverId + "&" + objType + "&" + CounterConstants.WAS_TPS);

		IFolderLayout mid2ResLayout = layout.createFolder(IConstants.LAYOUT_WASSERVICE_LEFT_MID2, IPageLayout.BOTTOM, 0.33f, IConstants.LAYOUT_WASSERVICE_LEFT_MID1);
		mid2ResLayout.addView(CounterRealTimeAllView.ID + ":" + serverId + "&" + objType + "&" + CounterConstants.WAS_ELAPSED_TIME);
		
		IFolderLayout downResLayout = layout.createFolder(IConstants.LAYOUT_WASSERVICE_LEFT_BOTTOM, IPageLayout.BOTTOM, 0.5f, IConstants.LAYOUT_WASSERVICE_LEFT_MID2);
		downResLayout.addView(CounterRealTimeAllView.ID + ":" + serverId + "&" + objType + "&" + CounterConstants.JAVA_HEAP_USED);

		IFolderLayout xlogTopLayout = layout.createFolder(IConstants.LAYOUT_WASSERVICE_CENTER_TOP, IPageLayout.LEFT, 1f, editorArea);
		xlogTopLayout.addView(XLogRealTimeView.ID + ":" + serverId + "&" + objType);
		
		
		layout.addPerspectiveShortcut(getId());
		Activator.getDefault().addPrePerspective(getId());
	}
	
	public static String getId() {
		return ID;
	}
}
