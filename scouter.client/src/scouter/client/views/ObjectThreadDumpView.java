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
package scouter.client.views;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.model.TextProxy;
import scouter.client.net.TcpProxy;
import scouter.client.stack.actions.FetchSingleStackJob;
import scouter.client.stack.base.MainProcessor;
import scouter.client.util.ColoringWord;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.CustomLineStyleListener;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
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
	private String objName;
	private int objHash;
	private int serverId;
	private boolean isTable = true;
	
	private Table table;
	private String indexFilename = null;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secondId = site.getSecondaryId();
		if (secondId != null) {
			String[] tokens = StringUtil.tokenizer(secondId, "&");
			this.objHash = CastUtil.cint(tokens[0]);
			this.isTable = false;
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
	Button findButton, analyzeBtn;
	Text searchText;
	public void createUpperMenu(Composite composite){
		headerComp = new Composite(composite, SWT.NONE);
		headerComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		headerComp.setLayout(UIUtil.formLayout(0, 0));
		
		analyzeBtn = new Button(headerComp, SWT.PUSH);
		analyzeBtn.setLayoutData(UIUtil.formData(null, -1, 0, 2, 100, -5, null, -1));
		analyzeBtn.setText("SFA");
		analyzeBtn.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					MainProcessor.instance().processStackContents(text.getText());
					break;
				}
			}
		});
		
		findButton = new Button(headerComp, SWT.PUSH);
		findButton.setLayoutData(UIUtil.formData(null, -1, 0, 2, analyzeBtn, -3, null, -1));
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
		
		SashForm sashHoriForm = null;
		if(isTable){
			sashHoriForm = new SashForm(textComposite, SWT.HORIZONTAL);
			sashHoriForm.SASH_WIDTH = 3;
			
			Composite tableComposite = new Composite(sashHoriForm, SWT.NONE);
			table = new Table(tableComposite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.FULL_SELECTION);
			table.setLinesVisible(true);
			table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			table.addSelectionListener(new SelectionListener() {
				public void widgetSelected(SelectionEvent e) {
					TableItem[] items = table.getSelection();
					if (items != null && items.length > 0) {
						TableItem first = items[0];
						TableItem last = items[items.length - 1];
					}
				}
				public void widgetDefaultSelected(SelectionEvent e) {}
			});
			
			table.addMouseListener(new MouseListener(){
				public void mouseDoubleClick(MouseEvent e) {
				}
	
				public void mouseDown(MouseEvent e) {
				}
				
				public void mouseUp(MouseEvent e) {
					TableItem[] items = table.getSelection();
					if(items == null){
						return;
					}
					if(serverId != 0){
						long from = (Long) items[0].getData();
						new FetchSingleStackJob(serverId, objName, from, null, ObjectThreadDumpView.this).schedule(500);
					}else{
						loadBatchStackContents((Long) items[0].getData(), Integer.parseInt(items[0].getText()));
					}
				}
			});
			
			TableColumn indexColumn = new TableColumn(table, SWT.NONE);
			TableColumn timeColumn = new TableColumn(table, SWT.NONE);
			TableColumnLayout tableColumnLayout = new TableColumnLayout();
			tableColumnLayout.setColumnData(indexColumn, new ColumnWeightData(20));
			tableColumnLayout.setColumnData(timeColumn, new ColumnWeightData(80));
			tableComposite.setLayout(tableColumnLayout);
			
			
			text = new StyledText(sashHoriForm, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		}else{
			text = new StyledText(textComposite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);			
		}
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
		if(sashHoriForm != null){
			sashHoriForm.setWeights(new int [] {10, 40});
		}
	}

	public void setInput(int serverId){
		this.serverId = serverId;
		this.setPartName("ThreadDump[" + TextProxy.object.getLoadText(DateUtil.yyyymmdd(TimeUtil.getCurrentTime(serverId)), objHash, serverId) + "]");
		load();
	}
	
	public void setInput(String stackText){
		text.setText(stackText);
	}
	
	public void setInput(String stackText, String objName, int serverId, long stackTime, List<Long> list){
		this.setPartName("ThreadDump[" + objName + " " + DateUtil.yyyymmdd(stackTime) + "]");
		this.objName = objName;
		this.serverId = serverId;
		text.setText(stackText);
		loadAgentStackList(list);
	}
	
	public void setInput(String objName, String filename, List<Long> [] lists){
		this.setPartName("ThreadDump[" + objName + " " + DateUtil.yyyymmdd(lists[0].get(0)) + "]");
		this.objName = objName;
		this.indexFilename = filename;
		loadBatchStackList(lists);
		if(lists != null){
			if(lists[1].size() == 1){
				loadThreadDump(0, 0);
			}else{
				loadThreadDump(0, ((Long)lists[1].get(1)).intValue());				
			}		}
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

	private void loadAgentStackList(final List<Long> list){
		if(list == null){
			return;
		}
		ExUtil.exec(table, new Runnable() {
			public void run() {
				long value;
				for (int i = 0 ; i < list.size(); i++) {
					value = list.get(i);
					TableItem item = new TableItem(table, SWT.NONE);
					item.setText(0, String.valueOf(i+1));
					item.setText(1, DateUtil.format(value, "yyyy-MM-dd HH:mm:ss"));
					item.setData(value);
				}
			}
		});		
	}
	
	private void loadBatchStackList(final List<Long> [] lists){
		if(lists == null){
			return;
		}
		ExUtil.exec(table, new Runnable() {
			public void run() {
				List<Long> time = lists[0];
				List<Long> position = lists[1];
				for (int i = 0 ; i < time.size(); i++) {
					TableItem item = new TableItem(table, SWT.NONE);
					item.setText(0, String.valueOf(i+1));
					item.setText(1, DateUtil.format(time.get(i), "yyyy-MM-dd HH:mm:ss"));
					item.setData(position.get(i));
				}
			}
		});		
	}	
	public void setFocus() {
	}

	private void loadThreadDump(long position, int length){
		File stackFile = null;
		if(this.indexFilename == null){
			return;
		}else{
			stackFile = new File(this.indexFilename.substring(0, this.indexFilename.length() - 4) + ".log");
		}
		
		String contents = "";
		RandomAccessFile afr = null;
		try {
			if(length == 0){
				length = (int)(stackFile.length() - position);
			}
			afr = new RandomAccessFile(stackFile, "r");
			afr.seek(position);
			byte [] buffer = new byte[length];
			int totalSize= 0;
			int readSize;
			while((readSize = afr.read(buffer, totalSize, length-totalSize)) > 0){
				totalSize += readSize;
				if(totalSize >= length){
					break;
				}
			}
			contents = new String(buffer, "UTF-8");
		}catch(Exception ex){
			ex.printStackTrace();
		}finally{
			if(afr != null){
				try {afr.close(); }catch(Exception ex){}
			}
		}
		text.setText(contents);
	}
	
	private void loadBatchStackContents(long position, int index){
		TableItem item = null;
		if(index < table.getItemCount()){
			item = table.getItem(index);
		}
		int length = 0;
		if(item != null){
			length = ((Long)item.getData()).intValue();
			length = (int)(length - position);
		}
		loadThreadDump(position, length);		
	}
}
