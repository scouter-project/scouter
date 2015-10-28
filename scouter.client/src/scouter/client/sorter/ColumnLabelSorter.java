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
package scouter.client.sorter;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import scouter.util.CastUtil;

public class ColumnLabelSorter extends ViewerComparator {
	public static final int ORDER_ASC = 1;
	public static final int NONE = 0;
	public static final int ORDER_DESC = -1;

	private TableColumn col = null;
	private int colIndex = 0;
	ICustomCompare custom;
	TableViewer viewer;
	Table table;
	private int dir = 0;
	
	
	public ColumnLabelSorter(TableViewer viewer) {
		this.viewer = viewer;
		this.table = viewer.getTable();
	}
	
	public ColumnLabelSorter setCustomCompare(ICustomCompare custom) {
		this.custom = custom;
		return this;
	}
	
	@Override
	public int compare(Viewer viewer, Object o1, Object o2) {
		if (dir == NONE || this.col == null) {
			return 0;
		}
		if (custom == null) {
			return dir * compareNormal(o1, o2);
		} else {
			return dir * custom.doCompare(this.col, this.colIndex, o1, o2);
		}
	}
	
	public void setColumn(TableColumn clickedColumn) {
		if (col == clickedColumn) {
			dir = dir * -1;
		} else {
			this.col = clickedColumn;
			this.dir = ORDER_ASC;
		}
		TableColumn[] cols = table.getColumns();
		int colLen = cols.length;;
		for (int i = 0; i < colLen; i++) {
			if (cols[i] == this.col) {
				colIndex = i;
				break;
			}
		}
		table.setSortColumn(clickedColumn);
		switch (dir) {
		case ORDER_ASC:
			table.setSortDirection(SWT.UP);
			break;
		case ORDER_DESC:
			table.setSortDirection(SWT.DOWN);
			break;
		}
		viewer.refresh();
	}
	

	protected int compareNormal(Object e1, Object e2) {
		try {
			ColumnLabelProvider labelProvider = (ColumnLabelProvider) viewer.getLabelProvider(colIndex);
			String t1 = labelProvider.getText(e1);
			String t2 = labelProvider.getText(e2);
			Boolean isNumber = (Boolean) this.col.getData("isNumber");
			if (isNumber != null && isNumber.booleanValue()) {
				t1 = numonly(t1);
				t2 = numonly(t2);
				double n1 = CastUtil.cdouble(t1);
				double n2 = CastUtil.cdouble(t2);
				return n1 == n2 ? 0 : (n1 > n2) ? 1 : -1;
			} else {
				if (t1 == null) t1 = "";
				if (t2 == null) t2 = "";
			}
			return t1.compareTo(t2);
		} catch(Throwable th) { }
		return 0;
	}
	
	public static String numonly(String t) {
		if (t == null) {
			return "";
		}
		char[] c = t.toCharArray();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < c.length; i++) {
			switch (c[i]) {
			case '-':
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
			case '.':
				sb.append(c[i]);
			}
		}
		return sb.toString();
	}
	
	public static interface ICustomCompare {
		public int doCompare(TableColumn col, int index, Object o1, Object o2);
	}
}
