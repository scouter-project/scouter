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
 */
package scouter.client.tags;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.csstudio.swt.xygraph.dataprovider.CircularBufferDataProvider;
import org.csstudio.swt.xygraph.dataprovider.Sample;
import org.csstudio.swt.xygraph.figures.Trace;
import org.csstudio.swt.xygraph.figures.Trace.PointStyle;
import org.csstudio.swt.xygraph.figures.Trace.TraceType;
import org.csstudio.swt.xygraph.figures.XYGraph;
import org.csstudio.swt.xygraph.linearscale.Range;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.model.CounterColorManager;
import scouter.client.net.INetReader;
import scouter.client.net.TcpProxy;
import scouter.client.popup.CalendarDialog;
import scouter.client.popup.CalendarDialog.ILoadCalendarDialog;
import scouter.client.util.ChartUtil;
import scouter.client.util.ColorUtil;
import scouter.client.util.ExUtil;
import scouter.client.util.ScouterUtil;
import scouter.io.DataInputX;
import scouter.lang.constants.TagConstants;
import scouter.lang.pack.AlertPack;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.lang.pack.XLogPack;
import scouter.lang.value.BooleanValue;
import scouter.lang.value.ListValue;
import scouter.lang.value.MapValue;
import scouter.lang.value.NullValue;
import scouter.lang.value.Value;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;
import scouter.util.LinkedMap;
import scouter.util.LinkedMap.LinkedEntry;
import scouter.util.StringUtil;

public class TagCountView extends ViewPart {
	
	public static final String ID = TagCountView.class.getName();
	
	private static final String DEFAULT_TAG_GROUP = TagConstants.GROUP_SERVICE;
	private static final int LIMIT_PER_PAGE = 100;
	
	int serverId;
	
	Composite parent;
	
	Combo tagGroupCombo;
	Label dateLbl;
	
	SashForm graphSash;
	
	FigureCanvas totalCanvas;
	XYGraph totalGraph;
	Trace totalTrace;
	
	Label rangeLbl;
	Label dataRangeLbl;
	Button leftBtn;
	Button rightBtn;
	
	FigureCanvas cntCanvas;
	XYGraph cntGraph;
	HashMap<String, Trace> cntTraceMap = new HashMap<String, Trace>();
	LinkedMap<String, float[]> valueMap = new LinkedMap<String, float[]>();
	
	private String objType;
	private String date;
	
	TreeMap<String, TagCount> nameTree = new TreeMap<String, TagCount>();
	
	Tree tagNameTree;
	CheckboxTreeViewer treeViewer;
	Table tagValueTable;
	
	SashForm dataTableSash;
	ServiceTableComposite serviceTable;
	AlertTableComposite alertTable;
	
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
		
		Composite menuComp = new Composite(parent, SWT.BORDER);
		menuComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		menuComp.setLayout(new GridLayout(4, false));
		
		tagGroupCombo = new Combo(menuComp, SWT.READ_ONLY | SWT.BORDER);
		GridData gd = new GridData(SWT.RIGHT, SWT.FILL, true, true);
		gd.widthHint = 100;
		tagGroupCombo.setLayoutData(gd);
		tagGroupCombo.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		tagGroupCombo.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				removeTagCountAll();
				loadTagNames(tagGroupCombo.getText());
				loadTotalCount(tagGroupCombo.getText());
				openDataTable();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		Composite dateComp = new Composite(menuComp, SWT.BORDER);
		dateComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		dateComp.setLayout(new RowLayout());
		dateComp.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		dateComp.setBackgroundMode(SWT.INHERIT_FORCE);
		dateLbl = new Label(dateComp, SWT.CENTER);
		dateLbl.setLayoutData(new RowData(160, SWT.DEFAULT));
		dateLbl.addMouseListener(new MouseAdapter() {
			public void mouseDown(MouseEvent e) {
				CalendarDialog dialog = new CalendarDialog(getViewSite().getShell().getDisplay(), new ILoadCalendarDialog(){
					public void onPressedOk(long startTime, long endTime) {}
					public void onPressedCancel() {}
					public void onPressedOk(String date) {
						setInput(date, objType);
					}
				});
				dialog.show(-1, -1, DateUtil.yyyymmdd(date));
			}
			
		});
		Button dayBtn = new Button(menuComp, SWT.PUSH);
		gd = new GridData(SWT.FILL, SWT.FILL, false, true);
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
		gd = new GridData(SWT.FILL, SWT.FILL, false, true);
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
					double lineWidth = (r.width - (gap / DateUtil.MILLIS_PER_MINUTE)) / noOfMin * 0.9d;
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
				long stime = (long) totalGraph.primaryXAxis.getPositionValue(e.x, false);
				if (stime < rangeX1 || stime > rangeX2) return;
				stime = stime / DateUtil.MILLIS_PER_MINUTE * DateUtil.MILLIS_PER_MINUTE;
				long etime = stime + DateUtil.MILLIS_PER_MINUTE - 1;
				loadInitData(tagGroupCombo.getText(), stime, etime, null);
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
					double x1 = ((Range) o).getLower();
					double x2 = ((Range) o).getUpper();
					if (Math.abs(x1-x2) < DateUtil.MILLIS_PER_FIVE_MINUTE){
						return;
					}
					if (x1 < x2) {
						rangeX1 = x1;
						rangeX2 = x2;
					} else {
						rangeX1 = x2;
						rangeX2 = x1;
					}
					totalGraph.primaryXAxis.setRange(rangeX1, rangeX2);
					zoomMode = true;
					totalCanvas.notifyListeners(SWT.Resize, new Event());
					adjustYAxisRange(totalGraph, (CircularBufferDataProvider) totalTrace.getDataProvider());
					updateTextDate();
				}
			}
		}, false);
		
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
					double lineWidth = (r.width - (gap / DateUtil.MILLIS_PER_MINUTE)) / noOfMin * 0.9d;
					lastWidth = lineWidth < 1 ? 1 : (int)lineWidth;
					for (Trace t : cntTraceMap.values()) {
						t.setLineWidth(lastWidth);
					}
				}
				
			}
		});
		
		cntCanvas.addMouseListener(new MouseListener() {
			
			
			public void mouseUp(MouseEvent e) {
			}
			
			public void mouseDown(MouseEvent e) {
			}
			
			public void mouseDoubleClick(MouseEvent e) {
				long stime = (long) cntGraph.primaryXAxis.getPositionValue(e.x, false);
				if (stime < rangeX1 || stime > rangeX2) return;
				stime = stime / DateUtil.MILLIS_PER_MINUTE * DateUtil.MILLIS_PER_MINUTE;
				long etime = stime + DateUtil.MILLIS_PER_MINUTE - 1;
				loadInitData(tagGroupCombo.getText(), stime, etime, makeFilterMv());
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
					double x1 = ((Range) o).getLower();
					double x2 = ((Range) o).getUpper();
					if (Math.abs(x1-x2) < DateUtil.MILLIS_PER_FIVE_MINUTE){
						return;
					}
					if (x1 < x2) {
						rangeX1 = x1;
						rangeX2 = x2;
					} else {
						rangeX1 = x2;
						rangeX2 = x1;
					}
					cntGraph.primaryXAxis.setRange(rangeX1, rangeX2);
					zoomMode = true;
					cntCanvas.notifyListeners(SWT.Resize, new Event());
					adjustYAxisRange(cntGraph, cntTraceMap.values());
					updateTextDate();
				}
			}
		}, false);
		
		graphSash.setWeights(new int[] {1, 1});
		graphSash.setMaximizedControl(totalComp);
		
		//SashForm downSash = new SashForm(sashForm, SWT.HORIZONTAL);
		Composite downSash = new Composite(sashForm, SWT.NONE);
	
		downSash.setLayout(new GridLayout(2, true));
		
		Composite treeComp = new Composite(downSash, SWT.BORDER);
		treeComp.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		//gd.widthHint = 200;
		treeComp.setLayoutData(gd);
		treeComp.setLayout(new GridLayout(1, true));
		Composite innerTreeComp = new Composite(treeComp, SWT.NONE);
		innerTreeComp.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		
		innerTreeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		TreeColumnLayout treeColumnLayout = new TreeColumnLayout();
		innerTreeComp.setLayout(treeColumnLayout);
		treeViewer = new CheckboxTreeViewer(innerTreeComp, SWT.BORDER | SWT.VIRTUAL | SWT.V_SCROLL | SWT.H_SCROLL);
		tagNameTree = treeViewer.getTree();
		tagNameTree.setHeaderVisible(true);
		tagNameTree.setLinesVisible(true);
//		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
//			public void doubleClick(DoubleClickEvent event) {
//				IStructuredSelection sel = (IStructuredSelection) event.getSelection();
//				Object o = sel.getFirstElement();
//				if (o instanceof TagCount) {
//					TreeItem[] items = tagNameTree.getSelection();
//					if (items != null && items.length >0) {
//						TreeItem item = items[0];
//						if (item != null) {
//							if(item.getExpanded()){
//								item.setExpanded(false);
//							}else{
//								item.setExpanded(true);
//							}
//						}							
//					}
//				}
//			}
//		});
		treeViewer.setContentProvider(new ViewContentProvider());
		treeViewer.setLabelProvider(new TableLabelProvider());
		treeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				if (event.getElement() instanceof TagCount) {
					TagCount tc = (TagCount) event.getElement();
					if (StringUtil.isNotEmpty(tc.tagName)) {
						if (event.getChecked()) {
							loadTagCount(tagGroupCombo.getText(), tc.tagName, tc.value);
							treeViewer.setGrayChecked(nameTree.get(tc.tagName), true);
						} else {
							removeTagCount(tc.value);
							Object[] objects = treeViewer.getCheckedElements();
							for (Object o : objects) {
								TagCount checked = (TagCount) o;
								if (tc.tagName.equals(checked.tagName)) {
									return;
								}
							}
							treeViewer.setGrayChecked(nameTree.get(tc.tagName), false);
						}
					}
				}
			}
		});
		
		
		TreeColumn nameColumn = new TreeColumn(tagNameTree, SWT.LEFT);
		nameColumn.setText("Name");
		TreeColumn cntColumn = new TreeColumn(tagNameTree, SWT.LEFT);
		cntColumn.setText("Count");
		
		treeColumnLayout.setColumnData(nameColumn, new ColumnWeightData(68));
		treeColumnLayout.setColumnData(cntColumn, new ColumnWeightData(22));
		
		treeViewer.setInput(nameTree);
		
		Composite rightTablecomp = new Composite(downSash, SWT.BORDER);
		rightTablecomp.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		rightTablecomp.setBackgroundMode(SWT.INHERIT_FORCE);
		rightTablecomp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		rightTablecomp.setLayout(new GridLayout(1,true));
		
		rangeLbl = new Label(rightTablecomp, SWT.CENTER);
		gd = new GridData(SWT.CENTER, SWT.CENTER, true, false);
		rangeLbl.setLayoutData(gd);
		FontData fontData = rangeLbl.getFont().getFontData()[0];
		Font font = new Font(getViewSite().getShell().getDisplay(), new FontData(fontData.getName(), fontData
		    .getHeight(), SWT.BOLD));
		rangeLbl.setFont(font);
		rangeLbl.setAlignment(SWT.CENTER);
		rangeLbl.setText("00:00 ~ 00:00");
		
		dataTableSash = new SashForm(rightTablecomp, SWT.HORIZONTAL);
		dataTableSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		serviceTable = new ServiceTableComposite(dataTableSash, SWT.NONE);
		alertTable = new AlertTableComposite(dataTableSash, SWT.NONE);
		
		Composite tableInfoComp = new Composite(rightTablecomp, SWT.NONE);
		gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		tableInfoComp.setLayoutData(gd);
		tableInfoComp.setLayout(new GridLayout(3, false));

		leftBtn = new Button(tableInfoComp, SWT.PUSH);
		gd = new GridData(SWT.RIGHT, SWT.CENTER, true, false);
		leftBtn.setLayoutData(gd);
		leftBtn.setText("<");
		leftBtn.setEnabled(false);
		leftBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (graphSash.getMaximizedControl() == cntCanvas.getParent()) {
					loadLeftData(tagGroupCombo.getText(), makeFilterMv());
				} else {
					loadLeftData(tagGroupCombo.getText(), null);
				}
			}
		});
		
		dataRangeLbl = new Label(tableInfoComp, SWT.RIGHT);
		gd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		gd.widthHint = 100;
		dataRangeLbl.setLayoutData(gd);
		dataRangeLbl.setAlignment(SWT.CENTER);
		
		
		rightBtn = new Button(tableInfoComp, SWT.PUSH);
		gd = new GridData(SWT.LEFT, SWT.CENTER, true, false);
		rightBtn.setLayoutData(gd);
		rightBtn.setText(">");
		rightBtn.setEnabled(false);
		rightBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (graphSash.getMaximizedControl() == cntCanvas.getParent()) {
					loadRightData(tagGroupCombo.getText(), makeFilterMv());
				} else {
					loadRightData(tagGroupCombo.getText(), null);
				}
			}
		});
		
		//downSash.setWeights(new int[] {1, 5});
		//downSash.setMaximizedControl(null);
		
		sashForm.setWeights(new int[] {1, 2});
		sashForm.setMaximizedControl(null);
	}

	@Override
	public void setFocus() {
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
		serviceTable.setInput(new ArrayList<Pack>(), serverId, date);
		alertTable.setInput(new ArrayList<Pack>(), serverId, date);
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
		dateLbl.setText(sb.toString());
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
		if (TagConstants.GROUP_SERVICE.equals(tagGroup)) {
			dataTableSash.setMaximizedControl(serviceTable);
		} else if (TagConstants.GROUP_ALERT.equals(tagGroup)) {
			dataTableSash.setMaximizedControl(alertTable);
		}
	}
	

	// This method must be called UI thread.
	private void drawStackCountGraph() {
		for (Trace t : cntTraceMap.values()) {
			cntGraph.removeTrace(t);
		}
		cntTraceMap.clear();
		float[] stackedValue = new float[1440];
		LinkedMap<String, float[]> tempMap = new LinkedMap<String, float[]>();
		Enumeration<LinkedEntry<String, float[]>> entries = valueMap.entries();
		while (entries.hasMoreElements()) {
			LinkedEntry<String, float[]> entry = entries.nextElement();
			String key = (String) entry.getKey();
			float[] values = (float[]) entry.getValue();
			for (int i = 0; i < values.length; i++) {
				stackedValue[i] += values[i];
			}
			float[] copiedArray = new float[stackedValue.length];
			System.arraycopy(stackedValue, 0, copiedArray, 0, stackedValue.length);
			tempMap.putFirst(key, copiedArray);
		}
		long stime = DateUtil.yyyymmdd(date);
		Enumeration<LinkedEntry<String, float[]>> entries2 = tempMap.entries();
		while (entries2.hasMoreElements()) {
			LinkedEntry<String, float[]> entry = entries2.nextElement();
			String key = (String) entry.getKey();
			float[] values = (float[]) entry.getValue();
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
		tagNameTree.removeAll();
		nameTree.clear();
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
					e.printStackTrace();
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				if (names != null) {
					final List<String> list = new ArrayList<String>();
					for (Value v : names) {
						if (TagConstants.NAME_TOTAL.equals(v.toString())) continue;
						list.add(v.toString());
					}
					for (int i = 0; i < list.size(); i++) {
						TagCount tag = new TagCount();
						tag.tagName = "";
						tag.value = list.get(i);
						nameTree.put(list.get(i), tag);
					}
					loadTagValues(tagGroup, list);
					ExUtil.exec(tagNameTree, new Runnable() {
						public void run() {
							treeViewer.refresh();
							treeViewer.setGrayedElements(nameTree.values().toArray());
						}
					});
				}
			}
			
			private void loadTagValues(final String tagGroup, final List<String> tagNameList) {
				final List<TagData> dataList = new ArrayList<TagData>();
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				try {
					MapPack param = new MapPack();
					param.put("objType", objType);
					param.put("tagGroup", tagGroup);
					ListValue tagNameLv = param.newList("tagName");
					for (String tagName : tagNameList) {
						tagNameLv.add(tagName);
					}
					param.put("date", date);
					
					tcp.process(RequestCmd.TAGCNT_TAG_VALUES, param, new INetReader() {
						public void process(DataInputX in) throws IOException {
							TagData data = new TagData();
							dataList.add(data);
							data.tagName = in.readText();
							data.totalSize = in.readInt();
							data.totalCnt = in.readFloat();
							int size = in.readInt();
							for (int i = 0; i < size; i++) {
								Value v = in.readValue();
								float cnt = in.readFloat();
								data.addValue(v, cnt);
							}
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				
				for (TagData data : dataList) {
					data.strValueList = TagCountUtil.loadTagString(serverId, date, data.valueList, data.tagName);
					String tagName = data.tagName;
					TagCount parentTag = nameTree.get(tagName);
					if (parentTag == null) return;
					for (int i = 0; i < data.strValueList.size(); i++) {
						TagCount child = new TagCount();
						child.tagName = parentTag.value;
						child.value = data.strValueList.get(i);
						child.count = data.cntList.get(i);
						parentTag.addChild(child);
					}
					parentTag.count = data.totalCnt;
					parentTag.value += " (" + data.totalSize + ")";
				}
			}
		});
	}
	
	static class TagData {
		String tagName;
		List<Value> valueList = new ArrayList<Value>();
		List<Float> cntList = new ArrayList<Float>();
		List<String> strValueList;
		int totalSize;
		float totalCnt;
		
		void addValue(Value v, float cnt) {
			valueList.add(v);
			cntList.add(cnt);
		}
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
				final float[] valueArray = new float[1440];
				try {
					MapPack param = new MapPack();
					param.put("objType", objType);
					param.put("tagGroup", tagGroup);
					param.put("tagName", tagName);
					param.put("tagValue", TagCountUtil.convertTagToValue(tagName, tagValue));
					param.put("date", date);
					tcp.process(RequestCmd.TAGCNT_TAG_VALUE_DATA, param, new INetReader() {
						public void process(DataInputX in) throws IOException {
							float[] values = in.readArray(new float[0]);
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
				final List<Float> valueList = new ArrayList<Float>();
				try {
					MapPack param = new MapPack();
					param.put("tagGroup", tagGroup);
					param.put("objType", objType);
					param.put("tagName", TagConstants.NAME_TOTAL);
					param.put("tagValue", NullValue.value);
					param.put("date", date);
					tcp.process(RequestCmd.TAGCNT_TAG_VALUE_DATA, param, new INetReader() {
						public void process(DataInputX in) throws IOException {
							float[] values = in.readArray(new float[0]);
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
							float value = valueList.get(i);
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
	
	private MapValue makeFilterMv() {
		MapValue filterMv = new MapValue();
		Object[] objects = treeViewer.getCheckedElements();
		for (Object o : objects) {
			if (o instanceof TagCount) {
				TagCount tc = (TagCount) o;
				if (StringUtil.isEmpty(tc.tagName)) continue;
				String tagName = tc.tagName;
				ListValue lv = filterMv.getList(tagName);
				if (lv == null) {
					lv = filterMv.newList(tagName);
				}
				lv.add(TagCountUtil.convertTagToValue(tagName, tc.value));
			}
		}
		return filterMv;
	}
	
	int lastIndex;
	int lastSize;
	long firstTime;
	long lastTime;
	long firstTxid;
	long lastTxid;
	
	private void loadInitData(final String tagGroup, final long stime, final long etime, final MapValue filterMv) {
		lastIndex = 0;
		lastSize = 0;
		firstTime = 0;
		lastTime = 0;
		firstTxid = 0;
		lastTxid = 0;
		leftBtn.setEnabled(false);
		rightBtn.setEnabled(false);
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				List<Pack> list = loadData(tagGroup, stime, etime, false, null, filterMv);
				final ArrayList<Pack> result = new ArrayList<Pack>(list);
				lastIndex = lastSize = result.size();
				ExUtil.exec(dataTableSash, new Runnable() {
					public void run() {
							rangeLbl.setText(DateUtil.format(stime, "HH:mm") + " ~ " + DateUtil.format(stime + DateUtil.MILLIS_PER_MINUTE, "HH:mm"));
							dataRangeLbl.setText(lastSize > 0 ? "1 ~ " + lastIndex : "0");
							if (lastIndex == LIMIT_PER_PAGE) {
								rightBtn.setEnabled(true);
							}
							if (TagConstants.GROUP_SERVICE.equals(tagGroup)) {
								if (lastSize > 0) {
									Pack p = result.get(0);
									XLogPack xp = (XLogPack) p;
									firstTime = xp.endTime;
									firstTxid = xp.txid;
									p = result.get(result.size()-1);
									xp = (XLogPack) p;
									lastTime = xp.endTime;
									lastTxid = xp.txid;
								}
								serviceTable.setInput(result, serverId, tagGroup);
							} else if (TagConstants.GROUP_ALERT.equals(tagGroup)) {
								if (lastSize > 0) {
									Pack p = result.get(0);
									AlertPack xp = (AlertPack) p;
									firstTime = xp.time;
									p = result.get(result.size()-1);
									xp = (AlertPack) p;
									lastTime = xp.time;
								}
								alertTable.setInput(result, serverId, tagGroup);
							}
					}
				});
			}
		});
	}
	
	private void loadRightData(final String tagGroup, final MapValue filterMv) {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				long stime = lastTime;
				long etime = (stime + DateUtil.MILLIS_PER_MINUTE) / DateUtil.MILLIS_PER_MINUTE * DateUtil.MILLIS_PER_MINUTE - 1;
				MapPack extra = new MapPack();
				if (TagConstants.GROUP_SERVICE.equals(tagGroup)) {
					extra.put("txid", lastTxid);
				}
				List<Pack> list = loadData(tagGroup, stime, etime, false, extra, filterMv);
				final ArrayList<Pack> result = new ArrayList<Pack>(list);
				lastSize = result.size();
				ExUtil.exec(dataTableSash, new Runnable() {
					public void run() {
						dataRangeLbl.setText((lastIndex + 1) + " ~ " + (lastSize > 0 ? (lastIndex + lastSize) : ""));
						lastIndex += lastSize;
						if (lastSize < LIMIT_PER_PAGE) {
							rightBtn.setEnabled(false);
						}
						leftBtn.setEnabled(true);
						if (TagConstants.GROUP_SERVICE.equals(tagGroup)) {
							if (lastSize > 0) {
								Pack p = result.get(0);
								XLogPack xp = (XLogPack) p;
								firstTime = xp.endTime;
								firstTxid = xp.txid;
								p = result.get(result.size()-1);
								xp = (XLogPack) p;
								lastTime = xp.endTime;
								lastTxid = xp.txid;
							} else {
								firstTime = lastTime + 1;
								firstTxid = lastTxid = 0;
							}
							serviceTable.setInput(result, serverId, tagGroup);
						} else if (TagConstants.GROUP_ALERT.equals(tagGroup)) {
							if (lastSize > 0) {
								Pack p = result.get(0);
								AlertPack xp = (AlertPack) p;
								firstTime = xp.time;
								p = result.get(result.size()-1);
								xp = (AlertPack) p;
								lastTime = xp.time;
							} else {
								firstTime = lastTime + 1;
								firstTxid = lastTxid = 0;
							}
							alertTable.setInput(result, serverId, tagGroup);
						}
					}
				});
			}
		});		
	}
	
	private void loadLeftData(final String tagGroup, final MapValue filterMv) {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				long etime = firstTime;
				long stime = etime / DateUtil.MILLIS_PER_MINUTE * DateUtil.MILLIS_PER_MINUTE;
				MapPack extra = new MapPack();
				if (TagConstants.GROUP_SERVICE.equals(tagGroup)) {
					extra.put("txid", firstTxid);
				}
				List<Pack> list = loadData(tagGroup, stime, etime, true, extra, filterMv);
				final ArrayList<Pack> revList = new ArrayList<Pack>();
				for (Pack p : list) {
					revList.add(0, p);
				}
				lastIndex -= lastSize;
				lastSize = revList.size();
				ExUtil.exec(dataTableSash, new Runnable() {
					public void run() {
						dataRangeLbl.setText((lastIndex - lastSize + 1) + " ~ " + lastIndex);
						if (lastIndex <= LIMIT_PER_PAGE) {
							leftBtn.setEnabled(false);
						}
						rightBtn.setEnabled(true);
						if (TagConstants.GROUP_SERVICE.equals(tagGroup)) {
							Pack p = revList.get(0);
							XLogPack xp = (XLogPack) p;
							firstTime = xp.endTime;
							firstTxid = xp.txid;
							p = revList.get(revList.size()-1);
							xp = (XLogPack) p;
							lastTime = xp.endTime;
							lastTxid = xp.txid;
							serviceTable.setInput(revList, serverId, tagGroup);
						} else if (TagConstants.GROUP_ALERT.equals(tagGroup)) {
							Pack p = revList.get(0);
							AlertPack xp = (AlertPack) p;
							firstTime = xp.time;
							p = revList.get(revList.size()-1);
							xp = (AlertPack) p;
							lastTime = xp.time;
							alertTable.setInput(revList, serverId, tagGroup);
						}
					}
				});
			}
		});	
	}
	
	private List<Pack> loadData(String tagGroup, long stime, long etime, boolean reverse, MapPack extra, MapValue filterMv) {
		List<Pack> list = null;
		TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
		try {
			MapPack param = new MapPack();
			param.put("objType", objType);
			param.put("stime", stime);
			param.put("etime", etime);
			param.put("tagGroup", tagGroup);
			param.put("date", date);
			param.put("max", LIMIT_PER_PAGE);
			param.put("reverse", new BooleanValue(reverse));
			if (extra != null) {
				Iterator<String> itr = extra.keys();
				while (itr.hasNext()) {
					String key = itr.next();
					Value v = extra.get(key);
					param.put(key, v);
				}
			}
			if (filterMv != null) {
				param.put("filter", filterMv);
			}
			list = tcp.process(RequestCmd.TAGCNT_TAG_ACTUAL_DATA, param);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			TcpProxy.putTcpProxy(tcp);
		}
		return list;
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
					return a.value;
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
}

