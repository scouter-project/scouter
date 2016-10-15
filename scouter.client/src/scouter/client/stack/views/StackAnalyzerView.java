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
package scouter.client.stack.views;

import java.util.ArrayList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.stack.actions.MainProcessorAction;
import scouter.client.stack.actions.OpenXMLEditorAction;
import scouter.client.stack.base.MainProcessor;
import scouter.client.stack.base.PopupMenuListener;
import scouter.client.stack.data.StackAnalyzedInfo;
import scouter.client.stack.data.StackFileInfo;
import scouter.client.stack.utils.HtmlUtils;
import scouter.client.util.ImageUtil;

public class StackAnalyzerView extends ViewPart {
	
	public final static String ID = StackAnalyzerView.class.getName();
	
	private Tree m_mainTree = null;
	private Table m_table = null;
	private Browser m_browser = null;

	public void createPartControl(Composite parent) {
		this.setPartName("SFA");
		MainProcessor mainProcessor = MainProcessor.instance();
		mainProcessor.setStackAnalyzerView(this);
		mainProcessor.setParentComposite(parent);
		initializeToobar();
		
		SashForm sashVertForm = new SashForm(parent, SWT.VERTICAL);
		sashVertForm.SASH_WIDTH = 3;
		SashForm sashHoriForm = new SashForm(sashVertForm, SWT.HORIZONTAL);
		sashHoriForm.SASH_WIDTH = 3;
		initializeTree(sashHoriForm);		
		initializeTable(sashHoriForm);
		initializeBrowser(sashVertForm);
		sashVertForm.setWeights(new int [] {55, 45});
		sashHoriForm.setWeights(new int [] {45, 55});
		
		createMainTreePopupMenu();
		createTablePopupMenu();

		parent.setVisible(true);

	}
	
	private void initializeTree(Composite parent){		
		m_mainTree = new Tree(parent, SWT.BORDER | SWT.FULL_SELECTION);
		m_mainTree.setLinesVisible(true);
		TreeColumn treeColumn = new TreeColumn(m_mainTree, SWT.LEFT);
		treeColumn.setAlignment(SWT.LEFT);
		treeColumn.setText("Division");
		treeColumn.setWidth(300);
		treeColumn = new TreeColumn(m_mainTree, SWT.RIGHT);
		treeColumn.setAlignment(SWT.RIGHT);
		treeColumn.setText("Count");
		treeColumn.setWidth(120);
		treeColumn = new TreeColumn(m_mainTree, SWT.LEFT);
		treeColumn.setAlignment(SWT.RIGHT);
		treeColumn.setText("Percent %");
		treeColumn.setWidth(100);
		
		m_mainTree.addSelectionListener(new SelectionListener(){
			public void widgetDefaultSelected(SelectionEvent event) {
			}
			public void widgetSelected(SelectionEvent event) {
				TreeItem item = (TreeItem)event.item;
				
				if(item == null)
					return;
				
                Object object = item.getData();
                MainProcessor mainProcessor = MainProcessor.instance();
                if ( object instanceof StackAnalyzedInfo ) {
                	mainProcessor.setTable((StackAnalyzedInfo)object);
                } else if ( object instanceof StackFileInfo ) {
                    mainProcessor.displayContent(HtmlUtils.getStackFileInfo((StackFileInfo)object));
                }
            }			
		});
		
		m_mainTree.setHeaderVisible(true);
		m_mainTree.setLinesVisible(true);
	}

	private void initializeTable(Composite parent){
		m_table = new Table(parent, SWT.BORDER  | SWT.FULL_SELECTION);
		TableColumn tableColumn = new TableColumn(m_table, SWT.RIGHT);
		tableColumn.setText("Count");
		tableColumn.setWidth(80);
		tableColumn = new TableColumn(m_table, SWT.RIGHT);
		tableColumn.setText("Internl %");
		tableColumn.setWidth(100);
		tableColumn = new TableColumn(m_table, SWT.RIGHT);
		tableColumn.setText("External %");
		tableColumn.setWidth(100);
		tableColumn = new TableColumn(m_table, SWT.LEFT);
		tableColumn.setText("Class.Method");
		tableColumn.setWidth(600);

		m_table.addSelectionListener(new SelectionListener(){
			public void widgetDefaultSelected(SelectionEvent event) {
			}
			@SuppressWarnings("unchecked")
			public void widgetSelected(SelectionEvent event) {
				TableItem item = (TableItem)event.item;
				
				if(item == null)
					return;
				
				Object object = item.getData();
				if(object == null){
					return;
				}
                MainProcessor.instance().displayContent(HtmlUtils.getUniqueStack((ArrayList<String>)object));
            }			
		});
		
		m_table.setHeaderVisible(true);
		m_table.setVisible(true);
	}
	
	private void initializeBrowser(Composite parent){
		m_browser = new Browser(parent, SWT.BORDER);
		GridData browserGrid = new GridData();
		browserGrid.horizontalSpan = 2;
		m_browser.setLayoutData(browserGrid);
		m_browser.setSize(800, 400);
        MainProcessor.instance().displayContent(null);
	}
	
	private void initializeToobar(){
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		IWorkbenchWindow win = PlatformUI.getWorkbench().getActiveWorkbenchWindow();		
		
		man.add(new Action("open local stack log", ImageUtil.getImageDescriptor(Images.folder)) {
			public void run() {
				new MainProcessorAction("Open Stack Log").run();
			}
		});

		man.add(new Action("open analyzed stack log", ImageUtil.getImageDescriptor(Images.folder_star)) {
			public void run() {
				new MainProcessorAction("Open Analyzed Stack Log").run();
			}
		});
		
		man.add(new Action("close all stack log", ImageUtil.getImageDescriptor(Images.close_folder)) {
			public void run() {
				new MainProcessorAction("Close All").run();
			}
		});
		man.add(new Separator());
		man.add(new Action("select local parser configuration", ImageUtil.getImageDescriptor(Images.config)) {
			public void run() {
				new MainProcessorAction("Select Parser Configuration").run();
			}
		});
		
		man.add(new OpenXMLEditorAction(win, "Edit parser configuration", ImageUtil.getImageDescriptor(Images.config_edit)));
	}
	
	private void createMainTreePopupMenu(){
		Menu popupMenu = new Menu(m_mainTree);
		
		PopupMenuListener listener = new PopupMenuListener();
		
		MenuItem menuItem = new MenuItem(popupMenu, SWT.NONE);
		menuItem.setText("Performance Tree");
		menuItem.addListener(SWT.Selection, listener);
		menuItem = new MenuItem(popupMenu, SWT.NONE);
		menuItem.setText("Reanalyze");
		menuItem.addListener(SWT.Selection, listener);
		menuItem = new MenuItem(popupMenu, SWT.NONE);
		menuItem.setText("Close");
		menuItem.addListener(SWT.Selection, listener);
		
		menuItem = new MenuItem(popupMenu, SWT.SEPARATOR);
		
		menuItem = new MenuItem(popupMenu, SWT.NONE);
		menuItem.setText("View Raw Index File");
		menuItem.addListener(SWT.Selection, listener);

		menuItem = new MenuItem(popupMenu, SWT.SEPARATOR);

		menuItem = new MenuItem(popupMenu, SWT.CASCADE);
		menuItem.setText("Manual Analysis");
		
		Menu subMenu = new Menu(popupMenu);
		menuItem.setMenu(subMenu);

		menuItem = new MenuItem(subMenu, SWT.NONE);
		menuItem.setText("Manual Performance Tree(Ascending)");
		menuItem.addListener(SWT.Selection, listener);
		menuItem = new MenuItem(subMenu, SWT.NONE);
		menuItem.setText("Manual Performance Tree(Descending)");
		menuItem.addListener(SWT.Selection, listener);
		menuItem = new MenuItem(subMenu, SWT.NONE);
		menuItem.setText("Manual Service Call");
		menuItem.addListener(SWT.Selection, listener);
		menuItem = new MenuItem(subMenu, SWT.NONE);
		menuItem.setText("Manual Stack Analyze(Include)");
		menuItem.addListener(SWT.Selection, listener);
		menuItem = new MenuItem(subMenu, SWT.NONE);
		menuItem.setText("Manual Stack Analyze(Exclude)");
		menuItem.addListener(SWT.Selection, listener);
		menuItem = new MenuItem(subMenu, SWT.NONE);
		menuItem.setText("Manual Thread Stack (max 20000 lines)");
		menuItem.addListener(SWT.Selection, listener);
		
		menuItem = new MenuItem(popupMenu, SWT.CASCADE);
		menuItem.setText("View Option");
		
		subMenu = new Menu(popupMenu);
		menuItem.setMenu(subMenu);

		MainProcessor mainProcessor = MainProcessor.instance();
		menuItem = new MenuItem(subMenu, SWT.CHECK);
		menuItem.setText("Exclude Stack");
		menuItem.setSelection(mainProcessor.isExcludeStack());
		menuItem.addListener(SWT.Selection, listener);
		menuItem = new MenuItem(subMenu, SWT.CHECK);
		menuItem.setText("Remove Line(Performance Tree)");
		menuItem.setSelection(mainProcessor.isRemoveLine());
		menuItem.addListener(SWT.Selection, listener);

		menuItem = new MenuItem(subMenu, SWT.SEPARATOR);

		menuItem = new MenuItem(subMenu, SWT.CHECK);
		menuItem.setText("Sort by Function");
		menuItem.setSelection(mainProcessor.isSortByFunction());
		menuItem.addListener(SWT.Selection, listener);

		menuItem = new MenuItem(subMenu, SWT.SEPARATOR);

		menuItem = new MenuItem(subMenu, SWT.CHECK);
		menuItem.setText("Simple Dump Time List");
		menuItem.setSelection(mainProcessor.isSimpleDumpTimeList());
		menuItem.addListener(SWT.Selection, listener);

		menuItem = new MenuItem(popupMenu, SWT.SEPARATOR);

		menuItem = new MenuItem(popupMenu, SWT.CHECK);
		menuItem.setText("Use Default Parser Configuration");
		menuItem.setSelection(mainProcessor.isDefaultConfiguration());
		menuItem.addListener(SWT.Selection, listener);

		menuItem = new MenuItem(popupMenu, SWT.CHECK);
		menuItem.setText("Analyze All Threads In Stack(No Filter)");
		menuItem.setSelection(mainProcessor.isAnalyzeAllThreads());
		menuItem.addListener(SWT.Selection, listener);
		
		m_mainTree.setMenu(popupMenu);
	}
	
	private void createTablePopupMenu(){
		Menu popupMenu = new Menu(m_table);
		
		PopupMenuListener listener = new PopupMenuListener();
		
		MenuItem menuItem = new MenuItem(popupMenu, SWT.NONE);
		menuItem.setText("Performance Tree(Ascending)");
		menuItem.addListener(SWT.Selection, listener);
		menuItem = new MenuItem(popupMenu, SWT.NONE);
		menuItem.setText("Performance Tree(Descending)");
		menuItem.addListener(SWT.Selection, listener);

		menuItem = new MenuItem(popupMenu, SWT.SEPARATOR);
		
		menuItem = new MenuItem(popupMenu, SWT.NONE);
		menuItem.setText("View Thread Stack (max 20000 lines)");
		menuItem.addListener(SWT.Selection, listener);
		menuItem = new MenuItem(popupMenu, SWT.NONE);
		menuItem.setText("View Service Call");
		menuItem.addListener(SWT.Selection, listener);
		
		menuItem = new MenuItem(popupMenu, SWT.SEPARATOR);
		
		menuItem = new MenuItem(popupMenu, SWT.NONE);
		menuItem.setText("Filter Stack Analyze");
		menuItem.addListener(SWT.Selection, listener);
		menuItem = new MenuItem(popupMenu, SWT.NONE);
		menuItem.setText("Copy Function");
		menuItem.addListener(SWT.Selection, listener);

		m_table.setMenu(popupMenu);
	}
	
	public void setFocus() {
	}
	
	public Tree getMainTree(){
		return m_mainTree;
	}
	
	public Table getTable(){
		return m_table;
	}
	
	public Browser getBrowser(){
		return m_browser;
	}
}
