/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.xygraph.figures;

import java.util.ArrayList;
import java.util.List;

import org.csstudio.swt.xygraph.dataprovider.IDataProvider;
import org.csstudio.swt.xygraph.linearscale.LinearScale;
import org.csstudio.swt.xygraph.linearscale.Range;
import org.csstudio.swt.xygraph.undo.AxisPanOrZoomCommand;
import org.csstudio.swt.xygraph.undo.SaveStateCommand;
import org.csstudio.swt.xygraph.undo.ZoomType;
import org.csstudio.swt.xygraph.util.GraphicsUtil;
import org.csstudio.swt.xygraph.util.Log10;
import org.csstudio.swt.xygraph.util.SWTConstants;
import org.csstudio.swt.xygraph.util.XYGraphMediaFactory;
import org.csstudio.swt.xygraph.util.XYGraphMediaFactory.CURSOR_TYPE;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.MouseMotionListener;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * The axis figure.
 *
 * @author Xihui Chen
 * @author Kay Kasemir - Axis zoom/pan tweaks
 */
public class Axis extends LinearScale{
    /** The ratio of the shrink/expand area for one zoom. */
    final static double ZOOM_RATIO = 0.1;

    /** The auto zoom interval in ms.*/
    final static int ZOOM_SPEED = 200;

//	private static final Color GRAY_COLOR = XYGraphMediaFactory.getInstance().getColor(
//			XYGraphMediaFactory.COLOR_GRAY);

    
    
    private String title;

    @Override
	public void setFont(Font font) {
		// TODO Auto-generated method stub
		super.setFont(font);
		this.scaleFontData = getFont().getFontData()[0];
	}



	final private List<Trace> traceList = new ArrayList<Trace>();

	private XYGraph xyGraph;
	private Grid grid;

	private Font titleFont;
	//title FontData : Add because of SWT illegal thread access
		private FontData titleFontData;
	
		//title FontData : Add because of SWT illegal thread access
			private FontData scaleFontData;

	private boolean autoScale = false;

	private boolean showMajorGrid = false;

	private boolean showMinorGrid = false;

	private Color majorGridColor;

	private Color minorGridColor;

	private boolean dashGridLine = true;

	private double autoScaleThreshold = 0.01;

	final private List<IAxisListener> listeners = new ArrayList<IAxisListener>();

	private ZoomType zoomType = ZoomType.NONE;

	private Point start;
	private Point end;
	private boolean armed;
	private Range startRange;
	private Cursor grabbing;
	private Color revertBackColor;

	private RGB colorRGB;
	private RGB majorGridColorRGB;

	

	public FontData getTitleFontData() {
		return titleFontData;
	}

	public FontData getScaleFontData() {
		return scaleFontData;
	}

	/**Constructor
	 * @param title title of the axis
	 * @param yAxis true if this is the Y-Axis, false if this is the X-Axis.
	 */
	public Axis(final String title, final boolean yAxis) {
		super();
		this.title = title;
		if(yAxis)
			setOrientation(Orientation.VERTICAL);
		//Save drawing line operation in RAP.
		if(GraphicsUtil.isRAP())
			setMinorTicksVisible(false);

		final AxisMouseListener panner = new AxisMouseListener();
		addMouseListener(panner);
		addMouseMotionListener(panner);
		grabbing = XYGraphMediaFactory.getCursor(CURSOR_TYPE.GRABBING);
		Font sysFont = Display.getCurrent().getSystemFont();
		titleFont = XYGraphMediaFactory.getInstance().getFont(
				new FontData(sysFont.getFontData()[0].getName(), 12, SWT.BOLD)); //$NON-NLS-1$
		if(getBackgroundColor() != null){
			RGB backRGB = getBackgroundColor().getRGB();
			revertBackColor = XYGraphMediaFactory.getInstance().getColor(255- backRGB.red,
					255 - backRGB.green, 255 - backRGB.blue);
		}else
			revertBackColor = XYGraphMediaFactory.getInstance().getColor(100,100,100);

	}

	public void addListener(final IAxisListener listener){
		if(listeners.contains(listener))
			return;
		listeners.add(listener);
	}

	public boolean removeListenr(final IAxisListener listener){
		return listeners.remove(listener);
	}

	private void fireRevalidated(){
		for(IAxisListener listener : listeners)
			listener.axisRevalidated(this);
	}

	private void fireAxisRangeChanged(final Range old_range, final Range new_range){
		for(IAxisListener listener : listeners)
			listener.axisRangeChanged(this, old_range, new_range);
	}

	@Override
	public void setRange(final double lower, final double upper) {
		if (Orientation.VERTICAL == getOrientation() && autoScale == false)
			return; //scouter.project 20150910
		Range old_range = getRange();
		super.setRange(lower, upper);
		fireAxisRangeChanged(old_range, getRange());
	}
	public void setRangeDirect(final double lower, final double upper) {
		Range old_range = getRange();
		super.setRange(lower, upper);
		fireAxisRangeChanged(old_range, getRange());
	}

	@Override
	protected void layout() {
		super.layout();
		fireRevalidated();
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		revalidate();
	}
	@Override
	public void setForegroundColor(final Color color) {	
		Color oldColor = getForegroundColor();
		super.setForegroundColor(color);
		colorRGB = color.getRGB();
		if(xyGraph != null)
			xyGraph.repaint();
		fireAxisForegroundColorChanged(oldColor, color);
	}
	
	@Override
	public void setMinorTicksVisible(boolean minorTicksVisible) {
		//Save line operation in RAP.
		if(GraphicsUtil.isRAP())
			super.setMinorTicksVisible(false);
		else
			super.setMinorTicksVisible(minorTicksVisible);
	}
	
	
	public RGB getForegroundColorRGB(){
		return colorRGB;
	}
	
	private void fireAxisForegroundColorChanged(Color oldColor,
			Color newColor) {
		for(IAxisListener listener : listeners)
			listener.axisForegroundColorChanged(this, oldColor, newColor);	
	}
	
	@Override
	public void setBackgroundColor(Color bg) {
		RGB backRGB = bg.getRGB();
		revertBackColor = XYGraphMediaFactory.getInstance().getColor(255- backRGB.red,
				255 - backRGB.green, 255 - backRGB.blue);
		super.setBackgroundColor(bg);
	}

	/**
	 * Edited by scouter.project@gmail.com
	 * Add case that no title axis
	 */
	@Override
	public Dimension getPreferredSize(final int wHint, final int hHint) {
		final Dimension d = super.getPreferredSize(wHint, hHint);
		if (isVisible()) {
			if (title.length() > 0) {
				if (isHorizontal())
					d.height += FigureUtilities.getTextExtents(title, titleFont).height;
				else
					d.width += FigureUtilities.getTextExtents(title, titleFont).height;
			} else {
				if (isHorizontal())
					d.height += 2;
				else
					d.width += 5;
			}
		} else { // Not visible, flatten it to use zero height resp. width
			if (isHorizontal())
				d.height = 0;
			else
				d.width = 0;
		}
		return d;
	}

	@Override
	protected void paintClientArea(final Graphics graphics) {
		// Don't do anything when hidden
		if (!isVisible())
			return;

		super.paintClientArea(graphics);

//		graphics.pushState();
		graphics.setFont(titleFont);
		final Dimension titleSize = FigureUtilities.getTextExtents(title, titleFont);
		if(isHorizontal()){
			if(getTickLablesSide() == LabelSide.Primary)
				graphics.drawText(title,
						bounds.x + bounds.width/2 - titleSize.width/2,
						bounds.y + bounds.height - titleSize.height);
			else
				graphics.drawText(title,
						bounds.x + bounds.width/2 - titleSize.width/2,
						bounds.y);
		}else{
		    final int w = titleSize.height;
		    final int h = titleSize.width +1;

			if(getTickLablesSide() == LabelSide.Primary){
				GraphicsUtil.drawVerticalText(graphics, title,
							bounds.x, bounds.y + bounds.height/2 - h/2, false);
			}else {
				GraphicsUtil.drawVerticalText(graphics, title,
								bounds.x + bounds.width - w, bounds.y + bounds.height/2 - h/2, true);
			}
		}

//		graphics.popState();

		// Show the start/end cursor or the 'rubberband' of a zoom operation?
		if(armed && end != null && start != null){
			switch (zoomType) {
			case RUBBERBAND_ZOOM:
			case HORIZONTAL_ZOOM:
			case VERTICAL_ZOOM:
				graphics.setLineStyle(SWTConstants.LINE_DOT);
				graphics.setLineWidth(1);
				graphics.setForegroundColor(revertBackColor);
				graphics.drawRectangle(start.x, start.y, end.x - start.x-1, end.y - start.y-1);
				break;

			default:
				break;
			}
		}
	}

	/** @return Range that reflects the minimum and maximum value of all
	 *          traces on this axis.
	 *          Returns <code>null</code> if there is no trace data.
	 */
    public Range getTraceDataRange()
    {
        double low = Double.POSITIVE_INFINITY;
        double high = Double.NEGATIVE_INFINITY;
        for (Trace trace : traceList)
        {
            if (trace.getDataProvider() == null || !trace.isVisible())
                continue;
            final Range range;
            if (isHorizontal())
                range = trace.getDataProvider().getXDataMinMax();
            else
                range = trace.getDataProvider().getYDataMinMax();
            if (range == null)
                continue;
            if (Double.isInfinite(range.getLower())
                    || Double.isInfinite(range.getUpper())
                    || Double.isNaN(range.getLower())
                    || Double.isNaN(range.getUpper()))
                continue;
            if (low > range.getLower())
                low = range.getLower();
            if (high < range.getUpper())
                high = range.getUpper();
        }
        if (Double.isInfinite(low) || Double.isInfinite(high))
            return null;
        return new Range(low, high);
    }

	/** Perform an auto-scale:
	 *  Axis limits are set to the value range of the traces on this axis.
	 *  Includes some optimization:
	 *  Axis range is set a little wider than exact trace data range.
	 *  When auto-scale would only perform a minor axis adjustment,
	 *  axis is left unchanged.
	 *
	 *  @param force If true, the axis will be auto-scaled by force regardless the autoScale field.
	 *  Otherwise, it will use the autoScale field to judge whether an auto-scale will be performed.
	 *  @return true if the axis is repainted due to range change.
	 *
	 *  @see #autoScaleThreshold
	 */
	public boolean performAutoScale(final boolean force){
	    // Anything to do? Autoscale not enabled nor forced?
		if (traceList.size() <= 0  ||  !(force || autoScale))
		    return false;
		if(!force && xyGraph.getZoomType() != ZoomType.NONE)
			return false;

	    // Get range of data in all traces
        final Range range = getTraceDataRange();
        if (range == null)
            return false;
		double tempMin = range.getLower();
		double tempMax = range.getUpper();

		// Get current axis range, determine how 'different' they are
		double max = getRange().getUpper();
		double min = getRange().getLower();

		if (isLogScaleEnabled())
		{   // Transition into log space
		    tempMin = Log10.log10(tempMin);
		    tempMax = Log10.log10(tempMax);
		    max = Log10.log10(max);
		    min = Log10.log10(min);
		}
//Give extra space in plot area
//		double extraSpace = (tempMax-tempMin)*0.05;
//		double tempMin2 = tempMin -extraSpace;
//		double tempMax2 = tempMax + extraSpace;
		
		final double thr = (max - min)*autoScaleThreshold;

		//if both the changes are lower than threshold, return
		if(((tempMin - min)>=0 && (tempMin - min)<thr)
				&& ((max - tempMax)>=0 && (max - tempMax)<thr)){
			return false;
		}else { //expand more space than needed
			if((tempMin - min)<0)
				tempMin -= thr;
			if((tempMax - max) > 0)
				tempMax += thr;
		}
	
//		if(tempMin>tempMin2)
//			tempMin=tempMin2;
//		if(tempMax<tempMax2)
//			tempMax = tempMax2;
		// Any change at all?
		if((Double.doubleToLongBits(tempMin) == Double.doubleToLongBits(min)
				&& Double.doubleToLongBits(tempMax) == Double.doubleToLongBits(max)) ||
				Double.isInfinite(tempMin) || Double.isInfinite(tempMax) ||
				Double.isNaN(tempMin) || Double.isNaN(tempMax))
			return false;

        if (isLogScaleEnabled())
        {   // Revert from log space
            tempMin = Log10.pow10(tempMin);
            tempMax = Log10.pow10(tempMax);
        }

		// Update axis
		setRange(tempMin, tempMax, true);
		repaint();
		return true;
	}

	/**Add a trace to the axis.
	 * @param trace the trace to be added.
	 */
	public void addTrace(final Trace trace){
		if(traceList.contains(trace))
			return;
		traceList.add(trace);
		performAutoScale(false);
	}

	/**Remove a trace from the axis.
	 * @param trace
	 * @return true if this axis contained the specified trace
	 */
	public boolean removeTrace(final Trace trace){
	    final boolean r = traceList.remove(trace);
		performAutoScale(false);
		return r;
	}


	/**
	 * @param title the title to set
	 */
	public void setTitle(final String title) {
		
		String oldTitle = this.title;
		this.title = title;
		if(xyGraph != null)
			xyGraph.repaint();
		fireAxisTitleChanged(oldTitle, title);
	}
	
	private void fireAxisTitleChanged(String oldTitle, String newTitle) {
		for(IAxisListener listener : listeners)
			listener.axisTitleChanged(this, oldTitle, newTitle);
	}


	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @return the autoScale
	 */
	public boolean isAutoScale() {
		return autoScale;
	}

	/**
	 * @param autoScale the autoScale to set
	 */
	public void setAutoScale(final boolean autoScale) {
	
		boolean oldAutoScale = this.autoScale;
		this.autoScale = autoScale;
		performAutoScale(false);
		fireAxisAutoScaleChanged(oldAutoScale, this.autoScale);
	}

	private void fireAxisAutoScaleChanged(boolean oldAutoScale,
			boolean newAutoScale) {
		for(IAxisListener listener : listeners)
			listener.axisAutoScaleChanged(this, oldAutoScale, newAutoScale);
		
	}

	/**
	 * @return the showMajorGrid
	 */
	public boolean isShowMajorGrid() {
		return showMajorGrid;
	}

	/**
	 * @param showMajorGrid the showMajorGrid to set
	 */
	public void setShowMajorGrid(final boolean showMajorGrid) {
		this.showMajorGrid = showMajorGrid;
		if(xyGraph != null)
			xyGraph.repaint();
	}

	/**
	 * @return the showMinorGrid
	 */
	public boolean isShowMinorGrid() {
		return showMinorGrid;
	}

	/**
	 * @param showMinorGrid the showMinorGrid to set
	 */
	public void setShowMinorGrid(final boolean showMinorGrid) {
		this.showMinorGrid = showMinorGrid;
		if(xyGraph != null)
			xyGraph.repaint();
	}

	/**
	 * @return the majorGridColor
	 */
	public Color getMajorGridColor() {
		if(majorGridColor == null)
			majorGridColor = XYGraphMediaFactory.getInstance().getColor
			(XYGraphMediaFactory.COLOR_GRAY);
		return majorGridColor;
	}

	/**
	 * @param majorGridColor the majorGridColor to set
	 */
	public void setMajorGridColor(final Color majorGridColor) {
		this.majorGridColor = majorGridColor;
		this.majorGridColorRGB = majorGridColor.getRGB();
		if(xyGraph != null)
			xyGraph.repaint();
	}

	
	
	public RGB getMajorGridColorRGB() {
		return majorGridColorRGB;
	}

	/**
	 * @return the minorGridColor
	 */
	public Color getMinorGridColor() {
		if(minorGridColor == null)
			minorGridColor = XYGraphMediaFactory.getInstance().getColor
			(XYGraphMediaFactory.COLOR_GRAY);
		return minorGridColor;
	}

	/**
	 * @param minorGridColor the minorGridColor to set
	 */
	public void setMinorGridColor(final Color minorGridColor) {
		this.minorGridColor = minorGridColor;
		if(xyGraph != null)
			xyGraph.repaint();
	}

	/**
	 * @param titleFont the titleFont to set
	 */
	public void setTitleFont(final Font titleFont) {
		this.titleFont = titleFont;
		this.titleFontData = titleFont.getFontData()[0];
		repaint();
	}


	/**
	 * @return the dashGridLine
	 */
	public boolean isDashGridLine() {
		return dashGridLine;
	}

	/**
	 * @param dashGridLine the dashGridLine to set
	 */
	public void setDashGridLine(final boolean dashGridLine) {
		this.dashGridLine = dashGridLine;
		if(xyGraph != null)
			xyGraph.repaint();
	}

	/**
	 * @param xyGraph the xyGraph to set
	 */
	public void setXyGraph(final XYGraph xyGraph) {
		this.xyGraph = xyGraph;
	}
	@Override
	public String toString() {
		return title;
	}

	public void dataChanged(final IDataProvider dataProvider) {
		if(autoScale)
			performAutoScale(false);
	}

	/**The autoScaleThreshold must be a value in range [0,1], which represents a percentage
	 * of the plot area for the threshold when autoScale is performed.The autoScale will performed
	 * only if the spare space exceeds this threshold. So it can reduce the CPU usage by
	 * increasing the threshold.
	 * @param autoScaleThreshold the autoScaleThreshold to set
	 */
	public void setAutoScaleThreshold(final double autoScaleThreshold) {
		if(autoScaleThreshold > 1 || autoScaleThreshold <0)
			throw new RuntimeException("The autoScaleThreshold must be a value in range [0,1]!"); //$NON-NLS-1$
		this.autoScaleThreshold = autoScaleThreshold;
	}

	/** @param zoom Zoom Type
	 *  @return <code>true</code> if the zoom type is applicable to this axis
 	 */
	private boolean isValidZoomType(final ZoomType zoom)
	{
	    return zoom == ZoomType.PANNING   ||
	           zoom == ZoomType.RUBBERBAND_ZOOM ||
               zoom == ZoomType.ZOOM_IN   ||
               zoom == ZoomType.ZOOM_OUT  ||
               (isHorizontal() &&
                (zoom == ZoomType.HORIZONTAL_ZOOM ||
                 zoom == ZoomType.ZOOM_IN_HORIZONTALLY ||
                 zoom == ZoomType.ZOOM_OUT_HORIZONTALLY)
               ) ||
               (!isHorizontal() &&
                (zoom == ZoomType.VERTICAL_ZOOM ||
                 zoom == ZoomType.ZOOM_OUT_VERTICALLY ||
                 zoom == ZoomType.ZOOM_IN_VERTICALLY)
               );
	}

	/**
	 * @param zoomType the zoomType to set
	 */
	public void setZoomType(final ZoomType zoomType)
	{
		this.zoomType = zoomType;
		// Set zoom's cursor if axis allows that type of zoom
		if (isValidZoomType(zoomType))
			setCursor(zoomType.getCursor());
		else
			setCursor(ZoomType.NONE.getCursor());
	}

	/**
	 * @return the titleFont
	 */
	public Font getTitleFont() {
		return titleFont;
	}

	/**
	 * @return the autoScaleThreshold
	 */
	public double getAutoScaleThreshold() {
		return autoScaleThreshold;
	}

	/**Set this axis as Y-Axis or X-Axis.
	 * @param isYAxis set true if the axis is Y-Axis; false if it is X-Axis.
	 */
	public void setYAxis(boolean isYAxis){
		if(xyGraph != null)
			xyGraph.removeAxis(this);
		setOrientation(isYAxis ? Orientation.VERTICAL : Orientation.HORIZONTAL);
		if(xyGraph != null)
			xyGraph.addAxis(this);
	}

	/**Set the axis on primary side (Bottom/Left) or secondary side (Top/Right).
	 * @param onPrimarySide set true if the axis on primary side(Bottom/Left);
	 * false if it is not on the primary side of xy graph(Top/Right).
	 */
	public void setPrimarySide(boolean onPrimarySide){
		setTickLableSide(onPrimarySide ? LabelSide.Primary : LabelSide.Secondary);
	}

	/**
	 * @return true if the axis is Y-Axis; false if it is X-Axis;
	 */
	public boolean isYAxis(){
		return !isHorizontal();
	}

	/**
	 * @return true if the axis is on the primary side of xy graph(Bottom/Left);
	 * false if it is on the secondary side(Top/Right).
	 */
	public boolean isOnPrimarySide(){
		return getTickLablesSide() == LabelSide.Primary;
	}

	/** Pan axis according to start/end from mouse listener */
	private void pan()
	{
		if(isHorizontal())
		    pan(startRange,
		        getPositionValue(start.x, false), getPositionValue(end.x, false));
		else
            pan(startRange,
                getPositionValue(start.y, false), getPositionValue(end.y, false));
	}

	/** Pan the axis
	 *  @param temp Original axis range before the panning started
	 *  @param t1 Start of the panning move
	 *  @param t2 End of the panning move
	 */
	void pan(final Range temp, double t1, double t2)
    {
        if (isLogScaleEnabled())
        {
            final double m = Math.log10(t2) - Math.log10(t1);
            t1 = Math.pow(10,Math.log10(temp.getLower()) - m);
            t2 = Math.pow(10,Math.log10(temp.getUpper()) - m);
        }
        else
        {
            final double m = t2-t1;
            t1 = temp.getLower() - m;
            t2 = temp.getUpper() - m;
        }
        setRange(t1, t2);
    }

    /** Zoom axis
	 *  @param center Axis position at the 'center' of the zoom
	 *  @param factor Zoom factor. Positive to zoom 'in', negative 'out'.
	 */
	void zoomInOut(final double center, final double factor)
    {
	    final double t1, t2;
	    if (isLogScaleEnabled())
	    {
	        final double l = Math.log10(getRange().getUpper()) -
                    Math.log10(getRange().getLower());
	        final double r1 = (Math.log10(center) - Math.log10(getRange().getLower()))/l;
	        final double r2 = (Math.log10(getRange().getUpper()) - Math.log10(center))/l;
            t1 = Math.pow(10, Math.log10(getRange().getLower()) + r1 * factor * l);
            t2 = Math.pow(10, Math.log10(getRange().getUpper()) - r2 * factor * l);
        }else{
            final double l = getRange().getUpper() - getRange().getLower();
            final double r1 = (center - getRange().getLower())/l;
            final double r2 = (getRange().getUpper() - center)/l;
            t1 = getRange().getLower() + r1 * factor * l;
            t2 = getRange().getUpper() - r2 * factor * l;
        }
        setRange(t1, t2, true);
    }

    /**
	 * @param grid the grid to set
	 */
	public void setGrid(Grid grid) {
		this.grid = grid;
	}

	/**
	 * @return the grid
	 */
	public Grid getGrid() {
		return grid;
	}



	@Override
	public void setLogScale(boolean enabled) throws IllegalStateException {
		// TODO Auto-generated method stub
		boolean old = isLogScaleEnabled();
		super.setLogScale(enabled);
		fireAxisLogScaleChanged(old, logScaleEnabled);
	}
	
	private void fireAxisLogScaleChanged(boolean old, boolean logScale) {
		
		if(old == logScale)
			return;
		
		for(IAxisListener listener : listeners)
			listener.axisLogScaleChanged(this, old, logScale);
	}



	/** Listener to mouse events, performs panning and some zooms
	 *  Is very similar to the PlotMouseListener, but unclear
	 *  how easy/useful it would be to base them on the same code.
	 */
	class AxisMouseListener extends MouseMotionListener.Stub implements MouseListener
	{
		private SaveStateCommand command;

        public void mousePressed(final MouseEvent me)
        {
            // Only react to 'main' mouse button, only react to 'real' zoom
            if (me.button != 1  ||  !isValidZoomType(zoomType))
                return;
            armed = true;
            // get start position
            switch (zoomType)
            {
            case RUBBERBAND_ZOOM:
            	if(isHorizontal())
            		start = new Point(me.getLocation().x, bounds.y);
            	else
            		start = new Point(bounds.x, me.getLocation().y);
                end = null;
                break;
            case HORIZONTAL_ZOOM:
                start = new Point(me.getLocation().x, bounds.y);
                end = null;
                break;
            case VERTICAL_ZOOM:
                start = new Point(bounds.x, me.getLocation().y);
                end = null;
                break;
            case PANNING:
                setCursor(grabbing);
                start = me.getLocation();
                end = null;
                startRange = getRange();
                break;
            case ZOOM_IN:
            case ZOOM_IN_HORIZONTALLY:
            case ZOOM_IN_VERTICALLY:
            case ZOOM_OUT:
            case ZOOM_OUT_HORIZONTALLY:
            case ZOOM_OUT_VERTICALLY:
                start = me.getLocation();
                end = new Point();
                // Start timer that will zoom while mouse button is pressed
                Display.getCurrent().timerExec(ZOOM_SPEED, new Runnable()
                {
                    public void run()
                    {
                        if (!armed)
                            return;
                        performInOutZoom();
                        Display.getCurrent().timerExec(ZOOM_SPEED, this);
                    }
                });
                break;
            default:
                break;
        	}

            //add command for undo operation
            command = new AxisPanOrZoomCommand(zoomType.getDescription(), Axis.this);
            me.consume();
        }

        public void mouseDoubleClicked(final MouseEvent me) { /* Ignored */ }

        @Override
		public void mouseDragged(final MouseEvent me)
		{
			if (! armed)
				return;
			switch (zoomType)
			{
            case RUBBERBAND_ZOOM:
                // Treat rubberband zoom on axis like horiz/vert. zoom
                if (isHorizontal())
                    end = new Point(me.getLocation().x, bounds.y + bounds.height);
                else
                    end = new Point(bounds.x + bounds.width, me.getLocation().y);
                break;
			case HORIZONTAL_ZOOM:
                end = new Point(me.getLocation().x, bounds.y + bounds.height);
                break;
            case VERTICAL_ZOOM:
                end = new Point(bounds.x + bounds.width, me.getLocation().y);
                break;
            case PANNING:
                end = me.getLocation();
                pan();
                break;
            default:
                break;
			}
			Axis.this.repaint();
		}

        @Override
		public void mouseExited(final MouseEvent me)
		{
            // Treat like releasing the button to stop zoomIn/Out timer
		    switch (zoomType)
            {
            case ZOOM_IN:
            case ZOOM_IN_HORIZONTALLY:
            case ZOOM_IN_VERTICALLY:
            case ZOOM_OUT:
            case ZOOM_OUT_HORIZONTALLY:
            case ZOOM_OUT_VERTICALLY:
                mouseReleased(me);
            default:
            }
		}

        public void mouseReleased(final MouseEvent me)
		{
		    if (! armed)
		        return;
            armed = false;
            if (zoomType == ZoomType.PANNING)
                setCursor(zoomType.getCursor());
			if (end == null || start == null || command == null)
				return;

			switch (zoomType)
			{
            case RUBBERBAND_ZOOM:
            case HORIZONTAL_ZOOM:
            case VERTICAL_ZOOM:
                performStartEndZoom();
                break;
            case PANNING:
                pan();
                break;
            case ZOOM_IN:
            case ZOOM_IN_HORIZONTALLY:
            case ZOOM_IN_VERTICALLY:
            case ZOOM_OUT:
            case ZOOM_OUT_HORIZONTALLY:
            case ZOOM_OUT_VERTICALLY:
                performInOutZoom();
                break;
            default:
                break;
			}
			command.saveState();
			xyGraph.getOperationsManager().addCommand(command);
			command = null;
            start = null;
            end = null;
		}

		/** Perform the zoom to mouse start/end */
        private void performStartEndZoom()
        {
            final double t1 = getPositionValue(isHorizontal() ? start.x : start.y, false);
            final double t2 = getPositionValue(isHorizontal() ? end.x   : end.y,   false);
            setRange(t1, t2, true);
        }

		/** Perform the in or out zoom according to zoomType */
        private void performInOutZoom()
        {
            final int pixel_pos = isHorizontal() ? start.x : start.y;
            final double center = getPositionValue(pixel_pos, false);
            switch (zoomType)
            {
            case ZOOM_IN:              zoomInOut(center, ZOOM_RATIO); break;
            case ZOOM_IN_HORIZONTALLY: zoomInOut(center, ZOOM_RATIO); break;
            case ZOOM_IN_VERTICALLY:   zoomInOut(center, ZOOM_RATIO); break;
            case ZOOM_OUT:             zoomInOut(center, -ZOOM_RATIO); break;
            case ZOOM_OUT_HORIZONTALLY:zoomInOut(center, -ZOOM_RATIO); break;
            case ZOOM_OUT_VERTICALLY:  zoomInOut(center, -ZOOM_RATIO); break;
            default:                   // NOP
            }
        }
	}
}
