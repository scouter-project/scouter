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

import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.graphics.Point;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.eclipse.ui.internal.registry.ActionSetRegistry;
import org.eclipse.ui.internal.registry.IActionSetDescriptor;
import scouter.Version;
import scouter.client.misc.UpdateCheckScheduler;
import scouter.client.notice.NoticeCheckScheduler;
import scouter.client.popup.ImportFromGitHubDialog;
import scouter.client.remote.CheckMyJob;
import scouter.client.threads.AlertProxyThread;
import scouter.client.threads.SessionObserver;
import scouter.client.workspace.WorkspaceManager;
import scouter.client.util.ExUtil;
import scouter.util.ThreadUtil;

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

	@SuppressWarnings("restriction")
	public void preWindowOpen() {
		removeUnwantedActionSets();

		configurer = getWindowConfigurer();
		configurer.setInitialSize(new Point(1440, 900));
		configurer.setShowMenuBar(true);
		configurer.setShowCoolBar(false);
		configurer.setShowStatusLine(true);
		//PlatformUI.getPreferenceStore().setValue(IWorkbenchPreferenceConstants.SHOW_MEMORY_MONITOR,	true);
		configurer.setShowProgressIndicator(true);
		//configurer.setShowFastViewBars(false);
		configurer.setShowPerspectiveBar(true);

		WorkspaceManager wm = WorkspaceManager.getInstance();
		String wsName = wm.getDisplayName(wm.getCurrentWorkspacePath());
		configurer.setTitle("Version - "+Version.getClientFullVersion() + " [" + wsName + "] (" + TimeZone.getDefault().getDisplayName() + ")");
	}

	@SuppressWarnings("restriction")
	public void postWindowOpen() {
        super.postWindowOpen();
        removeUnwantedMenus();
        checkGitHubSettings();
	}

    @SuppressWarnings("restriction")
    private void removeUnwantedMenus() {
        IWorkbenchWindow window = getWindowConfigurer().getWindow();
        if (window instanceof WorkbenchWindow) {
            MenuManager menuManager = ((WorkbenchWindow) window).getMenuManager();
            String[] idsToRemove = {
                    "org.eclipse.search.menu",
                    "org.eclipse.ui.run"
            };
            for (String id : idsToRemove) {
                IContributionItem item = menuManager.find(id);
                if (item != null) {
                    menuManager.remove(item);
                }
            }
            // Also remove by label for any remaining items
            for (IContributionItem item : menuManager.getItems()) {
                if (item instanceof MenuManager) {
                    String label = ((MenuManager) item).getMenuText();
                    if (label != null && (label.equals("Search") || label.equals("Run")
                            || label.equals("&Search") || label.equals("&Run"))) {
                        menuManager.remove(item);
                    }
                }
            }
            menuManager.update(true);
        }
    }

    @SuppressWarnings("restriction")
    private void removeUnwantedActionSets() {
        ActionSetRegistry reg = WorkbenchPlugin.getDefault().getActionSetRegistry();
        IActionSetDescriptor[] actionSets = reg.getActionSets();
        for (IActionSetDescriptor actionSet : actionSets) {
            String id = actionSet.getId();
            if (id.startsWith("org.eclipse.search") || id.startsWith("org.eclipse.ui.run")
                    || id.startsWith("org.eclipse.debug") || id.startsWith("org.eclipse.ui.externaltools")) {
                IExtension ext = actionSet.getConfigurationElement().getDeclaringExtension();
                reg.removeExtension(ext, new Object[]{actionSet});
            }
        }
        checkGitHubSettings();
    }

    private void checkGitHubSettings() {
        ExUtil.asyncRun(() -> {
            try {
                ThreadUtil.sleep(3000);
                if (ImportFromGitHubDialog.hasNewSettings()) {
                    Display.getDefault().asyncExec(() -> {
                        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
                        MessageDialog dialog = new MessageDialog(
                                shell,
                                "New Settings Available",
                                null,
                                "New workspace settings are available on GitHub.\nWould you like to import them now?",
                                MessageDialog.INFORMATION,
                                new String[] { "Import Now", "Remind Me Later", "Don't Ask for 30 Days" },
                                0);
                        int result = dialog.open();
                        if (result == 0) {
                            new ImportFromGitHubDialog(shell).open();
                        } else if (result == 2) {
                            ImportFromGitHubDialog.snooze30Days();
                        }
                    });
                }
            } catch (Exception e) {
                // ignore
            }
        });
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