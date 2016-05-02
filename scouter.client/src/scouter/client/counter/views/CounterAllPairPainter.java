package scouter.client.counter.views;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.csstudio.swt.xygraph.dataprovider.CircularBufferDataProvider;
import org.csstudio.swt.xygraph.dataprovider.ISample;
import org.csstudio.swt.xygraph.dataprovider.Sample;
import org.csstudio.swt.xygraph.figures.Trace;
import org.csstudio.swt.xygraph.figures.Trace.PointStyle;
import org.csstudio.swt.xygraph.figures.Trace.TraceType;
import org.csstudio.swt.xygraph.figures.XYGraph;
import org.csstudio.swt.xygraph.linearscale.Range;
import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import scouter.client.listeners.RangeMouseListener;
import scouter.client.model.AgentColorManager;
import scouter.client.model.TextProxy;
import scouter.client.preferences.PManager;
import scouter.client.preferences.PreferenceConstants;
import scouter.client.threads.ObjectSelectManager;
import scouter.client.threads.ObjectSelectManager.IObjectCheckListener;
import scouter.client.util.ChartUtil;
import scouter.client.util.ColorUtil;
import scouter.client.util.ExUtil;
import scouter.client.util.ScouterUtil;
import scouter.client.views.ScouterViewPart;
import scouter.util.DateUtil;
import scouter.util.FormatUtil;
import scouter.util.StringUtil;

public abstract class CounterAllPairPainter extends ScouterViewPart implements IObjectCheckListener {
	
	public final static String ID = CounterAllPairPainter.class.getName();

	protected XYGraph xyGraph;
	protected Map<Integer, TracePair> dataMap = new HashMap<Integer, TracePair>();
	TracePair nearestTracePair;
	protected FigureCanvas canvas;
	boolean isActive = false;
	
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout(1, true);
		layout.marginHeight = 5;
		layout.marginWidth = 5;
		parent.setLayout(layout);
		parent.setBackground(ColorUtil.getInstance().getColor(SWT.COLOR_WHITE));
		parent.setBackgroundMode(SWT.INHERIT_FORCE);
		
		canvas = new FigureCanvas(parent);
		canvas.setLayoutData(new GridData(GridData.FILL_BOTH));
		canvas.setScrollBarVisibility(FigureCanvas.NEVER);
		canvas.addControlListener(new ControlListener() {
			boolean lock = false;
			
			public void controlResized(ControlEvent e) {
				org.eclipse.swt.graphics.Rectangle r = canvas.getClientArea();
				if (!lock) {
					lock = true;
					if (ChartUtil.isShowDescriptionAllowSize(r.height)) {
						CounterAllPairPainter.this.setContentDescription(desc);
					} else {
						CounterAllPairPainter.this.setContentDescription("");
					}
					r = canvas.getClientArea();
					xyGraph.setSize(r.width, r.height);
					lock = false;
				}
			}
			
			public void controlMoved(ControlEvent e) {
			}
		});
		
		xyGraph = new XYGraph();
		xyGraph.setShowLegend(false);
		xyGraph.setShowTitle(false);
		canvas.setContents(xyGraph);
		
		xyGraph.primaryXAxis.setDateEnabled(true);
		xyGraph.primaryXAxis.setShowMajorGrid(true);
		
		xyGraph.primaryYAxis.setAutoScale(true);
		xyGraph.primaryYAxis.setShowMajorGrid(true);
		
		xyGraph.primaryXAxis.setTitle("");
		xyGraph.primaryYAxis.setTitle("");
		
		xyGraph.primaryXAxis.setFormatPattern("HH:mm:ss");
		xyGraph.primaryYAxis.setFormatPattern("#,##0");
		
		xyGraph.primaryYAxis.addMouseListener(new RangeMouseListener(getViewSite().getShell(), xyGraph.primaryYAxis));
		
		final DefaultToolTip toolTip = new DefaultToolTip(canvas, DefaultToolTip.RECREATE, true);
		toolTip.setFont(new Font(null, "Arial", 10, SWT.BOLD));
		toolTip.setBackgroundColor(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
		
		canvas.addMouseListener(new MouseListener() {
			public void mouseUp(MouseEvent e) {
				if (nearestTracePair != null) {
					int width = PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH);
					nearestTracePair.setLineWidth(width);
					nearestTracePair = null;
				}
				toolTip.hide();
			}
			public void mouseDown(MouseEvent e) {
				double x = xyGraph.primaryXAxis.getPositionValue(e.x, false);
				double y = xyGraph.primaryYAxis.getPositionValue(e.y, false);
				if (x < 0 || y < 0) {
					return;
				}
				double minDistance = 30.0d;
				long time = 0;
				double max = 0;
				double value = 0;
				Iterator<Integer> keys = dataMap.keySet().iterator();
				while (keys.hasNext()) {
					int objHash = keys.next();
					TracePair tp = dataMap.get(objHash);
					Trace t1 = tp.totalTrace;
					ISample s1 = ScouterUtil.getNearestPoint(t1.getDataProvider(), x);
					Trace t2 = tp.activeTrace;
					ISample s2 = ScouterUtil.getNearestPoint(t2.getDataProvider(), x);
					if (s1 != null && s2 != null) {
						int x1 = xyGraph.primaryXAxis.getValuePosition(s1.getXValue(), false);
						int y1 = xyGraph.primaryYAxis.getValuePosition(s1.getYValue(), false);
						int x2 = xyGraph.primaryXAxis.getValuePosition(s2.getXValue(), false);
						int y2 = xyGraph.primaryYAxis.getValuePosition(s2.getYValue(), false);
						double distance1 = ScouterUtil.getPointDistance(e.x, e.y, x1, y1);
						double distance2 = ScouterUtil.getPointDistance(e.x, e.y, x2, y2);
						double distance = distance1 > distance2 ? distance2 : distance1;
						if (minDistance > distance) {
							minDistance = distance;
							nearestTracePair = tp;
							time = (long) s1.getXValue();
							max = s1.getYValue();
							value = s2.getYValue();
						}
					}
					
				}
				if (nearestTracePair != null) {
					int width = PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH);
					nearestTracePair.setLineWidth(width + 2);
					toolTip.setText(TextProxy.object.getText(nearestTracePair.objHash)
							+ "\nTime : " + DateUtil.format(time, "HH:mm:ss")
							+ "\nMax : " + FormatUtil.print(max, "#,###.##")
							+ "\nValue : " +  FormatUtil.print(value, "#,###.##"));
					toolTip.show(new Point(e.x, e.y));
				}
			}
			public void mouseDoubleClick(MouseEvent e) {}
		});
		
		ObjectSelectManager.getInstance().addObjectCheckStateListener(this);
	}
	
	public void notifyChangeState() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				for (TracePair tp : dataMap.values()) {
					if (ObjectSelectManager.getInstance().isUnselectedObject(tp.objHash)) {
						tp.setVisible(false);
					} else {
						tp.setVisible(true);
					}
				}
				ExUtil.exec(canvas, new Runnable() {
					public void run() {
						redraw();
					}
				});
			}
		});
	}
	
	protected TracePair getTracePair(String objType, int objHash, int bufferSize) {
		TracePair tp = dataMap.get(objHash);
		if (tp == null) {
			tp = new TracePair();
			tp.objHash = objHash;
			
			CircularBufferDataProvider data1 = new CircularBufferDataProvider(true);
			data1.setBufferSize(bufferSize);
			data1.setCurrentXDataArray(new double[] {});
			data1.setCurrentYDataArray(new double[] {});
			String name = StringUtil.trimToEmpty(TextProxy.object.getText(objHash));
			Trace trace1 = new Trace(name+"(Max)", xyGraph.primaryXAxis, xyGraph.primaryYAxis, data1);
			trace1.setPointStyle(PointStyle.NONE);
			trace1.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
			trace1.setTraceType(TraceType.SOLID_LINE);
			trace1.setTraceColor(AgentColorManager.getInstance().assignColor(objType, objHash));
			xyGraph.addTrace(trace1);
			tp.totalTrace = trace1;
			
			CircularBufferDataProvider data2 = new CircularBufferDataProvider(true);
			data2.setBufferSize(bufferSize);
			data2.setCurrentXDataArray(new double[] {});
			data2.setCurrentYDataArray(new double[] {});
			Trace trace2 = new Trace(name+"(Value)", xyGraph.primaryXAxis, xyGraph.primaryYAxis, data2);
			trace2.setPointStyle(PointStyle.NONE);
			trace2.setLineWidth(PManager.getInstance().getInt(PreferenceConstants.P_CHART_LINE_WIDTH));
			trace2.setTraceType(TraceType.SOLID_LINE);
			trace2.setTraceColor(AgentColorManager.getInstance().assignColor(objType, objHash));
			xyGraph.addTrace(trace2);
			tp.activeTrace = trace2;
			
			dataMap.put(objHash, tp);
		}
		return tp;
	}

	protected double getMaxValue() {
		Iterator<Integer> objHashs = dataMap.keySet().iterator();
		double max = 0.0;
		Range xRange = xyGraph.primaryXAxis.getRange();
		double lower = xRange.getLower();
		double upper = xRange.getUpper();
		while (objHashs.hasNext()) {
			int objHash = objHashs.next();
			CircularBufferDataProvider data = (CircularBufferDataProvider) dataMap.get(objHash).totalTrace.getDataProvider();
			if (data != null) {
				for (int inx = 0; inx < data.getSize(); inx++) {
					Sample sample = (Sample) data.getSample(inx);
					double x = sample.getXValue();
					if(x < lower || x > upper) {
						continue;
					}
					double y = sample.getYValue();
					if (y > max) {
						max = y;
					}
				}
			}
		}
		return ChartUtil.getMaxValue(max);
	}
	
	@Override
	public void dispose() {
		super.dispose();
		ObjectSelectManager.getInstance().removeObjectCheckStateListener(this);
	}
	
	public void redraw() {
		if (canvas != null && canvas.isDisposed() == false) {
			canvas.redraw();
			xyGraph.repaint();
		}
	}
	
	public static class TracePair {
		int objHash;
		Trace totalTrace;
		Trace activeTrace;
		
		public void setLineWidth(int width) {
			if (totalTrace != null)  totalTrace.setLineWidth(width);
			if (activeTrace != null)  activeTrace.setLineWidth(width);
		}
		
		public void setVisible(boolean visible) {
			if (totalTrace != null)  totalTrace.setVisible(visible);
			if (activeTrace != null)  activeTrace.setVisible(visible);
		}
	}
}
