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

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import scouter.client.Images;
import scouter.util.CastUtil;

public class UIUtil {
	public static Point getMousePosition(){
		PointerInfo a = MouseInfo.getPointerInfo();
		Point b = a.getLocation();
		return b;
	}
	
	public static void setDialogDefaultFunctions(final Shell shell){
		shell.addListener(SWT.Traverse, new Listener() {
			public void handleEvent(Event event) {
				switch (event.detail) {
				case SWT.TRAVERSE_ESCAPE:
					shell.close();
					event.detail = SWT.TRAVERSE_NONE;
					event.doit = false;
					break;
				}
			}
		});
	}
	
	public static TableColumn create(final Table table, int swt, String name, final int col_tot, final int col_idx,
			final boolean isNum, int width) {
		final TableColumn c = new TableColumn(table, swt);
		c.setText(name);
		c.setWidth(width);
		c.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				TableItem[] items = table.getItems();
				boolean asc = CastUtil.cboolean(c.getData("sort"));
				c.setData("sort", new Boolean(!asc));
				if (isNum) {
					new SortUtil(asc).sort_num(items, col_idx, col_tot);
				} else {
					new SortUtil(asc).sort_str(items, col_idx, col_tot);
				}
			}
		});
		return c;
	}
	
	public static TableColumn create(final Table table, int swt, String name, final int col_tot, final int col_idx,
			final boolean isNum, int width, final ViewWithTable viewWithTable) {
		
		final TableColumn c = new TableColumn(table, swt);
		c.setText(name);
		c.setWidth(width);
		c.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				TableItem[] items = table.getItems();
				boolean asc = CastUtil.cboolean(c.getData("sort"));
				c.setData("sort", new Boolean(!asc));
				
				viewWithTable.setSortCriteria(asc, col_idx, isNum);

				if (isNum) {
					new SortUtil(asc).sort_num(items, col_idx, col_tot);
				} else {
					new SortUtil(asc).sort_str(items, col_idx, col_tot);
				}
				for (int i = 0; i < items.length; i++) {
					viewWithTable.setTableItem(items[i]);
				}
			}
		});
		return c;
	}
	
	public interface ViewWithTable {
		void setSortCriteria(boolean asc, int col_idx, boolean isNum);
		void setTableItem(TableItem t);
	}
	
	public static TableColumn create(final Table table, int swt, String name, final int col_tot, final int col_idx,
			final boolean isNum, int width, final XLogViewWithTable xLogViewWithTable) {
		
		final TableColumn c = new TableColumn(table, swt);
		c.setText(name);
		c.setWidth(width);
		c.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				TableItem[] items = table.getItems();
				boolean asc = CastUtil.cboolean(c.getData("sort"));
				c.setData("sort", new Boolean(!asc));

				if (isNum) {
					new SortUtil(asc).sort_num(items, col_idx, col_tot);
				} else {
					new SortUtil(asc).sort_str(items, col_idx, col_tot);
				}
				for (int i = 0; i < items.length; i++) {
					xLogViewWithTable.setTableItem(items[i]);
				}
				xLogViewWithTable.setChanges();
			}
		});
		return c;
	}
	
	public interface XLogViewWithTable {
		void setTableItem(TableItem t);
		void setChanges();
	}
	
	public static FormData labelFormData(Control control){
		FormData data = new FormData();
		if(control == null){
			data.right = new FormAttachment(0, 5);
		}else{
			data.right = new FormAttachment(control, -5);
		}
		data.top = new FormAttachment(0, 7);
		return data;
	}
	
	public static FormLayout formLayout(int marginHeight, int marginWidth){
		FormLayout innerlayout= new FormLayout ();
	    innerlayout.marginHeight = marginHeight;
	    innerlayout.marginWidth = marginWidth;
		return innerlayout;
	}
	
	public static FormData propertyFormData(Control control){
		if(control == null){
			return formData(0, 0, 0, 0, 100, 0, null, -1);
		}else{
			return formData(0, 0, control, 5, 100, 0, null, -1);
		}
	}
	public static FormData propertyKeyData(Control control){
		if(control == null){
			return formData(0, 3, 0, 8, null, -1, null, -1, 200);
		}else{
			return formData(0, 3, control, 11, null, -1, null, -1, 200);
		}
	}
	public static FormData propertyValueData(Control top, Control left){
		if(top == null){
			return formData(left, 3, 0, 6, 100, -3, null, -1);
		}else{
			return formData(left, 3, top, 10, 100, -3, null, -1);
		}
	}
	public static FormData propertyYNValueData(Control top, Control left){
		if(top == null){
			return formData(left, 3, 0, 5, null, -1, null, -1);
		}else{
			return formData(left, 3, top, 9, null, -1, null, -1);
		}
	}
	
	public static FormData formData(Object lobj, int loff, Object tobj, int toff, Object robj, int roff, Object bobj, int boff){
		return formData(lobj, loff, tobj, toff, robj, roff, bobj, boff, -1);
	}
	public static FormData formData(Object lobj, int loff, Object tobj, int toff, Object robj, int roff, Object bobj, int boff, int width){
		return formData(lobj, loff, tobj, toff, robj, roff, bobj, boff, width, -1);
	}
	public static FormData formData(Object lobj, int loff, Object tobj, int toff, Object robj, int roff, Object bobj, int boff, int width, int height){
		FormData data = new FormData();
		if(lobj instanceof Control){
			data.left = new FormAttachment((Control)lobj, loff);
		}else if(lobj instanceof Integer){
			data.left = new FormAttachment(Integer.parseInt(lobj.toString()), loff);
		}
		if(tobj instanceof Control){
			data.top = new FormAttachment((Control)tobj, toff);
		}else if(tobj instanceof Integer){
			data.top = new FormAttachment(Integer.parseInt(tobj.toString()), toff);
		}
		if(robj instanceof Control){
			data.right = new FormAttachment((Control)robj, roff);
		}else if(robj instanceof Integer){
			data.right = new FormAttachment(Integer.parseInt(robj.toString()), roff);
		}
		if(bobj instanceof Control){
			data.bottom = new FormAttachment((Control)bobj, boff);
		}else if(bobj instanceof Integer){
			data.bottom = new FormAttachment(Integer.parseInt(bobj.toString()), boff);
		}
		if(width > 0){
			data.width = width;
		}
		if(height > 0){
			data.height = height;
		}
		return data;
	}
	
	public static GridData gridData(int horAlign){
		GridData gridData = new GridData();
		gridData.horizontalAlignment = horAlign;
		return gridData;
	}
	
    public static int [] getScreenSize(){
    	int [] size = new int[2];
    	GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    	size[0] = gd.getDisplayMode().getWidth();
    	size[1] = gd.getDisplayMode().getHeight();
    	return size;
    }	
}
