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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import scouter.client.Activator;
import scouter.client.Images;
import scouter.client.stack.data.StackFileInfo;
import scouter.client.stack.data.StackParser;
import scouter.client.stack.utils.NumberUtils;
import scouter.client.stack.utils.StringUtils;

public class PerformanceWindow implements Listener{
	private Shell m_parentShell = null;
	private Shell m_shell = null;
	private Tree m_performanceTree = null;
    private StackFileInfo m_stackFileInfo = null;
    private String m_filter = null;
    private boolean m_isExcludeStack = true;
    private boolean m_isAscending = true;
    private boolean m_isRemoveLine = true;
    private boolean m_isInerPercent = false;
    private int m_totalCount = 0;
    private ArrayList<String> m_excludeStack = null;
    private ArrayList<String> m_singleStack = null;
    private int m_singleStackCount = 0;
    private int m_expandStartInx = 0;
 
    public PerformanceWindow(Shell shell, StackFileInfo stackFileInfo, String filter, boolean isExcludeStack, boolean isAscending, boolean isRemoveLine, boolean isInerPercent) {
    	m_parentShell = shell;
    	m_stackFileInfo = stackFileInfo;
        m_filter = filter;
        m_isExcludeStack = isExcludeStack;
        m_isAscending = isAscending;
        m_isRemoveLine = isRemoveLine;
        m_isInerPercent = isInerPercent;

        ArrayList<String> list = stackFileInfo.getParserConfig().getExcludeStack();
        if ( m_isExcludeStack && list != null && list.size() > 0 ) {
            m_excludeStack = list;
        }

        list = stackFileInfo.getParserConfig().getSingleStack();
        if ( list != null && list.size() > 0 ) {
            m_singleStack = list;
            m_singleStackCount = list.size();
        }
        
        StringBuilder buffer = new StringBuilder(200);
        buffer.append('[');
        if ( filter == null )
            buffer.append("All");
        else
            buffer.append(filter);
        buffer.append("] ").append(stackFileInfo.getFilename());
        buffer.append(" [EXC:").append(isExcludeStack).append("] [ASC:");
        buffer.append(isAscending).append(']');
        
        init(shell, buffer.toString());
    }
    

    private void init(Shell shell, String title) {
        m_shell = new Shell(shell, SWT.MIN | SWT.MAX | SWT.CLOSE | SWT.TITLE | SWT.RESIZE);
        m_shell.setText(title);
        m_shell.setImage(Images.tree_mode);
    	m_shell.setLayout(new FillLayout());
 
    	m_performanceTree = new Tree(m_shell, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
    	m_performanceTree.setBackgroundImage(Activator.getImage("icons/grid.jpg"));
    	constructTree();
        createTreePopupMenu();

        m_shell.open();
    }

    private void constructTree() {
        String filename = StackParser.getWorkingThreadFilename(m_stackFileInfo.getFilename());
        if ( filename == null )
            return;

        HashMap<String, Counter> tree = new HashMap<String, Counter>();
        BufferedReader reader = null;
        int startStackLine = m_stackFileInfo.getUsedParser().getConfig().getStackStartLine();
        try {
            File file = new File(filename);
            reader = new BufferedReader(new FileReader(file));

            String line = null;
            int lineCount = 0;
            boolean isFiltered = false;
            int filterIndex = 0;

            ArrayList<String> list = new ArrayList<String>(300);

            while ( (line = reader.readLine()) != null ) {
                if ( line.length() == 0 ) {
                    if ( isFiltered && list.size() > 0 ) {
                        createPerformanceTree(tree, list, filterIndex);
                    }

                    list = new ArrayList<String>(300);
                    lineCount = 0;
                    isFiltered = false;
                } else {
                    lineCount++;
                    if ( lineCount > startStackLine ) {
                        if ( !isFiltered ) {
                            if ( m_filter == null ) {
                                isFiltered = true;
                            } else if ( line.indexOf(m_filter) >= 0 ) {
                                isFiltered = true;
                                filterIndex = lineCount - (startStackLine + 1);
                            }
                        }

                        list.add(line);
                    }
                }
            }
            if ( isFiltered && list.size() > 0 ) {
                createPerformanceTree(tree, list, filterIndex);
            }
        } catch ( Exception ex ) {
            throw new RuntimeException(ex);
        } finally {
            if ( reader != null ) {
                try {
                    reader.close();
                } catch ( Exception e ) {
                }
            }
        }

        if ( m_isInerPercent && tree != null ) {
            Counter cntr;
            Iterator<Counter> itor = tree.values().iterator();
            while ( itor.hasNext() ) {
                cntr = itor.next();
                cntr.caculInerCount();
            }
        }

        TreeItem treeItem = new TreeItem(m_performanceTree, SWT.NONE);
        treeItem.setText("Stack File Nodes");
        treeItem.setData(null);
        treeItem.setExpanded(true);
        makePerformanceTreeUI(treeItem, tree, 0);
    }

    private void makePerformanceTreeUI( TreeItem parent, HashMap<String, Counter> tree, int depth ) {
        if ( tree == null || tree.size() == 0 )
            return;

        depth++;
        
        String key = null;
        Counter counter = null;
        StringBuilder buffer = null;
        int index = 0;

        Iterator<String> itor = tree.keySet().iterator();
        ArrayList<Counter> sortList = new ArrayList<Counter>();
        while ( itor.hasNext() ) {
            key = itor.next();
            counter = tree.get(key);
            counter.setValue(key);
            sortList.add(counter);
        }

        Collections.sort(sortList, new ValueComp());
        
        int itemCount = sortList.size();
        if(itemCount > 1 && m_expandStartInx == 0){
        	m_expandStartInx = depth;
        }
        
        TreeItem treeItem;
        for ( index = 0; index < itemCount; index++ ) {
            counter = sortList.get(index);
            buffer = new StringBuilder(100);
            buffer.append(counter.getCount()).append(" (").append(NumberUtils.intToPercent((counter.getCount() * 10000) / m_totalCount)).append("%) ");
            if ( m_isInerPercent ) {
                buffer.append(NumberUtils.intToPercent((counter.getInerCount() * 10000) / m_totalCount)).append("% ");
            }
            buffer.append(counter.getValue());

            treeItem = new TreeItem(parent,  SWT.NONE );
            treeItem.setText(buffer.toString());
            treeItem.setData(counter.getValue());
        	treeItem.setExpanded(true);

            if(m_expandStartInx == 0){
            	m_performanceTree.showItem(treeItem);            	
            }else if((depth - m_expandStartInx) <= 3){
            	m_performanceTree.showItem(treeItem);            	
            }
            
            if ( counter.getMap() != null ) {
                makePerformanceTreeUI(treeItem, counter.getMap(), depth);
            }
        }

    }

    private void createPerformanceTree( HashMap<String, Counter> tree, ArrayList<String> list, int filterIndex ) {
        int startIndex = 0;
        int size = list.size() - 1;
        if ( m_filter != null ) {
            startIndex = filterIndex;
        } else {
            startIndex = list.size() - 1;
        }

        m_totalCount++;
        String line = null;
        HashMap<String, Counter> currentMap = tree;
        Counter currentCounter = null;
        int index;
        int baseIndex = startIndex;

        while ( true ) {
            line = list.get(startIndex);

            if ( m_singleStackCount > 0 ) {
                for ( index = 0; index < m_singleStackCount; index++ ) {
                    if ( line.indexOf(m_singleStack.get(index)) >= 0 ) {
                        line = m_singleStack.get(index);
                        break;
                    }
                }
            }

            if ( m_excludeStack != null && currentMap != null && currentMap != tree ) {
                if ( StringUtils.checkExist(line, m_excludeStack) ) {
                    if ( m_isAscending ) {
                        if ( startIndex == 0 )
                            break;

                        startIndex--;
                    } else {
                        if ( startIndex == size )
                            break;

                        startIndex++;
                    }
                    continue;
                }
            }

            if ( m_filter != null && baseIndex == startIndex )
                line = StringUtils.makeSimpleLine(line, false);
            else
                line = StringUtils.makeSimpleLine(line, m_isRemoveLine);

            if ( currentCounter != null ) {
                currentMap = currentCounter.getMap();
                if ( currentMap == null )
                    currentMap = currentCounter.addMap();
            }

            currentCounter = currentMap.get(line);
            if ( currentCounter == null ) {
                currentCounter = new Counter();
                currentMap.put(line, currentCounter);
            }
            currentCounter.addCount();
            if ( m_isAscending ) {
                if ( startIndex == 0 )
                    break;

                startIndex--;
            } else {
                if ( startIndex == size )
                    break;

                startIndex++;
            }
        }
    }

    private class Counter {
        private int m_count = 0;
        private int m_inerCount = 0;
        private String m_value = null;
        private HashMap<String, Counter> m_map = null;

        public void addCount() {
            m_count++;
        }

        public int getCount() {
            return m_count;
        }

        public int getInerCount() {
            return m_inerCount;
        }

        public void setValue( String value ) {
            m_value = value;
        }

        public String getValue() {
            return m_value;
        }

        public HashMap<String, Counter> addMap() {
            if ( m_map == null ) {
                m_map = new HashMap<String, Counter>();
            }
            return m_map;
        }

        public HashMap<String, Counter> getMap() {
            return m_map;
        }

        public void caculInerCount() {
            if ( m_map == null ) {
                m_inerCount = m_count;
                return;
            }
            int subCount = 0;
            Iterator<Counter> itor = m_map.values().iterator();
            Counter cntr;
            while ( itor.hasNext() ) {
                cntr = itor.next();
                subCount += cntr.getCount();
                cntr.caculInerCount();
            }
            m_inerCount = m_count - subCount;
        }
    }

    private class ValueComp implements Comparator<Counter> {

        public int compare( Counter o1, Counter o2 ) {
            if ( o1.getCount() > o2.getCount() )
                return -1;
            else if ( o1.getCount() < o2.getCount() )
                return 1;

            return 0;
        }
    }

    private void createTreePopupMenu() {
		Menu popupMenu = new Menu(m_performanceTree);
		
		MenuItem menuItem = new MenuItem(popupMenu, SWT.NONE);
		menuItem.setText("Performance tree(Ascending)");
		menuItem.addListener(SWT.Selection, this);
		menuItem = new MenuItem(popupMenu, SWT.NONE);
		menuItem.setText("Performance tree(Descending)");
		menuItem.addListener(SWT.Selection, this);
		
		menuItem = new MenuItem(popupMenu, SWT.SEPARATOR);
		
		menuItem = new MenuItem(popupMenu, SWT.NONE);
		menuItem.setText("Copy function");
		menuItem.addListener(SWT.Selection, this);

		m_performanceTree.setMenu(popupMenu);       
    }
    
	public void handleEvent(Event event) {
		try {
			MenuItem item = (MenuItem)event.widget;
			String menuText = item.getText();
			if("Performance tree(Ascending)".endsWith(menuText)){
                createAnalyzedPerformance(true);
			}else if("Performance tree(Descending)".endsWith(menuText)){
                createAnalyzedPerformance(false);
			}else if("Copy function".endsWith(menuText)){
                CopyFunctionName();
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}		
	}    
    private void createAnalyzedPerformance( boolean isAscending ) {
        String filter = getSelectedFunctionName();

        if ( filter != null && m_stackFileInfo != null ){
        	new PerformanceWindow(m_parentShell, m_stackFileInfo, filter, m_isExcludeStack, isAscending, m_isRemoveLine, m_isInerPercent);
        }	
    }

    private void CopyFunctionName() {
        String functionName = getSelectedFunctionName();
        if ( functionName != null )
            StringUtils.setClipboard(functionName.trim());
    }

    private String getSelectedFunctionName() {
        TreeItem [] items = m_performanceTree.getSelection();
        if(items == null || items.length == 0){
        	return null;
        }
        
        String function = (String)items[0].getData();

        int endIndex = function.indexOf((int)'(');
        if ( endIndex >= 0 )
            function = function.substring(0, endIndex);

        return function;
    }
}
