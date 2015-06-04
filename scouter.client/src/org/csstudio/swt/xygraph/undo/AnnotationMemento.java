/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.xygraph.undo;

import org.csstudio.swt.xygraph.figures.Annotation.CursorLineStyle;
import org.csstudio.swt.xygraph.figures.Axis;
import org.csstudio.swt.xygraph.figures.Trace;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;

/**The memento to save the properties of an annotation
 * @author Xihui Chen
 *
 */
public class AnnotationMemento {
	
	private String name;
	private boolean free;
	private Axis xAxis, yAxis;
	private Trace trace;
	private Color annotationColor;
	private Font font;
	private CursorLineStyle cursorLineStyle;
	private boolean showName;
	private boolean showSampleInfo;
	private boolean showPosition;
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
	 * @return the free
	 */
	public boolean isFree() {
		return free;
	}
	/**
	 * @param free the free to set
	 */
	public void setFree(boolean free) {
		this.free = free;
	}
	/**
	 * @return the trace
	 */
	public Trace getTrace() {
		return trace;
	}
	/**
	 * @param trace the trace to set
	 */
	public void setTrace(Trace trace) {
		this.trace = trace;
	}
	/**
	 * @return the annotationColor
	 */
	public Color getAnnotationColor() {
		return annotationColor;
	}
	/**
	 * @param annotationColor the annotationColor to set
	 */
	public void setAnnotationColor(Color annotationColor) {
		this.annotationColor = annotationColor;
	}
	/**
	 * @return the font
	 */
	public Font getFont() {
		return font;
	}
	/**
	 * @param font the font to set
	 */
	public void setFont(Font font) {
		this.font = font;
	}
	/**
	 * @return the cursorLineStyle
	 */
	public CursorLineStyle getCursorLineStyle() {
		return cursorLineStyle;
	}
	/**
	 * @param cursorLineStyle the cursorLineStyle to set
	 */
	public void setCursorLineStyle(CursorLineStyle cursorLineStyle) {
		this.cursorLineStyle = cursorLineStyle;
	}
	/**
	 * @return the showName
	 */
	public boolean isShowName() {
		return showName;
	}
	/**
	 * @param showName the showName to set
	 */
	public void setShowName(boolean showName) {
		this.showName = showName;
	}
	/**
	 * @return the showSampleInfo
	 */
	public boolean isShowSampleInfo() {
		return showSampleInfo;
	}
	/**
	 * @param showSampleInfo the showSampleInfo to set
	 */
	public void setShowSampleInfo(boolean showSampleInfo) {
		this.showSampleInfo = showSampleInfo;
	}
	/**
	 * @return the showPosition
	 */
	public boolean isShowPosition() {
		return showPosition;
	}
	/**
	 * @param showPosition the showPosition to set
	 */
	public void setShowPosition(boolean showPosition) {
		this.showPosition = showPosition;
	}
	/**
	 * @param yAxis the yAxis to set
	 */
	public void setYAxis(Axis yAxis) {
		this.yAxis = yAxis;
	}
	/**
	 * @return the yAxis
	 */
	public Axis getYAxis() {
		return yAxis;
	}
	/**
	 * @param xAxis the xAxis to set
	 */
	public void setXAxis(Axis xAxis) {
		this.xAxis = xAxis;
	}
	/**
	 * @return the xAxis
	 */
	public Axis getXAxis() {
		return xAxis;
	}

}
