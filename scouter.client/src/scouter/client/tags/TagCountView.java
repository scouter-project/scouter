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
 */
package scouter.client.tags;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.csstudio.swt.xygraph.dataprovider.CircularBufferDataProvider;
import org.csstudio.swt.xygraph.dataprovider.Sample;
import org.csstudio.swt.xygraph.figures.Trace;
import org.csstudio.swt.xygraph.figures.Trace.PointStyle;
import org.csstudio.swt.xygraph.figures.Trace.TraceType;
import org.csstudio.swt.xygraph.figures.XYGraph;
import org.csstudio.swt.xygraph.linearscale.Range;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.model.CounterColorManager;
import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.client.popup.CalendarDialog;
import scouter.client.popup.CalendarDialog.ILoadCounterDialog;
import scouter.client.util.ChartUtil;
import scouter.client.util.ColorUtil;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.client.util.ScouterUtil;
import scouter.io.DataInputX;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.pack.XLogPack;
import scouter.lang.value.DecimalValue;
import scouter.lang.value.NullValue;
import scouter.lang.value.Value;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;
import scouter.util.LinkedMap;
import scouter.util.LinkedMap.ENTRY;
import scouter.util.StringUtil;

public class TagCountView extends ViewPart {
	
	public static final String ID = TagCountView.class.getName();
	
	private static final String DEFAULT_TAG_GROUP = "service";
	private static final String TOTAL_NAME = "@total";
	
	int serverId;
	
	Composite parent;
	
	CCombo tagGroupCombo;
	Text dateText;
	
	SashForm graphSash;
	
	FigureCanvas totalCanvas;
	XYGraph totalGraph;
	Trace totalTrace;
	
	Label dataRangeLbl;
	Button leftBtn;
	Button rightBtn;
	
	FigureCanvas cntCanvas;
	XYGraph cntGraph;
	HashMap<String, Trace> cntTraceMap = new HashMap<String, Trace>();
	LinkedMap<String, int[]> valueMap = new LinkedMap<String, int[]>();
	
	private String objType;
	private String date;
	
	HashMap<String, TagCount> nameTree = new HashMap<String, TagCount>();
	
	Tree tagNameTree;
	CheckboxTreeViewer treeViewer;
	Table tagValueTable;
	
	SashForm dataTableSash;
	ServiceTableComposite serviceTable;
	
	int lastWidth = 1;
	
	double rangeX1;
	double rangeX2;
	boolean zoomMode = false;
	

	@Override
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		serverId = CastUtil.cint(secId);
	}

	@Override
	public void createPartControl(Composite parent) {
		this.parent = parent; 
		parent.setLayout(new GridLayout(1, true));
		
		Composite menuComp = new Composite(parent, SWT.NONE);
		menuComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		menuComp.setLayout(new GridLayout(4, false));
		tagGroupCombo = new CCombo(menuComp, SWT.READ_ONLY | SWT.BORDER);
		GridData gd = new GridData(SWT.RIGHT, SWT.CENTER, true, false);
		gd.widthHint = 120;
		tagGroupCombo.setLayoutData(gd);
		tagGroupCombo.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		tagGroupCombo.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				loadTagNames(tagGroupCombo.getText());
				loadTotalCount(tagGroupCombo.getText());
				openDataTable();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		Composite dateComp = new Composite(menuComp, SWT.NONE);
		dateComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		dateComp.setLayout(new RowLayout());
		dateText = new Text(dateComp, SWT.SINGLE | SWT.BORDER);
		dateText.setLayoutData(new RowData(160, SWT.DEFAULT));
		Button dayBtn = new Button(menuComp, SWT.PUSH);
		gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gd.widthHint = 70;
		dayBtn.setLayoutData(gd);
		dayBtn.setText("&24H");
		dayBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				rangeX1 = DateUtil.yyyymmdd(date);
				rangeX2 = rangeX1 + DateUtil.MILLIS_PER_DAY - 1;
				totalGraph.primaryXAxis.setRange(rangeX1, rangeX2);
				cntGraph.primaryXAxis.setRange(rangeX1, rangeX2);
				totalCanvas.notifyListeners(SWT.Resize, new Event());
				cntCanvas.notifyListeners(SWT.Resize, new Event());
				adjustYAxisRange(totalGraph, (CircularBufferDataProvider) totalTrace.getDataProvider());
				adjustYAxisRange(cntGraph, cntTraceMap.values());
				updateTextDate();
			}
		});
		Button resetBtn = new Button(menuComp, SWT.PUSH);
		gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gd.widthHint = 70;
		resetBtn.setLayoutData(gd);
		resetBtn.setText("&Reset");
		resetBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				removeTagCountAll();
				setInput(date, objType);
			}
		});
		
		
		SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		graphSash = new SashForm(sashForm, SWT.HORIZONTAL);
		Composite totalComp = new Composite(graphSash, SWT.BORDER);
		totalComp.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		totalComp.setBackgroundMode(SWT.INHERIT_FORCE);
		totalComp.setLayout(new FillLayout());
		totalCanvas = new FigureCanvas(totalComp);
		totalCanvas.setScrollBarVisibility(FigureCanvas.NEVER);
		totalCanvas.addControlListener(new ControlListener() {
			public void controlMoved(ControlEvent arg0) {
			}
			
			public void controlResized(ControlEvent arg0) {
				Rectangle r = totalCanvas.getClientArea();
				totalGraph.setSize(r.width, r.height);
				if (zoomMode) {
					double gap = rangeX2 - rangeX1;
					double noOfMin =gap / DateUtil.MILLIS_PER_MINUTE;
					double lineWidth = (r.width - 50) / noOfMin;
					lastWidth = lineWidth < 1 ? 1 : (int)lineWidth;
					totalTrace.setLineWidth(lastWidth);
				}
				
			}
		});
		totalCanvas.addMouseListener(new MouseListener() {
			
			public void mouseUp(MouseEvent e) {
			}
			
			public void mouseDown(MouseEvent e) {
			}
			
			public void mouseDoubleClick(MouseEvent e) {
				if (rangeX2 - rangeX1 < DateUtil.MILLIS_PER_HOUR) {
					Image image = new Image(e.display, 1, 1);
					GC gc = new GC((FigureCanvas) e.widget);
					gc.copyArea(image, e.x, e.y);
					ImageData imageData = image.getImageData();
					PaletteData palette = imageData.palette;
					int pixelValue = imageData.getPixel(0, 0);
					RGB rgb = palette.getRGB(pixelValue);
					if (ColorUtil.getInstance().getColor(SWT.COLOR_DARK_BLUE).getRGB().equals(rgb)) {
						long stime = (long) totalGraph.primaryXAxis.getPositionValue(e.x, true);
						stime = stime / DateUtil.MILLIS_PER_MINUTE * DateUtil.MILLIS_PER_MINUTE;
						long etime = stime + DateUtil.MILLIS_PER_MINUTE - 1;
						loadData(tagGroupCombo.getText(), stime, etime, false);
					}
				}
			}
		});
		
		totalGraph = new XYGraph();
		totalGraph.setShowLegend(true);
		totalGraph.setShowTitle(false);
		totalCanvas.setContents(totalGraph);

		totalGraph.primaryXAxis.setDateEnabled(true);
		totalGraph.primaryXAxis.setShowMajorGrid(true);
		totalGraph.primaryYAxis.setAutoScale(true);
		totalGraph.primaryYAxis.setShowMajorGrid(true);
		totalGraph.primaryXAxis.setFormatPattern("HH:mm");
		totalGraph.primaryYAxis.setFormatPattern("#,##0");
		
		totalGraph.primaryXAxis.setTitle("");
		totalGraph.primaryYAxis.setTitle("");
		
		CircularBufferDataProvider totalProvider = new CircularBufferDataProvider(true);
		totalProvider.setBufferSize(1440);
		totalProvider.setCurrentXDataArray(new double[] {});
		totalProvider.setCurrentYDataArray(new double[] {});
		totalTrace = new Trace("Total", totalGraph.primaryXAxis, totalGraph.primaryYAxis, totalProvider);
		totalTrace.setPointStyle(PointStyle.NONE);
		totalTrace.setTraceType(TraceType.BAR);
		totalTrace.setAreaAlpha(255);
		totalTrace.setLineWidth(lastWidth);
		totalTrace.setTraceColor(ColorUtil.getInstance().getColor(SWT.COLOR_DARK_BLUE));
		totalGraph.addTrace(totalTrace);
		
		ScouterUtil.addHorizontalRangeListener(totalGraph.getPlotArea(), new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				Object o = evt.getNewValue();
				if (o != null && o instanceof Range) {
					rangeX1 = ((Range) o).getLower();
					rangeX2 = ((Range) o).getUpper();
					if (rangeX1 > rangeX2) {
						double temp = rangeX2;
						rangeX2 = rangeX1;
						rangeX1 = temp;
					}
					if (rangeX2 - rangeX1 < DateUtil.MILLIS_PER_MINUTE) {
						rangeX2 = rangeX1 + DateUtil.MILLIS_PER_MINUTE;
						totalGraph.primaryXAxis.setRange(rangeX1, rangeX2);
					}
					zoomMode = true;
					totalCanvas.notifyListeners(SWT.Resize, new Event());
					adjustYAxisRange(totalGraph, (CircularBufferDataProvider) totalTrace.getDataProvider());
					updateTextDate();
				}
			}
		}, true);
		
		Composite cntGraphComp = new Composite(graphSash, SWT.BORDER);
		cntGraphComp.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		cntGraphComp.setBackgroundMode(SWT.INHERIT_FORCE);
		cntGraphComp.setLayout(new FillLayout());
		cntCanvas = new FigureCanvas(cntGraphComp);
		cntCanvas.setScrollBarVisibility(FigureCanvas.NEVER);
		cntCanvas.addControlListener(new ControlListener() {
			public void controlMoved(ControlEvent arg0) {
			}
			
			public void controlResized(ControlEvent arg0) {
				Rectangle r = cntCanvas.getClientArea();
				cntGraph.setSize(r.width, r.height);
				if (zoomMode) {
					double gap = rangeX2 - rangeX1;
					double noOfMin =gap / DateUtil.MILLIS_PER_MINUTE;
					double lineWidth = (r.width - 50) / noOfMin;
					lastWidth = lineWidth < 1 ? 1 : (int)lineWidth;
					for (Trace t : cntTraceMap.values()) {
						t.setLineWidth(lastWidth);
					}
				}
				
			}
		});
		
		cntGraph = new XYGraph();
		cntGraph.setShowLegend(true);
		cntGraph.setShowTitle(false);
		cntCanvas.setContents(cntGraph);

		cntGraph.primaryXAxis.setDateEnabled(true);
		cntGraph.primaryXAxis.setShowMajorGrid(true);
		cntGraph.primaryYAxis.setAutoScale(true);
		cntGraph.primaryYAxis.setShowMajorGrid(true);
		cntGraph.primaryXAxis.setFormatPattern("HH:mm");
		cntGraph.primaryYAxis.setFormatPattern("#,##0");
		
		cntGraph.primaryXAxis.setTitle("");
		cntGraph.primaryYAxis.setTitle("");
		
		ScouterUtil.addHorizontalRangeListener(cntGraph.getPlotArea(), new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				Object o = evt.getNewValue();
				if (o != null && o instanceof Range) {
					rangeX1 = ((Range) o).getLower();
					rangeX2 = ((Range) o).getUpper();
					if (rangeX1 > rangeX2) {
						double temp = rangeX2;
						rangeX2 = rangeX1;
						rangeX1 = temp;
					}
					if (rangeX2 - rangeX1 < DateUtil.MILLIS_PER_MINUTE) {
						rangeX2 = rangeX1 + DateUtil.MILLIS_PER_MINUTE;
						cntGraph.primaryXAxis.setRange(rangeX1, rangeX2);
					}
					zoomMode = true;
					cntCanvas.notifyListeners(SWT.Resize, new Event());
					adjustYAxisRange(cntGraph, cntTraceMap.values());
					updateTextDate();
				}
			}
		}, true);
		
		graphSash.setWeights(new int[] {1, 1});
		graphSash.setMaximizedControl(totalComp);
		
		SashForm tableSash = new SashForm(sashForm, SWT.HORIZONTAL);
//		Composite tableComp = new Composite(sashForm, SWT.NONE);
//		tableComp.setLayout(new GridLayout(2, false));
		
		Composite treeComp = new Composite(tableSash, SWT.NONE);
//		GridData gr = new GridData(SWT.LEFT, SWT.FILL, false, true);
//		gr.widthHint = 250;
//		treeComp.setLayoutData(gr);
		
		treeViewer = new CheckboxTreeViewer(treeComp, SWT.BORDER | SWT.VIRTUAL | SWT.V_SCROLL | SWT.H_SCROLL);
		tagNameTree = treeViewer.getTree();
		tagNameTree.setHeaderVisible(true);
		tagNameTree.setLinesVisible(true);
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection sel = (IStructuredSelection) event.getSelection();
				Object o = sel.getFirstElement();
				if (o instanceof TagCount) {
					TagCount tc = (TagCount) o;
					if (tc.tagName == null && tc.count == 0) {
						loadTagValues(tagGroupCombo.getText(), tc.name);
					} else {
						TreeItem[] items = tagNameTree.getSelection();
						if (items != null && items.length >0) {
							TreeItem item = items[0];
							if (item != null) {
								if(item.getExpanded()){
									item.setExpanded(false);
								}else{
									item.setExpanded(true);
								}
							}							
						}
					}
				}
			}
		});
		treeViewer.setContentProvider(new ViewContentProvider());
		treeViewer.setLabelProvider(new TableLabelProvider());
		treeViewer.setCheckStateProvider(new TreeCheckStateProvider());
		treeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				if (event.getElement() instanceof TagCount) {
					TagCount tc = (TagCount) event.getElement();
					if (StringUtil.isNotEmpty(tc.tagName)) {
						if (event.getChecked()) {
							loadTagCount(tagGroupCombo.getText(), tc.tagName, tc.name);
						} else {
							removeTagCount(tc.name);
						}
					}
				}
			}
		});
		
		
		TreeColumnLayout treeColumnLayout = new TreeColumnLayout();
		treeComp.setLayout(treeColumnLayout);
		
		TreeColumn nameColumn = new TreeColumn(tagNameTree, SWT.LEFT);
		nameColumn.setText("Name/Value");
		treeColumnLayout.setColumnData(nameColumn, new ColumnWeightData(70));
		TreeColumn cntColumn = new TreeColumn(tagNameTree, SWT.RIGHT);
		cntColumn.setText("Count");
		treeColumnLayout.setColumnData(cntColumn, new ColumnWeightData(30));
		
		treeViewer.setInput(nameTree);
		
		Composite rightTablecomp = new Composite(tableSash, SWT.NONE);
		rightTablecomp.setLayout(new GridLayout(1,true));
		Composite tableInfoComp = new Composite(rightTablecomp, SWT.NONE);
		tableInfoComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		tableInfoComp.setLayout(new GridLayout(3, false));

		dataRangeLbl = new Label(tableInfoComp, SWT.NONE);
		gd = new GridData(SWT.RIGHT, SWT.CENTER, true, false);
		dataRangeLbl.setLayoutData(gd);
		
		leftBtn = new Button(tableInfoComp, SWT.PUSH);
		gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gd.widthHint = 40;
		leftBtn.setLayoutData(gd);
		leftBtn.setText("<");
		
		rightBtn = new Button(tableInfoComp, SWT.PUSH);
		gd = new GridData(SWT.FILL, SWT.CENTER, false, false);
		gd.widthHint = 40;
		rightBtn.setLayoutData(gd);
		rightBtn.setText(">");
		
		dataTableSash = new SashForm(rightTablecomp, SWT.HORIZONTAL);
		dataTableSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		serviceTable = new ServiceTableComposite(dataTableSash, SWT.NONE);
		
		tableSash.setWeights(new int[] {1, 5});
		tableSash.setMaximizedControl(null);
		
		sashForm.setWeights(new int[] {1, 2});
		sashForm.setMaximizedControl(null);
		
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("Change Date", ImageUtil.getImageDescriptor(Images.calendar)) {
			public void run() {
				CalendarDialog dialog = new CalendarDialog(getViewSite().getShell().getDisplay(), new ILoadCounterDialog(){
					public void onPressedOk(long startTime, long endTime) {}
					public void onPressedCancel() {}
					public void onPressedOk(String date) {
						setInput(date, objType);
					}
				});
				dialog.show(-1, -1, DateUtil.yyyymmdd(date));
			}
		});
	}

	@Override
	public void setFocus() {
		ScouterUtil.detachView(this);
	}

	public void setInput(String date, String objType) {
		this.date = date;
		this.objType = objType;
		setTitleImage(Images.getObjectIcon(objType, true, serverId));
		openTotalGraph();
		rangeX1 = DateUtil.yyyymmdd(date);
		rangeX2 = rangeX1 + DateUtil.MILLIS_PER_DAY - 1;
		updateTextDate();
		loadTagGroup();
		totalCanvas.notifyListeners(SWT.Resize, new Event());
		zoomMode = false;
	}
	
	private void updateTextDate() {
		StringBuffer sb = new StringBuffer();
		sb.append(date.substring(0, 4));
		sb.append("-");
		sb.append(date.substring(4, 6));
		sb.append("-");
		sb.append(date.substring(6, 8));
		sb.append("   ");
		sb.append(DateUtil.format((long)rangeX1, "HH:mm"));
		sb.append("~");
		sb.append(DateUtil.format((long)rangeX2, "HH:mm"));
		dateText.setText(sb.toString());
	}
	
	private void adjustYAxisRange(XYGraph graph, CircularBufferDataProvider provider) {
		double max = 0.0;
		for (int i = 0; i < provider.getSize(); i++) {
			Sample sample = (Sample) provider.getSample(i);
			double x = sample.getXValue();
			if(x < rangeX1 || x > rangeX2) {
				continue;
			}
			double y = sample.getYValue();
			if (y > max) {
				max = y;
			}
		}
		graph.primaryYAxis.setRange(0, ChartUtil.getMaxValue(max));
	}
	
	private void adjustYAxisRange(XYGraph graph, Collection<Trace> traceList) {
		double max = 0.0;
		for (Trace t : traceList) {
			CircularBufferDataProvider provider = (CircularBufferDataProvider) t.getDataProvider();
			for (int i = 0; i < provider.getSize(); i++) {
				Sample sample = (Sample) provider.getSample(i);
				double x = sample.getXValue();
				if(x < rangeX1 || x > rangeX2) {
					continue;
				}
				double y = sample.getYValue();
				if (y > max) {
					max = y;
			}
			}
		}
		graph.primaryYAxis.setRange(0, ChartUtil.getMaxValue(max));
	}
	
	private void openTotalGraph() {
		if (graphSash.getMaximizedControl() != totalCanvas.getParent()) {
			graphSash.setMaximizedControl(totalCanvas.getParent());
		}
	}
	
	private void openCountGraph() {
		if (graphSash.getMaximizedControl() != cntCanvas.getParent()) {
			graphSash.setMaximizedControl(cntCanvas.getParent());
		}
	}
	
	private void openDataTable() {
		String tagGroup = tagGroupCombo.getText();
		if ("service".equals(tagGroup)) {
			dataTableSash.setMaximizedControl(serviceTable);
		} else if ("alert".equals(tagGroup)) {
			
		}
	}
	

	// This method must be called UI thread.
	private void drawStackCountGraph() {
		for (Trace t : cntTraceMap.values()) {
			cntGraph.removeTrace(t);
		}
		cntTraceMap.clear();
		int[] stackedValue = new int[1440];
		LinkedMap<String, int[]> tempMap = new LinkedMap<String, int[]>();
		Enumeration<ENTRY> entries = valueMap.entries();
		while (entries.hasMoreElements()) {
			ENTRY entry = entries.nextElement();
			String key = (String) entry.getKey();
			int[] values = (int[]) entry.getValue();
			for (int i = 0; i < values.length; i++) {
				stackedValue[i] += values[i];
			}
			int[] copiedArray = new int[stackedValue.length];
			System.arraycopy(stackedValue, 0, copiedArray, 0, stackedValue.length);
			tempMap.putFirst(key, copiedArray);
		}
		long stime = DateUtil.yyyymmdd(date);
		Enumeration<ENTRY> entries2 = tempMap.entries();
		while (entries2.hasMoreElements()) {
			ENTRY entry = entries2.nextElement();
			String key = (String) entry.getKey();
			int[] values = (int[]) entry.getValue();
			Trace trace = getCountTrace(key);
			CircularBufferDataProvider provider = (CircularBufferDataProvider)trace.getDataProvider();
			provider.clearTrace();
			for (int i = 0; i < values.length; i++) {
				double x = stime + (DateUtil.MILLIS_PER_MINUTE * i + DateUtil.MILLIS_PER_SECOND * 30);
				provider.addSample(new Sample(x, values[i]));
			}
		}
		
	}
	
	private void loadTagGroup() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				
				List<Value> list = null;
				try {
					MapPack param = new MapPack();
					param.put("objType", objType);
					list = tcp.processValues(RequestCmd.TAGCNT_DIV_NAMES, param);
				} catch (Exception e) {
					
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				
				if (list != null) {
					final List<String> resultList = new ArrayList<String>();
					for (Value v : list) {
						resultList.add(v.toString());
					}
					ExUtil.exec(tagGroupCombo, new Runnable() {
						public void run() {
							tagGroupCombo.removeAll();
							int defaultIndex = -1;
							for (int i = 0; i < resultList.size(); i++) {
								String s = resultList.get(i);
								tagGroupCombo.add(s);
								if (DEFAULT_TAG_GROUP.equals(s)) {
									defaultIndex = i;
								}
							}
							if (defaultIndex > -1) {
								tagGroupCombo.select(defaultIndex);
								tagGroupCombo.notifyListeners(SWT.Selection, new Event());
							}
						}
					});
				}
				
			}
			
		});
	}
	
	private void loadTagNames(final String tagGroup) {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				List<Value> names = null;
				try {
					MapPack param = new MapPack();
					param.put("objType", objType);
					param.put("tagGroup", tagGroup);
					names = tcp.processValues(RequestCmd.TAGCNT_TAG_NAMES, param);
				} catch (Exception e) {
					
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				if (names != null) {
					final List<String> list = new ArrayList<String>();
					for (Value v : names) {
						if (TOTAL_NAME.equals(v.toString())) continue;
						list.add(v.toString());
					}
					ExUtil.exec(tagNameTree, new Runnable() {
						public void run() {
							tagNameTree.removeAll();
							nameTree.clear();
							for (int i = 0; i < list.size(); i++) {
								TagCount tag = new TagCount();
								tag.name = list.get(i);
								nameTree.put(list.get(i), tag);
							}
							treeViewer.refresh();
						}
					});
				}
			}
		});
	}
	
	
	private void loadTagValues(final String tagGroup, final String tagName) {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				final DecimalValue valueSize = new DecimalValue();
				final DecimalValue totalCount = new DecimalValue();
				final List<Value> valueList = new ArrayList<Value>();
				final List<Long> cntList = new ArrayList<Long>();
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				try {
					MapPack param = new MapPack();
					param.put("objType", objType);
					param.put("tagGroup", tagGroup);
					param.put("tagName", tagName);
					param.put("date", date);
					
					tcp.process(RequestCmd.TAGCNT_TAG_VALUES, param, new INetReader() {
						public void process(DataInputX in) throws IOException {
							valueSize.value = in.readInt();
							totalCount.value = in.readInt();
							int size = in.readInt();
							for (int i = 0; i < size; i++) {
								Value v = in.readValue();
								long cnt = in.readLong();
								valueList.add(v);
								cntList.add(cnt);
							}
						}
					});
					
				} catch (Exception e) {
					
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				
				final List<String> nameList = TagCountUtil.loadTagString(serverId, date, valueList, tagName);
				
				ExUtil.exec(tagNameTree, new Runnable() {
					public void run() {
						removeTagCountAll();
						TagCount parentTag = nameTree.get(tagName);
						if (parentTag == null) return;
						for (int i = 0; i < nameList.size(); i++) {
							TagCount child = new TagCount();
							child.tagName = parentTag.name;
							child.name = nameList.get(i);
							child.count = cntList.get(i);
							parentTag.addChild(child);
						}
						parentTag.count = totalCount.value;
						parentTag.name += " (" + valueSize.value + ")";
						treeViewer.refresh();
						treeViewer.setExpandedElements(new TagCount[] {parentTag});
					}
				});
			}
		});
		
	}
	
	private void removeTagCountAll() {
		for (Trace t : cntTraceMap.values()) {
			cntGraph.removeTrace(t);
		}
		cntTraceMap.clear();
		valueMap.clear();
		cntGraph.repaint();
	}
	
	private void removeTagCount(String tagValue) {
		Trace t = cntTraceMap.get(tagValue);
		if (t == null) return;
		cntGraph.removeTrace(t);
		cntGraph.repaint();
		cntTraceMap.remove(tagValue);
		valueMap.remove(tagValue);
		adjustYAxisRange(cntGraph, cntTraceMap.values());
	}
	
	private void loadTagCount(final String tagGroup, final String tagName, final String tagValue) {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				final int[] valueArray = new int[1440];
				try {
					MapPack param = new MapPack();
					param.put("objType", objType);
					param.put("tagGroup", tagGroup);
					param.put("tagName", tagName);
					param.put("tagValue", TagCountUtil.convertTagToValue(tagName, tagValue));
					param.put("date", date);
					tcp.process(RequestCmd.TAGCNT_TAG_VALUE_DATA, param, new INetReader() {
						public void process(DataInputX in) throws IOException {
							int[] values = in.readArray(new int[0]);
							for (int i = 0; i < values.length; i++) {
								valueArray[i] = values[i];
							}
						}
					});
				} catch (Exception e) {
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				
				valueMap.put(tagValue, valueArray);
				ExUtil.exec(cntCanvas, new Runnable() {
					public void run() {
						cntGraph.primaryXAxis.setRange(rangeX1, rangeX2);
						drawStackCountGraph();
						adjustYAxisRange(cntGraph, cntTraceMap.values());
						cntGraph.repaint();
						openCountGraph();
					}
				});
			}
		});
	}
	
	private void loadTotalCount(final String tagGroup) {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				final List<Integer> valueList = new ArrayList<Integer>();
				try {
					MapPack param = new MapPack();
					param.put("tagGroup", tagGroup);
					param.put("objType", objType);
					param.put("tagName", TOTAL_NAME);
					param.put("tagValue", NullValue.value);
					param.put("date", date);
					tcp.process(RequestCmd.TAGCNT_TAG_VALUE_DATA, param, new INetReader() {
						public void process(DataInputX in) throws IOException {
							int[] values = in.readArray(new int[0]);
							for (int i = 0; i < values.length; i++) {
								valueList.add(values[i]);
							}
						}
					});
				} catch (Exception e) {
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				ExUtil.exec(totalCanvas, new Runnable() {
					public void run() {
						long stime = DateUtil.yyyymmdd(date);
						totalGraph.primaryXAxis.setRange(rangeX1, rangeX2);
						CircularBufferDataProvider provider = (CircularBufferDataProvider)totalTrace.getDataProvider();
						provider.clearTrace();
						for (int i = 0; i < valueList.size(); i++) {
							double x = stime + (DateUtil.MILLIS_PER_MINUTE * i + DateUtil.MILLIS_PER_SECOND * 30);
							int value = valueList.get(i);
							provider.addSample(new Sample(x, value));
						}
						adjustYAxisRange(totalGraph, provider);
						totalGraph.repaint();
						openTotalGraph();
					}
				});
			}
		});
	}
	
	private Trace getCountTrace(String tagValue) {
		if (cntTraceMap.containsKey(tagValue)) {
			return cntTraceMap.get(tagValue);
		}
		CircularBufferDataProvider provider = new CircularBufferDataProvider(true);
		provider.setBufferSize(1440);
		provider.setCurrentXDataArray(new double[] {});
		provider.setCurrentYDataArray(new double[] {});
		Trace trace = new Trace(tagValue, cntGraph.primaryXAxis, cntGraph.primaryYAxis, provider);
		trace.setPointStyle(PointStyle.NONE);
		trace.setTraceType(TraceType.BAR);
		trace.setAreaAlpha(255);
		trace.setLineWidth(lastWidth);
		trace.setTraceColor(CounterColorManager.getInstance().assignColor(tagValue));
		cntGraph.addTrace(trace);
		cntTraceMap.put(tagValue, trace);
		return trace;
	}
	
	int lastCnt;
	long lastSTime;
	long lastETime;
	
	private void loadData(final String tagGroup, final long stime, final long etime, final boolean more) {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				List<Pack> list = null;
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				try {
					MapPack param = new MapPack();
					param.put("objType", objType);
					param.put("stime", stime);
					param.put("etime", etime);
					param.put("tagGroup", tagGroup);
					param.put("date", date);
					param.put("max", 100);
					list = tcp.process(RequestCmd.TAGCNT_TAG_ACTUAL_DATA, param);
				} catch (Exception e) {
					
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				final ArrayList<Pack> result = new ArrayList<Pack>(list);
				final int size = result.size();
				ExUtil.exec(dataTableSash, new Runnable() {
					public void run() {
						dataRangeLbl.setText((lastCnt+1) + " ~ " + (lastCnt + size));
						if ("service".equals(tagGroup)) {
							if (result.size() > 0) {
								Pack p = result.get(0);
								XLogPack xp = (XLogPack) p;
								lastSTime = xp.endTime;
								p = result.get(result.size()-1);
								xp = (XLogPack) p;
								lastETime = xp.endTime;
								lastCnt += result.size();
							}
							serviceTable.setInput(result, serverId, tagGroup);
						}
					}
				});
			}
		});
	}
	
	class TableLabelProvider implements ITableLabelProvider {
		
		public Image getColumnImage(Object obj, int columnIndex) {
			return null;
		}

		public String getColumnText(Object obj, int columnIndex) {
			switch (columnIndex) {
			case 0:
				if (obj instanceof TagCount) {
					TagCount a = (TagCount) obj;
					return a.name;
				}
			case 1:
				if (obj instanceof TagCount) {
					TagCount a = (TagCount) obj;
					return FormatUtil.print(a.count, "#,##0");
				}
			}
			return null;
		}

		public void addListener(ILabelProviderListener listener) {
		}

		public void dispose() {
		}

		public boolean isLabelProperty(Object element, String property) {
			return false;
		}
		public void removeListener(ILabelProviderListener listener) {
		}

	}
	
	class ViewContentProvider implements ITreeContentProvider {
		
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}

		public void dispose() {
		}

		@SuppressWarnings("rawtypes")
		public Object[] getElements(Object parent) {
			if (parent instanceof Map) {
				return ((Map) parent).values().toArray();
			}
			return new Object[0];
		}

		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof TagCount){
				return ((TagCount) parentElement).getChildArray();
			}
			return new Object[0];
		}

		public Object getParent(Object element) {
			if (element instanceof TagCount) {
				return nameTree.get(((TagCount) element).tagName);
			}
			return null;
		}

		public boolean hasChildren(Object element) {
			if (element instanceof TagCount){
				return ((TagCount) element).getChildSize() > 0;
			}
			return false;
		}

		public boolean equals(Object obj) {
			return true;
		}
		
	}
	
	class TreeCheckStateProvider implements ICheckStateProvider {
		public boolean isChecked(Object element) {
			return false;
		}

		public boolean isGrayed(Object element) {
			if (element instanceof TagCount) {
				return ((TagCount) element).tagName == null;
			}
			return true;
		}
	}
}

