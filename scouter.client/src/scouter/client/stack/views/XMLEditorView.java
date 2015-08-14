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

import java.util.ArrayList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ColoringWord;
import scouter.client.util.ImageUtil;
import scouter.client.util.CustomLineStyleListener;

public class XMLEditorView extends ViewPart {
	public final static String ID = XMLEditorView.class.getName();
	
	private ArrayList<ColoringWord> defaultHighlightings;
	
	private StyledText text;
	private String serverConfig;
	
	private Clipboard clipboard = new Clipboard(null);
	
	CustomLineStyleListener listener;
	
	public void createPartControl(Composite parent) {
		text = new StyledText(parent, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		listener = new CustomLineStyleListener(true, defaultHighlightings, false);
		text.addLineStyleListener(listener);
		text.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {
			}
			public void keyPressed(KeyEvent e) {
				if(e.stateMask == SWT.CTRL){
					if(e.keyCode == 's'){
						saveConfigurations();
					}else if(e.keyCode == 'a'){
						text.selectAll();
					}
				}
			}
		});
		
		initialToolBar();
	}

	public void setInput(int serverId){
		Server server = ServerManager.getInstance().getServer(serverId);
		if (server != null) {
			setPartName("Config Server[" + server.getName() + "]");
			loadConfig();
		}
	}
	
	private void saveAsConfigurations(){
	}

	private void saveConfigurations(){
	}
	
	private void initialToolBar() {
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("Save", ImageUtil.getImageDescriptor(Images.save)) {
			public void run() {
				saveConfigurations();
			}
		});
		man.add(new Action("SaveAs", ImageUtil.getImageDescriptor(Images.saveas)) {
			public void run() {
				saveConfigurations();
			}
		});
	}

	private void loadConfig() {
	}
	
	public void setFocus() {
		IStatusLineManager slManager= getViewSite().getActionBars().getStatusLineManager();
		slManager.setMessage("CTRL + S : save configurations, CTRL + A : select all text");
	}

}

