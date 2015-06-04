/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.xygraph.figures;


import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.csstudio.swt.xygraph.linearscale.AbstractScale.LabelSide;
import org.csstudio.swt.xygraph.linearscale.LinearScale.Orientation;
import org.csstudio.swt.xygraph.linearscale.Range;
import org.csstudio.swt.xygraph.undo.OperationsManager;
import org.csstudio.swt.xygraph.undo.XYGraphMemento;
import org.csstudio.swt.xygraph.undo.ZoomCommand;
import org.csstudio.swt.xygraph.undo.ZoomType;
import org.csstudio.swt.xygraph.util.Log10;
import org.csstudio.swt.xygraph.util.SingleSourceHelper;
import org.csstudio.swt.xygraph.util.XYGraphMediaFactory;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * XY-Graph Figure.
 * @author Xihui Chen
 * @author Kay Kasemir (performStagger)
 * @author Laurent PHILIPPE (property change support)
 */
public class XYGraph extends Figure{
	
	/**
	 * Add property change support to XYGraph
	 * Use for inform listener of xyGraphMem property changed
	 * @author L.PHILIPPE (GANIL)
	 */
	private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

	
	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		changeSupport.addPropertyChangeListener(listener);
	}

	@Override
	public void addPropertyChangeListener(String property,
			PropertyChangeListener listener) {
		changeSupport.addPropertyChangeListener(property, listener);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		changeSupport.removePropertyChangeListener(listener);
	}

	@Override
	public void removePropertyChangeListener(String property,
			PropertyChangeListener listener) {
		changeSupport.removePropertyChangeListener(property, listener);
	}
	
	public void fireConfigChanged() {
		changeSupport.firePropertyChange("config", null, this);
	}


	/**
	 * Save the Graph settings
	 * Send a property changed event when changed
	 * @author L.PHILIPPE (GANIL)
	 */
	private XYGraphMemento xyGraphMem;
	
	public XYGraphMemento getXyGraphMem() {
		return xyGraphMem;
	}

	public void setXyGraphMem(XYGraphMemento xyGraphMem) {
		XYGraphMemento old = this.xyGraphMem;
		this.xyGraphMem = xyGraphMem;
		changeSupport.firePropertyChange("xyGraphMem", old, this.xyGraphMem);
	
		System.out.println("**** XYGraph.setXyGraphMem() ****");
	}

	private static final int GAP = 2;
//	public final static Color WHITE_COLOR = ColorConstants.white;
//	public final static Color BLACK_COLOR = ColorConstants.black;

    /** Default colors for newly added item, used over when reaching the end.
     *  <p>
     *  Very hard to find a long list of distinct colors.
     *  This list is definitely too short...
     */
    final public static RGB[] DEFAULT_TRACES_COLOR =
    {
        new RGB( 21,  21, 196), // blue
        new RGB(242,  26,  26), // red
        new RGB( 33, 179,  33), // green
        new RGB(  0,   0,   0), // black
        new RGB(128,   0, 255), // violett
        new RGB(255, 170,   0), // (darkish) yellow
        new RGB(255,   0, 240), // pink
        new RGB(243, 132, 132), // peachy
        new RGB(  0, 255,  11), // neon green
        new RGB(  0, 214, 255), // neon blue
        new RGB(114,  40,   3), // brown
        new RGB(219, 128,   4), // orange
    };

	private int traceNum = 0;
	private boolean transparent = false;
	private boolean showLegend = true;

	private Map<Axis, Legend> legendMap;

	/** Graph title. Should never be <code>null</code> because
	 *  otherwise the ToolbarArmedXYGraph's GraphConfigPage
	 *  can crash.
	 */
	private String title = "";



	private Color titleColor;

	private Label titleLabel;

	//ADD BECAUSE OF SWT invalid Thread acess on getTitleColor()
	private FontData titleFontData;
	private RGB titleColorRgb; 
	
	private List<Axis> xAxisList;
	private List<Axis> yAxisList;
	private PlotArea plotArea;

	// TODO Clients can set these to null. Should these be 'final'? Or provider getter?
	public Axis primaryXAxis;
	public Axis primaryYAxis;

	private OperationsManager operationsManager;

	private ZoomType zoomType = ZoomType.NONE;


	/**
	 * Constructor.
	 */
	public XYGraph() {
		setOpaque(!transparent);
		legendMap = new LinkedHashMap<Axis, Legend>();
		titleLabel = new Label();
		String sysFontName = 
				Display.getCurrent().getSystemFont().getFontData()[0].getName();
		setTitleFont(XYGraphMediaFactory.getInstance().getFont(
				new FontData(sysFontName, 12, SWT.BOLD)));
		//titleLabel.setVisible(false);
		xAxisList = new ArrayList<Axis>();
		yAxisList = new ArrayList<Axis>();
		plotArea = new PlotArea(this);
		getPlotArea().setOpaque(!transparent);

		add(titleLabel);
		add(plotArea);
		primaryYAxis = new Axis("Y-Axis", true);
		primaryYAxis.setOrientation(Orientation.VERTICAL);
		primaryYAxis.setTickLableSide(LabelSide.Primary);
		primaryYAxis.setAutoScaleThreshold(0.1);
		addAxis(primaryYAxis);

		primaryXAxis = new Axis("X-Axis", false);
		primaryXAxis.setOrientation(Orientation.HORIZONTAL);
		primaryXAxis.setTickLableSide(LabelSide.Primary);
		addAxis(primaryXAxis);

		operationsManager = new OperationsManager();
	}

	@Override
	public boolean isOpaque() {
		return false;
	}

	@Override
	protected void layout() {
		Rectangle clientArea = getClientArea().getCopy();
		boolean hasRightYAxis = false;
		boolean hasTopXAxis = false;
		boolean hasLeftYAxis = false;
		boolean hasBottomXAxis = false;
		if(titleLabel != null && titleLabel.isVisible() && !(titleLabel.getText().length() <= 0)){
			Dimension titleSize = titleLabel.getPreferredSize();
			titleLabel.setBounds(new Rectangle(clientArea.x + clientArea.width/2 - titleSize.width/2,
					clientArea.y, titleSize.width, titleSize.height));
			clientArea.y += titleSize.height + GAP;
			clientArea.height -= titleSize.height + GAP;
		}
		if(showLegend){
			List<Integer> rowHPosList = new ArrayList<Integer>();
			List<Dimension> legendSizeList = new ArrayList<Dimension>();
			List<Integer> rowLegendNumList = new ArrayList<Integer>();
			List<Legend> legendList = new ArrayList<Legend>();
			Object[] yAxes = legendMap.keySet().toArray();
			int hPos = 0;
			int rowLegendNum = 0;
			for(int i = 0; i< yAxes.length; i++){
				Legend legend = legendMap.get(yAxes[i]);
				if(legend != null && legend.isVisible()){
					legendList.add(legend);
					Dimension legendSize = legend.getPreferredSize(clientArea.width, clientArea.height);
					legendSizeList.add(legendSize);
					if((hPos+legendSize.width + GAP) > clientArea.width){
						if(rowLegendNum ==0)
							break;
						rowHPosList.add(clientArea.x + (clientArea.width-hPos)/2);
						rowLegendNumList.add(rowLegendNum);
						rowLegendNum = 1;
						hPos = legendSize.width + GAP;
						clientArea.height -=legendSize.height +GAP;
						if(i==yAxes.length-1){
							hPos =legendSize.width + GAP;
							rowLegendNum = 1;
							rowHPosList.add(clientArea.x + (clientArea.width-hPos)/2);
							rowLegendNumList.add(rowLegendNum);
							clientArea.height -=legendSize.height +GAP;
						}
					}else{
						hPos+=legendSize.width + GAP;
						rowLegendNum++;
						if(i==yAxes.length-1){
							rowHPosList.add(clientArea.x + (clientArea.width-hPos)/2);
							rowLegendNumList.add(rowLegendNum);
							clientArea.height -=legendSize.height +GAP;
						}
					}
				}
			}
			int lm = 0;
			int vPos = clientArea.y + clientArea.height + GAP;
			for(int i=0; i<rowLegendNumList.size(); i++){
				hPos = rowHPosList.get(i);
				for(int j=0; j<rowLegendNumList.get(i); j++){
					legendList.get(lm).setBounds(new Rectangle(
							hPos, vPos, legendSizeList.get(lm).width, legendSizeList.get(lm).height));
					hPos += legendSizeList.get(lm).width + GAP;
					lm++;
				}
				vPos += legendSizeList.get(lm-1).height + GAP;
			}
		}

		for(int i=xAxisList.size()-1; i>=0; i--){
			Axis xAxis = xAxisList.get(i);
			Dimension xAxisSize = xAxis.getPreferredSize(clientArea.width, clientArea.height);
			if(xAxis.getTickLablesSide() == LabelSide.Primary){
				if(xAxis.isVisible())
					hasBottomXAxis = true;
				xAxis.setBounds(new Rectangle(clientArea.x,
					clientArea.y + clientArea.height - xAxisSize.height,
					xAxisSize.width, xAxisSize.height));
				clientArea.height -= xAxisSize.height;
			}else{
				if(xAxis.isVisible())
					hasTopXAxis = true;
				xAxis.setBounds(new Rectangle(clientArea.x,
					clientArea.y+1,
					xAxisSize.width, xAxisSize.height));
				clientArea.y += xAxisSize.height ;
				clientArea.height -= xAxisSize.height;
			}
		}

		for(int i=yAxisList.size()-1; i>=0; i--){
			Axis yAxis = yAxisList.get(i);
			int hintHeight = clientArea.height + (hasTopXAxis ? 1 :0) *yAxis.getMargin()
				+ (hasBottomXAxis ? 1 :0) *yAxis.getMargin();
			if(hintHeight > getClientArea().height)
				hintHeight = clientArea.height;
			Dimension yAxisSize = yAxis.getPreferredSize(clientArea.width,
					hintHeight);
			if(yAxis.getTickLablesSide() == LabelSide.Primary){ // on the left
				if(yAxis.isVisible())
					hasLeftYAxis = true;
				yAxis.setBounds(new Rectangle(clientArea.x,
					clientArea.y - (hasTopXAxis? yAxis.getMargin():0),
					yAxisSize.width, yAxisSize.height));
				clientArea.x += yAxisSize.width;
				clientArea.width -= yAxisSize.width;
			}else{ // on the right
				if(yAxis.isVisible())
					hasRightYAxis = true;
				yAxis.setBounds(new Rectangle(clientArea.x + clientArea.width - yAxisSize.width -1,
					clientArea.y- (hasTopXAxis? yAxis.getMargin():0),
					yAxisSize.width, yAxisSize.height));
				clientArea.width -= yAxisSize.width;
			}
		}

		//re-adjust xAxis boundss
		for(int i=xAxisList.size()-1; i>=0; i--){
			Axis xAxis = xAxisList.get(i);
			Rectangle r = xAxis.getBounds().getCopy();
			if(hasLeftYAxis)
				r.x = clientArea.x - xAxis.getMargin()-1;
			r.width = clientArea.width + (hasLeftYAxis ? xAxis.getMargin() : -1) +
					(hasRightYAxis? xAxis.getMargin() : 0);
			xAxis.setBounds(r);
		}

		if(plotArea != null && plotArea.isVisible()){

			Rectangle plotAreaBound = new Rectangle(
					primaryXAxis.getBounds().x + primaryXAxis.getMargin(),
					primaryYAxis.getBounds().y + primaryYAxis.getMargin(),
					primaryXAxis.getBounds().width - 2*primaryXAxis.getMargin(),
					primaryYAxis.getBounds().height - 2*primaryYAxis.getMargin()
					);
			plotArea.setBounds(plotAreaBound);

		}

		super.layout();
	}



	/**
	 * @param zoomType the zoomType to set
	 */
	public void setZoomType(ZoomType zoomType) {
		this.zoomType = zoomType;
		plotArea.setZoomType(zoomType);
		for(Axis axis : xAxisList)
			axis.setZoomType(zoomType);
		for(Axis axis : yAxisList)
			axis.setZoomType(zoomType);
	}

	/**
	 * @return the zoomType
	 */
	public ZoomType getZoomType() {
		return zoomType;
	}

	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title.trim();
		titleLabel.setText(title);
	}

	/**
	 * @param showTitle true if title should be shown; false otherwise.
	 */
	public void setShowTitle(boolean showTitle){
		titleLabel.setVisible(showTitle);
		revalidate();
	}

	/**
	 * @return true if title should be shown; false otherwise.
	 */
	public boolean isShowTitle(){
		return titleLabel.isVisible();
	}

	/**
	 * @param showLegend true if legend should be shown; false otherwise.
	 */
	public void setShowLegend(boolean showLegend){
		this.showLegend = showLegend;
		for(Axis yAxis : legendMap.keySet()){
			Legend legend = legendMap.get(yAxis);
			legend.setVisible(showLegend);
		}
		revalidate();
	}

	/**
	 * @return the showLegend
	 */
	public boolean isShowLegend() {
		return showLegend;
	}

	/**Add an axis to the graph
	 * @param axis
	 */
	public void addAxis(Axis axis){
		if(axis.isHorizontal())
			xAxisList.add(axis);
		else
			yAxisList.add(axis);
		plotArea.addGrid(new Grid(axis));
		add(axis);
		axis.setXyGraph(this);
		revalidate();
	}

	/**Remove an axis from the graph
	 * @param axis
	 * @return true if this axis exists.
	 */
	public boolean removeAxis(Axis axis){
		remove(axis);
		plotArea.removeGrid(axis.getGrid());
		revalidate();
		if(axis.isHorizontal())
			return xAxisList.remove(axis);
		else
			return yAxisList.remove(axis);
	}

	/**Add a trace
	 * @param trace
	 */
	public void addTrace(Trace trace){
		if (trace.getTraceColor() == null)
		{   // Cycle through default colors
		    trace.setTraceColor(XYGraphMediaFactory.getInstance().getColor(
		    		DEFAULT_TRACES_COLOR[traceNum % DEFAULT_TRACES_COLOR.length]));
        	++traceNum;
		}
		if(legendMap.containsKey(trace.getYAxis()))
			legendMap.get(trace.getYAxis()).addTrace(trace);
		else{
			legendMap.put(trace.getYAxis(), new Legend(this));
			legendMap.get(trace.getYAxis()).addTrace(trace);
			add(legendMap.get(trace.getYAxis()));
		}
		plotArea.addTrace(trace);
		trace.setXYGraph(this);
		trace.dataChanged(null);
		revalidate();
		repaint();
	}

	/**Remove a trace.
	 * @param trace
	 */
	public void removeTrace(Trace trace){
		if(legendMap.containsKey(trace.getYAxis())){
			legendMap.get(trace.getYAxis()).removeTrace(trace);
			if(legendMap.get(trace.getYAxis()).getTraceList().size() <=0){
				remove(legendMap.remove(trace.getYAxis()));
			}
		}
		plotArea.removeTrace(trace);
		revalidate();
		repaint();
	}

	/**Add an annotation
	 * @param annotation
	 */
	public void addAnnotation(Annotation annotation){
		plotArea.addAnnotation(annotation);
	}

	/**Remove an annotation
	 * @param annotation
	 */
	public void removeAnnotation(Annotation annotation){
		plotArea.removeAnnotation(annotation);
	}

	/**
	 * @param titleFont the titleFont to set
	 */
	public void setTitleFont(Font titleFont) {
		titleLabel.setFont(titleFont);
		titleFontData = titleFont.getFontData()[0];
	}

	/**
	 * @return the title font.
	 */
	public Font getTitleFont(){
		return titleLabel.getFont();
	}
	
	

	public FontData getTitleFontData() {
		return titleFontData;
	}

	/**
	 * @param titleColor the titleColor to set
	 */
	public void setTitleColor(Color titleColor) {
		this.titleColor = titleColor;
		titleLabel.setForegroundColor(titleColor);
		this.titleColorRgb = titleColor.getRGB();
	}

	/**
	 * {@inheritDoc}
	 */
	public void paintFigure(final Graphics graphics) {
		if (!transparent) {
			graphics.fillRectangle(getClientArea());
		}
		super.paintFigure(graphics);
	}

	/**
	 * @param transparent the transparent to set
	 */
	public void setTransparent(boolean transparent) {
		this.transparent = transparent;
		getPlotArea().setOpaque(!transparent);
		repaint();
	}


	/**
	 * @return the transparent
	 */
	public boolean isTransparent() {
		return transparent;
	}


	/** TODO This allows clients to change the traces via getPlotArea().getTraceList() and then add/remove/clear/...,
	 *       circumventing the designated addTrace()/removeTrace().
	 *       Can it be non-public?
	 * @return the plotArea, which contains all the elements drawn inside it.
	 */
	public PlotArea getPlotArea() {
		return plotArea;
	}

	/** @return Image of the XYFigure. Receiver must dispose. */
	public Image getImage(){
		return SingleSourceHelper.getXYGraphSnapShot(this);
	}


	/**
	 * @return the titleColor
	 */
	public Color getTitleColor() {
		if(titleColor == null)
			return getForegroundColor();
		return titleColor;
	}
	
	
	public RGB getTitleColorRgb() {
		return titleColorRgb;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @return the operationsManager
	 */
	public OperationsManager getOperationsManager() {
		return operationsManager;
	}

	/**
	 * @return the xAxisList
	 */
	public List<Axis> getXAxisList() {
		return xAxisList;
	}

	/**
	 * @return the yAxisList
	 */
	public List<Axis> getYAxisList() {
		return yAxisList;
	}

	/**
	 * @return the all the axis include xAxes and yAxes.
	 * yAxisList is appended to xAxisList in the returned list.
	 */
	public List<Axis> getAxisList(){
		List<Axis> list = new ArrayList<Axis>();
		list.addAll(xAxisList);
		list.addAll(yAxisList);
		return list;
	}

	/**
	 * @return the legendMap
	 */
	public Map<Axis, Legend> getLegendMap() {
		return legendMap;
	}

	/**
	 * Perform forced autoscale to all axes.
	 */
	public void performAutoScale(){
	    final ZoomCommand command = new ZoomCommand("Auto Scale", xAxisList, yAxisList);
		for(Axis axis : xAxisList){
			axis.performAutoScale(true);
		}
		for(Axis axis : yAxisList){
			axis.performAutoScale(true);
		}
		command.saveState();
		operationsManager.addCommand(command);
	}

	/** Stagger all axes: Autoscale each axis so that traces on various
	 *  axes don't overlap
	 */
    public void performStagger()
    {
        final double GAP = 0.1;

        final ZoomCommand command = new ZoomCommand("Stagger Axes", null, yAxisList);

        // Arrange all axes so they don't overlap by assigning 1/Nth of
        // the vertical range to each one
        final int N = yAxisList.size();
        for (int i=0; i<N; ++i)
        {
            final Axis yaxis = yAxisList.get(i);
            // Does axis handle itself in another way?
            if (yaxis.isAutoScale())
                continue;

            // Determine range of values on this axis
            final Range axis_range = yaxis.getTraceDataRange();
            // Skip axis which for some reason cannot determine its range
            if (axis_range ==  null)
                continue;

            double low = axis_range.getLower();
            double high = axis_range.getUpper();
            if (low == high)
            {   // Center trace with constant value (empty range)
                final double half = Math.abs(low/2);
                low -= half;
                high += half;
            }

            if (yaxis.isLogScaleEnabled())
            {   // Transition into log space
                low = Log10.log10(low);
                high = Log10.log10(high);
            }

            double span = high - low;
            // Make some extra space
            low -= GAP*span;
            high += GAP*span;
            span = high-low;

            // With N axes, assign 1/Nth of the vertical plot space to this axis
            // by shifting the span down according to the axis index,
            // using a total of N*range.
            low -= (N-i-1)*span;
            high += i*span;

            if (yaxis.isLogScaleEnabled())
            {   // Revert from log space
                low = Log10.pow10(low);
                high = Log10.pow10(high);
            }

            // Sanity check for empty traces
            if (low < high  &&
                !Double.isInfinite(low) && !Double.isInfinite(high))
                yaxis.setRange(low, high);
        }

        command.saveState();
        operationsManager.addCommand(command);
    }
}
