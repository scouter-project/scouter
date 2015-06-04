/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.xygraph.toolbar;

import org.csstudio.swt.xygraph.figures.XYGraph;
import org.csstudio.swt.xygraph.util.XYGraphMediaFactory;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**This will help to create the necessary widgets 
 * to configure an annotation's properties.
 * @author Xihui Chen
 *
 */
public class GraphConfigPage {
	private XYGraph xyGraph;
	private Text titleText;
	private Font titleFont;
	private ColorSelector titleColorSelector;
	private ColorSelector plotAreaColorSelector;
	private Button showTitle;
	private Button showLegend;
	private Button showPlotAreaBorder;
	private Button transparent;


	public GraphConfigPage(XYGraph xyGraph) {
		this.xyGraph = xyGraph;
		titleFont = xyGraph.getTitleFont();
	}
	
	public void createPage(final Composite composite){
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(new GridLayout(3, false));
		GridData gd;
		GridData labelGd = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);		
		
		final Label titleLabel = new Label(composite, 0);
		titleLabel.setText("Title: ");
		titleLabel.setLayoutData(labelGd);
		
		titleText = new Text(composite, SWT.BORDER | SWT.SINGLE);
		titleText.setText(xyGraph.getTitle());
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		titleText.setLayoutData(gd);
		
		final Label fontLabel = new Label(composite, 0);
		fontLabel.setText("Title Font: " + (titleFont==null? "System Default" : titleFont.getFontData()[0].getName()));
		fontLabel.setFont(titleFont);
		fontLabel.setForeground(xyGraph.getTitleColor());
		labelGd = new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);	
		fontLabel.setLayoutData(labelGd);
		
		final Button fontButton = new Button(composite, SWT.PUSH);
		fontButton.setText("Change...");
		gd = new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 1, 1);
		fontButton.setLayoutData(gd);
		fontButton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				FontDialog fontDialog = new FontDialog(composite.getShell());
				if(titleFont != null)
					fontDialog.setFontList(titleFont.getFontData());
				FontData fontData = fontDialog.open();
				if(fontData != null){
					titleFont = XYGraphMediaFactory.getInstance().getFont(fontData);
					fontLabel.setFont(titleFont);
					fontLabel.setText("Title Font: " + fontData.getName());
					composite.getShell().layout(true, true);
				}
			}
		});
			
		
		final Label colorLabel = new Label(composite, 0);
		colorLabel.setText("Title Color:");		
		labelGd = new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);	
		colorLabel.setLayoutData(labelGd);
		
		titleColorSelector = new ColorSelector(composite);
		gd = new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 1, 1);
		titleColorSelector.getButton().setLayoutData(gd);
		titleColorSelector.setColorValue(xyGraph.getTitleColor().getRGB());
		titleColorSelector.addListener(new IPropertyChangeListener(){
			public void propertyChange(PropertyChangeEvent event) {
				fontLabel.setForeground(XYGraphMediaFactory.getInstance().getColor(
						titleColorSelector.getColorValue()));
			}
		});
		final Label plotAreaColorLabel = new Label(composite, 0);
		plotAreaColorLabel.setText("Plot Area Background Color:");		
		labelGd = new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);	
		plotAreaColorLabel.setLayoutData(labelGd);
		
		plotAreaColorSelector = new ColorSelector(composite);
		gd = new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 1, 1);
		plotAreaColorSelector.getButton().setLayoutData(gd);
		plotAreaColorSelector.setColorValue(
				xyGraph.getPlotArea().getBackgroundColor().getRGB());
		
		showTitle = new Button(composite, SWT.CHECK);
		showTitle.setSelection(xyGraph.isShowTitle());
		showTitle.setText("Show Title");
		gd = new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 3, 1);
		showTitle.setLayoutData(gd);
		
		showLegend = new Button(composite, SWT.CHECK);
		showLegend.setSelection(xyGraph.isShowLegend());
		showLegend.setText("Show Legend");
		gd = new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 3, 1);
		showLegend.setLayoutData(gd);
		
		showPlotAreaBorder = new Button(composite, SWT.CHECK);
		showPlotAreaBorder.setSelection(xyGraph.getPlotArea().isShowBorder());
		showPlotAreaBorder.setText("Show Plot Area Border");
		gd = new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 3, 1);
		showPlotAreaBorder.setLayoutData(gd);
		
		transparent = new Button(composite, SWT.CHECK);
		transparent.setSelection(xyGraph.isTransparent());
		transparent.setText("Transparent");
		gd = new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 3, 1);
		transparent.setLayoutData(gd);
	}
	
	public void applyChanges(){
		xyGraph.setTitle(titleText.getText());
		xyGraph.setTitleFont(titleFont);
		xyGraph.setTitleColor(XYGraphMediaFactory.getInstance().getColor(
				titleColorSelector.getColorValue()));
		xyGraph.getPlotArea().setBackgroundColor(
				XYGraphMediaFactory.getInstance().getColor(
						plotAreaColorSelector.getColorValue()));
		xyGraph.setShowTitle(showTitle.getSelection());
		xyGraph.setShowLegend(showLegend.getSelection());
		xyGraph.getPlotArea().setShowBorder(showPlotAreaBorder.getSelection());
		xyGraph.setTransparent(transparent.getSelection());
	}

	/**
	 * @return the annotation
	 */
	public XYGraph getXYGraph() {
		return xyGraph;
	}
	
	
}
