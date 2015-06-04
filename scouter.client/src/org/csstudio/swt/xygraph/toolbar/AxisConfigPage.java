/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.xygraph.toolbar;

import org.csstudio.swt.xygraph.figures.Axis;
import org.csstudio.swt.xygraph.figures.XYGraph;
import org.csstudio.swt.xygraph.linearscale.Range;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

/**This will help to create the necessary widgets 
 * to configure an axis's properties.
 * @author Xihui Chen
 *
 */
public class AxisConfigPage {
	private XYGraph xyGraph;
	private Axis axis;
	private Text titleText;
	private Label scaleFontLabel;
	private Font scaleFont;	
	private Label titleFontLabel;
	private Font titleFont;
	private ColorSelector axisColorSelector;
	private Button primaryButton;
	private Button logButton;
	
	private Button autoScaleButton;
	private Label maxOrAutoScaleLabel;
	private DoubleInputText maxOrAutoScaleThrText;	
	private Label minLabel;
	private DoubleInputText minText;
	
	private Button dateEnabledButton;
	private Button autoFormat;
	private Label formatLabel;
	private Text formatText;	
	
	private Button showGridButton;
	private Button dashGridLineButton;
	private ColorSelector gridColorSelector;
	
	private Composite composite;
	
	public AxisConfigPage(XYGraph xyGraph, Axis axis) {
		this.xyGraph = xyGraph;
		this.axis = axis;
		scaleFont = axis.getFont();
		titleFont = axis.getTitleFont();
	}
	
	public void createPage(final Composite composite){
		this.composite = composite;
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(new GridLayout(3, false));
		GridData gd;
		GridData labelGd = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);		
		
		final Label titleLabel = new Label(composite, 0);
		titleLabel.setText("Title: ");
		titleLabel.setLayoutData(labelGd);
		
		titleText = new Text(composite, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		titleText.setLayoutData(gd);		
		
		titleFontLabel = new Label(composite, 0);		
		labelGd = new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);	
		titleFontLabel.setLayoutData(labelGd);
		
		final Button titleFontButton = new Button(composite, SWT.PUSH);
		titleFontButton.setText("Change...");
		gd = new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 1, 1);
		titleFontButton.setLayoutData(gd);
		titleFontButton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				FontDialog fontDialog = new FontDialog(composite.getShell());
				if(titleFont != null)
					fontDialog.setFontList(titleFont.getFontData());
				FontData fontData = fontDialog.open();
				if(fontData != null){
					titleFont = XYGraphMediaFactory.getInstance().getFont(fontData);
					titleFontLabel.setFont(titleFont);
					titleFontLabel.setText("Title Font: " + fontData.getName());
					composite.getShell().layout(true, true);
				}
			}
		});
		
		
		scaleFontLabel = new Label(composite, 0);		
		labelGd = new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);	
		scaleFontLabel.setLayoutData(labelGd);
		
		final Button scaleFontButton = new Button(composite, SWT.PUSH);
		scaleFontButton.setText("Change...");
		gd = new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 1, 1);
		scaleFontButton.setLayoutData(gd);
		scaleFontButton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				FontDialog fontDialog = new FontDialog(composite.getShell());
				if(scaleFont != null)
					fontDialog.setFontList(scaleFont.getFontData());
				FontData fontData = fontDialog.open();
				if(fontData != null){
					scaleFont = XYGraphMediaFactory.getInstance().getFont(fontData);
					scaleFontLabel.setFont(scaleFont);
					scaleFontLabel.setText("Scale Font: " + fontData.getName());
					composite.getShell().layout(true, true);
				}
			}
		});
		
		final Label colorLabel = new Label(composite, 0);
		colorLabel.setText("Axis Color:");		
		labelGd = new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);	
		colorLabel.setLayoutData(labelGd);
		
		axisColorSelector = new ColorSelector(composite);
		gd = new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 1, 1);
		axisColorSelector.getButton().setLayoutData(gd);
		axisColorSelector.addListener(new IPropertyChangeListener(){
			public void propertyChange(PropertyChangeEvent event) {
				scaleFontLabel.setForeground(XYGraphMediaFactory.getInstance().getColor(
						axisColorSelector.getColorValue()));
				titleFontLabel.setForeground(XYGraphMediaFactory.getInstance().getColor(
						axisColorSelector.getColorValue()));
			}
		});
		
		
		primaryButton = new Button(composite, SWT.CHECK);
		configCheckButton(primaryButton, "On Primary Side(Bottom/Left)");
		
		logButton = new Button(composite, SWT.CHECK);
		configCheckButton(logButton, "Log");
		
		autoScaleButton = new Button(composite, SWT.CHECK);
		configCheckButton(autoScaleButton, "Auto Scale Enabled");
		
		maxOrAutoScaleLabel = new Label(composite, 0);
		labelGd = new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 1, 1);	
		maxOrAutoScaleLabel.setLayoutData(labelGd);			
		maxOrAutoScaleThrText = new DoubleInputText(composite, SWT.BORDER | SWT.SINGLE);		
		gd = new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1);
		maxOrAutoScaleThrText.getText().setLayoutData(gd);		
		
		minLabel = new Label(composite, 0);
		minLabel.setText("Minimum: ");	
		labelGd = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);	
		minLabel.setLayoutData(labelGd);
		minText = new DoubleInputText(composite, SWT.BORDER | SWT.SINGLE);		
		gd = new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1);
		minText.getText().setLayoutData(gd);
		
		//autoScale button listener
		autoScaleButton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {	
				if(autoScaleButton.getSelection()){
					maxOrAutoScaleLabel.setText("Auto Scale Threshold(%)");
					maxOrAutoScaleThrText.setRange(new Range(0, 100));
					maxOrAutoScaleThrText.getText().setText(
							String.valueOf(axis.getAutoScaleThreshold()));
					minLabel.setVisible(false);
					minText.getText().setVisible(false);
				}else{
					maxOrAutoScaleLabel.setText("Maximum");
					maxOrAutoScaleThrText.setRange(null);
					maxOrAutoScaleThrText.getText().setText(
							String.valueOf(axis.getRange().getUpper()));
					minLabel.setVisible(true);
					minText.getText().setVisible(true);
				}
				composite.getShell().layout(true, true);
			}
		});
		
		dateEnabledButton = new Button(composite, SWT.CHECK);
		configCheckButton(dateEnabledButton, "Time Format Enabled");
		
		autoFormat = new Button(composite, SWT.CHECK);
		configCheckButton(autoFormat, "Auto Format");
		
		formatLabel = new Label(composite, 0);		
		labelGd = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);	
		formatLabel.setLayoutData(labelGd);		
		formatText = new Text(composite, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		formatText.setLayoutData(gd);		
		
		dateEnabledButton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean saveDateEnabled = axis.isDateEnabled();
				boolean saveAutoFormat = axis.isAutoFormat();
				axis.setDateEnabled(dateEnabledButton.getSelection());
				axis.setAutoFormat(true);
				formatLabel.setText(dateEnabledButton.getSelection()? 
						"Time Format: " : "Numeric Format: ");
				formatText.setText(axis.getFormatPattern());
				axis.setDateEnabled(saveDateEnabled);
				axis.setAutoFormat(saveAutoFormat);
				composite.getShell().layout(true, true);
				
			}
		});
		
		autoFormat.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				formatText.setEnabled(!autoFormat.getSelection());
				if(autoFormat.getSelection()){
					boolean saveDateEnabled = axis.isDateEnabled();
					boolean saveAutoFormat = axis.isAutoFormat();
					axis.setDateEnabled(dateEnabledButton.getSelection());
					axis.setAutoFormat(autoFormat.getSelection());
					formatText.setText(axis.getFormatPattern());
					axis.setDateEnabled(saveDateEnabled);
					axis.setAutoFormat(saveAutoFormat);
				}
				
			}
		});
		
		showGridButton = new Button(composite, SWT.CHECK);
		configCheckButton(showGridButton, "Show Grid Line");
		dashGridLineButton = new Button(composite, SWT.CHECK);
		configCheckButton(dashGridLineButton, "Dash Grid Line");
		
		Label gridColorLabel = new Label(composite, 0);		
		gridColorLabel.setText("Grid Color");
		labelGd = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);	
		gridColorLabel.setLayoutData(labelGd);		
		
		gridColorSelector = new ColorSelector(composite);
		gd = new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 1, 1);
		gridColorSelector.getButton().setLayoutData(gd);			
		initialize();
	}

	
	
	private void configCheckButton(Button button, String text) {
		button.setText(text);	
		button.setLayoutData(
				new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 3, 2));
	}
	
	
	/**
	 * @return the composite
	 */
	public Composite getComposite() {
		return composite;
	}

	public void applyChanges(){
		axis.setTitle(titleText.getText());
		
		axis.setFont(scaleFont);
		axis.setTitleFont(titleFont);
		
		axis.setForegroundColor(XYGraphMediaFactory.getInstance().getColor(
				axisColorSelector.getColorValue()));
		axis.setPrimarySide(primaryButton.getSelection());
		axis.setLogScale(logButton.getSelection());
		axis.setAutoScale(autoScaleButton.getSelection());
		if(autoScaleButton.getSelection())
			axis.setAutoScaleThreshold(maxOrAutoScaleThrText.getDoubleValue());			
		else
			axis.setRange(minText.getDoubleValue(), maxOrAutoScaleThrText.getDoubleValue());
		axis.setDateEnabled(dateEnabledButton.getSelection());
		axis.setAutoFormat(autoFormat.getSelection());
		if(!autoFormat.getSelection()){
			String saveFormat = axis.getFormatPattern();
			axis.setFormatPattern(formatText.getText());			
			try {
				axis.format(0);
			} catch (Exception e) {				
				axis.setFormatPattern(saveFormat);
				MessageBox mb = new MessageBox(Display.getCurrent().getActiveShell(), 
							SWT.ICON_ERROR | SWT.OK);
				mb.setMessage("Failed to set format due to incorrect format pattern: "
						+ e.getMessage());
				mb.setText("Format pattern error!");				
				mb.open();
			}
		}
		axis.setShowMajorGrid(showGridButton.getSelection());
		axis.setDashGridLine(dashGridLineButton.getSelection());
		axis.setMajorGridColor( XYGraphMediaFactory.getInstance().getColor(
				gridColorSelector.getColorValue()));		
	}


	
	private void initialize(){
		titleText.setText(axis.getTitle());
		scaleFontLabel.setForeground(axis.getForegroundColor());
		scaleFontLabel.setFont(scaleFont);
		scaleFontLabel.setText("Scale Font: " + scaleFont.getFontData()[0].getName());
		titleFontLabel.setForeground(axis.getForegroundColor());
		titleFontLabel.setFont(titleFont);
		titleFontLabel.setText("Title Font: " + titleFont.getFontData()[0].getName());
		axisColorSelector.setColorValue(axis.getForegroundColor().getRGB());
		primaryButton.setSelection(axis.isOnPrimarySide());
		if(axis == xyGraph.primaryXAxis || axis == xyGraph.primaryYAxis)
			primaryButton.setEnabled(false);
		logButton.setSelection(axis.isLogScaleEnabled());
		autoScaleButton.setSelection(axis.isAutoScale());
		if(autoScaleButton.getSelection()){
			maxOrAutoScaleLabel.setText("Auto Scale Threshold(%)");
			maxOrAutoScaleThrText.setRange(new Range(0, 100));
			maxOrAutoScaleThrText.getText().setText(
					String.valueOf(axis.getAutoScaleThreshold()));
			minLabel.setVisible(false);
			minText.getText().setVisible(false);
		}else{
			maxOrAutoScaleLabel.setText("Maximum");
			maxOrAutoScaleThrText.setRange(null);
			maxOrAutoScaleThrText.getText().setText(
					String.valueOf(axis.getRange().getUpper()));
			minLabel.setVisible(true);
			minText.getText().setVisible(true);
		}
		
		minText.getText().setText(String.valueOf(axis.getRange().getLower()));
		dateEnabledButton.setSelection(axis.isDateEnabled());		
		autoFormat.setSelection(axis.isAutoFormat());
		formatLabel.setText(dateEnabledButton.getSelection()? 
				"Time Format: " : "Numeric Format: ");	
		formatText.setText(axis.getFormatPattern());
		//formatLabel.setVisible(!autoFormat.getSelection());
		formatText.setEnabled(!autoFormat.getSelection());
		
		showGridButton.setSelection(axis.isShowMajorGrid());
		dashGridLineButton.setSelection(axis.isDashGridLine());
		gridColorSelector.setColorValue(axis.getMajorGridColor().getRGB());		
	}
	
	
}
