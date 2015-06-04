/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.swt.xygraph.toolbar;

import org.csstudio.swt.xygraph.linearscale.Range;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

/**A SWT text which only allow double input. It also has the possibility for range check.
 * @author Xihui Chen
 *
 */
public class DoubleInputText{
	
	private Text text;
	
	private String previousText;
	
	private Range range;	

	public DoubleInputText(Composite parent, int style) {		
		text = new Text(parent, style);
		text.addFocusListener(new FocusListener(){

			public void focusGained(FocusEvent e) {
				previousText = text.getText();
			}

			public void focusLost(FocusEvent e) {
				try{
					double acceptedValue = Double.parseDouble(text.getText());
					if(range != null){
						if (acceptedValue > range.getUpper()) {
							acceptedValue = range.getUpper();
						} else if (acceptedValue < range.getLower()) {
							acceptedValue = range.getLower();
						}						
					}	
					text.setText(String.valueOf(acceptedValue));
				}catch (Exception exception) {
					text.setText(previousText);
					return;
				}	
			}
			
		});
		
	}
	
	public DoubleInputText(Composite parent, int style, double max, double min) {
		this(parent, style);
		range = new Range(min, max);
	}	
	
	/**
	 * @return the range
	 */
	public Range getRange() {
		return range;
	}

	/**
	 * @param range the range to set. null for no range check.
	 */
	public void setRange(Range range) {
		this.range = range;
	}


	/**
	 * @return the text
	 */
	public Text getText() {
		return text;
	}
	
	public double getDoubleValue(){
		return Double.parseDouble(text.getText());
	}
	

}
