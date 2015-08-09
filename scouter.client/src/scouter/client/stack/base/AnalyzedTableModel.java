/*
 *  Copyright 2015 LG CNS.
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
package scouter.client.stack.base;

import java.util.ArrayList;
import javax.swing.table.AbstractTableModel;

import scouter.client.stack.data.StackAnalyzedValue;
import scouter.client.stack.utils.NumberUtils;

@SuppressWarnings("serial")
public class AnalyzedTableModel extends AbstractTableModel {
    private static int DEFINED_COLUMNS = 4;
    
    private ArrayList<StackAnalyzedValue> m_elements = null;
        
    private String[] m_columnNames = {"Count", "Internal %", "External %","Class.function"};
    
    public AnalyzedTableModel() {
    }
    
    public void setElements(ArrayList<StackAnalyzedValue> info) {
    	m_elements = info;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
    	StackAnalyzedValue value = m_elements.get(rowIndex);
    	
        switch(columnIndex) {
        case 0 :
            return new Integer(value.getCount());
        case 1 :
            return NumberUtils.intToPercent(value.getIntPct());
        case 2 :
            return NumberUtils.intToPercent(value.getExtPct());
        case 3 :
            return value.getValue();
    }
    return null;
    }
    
    public String getColumnName(int col) {
        return m_columnNames[col];
    }

    public int getRowCount() {
    	return m_elements.size();
    }

    public int getColumnCount() {
        return DEFINED_COLUMNS;
    }
 
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public Class getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }   
}