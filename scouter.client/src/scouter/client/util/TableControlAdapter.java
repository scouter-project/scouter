/*
 *  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 *
 */
package scouter.client.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

public class TableControlAdapter extends ControlAdapter{

	Table table;
	TableColumn[] cols;
	int[] colWidthRatio;
	public TableControlAdapter(Table table, TableColumn[] cols, int[] colWidthRatio) {
		super();
		this.table = table;
		this.cols = cols;
		this.colWidthRatio = colWidthRatio;
	}
	
	public void controlResized(ControlEvent e) {
		Rectangle area;
		if(e.widget instanceof Composite){
			area = ((Composite)e.widget).getClientArea();
		}else if(e.widget instanceof Table){
			area = ((Table)e.widget).getClientArea();
		}else{
			return;
		}
		
		org.eclipse.swt.graphics.Point preferredSize = table.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		int width = area.width - 2 * table.getBorderWidth();
		if (preferredSize.y > area.height + table.getHeaderHeight()) {
			org.eclipse.swt.graphics.Point vBarSize = table.getVerticalBar().getSize();
			width -= vBarSize.x;
		}
		org.eclipse.swt.graphics.Point oldSize = table.getSize();
		if (oldSize.x > area.width) {
			adjustColumnSize(width - 20);
			table.setSize(area.width, area.height);
		} else {
			table.setSize(area.width, area.height);
			adjustColumnSize(width - 20);
		}
		super.controlResized(e);
	}

	private void adjustColumnSize(int width){
		
		int fullWidthColIndex = -1;
		int extraWidthSum = -1;
		
		for(int i = 0 ; i < colWidthRatio.length ; i++){
			int ratio = colWidthRatio[i];
			if(ratio != -1){
				cols[i].setWidth(width / ratio);
				extraWidthSum += cols[i].getWidth();
			}else{
				fullWidthColIndex = i;
			}
		}
		
		if(fullWidthColIndex != -1){
			cols[fullWidthColIndex].setWidth(width - extraWidthSum);
		}
	}
}
