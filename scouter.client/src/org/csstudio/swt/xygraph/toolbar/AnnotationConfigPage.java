/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.xygraph.toolbar;

import org.csstudio.swt.xygraph.Messages;
import org.csstudio.swt.xygraph.figures.Annotation;
import org.csstudio.swt.xygraph.figures.Annotation.CursorLineStyle;
import org.csstudio.swt.xygraph.figures.Axis;
import org.csstudio.swt.xygraph.figures.Trace;
import org.csstudio.swt.xygraph.figures.XYGraph;
import org.csstudio.swt.xygraph.util.XYGraphMediaFactory;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**This will help to create the necessary widgets 
 * to configure an annotation's properties.
 * @author Xihui Chen
 * @author Kay Kasemir Layout tweaks
 */
public class AnnotationConfigPage {
	private XYGraph xyGraph;
	private Annotation annotation;
	private Text nameText;
	private Button snapToTrace;
	private Button useDefaultColorButton;
	private Combo xAxisOrTraceCombo;
	private Combo yAxisCombo;
	private ColorSelector colorSelector;
	private Font font;
	private Combo cursorLineCombo;
	private Button showNameButton;
	private Button showSampleInfoButton;
	private Button showPositionButton;
	private Label fontLabel;
	private Composite composite;
	private Label xAxisLabel;
	private Label yAxisLabel;
	private Label colorLabel;
	
	public AnnotationConfigPage(XYGraph xyGraph, Annotation annotation) {
		this.xyGraph = xyGraph;
		this.annotation = annotation;
		font = annotation.getFont();
	}
	
	public void createPage(final Composite composite){
		this.composite = composite;
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(new GridLayout(2, false));
		
		final Label nameLabel = new Label(composite, 0);
		nameLabel.setText(Messages.Annotation_Name);
		nameLabel.setLayoutData(new GridData());
		
		nameText = new Text(composite, SWT.BORDER | SWT.SINGLE);
		nameText.setToolTipText(Messages.Annotation_NameTT);
		nameText.setLayoutData(new GridData(SWT.FILL, 0, true, false));		
		
		snapToTrace = new Button(composite, SWT.CHECK);
		snapToTrace.setText(Messages.Annotation_Snap);		
		snapToTrace.setToolTipText(Messages.Annotation_SnapTT);
		snapToTrace.setLayoutData(new GridData(0, 0, false, false, 2, 1));
		
		xAxisLabel = new Label(composite, 0);
		xAxisLabel.setLayoutData(new GridData());		
		
		xAxisOrTraceCombo = new Combo(composite, SWT.DROP_DOWN);		
		xAxisOrTraceCombo.setLayoutData(new GridData(SWT.FILL, 0, true, false));
		
		yAxisLabel = new Label(composite, 0);
		yAxisLabel.setText(Messages.Annotation_YAxis);	
		yAxisLabel.setLayoutData(new GridData());
		
		yAxisCombo = new Combo(composite, SWT.DROP_DOWN);
		yAxisCombo.setToolTipText(Messages.Annotation_YAxisSnapTT);
		yAxisCombo.setLayoutData(new GridData(SWT.FILL, 0, true, false));
		
		//snapToTrace listener
		snapToTrace.addSelectionListener(new SelectionAdapter()
		{
		    @Override
			public void widgetSelected(final SelectionEvent e)
			{
		        if (snapToTrace.getSelection())
			    {
					xAxisLabel.setText(Messages.Annotation_Trace);
					xAxisOrTraceCombo.setToolTipText(Messages.Annotation_TraceSnapTT);
			    }
			    else
                {
                    xAxisLabel.setText(Messages.Annotation_XAxis);
                    xAxisOrTraceCombo.setToolTipText(Messages.Annotation_XAxisSnapTT);
                }

				xAxisOrTraceCombo.removeAll();
				if(snapToTrace.getSelection()){
					for(Trace trace : xyGraph.getPlotArea().getTraceList())
						xAxisOrTraceCombo.add(trace.getName());
				}else{
					for(Axis axis : xyGraph.getXAxisList())
						xAxisOrTraceCombo.add(axis.getTitle());
				}
				xAxisOrTraceCombo.select(0);
				if(annotation.isFree() && !snapToTrace.getSelection())
					xAxisOrTraceCombo.select(
							xyGraph.getXAxisList().indexOf(annotation.getXAxis()));
				else if(!annotation.isFree() && snapToTrace.getSelection())
					xAxisOrTraceCombo.select(xyGraph.getPlotArea().
							getTraceList().indexOf(annotation.getTrace()));
				
				yAxisLabel.setVisible(!snapToTrace.getSelection());
				yAxisCombo.setVisible(!snapToTrace.getSelection());	
				composite.layout(true, true);
			}
		});
		//annotation color
		useDefaultColorButton = new Button(composite, SWT.CHECK);
		useDefaultColorButton.setText(Messages.Annotation_ColorFromYAxis);
		useDefaultColorButton.setLayoutData(new GridData(SWT.FILL, 0, false, false, 2, 1));		
		
		colorLabel = new Label(composite, 0);
		colorLabel.setText(Messages.Annotation_Color);		
		colorLabel.setLayoutData(new GridData());
		
		colorSelector = new ColorSelector(composite);
		colorSelector.getButton().setLayoutData(new GridData());		
		useDefaultColorButton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				colorSelector.getButton().setVisible(!useDefaultColorButton.getSelection());
				colorLabel.setVisible(!useDefaultColorButton.getSelection());
			}
		});
		
		fontLabel = new Label(composite, 0);
		fontLabel.setLayoutData(new GridData());
		
		final Button fontButton = new Button(composite, SWT.PUSH);
		fontButton.setText(Messages.Annotation_ChangeFont);
		fontButton.setLayoutData(new GridData());
		fontButton.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				FontDialog fontDialog = new FontDialog(composite.getShell());
				if(font != null)
					fontDialog.setFontList(font.getFontData());
				FontData fontData = fontDialog.open();
				if(fontData != null){
					font = XYGraphMediaFactory.getInstance().getFont(fontData);
					fontLabel.setFont(font);
					fontLabel.setText(Messages.Annotation_Font + fontData.getName());
					composite.getShell().layout(true, true);
				}
			}
		});
		
		final Label cursorLineLabel = new Label(composite, 0);
		cursorLineLabel.setText(Messages.Annotation_Cursor);
		cursorLineLabel.setLayoutData(new GridData());
		
		cursorLineCombo = new Combo(composite, SWT.DROP_DOWN);
		cursorLineCombo.setItems(CursorLineStyle.stringValues());
		cursorLineCombo.setLayoutData(new GridData());
		
		showNameButton = new Button(composite, SWT.CHECK);		
		showNameButton.setText(Messages.Annotation_ShowName);
		showNameButton.setLayoutData(new GridData(0, 0, false, false, 2, 1));
		
		showSampleInfoButton = new Button(composite, SWT.CHECK);		
		showSampleInfoButton.setText(Messages.Annotation_ShowInfo);
		showSampleInfoButton.setLayoutData(new GridData(0, 0, false, false, 2, 1));
		
		showPositionButton = new Button(composite, SWT.CHECK);
		showPositionButton.setText(Messages.Annotation_ShowPosition);
		showPositionButton.setLayoutData(new GridData(0, 0, false, false, 2, 1));
		initialize();
	}
	
	/**
	 * @return the composite
	 */
	public Composite getComposite() {
		return composite;
	}

	public void applyChanges(){
		annotation.setName(nameText.getText());		
		if(snapToTrace.getSelection())			
			annotation.setTrace(xyGraph.getPlotArea().getTraceList().get(
					xAxisOrTraceCombo.getSelectionIndex()));			
		else
			annotation.setFree(xyGraph.getXAxisList().get(
					xAxisOrTraceCombo.getSelectionIndex()), 
					xyGraph.getYAxisList().get(yAxisCombo.getSelectionIndex()));
		
		if(!useDefaultColorButton.getSelection())
			annotation.setAnnotationColor(XYGraphMediaFactory.getInstance().getColor(
					colorSelector.getColorValue()));
		else
			annotation.setAnnotationColor(null);
		
		annotation.setFont(font);
		annotation.setCursorLineStyle(CursorLineStyle.values()[
				cursorLineCombo.getSelectionIndex()]);
		annotation.setShowName(showNameButton.getSelection());
		annotation.setShowSampleInfo(showSampleInfoButton.getSelection());
		annotation.setShowPosition(showPositionButton.getSelection());	
	}

	/**
	 * @return the annotation
	 */
	public Annotation getAnnotation() {
		return annotation;
	}

	/**
	 * @param annotation the annotation to set
	 */
	public void setAnnotation(Annotation annotation) {
		this.annotation = annotation;		
	}
	
	private void initialize(){
		nameText.setText(annotation.getName());
		nameText.setSelection(0, nameText.getText().length());
		snapToTrace.setSelection(!annotation.isFree());
		xAxisLabel.setText(snapToTrace.getSelection()?
				Messages.Annotation_Trace : Messages.Annotation_XAxis);
		xAxisOrTraceCombo.removeAll();
		if(!annotation.isFree()){
			for(Trace trace : xyGraph.getPlotArea().getTraceList())
				xAxisOrTraceCombo.add(trace.getName());
			xAxisOrTraceCombo.select(xyGraph.getPlotArea().getTraceList().indexOf(
					annotation.getTrace()));			
		}else{
			for(Axis axis : xyGraph.getXAxisList())
				xAxisOrTraceCombo.add(axis.getTitle());
			xAxisOrTraceCombo.select(xyGraph.getXAxisList().indexOf(annotation.getXAxis()));
		}
		for(Axis axis : xyGraph.getYAxisList())
			yAxisCombo.add(axis.getTitle());	
		yAxisCombo.select(xyGraph.getYAxisList().indexOf(annotation.getYAxis()));
		yAxisLabel.setVisible(!snapToTrace.getSelection());
		yAxisCombo.setVisible(!snapToTrace.getSelection());	
		useDefaultColorButton.setSelection(annotation.getAnnotationColor() == null);
		colorLabel.setVisible(!useDefaultColorButton.getSelection());
		colorSelector.getButton().setVisible(annotation.getAnnotationColor() != null);
		colorSelector.setColorValue(
				annotation.getAnnotationColor() == null ? 
				annotation.getYAxis().getForegroundColor().getRGB() :
				annotation.getAnnotationColor().getRGB());
		
		fontLabel.setText(Messages.Annotation_Font + (font==null? Messages.Annotation_SystemDefault : font.getFontData()[0].getName()));
		fontLabel.setFont(font);
		cursorLineCombo.select(annotation.getCursorLineStyle().getIndex());
		showNameButton.setSelection(annotation.isShowName());
		showSampleInfoButton.setSelection(annotation.isShowSampleInfo());
		showPositionButton.setSelection(annotation.isShowPosition());

	}
	
	
}
