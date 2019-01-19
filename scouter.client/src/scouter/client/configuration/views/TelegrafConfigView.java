package scouter.client.configuration.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import scouter.client.Images;
import scouter.client.configuration.model.SingleString;
import scouter.client.configuration.model.TagFilterMapping;
import scouter.client.configuration.model.TreeModel;
import scouter.client.configuration.views.dialog.SingleValueDialog;
import scouter.client.configuration.views.sub.CounterMappingCounterNameEditingSupport;
import scouter.client.configuration.views.sub.CounterMappingDeltaTypeSupport;
import scouter.client.configuration.views.sub.CounterMappingDisplayNameEditingSupport;
import scouter.client.configuration.views.sub.CounterMappingNormalizeSecSupport;
import scouter.client.configuration.views.sub.CounterMappingTgFieldEditingSupport;
import scouter.client.configuration.views.sub.CounterMappingTotalizableEditingSupport;
import scouter.client.configuration.views.sub.CounterMappingUnitEditingSupport;
import scouter.client.configuration.views.sub.HostScouterEditingSupport;
import scouter.client.configuration.views.sub.HostTelegrafEditingSupport;
import scouter.client.configuration.views.sub.SingleColumnEditingSupport;
import scouter.client.configuration.views.sub.TagMappingTagEditingSupport;
import scouter.client.configuration.views.sub.TagMappingValueEditingSupport;
import scouter.client.constants.HelpConstants;
import scouter.client.net.TcpProxy;
import scouter.client.server.Server;
import scouter.client.server.ServerManager;
import scouter.client.util.ColorUtil;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.lang.pack.MapPack;
import scouter.net.RequestCmd;
import scouter.server.support.telegraf.TgConfig;
import scouter.server.support.telegraf.TgCounterMapping;
import scouter.server.support.telegraf.TgmConfig;
import scouter.util.StringUtil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TelegrafConfigView extends ViewPart {

    private static class ViewerLabelProvider extends LabelProvider {
        public Image getImage(Object element) {
            return super.getImage(element);
        }

        public String getText(Object element) {
            return ((TreeModel) element).displayName;
        }
    }

    private static class TreeContentProvider implements ITreeContentProvider {
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            System.out.println("inputChanged");
        }

        public void dispose() {
        }

        public Object[] getElements(Object inputElement) {
            return getChildren(inputElement);
        }

        public Object[] getChildren(Object parentElement) {
            return ((TreeModel) parentElement).child.toArray();
        }

        public Object getParent(Object element) {
            if (element == null) return null;
            return ((TreeModel) element).parent;
        }

        public boolean hasChildren(Object element) {
            return ((TreeModel) element).child.size() > 0;
        }
    }

    private int serverId;
    private TreeModel root;
    private TreeModel measurementsParent;
    private TgConfig tgConfig;
    private TgmConfig m_tgmConfig;
    private Map<String, List<TagFilterMapping>> m_tagFilterMappingMap = new HashMap<>();
    private Map<String, List<SingleString>> m_familyAppendTagsMap = new HashMap<>();
    private Map<String, List<SingleString>> m_objTypePrependTagsMap = new HashMap<>();
    private Map<String, List<SingleString>> m_objTypeAppendTagsMap = new HashMap<>();
    private Map<String, List<SingleString>> m_objNameAppendTagsMap = new HashMap<>();

    List<TagFilterMapping> m_tagFilterMappings;
    List<SingleString> m_familyAppendTags;
    List<SingleString> m_objTypePrependTags;
    List<SingleString> m_objTypeAppendTags;
    List<SingleString> m_objNameAppendTags;

    public static final String ID = "scouter.client.configuration.views.TelegrafConfigView"; //$NON-NLS-1$
    private Action saveAction;
    private Action helpAction;
    private Action reloadAction;
    Composite mainContainer;
    private Composite compContentsBody;
    private Composite compGeneral;
    private Composite compMeasurement;
    TreeViewer treeViewer;
    private Button btnTgmEnabled;
    private Button btnTgmDebugEnabled;
    private Button btnTgmNormalizeDefault;
    private Text txtNormalizeSec;
    private Text txtTgObjDeadTimeMs;
    private TableViewer tvHostMapping;
    private Table tblHostMapping;
    private TableViewer tvTagFilter;
    private Table tblTagFilter;
    private Text txtMtGeneralName;
    private Button btnMtEnabled;
    private Button btnMtDebugEnabled;
    private Text txtMtHostTag;
    private Text txtMtObjFamily;
    private Table tblFamilyAppendTag;
    private TableViewer tvFamilyAppendTag;
    private Text txtMtObjType;
    private Text txtMtObjTypeIcon;
    private Table tblObjTypePrependTag;
    private TableViewer tvObjTypePrependTag;
    private Table tblObjTypeAppendTag;
    private TableViewer tvObjTypeAppendTag;
    private Text txtMtObjName;
    private Table tblObjNameAppendTag;
    private TableViewer tvObjNameAppendTag;
    private TableViewer tvCounterMapping;
    private Table tblCounterMapping;

    public TelegrafConfigView() {
        this.tgConfig = new TgConfig();
    }

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
//        mainContainer.addKeyListener(new KeyListener() {
//            public void keyReleased(KeyEvent e) {
//            }
//            public void keyPressed(KeyEvent e) {
//                if (e.stateMask == SWT.CTRL || e.stateMask == SWT.COMMAND) {
//                    if (e.keyCode == 's' || e.keyCode == 'S') {
//                        saveConfig();
//                    }
//                }
//            }
//        });

        SashForm mainSash = new SashForm(mainContainer, SWT.NONE);

        //============================================================================
        // Menu & Tree area
        //============================================================================
        Composite compMenu = new Composite(mainSash, SWT.NONE);
        GridLayout gl_compMenu = new GridLayout(1, false);
        compMenu.setLayout(gl_compMenu);

        treeViewer = new TreeViewer(compMenu, SWT.BORDER);
        treeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                if (event.getSelection().isEmpty()) return;

                if (event.getSelection() instanceof IStructuredSelection) {
                    IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                    Object element = selection.getFirstElement();
                    if (element == null) return;

                    if (element instanceof TreeModel) {
                        TreeModel model = (TreeModel) element;
                        if (model.element instanceof TgmConfig) {
                            m_tgmConfig = (TgmConfig) model.element;

                            //general
                            txtMtGeneralName.setText(m_tgmConfig.measurementName);
                            txtMtHostTag.setText(m_tgmConfig.hostTag);

                            btnMtEnabled.setSelection(m_tgmConfig.enabled);
                            btnMtDebugEnabled.setSelection(m_tgmConfig.debugEnabled);

                            tvHostMapping.setInput(m_tgmConfig.hostMappings);

                            //object
                            txtMtObjFamily.setText(m_tgmConfig.objFamilyBase);
                            txtMtObjType.setText(m_tgmConfig.objTypeBase);
                            txtMtObjTypeIcon.setText(m_tgmConfig.objTypeIcon);
                            txtMtObjName.setText(m_tgmConfig.objNameBase);

                            List<TagFilterMapping> tagFilterMappings = m_tagFilterMappingMap.get(m_tgmConfig.measurementName);
                            if (tagFilterMappings == null) {
                                tagFilterMappings = TagFilterMapping.of(m_tgmConfig.tagFilters);
                                m_tagFilterMappingMap.put(m_tgmConfig.measurementName, tagFilterMappings);
                            }
                            m_tagFilterMappings = tagFilterMappings;
                            tvTagFilter.setInput(tagFilterMappings);

                            List<SingleString> familyAppendTags = m_familyAppendTagsMap.get(m_tgmConfig.measurementName);
                            if (familyAppendTags == null) {
                                familyAppendTags = SingleString.of(m_tgmConfig.objFamilyAppendTags);
                                m_familyAppendTagsMap.put(m_tgmConfig.measurementName, familyAppendTags);
                            }
                            m_familyAppendTags = familyAppendTags;
                            tvFamilyAppendTag.setInput(familyAppendTags);

                            List<SingleString> objTypePrepentTags = m_objTypePrependTagsMap.get(m_tgmConfig.measurementName);
                            if (objTypePrepentTags == null) {
                                objTypePrepentTags = SingleString.of(m_tgmConfig.objTypePrependTags);
                                m_objTypePrependTagsMap.put(m_tgmConfig.measurementName, objTypePrepentTags);
                            }
                            m_objTypePrependTags = objTypePrepentTags;
                            tvObjTypePrependTag.setInput(objTypePrepentTags);

                            List<SingleString> objTypeAppendTags = m_objTypeAppendTagsMap.get(m_tgmConfig.measurementName);
                            if (objTypeAppendTags == null) {
                                objTypeAppendTags = SingleString.of(m_tgmConfig.objTypeAppendTags);
                                m_objTypeAppendTagsMap.put(m_tgmConfig.measurementName, objTypeAppendTags);
                            }
                            m_objTypeAppendTags = objTypeAppendTags;
                            tvObjTypeAppendTag.setInput(objTypeAppendTags);

                            List<SingleString> objNameAppendTags = m_objNameAppendTagsMap.get(m_tgmConfig.measurementName);
                            if (objNameAppendTags == null) {
                                objNameAppendTags = SingleString.of(m_tgmConfig.objNameAppendTags);
                                m_objNameAppendTagsMap.put(m_tgmConfig.measurementName, objNameAppendTags);
                            }
                            m_objNameAppendTags = objNameAppendTags;
                            tvObjNameAppendTag.setInput(objNameAppendTags);

                            //counter mapping
                            tvCounterMapping.setInput(m_tgmConfig.counterMappings);

                            //stack
                            StackLayout stack = (StackLayout) compContentsBody.getLayout();
                            stack.topControl = compMeasurement;
                            compContentsBody.layout();

                        } else if (model.displayName.equals("General")) {
                            StackLayout stack = (StackLayout) compContentsBody.getLayout();
                            stack.topControl = compGeneral;
                            compContentsBody.layout();
                        }
                    }
                }
            }
        });
        treeViewer.setAutoExpandLevel(2);
        Tree tree = treeViewer.getTree();
        GridData gd_tree = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gd_tree.widthHint = 160;
        tree.setLayoutData(gd_tree);
        treeViewer.setLabelProvider(new ViewerLabelProvider());
        treeViewer.setContentProvider(new TreeContentProvider());

        Button btnAddMeasurement = new Button(compMenu, SWT.NONE);
        btnAddMeasurement.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                SingleValueDialog dialog = new SingleValueDialog(parent.getShell(), "Measurement name");
                dialog.create("Add Measurement", "add measurement name here.");
                if (dialog.open() == Window.OK && StringUtil.isNotEmpty(dialog.getValue())) {
                    List<String> nameList = measurementsParent.child.stream().map(TreeModel::getDisplayName).collect(Collectors.toList());
                    if (!nameList.contains(dialog.getValue())) {
                        TgmConfig tconfig = new TgmConfig(dialog.getValue());
                        new TreeModel(measurementsParent, dialog.getValue(), tconfig);
                        treeViewer.refresh();
                    }
                }
            }
        });
        btnAddMeasurement.setImage(ImageUtil.getImageDescriptor(Images.add).createImage());
        btnAddMeasurement.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        btnAddMeasurement.setText("Add Measurement");

        Button btnRemoveMeasurement = new Button(compMenu, SWT.NONE);
        btnRemoveMeasurement.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                StructuredSelection selection = (StructuredSelection) treeViewer.getSelection();
                TreeModel model = (TreeModel) selection.getFirstElement();
                if (model.element != null && model.element instanceof TgmConfig) {
                    model.removeMe();
                    treeViewer.refresh();
                }
            }
        });
        btnRemoveMeasurement.setImage(ImageUtil.getImageDescriptor(Images.minus).createImage());
        btnRemoveMeasurement.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        btnRemoveMeasurement.setText("Remove Measurement");


        //============================================================================
        // Contents area
        //============================================================================
        Composite compContents = new Composite(mainSash, SWT.NONE);
        compContents.setLayout(new GridLayout(1, false));

        compContentsBody = new Composite(compContents, SWT.BORDER);
        compContentsBody.setLayout(new StackLayout());
        compContentsBody.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        //============================================================================
        // General Config
        //============================================================================
        compGeneral = new Composite(compContentsBody, SWT.NONE);
        compGeneral.setLayout(new GridLayout(2, false));

        Label lblTgGeneral0 = new Label(compGeneral, SWT.NONE);
        lblTgGeneral0.setAlignment(SWT.RIGHT);
        GridData gd_lblTgg0 = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
        gd_lblTgg0.widthHint = 150;
        lblTgGeneral0.setLayoutData(gd_lblTgg0);
        lblTgGeneral0.setText("Enabled");

        btnTgmEnabled = new Button(compGeneral, SWT.CHECK);
        btnTgmEnabled.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		Button button = (Button) e.widget;
        		m_tgmConfig.enabled = button.getSelection();
        	}
        });
        btnTgmEnabled.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));

        Label lblTgGeneral1 = new Label(compGeneral, SWT.NONE);
        lblTgGeneral1.setAlignment(SWT.RIGHT);
        GridData gd_lblTgg1 = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
        gd_lblTgg1.widthHint = 150;
        lblTgGeneral1.setLayoutData(gd_lblTgg1);
        lblTgGeneral1.setText("Debug Enabled");

        btnTgmDebugEnabled = new Button(compGeneral, SWT.CHECK);
        btnTgmDebugEnabled.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		Button button = (Button) e.widget;
        		m_tgmConfig.debugEnabled= button.getSelection();
        	}
        });
        btnTgmDebugEnabled.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));

        Label lblTgGeneral2 = new Label(compGeneral, SWT.NONE);
        lblTgGeneral2.setAlignment(SWT.RIGHT);
        GridData gd_lblTgGeneral2 = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
        gd_lblTgGeneral2.widthHint = 150;
        lblTgGeneral2.setLayoutData(gd_lblTgGeneral2);
        lblTgGeneral2.setText("Normalize Delta-Counter");

        btnTgmNormalizeDefault = new Button(compGeneral, SWT.CHECK);
        btnTgmNormalizeDefault.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));

        Label lblTgGeneral3 = new Label(compGeneral, SWT.NONE);
        lblTgGeneral3.setAlignment(SWT.RIGHT);
        GridData gd_lblTgg3 = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
        gd_lblTgg3.widthHint = 150;
        lblTgGeneral3.setLayoutData(gd_lblTgg3);
        lblTgGeneral3.setText("Delta Normalizing Sec.");

        txtNormalizeSec = new Text(compGeneral, SWT.BORDER);
        txtNormalizeSec.addVerifyListener(new VerifyListener() {
            public void verifyText(VerifyEvent e) {
                e.doit = StringUtil.isInteger(e.text);
            }
        });
        txtNormalizeSec.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Label lblTgGeneral4 = new Label(compGeneral, SWT.NONE);
        lblTgGeneral4.setAlignment(SWT.RIGHT);
        GridData gd_lblTgg4 = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
        gd_lblTgg4.widthHint = 150;
        lblTgGeneral4.setLayoutData(gd_lblTgg4);
        lblTgGeneral4.setText("Dead marking time(ms)");

        txtTgObjDeadTimeMs = new Text(compGeneral, SWT.BORDER);
        txtTgObjDeadTimeMs.addVerifyListener(new VerifyListener() {
            public void verifyText(VerifyEvent e) {
                e.doit = StringUtil.isInteger(e.text);
            }
        });
        txtTgObjDeadTimeMs.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));


        //============================================================================
        // Measurement Config Tabs
        //============================================================================
        compMeasurement = new Composite(compContentsBody, SWT.NONE);
        compMeasurement.setLayout(new FillLayout(SWT.HORIZONTAL));

        TabFolder tabFolder = new TabFolder(compMeasurement, SWT.NONE);

        TabItem tiGeneral = new TabItem(tabFolder, SWT.NONE);
        tiGeneral.setText("General Setting");

        TabItem tiObject = new TabItem(tabFolder, SWT.NONE);
        tiObject.setText("Object Setting");

        TabItem tiCounter = new TabItem(tabFolder, SWT.NONE);
        tiCounter.setText("Counter Mapping");

        //============================================================================
        // Measurement Config - General
        //============================================================================
        ScrolledComposite sc1 = new ScrolledComposite(tabFolder, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        sc1.setExpandVertical(true);
        sc1.setExpandHorizontal(true);
        tiGeneral.setControl(sc1);
        sc1.setLayout(new GridLayout(1, false));

        Composite cmpMtGeneral = new Composite(sc1, SWT.NONE);
        cmpMtGeneral.setLayout(new GridLayout(2, false));
        sc1.setContent(cmpMtGeneral);
        sc1.setMinSize(new Point(300, 300));

        Label lblMtGeneralName = new Label(cmpMtGeneral, SWT.RIGHT);
        lblMtGeneralName.setAlignment(SWT.RIGHT);
        GridData gd_lblMtGeneralName = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
        gd_lblMtGeneralName.widthHint = 150;
        lblMtGeneralName.setLayoutData(gd_lblMtGeneralName);
        lblMtGeneralName.setText("Measurement");

        txtMtGeneralName = new Text(cmpMtGeneral, SWT.BORDER);
        txtMtGeneralName.setForeground(ColorUtil.getInstance().getColor("blue"));
        txtMtGeneralName.setEditable(false);
        txtMtGeneralName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        txtMtGeneralName.setEnabled(false);

        Label lblMtGeneral0 = new Label(cmpMtGeneral, SWT.RIGHT);
        lblMtGeneral0.setAlignment(SWT.RIGHT);
        GridData gd_lblMtGeneral0 = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
        gd_lblMtGeneral0.widthHint = 150;
        lblMtGeneral0.setLayoutData(gd_lblMtGeneral0);
        lblMtGeneral0.setText("Enabled");

        btnMtEnabled = new Button(cmpMtGeneral, SWT.CHECK);
        btnMtEnabled.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		Button button = (Button) e.widget;
        		m_tgmConfig.enabled = button.getSelection();
        	}
        });
        btnMtEnabled.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));

        Label lblMtGeneral1 = new Label(cmpMtGeneral, SWT.RIGHT);
        lblMtGeneral1.setAlignment(SWT.RIGHT);
        GridData gd_lblMtGeneral1 = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
        gd_lblMtGeneral1.widthHint = 150;
        lblMtGeneral1.setLayoutData(gd_lblMtGeneral1);
        lblMtGeneral1.setText("Debug Enabled");

        btnMtDebugEnabled = new Button(cmpMtGeneral, SWT.CHECK);
        btnMtDebugEnabled.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		Button button = (Button) e.widget;
        		m_tgmConfig.debugEnabled = button.getSelection();
        	}
        });
        btnMtDebugEnabled.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));

        Label lblMtGeneral2 = new Label(cmpMtGeneral, SWT.RIGHT);
        lblMtGeneral2.setAlignment(SWT.RIGHT);
        GridData gd_lblMtGeneral2 = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
        gd_lblMtGeneral2.widthHint = 150;
        lblMtGeneral2.setLayoutData(gd_lblMtGeneral2);
        lblMtGeneral2.setText("Host Tag");

        txtMtHostTag = new Text(cmpMtGeneral, SWT.BORDER);
        txtMtHostTag.addModifyListener(new ModifyListener() {
        	public void modifyText(ModifyEvent e) {
        		Text text = (Text) e.widget;
        		m_tgmConfig.hostTag = text.getText();
        	}
        });
        txtMtHostTag.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Label lblMtGeneral3 = new Label(cmpMtGeneral, SWT.RIGHT);
        lblMtGeneral3.setAlignment(SWT.RIGHT);
        GridData gd_lblMtGeneral3 = new GridData(SWT.RIGHT, SWT.TOP, false, false, 1, 1);
        gd_lblMtGeneral3.widthHint = 150;
        lblMtGeneral3.setLayoutData(gd_lblMtGeneral3);
        lblMtGeneral3.setText("Host Mappings");

        Composite composite = new Composite(cmpMtGeneral, SWT.NONE);
        GridData gd_composite = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 2);
        gd_composite.heightHint = 110;
        composite.setLayoutData(gd_composite);
        TableColumnLayout layoutHostMappingTable = new TableColumnLayout();
        composite.setLayout(layoutHostMappingTable);

        tvHostMapping = new TableViewer(composite, SWT.BORDER | SWT.FULL_SELECTION);
        tblHostMapping = tvHostMapping.getTable();
        tblHostMapping.setHeaderVisible(true);
        tblHostMapping.setLinesVisible(true);
        tvHostMapping.setContentProvider(ArrayContentProvider.getInstance());

        createHostColumn(tvHostMapping, layoutHostMappingTable);

        Composite cmpHostMappingBtn = new Composite(cmpMtGeneral, SWT.NONE);
        GridLayout gl_cmpHostMappingBtn = new GridLayout(2, false);
        gl_cmpHostMappingBtn.marginWidth = 0;
        gl_cmpHostMappingBtn.marginHeight = 0;
        gl_cmpHostMappingBtn.horizontalSpacing = 0;
        cmpHostMappingBtn.setLayout(gl_cmpHostMappingBtn);
        cmpHostMappingBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));

        Button btnHostMappingAdd = new Button(cmpHostMappingBtn, SWT.RIGHT);
        btnHostMappingAdd.setImage(Images.add);
        btnHostMappingAdd.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                m_tgmConfig.hostMappings.add(new TgmConfig.HostMapping("[tg-host]", "[scouter-host]"));
                tvHostMapping.refresh();
            }
        });
        btnHostMappingAdd.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));

        Button btnHostMappingDelete = new Button(cmpHostMappingBtn, SWT.RIGHT);
        btnHostMappingDelete.setImage(Images.minus);
        btnHostMappingDelete.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int index = tblHostMapping.getSelectionIndex();
                if (index >= 0) {
                    m_tgmConfig.hostMappings.remove(index);
                }
                tvHostMapping.refresh();
            }
        });

        Label lblMtGeneral4 = new Label(cmpMtGeneral, SWT.RIGHT);
        lblMtGeneral4.setAlignment(SWT.RIGHT);
        GridData gd_lblMtGeneral4 = new GridData(SWT.RIGHT, SWT.TOP, false, false, 1, 1);
        gd_lblMtGeneral4.widthHint = 150;
        lblMtGeneral4.setLayoutData(gd_lblMtGeneral4);
        lblMtGeneral4.setText("Tag Filters");

        Composite compTagFilter = new Composite(cmpMtGeneral, SWT.NONE);
        GridData gd_compTagFilter = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 2);
        gd_compTagFilter.heightHint = 110;
        compTagFilter.setLayoutData(gd_compTagFilter);
        TableColumnLayout layoutTagFilterTable = new TableColumnLayout();
        compTagFilter.setLayout(layoutTagFilterTable);

        tvTagFilter = new TableViewer(compTagFilter, SWT.BORDER | SWT.FULL_SELECTION);
        tblTagFilter = tvTagFilter.getTable();
        tblTagFilter.setHeaderVisible(true);
        tblTagFilter.setLinesVisible(true);
        tvTagFilter.setContentProvider(ArrayContentProvider.getInstance());
        createTagFilterColumn(tvTagFilter, layoutTagFilterTable);

        Composite cmpTagFilterBtn = new Composite(cmpMtGeneral, SWT.NONE);
        GridLayout gl_cmpTagFilterBtn = new GridLayout(2, false);
        gl_cmpTagFilterBtn.marginWidth = 0;
        gl_cmpTagFilterBtn.marginHeight = 0;
        gl_cmpTagFilterBtn.horizontalSpacing = 0;
        cmpTagFilterBtn.setLayout(gl_cmpTagFilterBtn);
        cmpTagFilterBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));

        Button btnTagFilterAdd = new Button(cmpTagFilterBtn, SWT.RIGHT);
        btnTagFilterAdd.setImage(Images.add);
        btnTagFilterAdd.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                m_tagFilterMappings.add(new TagFilterMapping("[tag]", "[mapping value]"));
                tvTagFilter.refresh();
            }
        });
        btnTagFilterAdd.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));

        Button btnTagFilterDelete = new Button(cmpTagFilterBtn, SWT.RIGHT);
        btnTagFilterDelete.setImage(Images.minus);
        btnTagFilterDelete.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int index = tblTagFilter.getSelectionIndex();
                if (index >= 0) {
                    m_tagFilterMappings.remove(index);
                }
                tvTagFilter.refresh();
            }
        });

        //============================================================================
        // Measurement Config - Object
        //============================================================================
        ScrolledComposite sc2 = new ScrolledComposite(tabFolder, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        sc2.setExpandVertical(true);
        sc2.setExpandHorizontal(true);
        tiObject.setControl(sc2);
        sc2.setLayout(new GridLayout(1, false));
        
        Composite cmpMtObj = new Composite(sc2, SWT.NONE);
        sc2.setContent(cmpMtObj);
        sc2.setMinSize(new Point(300, 300));
        cmpMtObj.setLayout(new GridLayout(2, false));

        //objFamily ...............
        Label lblMtObjFamily = new Label(cmpMtObj, SWT.RIGHT);
        lblMtObjFamily.setAlignment(SWT.RIGHT);
        GridData gd_lblMtObjFamily = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
        gd_lblMtObjFamily.widthHint = 150;
        lblMtObjFamily.setLayoutData(gd_lblMtObjFamily);
        lblMtObjFamily.setText("Family");

        txtMtObjFamily = new Text(cmpMtObj, SWT.BORDER);
        txtMtObjFamily.addModifyListener(new ModifyListener() {
        	public void modifyText(ModifyEvent e) {
        		Text text = (Text) e.widget;
        		m_tgmConfig.objFamilyBase = text.getText();
        	}
        });
        txtMtObjFamily.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Label lblMtObjFamilyAppendTags = new Label(cmpMtObj, SWT.RIGHT);
        lblMtObjFamilyAppendTags.setAlignment(SWT.RIGHT);
        GridData gd_lblMtFamilyAppendTags = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
        gd_lblMtFamilyAppendTags.widthHint = 150;
        lblMtObjFamilyAppendTags.setLayoutData(gd_lblMtFamilyAppendTags);
        lblMtObjFamilyAppendTags.setText("Family Append Tags");

        Composite compFamilyAppendTags = new Composite(cmpMtObj, SWT.NONE);
        GridData gd_compFamilyAppendTags = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 2);
        gd_compFamilyAppendTags.heightHint = 63;
        compFamilyAppendTags.setLayoutData(gd_compFamilyAppendTags);
        TableColumnLayout layoutFamilyAppendTagTable = new TableColumnLayout();
        compFamilyAppendTags.setLayout(layoutFamilyAppendTagTable);

        tvFamilyAppendTag = new TableViewer(compFamilyAppendTags, SWT.BORDER | SWT.FULL_SELECTION);
        tblFamilyAppendTag = tvFamilyAppendTag.getTable();
        tblFamilyAppendTag.setHeaderVisible(false);
        tblFamilyAppendTag.setLinesVisible(true);
        tvFamilyAppendTag.setContentProvider(ArrayContentProvider.getInstance());
        createSingleTagColumn(tvFamilyAppendTag, layoutFamilyAppendTagTable);
        
        Composite cmpFamilyAppendTagBtn = new Composite(cmpMtObj, SWT.NONE);
        GridLayout gl_cmpFamilyAppendTagBtn = new GridLayout(2, false);
        gl_cmpFamilyAppendTagBtn.marginWidth = 0;
        gl_cmpFamilyAppendTagBtn.marginHeight = 0;
        gl_cmpFamilyAppendTagBtn.horizontalSpacing = 0;
        cmpFamilyAppendTagBtn.setLayout(gl_cmpFamilyAppendTagBtn);
        cmpFamilyAppendTagBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));

        Button btnFamilyAppendTagAdd = new Button(cmpFamilyAppendTagBtn, SWT.RIGHT);
        btnFamilyAppendTagAdd.setImage(Images.add);
        btnFamilyAppendTagAdd.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                m_familyAppendTags.add(new SingleString("[tag]"));
                tvFamilyAppendTag.refresh();
            }
        });
        btnFamilyAppendTagAdd.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));

        Button btnFamilyAppendTagDelete = new Button(cmpFamilyAppendTagBtn, SWT.RIGHT);
        btnFamilyAppendTagDelete.setImage(Images.minus);
        btnFamilyAppendTagDelete.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int index = tblFamilyAppendTag.getSelectionIndex();
                if (index >= 0) {
                    m_familyAppendTags.remove(index);
                }
                tvFamilyAppendTag.refresh();
            }
        });



        //objType ...................

        Label lblMtObjType = new Label(cmpMtObj, SWT.RIGHT);
        lblMtObjType.setAlignment(SWT.RIGHT);
        GridData gd_lblMtObjType = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
        gd_lblMtObjType.widthHint = 150;
        lblMtObjType.setLayoutData(gd_lblMtObjType);
        lblMtObjType.setText("Object Type");

        txtMtObjType = new Text(cmpMtObj, SWT.BORDER);
        txtMtObjType.addModifyListener(new ModifyListener() {
        	public void modifyText(ModifyEvent e) {
        		Text text = (Text) e.widget;
        		m_tgmConfig.objTypeBase = text.getText();
        	}
        });
        txtMtObjType.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Label lblMtObjTypeIcon = new Label(cmpMtObj, SWT.RIGHT);
        lblMtObjTypeIcon.setAlignment(SWT.RIGHT);
        GridData gd_lblMtObjTypeICon = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
        gd_lblMtObjTypeICon.widthHint = 150;
        lblMtObjTypeIcon.setLayoutData(gd_lblMtObjTypeICon);
        lblMtObjTypeIcon.setText("Object Type Icon");

        txtMtObjTypeIcon = new Text(cmpMtObj, SWT.BORDER);
        txtMtObjTypeIcon.addModifyListener(new ModifyListener() {
        	public void modifyText(ModifyEvent e) {
        		Text text = (Text) e.widget;
        		m_tgmConfig.objTypeIcon = text.getText();
        	}
        });
        txtMtObjTypeIcon.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Label lblMtObjTypePrependTags = new Label(cmpMtObj, SWT.RIGHT);
        lblMtObjTypePrependTags.setAlignment(SWT.RIGHT);
        GridData gd_lblMtObjTypePrependTags = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
        gd_lblMtObjTypePrependTags.widthHint = 150;
        lblMtObjTypePrependTags.setLayoutData(gd_lblMtObjTypePrependTags);
        lblMtObjTypePrependTags.setText("ObjType Prepend Tags");

        Composite compObjTypePrependTags = new Composite(cmpMtObj, SWT.NONE);
        GridData gd_compObjTypePrependTags = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 2);
        gd_compObjTypePrependTags.heightHint = 63;
        compObjTypePrependTags.setLayoutData(gd_compObjTypePrependTags);
        TableColumnLayout layoutObjTypePrependTagTable = new TableColumnLayout();
        compObjTypePrependTags.setLayout(layoutObjTypePrependTagTable);

        tvObjTypePrependTag = new TableViewer(compObjTypePrependTags, SWT.BORDER | SWT.FULL_SELECTION);
        tblObjTypePrependTag = tvObjTypePrependTag.getTable();
        tblObjTypePrependTag.setHeaderVisible(false);
        tblObjTypePrependTag.setLinesVisible(true);
        tvObjTypePrependTag.setContentProvider(ArrayContentProvider.getInstance());
        createSingleTagColumn(tvObjTypePrependTag, layoutObjTypePrependTagTable);

        Composite cmpObjTypePrependTagBtn = new Composite(cmpMtObj, SWT.NONE);
        GridLayout gl_cmpObjTypePrependTagBtn = new GridLayout(2, false);
        gl_cmpObjTypePrependTagBtn.marginWidth = 0;
        gl_cmpObjTypePrependTagBtn.marginHeight = 0;
        gl_cmpObjTypePrependTagBtn.horizontalSpacing = 0;
        cmpObjTypePrependTagBtn.setLayout(gl_cmpObjTypePrependTagBtn);
        cmpObjTypePrependTagBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));

        Button btnObjTypePrependTagAdd = new Button(cmpObjTypePrependTagBtn, SWT.RIGHT);
        btnObjTypePrependTagAdd.setImage(Images.add);
        btnObjTypePrependTagAdd.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                m_objTypePrependTags.add(new SingleString("[tag]"));
                tvObjTypePrependTag.refresh();
            }
        });
        btnObjTypePrependTagAdd.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));

        Button btnObjTypePrependTagDelete = new Button(cmpObjTypePrependTagBtn, SWT.RIGHT);
        btnObjTypePrependTagDelete.setImage(Images.minus);
        btnObjTypePrependTagDelete.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int index = tblObjTypePrependTag.getSelectionIndex();
                if (index >= 0) {
                    m_objTypePrependTags.remove(index);
                }
                tvObjTypePrependTag.refresh();
            }
        });



        Label lblMtObjTypeAppendTags = new Label(cmpMtObj, SWT.RIGHT);
        lblMtObjTypeAppendTags.setAlignment(SWT.RIGHT);
        GridData gd_lblMtObjTypeAppendTags = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
        gd_lblMtObjTypeAppendTags.widthHint = 150;
        lblMtObjTypeAppendTags.setLayoutData(gd_lblMtObjTypeAppendTags);
        lblMtObjTypeAppendTags.setText("ObjType Append Tags");

        Composite compObjTypeAppendTags = new Composite(cmpMtObj, SWT.NONE);
        GridData gd_compObjTypeAppendTags = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 2);
        gd_compObjTypeAppendTags.heightHint = 63;
        compObjTypeAppendTags.setLayoutData(gd_compObjTypeAppendTags);
        TableColumnLayout layoutObjTypeAppendTagTable = new TableColumnLayout();
        compObjTypeAppendTags.setLayout(layoutObjTypeAppendTagTable);

        tvObjTypeAppendTag = new TableViewer(compObjTypeAppendTags, SWT.BORDER | SWT.FULL_SELECTION);
        tblObjTypeAppendTag = tvObjTypeAppendTag.getTable();
        tblObjTypeAppendTag.setHeaderVisible(false);
        tblObjTypeAppendTag.setLinesVisible(true);
        tvObjTypeAppendTag.setContentProvider(ArrayContentProvider.getInstance());
        createSingleTagColumn(tvObjTypeAppendTag, layoutObjTypeAppendTagTable);

        Composite cmpObjTypeAppendTagBtn = new Composite(cmpMtObj, SWT.NONE);
        cmpObjTypeAppendTagBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
        GridLayout gl_cmpObjTypeAppendTagBtn = new GridLayout(2, false);
        gl_cmpObjTypeAppendTagBtn.marginWidth = 0;
        gl_cmpObjTypeAppendTagBtn.marginHeight = 0;
        gl_cmpObjTypeAppendTagBtn.horizontalSpacing = 0;
        cmpObjTypeAppendTagBtn.setLayout(gl_cmpObjTypeAppendTagBtn);

        Button btnObjTypeAppendTagAdd = new Button(cmpObjTypeAppendTagBtn, SWT.RIGHT);
        btnObjTypeAppendTagAdd.setImage(Images.add);
        btnObjTypeAppendTagAdd.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                m_objTypeAppendTags.add(new SingleString("[tag]"));
                tvObjTypeAppendTag.refresh();
            }
        });
        btnObjTypeAppendTagAdd.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));

        Button btnObjTypeAppendTagDelete = new Button(cmpObjTypeAppendTagBtn, SWT.RIGHT);
        btnObjTypeAppendTagDelete.setImage(Images.minus);
        btnObjTypeAppendTagDelete.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int index = tblObjTypeAppendTag.getSelectionIndex();
                if (index >= 0) {
                    m_objTypeAppendTags.remove(index);
                }
                tvObjTypeAppendTag.refresh();
            }
        });


        //objName ...................

        Label lblMtObjName = new Label(cmpMtObj, SWT.RIGHT);
        lblMtObjName.setAlignment(SWT.RIGHT);
        GridData gd_lblMtObjName = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
        gd_lblMtObjName.widthHint = 150;
        lblMtObjName.setLayoutData(gd_lblMtObjName);
        lblMtObjName.setText("Object Name");

        txtMtObjName = new Text(cmpMtObj, SWT.BORDER);
        txtMtObjName.addModifyListener(new ModifyListener() {
        	public void modifyText(ModifyEvent e) {
        		Text text = (Text) e.widget;
        		m_tgmConfig.objNameBase = text.getText();
        	}
        });
        txtMtObjName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Label lblMtObjNameAppendTags = new Label(cmpMtObj, SWT.RIGHT);
        lblMtObjNameAppendTags.setAlignment(SWT.RIGHT);
        GridData gd_lblMtObjNameAppendTags = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
        gd_lblMtObjNameAppendTags.widthHint = 150;
        lblMtObjNameAppendTags.setLayoutData(gd_lblMtObjNameAppendTags);
        lblMtObjNameAppendTags.setText("ObjName Append Tags");

        Composite compObjNameAppendTags = new Composite(cmpMtObj, SWT.NONE);
        GridData gd_compObjNameAppendTags = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 2);
        gd_compObjNameAppendTags.heightHint = 63;
        compObjNameAppendTags.setLayoutData(gd_compObjNameAppendTags);
        TableColumnLayout layoutObjNameAppendTagTable = new TableColumnLayout();
        compObjNameAppendTags.setLayout(layoutObjNameAppendTagTable);

        tvObjNameAppendTag = new TableViewer(compObjNameAppendTags, SWT.BORDER | SWT.FULL_SELECTION);
        tblObjNameAppendTag = tvObjNameAppendTag.getTable();
        tblObjNameAppendTag.setHeaderVisible(false);
        tblObjNameAppendTag.setLinesVisible(true);
        tvObjNameAppendTag.setContentProvider(ArrayContentProvider.getInstance());
        createSingleTagColumn(tvObjNameAppendTag, layoutObjNameAppendTagTable);

        Composite cmpObjNameAppendTagBtn = new Composite(cmpMtObj, SWT.NONE);
        cmpObjNameAppendTagBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
        GridLayout gl_cmpObjNameAppendTagBtn = new GridLayout(2, false);
        gl_cmpObjNameAppendTagBtn.marginWidth = 0;
        gl_cmpObjNameAppendTagBtn.marginHeight = 0;
        gl_cmpObjNameAppendTagBtn.horizontalSpacing = 0;
        cmpObjNameAppendTagBtn.setLayout(gl_cmpObjNameAppendTagBtn);

        Button btnObjNameAppendTagAdd = new Button(cmpObjNameAppendTagBtn, SWT.RIGHT);
        btnObjNameAppendTagAdd.setImage(Images.add);
        btnObjNameAppendTagAdd.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                m_objNameAppendTags.add(new SingleString("[tag]"));
                tvObjNameAppendTag.refresh();
            }
        });
        btnObjNameAppendTagAdd.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));

        Button btnObjNameAppendTagDelete = new Button(cmpObjNameAppendTagBtn, SWT.RIGHT);
        btnObjNameAppendTagDelete.setImage(Images.minus);
        new Label(cmpMtObj, SWT.NONE);
        new Label(cmpMtObj, SWT.NONE);
        btnObjNameAppendTagDelete.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int index = tblObjNameAppendTag.getSelectionIndex();
                if (index >= 0) {
                    m_objNameAppendTags.remove(index);
                }
                tvObjNameAppendTag.refresh();
            }
        });

        //============================================================================
        // Measurement Config - Counter
        //============================================================================
        ScrolledComposite sc3 = new ScrolledComposite(tabFolder, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        sc3.setExpandVertical(true);
        sc3.setExpandHorizontal(true);
        tiCounter.setControl(sc3);
        sc3.setLayout(new GridLayout(1, false));
        
        Composite cmpCounterMapping = new Composite(sc3, SWT.NONE);
        sc3.setContent(cmpCounterMapping);
        sc3.setMinSize(cmpCounterMapping.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        cmpCounterMapping.setLayout(new GridLayout(1, false));
        
        Composite compCounterMappingButton = new Composite(cmpCounterMapping, SWT.NONE);
        GridLayout gl_compCounterMappingButton = new GridLayout(2, false);
        compCounterMappingButton.setLayout(gl_compCounterMappingButton);
        compCounterMappingButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
        
        Button btnCounterMappingAdd = new Button(compCounterMappingButton, SWT.NONE);
        btnCounterMappingAdd.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false, 1, 1));
        btnCounterMappingAdd.setImage(Images.add);
        btnCounterMappingAdd.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            	m_tgmConfig.counterMappings.add(new TgCounterMapping("[field]", "[counter]"));
                tvCounterMapping.refresh();
            }
        });
        
        Button btnCounterMappingRemove = new Button(compCounterMappingButton, SWT.NONE);
        btnCounterMappingRemove.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        btnCounterMappingRemove.setImage(Images.minus);
        btnCounterMappingRemove.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int index = tblCounterMapping.getSelectionIndex();
                if (index >= 0) {
                    m_tgmConfig.counterMappings.remove(index);
                }
                tvCounterMapping.refresh();
            }
        });

        Composite cmpCounterMappingTable = new Composite(cmpCounterMapping, SWT.NONE);
        cmpCounterMappingTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2));
        TableColumnLayout layoutCounterMappingTable = new TableColumnLayout();
        cmpCounterMappingTable.setLayout(layoutCounterMappingTable);

        tvCounterMapping = new TableViewer(cmpCounterMappingTable, SWT.BORDER | SWT.FULL_SELECTION);
        tblCounterMapping = tvCounterMapping.getTable();
        tblCounterMapping.setHeaderVisible(true);
        tblCounterMapping.setLinesVisible(true);
        tvCounterMapping.setContentProvider(ArrayContentProvider.getInstance());
        createCounterMappingColumn(tvCounterMapping, layoutCounterMappingTable);

        mainSash.setWeights(new int[]{1, 3});

        createActions();
        initializeToolBar();
        initializeMenu();
    }

    private void createHostColumn(TableViewer tvHostMapping, TableColumnLayout layoutHostMappingTable) {
        TableViewerColumn vcolTelegraf = new TableViewerColumn(tvHostMapping, SWT.NONE);
        TableColumn colTelegraf = vcolTelegraf.getColumn();
        layoutHostMappingTable.setColumnData(colTelegraf, new ColumnWeightData(50, 30, true));
        colTelegraf.setAlignment(SWT.CENTER);
        colTelegraf.setText("Telegraf");
        vcolTelegraf.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((TgmConfig.HostMapping) element).telegraf;
            }
        });
        vcolTelegraf.setEditingSupport(new HostTelegrafEditingSupport(tvHostMapping));

        TableViewerColumn vcolScouter = new TableViewerColumn(tvHostMapping, SWT.NONE);
        TableColumn colScouter = vcolScouter.getColumn();
        layoutHostMappingTable.setColumnData(colScouter, new ColumnWeightData(50, 30, true));
        colScouter.setText("Scouter");
        vcolScouter.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((TgmConfig.HostMapping) element).scouter;
            }
        });
        vcolScouter.setEditingSupport(new HostScouterEditingSupport(tvHostMapping));
    }

    private void createTagFilterColumn(TableViewer tv, TableColumnLayout layoutHostMappingTable) {
        TableViewerColumn vcolTag = new TableViewerColumn(tv, SWT.NONE);
        TableColumn colTag = vcolTag.getColumn();
        layoutHostMappingTable.setColumnData(colTag, new ColumnWeightData(50, 30, true));
        colTag.setAlignment(SWT.CENTER);
        colTag.setText("Tag name");
        vcolTag.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((TagFilterMapping) element).tag;
            }
        });
        vcolTag.setEditingSupport(new TagMappingTagEditingSupport(tv));

        TableViewerColumn vcolMatch = new TableViewerColumn(tv, SWT.NONE);
        TableColumn colMatch = vcolMatch.getColumn();
        layoutHostMappingTable.setColumnData(colMatch, new ColumnWeightData(50, 30, true));
        colMatch.setText("Tag value");
        vcolMatch.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((TagFilterMapping) element).mappingValue;
            }
        });
        vcolMatch.setEditingSupport(new TagMappingValueEditingSupport(tv));
    }

    private void createSingleTagColumn(TableViewer tv, TableColumnLayout tbl) {
        TableViewerColumn vcolTag = new TableViewerColumn(tv, SWT.NONE);
        TableColumn colTag = vcolTag.getColumn();
        tbl.setColumnData(colTag, new ColumnWeightData(100, 30, true));
        colTag.setAlignment(SWT.CENTER);
        colTag.setText("Append Tag");
        vcolTag.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((SingleString) element).value;
            }
        });
        vcolTag.setEditingSupport(new SingleColumnEditingSupport(tv));
    }

    private void createCounterMappingColumn(TableViewer tv, TableColumnLayout tbl) {
        TableViewerColumn vcolTgFieldName = new TableViewerColumn(tv, SWT.NONE);
        TableColumn colTgFieldName = vcolTgFieldName.getColumn();
        tbl.setColumnData(colTgFieldName, new ColumnWeightData(15, 30, true));
        colTgFieldName.setAlignment(SWT.LEFT);
        colTgFieldName.setText("Tg-Field");
        vcolTgFieldName.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((TgCounterMapping) element).tgFieldName;
            }
        });
        vcolTgFieldName.setEditingSupport(new CounterMappingTgFieldEditingSupport(tv));

        TableViewerColumn vcolCounterName = new TableViewerColumn(tv, SWT.NONE);
        TableColumn colCounterName = vcolCounterName.getColumn();
        tbl.setColumnData(colCounterName, new ColumnWeightData(20, 30, true));
        colCounterName.setAlignment(SWT.LEFT);
        colCounterName.setText("Counter");
        vcolCounterName.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((TgCounterMapping) element).counterName;
            }
        });
        vcolCounterName.setEditingSupport(new CounterMappingCounterNameEditingSupport(tv));

        TableViewerColumn vcolDeltaType = new TableViewerColumn(tv, SWT.NONE);
        TableColumn colDeltaType = vcolDeltaType.getColumn();
        tbl.setColumnData(colDeltaType, new ColumnWeightData(10, 30, true));
        colDeltaType.setAlignment(SWT.LEFT);
        colDeltaType.setText("DeltaType");
        vcolDeltaType.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((TgCounterMapping) element).deltaType.name();
            }
        });
        vcolDeltaType.setEditingSupport(new CounterMappingDeltaTypeSupport(tv));

        TableViewerColumn vcolDisplayName = new TableViewerColumn(tv, SWT.NONE);
        TableColumn colDisplayName = vcolDisplayName.getColumn();
        tbl.setColumnData(colDisplayName, new ColumnWeightData(20, 15, true));
        colDisplayName.setAlignment(SWT.LEFT);
        colDisplayName.setText("Display Name");
        vcolDisplayName.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((TgCounterMapping) element).displayName;
            }
        });
        vcolDisplayName.setEditingSupport(new CounterMappingDisplayNameEditingSupport(tv));

        TableViewerColumn vcolUnit = new TableViewerColumn(tv, SWT.NONE);
        TableColumn colUnit = vcolUnit.getColumn();
        tbl.setColumnData(colUnit, new ColumnWeightData(5, 12, true));
        colUnit.setAlignment(SWT.LEFT);
        colUnit.setText("Unit");
        vcolUnit.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((TgCounterMapping) element).unit;
            }
        });
        vcolUnit.setEditingSupport(new CounterMappingUnitEditingSupport(tv));

        TableViewerColumn vcolNormalizeSec = new TableViewerColumn(tv, SWT.NONE);
        TableColumn colNormalizeSec = vcolNormalizeSec.getColumn();
        tbl.setColumnData(colNormalizeSec, new ColumnWeightData(5, 15, true));
        colNormalizeSec.setAlignment(SWT.LEFT);
        colNormalizeSec.setText("Normalize(sec)");
        vcolNormalizeSec.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return String.valueOf(((TgCounterMapping) element).normalizeSeconds);
            }
        });
        vcolNormalizeSec.setEditingSupport(new CounterMappingNormalizeSecSupport(tv));

        TableViewerColumn vcolTotalizable = new TableViewerColumn(tv, SWT.NONE);
        TableColumn colTotalizable = vcolTotalizable.getColumn();
        tbl.setColumnData(colTotalizable, new ColumnWeightData(5, 12, true));
        colTotalizable.setAlignment(SWT.LEFT);
        colTotalizable.setText("Totalizable");
        vcolTotalizable.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((TgCounterMapping) element).totalizable ? "Y" : "N";
            }
        });
        vcolTotalizable.setEditingSupport(new CounterMappingTotalizableEditingSupport(tv));

    }

    private TreeModel createMenuModel() {
        root = new TreeModel(null, "ROOT");
        new TreeModel(root, "General");
        measurementsParent = new TreeModel(root, "Measurements");

        for (TgmConfig tgmConfig : tgConfig.measurements) {
            TreeModel m = new TreeModel(measurementsParent, tgmConfig.measurementName);
            m.element = tgmConfig;
        }
        return root;
    }

    /**
     * Create the actions.
     */
    private void createActions() {
        {
            saveAction = new Action("Save", ImageUtil.getImageDescriptor(Images.save)) {
            	public void run() {
            		mainContainer.notifyListeners(SWT.FocusOut, new Event());
            		tblCounterMapping.notifyListeners(SWT.FocusOut, new Event());
                    ExUtil.exec(mainContainer, new Runnable() {
                        public void run() {
                            saveConfig();
                        }
                    });
                }
            };
            helpAction = new Action("Help", ImageUtil.getImageDescriptor(Images.help)) {
                public void run() {
                    org.eclipse.swt.program.Program.launch(HelpConstants.HELP_URL_TG_CONFIG_VIEW);
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
        toolbarManager.add(helpAction);
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

            setPartName("Config Telegraf Server[" + server.getName() + "]");
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
                    String tgConfigContents = mpack.getText("tgConfigContents");

                    if (StringUtil.isEmpty(tgConfigContents)) {
                        return;
                    }
                    try {
                        JAXBContext jaxbContext = JAXBContext.newInstance(TgConfig.class);
                        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

                        tgConfig = (TgConfig) unmarshaller.unmarshal(new StringReader(tgConfigContents));

                    } catch (JAXBException e) {
                        e.printStackTrace();
                    }
                }
                ExUtil.exec(mainContainer, new Runnable() {
                    public void run() {
                        treeViewer.setInput(createMenuModel());
                        treeViewer.refresh();
                        syncGeneralFromModel();
                        treeViewer.setSelection(new StructuredSelection(root.child.get(0)));
                    }
                });
            }
        });
    }

    private void saveConfig() {
        if (MessageDialog.openConfirm(mainContainer.getShell(), "Save Telegraf Config", "Do you want to save telegraf config?")) {
        }

        syncGeneralToModel();
        syncMeasurementToModel();
        TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
        try {
            String contents = null;
            try {
                StringWriter sw = new StringWriter();
                JAXBContext jaxbContext = JAXBContext.newInstance(TgConfig.class);
                Marshaller marshaller = jaxbContext.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                marshaller.marshal(tgConfig, sw);
                contents = sw.toString();
            } catch (JAXBException e) {
                e.printStackTrace();
            }
            MapPack param = new MapPack();
            param.put("tgConfigContents", contents);
            System.out.println(contents);
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

    private void syncGeneralFromModel() {
        btnTgmEnabled.setSelection(tgConfig.enabled);
        btnTgmDebugEnabled.setSelection(tgConfig.debugEnabled);
        btnTgmNormalizeDefault.setSelection(tgConfig.deltaCounterNormalizeDefault);
        txtNormalizeSec.setText(String.valueOf(tgConfig.deltaCounterNormalizeDefaultSeconds));
        txtTgObjDeadTimeMs.setText(String.valueOf(tgConfig.objectDeadtimeMs));
    }

    private void syncGeneralToModel() {
        tgConfig.enabled = btnTgmEnabled.getSelection();
        tgConfig.debugEnabled = btnTgmDebugEnabled.getSelection();
        tgConfig.deltaCounterNormalizeDefault = btnTgmNormalizeDefault.getSelection();
        tgConfig.deltaCounterNormalizeDefaultSeconds = Integer.parseInt(txtNormalizeSec.getText());
        tgConfig.objectDeadtimeMs = Integer.parseInt(txtTgObjDeadTimeMs.getText());
    }

    private void syncMeasurementToModel() {
        tgConfig.measurements = new ArrayList<TgmConfig>();
        for (TreeModel model : measurementsParent.child) {
            TgmConfig mconf = (TgmConfig) model.element;
            tgConfig.measurements.add(mconf);

            List<TagFilterMapping> tagFilterMappings = m_tagFilterMappingMap.get(mconf.measurementName);
            if (tagFilterMappings != null) {
                mconf.tagFilters = TagFilterMapping.toOriginal(tagFilterMappings);
            }
            List<SingleString> familyAppendTags = m_familyAppendTagsMap.get(mconf.measurementName);
            if (familyAppendTags != null) {
                mconf.objFamilyAppendTags = SingleString.toOriginal(familyAppendTags);
            }
            List<SingleString> objTypePrependTags = m_objTypePrependTagsMap.get(mconf.measurementName);
            if (objTypePrependTags != null) {
                mconf.objTypePrependTags = SingleString.toOriginal(objTypePrependTags);
            }
            List<SingleString> objTypeAppendTags = m_objTypeAppendTagsMap.get(mconf.measurementName);
            if (objTypeAppendTags != null) {
                mconf.objTypeAppendTags = SingleString.toOriginal(objTypeAppendTags);
            }
            List<SingleString> objNameAppendTags = m_objNameAppendTagsMap.get(mconf.measurementName);
            if (objNameAppendTags != null) {
                mconf.objNameAppendTags = SingleString.toOriginal(objNameAppendTags);
            }
        }
    }
}
