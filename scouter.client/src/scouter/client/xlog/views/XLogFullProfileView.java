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
package scouter.client.xlog.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import scouter.client.Activator;
import scouter.client.Images;
import scouter.client.model.TextProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.MyKeyAdapter;
import scouter.client.util.StepWrapper;
import scouter.client.util.UIUtil;
import scouter.client.util.UIUtil.XLogViewWithTable;
import scouter.client.xlog.SaveProfileJob;
import scouter.client.xlog.XLogUtil;
import scouter.lang.pack.XLogPack;
import scouter.lang.step.ApiCallStep;
import scouter.lang.step.ApiCallSum;
import scouter.lang.step.DispatchStep;
import scouter.lang.step.MessageStep;
import scouter.lang.step.MethodStep;
import scouter.lang.step.MethodSum;
import scouter.lang.step.SqlStep;
import scouter.lang.step.SqlSum;
import scouter.lang.step.Step;
import scouter.lang.step.StepEnum;
import scouter.lang.step.StepSingle;
import scouter.lang.step.StepSummary;
import scouter.lang.step.ThreadCallPossibleStep;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.Hexa32;
import scouter.util.StringUtil;
import scouter.util.SystemUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;


public class XLogFullProfileView extends ViewPart implements XLogViewWithTable {
	public static final String ID = XLogFullProfileView.class.getName();
	private StyledText header, text;
	Button prevBtn, nextBtn, endBtn, startBtn, summaryBtn;
	Text targetPage;
	Label totalPage, totalProfile;
	
	
	@SuppressWarnings("unused")
	private IMemento memento;
	
	IToolBarManager man;
	Action spaceNarrow, spaceWide;
	
	int pageNum = 0;
	int rowPerPage = 30;
	
	SashForm sashForm;
	Action showSearchAreaBtn;
	Composite mainComposite;
	
	String[] titles = {"Page", "Profile"};
	
	Text searchText;
	Table searchResultTable;
	Label searchLbl;
	ProgressBar searchProg;
	Button searchBtn, stopBtn;
	
	Text searchFromTxt, searchToTxt;
	Text maxCountTxt;
	
	MyKeyAdapter adapter;
	
	private final int SORT_WITH_COUNT = 100;
	private final int SORT_WITH_SUM   = 200;
	private final int SORT_WITH_AVG   = 300;
	private int CURRENT_SORT_CRITERIA = SORT_WITH_COUNT;
	
	Button sortCountBtn, sortSumBtn, sortAvgBtn;
	
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		this.memento = memento;
	}

	public void createPartControl(Composite parent) {
		
		adapter = new MyKeyAdapter(parent.getShell(), "SelectedLog.txt");
		
		man = getViewSite().getActionBars().getToolBarManager();
		
		sashForm = new SashForm(parent, SWT.HORIZONTAL);
		sashForm.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_GRAY));
		sashForm.SASH_WIDTH = 1;
		
		mainComposite = new Composite(sashForm, SWT.NONE);
		mainComposite.setLayout(new GridLayout(1, true));
		
		header = new StyledText(mainComposite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.WRAP );
		GridData bodyData = new GridData(GridData.FILL, GridData.FILL, true, false);
		bodyData.heightHint = 100;
		header.setLayoutData(bodyData);
		header.setText("");
		if(SystemUtil.IS_MAC_OSX){
			header.setFont(new Font(null, "Courier New", 12, SWT.NORMAL));		
		}else{
			header.setFont(new Font(null, "Courier New", 10, SWT.NORMAL));
		}
		header.setBackgroundImage(Activator.getImage("icons/grid.jpg"));
		

		Composite centerComp = new Composite(mainComposite, SWT.NONE);
		bodyData = new GridData(GridData.FILL, GridData.FILL, true, false);
		bodyData.heightHint = 55;
		centerComp.setLayoutData(bodyData);
		centerComp.setLayout(UIUtil.formLayout(5, 5));
		
		summaryBtn = new Button(centerComp, SWT.TOGGLE);
		summaryBtn.setText("Summary");
		summaryBtn.setLayoutData(UIUtil.formData(null, -1, null, -1, 100, 0, null, -1));
		summaryBtn.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				Button b = (Button) e.widget;
				if(b.getSelection()){
					getSummary();
				}else{
					pageNum = 0;
					setProfile();
				}
			}
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		endBtn = new Button(centerComp, SWT.NONE);
		endBtn.setImage(Images.toend);
		endBtn.setText("");
		endBtn.setLayoutData(UIUtil.formData(null, -1, null, -1, summaryBtn, 0, null, -1, 40));
		endBtn.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				pageNum = total - 1;
				targetPage.setText(""+(pageNum + 1));
				setProfile();
			}
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
	    nextBtn = new Button(centerComp, SWT.NONE);
	    nextBtn.setImage(Images.tonext);
	    nextBtn.setText("Next");
		nextBtn.setLayoutData(UIUtil.formData(null, -1, null, -1, endBtn, 0, null, -1, 80));
		nextBtn.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				pageNum++;
				targetPage.setText(""+(pageNum + 1));
				setProfile();
			}
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		prevBtn = new Button(centerComp, SWT.NONE);
		prevBtn.setImage(Images.toprev);
		prevBtn.setText("Before");
		prevBtn.setLayoutData(UIUtil.formData(null, -1, null, -1, nextBtn, 0, null, -1, 80));
	    prevBtn.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				pageNum--;
				targetPage.setText(""+(pageNum + 1));
				setProfile();
			}
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
	    startBtn = new Button(centerComp, SWT.NONE);
	    startBtn.setImage(Images.tostart);
	    startBtn.setText("");
		startBtn.setLayoutData(UIUtil.formData(null, -1, null, -1, prevBtn, 0, null, -1, 40));
		startBtn.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				pageNum = 0;
				targetPage.setText(""+(pageNum + 1));
				setProfile();
			}
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
	    
	    totalPage = new Label(centerComp, SWT.NONE);
		totalPage.setLayoutData(UIUtil.formData(null, -1, 0, 3, startBtn, 0, null, -1, 60));
	    
		totalProfile = new Label(centerComp, SWT.NONE);
		totalProfile.setLayoutData(UIUtil.formData(0, 0, 0, 3, null, -1, null, -1, 100));
		
	    targetPage = new Text(centerComp, SWT.BORDER | SWT.RIGHT);
		targetPage.setLayoutData(UIUtil.formData(null, -1, null, -1, totalPage, 0, null, -1, 60));
		targetPage.addListener(SWT.Traverse, new Listener() {
			public void handleEvent(Event event) {
				if (event.detail == SWT.TRAVERSE_RETURN) {
					pageNum = CastUtil.cint(targetPage.getText()) - 1;
					if(pageNum > total){
						pageNum = total - 1;
					}
					targetPage.setText(""+(pageNum + 1));
					setProfile();
				}
			}
		});
		targetPage.addVerifyListener(new VerifyListener() { 
	        public void verifyText(VerifyEvent e) {
	            Text text = (Text)e.getSource();
	            final String oldS = text.getText();
	            String newS = oldS.substring(0, e.start) + e.text + oldS.substring(e.end);
	            boolean isFloat = true;
				try {
					Float.parseFloat(newS);
				} catch (NumberFormatException ex) {
					isFloat = false;
				}
	            if(!isFloat)
	                e.doit = false;
	        }
	    });
		
		sortCountBtn = new Button(centerComp, SWT.NONE);
		sortCountBtn.setText("Count");
		sortCountBtn.setLayoutData(UIUtil.formData(0, 2, totalProfile, 8, null, -1, null, -1, 70));
		sortCountBtn.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				CURRENT_SORT_CRITERIA = SORT_WITH_COUNT;
				getSummary();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		sortSumBtn = new Button(centerComp, SWT.NONE);
		sortSumBtn.setText("Sum");
		sortSumBtn.setLayoutData(UIUtil.formData(sortCountBtn, 20, totalProfile, 8, null, -1, null, -1, 70));
		sortSumBtn.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				CURRENT_SORT_CRITERIA = SORT_WITH_SUM;
				getSummary();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		sortAvgBtn = new Button(centerComp, SWT.NONE);
		sortAvgBtn.setText("Avg");
		sortAvgBtn.setLayoutData(UIUtil.formData(sortSumBtn, 20, totalProfile, 8, null, -1, null, -1, 70));
		sortAvgBtn.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				CURRENT_SORT_CRITERIA = SORT_WITH_AVG;
				getSummary();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		
		
		text = new StyledText(mainComposite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		bodyData = new GridData(GridData.FILL, GridData.FILL, true, true); 
		text.setLayoutData(bodyData);
		text.setText("");
		if(SystemUtil.IS_MAC_OSX){
		    text.setFont(new Font(null, "Courier New", 12, SWT.NORMAL));		
		}else{
		    text.setFont(new Font(null, "Courier New", 10, SWT.NORMAL));
		}
		text.setBackgroundImage(Activator.getImage("icons/grid.jpg"));
		text.addKeyListener(adapter);
		text.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {
			}
			
			public void keyPressed(KeyEvent e) {
				if (e.stateMask == SWT.CTRL) {
					if (e.keyCode == 'f') {
						showSearchAreaBtn.setChecked(true);
						sashForm.setMaximizedControl(null);
		        		searchText.setFocus();
					}
				}
			}
		});
		
		// RIGHT
		Composite searchComposite = new Composite(sashForm, SWT.NONE);
		GridLayout searchLayout = new GridLayout(2, true);
		searchComposite.setLayout(searchLayout);
		
		
		
		Composite searchArea = new Composite(searchComposite, SWT.NONE);
		searchArea.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
		searchArea.setLayout(UIUtil.formLayout(1, 1));
		

		Label pageLbl = new Label(searchArea, SWT.NONE);
		pageLbl.setText("Page:");
		pageLbl.setLayoutData(UIUtil.formData(0, 2, 0, 2, null, -1, null, -1, 70));
		searchFromTxt = new Text(searchArea, SWT.BORDER | SWT.RIGHT);
		searchFromTxt.setLayoutData(UIUtil.formData(pageLbl, 2, 0, 0, null, -1, null, -1, 45));
		Label fromToLbl = new Label(searchArea, SWT.NONE);
		fromToLbl.setText("~");
		fromToLbl.setLayoutData(UIUtil.formData(searchFromTxt, 2, 0, 2, null, -1, null, -1));
		searchToTxt = new Text(searchArea, SWT.BORDER | SWT.RIGHT);
		searchToTxt.setLayoutData(UIUtil.formData(fromToLbl, 2, 0, 0, 100, -5, null, -1));
		

		Label maxLbl = new Label(searchArea, SWT.NONE);
		maxLbl.setText("Max Count:");
		maxLbl.setLayoutData(UIUtil.formData(0, 2, searchToTxt, 5, null, -1, null, -1, 70));
		maxCountTxt = new Text(searchArea, SWT.BORDER | SWT.RIGHT);
		maxCountTxt.setLayoutData(UIUtil.formData(maxLbl, 2, searchToTxt, 3, 100, -5, null, -1));
		
		stopBtn = new Button(searchArea, SWT.NONE);
//		stopBtn.setImage(Images.stop);
		stopBtn.setText("Stop");
		stopBtn.setLayoutData(UIUtil.formData(null, -1, maxCountTxt, 5, 100, -5, null, -1));
		stopBtn.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				breakSearch = true;
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		searchBtn = new Button(searchArea, SWT.NONE);
		searchBtn.setText("search");		
		searchBtn.setLayoutData(UIUtil.formData(null, -1, maxCountTxt, 5, stopBtn, -5, null, -1));
		searchBtn.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				searchWithNewText();
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		searchText = new Text(searchArea, SWT.BORDER);
		searchText.setLayoutData(UIUtil.formData(0, 2, maxCountTxt, 7, searchBtn, -5, null, -1));
		searchText.addKeyListener(new KeyListener() {
			public void keyReleased(KeyEvent e) {
				if(e.character == SWT.CR){
					searchWithNewText();
				}
			}
			public void keyPressed(KeyEvent e) {
			}
		});
		
		Composite labelComp = new Composite(searchComposite, SWT.NONE);
		labelComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		labelComp.setLayout(UIUtil.formLayout(0, 0));
		
		searchLbl = new Label(labelComp, SWT.NONE | SWT.RIGHT);
		searchLbl.setLayoutData(UIUtil.formData(0, 2, 0, 2, 100, -5, null, -1));
		
		searchProg = new ProgressBar(labelComp, SWT.SMOOTH/* SWT.HORIZONTAL | SWT.INDETERMINATE*/);
		searchProg.setVisible(false);
		searchProg.setLayoutData(UIUtil.formData(0, 2, 0, 0, 100, -5, null, -1, -1, 10));
		
		
		Composite tableComp = new Composite(searchComposite, SWT.NONE);
		tableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		searchResultTable = new Table(tableComp, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		searchResultTable.setHeaderVisible(true);

		TableColumn column0 = UIUtil.create(searchResultTable, SWT.RIGHT, titles[0], 6, 0, false, 100, this);
		TableColumn column1 = UIUtil.create(searchResultTable, SWT.NONE,  titles[1], 6, 1, false, 100, this);
		
		for (int loopIndex = 0; loopIndex < titles.length; loopIndex++) {
			searchResultTable.getColumn(loopIndex).pack();
		}
		
		TableColumnLayout layout = new TableColumnLayout();
		tableComp.setLayout( layout );
		
		layout.setColumnData( column0, new ColumnWeightData( 15 ) );
		layout.setColumnData( column1, new ColumnWeightData( 85 ) );
		
		searchResultTable.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				TableItem selectItem = (TableItem)event.item;
				searchLineIndex = CastUtil.cint(selectItem.getData());
				
				int select = CastUtil.cint(selectItem.getText(0)) - 1;
				if(pageNum != select){
					pageNum = select;
					if(pageNum > total){
						pageNum = total - 1;
					}
					targetPage.setText(""+(pageNum + 1));
				}
				setProfile();
			}
		});
		
		
		
		showSearchAreaBtn = new Action("Search Text", IAction.AS_CHECK_BOX){ 
	        public void run(){    
	        	if(showSearchAreaBtn.isChecked()){
	        		sashForm.setMaximizedControl(null);
	        		searchText.setFocus();
	        	}else{
	        		sashForm.setMaximizedControl(mainComposite);
	        	}
	        }
	    };  
	    showSearchAreaBtn.setImageDescriptor(ImageUtil.getImageDescriptor(Images.SEARCH));
	    man.add(showSearchAreaBtn);
		
		sashForm.setWeights(new int[]{3, 1});
//		sashForm.setMaximizedControl(mainComposite);
		
		// sjkim request.
		sashForm.setMaximizedControl(null);
		showSearchAreaBtn.setChecked(true);
		searchText.setFocus();
	}
	
	private int searchLineIndex = -1;
	
	int index;
	String m = "";
	int prog = 0;
	String searchTxt;
	
	boolean breakSearch = false;
	int maxCount = 0;
	
	private void searchWithNewText(){
		searchTxt = searchText.getText();
		searchCnt = 0;
		if(CastUtil.cint(searchFromTxt.getText()) < 1){
			searchFromTxt.setText("1");
		}
		if(CastUtil.cint(searchToTxt.getText()) > total){
			searchToTxt.setText(""+total);
		}
		
		int startProfile = CastUtil.cint(searchFromTxt.getText()) * rowPerPage - rowPerPage; 
		int endProfile = CastUtil.cint(searchToTxt.getText()) * rowPerPage;
		
		if(endProfile > profiles.length){
			endProfile = profiles.length;
		}
		
		maxCount = CastUtil.cint(maxCountTxt.getText()); 
		
		clearTable();
		searchWithText(startProfile, endProfile);
	}
	
	private void clearTable(){
		searchResultTable.removeAll();
	}
	
	int searchCnt = 0;
	private void searchWithText(final int startProfile, final int endProfile){
		
		prog = 0;
		breakSearch = false;
		
		searchText.setText(searchTxt);
		
		setSearchWidgets(true);
		
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				for (int i = startProfile; i < endProfile && !breakSearch; i++) {
					Step step;
					if(profiles[i].step instanceof StepSingle){
						step = (StepSingle) profiles[i].step;
						index = ((StepSingle)step).index;
					}else{
						step = (StepSummary) profiles[i].step;
						index = profiles[i].sSummaryIdx;
					}
					
					m = "";
					switch (step.getStepType()) {
					case StepEnum.METHOD:
					case StepEnum.METHOD2:
						m = TextProxy.method.getText(((MethodStep)step).hash);
						if (m == null){
							m = Hexa32.toString32(((MethodStep)step).hash);
						}
						break;
					case StepEnum.METHOD_SUM:
						m = TextProxy.method.getText(((MethodSum)step).hash);
						if (m == null){
							m = Hexa32.toString32(((MethodSum)step).hash);
						}
						break;
					case StepEnum.SQL:
					case StepEnum.SQL2:
					case StepEnum.SQL3:
						SqlStep sql = (SqlStep) step;
						
						m = TextProxy.sql.getText(sql.hash);
						if (m == null){
							m = Hexa32.toString32(sql.hash);
						}
						if (StringUtil.isEmpty(sql.param) == false) {
							m += " [" + sql.param + "]";
						}
						if (sql.error != 0) {
							m += TextProxy.error.getText(sql.error);
						}
						break;
					case StepEnum.SQL_SUM:
						SqlSum sqlsum = (SqlSum) step;
						
						m = TextProxy.sql.getText(sqlsum.hash);
						if (m == null){
							m = Hexa32.toString32(sqlsum.hash);
						}
						if (StringUtil.isEmpty(sqlsum.param) == false) {
							m += " [" + sqlsum.param + "]";
						}
						if (sqlsum.error != 0) {
							m += TextProxy.error.getText(sqlsum.error);
						}
						break;
					case StepEnum.MESSAGE:
						m = ((MessageStep) step).message;
						break;
					case StepEnum.APICALL:
					case StepEnum.APICALL2:
						ApiCallStep apicall = (ApiCallStep) step;
						m = TextProxy.apicall.getText(apicall.hash);
						if (m == null)
							m = Hexa32.toString32(apicall.hash);
						if (apicall.error != 0) {
							m += TextProxy.error.getText(apicall.error);
						}
						break;
					case StepEnum.APICALL_SUM:
						ApiCallSum apicallsum = (ApiCallSum) step;
						m = TextProxy.apicall.getText(apicallsum.hash);
						if (m == null)
							m = Hexa32.toString32(apicallsum.hash);
						if (apicallsum.error != 0) {
							m += TextProxy.error.getText(apicallsum.error);
						}
						break;
					case StepEnum.DISPATCH:
						DispatchStep dispatchStep = (DispatchStep) step;
						m = TextProxy.apicall.getText(dispatchStep.hash);
						if (m == null)
							m = Hexa32.toString32(dispatchStep.hash);
						if (dispatchStep.error != 0) {
							m += TextProxy.error.getText(dispatchStep.error);
						}
						break;
					case StepEnum.THREAD_CALL_POSSIBLE:
						ThreadCallPossibleStep tcStep = (ThreadCallPossibleStep) step;
						m = TextProxy.apicall.getText(tcStep.hash);
						if (m == null)
							m = Hexa32.toString32(tcStep.hash);
						break;
					}

					if(m.indexOf(searchTxt) != -1){
						searchResultTable.getDisplay().syncExec(new Runnable() {
							public void run() {
								if (searchResultTable.isDisposed())
									return;
								
								TableItem item = new TableItem(searchResultTable, SWT.NULL);
								item.setText(0, "" + ((index / rowPerPage) + 1));
								item.setText(1, m);
								item.setData(index);
								searchCnt++;
							}
						});
						
					}
					prog = i - startProfile;
					
					searchResultTable.getDisplay().syncExec(new Runnable() {
						public void run() {
							searchProg.setSelection((prog * 100 / (endProfile - startProfile)));							
						}
					});
					
					if(searchCnt >= maxCount){
						breakSearch = true;
					}
				}
				
				searchResultTable.getDisplay().syncExec(new Runnable() {
					public void run() {
						setSearchWidgets(false);
					}
				});
			}
		});
		
	}
	
	private void setSearchWidgets(boolean searchDoing){
		if(searchDoing){
			searchText.setEnabled(false);
			searchLbl.setVisible(false);
			searchProg.setVisible(true);
			searchBtn.setEnabled(false);
		}else{
			searchText.setEnabled(true);
			searchText.setFocus();
			searchText.selectAll();
			searchLbl.setVisible(true);
			searchLbl.setText("Count : "+searchCnt);
			searchProg.setVisible(false);
			searchBtn.setEnabled(true);
		}
	}
	
	
	private void setWidgets(boolean enabled){
		prevBtn.setEnabled(enabled);
		nextBtn.setEnabled(enabled);
		endBtn.setEnabled(enabled);
		startBtn.setEnabled(enabled);
		targetPage.setEnabled(enabled);
		
		sortCountBtn.setEnabled(!enabled);
		sortSumBtn.setEnabled(!enabled);
		sortAvgBtn.setEnabled(!enabled);
	}
	
	protected void getSummary() {
		
		setWidgets(false);
		
		HashMap<String, ProfileSummary> summary = new HashMap<String, ProfileSummary>();
		
		Color red = text.getDisplay().getSystemColor(SWT.COLOR_RED);
		Color dgreen = text.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN);
		
		for(int inx = 0 ; inx < profiles.length ; inx++){
			StepSingle step = (StepSingle)profiles[inx].step;
			
			switch (step.getStepType()) {
			case StepEnum.METHOD:
			case StepEnum.METHOD2:
				putSummary(summary, (MethodStep)step);
				break;
			case StepEnum.SQL:
			case StepEnum.SQL2:
			case StepEnum.SQL3:
				putSummary(summary, (SqlStep)step);
				break;
			case StepEnum.MESSAGE:
				putSummary(summary, (MessageStep)step);
				break;
			case StepEnum.APICALL:
			case StepEnum.APICALL2:
				putSummary(summary, (ApiCallStep)step);
				break;
			}
		}
		
		ValueComparator bvc =  new ValueComparator(summary);
        TreeMap<String, ProfileSummary> sorted_map = new TreeMap<String, ProfileSummary>(bvc);

        sorted_map.putAll(summary);
		
		StringBuffer sb = new StringBuffer();
		java.util.List<StyleRange> sr = new ArrayList<StyleRange>();
		int length = 0;
		
		sb.append("------------------------------------------------------------------------------------------\n");
		sb.append("   Count       Sum        Avg         Contents\n");
		sb.append("------------------------------------------------------------------------------------------\n");
		
		Iterator<String> itr = sorted_map.keySet().iterator();
		
		while (itr.hasNext()) {
			String key = itr.next();
			ProfileSummary value = summary.get(key);
			sb.append(String.format("%10d", value.callCnt));
			sb.append(" ");
			sb.append(String.format("%10d", value.sumResTime));
			sb.append(" ");
			sb.append(String.format("%10d", value.avgResTime));
			sb.append("   ");
			
			length = sb.length();
			sb.append(value.name);
			
			switch(value.type){
				case TYPE_MESSAGE:
				sr.add(style(length, sb.length() - length, dgreen, SWT.NORMAL));
				break;
			case TYPE_SQL:
				sr.add(style(length, sb.length() - length, red, SWT.NORMAL));
				break;
			case TYPE_APICALL:
				sr.add(style(length, sb.length() - length, red, SWT.NORMAL));
				break;
			default:
				break;
			}
			sb.append("\n");
		}
		sb.append("------------------------------------------------------------------------------------------\n");
		text.setText(sb.toString());
		text.setStyleRanges(sr.toArray(new StyleRange[sr.size()]));
	}

	private final int TYPE_MESSAGE = 102;
	private final int TYPE_SQL     = 103;
	private final int TYPE_METHOD  = 104;
	private final int TYPE_APICALL  = 105;
	
	private void putSummary(HashMap<String, ProfileSummary> summary, ApiCallStep step) {
		String m = TextProxy.apicall.getText(step.hash);
		if (m == null)
			m = Hexa32.toString32(step.hash);
		ProfileSummary ps = summary.get(m);
		if(ps == null){
			summary.put(m, new ProfileSummary(m, 1, step.elapsed, step.elapsed, TYPE_APICALL));
		}else{
			ps.addResTime(step.elapsed);
			summary.put(m, ps);
		}
	}

	private void putSummary(HashMap<String, ProfileSummary> summary, MessageStep step) {
		String m = step.message;
		ProfileSummary ps = summary.get(m);
		if(ps == null){
			summary.put(m, new ProfileSummary(m, 1, 0, 0, TYPE_MESSAGE));
		}else{
			ps.addResTime(0);
			summary.put(m, ps);
		}
	}

	private void putSummary(HashMap<String, ProfileSummary> summary, SqlStep step) {
		String m = TextProxy.sql.getText(step.hash);
		if (m == null)
			m = Hexa32.toString32(step.hash);
		ProfileSummary ps = summary.get(m);
		if(ps == null){
			summary.put(m, new ProfileSummary(m, 1, step.elapsed, step.elapsed, TYPE_SQL));
		}else{
			ps.addResTime(step.elapsed);
			summary.put(m, ps);
		}
	}

	private void putSummary(HashMap<String, ProfileSummary> summary, MethodStep step) {
		String m = TextProxy.method.getText(step.hash);
		if (m == null)
			m = Hexa32.toString32(step.hash);
		ProfileSummary ps = summary.get(m);
		if(ps == null){
			summary.put(m, new ProfileSummary(m, 1, step.elapsed, step.elapsed, TYPE_METHOD));
		}else{
			ps.addResTime(step.elapsed);
			summary.put(m, ps);
		}
	}

	XLogPack p;
	StepWrapper[] profiles;
	int total;
	private int serverId;
	private boolean isSummary;
	public void setInput(File xLogDir, int serverId, boolean isSummary){
		this.serverId = serverId;
		this.isSummary = isSummary;
		
		p = SaveProfileJob.getTranxData(xLogDir, header, serverId);
		profiles = SaveProfileJob.getProfileData(p, xLogDir, isSummary);
		if (profiles == null || profiles.length < 1) {
			return;
		}
		
		String date = DateUtil.yyyymmdd(p.endTime);
		
		Step[] steps = new Step[profiles.length];
		for(int inx = 0 ; inx < profiles.length ; inx++){
			steps[inx] = profiles[inx].step;
		}
		XLogUtil.loadStepText(serverId, date, steps);
		
		totalProfile.setText("Profile:"+(((StepWrapper)profiles[profiles.length - 1]).getLastIndex() + 1));
		total = (profiles.length / rowPerPage) + 1;
		if(profiles.length % rowPerPage == 0){
			total = total - 1;	
		}
		totalPage.setText("/"+total);
		targetPage.setText(""+(pageNum + 1));
		
		searchToTxt.setText((total > 300)? "300":""+total);
		searchFromTxt.setText("1");
		maxCountTxt.setText("10000");
		
		if(isSummary){
			summaryBtn.setEnabled(false);
		}
		
		adapter.setFileName(TextProxy.object.getLoadText(date, p.objHash, serverId) + "_ " + Hexa32.toString32(p.txid)+".txt");
		
		setProfile();
	}
	
	public void setProfile(){
		setWidgets(true);
		text.setText("");
		int lastIdx = ((StepWrapper)profiles[profiles.length - 1]).getLastIndex();
		int length = Integer.toString(lastIdx).length();
		SaveProfileJob.setProfileData(p, profiles, text, pageNum, rowPerPage, prevBtn, nextBtn, startBtn, endBtn, length, serverId, searchLineIndex, isSummary);
	}
	
	public void setFocus() {
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	

	public class ProfileSummary{
		String name;
		int callCnt;
		int sumResTime;
		int avgResTime;
		int type;
		
		public ProfileSummary(String name, int callCnt, int sumResTime,
				int avgResTime, int type) {
			super();
			this.name = name;
			this.callCnt = callCnt;
			this.sumResTime = sumResTime;
			this.avgResTime = avgResTime;
			this.type = type;
		}

		public void addResTime(int resTime){
			callCnt += 1;
			sumResTime += resTime;
			avgResTime = (int) Math.ceil(sumResTime / (double)callCnt );
		}
		
		public int getSortValue(int type){
			if(type == SORT_WITH_COUNT){
				return callCnt;
			}else if(type == SORT_WITH_SUM){
				return sumResTime;
			}else if(type == SORT_WITH_AVG){
				return avgResTime;
			}else{
				return callCnt;
			}
		}
	}
	
	class ValueComparator implements Comparator<String> {

	    Map<String, ProfileSummary> base;
	    public ValueComparator(Map<String, ProfileSummary> base) {
	        this.base = base;
	    }

	    public int compare(String a, String b) {
	        if (base.get(a).getSortValue(CURRENT_SORT_CRITERIA) >= base.get(b).getSortValue(CURRENT_SORT_CRITERIA)) {
	            return -1;
	        } else {
	            return 1;
	        } 
	    }
	}
	
	private StyleRange style(int start, int length, Color c, int f) {
		StyleRange t = new StyleRange();
		t.start = start;
		t.length = length;
		t.foreground = c;
		t.fontStyle = f;
		return t;
	}

	public void setTableItem(TableItem t) {
		// TODO Auto-generated method stub
		
	}

	public void setChanges() {
		// TODO Auto-generated method stub
		
	}
	
}
