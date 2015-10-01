/*
 *  Copyright 2015 the original author or authors.
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
package scouter.client.views;

import java.util.ArrayList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.model.TextProxy;
import scouter.client.net.TcpProxy;
import scouter.client.util.ColoringWord;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.CustomLineStyleListener;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.ScouterUtil;
import scouter.client.util.TimeUtil;
import scouter.client.util.UIUtil;
import scouter.lang.pack.MapPack;
import scouter.lang.value.ListValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.StringUtil;

public class ObjectThreadDumpView extends ViewPart {

	public final static String ID = ObjectThreadDumpView.class.getName();
	
	private ArrayList<ColoringWord> defaultHighlightings;
	
	private StyledText text;
	private int objHash;
	private int serverId;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secondId = site.getSecondaryId();
		if (secondId != null) {
			String[] tokens = StringUtil.tokenizer(secondId, "&");
			this.objHash = CastUtil.cint(tokens[0]);
		}
		initializeColoring();
	}
	
	public void initializeColoring(){
		defaultHighlightings = new ArrayList<ColoringWord>();
		
		defaultHighlightings.add(new ColoringWord("java.lang.Thread.State:", SWT.COLOR_BLUE, false));
		defaultHighlightings.add(new ColoringWord("Object.wait()", SWT.COLOR_BLUE, false));
		defaultHighlightings.add(new ColoringWord("daemon", SWT.COLOR_BLUE, false));
		defaultHighlightings.add(new ColoringWord("java.util", SWT.COLOR_BLUE, false));
		defaultHighlightings.add(new ColoringWord("prio", SWT.COLOR_BLUE, false));
		defaultHighlightings.add(new ColoringWord("org.apache", SWT.COLOR_BLUE, false));
		
		defaultHighlightings.add(new ColoringWord("locked", SWT.COLOR_RED, false));
		
		defaultHighlightings.add(new ColoringWord("java.lang.Thread.run", SWT.COLOR_DARK_GREEN, false));
		
		defaultHighlightings.add(new ColoringWord("java.lang", SWT.COLOR_DARK_MAGENTA, false));
		
		defaultHighlightings.add(new ColoringWord("waiting on", SWT.COLOR_DARK_RED, false));
	}

	Composite headerComp;
	Button findButton;
	Text searchText;
	public void createUpperMenu(Composite composite){
		headerComp = new Composite(composite, SWT.NONE);
		headerComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		headerComp.setLayout(UIUtil.formLayout(0, 0));
		
		findButton = new Button(headerComp, SWT.PUSH);
		findButton.setLayoutData(UIUtil.formData(null, -1, 0, 2, 100, -5, null, -1));
		findButton.setText("Find");
		findButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					listener.setSearchString(searchText.getText());
			        text.redraw();
				}
			}
		});
		
		searchText = new Text(headerComp, SWT.BORDER);
		searchText.setLayoutData(UIUtil.formData(null, -1, 0, 5, findButton, -5, null, -1, 300));
		searchText.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if(e.keyCode == 13){ // enter key
					listener.setSearchString(searchText.getText());
			        text.redraw();
				}
			}
			public void keyReleased(KeyEvent e) {}
		});
		
	}
	
	Shell shell;
	
	boolean loaded = false;
	
	CustomLineStyleListener listener;
	
	public void createPartControl(final Composite parent) {
		shell = parent.getShell();

		parent.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {
			}
			public void keyPressed(KeyEvent e) {
				
				
			}
		});
		
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout gLayout = new GridLayout(1, true);
		gLayout.horizontalSpacing = 0;
		gLayout.marginHeight = 0;
		gLayout.marginWidth = 0;
		composite.setLayout(gLayout);
		createUpperMenu(composite);
		
		Composite textComposite = new Composite(composite, SWT.NONE);
		textComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		textComposite.setLayout(new FillLayout());
		text = new StyledText(textComposite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		
		text.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {
			}
			public void keyPressed(KeyEvent e) {
				if(e.stateMask == SWT.CTRL){
					if(e.keyCode == 'a'){
						text.selectAll();
					}else if(e.keyCode == 'f'){
						searchText.setFocus();
						searchText.selectAll();
					}
				}
				
			}
		});
		listener = new CustomLineStyleListener(false, defaultHighlightings, false, true, SWT.COLOR_YELLOW);
		text.addLineStyleListener(listener);
		
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("Copy", ImageUtil.getImageDescriptor(Images.copy)) {
			public void run() {
				Clipboard clipboard = new Clipboard(Display.getDefault());
	            clipboard.setContents(new Object[] { text.getText() }, new Transfer[] { TextTransfer.getInstance() });
	            clipboard.dispose();
	            MessageDialog.openInformation(parent.getShell(), "Copied", "Copy all contents to clipboard");
			}
		});
	}

	public void setInput(int serverId){
		this.serverId = serverId;
		this.setPartName("ThreadDump[" + TextProxy.object.getLoadText(DateUtil.yyyymmdd(TimeUtil.getCurrentTime(serverId)), objHash, serverId) + "]");
		load();
	}
	
	public void load() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				MapPack mpack = null;
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				try {
					MapPack param = new MapPack();
					param.put("objHash", objHash);
					mpack = (MapPack) tcp.getSingle(RequestCmd.OBJECT_THREAD_DUMP, param);
				} catch(Exception e){
					ConsoleProxy.errorSafe(e.toString());
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				
				if (mpack != null) {
					String error = mpack.getText("error");
					if (error != null) {
						ConsoleProxy.errorSafe(error);
					}
					final ListValue lv = mpack.getList("threadDump");
					
					ExUtil.exec(text, new Runnable() {
						public void run() {
							StringBuilder sb = new StringBuilder();
							for (int i = 0; i < lv.size(); i++) {
								sb.append(lv.getString(i) + "\n");
							}
							text.setText(sb.toString());
						}
					});
				}
			}
		});
	}

	public void setFocus() {
	}

}
