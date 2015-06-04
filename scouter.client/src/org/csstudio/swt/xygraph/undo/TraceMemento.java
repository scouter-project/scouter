/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.xygraph.undo;

import org.csstudio.swt.xygraph.figures.Axis;
import org.csstudio.swt.xygraph.figures.Trace.BaseLine;
import org.csstudio.swt.xygraph.figures.Trace.ErrorBarType;
import org.csstudio.swt.xygraph.figures.Trace.PointStyle;
import org.csstudio.swt.xygraph.figures.Trace.TraceType;
import org.eclipse.swt.graphics.Color;

/**The memento to save the properties of a trace.
 * @author Xihui Chen
 *
 */
public class TraceMemento {
	
	private String name;
	private Axis xAxis, yAxis;
	private Color traceColor;
	private TraceType traceType;
	private int lineWidth;
	private PointStyle pointStyle;
	private int pointSize;
	private BaseLine baseLine;
	private int areaAlpha;
	private boolean antiAliasing;
	private boolean errorBarEnabled;
	private ErrorBarType xErrorBarType, yErrorBarType;
	private Color errorBarColor;
	private int errorBarCapWidth;
	private boolean drawYErrorInArea;
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the xAxis
	 */
	public Axis getXAxis() {
		return xAxis;
	}
	/**
	 * @param axis the xAxis to set
	 */
	public void setXAxis(Axis axis) {
		xAxis = axis;
	}
	/**
	 * @return the yAxis
	 */
	public Axis getYAxis() {
		return yAxis;
	}
	/**
	 * @param axis the yAxis to set
	 */
	public void setYAxis(Axis axis) {
		yAxis = axis;
	}
	/**
	 * @return the traceColor
	 */
	public Color getTraceColor() {
		return traceColor;
	}
	/**
	 * @param traceColor the traceColor to set
	 */
	public void setTraceColor(Color traceColor) {
		this.traceColor = traceColor;
	}
	/**
	 * @return the traceType
	 */
	public TraceType getTraceType() {
		return traceType;
	}
	/**
	 * @param traceType the traceType to set
	 */
	public void setTraceType(TraceType traceType) {
		this.traceType = traceType;
	}
	/**
	 * @return the lineWidth
	 */
	public int getLineWidth() {
		return lineWidth;
	}
	/**
	 * @param lineWidth the lineWidth to set
	 */
	public void setLineWidth(int lineWidth) {
		this.lineWidth = lineWidth;
	}
	/**
	 * @return the pointStyle
	 */
	public PointStyle getPointStyle() {
		return pointStyle;
	}
	/**
	 * @param pointStyle the pointStyle to set
	 */
	public void setPointStyle(PointStyle pointStyle) {
		this.pointStyle = pointStyle;
	}
	/**
	 * @return the pointSize
	 */
	public int getPointSize() {
		return pointSize;
	}
	/**
	 * @param pointSize the pointSize to set
	 */
	public void setPointSize(int pointSize) {
		this.pointSize = pointSize;
	}
	/**
	 * @return the baseLine
	 */
	public BaseLine getBaseLine() {
		return baseLine;
	}
	/**
	 * @param baseLine the baseLine to set
	 */
	public void setBaseLine(BaseLine baseLine) {
		this.baseLine = baseLine;
	}
	/**
	 * @return the areaAlpha
	 */
	public int getAreaAlpha() {
		return areaAlpha;
	}
	/**
	 * @param areaAlpha the areaAlpha to set
	 */
	public void setAreaAlpha(int areaAlpha) {
		this.areaAlpha = areaAlpha;
	}
	/**
	 * @return the antiAliasing
	 */
	public boolean isAntiAliasing() {
		return antiAliasing;
	}
	/**
	 * @param antiAliasing the antiAliasing to set
	 */
	public void setAntiAliasing(boolean antiAliasing) {
		this.antiAliasing = antiAliasing;
	}
	/**
	 * @return the errorBarEnabled
	 */
	public boolean isErrorBarEnabled() {
		return errorBarEnabled;
	}
	/**
	 * @param errorBarEnabled the errorBarEnabled to set
	 */
	public void setErrorBarEnabled(boolean errorBarEnabled) {
		this.errorBarEnabled = errorBarEnabled;
	}
	/**
	 * @return the xErrorBarType
	 */
	public ErrorBarType getXErrorBarType() {
		return xErrorBarType;
	}
	/**
	 * @param errorBarType the xErrorBarType to set
	 */
	public void setXErrorBarType(ErrorBarType errorBarType) {
		xErrorBarType = errorBarType;
	}
	/**
	 * @return the yErrorBarType
	 */
	public ErrorBarType getYErrorBarType() {
		return yErrorBarType;
	}
	/**
	 * @param errorBarType the yErrorBarType to set
	 */
	public void setYErrorBarType(ErrorBarType errorBarType) {
		yErrorBarType = errorBarType;
	}
	/**
	 * @return the errorBarColor
	 */
	public Color getErrorBarColor() {
		return errorBarColor;
	}
	/**
	 * @param errorBarColor the errorBarColor to set
	 */
	public void setErrorBarColor(Color errorBarColor) {
		this.errorBarColor = errorBarColor;
	}
	/**
	 * @return the errorBarCapWidth
	 */
	public int getErrorBarCapWidth() {
		return errorBarCapWidth;
	}
	/**
	 * @param errorBarCapWidth the errorBarCapWidth to set
	 */
	public void setErrorBarCapWidth(int errorBarCapWidth) {
		this.errorBarCapWidth = errorBarCapWidth;
	}
	/**
	 * @return the drawYErrorInArea
	 */
	public boolean isDrawYErrorInArea() {
		return drawYErrorInArea;
	}
	/**
	 * @param drawYErrorInArea the drawYErrorInArea to set
	 */
	public void setDrawYErrorInArea(boolean drawYErrorInArea) {
		this.drawYErrorInArea = drawYErrorInArea;
	}	
	
}
