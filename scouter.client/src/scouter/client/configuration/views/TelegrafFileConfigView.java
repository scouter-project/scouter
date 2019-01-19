/*
 *  Copyright 2015 the original author or authors.
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package scouter.client.configuration.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.net.TcpProxy;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.lang.pack.MapPack;
import scouter.net.RequestCmd;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.ModifyEvent;

public class TelegrafFileConfigView extends ViewPart {

    private int serverId;
    public static final String ID = "scouter.client.configuration.views.TelegrafFileConfigView"; //$NON-NLS-1$
    private Action saveAction;
    private Action reloadAction;
    private Composite mainContainer;
    private String m_tgConfigContents;
    private StyledText styledText;

    public TelegrafFileConfigView() {}

    /**
     * Create contents of the view part.
     *
     * @param parent
     */
    @Override
    public void createPartControl(Composite parent) {
        parent.setLayout(new FillLayout(SWT.HORIZONTAL));
        mainContainer = new Composite(parent, SWT.NONE);
        mainContainer.setLayout(new FillLayout(SWT.HORIZONTAL));
        {
        	styledText = new StyledText(mainContainer, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        	styledText.addModifyListener(new ModifyListener() {
        		public void modifyText(ModifyEvent e) {
        			m_tgConfigContents = styledText.getText();
        		}
        	});
            styledText.addKeyListener(new KeyListener() {
                public void keyReleased(KeyEvent e) {
                }

                public void keyPressed(KeyEvent e) {
                    if (e.stateMask == SWT.CTRL || e.stateMask == SWT.COMMAND) {
                        if (e.keyCode == 's' || e.keyCode == 'S') {
                            saveConfig();
                        } else if (e.keyCode == 'a' || e.keyCode == 'A') {
                            styledText.selectAll();
                        }
                    }
                }
            });
        }
        
        createActions();
        initializeToolBar();
        initializeMenu();
    }

    /**
     * Create the actions.
     */
    private void createActions() {
        {
            saveAction = new Action("Save", ImageUtil.getImageDescriptor(Images.save)) {
            	public void run() {
                    saveConfig();
                }
            };
            reloadAction = new Action("Reload", ImageUtil.getImageDescriptor(Images.refresh)) {
                public void run() {
                    loadConfig();
                }
            };
        }
    }

    /**
     * Initialize the toolbar.
     */
    private void initializeToolBar() {
        IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
        toolbarManager.add(reloadAction);
        toolbarManager.add(saveAction);
    }

    /**
     * Initialize the menu.
     */
    private void initializeMenu() {
        IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();
    }

    @Override
    public void setFocus() {
        // Set the focus
    }

    public void setInput(int serverId) {
        this.serverId = serverId;
        Server server = ServerManager.getInstance().getServer(serverId);
        if (server != null) {

            setPartName("Config Telegraf-File Server[" + server.getName() + "]");
            loadConfig();
        }
    }

    private void loadConfig() {
        ExUtil.asyncRun(new Runnable() {
            public void run() {
                MapPack mpack = null;
                TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
                try {
                    mpack = (MapPack) tcp.getSingle(RequestCmd.GET_CONFIGURE_TELEGRAF, null);
                } finally {
                    TcpProxy.putTcpProxy(tcp);
                }
                if (mpack != null) {
                    m_tgConfigContents = mpack.getText("tgConfigContents");
                }
                ExUtil.exec(mainContainer, new Runnable() {
                    public void run() {
                        styledText.setText(m_tgConfigContents);
                    }
                });
            }
        });
    }

    private void saveConfig() {
        TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
        try {
            String contents = m_tgConfigContents;
            MapPack param = new MapPack();
            param.put("tgConfigContents", contents);
            MapPack out = (MapPack) tcp.getSingle(RequestCmd.SET_CONFIGURE_TELEGRAF, param);

            if (out != null) {
                String config = out.getText("result");
                if ("true".equalsIgnoreCase(config)) {
                    MessageDialog.open(MessageDialog.INFORMATION
                            , PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()
                            , "Success"
                            , "Configuration saving is done."
                            , SWT.NONE);
                } else {
                    MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell()
                            , "Error"
                            , "Configuration saving is failed.");
                }
            }
        } catch (Exception e) {
            ConsoleProxy.errorSafe(e.toString());
        } finally {
            TcpProxy.putTcpProxy(tcp);
        }
    }
}
