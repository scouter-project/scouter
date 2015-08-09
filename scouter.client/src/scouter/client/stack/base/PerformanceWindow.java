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

package scouter.client.stack.base;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import scouter.client.stack.data.StackFileInfo;
import scouter.client.stack.data.StackParser;
import scouter.client.stack.utils.NumberUtils;
import scouter.client.stack.utils.StringUtils;


@SuppressWarnings("serial")
public class PerformanceWindow extends JFrame implements MenuListener, ActionListener {
    private JTree m_performanceTree = null;
    private StackFileInfo m_stackFileInfo = null;
    private String m_filter = null;
    private boolean m_isExcludeStack = true;
    private boolean m_isAscending = true;
    private boolean m_isFullFunction = true;
    private boolean m_isInerPercent = false;
    private int m_totalCount = 0;
    private ArrayList<String> m_excludeStack = null;
    private ArrayList<String> m_singleStack = null;
    private int m_singleStackCount = 0;

    static public void createPerformanceWindow( StackFileInfo stackFileInfo, String filter, boolean isExcludeStack, boolean isAscending, boolean isFullFunction, boolean isInerPercent ) {
        @SuppressWarnings("unused")
        PerformanceWindow window = new PerformanceWindow(stackFileInfo, filter, isExcludeStack, isAscending, isFullFunction, isInerPercent);
    }

    static public void startPerformanceWindow( final StackFileInfo stackFileInfo, final String filter, final boolean isExcludeStack, final boolean isAscending, final boolean isFullFunction,
            final boolean isInerPercent ) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createPerformanceWindow(stackFileInfo, filter, isExcludeStack, isAscending, isFullFunction, isInerPercent);
            }
        });
    }

    public PerformanceWindow(StackFileInfo stackFileInfo, String filter, boolean isExcludeStack, boolean isAscending, boolean isFullFunction, boolean isInerPercent) {
        super();
        StringBuilder buffer = new StringBuilder(200);
        buffer.append('[');
        if ( filter == null )
            buffer.append("All");
        else
            buffer.append(filter);
        buffer.append("] ").append(stackFileInfo.getFilename());
        buffer.append(" [EXC:").append(isExcludeStack).append("] [ASC:");
        buffer.append(isAscending).append(']');

        this.setTitle(buffer.toString());

        m_stackFileInfo = stackFileInfo;
        m_filter = filter;
        m_isExcludeStack = isExcludeStack;
        m_isAscending = isAscending;
        m_isFullFunction = isFullFunction;
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
        init();
    }

    private void init() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Stack File Nodes");

        constructTree(root);

        m_performanceTree = new JTree(root);
        m_performanceTree.setRootVisible(true);
        // addTreeListener(m_performanceTree);

        m_performanceTree.setShowsRootHandles(true);
        m_performanceTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        m_performanceTree.setCellRenderer(new PerformanceTreeRenderer());

        Container content = getContentPane();
        JScrollPane scrollPane = new JScrollPane(m_performanceTree);
        content.add(scrollPane, BorderLayout.CENTER);

        setVisible(true);
        setSize(500, 500);

        createTreePopupMenu();

        this.validate();
    }

    private void constructTree( DefaultMutableTreeNode root ) {
        String filename = StackParser.getWorkingThreadFilename(m_stackFileInfo.getFilename());
        if ( filename == null )
            return;

        // Tree Map ��
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
        // Tree UI ó��
        makePerformanceTreeUI(root, tree);
    }

    private void makePerformanceTreeUI( DefaultMutableTreeNode node, HashMap<String, Counter> tree ) {
        if ( tree == null || tree.size() == 0 )
            return;

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

        for ( index = 0; index < sortList.size(); index++ ) {
            counter = sortList.get(index);
            buffer = new StringBuilder(100);
            buffer.append(counter.getCount()).append(" (").append(NumberUtils.intToPercent((counter.getCount() * 10000) / m_totalCount)).append("%) ");
            if ( m_isInerPercent ) {
                buffer.append(NumberUtils.intToPercent((counter.getInerCount() * 10000) / m_totalCount)).append("% ");
            }
            buffer.append(counter.getValue());

            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(buffer.toString());
            node.add(childNode);
            if ( counter.getMap() != null ) {
                makePerformanceTreeUI(childNode, counter.getMap());
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
                line = StringUtils.makeSimpleLine(line, m_isFullFunction);

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
        JMenuItem menuItem = null;
        ;

        // Create the popup menu.
        JPopupMenu popup = new JPopupMenu();

        menuItem = new JMenuItem("Performance tree(Ascending)");
        menuItem.addActionListener(this);
        popup.add(menuItem);

        menuItem = new JMenuItem("Performance tree(Descending)");
        menuItem.addActionListener(this);
        popup.add(menuItem);

        popup.addSeparator();

        menuItem = new JMenuItem("Copy function");
        menuItem.addActionListener(this);
        popup.add(menuItem);

        MouseListener popupListener = new PopupListener(popup);
        m_performanceTree.addMouseListener(popupListener);
    }

    @Override
    public void menuCanceled( MenuEvent e ) {
    }

    @Override
    public void menuDeselected( MenuEvent e ) {
    }

    @Override
    public void menuSelected( MenuEvent e ) {
    }

    @Override
    public void actionPerformed( ActionEvent e ) {
        if ( e.getSource() instanceof JMenuItem ) {
            JMenuItem source = (JMenuItem)(e.getSource());
            if ( "Performance tree(Ascending)".equals(source.getText()) ) {
                createAnalyzedPerformance(true);
            } else if ( "Performance tree(Descending)".equals(source.getText()) ) {
                createAnalyzedPerformance(false);
            } else if ( "Copy function".equals(source.getText()) ) {
                CopyFunctionName();
            }
            source.setSelected(false);
        }
    }

    private void createAnalyzedPerformance( boolean isAscending ) {
        String filter = getSelectedFunctionName();

        if ( filter != null && m_stackFileInfo != null )
            PerformanceWindow.startPerformanceWindow(m_stackFileInfo, filter, m_isExcludeStack, isAscending, m_isFullFunction, m_isInerPercent);

    }

    private void CopyFunctionName() {
        String functionName = getSelectedFunctionName();
        if ( functionName != null )
            StringUtils.setClipboard(functionName.trim());
    }

    private String getSelectedFunctionName() {
        TreePath path = m_performanceTree.getSelectionPath();

        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)path.getLastPathComponent();
        if ( selectedNode == null )
            return null;

        String function = (String)selectedNode.getUserObject();

        int startIndex = function.indexOf(')');
        if ( startIndex < 0 )
            return null;

        if ( m_isInerPercent ) {
            startIndex = function.indexOf('%', startIndex);
            if ( startIndex < 0 ) {
                return null;
            }
        }

        int endIndex = function.indexOf((int)'(', startIndex + 2);

        if ( endIndex < 0 )
            function = function.substring(startIndex + 2);
        else
            function = function.substring(startIndex + 2, endIndex);

        return function;

    }
}
