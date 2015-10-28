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
package scouter.client.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

import scouter.client.Images;
import scouter.client.model.TextProxy;
import scouter.client.net.TcpProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.views.WhiteBoardView;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.net.RequestCmd;

public class OpenRedisInfoViewAction extends Action {
	public final static String ID = OpenRedisInfoViewAction.class.getName();

	private final IWorkbenchWindow window;
	int serverId;
	int objHash;

	public OpenRedisInfoViewAction(IWorkbenchWindow window, int serverId, int objHash) {
		this.window = window;
		this.serverId = serverId;
		this.objHash = objHash;
		setText("Info");
	}

	public void run() {
		new LoadRedisInfo().schedule();
	}
	
	class LoadRedisInfo extends Job {

		public LoadRedisInfo() {
			super("Load Redis Info");
		}

		protected IStatus run(IProgressMonitor monitor) {
			TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
			try {
				MapPack param = new MapPack();
				param.put("objHash", objHash);
				Pack p = tcp.getSingle(RequestCmd.REDIS_INFO, param);
				if (p != null) {
					MapPack m = (MapPack) p;
					final String content = m.getText("info");
					ExUtil.exec(window.getShell().getDisplay(), new Runnable() {
						public void run() {
							try {
								WhiteBoardView view =  (WhiteBoardView) window.getActivePage().showView(WhiteBoardView.ID, serverId + "&" + objHash, IWorkbenchPage.VIEW_ACTIVATE);
								if (view != null) {
									view.setInput("Info[" + TextProxy.object.getText(objHash) + "]", content);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				TcpProxy.putTcpProxy(tcp);
			}
			return Status.OK_STATUS;
		}
	}
}
