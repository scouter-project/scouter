/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.xygraph.figures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.csstudio.swt.xygraph.Messages;
import org.csstudio.swt.xygraph.Preferences;
import org.csstudio.swt.xygraph.dataprovider.IDataProvider;
import org.csstudio.swt.xygraph.dataprovider.IDataProviderListener;
import org.csstudio.swt.xygraph.dataprovider.ISample;
import org.csstudio.swt.xygraph.dataprovider.Sample;
import org.csstudio.swt.xygraph.linearscale.AbstractScale.LabelSide;
import org.csstudio.swt.xygraph.linearscale.Range;
import org.csstudio.swt.xygraph.util.SWTConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * The trace figure.
 * 
 * @author Xihui Chen
 * @author Kay Kasemir (synchronization, STEP_HORIZONTALLY tweaks)
 * @author Laurent PHILIPPE (Add trace listeners)
 * @author Takashi Nakamoto @ Cosylab (performance improvement)
 */
public class Trace extends Figure implements IDataProviderListener,
		IAxisListener {
	/** Size of 'markers' used on X axis to indicate non-plottable samples */
	final private static int MARKER_SIZE = 6;

	/**
	 * Use advanced graphics? Might not make a real performance difference, but
	 * since this it called a lot, keep it in variable
	 */
	final private boolean use_advanced_graphics = Preferences
			.useAdvancedGraphics();

	/**
	 * The way how the trace will be drawn.
	 * 
	 * @author Xihui Chen
	 */
	public enum TraceType {
		/** Solid Line */
		SOLID_LINE(Messages.TraceSolid),

		/** Dash Line */
		DASH_LINE(Messages.TraceDash),

		/**
		 * Only draw point whose style is defined by pointStyle. Its size is
		 * defined by pointSize.
		 */
		POINT(Messages.TracePoint),

		/**
		 * Draw each data point as a bar whose width is defined by lineWidth.
		 * The data point is in the middle of the bar on X direction. The bottom
		 * of the bar depends on the baseline. The alpha of the bar is defined
		 * by areaAlpha.
		 */
		BAR(Messages.TraceBar),

		/**
		 * Fill the area under the trace. The bottom of the filled area depends
		 * on the baseline. The alpha of the filled area is defined by
		 * areaAlpha.
		 */
		AREA(Messages.TraceArea),
		/**
		 * Solid line in step. It looks like the y value(on vertical direction)
		 * changed firstly.
		 */
		STEP_VERTICALLY(Messages.TraceStepVert),

		/**
		 * Solid line in step. It looks like the x value(on horizontal
		 * direction) changed firstly.
		 */
		STEP_HORIZONTALLY(Messages.TraceStepHoriz);

		/** Draw a single point. Only the last data point will be drawn. */
		// SINGLE_POINT("Single Point");

		private TraceType(String description) {
			this.description = description;
		}

		private String description;

		@Override
		public String toString() {
			return description;
		}

		public static String[] stringValues() {
			String[] sv = new String[values().length];
			int i = 0;
			for (TraceType p : values())
				sv[i++] = p.toString();
			return sv;
		}
	}

	public enum BaseLine {
		NEGATIVE_INFINITY, ZERO, POSITIVE_INFINITY;

		public static String[] stringValues() {
			String[] sv = new String[values().length];
			int i = 0;
			for (BaseLine p : values())
				sv[i++] = p.toString();
			return sv;
		}
	}

	public enum PointStyle {
		NONE(Messages.PointNone), POINT(Messages.PointPoint), CIRCLE(
				Messages.PointCircle), TRIANGLE(Messages.PointTriangle), FILLED_TRIANGLE(
				Messages.PointFilledTriangle), SQUARE(Messages.PointSquare), FILLED_SQUARE(
				Messages.PointFilledSquare), DIAMOND(Messages.PointDiamond), FILLED_DIAMOND(
				Messages.PointFilledDiamond), XCROSS(Messages.PointCross), CROSS(
				Messages.ProintCross2), BAR(Messages.PointBar);

		private PointStyle(String description) {
			this.description = description;
		}

		private String description;

		@Override
		public String toString() {
			return description;
		}

		public static String[] stringValues() {
			String[] sv = new String[values().length];
			int i = 0;
			for (PointStyle p : values())
				sv[i++] = p.toString();
			return sv;
		}
	}

	public enum ErrorBarType {
		NONE, PLUS, MINUS, BOTH;

		public static String[] stringValues() {
			String[] sv = new String[values().length];
			int i = 0;
			for (ErrorBarType p : values())
				sv[i++] = p.toString();
			return sv;
		}
	}

	/**
	 * List of trace listeners
	 * 
	 * @author Laurent PHILIPPE
	 */
	final private List<ITraceListener> listeners = new ArrayList<ITraceListener>();

	public void addListener(final ITraceListener listener) {
		if (listeners.contains(listener))
			return;
		listeners.add(listener);
	}

	public boolean removeListener(final ITraceListener listener) {
		return listeners.remove(listener);
	}

	private String name;

	private IDataProvider traceDataProvider;

	private Axis xAxis;
	private Axis yAxis;

	/**
	 * Color used to draw the main line/marker of the trace. Also used for error
	 * bars unless errorBarColor is defined
	 */
	private Color traceColor;

	private TraceType traceType = TraceType.SOLID_LINE;

	private BaseLine baseLine = BaseLine.ZERO;

	private PointStyle pointStyle = PointStyle.NONE;

	/**
	 * If traceType is bar, this is the width of the bar.
	 */
	private int lineWidth = 1;

	private int pointSize = 4;

	private int areaAlpha = 100;

	private boolean antiAliasing = true;

	private boolean errorBarEnabled = false;
	private ErrorBarType yErrorBarType = ErrorBarType.BOTH;
	private ErrorBarType xErrorBarType = ErrorBarType.BOTH;
	private int errorBarCapWidth = 4;
	private boolean errorBarColorSetFlag = false;

	/**
	 * Color used for error bars. If <code>null</code>, traceColor is used
	 */
	private Color errorBarColor;
	private boolean drawYErrorInArea = false;
	private XYGraph xyGraph;

	private List<ISample> hotSampleist;

	public Trace(String name, Axis xAxis, Axis yAxis, IDataProvider dataProvider) {
		this.setName(name);
		this.xAxis = xAxis;
		this.yAxis = yAxis;
		xAxis.addTrace(this);
		yAxis.addTrace(this);
		xAxis.addListener(this);
		yAxis.addListener(this);
		setDataProvider(dataProvider);
		hotSampleist = new ArrayList<ISample>();
	}

	private void drawErrorBar(Graphics graphics, Point dpPos, ISample dp) {
		graphics.pushState();
		graphics.setForegroundColor(errorBarColor);
		graphics.setLineStyle(SWTConstants.LINE_SOLID);
		graphics.setLineWidth(1);
		Point ep;
		switch (yErrorBarType) {
		case BOTH:
		case MINUS:
			ep = new Point(xAxis.getValuePosition(dp.getXValue(), false),
					yAxis.getValuePosition(
							dp.getYValue() - dp.getYMinusError(), false));
			graphics.drawLine(dpPos, ep);
			graphics.drawLine(ep.x - errorBarCapWidth / 2, ep.y, ep.x
					+ errorBarCapWidth / 2, ep.y);
			if (yErrorBarType != ErrorBarType.BOTH)
				break;
		case PLUS:
			ep = new Point(xAxis.getValuePosition(dp.getXValue(), false),
					yAxis.getValuePosition(dp.getYValue() + dp.getYPlusError(),
							false));
			graphics.drawLine(dpPos, ep);
			graphics.drawLine(ep.x - errorBarCapWidth / 2, ep.y, ep.x
					+ errorBarCapWidth / 2, ep.y);
			break;
		default:
			break;
		}

		switch (xErrorBarType) {
		case BOTH:
		case MINUS:
			ep = new Point(xAxis.getValuePosition(
					dp.getXValue() - dp.getXMinusError(), false),
					yAxis.getValuePosition(dp.getYValue(), false));
			graphics.drawLine(dpPos, ep);
			graphics.drawLine(ep.x, ep.y - errorBarCapWidth / 2, ep.x, ep.y
					+ errorBarCapWidth / 2);
			if (xErrorBarType != ErrorBarType.BOTH)
				break;
		case PLUS:
			ep = new Point(xAxis.getValuePosition(
					dp.getXValue() + dp.getXPlusError(), false),
					yAxis.getValuePosition(dp.getYValue(), false));
			graphics.drawLine(dpPos, ep);
			graphics.drawLine(ep.x, ep.y - errorBarCapWidth / 2, ep.x, ep.y
					+ errorBarCapWidth / 2);
			break;
		default:
			break;
		}

		graphics.popState();
	}

	private void drawYErrorArea(final Graphics graphics, final ISample predp,
			final ISample dp, final Point predpPos, final Point dpPos) {
		// Shortcut if there is no error area
		if (predp.getYPlusError() == 0.0 && predp.getYMinusError() == 0.0
				&& dp.getYPlusError() == 0.0 && dp.getYMinusError() == 0.0)
			return;

		graphics.pushState();
		Color lighter = null;
		if (use_advanced_graphics) {
			graphics.setBackgroundColor(errorBarColor);
			graphics.setAlpha(areaAlpha);
		} else {
			final float[] hsb = errorBarColor.getRGB().getHSB();
			lighter = new Color(Display.getCurrent(), new RGB(hsb[0], hsb[1]
					* areaAlpha / 255, 1.0f));
			graphics.setBackgroundColor(lighter);
		}

		final int predp_xpos = xAxis.getValuePosition(predp.getXValue(), false);
		final int dp_xpos = xAxis.getValuePosition(dp.getXValue(), false);
		Point preEp, ep;
		switch (yErrorBarType) {
		case BOTH:
		case PLUS:
			preEp = new Point(predp_xpos, yAxis.getValuePosition(
					predp.getYValue() + predp.getYPlusError(), false));
			ep = new Point(dp_xpos, yAxis.getValuePosition(
					dp.getYValue() + dp.getYPlusError(), false));
			graphics.fillPolygon(new int[] { predpPos.x, predpPos.y, preEp.x,
					preEp.y, ep.x, ep.y, dpPos.x, dpPos.y });
			if (yErrorBarType != ErrorBarType.BOTH)
				break;
		case MINUS:
			preEp = new Point(predp_xpos, yAxis.getValuePosition(
					predp.getYValue() - predp.getYMinusError(), false));
			ep = new Point(dp_xpos, yAxis.getValuePosition(
					dp.getYValue() - dp.getYMinusError(), false));
			graphics.fillPolygon(new int[] { predpPos.x, predpPos.y, preEp.x,
					preEp.y, ep.x, ep.y, dpPos.x, dpPos.y });
			break;
		default:
			break;
		}
		graphics.popState();
		if (lighter != null)
			lighter.dispose();
	}

	/**
	 * Draw point with the pointStyle and size of the trace;
	 * 
	 * @param graphics
	 * @param pos
	 */
	public void drawPoint(Graphics graphics, Point pos) {
		// Shortcut when no point requested
		if (pointStyle == PointStyle.NONE)
			return;
		graphics.pushState();
		graphics.setBackgroundColor(traceColor);
		// graphics.setForegroundColor(traceColor);
		graphics.setLineWidth(1);
		graphics.setLineStyle(SWTConstants.LINE_SOLID);
		switch (pointStyle) {
		case POINT:
			graphics.fillOval(new Rectangle(pos.x - pointSize / 2, pos.y
					- pointSize / 2, pointSize, pointSize));
			break;
		case CIRCLE:
			graphics.drawOval(new Rectangle(pos.x - pointSize / 2, pos.y
					- pointSize / 2, pointSize, pointSize));
			break;
		case TRIANGLE:
			graphics.drawPolygon(new int[] { pos.x - pointSize / 2,
					pos.y + pointSize / 2, pos.x, pos.y - pointSize / 2,
					pos.x + pointSize / 2, pos.y + pointSize / 2 });
			break;
		case FILLED_TRIANGLE:
			graphics.fillPolygon(new int[] { pos.x - pointSize / 2,
					pos.y + pointSize / 2, pos.x, pos.y - pointSize / 2,
					pos.x + pointSize / 2, pos.y + pointSize / 2 });
			break;
		case SQUARE:
			graphics.drawRectangle(new Rectangle(pos.x - pointSize / 2, pos.y
					- pointSize / 2, pointSize, pointSize));
			break;
		case FILLED_SQUARE:
			graphics.fillRectangle(new Rectangle(pos.x - pointSize / 2, pos.y
					- pointSize / 2, pointSize, pointSize));
			break;
		case BAR:
			graphics.drawLine(pos.x, pos.y - pointSize / 2, pos.x, pos.y
					+ pointSize / 2);
			break;
		case CROSS:
			graphics.drawLine(pos.x, pos.y - pointSize / 2, pos.x, pos.y
					+ pointSize / 2);
			graphics.drawLine(pos.x - pointSize / 2, pos.y, pos.x + pointSize
					/ 2, pos.y);
			break;
		case XCROSS:
			graphics.drawLine(pos.x - pointSize / 2, pos.y - pointSize / 2,
					pos.x + pointSize / 2, pos.y + pointSize / 2);
			graphics.drawLine(pos.x + pointSize / 2, pos.y - pointSize / 2,
					pos.x - pointSize / 2, pos.y + pointSize / 2);
			break;
		case DIAMOND:
			graphics.drawPolyline(new int[] { pos.x, pos.y - pointSize / 2,
					pos.x - pointSize / 2, pos.y, pos.x, pos.y + pointSize / 2,
					pos.x + pointSize / 2, pos.y, pos.x, pos.y - pointSize / 2 });
			break;
		case FILLED_DIAMOND:
			graphics.fillPolygon(new int[] { pos.x, pos.y - pointSize / 2,
					pos.x - pointSize / 2, pos.y, pos.x, pos.y + pointSize / 2,
					pos.x + pointSize / 2, pos.y });
			break;
		default:
			break;
		}
		graphics.popState();
	}

	/**
	 * Draw line with the line style and line width of the trace.
	 * 
	 * @param graphics
	 * @param p1
	 * @param p2
	 */
	public void drawLine(Graphics graphics, Point p1, Point p2) {
		graphics.pushState();
		graphics.setLineWidth(lineWidth);
		switch (traceType) {
		case SOLID_LINE:
			graphics.setLineStyle(SWTConstants.LINE_SOLID);
			graphics.drawLine(p1, p2);
			break;
		case BAR:
			if (use_advanced_graphics)
				graphics.setAlpha(areaAlpha);
			graphics.setLineStyle(SWTConstants.LINE_SOLID);
			graphics.drawLine(p1, p2);
			break;
		case DASH_LINE:
			graphics.setLineStyle(SWTConstants.LINE_DASH);
			graphics.drawLine(p1, p2);
			break;
		case AREA:
			int basey;
			switch (baseLine) {
			case NEGATIVE_INFINITY:
				basey = yAxis.getValuePosition(yAxis.getRange().getLower(),
						false);
				break;
			case POSITIVE_INFINITY:
				basey = yAxis.getValuePosition(yAxis.getRange().getUpper(),
						false);
				break;
			default:
				basey = yAxis.getValuePosition(0, false);
				break;
			}
			if (use_advanced_graphics)
				graphics.setAlpha(areaAlpha);
			graphics.setBackgroundColor(traceColor);
			graphics.fillPolygon(new int[] { p1.x, p1.y, p1.x, basey, p2.x,
					basey, p2.x, p2.y });
			break;
		case STEP_HORIZONTALLY:
			graphics.setLineStyle(SWTConstants.LINE_SOLID);
			graphics.drawLine(p1.x, p1.y, p2.x, p1.y);
			graphics.drawLine(p2.x, p1.y, p2.x, p2.y);
			break;
		case STEP_VERTICALLY:
			graphics.setLineStyle(SWTConstants.LINE_SOLID);
			graphics.drawLine(p1.x, p1.y, p1.x, p2.y);
			graphics.drawLine(p1.x, p2.y, p2.x, p2.y);
			break;

		default:
			break;
		}
		graphics.popState();
	}
	
	/**
 	 * Draw polyline with the line style and line width of the trace.
 	 * 
	 * @param graphics
	 * @param pl
	 */
	private void drawPolyline(Graphics graphics, PointList pl) {
		graphics.pushState();
		graphics.setLineWidth(lineWidth);
		switch(traceType) {
		case SOLID_LINE:
		case STEP_HORIZONTALLY:
		case STEP_VERTICALLY:
			graphics.setLineStyle(SWTConstants.LINE_SOLID);
			graphics.drawPolyline(pl);
			break;
		case DASH_LINE:
			graphics.setLineStyle(SWTConstants.LINE_DASH);
			graphics.drawPolyline(pl);
			break;
		default:
			break;
		}
		graphics.popState();
	}
	
	/**
	 * Added by scouter.project@gmail.com
	 * Setting visible, invisible
	 */
	
	private boolean visible = true;
	
	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	@Override
	protected void paintFigure(Graphics graphics) {
		super.paintFigure(graphics);
		graphics.pushState();
		if (use_advanced_graphics)
			graphics.setAntialias(antiAliasing ? SWT.ON : SWT.OFF);
		graphics.setForegroundColor(traceColor);
		graphics.setLineWidth(lineWidth);
		ISample predp = null;
		boolean predpInRange = false;
		Point dpPos = null;
		hotSampleist.clear();
		if (traceDataProvider == null)
			throw new RuntimeException(
					"No DataProvider defined for trace: " + name); //$NON-NLS-1$
		// Lock data provider to prevent changes while painting
		synchronized (traceDataProvider) {
			if (traceDataProvider.getSize() > 0 /* Added by scouter.project@gmail.com */&& visible) {
				// Is only a sub-set of the trace data visible?
				final int startIndex, endIndex;
				if (traceDataProvider.isChronological()) {
					final Range indexRange = getIndexRangeOnXAxis();
					if (indexRange == null) {
						startIndex = 0;
						endIndex = -1;
					} else {
						startIndex = (int) indexRange.getLower();
						endIndex = (int) indexRange.getUpper();
					}
				} else { // Cannot optimize range, use all data points
					startIndex = 0;
					endIndex = traceDataProvider.getSize() - 1;
				}
				
				// Set of points which were already drawn
				HashSet<Point> hsPoint = new HashSet<Point>();
				
				// List of points for drawing polyline. 
				PointList plPolyline = new PointList();
				
				// List of bottom/top point in a certain horizontal
				// pixel location for the BAR line type.
				HashMap<Integer,Integer> bottomPoints = new HashMap<Integer,Integer>();
				HashMap<Integer,Integer> topPoints = new HashMap<Integer,Integer>();
				
				Point maxInRegion = null;
				Point minInRegion = null;
				Point lastInRegion = null;
				
				for (int i = startIndex; i <= endIndex; i++) {
					ISample dp = traceDataProvider.getSample(i);
					final boolean dpInXRange = xAxis.getRange().inRange(
							dp.getXValue());
					// Mark 'NaN' samples on X axis
					final boolean valueIsNaN = Double.isNaN(dp.getYValue());
					if (dpInXRange && valueIsNaN) {
						Point markPos = new Point(
								xAxis.getValuePosition(dp.getXValue(), false),
								yAxis.getValuePosition(
										xAxis.getTickLablesSide() == LabelSide.Primary ? yAxis
												.getRange().getLower() : yAxis
												.getRange().getUpper(), false));
						graphics.setBackgroundColor(traceColor);
						graphics.fillRectangle(markPos.x - MARKER_SIZE / 2,
								markPos.y - MARKER_SIZE / 2, MARKER_SIZE,
								MARKER_SIZE);
						Sample nanSample = new Sample(
								dp.getXValue(),
								xAxis.getTickLablesSide() == LabelSide.Primary ? yAxis
										.getRange().getLower() : yAxis
										.getRange().getUpper(),
								dp.getYPlusError(), dp.getYMinusError(),
								Double.NaN, dp.getXMinusError(), dp.getInfo());
						hotSampleist.add(nanSample);
					}
					// Is data point in the plot area?
					boolean dpInRange = dpInXRange
							&& yAxis.getRange().inRange(dp.getYValue());
					// draw point
					if (dpInRange) {
						dpPos = new Point(xAxis.getValuePosition(
								dp.getXValue(), false), yAxis.getValuePosition(
								dp.getYValue(), false));
						hotSampleist.add(dp);
						
						// Do not draw points in the same place to improve performance
						if (!hsPoint.contains(dpPos)) {
							drawPoint(graphics, dpPos);
							hsPoint.add(dpPos);
						}
						
						if (errorBarEnabled && !drawYErrorInArea)
							drawErrorBar(graphics, dpPos, dp);
					}
					if (traceType == TraceType.POINT && !drawYErrorInArea)
						continue; // no need to draw line

					// draw line
					if (traceType == TraceType.BAR) {
						switch (baseLine) {
						case NEGATIVE_INFINITY:
							predp = new Sample(dp.getXValue(), yAxis.getRange()
									.getLower());
							break;
						case POSITIVE_INFINITY:
							predp = new Sample(dp.getXValue(), yAxis.getRange()
									.getUpper());
							break;
						default:
							predp = new Sample(dp.getXValue(), 0);
							break;
						}
						predpInRange = xAxis.getRange().inRange(
								predp.getXValue())
								&& yAxis.getRange().inRange(predp.getYValue());
					}
					if (predp == null) { // No previous data point from which to
											// draw a line
						predp = dp;
						predpInRange = dpInRange;
						continue;
					}

					// Save original dp info because handling of NaN or
					// axis intersections might patch it
					final ISample origin_dp = dp;
					final boolean origin_dpInRange = dpInRange;

					// In 'STEP' modes, if there was a value, now there is none,
					// continue that last value until the NaN location
					if (valueIsNaN
							&& !Double.isNaN(predp.getYValue())
							&& (traceType == TraceType.STEP_HORIZONTALLY || traceType == TraceType.STEP_VERTICALLY)) {
						// Patch 'y' of dp, re-compute dpInRange for new 'y'
						dp = new Sample(dp.getXValue(), predp.getYValue());
						dpInRange = yAxis.getRange().inRange(dp.getYValue());
					}

					if (traceType != TraceType.AREA) {
						if (!predpInRange && !dpInRange) { // both are out of
															// plot area
							ISample[] dpTuple = getIntersection(predp, dp);
							if (dpTuple[0] == null || dpTuple[1] == null) { // no
																			// intersection
																			// with
																			// plot
																			// area
								predp = origin_dp;
								predpInRange = origin_dpInRange;
								continue;
							} else {
								predp = dpTuple[0];
								dp = dpTuple[1];
							}
						} else if (!predpInRange || !dpInRange) { // one in and
																	// one out
							// calculate the intersection point with the
							// boundary of plot area.
							if (!predpInRange) {
								predp = getIntersection(predp, dp)[0];
								if (predp == null) { // no intersection
									predp = origin_dp;
									predpInRange = origin_dpInRange;
									continue;
								}
							} else {
								dp = getIntersection(predp, dp)[0];
								if (dp == null) { // no intersection
									predp = origin_dp;
									predpInRange = origin_dpInRange;
									continue;
								}
							}
						}
					}

					final Point predpPos = new Point(xAxis.getValuePosition(
							predp.getXValue(), false), yAxis.getValuePosition(
							predp.getYValue(), false));
					dpPos = new Point(xAxis.getValuePosition(dp.getXValue(),
							false), yAxis.getValuePosition(dp.getYValue(),
							false));

					if (!dpPos.equals(predpPos)) {
						if (errorBarEnabled && drawYErrorInArea
								&& traceType != TraceType.BAR)
							drawYErrorArea(graphics, predp, dp, predpPos, dpPos);
						
						switch (traceType) {
						case SOLID_LINE:
						case DASH_LINE:
						case STEP_HORIZONTALLY:
						case STEP_VERTICALLY:
							if (plPolyline.size() == 0)
								plPolyline.addPoint(predpPos);

							if (traceDataProvider.isChronological()) {
								// Line drawing optimization is available only when the trace data
								// is ascending sorted on X axis. 
								if (!predpPos.equals(plPolyline.getLastPoint()) && predpPos.x != plPolyline.getLastPoint().x) {
									// The line for this trace is not continuous.
									// Draw a polylin at this point, and start to reconstruct a new
									// polyline for the rest of the trace.
									
									if (lastInRegion != null) {
										// There were several points which have the same X value.
										// Draw lines that connect those points at once.
										if (minInRegion != null)
											plPolyline.addPoint(minInRegion);
										if (maxInRegion != null)
											plPolyline.addPoint(maxInRegion);

										plPolyline.addPoint(lastInRegion);
										
										minInRegion = null;
										maxInRegion = null;
										lastInRegion = null;
									}
									
									drawPolyline(graphics, plPolyline);
									plPolyline.removeAllPoints();
									plPolyline.addPoint(predpPos);
									
									switch (traceType) {
									case STEP_HORIZONTALLY:
										plPolyline.addPoint(dpPos.x, predpPos.y);
										break;
									case STEP_VERTICALLY:
										plPolyline.addPoint(predpPos.x, dpPos.y);
										break;
									}
									
									plPolyline.addPoint(dpPos);
								} else {
									if (predpPos.x != dpPos.x) {
										if (lastInRegion == null) {
											switch (traceType) {
											case STEP_HORIZONTALLY:
												plPolyline.addPoint(dpPos.x, predpPos.y);
												break;
											case STEP_VERTICALLY:
												plPolyline.addPoint(predpPos.x, dpPos.y);
												break;
											}
											
											plPolyline.addPoint(dpPos);
										} else {
											// There were several points which have the same X value.
											// Draw lines that connect those points at once.
											if (minInRegion != null)
												plPolyline.addPoint(minInRegion);
											if (maxInRegion != null)
												plPolyline.addPoint(maxInRegion);

											plPolyline.addPoint(lastInRegion);
											
											switch (traceType) {
											case STEP_HORIZONTALLY:
												plPolyline.addPoint(dpPos.x, lastInRegion.y);
												break;
											case STEP_VERTICALLY:
												plPolyline.addPoint(lastInRegion.x, dpPos.y);
												break;
											}

											// The first point of the next region is drawn anyway.
											plPolyline.addPoint(dpPos);
										}
										
										minInRegion = null;
										maxInRegion = null;
										lastInRegion = null;
									} else {
										// The current point has the same X value as the previous point.
										if (lastInRegion == null) {
											// At this moment, there are two points which have the same
											// X value.
											lastInRegion = dpPos;
										} else if (minInRegion == null) {
											// At this moment, there are three points which have the
											// same X value.
											minInRegion = lastInRegion;
											lastInRegion = dpPos;
										} else if (maxInRegion == null) {
											// At this moment, there are four points which have the same
											// X value.
											if (minInRegion.y > lastInRegion.y) {
												maxInRegion = minInRegion;
												minInRegion = lastInRegion;
											} else {
												maxInRegion = lastInRegion;
											}
											lastInRegion = dpPos;
										} else {
											// There are more than four points which have the same X
											// value.
											if (lastInRegion.y > maxInRegion.y) {
												maxInRegion = lastInRegion; 
											} else if (lastInRegion.y < minInRegion.y) {
												minInRegion = lastInRegion;
											}
											lastInRegion = dpPos;
										}
									}
								}
							} else {
								if (!predpPos.equals(plPolyline.getLastPoint())) {
									// The line for this trace may not be continuous.
									// Draw a polyline at this point, and start to reconstruct a new
									// polyline for the rest of the trace.
									drawPolyline(graphics, plPolyline);
									plPolyline.removeAllPoints();
									plPolyline.addPoint(predpPos);
								}
								
								switch (traceType) {
								case STEP_HORIZONTALLY:
									plPolyline.addPoint(dpPos.x, predpPos.y);
									break;
								case STEP_VERTICALLY:
									plPolyline.addPoint(predpPos.x, dpPos.y);
									break;
								}
								
								plPolyline.addPoint(dpPos);
							}
							
							break;
						case BAR:
							if (!use_advanced_graphics && predpPos.x() == dpPos.x()) {
								// Stores bar line infomration in memory, and draw lines later.
								Integer posX = new Integer(predpPos.x());
								Integer highY;
								Integer lowY;
								
								if (dpPos.y() > predpPos.y()) {
									highY = new Integer(dpPos.y());
									lowY = new Integer(predpPos.y());
								} else {
									highY = new Integer(predpPos.y());
									lowY = new Integer(dpPos.y());
								}
								
								if (bottomPoints.containsKey(posX)) {
									if (lowY.compareTo(bottomPoints.get(posX)) < 0) { 
										bottomPoints.put(posX, lowY);
									}
									if (highY.compareTo(topPoints.get(posX)) > 0) {
										topPoints.put(posX, highY);
									}
								} else {
									bottomPoints.put(posX, lowY);
									topPoints.put(posX, highY);
								}
							} else {
								// If the X value is different for some reason, or the advanced graphics is
								// turned on, fall back to the original drawing algorithm.
								drawLine(graphics, predpPos, dpPos);			
							}
							break;
						default:
							drawLine(graphics, predpPos, dpPos);
							break;
						}
					}

					predp = origin_dp;
					predpInRange = origin_dpInRange;
				}
				
				switch (traceType) {
				case SOLID_LINE:
				case DASH_LINE:
				case STEP_HORIZONTALLY:
				case STEP_VERTICALLY:
					// Draw polyline which was not drawn yet.
					drawPolyline(graphics, plPolyline);
					break;
				case BAR:
					// Draw bar lines
					Set<Integer> xSet = bottomPoints.keySet();
					for (Iterator<Integer> i = xSet.iterator(); i.hasNext(); ) {
						Integer posX = (Integer) i.next();
						Point p1 = new Point(posX.intValue(), bottomPoints.get(posX).intValue());
						Point p2 = new Point(posX.intValue(), topPoints.get(posX).intValue());
						drawLine(graphics, p1, p2);
					}
					break;
				default:
					break;
				}
			}
		}
		graphics.popState();
	}

	/**
	 * Compute axes intersection considering the 'TraceType'
	 * 
	 * @param dp1
	 *            'Start' point of line
	 * @param dp2
	 *            'End' point of line
	 * @return The intersection points with the axes when draw the line between
	 *         the two data points. The index 0 of the result is the first
	 *         intersection point. index 1 is the second one.
	 */
	private ISample[] getIntersection(final ISample dp1, final ISample dp2) {
		if (traceType == TraceType.STEP_HORIZONTALLY) {
			final ISample[] result = new Sample[2];
			int count = 0;
			// Data point between dp1 and dp2 using horizontal steps:
			// dp2
			// |
			// dp1--------dp
			final ISample dp = new Sample(dp2.getXValue(), dp1.getYValue());
			// Check intersections of horizontal dp1------dp section
			final ISample iy[] = getStraightLineIntersection(dp1, dp);
			// Intersects both y axes?
			if (iy[1] != null)
				return iy;
			// Intersects one y axis?
			if (iy[0] != null)
				result[count++] = iy[0];
			// Check intersections of vertical dp/dp2 section with x axes
			final ISample ix[] = getStraightLineIntersection(dp, dp2);
			// Intersects both x axes?
			if (ix[1] != null)
				return ix;
			// Intersects one x axis?
			if (ix[0] != null)
				result[count++] = ix[0];
			return result;
		}
		if (traceType == TraceType.STEP_VERTICALLY) {
			final ISample[] result = new Sample[2];
			int count = 0;
			// Data point between dp1 and dp2 using vertical steps:
			// dp---------dp2
			// |
			// dp1
			final ISample dp = new Sample(dp1.getXValue(), dp2.getYValue());
			// Check intersections of vertical dp1/dp section
			final ISample ix[] = getStraightLineIntersection(dp1, dp);
			// Intersects both X axes?
			if (ix[1] != null)
				return ix;
			// Intersects one X axis?
			if (ix[0] != null)
				result[count++] = ix[0];
			// Check intersection of horizontal dp----dp2 section with Y axes
			final ISample iy[] = getStraightLineIntersection(dp, dp2);
			// Intersects both y axes?
			if (iy[1] != null)
				return iy;
			// Intersects one y axis?
			if (iy[0] != null)
				result[count++] = iy[0];
			return result;
		}
		return getStraightLineIntersection(dp1, dp2);
	}

	/**
	 * Compute intersection of straight line with axes, no correction for
	 * 'TraceType'.
	 * 
	 * @param dp1
	 *            'Start' point of line
	 * @param dp2
	 *            'End' point of line
	 * @return The intersection points between the line, which is the straight
	 *         line between the two data points, and the axes. Result could be {
	 *         null, null }, { point1, null } or { point1, point2 }.
	 */
	private ISample[] getStraightLineIntersection(final ISample dp1,
			final ISample dp2) {
		final double x1 = dp1.getXValue();
		final double y1 = dp1.getYValue();
		final double x2 = dp2.getXValue();
		final double y2 = dp2.getYValue();
		final double dx = x2 - x1;
		final double dy = y2 - y1;
		final ISample[] dpTuple = new Sample[2];
		int count = 0; // number of valid dbTuple entries
		double x, y;

		if (dy != 0.0) { // Intersection with lower xAxis
			final double ymin = yAxis.getRange().getLower();
			x = (ymin - y1) * dx / dy + x1;
			y = ymin;
			if (evalDP(x, y, dp1, dp2))
				dpTuple[count++] = new Sample(x, y);
			// Intersection with upper xAxis
			final double ymax = yAxis.getRange().getUpper();
			x = (ymax - y1) * dx / dy + x1;
			y = ymax;
			if (evalDP(x, y, dp1, dp2))
				dpTuple[count++] = new Sample(x, y);
		}
		// A line that runs diagonally through the plot,
		// hitting for example the lower left as well as upper right corners
		// would cut both X as well as both Y axes.
		// Return only the X axes hits, since Y axes hits are actually the
		// same points.
		if (count == 2)
			return dpTuple;
		if (dx != 0.0) { // Intersection with left yAxis
			final double xmin = xAxis.getRange().getLower();
			x = xmin;
			y = (xmin - x1) * dy / dx + y1;
			if (evalDP(x, y, dp1, dp2))
				dpTuple[count++] = new Sample(x, y);
			// Intersection with right yAxis
			final double xmax = xAxis.getRange().getUpper();
			x = xmax;
			y = (xmax - x1) * dy / dx + y1;
			if (dx != 0 && evalDP(x, y, dp1, dp2))
				dpTuple[count++] = new Sample(x, y);
		}
		return dpTuple;
	}

	/**
	 * Sanity check: Point x/y was computed to be an axis intersection, but that
	 * can fail because of rounding errors or for samples with NaN, Infinity. Is
	 * it in the plot area? Is it between the start/end points.
	 * 
	 * @param x
	 * @param y
	 * @param dp1
	 * @param dp2
	 * @return true if the point (x,y) is between dp1 and dp2 BUT not equal to
	 *         either AND within the x/y axes. false otherwise
	 */
	private boolean evalDP(final double x, final double y, final ISample dp1,
			final ISample dp2) {
		// First check axis limits
		if (!xAxis.getRange().inRange(x) || !yAxis.getRange().inRange(y))
			return false;
		// Check if dp is between dp1 and dp2.
		// Could this be done without constructing 2 new Ranges?
		if (!new Range(dp1.getXValue(), dp2.getXValue()).inRange(x)
				|| !new Range(dp1.getYValue(), dp2.getYValue()).inRange(y))
			return false;
		// TODO why the ==dp1,2 test?
		final ISample dp = new Sample(x, y);
		if (dp.equals(dp1) || dp.equals(dp2))
			return false;
		return true;
	}

	/**
	 * @param axis
	 *            the xAxis to set
	 */
	public void setXAxis(Axis axis) {
		if (xAxis == axis)
			return;
		if (xAxis != null) {
			xAxis.removeListenr(this);
			xAxis.removeTrace(this);
		}

		/*
		 * if(traceDataProvider != null){
		 * traceDataProvider.removeDataProviderListener(xAxis);
		 * traceDataProvider.addDataProviderListener(axis); }
		 */
		xAxis = axis;
		xAxis.addTrace(this);
		xAxis.addListener(this);
		revalidate();
	}

	/**
	 * @return the xAxis
	 */
	public Axis getXAxis() {
		return xAxis;
	}

	/**
	 * @param axis
	 *            the yAxis to set
	 */
	public void setYAxis(Axis axis) {

		Axis old = yAxis;

		if (yAxis == axis) {
			return;
		} 

		xyGraph.getLegendMap().get(yAxis).removeTrace(this);
		if (xyGraph.getLegendMap().get(yAxis).getTraceList().size() <= 0) {
			xyGraph.remove(xyGraph.getLegendMap().get(yAxis));
			xyGraph.getLegendMap().remove(yAxis);
		}
		if (xyGraph.getLegendMap().containsKey(axis))
			xyGraph.getLegendMap().get(axis).addTrace(this);
		else {
			xyGraph.getLegendMap().put(axis, new Legend(xyGraph));
			xyGraph.getLegendMap().get(axis).addTrace(this);
			xyGraph.add(xyGraph.getLegendMap().get(axis));
		}

		if (yAxis != null) {
			yAxis.removeListenr(this);
			yAxis.removeTrace(this);
		}
		/*
		 * if(traceDataProvider != null){
		 * traceDataProvider.removeDataProviderListener(yAxis);
		 * traceDataProvider.addDataProviderListener(axis); }
		 */
		yAxis = axis;
		yAxis.addTrace(this);
		yAxis.addListener(this);

		fireYAxisChanged(old, yAxis);

		xyGraph.repaint();
	}

	private void fireYAxisChanged(Axis oldName, Axis newName) {
		for (ITraceListener listener : listeners)
			listener.traceYAxisChanged(this, oldName, newName);
	}

	/**
	 * @param traceDataProvider
	 *            the traceDataProvider to set
	 */
	public void setDataProvider(IDataProvider traceDataProvider) {
		traceDataProvider.addDataProviderListener(this);
		// traceDataProvider.addDataProviderListener(xAxis);
		// traceDataProvider.addDataProviderListener(yAxis);
		this.traceDataProvider = traceDataProvider;
	}

	/**
	 * @return the traceType
	 */
	public TraceType getTraceType() {
		return traceType;
	}

	/**
	 * @param traceColor
	 *            Desired trace color
	 */
	public void setTraceColor(final Color traceColor) {
		Color old = this.traceColor;
		this.traceColor = traceColor;
		if (!errorBarColorSetFlag)
			errorBarColor = traceColor;
		if (xyGraph != null)
			xyGraph.repaint();
		fireTraceColorChanged(old, this.traceColor);
	}

	private void fireTraceColorChanged(Color old, Color newColor) {

		if (old == newColor)
			return;

		for (ITraceListener listener : listeners)
			listener.traceColorChanged(this, old, newColor);
	}

	/**
	 * @return the traceColor
	 */
	public Color getTraceColor() {
		return traceColor;
	}

	/**
	 * @param traceType
	 *            the traceType to set
	 */
	public void setTraceType(TraceType traceType) {
		TraceType old = this.traceType;
		this.traceType = traceType;
		if (xyGraph != null)
			xyGraph.repaint();

		fireTraceTypeChanged(old, this.traceType);
	}

	private void fireTraceTypeChanged(TraceType old, TraceType newTraceType) {

		if (old == newTraceType)
			return;

		for (ITraceListener listener : listeners)
			listener.traceTypeChanged(this, old, newTraceType);
	}

	/**
	 * @param baseLine
	 *            the baseLine to set
	 */
	public void setBaseLine(BaseLine baseLine) {
		this.baseLine = baseLine;
		if (xyGraph != null)
			xyGraph.repaint();
	}

	/**
	 * @param pointStyle
	 *            the pointStyle to set
	 */
	public void setPointStyle(PointStyle pointStyle) {
		this.pointStyle = pointStyle;
		if (xyGraph != null)
			xyGraph.repaint();
	}

	/**
	 * @param lineWidth
	 *            the lineWidth to set
	 */
	public void setLineWidth(int lineWidth) {
		this.lineWidth = lineWidth;
		if (xyGraph != null)
			xyGraph.repaint();
	}

	/**
	 * @param pointSize
	 *            the pointSize to set
	 */
	public void setPointSize(int pointSize) {
		this.pointSize = pointSize;
		if (xyGraph != null)
			xyGraph.repaint();
	}

	/**
	 * @param areaAlpha
	 *            the areaAlpha to set
	 */
	public void setAreaAlpha(int areaAlpha) {
		this.areaAlpha = areaAlpha;
		if (xyGraph != null)
			xyGraph.repaint();
	}

	/**
	 * @param antiAliasing
	 *            the antiAliasing to set
	 */
	public void setAntiAliasing(boolean antiAliasing) {
		this.antiAliasing = antiAliasing;
		if (xyGraph != null)
			xyGraph.repaint();
	}

	/**
	 * @param name
	 *            the name of the trace to set
	 */
	public void setName(String name) {
		String oldName = this.name;
		this.name = name;
		revalidate();
		if (xyGraph != null)
			xyGraph.repaint();

		fireTraceNameChanged(oldName, this.name);

	}

	private void fireTraceNameChanged(String oldName, String newName) {
		// TODO Auto-generated method stub
		if (((oldName == null) && (newName == null))
				|| ((oldName != null) && oldName.equals(newName)))
			return;

		for (ITraceListener listener : listeners)
			listener.traceNameChanged(this, oldName, newName);
	}

	/**
	 * @return the name of the trace
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the pointSize
	 */
	public int getPointSize() {
		return pointSize;
	}

	/**
	 * @return the areaAlpha
	 */
	public int getAreaAlpha() {
		return areaAlpha;
	}

	/**
	 * @return the yAxis
	 */
	public Axis getYAxis() {
		return yAxis;
	}

	@Override
	public String toString() {
		return name;
	}

	public void dataChanged(IDataProvider dataProvider) {
		// if the axis has been repainted, it will cause the trace to be
		// repainted autoly,
		// the trace doesn't have to be repainted again.
		boolean xRepainted = xAxis.performAutoScale(false);
		boolean yRepainted = yAxis.performAutoScale(false);
		if (!xRepainted && !yRepainted)
			repaint();
	}

	/**
	 * Get the corresponding sample index range based on the range of xAxis.
	 * This will help trace to draw only the part of data confined in xAxis. So
	 * it may also provides the first data out of the range to make the line
	 * could be drawn between inside data and outside data. <b>This method only
	 * works for chronological data, which means the data is naturally sorted on
	 * xAxis.</b>
	 * 
	 * @return the Range of the index.
	 */
	private Range getIndexRangeOnXAxis() {
		Range axisRange = xAxis.getRange();
		if (traceDataProvider.getSize() <= 0)
			return null;
		double min = axisRange.getLower() > axisRange.getUpper() ? axisRange
				.getUpper() : axisRange.getLower();
		double max = axisRange.getUpper() > axisRange.getLower() ? axisRange
				.getUpper() : axisRange.getLower();

		if (min > traceDataProvider.getSample(traceDataProvider.getSize() - 1)
				.getXValue()
				|| max < traceDataProvider.getSample(0).getXValue())
			return null;

		int lowIndex = 0;
		int highIndex = traceDataProvider.getSize() - 1;
		if (min > traceDataProvider.getSample(0).getXValue())
			lowIndex = nearBinarySearchX(min, true);
		if (max < traceDataProvider.getSample(highIndex).getXValue())
			highIndex = nearBinarySearchX(max, false);
		return new Range(lowIndex, highIndex);
	}

	// It will return the index on the closest left(if left is true) or right of
	// the data
	// Like public version, but without range checks.
	private int nearBinarySearchX(double key, boolean left) {
		int low = 0;
		int high = traceDataProvider.getSize() - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			double midVal = traceDataProvider.getSample(mid).getXValue();

			int cmp;
			if (midVal < key) {
				cmp = -1; // Neither val is NaN, thisVal is smaller
			} else if (midVal > key) {
				cmp = 1; // Neither val is NaN, thisVal is larger
			} else {
				long midBits = Double.doubleToLongBits(midVal);
				long keyBits = Double.doubleToLongBits(key);
				cmp = (midBits == keyBits ? 0 : // Values are equal
						(midBits < keyBits ? -1 : // (-0.0, 0.0) or (!NaN, NaN)
								1)); // (0.0, -0.0) or (NaN, !NaN)
			}

			if (cmp < 0) {
				if (mid < traceDataProvider.getSize() - 1
						&& key < traceDataProvider.getSample(mid + 1)
								.getXValue()) {
					if (left)
						return mid;
					else
						return mid + 1;
				}
				low = mid + 1;
			}

			else if (cmp > 0) {
				if (mid > 0
						&& key > traceDataProvider.getSample(mid - 1)
								.getXValue())
					if (left)
						return mid - 1;
					else
						return mid;
				high = mid - 1;
			}

			else
				return mid; // key found
		}
		return -(low + 1); // key not found.
	}

	public void axisRevalidated(Axis axis) {
		repaint();
	}

	public void axisRangeChanged(Axis axis, Range old_range, Range new_range) {
		// do nothing
	}

	/**
	 * @return the traceDataProvider
	 */
	public IDataProvider getDataProvider() {
		return traceDataProvider;
	}

	/**
	 * @param errorBarEnabled
	 *            the errorBarEnabled to set
	 */
	public void setErrorBarEnabled(boolean errorBarEnabled) {
		this.errorBarEnabled = errorBarEnabled;
	}

	/**
	 * @param errorBarType
	 *            the yErrorBarType to set
	 */
	public void setYErrorBarType(ErrorBarType errorBarType) {
		yErrorBarType = errorBarType;
	}

	/**
	 * @param errorBarType
	 *            the xErrorBarType to set
	 */
	public void setXErrorBarType(ErrorBarType errorBarType) {
		xErrorBarType = errorBarType;
	}

	/**
	 * @param drawYErrorInArea
	 *            the drawYErrorArea to set
	 */
	public void setDrawYErrorInArea(boolean drawYErrorInArea) {
		this.drawYErrorInArea = drawYErrorInArea;
	}

	/**
	 * @param errorBarCapWidth
	 *            the errorBarCapWidth to set
	 */
	public void setErrorBarCapWidth(int errorBarCapWidth) {
		this.errorBarCapWidth = errorBarCapWidth;
	}

	/**
	 * @param errorBarColor
	 *            Desired color for error bars, or <code>null</code> to use
	 *            trace color
	 */
	public void setErrorBarColor(final Color errorBarColor) {
		this.errorBarColor = errorBarColor;
		errorBarColorSetFlag = true;
	}

	/**
	 * Hot Sample is the sample on the trace which has been drawn in plot area.
	 * 
	 * @return the hotPointList
	 */
	public List<ISample> getHotSampleList() {
		return hotSampleist;
	}

	/**
	 * @return the baseLine
	 */
	public BaseLine getBaseLine() {
		return baseLine;
	}

	/**
	 * @return the pointStyle
	 */
	public PointStyle getPointStyle() {
		return pointStyle;
	}

	/**
	 * @return the lineWidth
	 */
	public int getLineWidth() {
		return lineWidth;
	}

	/**
	 * @return the antiAliasing
	 */
	public boolean isAntiAliasing() {
		return antiAliasing;
	}

	/**
	 * @return the errorBarEnabled
	 */
	public boolean isErrorBarEnabled() {
		return errorBarEnabled;
	}

	/**
	 * @return the yErrorBarType
	 */
	public ErrorBarType getYErrorBarType() {
		return yErrorBarType;
	}

	/**
	 * @return the xErrorBarType
	 */
	public ErrorBarType getXErrorBarType() {
		return xErrorBarType;
	}

	/**
	 * @return the errorBarCapWidth
	 */
	public int getErrorBarCapWidth() {
		return errorBarCapWidth;
	}

	/** @return Color used for error bars or 'area' */
	public Color getErrorBarColor() {
		return errorBarColor;
	}

	/**
	 * @return the drawYErrorInArea
	 */
	public boolean isDrawYErrorInArea() {
		return drawYErrorInArea;
	}

	/**
	 * @param xyGraph
	 *            the xyGraph to set
	 */
	public void setXYGraph(XYGraph xyGraph) {
		this.xyGraph = xyGraph;
	}

	/**
	 * @return the xyGraph
	 */
	public XYGraph getXYGraph() {
		return xyGraph;
	}

	public void axisForegroundColorChanged(Axis axis, Color oldColor,
			Color newColor) {
		// TODO Auto-generated method stub

	}

	public void axisTitleChanged(Axis axis, String oldTitle, String newTitle) {
		// TODO Auto-generated method stub

	}

	public void axisAutoScaleChanged(Axis axis, boolean oldAutoScale,
			boolean newAutoScale) {
		// TODO Auto-generated method stub

	}

	public void axisLogScaleChanged(Axis axis, boolean old, boolean logScale) {
		// TODO Auto-generated method stub
		
	}

}
