/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.xygraph.undo;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;

/**The memento to hold the properties of an XYGraph, 
 * so to make the operation on XYGraph undoable.
 * @author Xihui Chen
 *
 */
public class XYGraphMemento {

	private String Title;
	private Font titleFont;
	
	/**
	 * Add because getTitleFont send a SWTERROR if the receiver is dispose. 
	 * It is the case when you save the plt file after ask to close CSS.
	 */
	private FontData titleFontData;
	
	private Color titleColor;
	private Color plotAreaBackColor;
	private boolean showTitle;
	private boolean showLegend;
	private boolean showPlotAreaBorder;
	private boolean transparent;
	
	private List<AnnotationMemento> annotationMementoList;
	private List<AxisMemento> axisMementoList;
	private List<TraceMemento> traceMementoList;
	
	public XYGraphMemento() {
		annotationMementoList = new ArrayList<AnnotationMemento>();
		axisMementoList = new ArrayList<AxisMemento>();
		traceMementoList = new ArrayList<TraceMemento>();
	}
	
	public void addAnnotationMemento(AnnotationMemento memento){
		annotationMementoList.add(memento);
	}
	
	public void addAxisMemento(AxisMemento memento){
		axisMementoList.add(memento);
	}
	
	public void addTraceMemento(TraceMemento memento){
		traceMementoList.add(memento);
	}
	
	
	/**
	 * @return the title
	 */
	public String getTitle() {
		return Title;
	}
	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		Title = title;
	}
	/**
	 * @return the titleFont
	 */
	public Font getTitleFont() {
		return titleFont;
	}
	/**
	 * @param titleFont the titleFont to set
	 */
	public void setTitleFont(Font titleFont) {
		this.titleFont = titleFont;
		this.titleFontData = this.titleFont.getFontData()[0];
	}
	
	public FontData getTitleFontData() {
		return titleFontData;
	}
	/**
	 * @return the titleColor
	 */
	public Color getTitleColor() {
		return titleColor;
	}
	/**
	 * @param titleColor the titleColor to set
	 */
	public void setTitleColor(Color titleColor) {
		this.titleColor = titleColor;
	}
	/**
	 * @return the plotAreaBackColor
	 */
	public Color getPlotAreaBackColor() {
		return plotAreaBackColor;
	}
	/**
	 * @param plotAreaBackColor the plotAreaBackColor to set
	 */
	public void setPlotAreaBackColor(Color plotAreaBackColor) {
		this.plotAreaBackColor = plotAreaBackColor;
	}
	/**
	 * @return the showTitle
	 */
	public boolean isShowTitle() {
		return showTitle;
	}
	/**
	 * @param showTitle the showTitle to set
	 */
	public void setShowTitle(boolean showTitle) {
		this.showTitle = showTitle;
	}
	/**
	 * @return the showLegend
	 */
	public boolean isShowLegend() {
		return showLegend;
	}
	/**
	 * @param showLegend the showLegend to set
	 */
	public void setShowLegend(boolean showLegend) {
		this.showLegend = showLegend;
	}
	/**
	 * @return the showPlotAreaBorder
	 */
	public boolean isShowPlotAreaBorder() {
		return showPlotAreaBorder;
	}
	/**
	 * @param showPlotAreaBorder the showPlotAreaBorder to set
	 */
	public void setShowPlotAreaBorder(boolean showPlotAreaBorder) {
		this.showPlotAreaBorder = showPlotAreaBorder;
	}
	/**
	 * @return the transparen
	 */
	public boolean isTransparent() {
		return transparent;
	}
	/**
	 * @param transparen the transparen to set
	 */
	public void setTransparent(boolean transparen) {
		this.transparent = transparen;
	}


	/**
	 * @return the annotationMementoList
	 */
	public List<AnnotationMemento> getAnnotationMementoList() {
		return annotationMementoList;
	}


	/**
	 * @return the axisMementoList
	 */
	public List<AxisMemento> getAxisMementoList() {
		return axisMementoList;
	}


	/**
	 * @return the traceMementoList
	 */
	public List<TraceMemento> getTraceMementoList() {
		return traceMementoList;
	}
	
	
	
}
