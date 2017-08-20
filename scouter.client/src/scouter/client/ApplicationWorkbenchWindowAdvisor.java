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

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import scouter.Version;
import scouter.client.misc.UpdateCheckScheduler;
import scouter.client.notice.NoticeCheckScheduler;
import scouter.client.remote.CheckMyJob;
import scouter.client.threads.AlertProxyThread;
import scouter.client.threads.SessionObserver;

import java.util.TimeZone;

/*
 * ApplicationWorkbenchWindowAdvisor.preWindowOpen()
 * ApplicationWorkbenchWindowAdvisor.createActionBarAdvisor()
 * ApplicationActionBarAdvisor.makeActions()
 * ApplicationActionBarAdvisor.fillMenuBar()
 * ApplicationActionBarAdvisor.fillCoolBar()
 * ApplicationActionBarAdvisor.fillStatusLine()
 * ApplicationWorkbenchWindowAdvisor.postWindowOpen()
 * ApplicationActionBarAdvisor.dispose()
 * ApplicationWorkbenchWindowAdvisor.dispose()
 */

public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {
	
	private ApplicationActionBarAdvisor actionBarAdvisor;
	Display display;
	
	public ApplicationWorkbenchWindowAdvisor(
			IWorkbenchWindowConfigurer configurer) {
		super(configurer);
	}

	public ActionBarAdvisor createActionBarAdvisor(
			IActionBarConfigurer configurer) {
		actionBarAdvisor = new ApplicationActionBarAdvisor(configurer);
		return actionBarAdvisor;
	}

	
	IWorkbenchWindowConfigurer configurer;
	
	public void preWindowOpen() {
		configurer = getWindowConfigurer();
		configurer.setInitialSize(new Point(1440, 900));
		configurer.setShowMenuBar(true);
		configurer.setShowCoolBar(false);
		configurer.setShowStatusLine(true);
		//PlatformUI.getPreferenceStore().setValue(IWorkbenchPreferenceConstants.SHOW_MEMORY_MONITOR,	true); 
		configurer.setShowProgressIndicator(true);
		//configurer.setShowFastViewBars(false);
		configurer.setShowPerspectiveBar(true);
		
		configurer.setTitle("Version - "+Version.getClientFullVersion() + "(" + TimeZone.getDefault().getDisplayName() + ")");
	}
	
	public void postWindowOpen() {
		super.postWindowOpen();
	}

	public void dispose() {
		super.dispose();
	}

	public void postWindowRestore() throws WorkbenchException {
		super.postWindowRestore();
	}

	public void postWindowCreate() {
		super.postWindowCreate();
		boolean isrcp = false;
		if (Platform.getProduct() != null) {
			if (Activator.PRODUCT_ID.equals(Platform.getProduct().getId())
					&& Activator.APPLICATION_ID.equals(Platform.getProduct()
							.getApplication())) {
				isrcp = true;
			}
		} else {
			if (Activator.APPLICATION_ID.equals(System
					.getProperty("eclipse.application"))) {
				isrcp = true;
			}
		}

		if (isrcp) {
			IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
			configurer.getWindow().getShell().setMaximized(true);
			startBackgroundJob();
		}
	}

	private void startBackgroundJob() {
		CheckMyJob.getInstance();
		SessionObserver.load();
		AlertProxyThread.getInstance();
		NoticeCheckScheduler.INSTANCE.initialize();
		UpdateCheckScheduler.INSTANCE.initialize();
	}

}