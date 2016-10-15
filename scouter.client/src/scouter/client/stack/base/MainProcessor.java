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
package scouter.client.stack.base;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;

import scouter.client.Images;
import scouter.client.PerspectiveStackAnalyzer;
import scouter.client.stack.config.ParserConfig;
import scouter.client.stack.config.ParserConfigReader;
import scouter.client.stack.config.XMLReader;
import scouter.client.stack.data.StackAnalyzedInfo;
import scouter.client.stack.data.StackAnalyzedValue;
import scouter.client.stack.data.StackAnalyzedValueComp;
import scouter.client.stack.data.StackFileInfo;
import scouter.client.stack.data.StackParser;
import scouter.client.stack.data.UniqueStackValue;
import scouter.client.stack.utils.HtmlUtils;
import scouter.client.stack.utils.ResourceUtils;
import scouter.client.stack.utils.StringUtils;
import scouter.client.stack.utils.ValueObject;
import scouter.client.stack.views.StackAnalyzerView;

public class MainProcessor{
	static private MainProcessor m_mainProcessor = null;
	
	private StackAnalyzerView m_stackAnalyzerView = null;
	private Composite m_parentComposite = null;
	
    private boolean m_isExcludeStack = true;
    private boolean m_isRemoveLine = false;
    private boolean m_isInerPercent = false;
    private boolean m_isSortByFunction = false;
    private boolean m_isSimpleDumpTimeList = true;
    private boolean m_isDefaultConfiguration = false;
    private boolean m_isAnalyzeAllThreads = false;
    
	static {
		m_mainProcessor = new MainProcessor();
	}
	
	static public MainProcessor instance(){
		return m_mainProcessor;
	}

	public StackAnalyzerView getStackAnalyzerView() {
		return m_stackAnalyzerView;
	}
	
	public void setStackAnalyzerView(StackAnalyzerView stackAnalyzerView) {
		m_stackAnalyzerView = stackAnalyzerView;
	}
	
	public void setParentComposite(Composite parent){
		m_parentComposite = parent;
	}

	public Composite getParentComposite(){
		return m_parentComposite;
	}	
		
	public boolean isExcludeStack(){
		return m_isExcludeStack;
	}
	
	public void setExcludeStack(boolean value) {
		m_isExcludeStack = value;
	}
	
	public boolean isRemoveLine(){
		return m_isRemoveLine;
	}

	public void setRemoveLine(boolean value) {
		m_isRemoveLine = value;
	}
	
	public boolean isInerPercent(){
		return m_isInerPercent;
	}

	public void setInerPercent(boolean value) {
		m_isInerPercent = value;
	}
	
	public boolean isSortByFunction(){
		return m_isSortByFunction;
	}

	public void setSortByFunction(boolean value) {
		m_isSortByFunction = value;
	}
	
	public boolean isSimpleDumpTimeList(){
		return m_isSimpleDumpTimeList ;
	}

	public void setSimpleDumpTimeList(boolean value) {
		m_isSimpleDumpTimeList = value;
	}
	
	public boolean isDefaultConfiguration(){
		return m_isDefaultConfiguration;
	}

	public void setDefaultConfiguration(boolean value) {
		m_isDefaultConfiguration = value;
	}
	
	public boolean isAnalyzeAllThreads(){
		return m_isAnalyzeAllThreads;
	}
	
	public void setAnalyzeAllThread(boolean value){
		m_isAnalyzeAllThreads = value;
	}
	
	public void processMenu(String menuName){
        System.out.println("Selected:" + menuName);
        if ( menuName.substring(1, 3).equals(":\\") ) {
        //    openFiles(new File[] { new File(menuName) }, true);
        } else if ( "Open Stack Log".equals(menuName) ) {  	
            chooseStackFile();
        } else if ( "Open Analyzed Stack Log".equals(menuName) ) {
            openAnalyzedInfo();
        } else if ( "Close All".equals(menuName) ) {
            closeStackAllFileInfo();
        } else if ( "Select Parser Configuration".equals(menuName) ) {
            selectCurrentParserConfig();
        } else if ( "Manual Performance Tree(Ascending)".equals(menuName) ) {
            createManualJob(FilterInputDialog.TASK.PERFORMANCE_TREE, true);
        } else if ( "Manual Performance Tree(Descending)".equals(menuName) ) {
            createManualJob(FilterInputDialog.TASK.PERFORMANCE_TREE, false);
        } else if ( "Manual Service Call".equals(menuName) ) {
            createManualJob(FilterInputDialog.TASK.SERVICE_CALL, true);
        } else if ( "Manual Thread Stack (max 20000 lines)".equals(menuName) ) {
            createManualJob(FilterInputDialog.TASK.THREAD_STACK, true);
        } else if ( "Manual Stack Analyze(Include)".equals(menuName) ) {
            createManualJob(FilterInputDialog.TASK.FILTER_ANALYZER, true);
        } else if ( "Manual Stack Analyze(Exclude)".equals(menuName) ) {
            createManualJob(FilterInputDialog.TASK.FILTER_ANALYZER, false);
            // Main Tree Popup menu
        } else if ( "Performance Tree".equals(menuName) ) {
            createMainPerformance();
        } else if ( "Close".equals(menuName) ) {
            closeStackFileInfo();
        } else if ( "Reanalyze".equals(menuName) ) {
            reanalyzeStackFileInfo();
        } else if ( "View Raw Index File".equals(menuName) ) {
            viewRawIndexFile();
            // Main Table Popup menu
        }  else if ( "Performance Tree(Ascending)".equals(menuName) ) {
            createAnalyzedPerformance(true);
        } else if ( "Performance Tree(Descending)".equals(menuName) ) {
            createAnalyzedPerformance(false);
        } else if ( "View Thread Stack (max 20000 lines)".equals(menuName)) {
            viewThreadStack();
        } else if ( "View Service Call".equals(menuName) ) {
            viewServiceCall();
        } else if ( "Filter Stack Analyze".equals(menuName) ) {
            analyzeFilterStack(null);
        } else if ( "Copy Function".equals(menuName) ) {
            copyFunctionName();
        }
    }

   private void chooseStackFile() {
       PreferenceManager prefManager = PreferenceManager.get();

       String fileName = ResourceUtils.selectFileDialog(m_parentComposite, "Stack Log File", new String [] {"Stack Log Files", "All Files"}, new String [] {"*.log;*.txt;*.gz;*.stack", "*.*"});

       if(fileName == null){
    	   return;
       }
    	
       File file = new File(fileName);
       prefManager.setSelectedPath(file.getParentFile());
       openFile(file, false);
   }

   private ParserConfig selectAdoptiveParserConfig(){
       PreferenceManager prefManager = PreferenceManager.get();
       String configFile = prefManager.getCurrentParserConfig();
       if(m_isDefaultConfiguration){
   		configFile = XMLReader.DEFAULT_XMLCONFIG;        	
       }
       
       if (configFile == null ) {
			MessageBox messageBox = new MessageBox(m_parentComposite.getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO | SWT.APPLICATION_MODAL);
			messageBox.setText("Check Setting selection");
			messageBox.setMessage("The configuration file is not selected.\r\nDo you want to use the default configuration?");
			int result = messageBox.open();
			if(result == SWT.YES){
				configFile = XMLReader.DEFAULT_XMLCONFIG;
			}else{
			        configFile = selectCurrentParserConfig();
			        if ( configFile == null ) {
			            throw new RuntimeException("Parser config file is not selected!");
			        }
			}
       }

       ParserConfigReader reader = new ParserConfigReader(configFile);
       return reader.read();	   
   }
   
   private void addProcessedStack(StackFileInfo stackFileInfo){
       if ( stackFileInfo == null ){
    	   return;
       }
       addMainTree(stackFileInfo);
       displayContent(null);	   
   }
   
   private void openContents( String contents) {
	   ParserConfig config = selectAdoptiveParserConfig();
       StackFileInfo stackFileInfo = processStackContents(contents, config, null, false, true);       
       addProcessedStack(stackFileInfo);	   
   }
   
   private void openFile( File file, boolean isRecent ) {
	   ParserConfig config = selectAdoptiveParserConfig();
       StackFileInfo stackFileInfo = processStackFile(file.getAbsolutePath(), config, null, isRecent, true);       
       addProcessedStack(stackFileInfo);
   }
   
	private void openAnalyzedInfo() {
		ArrayList<String> list = new ArrayList<String>();
		list.add(StackParser.INFO_EXTENSION);
		String filename = ResourceUtils.selectFileDialog(m_parentComposite, "Analyzed Info File", new String [] {"Stack Analyzed Files", "All Files"}, new String [] {"*.info", "*.*"});
		if ( filename == null )
			return;

		openAnalyzedFile(filename);
	} 

	private void openAnalyzedFile( String filename ) {
		StackFileInfo fileInfo = StackParser.loadAnalyzedInfo(filename);
		addProcessedStack(fileInfo);
	}
	
	private StackFileInfo processStackContents( String contents, ParserConfig config, String filter, boolean isRecent, boolean isInclude ) {
		StackFileInfo stackFileInfo = new StackFileInfo("Stacks");
		return processStackContents(contents, stackFileInfo, config, filter, isRecent, isInclude);
	}
	
	private StackFileInfo processStackFile( String stackFilename, ParserConfig config, String filter, boolean isRecent, boolean isInclude ) {
		StackFileInfo stackFileInfo = new StackFileInfo(stackFilename);
		return processStackFile(stackFileInfo, config, filter, isRecent, isInclude);
	}	
   
	private StackFileInfo postSTackFile(StackFileInfo stackFileInfo, ParserConfig config, String filter, boolean isRecent){
        if ( stackFileInfo.getTotalWorkingCount() <= 0 ) {
        	MessageBox messageBox = new MessageBox(m_parentComposite.getShell(), SWT.ICON_ERROR | SWT.YES | SWT.APPLICATION_MODAL);
        	messageBox.setText("File open error");
        	messageBox.setMessage(new StringBuilder(200).append("A working thread is not exists in ").append(stackFileInfo.getFilename()).append(". configure a ").append(config.getConfigFilename()).append(". ").toString());
        	messageBox.open();
            return null;
        }

        if ( !isRecent){
    	    PreferenceManager prefManager = PreferenceManager.get();
        	if(filter == null ) {
        		prefManager.addToStackFiles(stackFileInfo.getFilename());
        	}
        	prefManager.addToAnalyzedStackFiles(stackFileInfo.getFilename());
        }
        return stackFileInfo;
	}
	
	private StackFileInfo processStackContents(String contents, StackFileInfo stackFileInfo, ParserConfig config, String filter, boolean isRecent, boolean isInclude ) {	
	    try {
	        StackParser parser = StackParser.getParser(config, filter, isInclude);
	        parser.setStackContents(contents);
	        parser.analyze(stackFileInfo);
	        stackFileInfo = postSTackFile(stackFileInfo,config, filter, isRecent);
	    } catch ( RuntimeException ex ) {
	        StackParser.removeAllAnalyzedFile(stackFileInfo);
	        throw ex;
	    }
	    return stackFileInfo;
	}
	
	private StackFileInfo processStackFile( StackFileInfo stackFileInfo, ParserConfig config, String filter, boolean isRecent, boolean isInclude ) {	
	    try {
	        StackParser parser = StackParser.getParser(config, filter, isInclude);
	        parser.analyze(stackFileInfo);
	        stackFileInfo = postSTackFile(stackFileInfo,config, filter, isRecent);
	    } catch ( RuntimeException ex ) {
	        StackParser.removeAllAnalyzedFile(stackFileInfo);
	        throw ex;
	    }
	    return stackFileInfo;
	}

    private String selectCurrentParserConfig() {
    	String fileName = ResourceUtils.selectFileDialog(m_parentComposite, "XML Parser Configuration", new String [] {"XML config", "All Files"}, new String [] {"*.xml", "*.*"});
    	if(fileName != null){
    		PreferenceManager.get().setCurrentParserConfig(fileName);
    		displayContent(null);
    	}
    	return fileName;
    }
    
    public Browser getBrowser(){
    	if(m_stackAnalyzerView == null)
    		return null;
    	
    	return m_stackAnalyzerView.getBrowser();
    }
    
    
    public Tree getMainTree(){
    	if(m_stackAnalyzerView == null)
    		return null;
    	
    	return m_stackAnalyzerView.getMainTree();
    }
    
    public Table getTable(){
    	if(m_stackAnalyzerView == null)
    		return null;
    	
    	return m_stackAnalyzerView.getTable();
    }
    
    public void displayContent( String textHtml ) {
    	Browser browser = getBrowser();
        if ( textHtml != null ) {
        	browser.setText(textHtml);
        } else {
        	browser.setText(HtmlUtils.getDefaultBody());
        }
    }
    
    private void addMainTree( StackFileInfo stackFileInfo ) {
    	Tree tree = getMainTree();
    	TreeItem item = new TreeItem(tree, SWT.NONE);
    	item.setText(stackFileInfo.toTreeInfo());
    	item.setImage(Images.thread);
    	item.setData(stackFileInfo);
    	addMainTreeSubItem(item,stackFileInfo.getStackAnalyzedInfoList());
    	item.setExpanded(true);
    }
    
    private void addMainTreeSubItem(TreeItem parent, ArrayList<StackAnalyzedInfo> list){
    	TreeItem subItem;
    	for(StackAnalyzedInfo info : list){
    	   	subItem = new TreeItem(parent, SWT.NONE);
    	   	subItem.setText(info.toTreeInfo());	
    	   	subItem.setImage(Images.grid);
    	   	subItem.setData(info);
    	}    	
    }
        
    public void setTable(StackAnalyzedInfo stackAnalyzedInfo){
    	clearTable();
    	Table table = getTable();
    	if(stackAnalyzedInfo == null){
    		return;
    	}
    	ArrayList<StackAnalyzedValue> list = null;
        if ( m_isSortByFunction ) {
        	list = StackAnalyzedValueComp.sortClone(stackAnalyzedInfo.getAnalyzedList(), false);
        }else{
        	list = stackAnalyzedInfo.getAnalyzedList();
        }
        
        TableItem item;
    	for(StackAnalyzedValue value : list){
    		item = new TableItem(table, SWT.BORDER);
    		item.setText(value.toTableInfo());
    		if(value instanceof UniqueStackValue){
    			item.setData(((UniqueStackValue)value).getStack());
    		}
    	} 	
    }
    
    private void closeStackAllFileInfo() {
    	Tree tree = this.getMainTree();
        if ( tree != null ){
        	tree.clearAll(true);
        	tree.setItemCount(0);
        }
        
        clearTable();
        this.displayContent(null);
    }
    
    public void clearTable(){
    	Table table = getTable();
    	if(table != null){
	    	table.clearAll();
	    	table.setItemCount(0);
    	}
    }
    
    public TreeItem getSelectedItemFromMainTree(){
    	Tree tree = this.getMainTree();
    	if(tree == null){
    		return null;
    	}
    	
    	TreeItem [] items = tree.getSelection();
    	if(items == null || items.length == 0){
    		return null;
    	}
    	return items[0];
    }
    
    public Object getSelectedFromMainTree(){
    	TreeItem item = getSelectedItemFromMainTree();
    	if(item == null){
    		return null;
    	}
    	return item.getData();    	
    }
 
    public TreeItem getSelectedRootItemFromMainTree(){
    	Tree tree = this.getMainTree();
    	if(tree == null){
    		return null;
    	}
    	
    	TreeItem [] items = tree.getSelection();
    	if(items == null || items.length == 0){
    		return null;
    	}
    	
    	if(items[0].getData() instanceof StackAnalyzedInfo){
    		return items[0].getParentItem();
    	}
    	return items[0];
    }    
    
    public StackFileInfo getSelectedStackFileInfo() {
    	Object object =getSelectedFromMainTree();
        StackFileInfo stackFileInfo = null;

        if ( object instanceof StackAnalyzedInfo ) {
            stackFileInfo = ((StackAnalyzedInfo)object).getStackFileInfo();
        } else if ( object instanceof StackFileInfo ) {
            stackFileInfo = ((StackFileInfo)object);
        }

        return stackFileInfo;
    }    
    
    private void createMainPerformance() {
        StackFileInfo stackFileInfo = getSelectedStackFileInfo();
        if ( stackFileInfo != null ){
        	new PerformanceWindow(m_parentComposite.getShell(), stackFileInfo, null, m_isExcludeStack, true, m_isRemoveLine, m_isInerPercent);
        }
    }  
    
    private void viewRawIndexFile(){
    	Object object = getSelectedFromMainTree();
    	if(object == null){
    		return;
    	}  	
        if (!(object instanceof StackAnalyzedInfo)){
        	return;
        }
        StackAnalyzedInfo analyzedInfo = (StackAnalyzedInfo)object;
        if(analyzedInfo.getExtension().equals(StackParser.UNIQUE_EXT)){
        	return;
        }
        
    	StackFileInfo stackFileInfo = analyzedInfo.getStackFileInfo();
    	
    	System.out.println("StackFile:"+ stackFileInfo.toString());        	
    	System.out.println("File:"+ analyzedInfo.toString());
    	
        String analyzedFilename = StackParser.getAnaylzedFilename(stackFileInfo.getFilename(), analyzedInfo.getExtension());
        File file = new File(analyzedFilename);
        if ( !file.isFile() )
            return;

        BufferedReader reader = null;
        boolean isDetail = false;
        boolean isStart = false;
        try {
            reader = new BufferedReader(new FileReader(file));

            String line = null;
            int totalCount = 0;
            HashMap<String, Integer> map = new HashMap<String, Integer>();
            while ( (line = reader.readLine()) != null ) {
                line = line.trim();
                if(line.length() > 0){
                	isStart = true;
                }
                if(line.length() == 0){
	                if(isStart){
	                	isDetail = true;
	                }
                	continue;
                }
                if(isDetail){
                	totalCount++;
                	HtmlUtils.caculCounter(line, map);
                }                
            }
            
            ArrayList<ValueObject> list = HtmlUtils.sortCounter(map);   
            
            StringBuilder buffer = new StringBuilder(102400);
            buffer.append(HtmlUtils.getMainBodyStart());
            buffer.append(HtmlUtils.getCurrentConfigurationBody()).append("<br><br>");
            buffer.append("<b>[ ").append(stackFileInfo.getFilename()).append(" ]</b><BR>");
            buffer.append("<b>").append(analyzedInfo.toString()).append(" - ").append(analyzedFilename).append("</b><br><br>");

            buffer.append("<table border='1'><tr align='center'><th>Count</th><th>Percent</th><th>Class.method</th></tr>");
            int value;
            for(ValueObject valueObject : list){
            	value = valueObject.getValue();
            	buffer.append("<tr><td align='right'>").append(value).append("</td><td align='right'>").append((int)((100 * value)/totalCount)).append('%').append("</td>");
            	buffer.append("<td align='left'>").append(valueObject.getKey()).append("</td></tr>");
            }
            buffer.append("</table>");
            buffer.append(HtmlUtils.getMainBodyEnd());
            
            displayContent(buffer.toString());
        } catch ( Exception ex ) {
            throw new RuntimeException(ex);
        } finally {
            try {
                if ( reader != null )
                    reader.close();
            } catch ( Exception ex ) {
            }
        }
    }
    
    private void createManualJob( FilterInputDialog.TASK jobtype, boolean isAscending ) {
        try {
        	new FilterInputDialog(m_parentComposite.getShell(), isAscending, jobtype);
        } catch ( Exception e ) {
            throw new RuntimeException(e);
        }
    }
    
    private void createAnalyzedPerformance( boolean isAscending ) {
        createAnalyzedPerformance(getSelectedAnalyzedFunction(), isAscending);
    }

    public void createAnalyzedPerformance( String filter, boolean isAscending ) {
        if ( filter == null )
            return;

        StackFileInfo stackFileInfo = getSelectedStackFileInfo();
        if ( stackFileInfo != null ){
            new PerformanceWindow(m_parentComposite.getShell(), stackFileInfo, filter, m_isExcludeStack, isAscending, m_isRemoveLine, m_isInerPercent);
        }
    }   
    
    public void analyzeFilterStack( String inputFilter ) {
        analyzeFilterStack(inputFilter, true);
    }

    private String getSelectedAnalyzedFunction() {
    	Table table = getTable();
    	if(table == null){
    		return null;
    	}
    	TableItem [] items = table.getSelection();
    	if(items == null || items.length == 0){
    		return null;
    	}
    	
    	return items[0].getText(3);
    }
    
    public void analyzeFilterStack( String inputFilter, boolean isInclude ) {
        String filter = null;
        if ( inputFilter == null ) {
            filter = getSelectedAnalyzedFunction();
        } else {
            filter = inputFilter;
        }

        if ( filter == null )
            return;

        StackFileInfo stackFileInfo = getSelectedStackFileInfo();
        if ( stackFileInfo == null )
            return;

        ParserConfigReader reader = new ParserConfigReader(stackFileInfo.getParserConfig().getConfigFilename());
        ParserConfig config = reader.read();

        StackFileInfo filteredStackFileInfo = processStackFile(StackParser.getWorkingThreadFilename(stackFileInfo.getFilename()), config, filter, false, isInclude);
        addProcessedStack(filteredStackFileInfo);
    }
    
    private void viewThreadStack() {
        viewThreadStack(getSelectedAnalyzedFunction());
    }

    public void viewThreadStack( String filter ) {
        if ( filter == null )
            return;

        StackFileInfo stackFileInfo = getSelectedStackFileInfo();
        if ( stackFileInfo == null )
            return;

        String filename = StackParser.getWorkingThreadFilename(stackFileInfo.getFilename());
        int stackStartLine = stackFileInfo.getParserConfig().getStackStartLine();
        if ( filename != null && filter != null ) {
        	Browser broswer = getBrowser();
            if ( m_isExcludeStack )
            	broswer.setText(HtmlUtils.filterThreadStack(filename, filter, stackFileInfo.getParserConfig().getExcludeStack(), stackStartLine));
            else
            	broswer.setText(HtmlUtils.filterThreadStack(filename, filter, null, stackStartLine));
        }

    }

    private void viewServiceCall() {
        viewServiceCall(getSelectedAnalyzedFunction());
    }

    public void viewServiceCall( String filter ) {
        if ( filter == null )
            return;

        StackFileInfo stackFileInfo = getSelectedStackFileInfo();
        if ( stackFileInfo == null )
            return;

        String filename = StackParser.getWorkingThreadFilename(stackFileInfo.getFilename());
        int stackStartLine = stackFileInfo.getParserConfig().getStackStartLine();
        if ( filename != null && filter != null ) {
            getBrowser().setText(HtmlUtils.filterServiceCall(filename, filter, stackFileInfo.getParserConfig().getService(), stackStartLine));
        }
    }  
    
    private void copyFunctionName() {
        String filter = getSelectedAnalyzedFunction();
        if ( filter != null && filter.length() > 0 )
            StringUtils.setClipboard(filter);
    }  

    private void closeStackFileInfo() {
    	TreeItem item = getSelectedItemFromMainTree();
    	if(item == null){
    		return;
    	}
    	
    	if(item.getData() instanceof StackAnalyzedInfo){
    		item = item.getParentItem();
    	}
    	item.removeAll();
    	item.dispose();
    	
    	Table table = getTable();
    	table.clearAll();
    	table.setItemCount(0);
    	displayContent(null);
    }  
    
    private void reanalyzeStackFileInfo() {
    	TreeItem item = getSelectedRootItemFromMainTree();
    	if(item == null){
    		return;
    	}
    	
        StackFileInfo stackFileInfo = (StackFileInfo)item.getData();
        if ( stackFileInfo == null ){
            return;
        }

        try {
            ParserConfigReader reader = new ParserConfigReader(stackFileInfo.getParserConfig().getConfigFilename());
            ParserConfig config = reader.read();

            StackParser.removeAllAnalyzedFile(stackFileInfo);

            processStackFile(stackFileInfo, config, null, false, true);
        } catch ( RuntimeException ex ) {
            closeStackFileInfo();
            throw ex;
        }
        item.removeAll();
        item.setText(stackFileInfo.toTreeInfo());
        addMainTreeSubItem(item, stackFileInfo.getStackAnalyzedInfoList());
        item.setExpanded(true);
        
        clearTable();
    }
    
    public void openStackAnalyzer(){
    	IWorkbench workbench = PlatformUI.getWorkbench();
    	IWorkbenchWindow window = workbench.getActiveWorkbenchWindow(); 
    	try{ 
    		workbench.showPerspective(PerspectiveStackAnalyzer.ID, window);
    		window.getActivePage().showView(StackAnalyzerView.ID);
    	} catch (WorkbenchException e) { 
    		System.out.println("Unable to open Perspective: " + PerspectiveStackAnalyzer.ID); 
    	}    	
    }
  
    public void processStackFile(String fileName){
    	openStackAnalyzer();
    	File file = new File(fileName);
    	openFile(file, false);
    }

    public void processStackContents(String contents){
    	openStackAnalyzer();
    	openContents(contents);
    }    
}
