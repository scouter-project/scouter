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

import scouter.util.DateUtil;

import scouter.client.group.view.GroupNavigationView;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.views.AlertView;
import scouter.client.views.ObjectDailyListView;
import scouter.client.views.ObjectNavigationView;
import scouter.client.views.WorkspaceExplorer;

import scouter.client.cubrid.CubridSingleItem;
import scouter.client.cubrid.views.CubridSingleRealTimeMultiView;
import scouter.client.cubrid.views.CubridSpaceDbView;
import scouter.client.cubrid.views.CubridLongTransactionList;
import scouter.client.cubrid.views.CubridRealtimeDmlView;
import scouter.client.cubrid.views.CubridServerInfoView;

public class PerspectiveCubrid implements IPerspectiveFactory  {
	
	public static final String ID = PerspectiveCubrid.class.getName();
	
	public void createInitialLayout(IPageLayout layout) {
		String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(false);
		
		Server server = ServerManager.getInstance().getDefaultServer();
		int serverId = server.getId();;
		
		IFolderLayout agentLayout = layout.createFolder("perspective.database.objnavigation", IPageLayout.LEFT, 0.3f, editorArea);
		agentLayout.addPlaceholder(ObjectNavigationView.ID + ":*");
		agentLayout.addPlaceholder(ObjectDailyListView.ID + ":*");
		agentLayout.addPlaceholder(WorkspaceExplorer.ID);
		agentLayout.addPlaceholder(GroupNavigationView.ID);
		agentLayout.addView(ObjectNavigationView.ID);
		layout.getViewLayout(ObjectNavigationView.ID).setCloseable(false);

		IFolderLayout consoleLayout = layout.createFolder("perspective.database.console", IPageLayout.BOTTOM, 0.5f, "perspective.database.objnavigation");
		consoleLayout.addView(IConsoleConstants.ID_CONSOLE_VIEW);
		
		IFolderLayout alertLayout = layout.createFolder("perspective.database.alert", IPageLayout.TOP, 0.5f, "perspective.database.console");
		alertLayout.addView(AlertView.ID);
		
		IFolderLayout layout1 = layout.createFolder("perspective.cubrid.serverinfo", IPageLayout.LEFT, 0.4f, editorArea);
		layout1.addView(CubridServerInfoView.ID + ":" + serverId);
		layout.getViewLayout(CubridServerInfoView.ID + ":" + serverId).setCloseable(false);
		
		IFolderLayout layout3 = layout.createFolder("perspective.cubrid.multiview.xaslplan", IPageLayout.BOTTOM, 0.2f, "perspective.cubrid.serverinfo");
		layout3.addView(CubridSingleRealTimeMultiView.ID + ":" + serverId + "&" + "default" 
				+ "&" + CubridSingleItem.XASL_PLAN_HIT_RATE.ordinal() + "&" + DateUtil.MILLIS_PER_TEN_MINUTE);
		
		IFolderLayout layout4 = layout.createFolder("perspective.cubrid.multiview.tps", IPageLayout.BOTTOM, 0.3f, "perspective.cubrid.multiview.xaslplan");
		layout4.addView(CubridSingleRealTimeMultiView.ID + ":" + serverId + "&" + "default" 
				+ "&" + CubridSingleItem.TPS.ordinal() + "&" + DateUtil.MILLIS_PER_TEN_MINUTE);

		IFolderLayout layout5 = layout.createFolder("perspective.cubrid.multiview.qps", IPageLayout.BOTTOM, 0.5f, "perspective.cubrid.multiview.tps");
		layout5.addView(CubridSingleRealTimeMultiView.ID + ":" + serverId + "&" + "default" 
				+ "&" + CubridSingleItem.QPS.ordinal() + "&" + DateUtil.MILLIS_PER_TEN_MINUTE);

		IFolderLayout layout6 = layout.createFolder("perspective.cubrid.mutiview.pagefetches", IPageLayout.RIGHT, 0.3f, editorArea);
		layout6.addView(CubridSingleRealTimeMultiView.ID + ":" + serverId + "&" + "default" 
				+ "&" + CubridSingleItem.DATA_PAGE_FETCHES.ordinal() + "&" + DateUtil.MILLIS_PER_TEN_MINUTE);
		
		IFolderLayout layout7 = layout.createFolder("perspective.cubrid.longtran", IPageLayout.BOTTOM, 0.3f, "perspective.cubrid.mutiview.pagefetches");
		layout7.addView(CubridLongTransactionList.ID + ":" + serverId + "&" + "default");
		
		IFolderLayout layout8 = layout.createFolder("perspective.cubrid.mutiview.pageiowrite", IPageLayout.TOP, 0.3f, "perspective.cubrid.longtran");
		layout8.addView(CubridSingleRealTimeMultiView.ID + ":" + serverId + "&" + "default" 
				+ "&" + CubridSingleItem.DATA_PAGE_IO_WRITES.ordinal() + "&" + DateUtil.MILLIS_PER_TEN_MINUTE);
		
		IFolderLayout layout9 = layout.createFolder("perspective.cubrid.dmlview", IPageLayout.BOTTOM, 0.4f, "perspective.cubrid.longtran");
		layout9.addView(CubridRealtimeDmlView.ID + ":" + serverId + "&" + "default");
		
		IFolderLayout layout2 = layout.createFolder("perspective.cubrid.dbspaceinfo", IPageLayout.RIGHT, 0.5f, "perspective.cubrid.serverinfo");
		layout2.addView(CubridSpaceDbView.ID + ":" + serverId + "&" + "default");
		
		layout.addPerspectiveShortcut(getId());
		
	}
	
	public static String getId() {
		return ID;
	}
}
