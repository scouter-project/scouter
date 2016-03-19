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
import org.eclipse.ui.console.IConsoleConstants;

import scouter.client.group.view.GroupNavigationView;
import scouter.client.maria.views.DbDailyTotalConnView;
import scouter.client.maria.views.DbRealtimeTotalActivityView;
import scouter.client.maria.views.DbRealtimeTotalConnView;
import scouter.client.maria.views.DbRealtimeTotalHitRatioView;
import scouter.client.maria.views.DbRealtimeTotalResponseView;
import scouter.client.maria.views.DbTodayTotalActivityView;
import scouter.client.maria.views.DbTodayTotalConnView;
import scouter.client.maria.views.DigestTableView;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.views.AlertView;
import scouter.client.views.ObjectDailyListView;
import scouter.client.views.ObjectNavigationView;
import scouter.client.views.WorkspaceExplorer;

public class PerspectiveMaria implements IPerspectiveFactory  {
	
	public static final String ID = PerspectiveMaria.class.getName();
	
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(false);
		
		Server server = ServerManager.getInstance().getDefaultServer();
		int serverId = server.getId();;
		
		IFolderLayout agentLayout = layout.createFolder("perspective.database.objnavigation", IPageLayout.LEFT, 0.20f, editorArea);
		agentLayout.addPlaceholder(ObjectNavigationView.ID + ":*");
		agentLayout.addPlaceholder(ObjectDailyListView.ID + ":*");
		agentLayout.addPlaceholder(WorkspaceExplorer.ID);
		agentLayout.addPlaceholder(GroupNavigationView.ID);
		agentLayout.addView(ObjectNavigationView.ID);
		layout.getViewLayout(ObjectNavigationView.ID).setCloseable(false);
		
		IFolderLayout consoleLayout = layout.createFolder("perspective.database.console", IPageLayout.BOTTOM, 0.4f, "perspective.database.objnavigation");
		consoleLayout.addView(IConsoleConstants.ID_CONSOLE_VIEW);
		
		IFolderLayout alertLayout = layout.createFolder("perspective.database.alert", IPageLayout.TOP, 0.6f, "perspective.database.console");
		alertLayout.addView(AlertView.ID);
		
		IFolderLayout bottomLayout = layout.createFolder("perspective.database.digesttable", IPageLayout.BOTTOM, 0.65f, editorArea);
		bottomLayout.addView(DigestTableView.ID + ":" + serverId);
		
		IFolderLayout layout1 = layout.createFolder("perspective.mariadb.conn", IPageLayout.TOP, 0.5f, editorArea);
		layout1.addPlaceholder(DbTodayTotalConnView.ID + ":*");
		layout1.addPlaceholder(DbDailyTotalConnView.ID + ":*");
		layout1.addView(DbRealtimeTotalConnView.ID + ":" + serverId);
		
		IFolderLayout layout2 = layout.createFolder("perspective.mariadb.elapsed", IPageLayout.BOTTOM, 0.5f, "perspective.mariadb.conn");
		layout2.addView(DbRealtimeTotalResponseView.ID + ":" + serverId);
		
		IFolderLayout layout3 = layout.createFolder("perspective.mariadb.activity", IPageLayout.RIGHT, 0.5f, "perspective.mariadb.conn");
		layout3.addPlaceholder(DbTodayTotalActivityView.ID + ":*");
		layout3.addView(DbRealtimeTotalActivityView.ID + ":" + serverId);
		
		IFolderLayout layout4 = layout.createFolder("perspective.mariadb.hitratio", IPageLayout.RIGHT, 0.5f, "perspective.mariadb.elapsed");
		layout4.addView(DbRealtimeTotalHitRatioView.ID + ":" + serverId);
		
		layout.addPerspectiveShortcut(getId());
		Activator.getDefault().addPrePerspective(getId());
	}
	
	public static String getId() {
		return ID;
	}
}
