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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import scouter.client.stack.config.ParserConfig;
import scouter.client.stack.config.ParserConfigReader;
import scouter.client.stack.config.XMLReader;
import scouter.client.stack.data.StackAnalyzedInfo;
import scouter.client.stack.data.StackAnalyzedValueComp;
import scouter.client.stack.data.StackFileInfo;
import scouter.client.stack.data.StackParser;
import scouter.client.stack.utils.HtmlUtils;
import scouter.client.stack.utils.ResourceUtils;
import scouter.client.stack.utils.StringUtils;


public class MainFrame extends JPanel implements ListSelectionListener, TreeSelectionListener, ActionListener, MenuListener {
    private static final long serialVersionUID = 1L;

    private static int DIVIDER_SIZE = 4;

    protected static JFrame m_frame = null;
    private static MainFrame m_mainFrame = null;
    private static int m_fontSizeModifier = 0;

    private JTree m_logTree = null;
    private JTable m_analyzedTable = null;
    private JEditorPane m_htmlPane = null;
    private ViewScrollPane m_htmlView = null;
    private JSplitPane m_splitPane = null;
    protected JSplitPane m_topSplitPane = null;
    protected DefaultTreeModel m_treeModel = null;
    private boolean m_isTreeMainView = false;
    private boolean m_isExcludeStack = true;
    private boolean m_isFullFunction = true;
    private boolean m_isInerPercent = false;
    private boolean m_isSortByFunction = false;
    private boolean m_isSimpleDumpTimeList = true;
    private boolean m_isDefaultConfiguration = false;

    public static void setFrame( JFrame frame ) {
        m_frame = frame;
    }

    public static JFrame getFrame() {
        return m_frame;
    }

    public static MainFrame instance() {
        if ( m_mainFrame == null ) {
            m_mainFrame = new MainFrame();
        }

        return m_mainFrame;
    }

    public MainFrame() {
        super(new BorderLayout());
        setupLookAndFeel();
    }

    public void valueChanged( ListSelectionEvent e ) {
    }

    public void valueChanged( TreeSelectionEvent e ) {
    }

    public void actionPerformed( ActionEvent e ) {
        if ( e.getSource() instanceof JMenuItem ) {
            JMenuItem source = (JMenuItem)(e.getSource());
            try {
            	processMenu(source.getText(), source);
            } catch ( RuntimeException ex ) {
                JOptionPane.showMessageDialog(m_frame, ex.toString(), source.getText(), JOptionPane.ERROR_MESSAGE);
                throw ex;
            }
        }
    }
    
    public void processMenu(String menuName, JMenuItem source){
        System.out.println("Selected:" + menuName);
        if ( menuName.substring(1, 3).equals(":\\") ) {
            openFiles(new File[] { new File(menuName) }, true);
        } else if ( "Open Stack Log".equals(menuName) ) {
            chooseStackFile();
        } else if ( "Open Analyzed Stack".equals(menuName) ) {
            openAnalyzedInfo();
        } else if ( "Close All".equals(menuName) ) {
            closeStackAllFileInfo();
        } else if ( "Select Parser Configuration".equals(menuName) ) {
            selectCurrentParserConfig();
        } else if ( "Help".equals(menuName) ) {
        	HelpWindow.startHelpWindow();
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
            /* Main Tree Popup menu */
        } else if ( "Performance Tree".equals(menuName) ) {
            createMainPerformance();
        } else if ( "Close".equals(menuName) ) {
            closeStackFileInfo();
        } else if ( "Reanalyze".equals(menuName) ) {
            reanalyzeStackFileInfo();
        } else if ( "View Raw Index File".equals(menuName) ) {
            viewRawIndexFile();
            /* Main Table Popup menu */
        } else if ( "Performance Tree(Ascending)".equals(menuName) ) {
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
        
        if(source != null){
        	if ( "Exclude Stack".equals(menuName) ) {
                m_isExcludeStack = ((JCheckBoxMenuItem)source).getState();
            } else if ( "Remove Line(Performance Tree)".equals(source.getText()) ) {
                m_isFullFunction = ((JCheckBoxMenuItem)source).getState();
            } else if ( "Inner Percent(Performance Tree)".equals(source.getText()) ) {
                m_isInerPercent = ((JCheckBoxMenuItem)source).getState();
            } else if ( "Sort by Function".equals(source.getText()) ) {
                m_isSortByFunction = ((JCheckBoxMenuItem)source).getState();
            } else if ( "Simple Dump Time List".equals(menuName) ) {
                m_isSimpleDumpTimeList = ((JCheckBoxMenuItem)source).getState();
            } else if ( "Use Default Parser Configuration".equals(menuName) ) {
            	m_isDefaultConfiguration = ((JCheckBoxMenuItem)source).getState();
            }        	
        }	
    }

    public void menuCanceled( MenuEvent e ) {
    }

    public void menuDeselected( MenuEvent e ) {
    }

    public void menuSelected( MenuEvent e ) {
    }

    private void setupLookAndFeel() {
        try {
            UIManager.LookAndFeelInfo currentLAFI = null;
            String plaf = "Mac,Windows,Metal";
            UIManager.getLookAndFeelDefaults().put("ClassLoader", getClass().getClassLoader());
            UIManager.LookAndFeelInfo[] plafs = UIManager.getInstalledLookAndFeels();

            if ( (plaf != null) && (!"".equals(plaf)) ) {

                String[] instPlafs = plaf.split(",");
                search: for ( int i = 0; i < instPlafs.length; i++ ) {
                    for ( int j = 0; j < plafs.length; j++ ) {
                        currentLAFI = plafs[j];
                        if ( currentLAFI.getName().startsWith(instPlafs[i]) ) {
                            UIManager.setLookAndFeel(currentLAFI.getClassName());
                            ResourceUtils.setUIFont(new FontUIResource("SansSerif", Font.PLAIN, 13));
                            break search;
                        }
                    }
                }
            }

            if ( plaf.startsWith("GTK") ) {
                setFontSizeModifier(2);
            }
        } catch ( Exception except ) {
            ResourceUtils.setUIFont(new FontUIResource("SansSerif", Font.PLAIN, 11));
        }
    }

    public static String getFontSizeModifier( int add ) {
        String result = String.valueOf(m_fontSizeModifier + add);
        if ( (m_fontSizeModifier + add) > 0 ) {
            result = "+" + (m_fontSizeModifier + add);
        }
        return result;
    }

    public static void setFontSizeModifier( int value ) {
        m_fontSizeModifier = value;
    }

    public void init( ) {
        InputStream is = MainFrame.class.getResourceAsStream("/scouter/client/stack/doc/welcome.html");

        m_htmlPane = new JEditorPane();
        m_htmlPane.setContentType("text/html");
        m_htmlPane.setText(parseMainHTML(is));
        m_htmlPane.setEditable(false);

        JEditorPane emptyPane = new JEditorPane("text/html", "");
        emptyPane.setEditable(false);

        m_htmlPane.addHyperlinkListener(
                new HyperlinkListener() {
                    public void hyperlinkUpdate( HyperlinkEvent evt ) {
                        if ( evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED ) {
                            System.out.println("Link:" + evt.getDescription());
                            if ( evt.getDescription().equals("openfile://") ) {
                                chooseStackFile();
                            } else if ( evt.getDescription().startsWith("openfile") && !evt.getDescription().endsWith("//") ) {
                                File[] files = { new File(evt.getDescription().substring(11)) };
                                openFiles(files, false);
                            } else if ( evt.getDescription().equals("openanalyzedfile://") ) {
                                openAnalyzedInfo();
                            } else if ( evt.getDescription().startsWith("openanalyzedfile") && !evt.getDescription().endsWith("//") ) {
                                String fileName = new StringBuilder(100).append(evt.getDescription().substring(19)).append('_').append(StackParser.INFO_EXT).append('.').append(StackParser.INFO_EXTENSION).toString();
                                openAnalyzedFile(fileName);
                            } else if ( evt.getDescription().equals("parserconfig://") ) {
                                selectCurrentParserConfig();
                            }
                        }
                    }

                });

        m_htmlView = new ViewScrollPane(m_htmlPane, false);
        ViewScrollPane emptyView = new ViewScrollPane(emptyPane, false);
        
        m_topSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        m_topSplitPane.setLeftComponent(emptyView);
        m_topSplitPane.setDividerSize(DIVIDER_SIZE);
        m_topSplitPane.setContinuousLayout(true);

        m_splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        m_splitPane.setBottomComponent(m_htmlView);
        m_splitPane.setTopComponent(m_topSplitPane);
        m_splitPane.setDividerSize(DIVIDER_SIZE);
        m_splitPane.setContinuousLayout(true);

        Dimension minimumSize = new Dimension(200, 50);
        m_htmlView.setMinimumSize(minimumSize);
        emptyView.setMinimumSize(minimumSize);

        add(m_htmlView, BorderLayout.CENTER);
    }

    private void addTreeListener( JTree tree ) {
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged( TreeSelectionEvent e ) {
                Object object = ((DefaultMutableTreeNode)e.getPath().getLastPathComponent()).getUserObject();
                if ( object instanceof StackAnalyzedInfo ) {
                    if ( m_analyzedTable == null ) {
                        createMainTable((StackAnalyzedInfo)object);
                    } else {
                        AnalyzedTableModel model = (AnalyzedTableModel)m_analyzedTable.getModel();
                        if ( m_isSortByFunction ) {
                            model.setElements(StackAnalyzedValueComp.sortClone(((StackAnalyzedInfo)object).getAnalyzedList(), false));
                        } else {
                            model.setElements(((StackAnalyzedInfo)object).getAnalyzedList());
                        }
                    }
                    m_analyzedTable.revalidate();
                    m_analyzedTable.repaint();
                } else if ( object instanceof StackFileInfo ) {
                    displayContent(HtmlUtils.getStackFileInfo((StackFileInfo)object));
                }
            }
        });
    }

    public void saveState() {
         PreferenceManager.get().flush();
    }

    private String parseMainHTML( InputStream is ) {
        BufferedReader br = null;
        String resultString = null;

        PreferenceManager perf = PreferenceManager.get();
        StringBuffer result = new StringBuffer();

        try {
            br = new BufferedReader(new InputStreamReader(is));
            while ( br.ready() ) {
                result.append(br.readLine());
                result.append("\n");
            }
            resultString = result.toString();
            resultString = resultString.replaceFirst("<!-- ##recentfiles## -->", HtmlUtils.getAsTable("openfile://", perf.getStackFiles()));
            resultString = resultString.replaceFirst("<!-- ##recentanalyzedfiles## -->", HtmlUtils.getAsTable("openanalyzedfile://", perf.getAnalyzedStackFiles()));
            
            if ( perf.getCurrentParserConfig() != null ){
                resultString = resultString.replaceFirst("<!-- ##currentparserconfig## -->", perf.getCurrentParserConfig().replace('\\', '/'));            	
            }
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
        } catch ( IOException ex ) {
            ex.printStackTrace();
        } finally {
            try {
                if ( br != null ) {
                    br.close();
                    is.close();
                }
            } catch ( IOException ex ) {
                ex.printStackTrace();
            }
        }
        resultString = resultString.replaceFirst("<!-- ##recentfiles## -->", "");
        resultString = resultString.replaceFirst("<!-- ##recentanalyzedfiles## -->", "");
        resultString = resultString.replaceFirst("<!-- ##currentparserconfig## -->", "");
        return (resultString);
    }

    private void openFiles( File[] files, boolean isRecent ) {
        PreferenceManager prefManager = PreferenceManager.get();
        String configFile = prefManager.getCurrentParserConfig();
        if(m_isDefaultConfiguration){
    		configFile = XMLReader.DEFAULT_XMLCONFIG;        	
        }
        
        if (configFile == null ) {
        
        	int result = JOptionPane.showConfirmDialog (null, "The configuration file is not selected.\r\nDo you want to use the default configuration?","Check Setting selection",JOptionPane.YES_NO_OPTION);
        	if(result == JOptionPane.YES_OPTION ){
        		configFile = XMLReader.DEFAULT_XMLCONFIG;
        	}else{
	            configFile = selectCurrentParserConfig();
	            if ( configFile == null ) {
	                throw new RuntimeException("Parser config file is not selected!");
	            }
        	}
        }

        if ( !m_isTreeMainView ) {
            m_isTreeMainView = true;
            initStackDisplay(null);
            createMainTree();
        }

        ParserConfigReader reader = new ParserConfigReader(configFile);
        ParserConfig config = reader.read();

        StackFileInfo stackFileInfo = null;
        for ( int i = 0; i < files.length; i++ ) {
            stackFileInfo = processStackFile(files[i].getAbsolutePath(), config, null, isRecent, true);
            if ( stackFileInfo != null )
                addMainTree(stackFileInfo);
        }

 //       this.getRootPane().revalidate();
        displayContent(null);
    }

    private StackFileInfo processStackFile( StackFileInfo stackFileInfo, ParserConfig config, String filter, boolean isRecent, boolean isInclude ) {
        PreferenceManager prefManager = PreferenceManager.get();

        try {
            StackParser parser = StackParser.getParser(config, filter, isInclude);
            parser.analyze(stackFileInfo);

            if ( stackFileInfo.getTotalWorkingCount() <= 0 ) {
                JOptionPane.showMessageDialog(m_frame,
                        new StringBuilder(200).append("A working thread is not exists in ").append(stackFileInfo.getFilename()).append(". configure a ").append(config.getConfigFilename())
                                .append(". ").toString(), "File open error", JOptionPane.ERROR_MESSAGE);
                return null;
            }

            if ( !isRecent){
            	if(filter == null ) {
            		prefManager.addToStackFiles(stackFileInfo.getFilename());
            	}
            	prefManager.addToAnalyzedStackFiles(stackFileInfo.getFilename());
            }
        } catch ( RuntimeException ex ) {
            StackParser.removeAllAnalyzedFile(stackFileInfo);
            throw ex;
        }
        return stackFileInfo;
    }

    private StackFileInfo processStackFile( String stackFilename, ParserConfig config, String filter, boolean isRecent, boolean isInclude ) {
        StackFileInfo stackFileInfo = new StackFileInfo(stackFilename);
        return processStackFile(stackFileInfo, config, filter, isRecent, isInclude);
    }

    private void initStackDisplay( String content ) {
        if ( m_topSplitPane.getDividerLocation() <= 0 ) {
            m_topSplitPane.setDividerLocation(200);
        }

        // change from html view to split pane
        remove(0);
        revalidate();
        m_htmlPane.setText("");
        m_splitPane.setBottomComponent(m_htmlView);
        add(m_splitPane, BorderLayout.CENTER);
        if ( PreferenceManager.get().getDividerPos() > 0 ) {
            m_splitPane.setDividerLocation(PreferenceManager.get().getDividerPos());
        } else {
            // set default divider location
            m_splitPane.setDividerLocation(200);
        }
        revalidate();
    }

    private void chooseStackFile() {
        PreferenceManager prefManager = PreferenceManager.get();

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(prefManager.getSelectedPath());
        fileChooser.setFileFilter(new StackFileFilter());
        fileChooser.setMultiSelectionEnabled(true);
//        fileChooser.setName("Stack Log File");

        if ( (prefManager.getPreferredSizeFileChooser().height > 0) ) {
            fileChooser.setPreferredSize(prefManager.getPreferredSizeFileChooser());
        }
        int returnVal = fileChooser.showOpenDialog(this.getRootPane());

        prefManager.setPreferredSizeFileChooser(fileChooser.getSize());

        if ( returnVal == JFileChooser.APPROVE_OPTION ) {
            File[] files = fileChooser.getSelectedFiles();
            if ( files != null && files.length > 0 ) {
                prefManager.setSelectedPath(files[0].getParentFile());
            }
            openFiles(files, false);
        }
    }

    private void displayContent( String text ) {
        if ( m_splitPane.getBottomComponent() != m_htmlView ) {
            m_splitPane.setBottomComponent(m_htmlView);
        }
        if ( text != null ) {
            m_htmlPane.setContentType("text/html");
            m_htmlPane.setText(text);
            m_htmlPane.setCaretPosition(0);
        } else {
            m_htmlPane.setText(HtmlUtils.getDefaultBody());
        }
    }

    private void createMainTable( StackAnalyzedInfo info ) {
        AnalyzedTableModel model = new AnalyzedTableModel();

        if ( m_isSortByFunction ) {
            model.setElements(StackAnalyzedValueComp.sortClone(info.getAnalyzedList(), false));
        } else {
            model.setElements(info.getAnalyzedList());
        }

        m_analyzedTable = new JTable(model);
        m_analyzedTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        m_analyzedTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        TableColumnModel columnModel = m_analyzedTable.getColumnModel();

        int width = 0;
        DefaultTableCellRenderer cellRenderer = null;

        for ( int i = 0; i < columnModel.getColumnCount(); i++ ) {
            TableColumn column = columnModel.getColumn(i);
            cellRenderer = new DefaultTableCellRenderer();
            switch ( i ) {
            case 0:
                width = 70;
                cellRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
                break;
            case 1:
                width = 100;
                cellRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
                break;
            case 2:
                width = 100;
                cellRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
                break;
            case 3:
                width = 500;
                cellRenderer.setHorizontalAlignment(SwingConstants.LEFT);
                break;
            }
            column.setCellRenderer(cellRenderer);
            column.setPreferredWidth(width);
        }

        ViewScrollPane tableView = new ViewScrollPane(m_analyzedTable, false);

        m_topSplitPane.setRightComponent(tableView);

        Dimension minimumSize = new Dimension(200, 50);
        tableView.setMinimumSize(minimumSize);

        createTablePopupMenu();
    }

    private void createMainTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Stack File Nodes");
        m_treeModel = new DefaultTreeModel(root);

        m_logTree = new JTree(root);
        m_logTree.setRootVisible(false);
        addTreeListener(m_logTree);

        m_logTree.setShowsRootHandles(true);
        m_logTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        m_logTree.setCellRenderer(new MainTreeRenderer());

        ViewScrollPane treeView = new ViewScrollPane(m_logTree, false);

        m_topSplitPane.setLeftComponent(treeView);

        Dimension minimumSize = new Dimension(200, 50);
        treeView.setMinimumSize(minimumSize);

        m_logTree.addTreeSelectionListener(this);

        createTreePopupMenu();
    }

    private DefaultMutableTreeNode makeMainTreeNewNode( StackFileInfo stackFileInfo ) {
        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(stackFileInfo);
        makeMainTreeChildNode(stackFileInfo, newNode);
        return newNode;
    }

    private DefaultMutableTreeNode makeMainTreeChildNode( StackFileInfo stackFileInfo, DefaultMutableTreeNode newNode ) {
        DefaultMutableTreeNode item = null;
        StackAnalyzedInfo info = null;

        ArrayList<StackAnalyzedInfo> list = stackFileInfo.getStackAnalyzedInfoList();
        if ( list != null ) {
            for ( int i = 0; i < list.size(); i++ ) {
                info = list.get(i);
                item = new DefaultMutableTreeNode(info);
                newNode.add(item);
            }
        }
        return newNode;
    }

    private void addMainTree( StackFileInfo stackFileInfo ) {
        if ( m_logTree == null )
            return;

        DefaultMutableTreeNode newNode = makeMainTreeNewNode(stackFileInfo);

        DefaultTreeModel model = (DefaultTreeModel)m_logTree.getModel();
        ((DefaultMutableTreeNode)model.getRoot()).add(newNode);
        model.reload();
    }

    private String selectCurrentParserConfig() {
        ArrayList<String> list = new ArrayList<String>();
        list.add("xml");
        String filename = selectFile("XML Parser Configuration", list);

        if ( filename != null ) {
            PreferenceManager.get().setCurrentParserConfig(filename);
        }
        return filename;
    }

    private void openAnalyzedInfo() {
        ArrayList<String> list = new ArrayList<String>();
        list.add(StackParser.INFO_EXTENSION);
        String filename = selectFile("Analyzed Info File", list);
        if ( filename == null )
            return;

        openAnalyzedFile(filename);
    }

    private String selectFile( String caption, ArrayList<String> extensionList ) {
        JFileChooser fileChooser = new JFileChooser();
        PreferenceManager prefManager = PreferenceManager.get();

        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setCurrentDirectory(new File(prefManager.getPreference(caption, ".")));

        if ( prefManager.getPreferredSizeFileChooser().height > 0 ) {
            fileChooser.setPreferredSize(prefManager.getPreferredSizeFileChooser());
        }
        fileChooser.setFileFilter(new SelectFileFilter(caption, extensionList));

        int returnVal = fileChooser.showOpenDialog(this.getRootPane());

        fileChooser.setPreferredSize(fileChooser.getSize());
        prefManager.setPreferredSizeFileChooser(fileChooser.getSize());

        if ( returnVal == JFileChooser.APPROVE_OPTION ) {
            File file = fileChooser.getSelectedFile();
            if ( file != null ) {
                prefManager.setPreference(caption, file.getParentFile().getPath());
                return file.getAbsolutePath();
            }
        }
        return null;
    }

    private void createTreePopupMenu() {
        JMenuItem menuItem = null;

        JPopupMenu popup = new JPopupMenu();

        menuItem = new JMenuItem("Performance Tree");
        menuItem.addActionListener(this);
        popup.add(menuItem);
        menuItem = new JMenuItem("Reanalyze");
        menuItem.addActionListener(this);
        popup.add(menuItem);
        menuItem = new JMenuItem("Close");
        menuItem.addActionListener(this);
        popup.add(menuItem);

        popup.addSeparator();
        
        menuItem = new JMenuItem("View Raw Index File");
        menuItem.addActionListener(this);
        popup.add(menuItem);

        popup.addSeparator();
       
        JMenu menu = new JMenu("Manual");
        menu.setMnemonic(KeyStroke.getKeyStroke("M").getKeyCode());
        menu.getAccessibleContext().setAccessibleDescription("Manual Menu");

        menuItem = new JMenuItem("Manual Performance Tree(Ascending)");
        menuItem.setMnemonic(KeyStroke.getKeyStroke("A").getKeyCode());
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JMenuItem("Manual Performance Tree(Descending)");
        menuItem.setMnemonic(KeyStroke.getKeyStroke("D").getKeyCode());
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JMenuItem("Manual Service Call");
        menuItem.setMnemonic(KeyStroke.getKeyStroke("A").getKeyCode());
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JMenuItem("Manual Stack Analyze(Include)");
        menuItem.setMnemonic(KeyStroke.getKeyStroke("Y").getKeyCode());
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JMenuItem("Manual Stack Analyze(Exclude)");
        menuItem.setMnemonic(KeyStroke.getKeyStroke("Z").getKeyCode());
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JMenuItem("Manual Thread Stack (max 20000 lines)");
        menuItem.setMnemonic(KeyStroke.getKeyStroke("D").getKeyCode());
        menuItem.addActionListener(this);
        menu.add(menuItem);
        
        popup.add(menu);
        
        menu = new JMenu("View");
        menu.setMnemonic(KeyStroke.getKeyStroke("V").getKeyCode());
        menu.getAccessibleContext().setAccessibleDescription("View Menu");

        menuItem = new JCheckBoxMenuItem("Exclude Stack", m_isExcludeStack);
        menuItem.setMnemonic(KeyStroke.getKeyStroke("E").getKeyCode());
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JCheckBoxMenuItem("Remove Line(Performance Tree)", m_isFullFunction);
        menuItem.setMnemonic(KeyStroke.getKeyStroke("F").getKeyCode());
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JCheckBoxMenuItem("Inner Percent(Performance Tree)", m_isInerPercent);
        menuItem.setMnemonic(KeyStroke.getKeyStroke("I").getKeyCode());
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menu.addSeparator();

        menuItem = new JCheckBoxMenuItem("Sort by Function", m_isSortByFunction);
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menu.addSeparator();

        menuItem = new JCheckBoxMenuItem("Simple Dump Time List", m_isSimpleDumpTimeList);
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menu.addSeparator();
        
        menuItem = new JCheckBoxMenuItem("Use Default Parser Configuration", m_isDefaultConfiguration);
        menuItem.addActionListener(this);
        menu.add(menuItem);
       
        popup.add(menu);
        
        menuItem = new JMenuItem("Help", KeyEvent.VK_A);
        menuItem.addActionListener(this);
        popup.add(menuItem);
        
        MouseListener popupListener = new PopupListener(popup);
        m_logTree.addMouseListener(popupListener);
    }

    private void createTablePopupMenu() {
        JMenuItem menuItem = null;

        JPopupMenu popup = new JPopupMenu();

        menuItem = new JMenuItem("Performance Tree(Ascending)");
        menuItem.addActionListener(this);
        popup.add(menuItem);

        menuItem = new JMenuItem("Performance Tree(Descending)");
        menuItem.addActionListener(this);
        popup.add(menuItem);

        popup.addSeparator();

        menuItem = new JMenuItem("View Thread Stack (max 20000 lines)");
        menuItem.addActionListener(this);
        popup.add(menuItem);

        menuItem = new JMenuItem("View Service Call");
        menuItem.addActionListener(this);
        popup.add(menuItem);

        popup.addSeparator();

        menuItem = new JMenuItem("Filter Stack Analyze");
        menuItem.addActionListener(this);
        popup.add(menuItem);

        popup.addSeparator();

        menuItem = new JMenuItem("Copy Function");
        menuItem.addActionListener(this);
        popup.add(menuItem);

        MouseListener popupListener = new PopupListener(popup);
        m_analyzedTable.addMouseListener(popupListener);
    }

    private void createMainPerformance() {
        StackFileInfo stackFileInfo = getSelectedStackFileInfo();
        if ( stackFileInfo != null )
            PerformanceWindow.startPerformanceWindow(stackFileInfo, null, m_isExcludeStack, true, m_isFullFunction, m_isInerPercent);
    }

    private String getSelectedAnalyzedFunction() {
        int row = m_analyzedTable.getSelectedRow();
        if ( row < 0 )
            return null;
        return (String)m_analyzedTable.getValueAt(row, 3);
    }

    private void createAnalyzedPerformance( boolean isAscending ) {
        createAnalyzedPerformance(getSelectedAnalyzedFunction(), isAscending);
    }

    public void createAnalyzedPerformance( String filter, boolean isAscending ) {
        if ( filter == null )
            return;

        StackFileInfo stackFileInfo = getSelectedStackFileInfo();
        if ( stackFileInfo != null )
            PerformanceWindow.startPerformanceWindow(stackFileInfo, filter, m_isExcludeStack, isAscending, m_isFullFunction, m_isInerPercent);
    }
    
    

    public StackFileInfo getSelectedStackFileInfo() {
        if ( m_logTree == null )
            return null;

        DefaultMutableTreeNode node = ((DefaultMutableTreeNode)m_logTree.getLastSelectedPathComponent());
        if ( node == null )
            return null;

        Object object = node.getUserObject();
        StackFileInfo stackFileInfo = null;

        if ( object instanceof StackAnalyzedInfo ) {
            stackFileInfo = ((StackAnalyzedInfo)object).getStackFileInfo();
        } else if ( object instanceof StackFileInfo ) {
            stackFileInfo = ((StackFileInfo)object);
        }

        return stackFileInfo;
    }

    private DefaultMutableTreeNode getSelectedMainNode() {
        if ( m_logTree == null )
            return null;
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)m_logTree.getLastSelectedPathComponent();

        Object object = node.getUserObject();

        if ( object instanceof StackAnalyzedInfo ) {
            node = (DefaultMutableTreeNode)node.getParent();
        }

        return node;
    }

    public void analyzeFilterStack( String inputFilter ) {
        analyzeFilterStack(inputFilter, true);
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
        if ( filteredStackFileInfo != null ) {
            addMainTree(filteredStackFileInfo);
//            this.getRootPane().revalidate();
            displayContent(null);
        }
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
            if ( m_isExcludeStack )
                m_htmlPane.setText(HtmlUtils.filterThreadStack(filename, filter, stackFileInfo.getParserConfig().getExcludeStack(), stackStartLine));
            else
                m_htmlPane.setText(HtmlUtils.filterThreadStack(filename, filter, null, stackStartLine));
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
            m_htmlPane.setText(HtmlUtils.filterServiceCall(filename, filter, stackFileInfo.getParserConfig().getService(), stackStartLine));
        }

    }

    private void openAnalyzedFile( String filename ) {
        if ( !m_isTreeMainView ) {
            m_isTreeMainView = true;
            initStackDisplay(null);
            createMainTree();
        }

        StackFileInfo fileinfo = StackParser.loadAnalyzedInfo(filename);
        if ( fileinfo != null ) {
            addMainTree(fileinfo);
            this.getRootPane().revalidate();
            displayContent(null);
        }
    }

    private void closeStackFileInfo() {
        if ( m_logTree == null )
            return;
        DefaultTreeModel model = (DefaultTreeModel)m_logTree.getModel();
        DefaultMutableTreeNode node = this.getSelectedMainNode();
        if ( node == null )
            return;
        model.removeNodeFromParent(node);
    }

    private void closeStackAllFileInfo() {
        if ( m_logTree == null )
            return;

        DefaultTreeModel model = (DefaultTreeModel)m_logTree.getModel();
        int count = model.getChildCount(model.getRoot());
        DefaultMutableTreeNode node;
        for ( int i = 0; i < count; i++ ) {
            node = (DefaultMutableTreeNode)model.getChild(model.getRoot(), 0);
            model.removeNodeFromParent(node);
        }
    }

    private void reanalyzeStackFileInfo() {
        StackFileInfo stackFileInfo = getSelectedStackFileInfo();
        if ( stackFileInfo == null )
            return;

        DefaultTreeModel model = null;
        try {
            ParserConfigReader reader = new ParserConfigReader(stackFileInfo.getParserConfig().getConfigFilename());
            ParserConfig config = reader.read();

            StackParser.removeAllAnalyzedFile(stackFileInfo);

            processStackFile(stackFileInfo, config, null, false, true);

            model = (DefaultTreeModel)m_logTree.getModel();
        } catch ( RuntimeException ex ) {
            closeStackFileInfo();
            throw ex;
        }
        DefaultMutableTreeNode node = this.getSelectedMainNode();
        node.setUserObject(stackFileInfo);

        node.removeAllChildren();
        makeMainTreeChildNode(stackFileInfo, node);

        model.reload();
    }

    private void copyFunctionName() {
        String filter = getSelectedAnalyzedFunction();
        if ( filter != null && filter.length() > 0 )
            StringUtils.setClipboard(filter);
    }

    private void createManualJob( FilterInputDialog.TASK jobtype, boolean isAscending ) {
        try {
            FilterInputDialog.init(this, isAscending, jobtype);
        } catch ( Exception e ) {
            throw new RuntimeException(e);
        }
    }

    public boolean isSimpleDumpTimeList() {
        return m_isSimpleDumpTimeList;
    }
    
    private void viewRawIndexFile(){
    	DefaultMutableTreeNode node = (DefaultMutableTreeNode)m_logTree.getLastSelectedPathComponent();
    	Object object = node.getUserObject();
    	
        if (!(object instanceof StackAnalyzedInfo))
        	return;
        	
        StackAnalyzedInfo analyzedInfo = (StackAnalyzedInfo)object;
    	node = (DefaultMutableTreeNode)node.getParent();
    	StackFileInfo stackFileInfo = (StackFileInfo)node.getUserObject();
    	
    	System.out.println("StackFile:"+ stackFileInfo.toString());        	
    	System.out.println("File:"+ analyzedInfo.toString());
    	
    	
        String analyzedFilename = StackParser.getAnaylzedFilename(stackFileInfo.getFilename(), analyzedInfo.getExtension());
        File file = new File(analyzedFilename);
        if ( !file.isFile() )
            return;

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));

            String line = null;
            StringBuilder buffer = new StringBuilder(102400);
            buffer.append(HtmlUtils.getCurrentConfigurationBody()).append("<br><br>");
            buffer.append("<b>[ ").append(stackFileInfo.getFilename()).append(" ]</b><BR>");
            buffer.append("<b>").append(analyzedInfo.toString()).append(" - ").append(analyzedFilename).append("</b><br><br>");
            while ( (line = reader.readLine()) != null ) {
                line = line.trim();
                buffer.append(line).append("<br>");
            }            
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
}
