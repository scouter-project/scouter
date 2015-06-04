/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.xygraph.undo;

import org.csstudio.swt.xygraph.linearscale.Range;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;

/**The memento to save the properties of an axis.
 * @author Xihui Chen
 *
 */
public class AxisMemento {
	
	private String title;
	private Font titleFont;
	private Color foregroundColor;
	private boolean onPrimarySide;
	private boolean logScale;
	private boolean autoScale;
	private double autoScaleThreshold;
	private Range range;
	private boolean dateEnabled;
	private boolean autoFormat;
	private String formatPattern;
	private boolean showMajorGrid;
	private boolean dashGridLine;
	private Color majorGridColor;
	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	/**
	 * @return the font
	 */
	public Font getTitleFont() {
		return titleFont;
	}
	/**
	 * @param font the font to set
	 */
	public void setTitleFont(Font font) {
		this.titleFont = font;
	}
	/**
	 * @return the foreGroundColor
	 */
	public Color getForegroundColor() {
		return foregroundColor;
	}
	/**
	 * @param foreGroundColor the foreGroundColor to set
	 */
	public void setForegroundColor(Color foreGroundColor) {
		this.foregroundColor = foreGroundColor;
	}
	/**
	 * @return the onPrimarySide
	 */
	public boolean isOnPrimarySide() {
		return onPrimarySide;
	}
	/**
	 * @param onPrimarySide the onPrimarySide to set
	 */
	public void setPrimarySide(boolean onPrimarySide) {
		this.onPrimarySide = onPrimarySide;
	}
	/**
	 * @return the logScale
	 */
	public boolean isLogScaleEnabled() {
		return logScale;
	}
	/**
	 * @param logScale the logScale to set
	 */
	public void setLogScale(boolean logScale) {
		this.logScale = logScale;
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
	public void setAutoScale(boolean autoScale) {
		this.autoScale = autoScale;
	}
	/**
	 * @return the autoScaleThreshold
	 */
	public double getAutoScaleThreshold() {
		return autoScaleThreshold;
	}
	/**
	 * @param autoScaleThreshold the autoScaleThreshold to set
	 */
	public void setAutoScaleThreshold(double autoScaleThreshold) {
		this.autoScaleThreshold = autoScaleThreshold;
	}
	/**
	 * @return the range
	 */
	public Range getRange() {
		return range;
	}
	/**
	 * @param range the range to set
	 */
	public void setRange(Range range) {
		this.range = range;
	}
	/**
	 * @return the dateEnabled
	 */
	public boolean isDateEnabled() {
		return dateEnabled;
	}
	/**
	 * @param dateEnabled the dateEnabled to set
	 */
	public void setDateEnabled(boolean dateEnabled) {
		this.dateEnabled = dateEnabled;
	}
	/**
	 * @return the autoFormat
	 */
	public boolean isAutoFormat() {
		return autoFormat;
	}
	/**
	 * @param autoFormat the autoFormat to set
	 */
	public void setAutoFormat(boolean autoFormat) {
		this.autoFormat = autoFormat;
	}
	/**
	 * @return the formatPattern
	 */
	public String getFormatPattern() {
		return formatPattern;
	}
	/**
	 * @param formatPattern the formatPattern to set
	 */
	public void setFormatPattern(String formatPattern) {
		this.formatPattern = formatPattern;
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
	public void setShowMajorGrid(boolean showMajorGrid) {
		this.showMajorGrid = showMajorGrid;
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
	public void setDashGridLine(boolean dashGridLine) {
		this.dashGridLine = dashGridLine;
	}
	/**
	 * @return the majorGridColor
	 */
	public Color getMajorGridColor() {
		return majorGridColor;
	}
	/**
	 * @param majorGridColor the majorGridColor to set
	 */
	public void setMajorGridColor(Color majorGridColor) {
		this.majorGridColor = majorGridColor;
	}
	
	

}
