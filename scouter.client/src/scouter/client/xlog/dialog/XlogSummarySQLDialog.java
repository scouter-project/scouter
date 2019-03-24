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
package scouter.client.xlog.dialog;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import scouter.client.model.TextProxy;
import scouter.client.model.XLogData;
import scouter.client.popup.SQLFormatDialog;
import scouter.client.util.UIUtil;
import scouter.lang.step.HashedMessageStep;
import scouter.lang.step.SqlStep;
import scouter.lang.step.SqlStep3;
import scouter.lang.step.Step;
import scouter.lang.step.StepEnum;
import scouter.lang.step.StepSingle;
import scouter.util.FormatUtil;
import scouter.util.Hexa32;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

public class XlogSummarySQLDialog extends Dialog {
	private Table   sqlTable;
	private Table   bindTable;
	private Step[]	steps;
	private XLogData xperf;
	private HashMap<Integer, SQLSumData> sqlMap = new HashMap<Integer, SQLSumData>(); 
	
	public XlogSummarySQLDialog(Shell shell,  Step[] steps, XLogData xperf) {
		super(shell);
		this.xperf = xperf;
		this.steps = steps;
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite container =  (Composite) super.createDialogArea(parent);
		FillLayout layout = new FillLayout();
		container.setLayout(layout);
		SashForm sashVertForm = new SashForm(container, SWT.VERTICAL);
		sashVertForm.SASH_WIDTH = 3;
		initSQLTable(sashVertForm);
		initBindTable(sashVertForm);

		sashVertForm.setWeights(new int [] {55, 45});

		processSqlData();
		displaySQLSumData();
		
		return container;
	}
	
	private void initSQLTable(Composite parent){
		sqlTable = new Table(parent, SWT.BORDER  | SWT.FULL_SELECTION);
		TableColumn tableColumn = new TableColumn(sqlTable, SWT.RIGHT);
		tableColumn.setText("Execs");
		tableColumn.setWidth(80);
		tableColumn = new TableColumn(sqlTable, SWT.RIGHT);
		tableColumn.setText("Binds");
		tableColumn.setWidth(80);
		tableColumn = new TableColumn(sqlTable, SWT.RIGHT);
		tableColumn.setText("Exec Time");
		tableColumn.setWidth(80);
		tableColumn = new TableColumn(sqlTable, SWT.RIGHT);
		tableColumn.setText("Fetch Time");
		tableColumn.setWidth(80);
		tableColumn = new TableColumn(sqlTable, SWT.RIGHT);
		tableColumn.setText("Total Rows");
		tableColumn.setWidth(80);
		tableColumn = new TableColumn(sqlTable, SWT.LEFT);
		tableColumn.setText("SQL Text");
		tableColumn.setWidth(600);
		sqlTable.setHeaderVisible(true);
		sqlTable.setVisible(true);
		
		sqlTable.addSelectionListener(new SelectionListener(){
			public void widgetDefaultSelected(SelectionEvent event) {
			}
			@SuppressWarnings("unchecked")
			public void widgetSelected(SelectionEvent event) {
				bindTable.clearAll();
				bindTable.setItemCount(0);
				
				TableItem item = (TableItem)event.item;
				if(item == null)
					return;
				
				Integer hash = (Integer)item.getData();
				ArrayList<BindSumData> list = getLBindSumDataList(hash.intValue());

				Collections.sort(list, new BindSumDataComp());
				
				String sqlText = sqlMap.get(hash).sqlText;
				if (sqlText != null) {
					sqlText = sqlText.replaceAll("(\r\n|\r|\n|\n\r)", " ");
				}
				TableItem bindItem;
		    	for(BindSumData value : list){
		    		bindItem = new TableItem(bindTable, SWT.BORDER);
		    		bindItem.setText(value.toTableInfo());
		    		bindItem.setData(sqlText);
		    	}
            }
		});		
	}
	
	private void initBindTable(Composite parent){
		bindTable = new Table(parent, SWT.BORDER  | SWT.FULL_SELECTION);
		TableColumn tableColumn = new TableColumn(bindTable, SWT.RIGHT);
		tableColumn.setText("Execs");
		tableColumn.setWidth(80);
		tableColumn = new TableColumn(bindTable, SWT.RIGHT);
		tableColumn.setText("Exec Time");
		tableColumn.setWidth(80);
		tableColumn = new TableColumn(bindTable, SWT.RIGHT);
		tableColumn.setText("Fetch Time");
		tableColumn.setWidth(80);
		tableColumn = new TableColumn(bindTable, SWT.RIGHT);
		tableColumn.setText("Total Rows");
		tableColumn.setWidth(80);
		tableColumn = new TableColumn(bindTable, SWT.LEFT);
		tableColumn.setText("Bind Variables");
		tableColumn.setWidth(600);
		bindTable.setHeaderVisible(true);
		bindTable.setVisible(true);
		bindTable.setToolTipText("Please double click on in order to view the complete SQL statement");
		bindTable.addMouseListener(new MouseListener(){
			public void mouseDoubleClick(MouseEvent event) {
				TableItem [] items = bindTable.getSelection();
				if(items.length == 0){
					return;
				}
				TableItem item = items[0];
				String sqlText = (String)item.getData();
				String binds = item.getText(4);
				new SQLFormatDialog().show(sqlText, null, binds);
			}

			@Override
			public void mouseDown(MouseEvent event) {
			}

			@Override
			public void mouseUp(MouseEvent event) {
			}			
		});
	}
	
	protected void displaySQLSumData(){
		sqlTable.clearAll();
		sqlTable.setItemCount(0);
		ArrayList<SQLSumData> list = getLSQLSumDataList();

		Collections.sort(list, new SQLSumDataComp());
		
        TableItem item;
    	for(SQLSumData value : list){
    		item = new TableItem(sqlTable, SWT.BORDER);
    		item.setText(value.toTableInfo());
    		item.setData(new Integer(value.hash));
    	}
	}

	protected void processSqlData(){
		StepSingle stepSingle;
		SqlStep sql;
		HashedMessageStep message;
		SQLSumData sqlSumData = null;
		BindSumData bindSumData = null;
		String bindParam;
	
		for (int i = 0; i < steps.length; i++) {
			stepSingle = (StepSingle)steps[i];

			switch(stepSingle.getStepType()){
			case StepEnum.SQL:
			case StepEnum.SQL2:
			case StepEnum.SQL3:
				sql = (SqlStep)stepSingle;
				
				sqlSumData = sqlMap.get(sql.hash);
				if(sqlSumData == null){
					sqlSumData = new SQLSumData();
					sqlSumData.hash = sql.hash;
					sqlSumData.sqlText = TextProxy.sql.getText(sql.hash);
					if (sqlSumData.sqlText != null) {
						sqlSumData.sqlText = sqlSumData.sqlText.replaceAll("(\r\n|\r|\n|\n\r)", " ");
					}
					sqlMap.put(sql.hash, sqlSumData);
				}
				sqlSumData.execs++;
				sqlSumData.execTime += sql.elapsed;
	
				bindSumData = sqlSumData.bindMap.get(sql.param);
				if(bindSumData == null){
					sqlSumData.binds++;
					bindSumData = new BindSumData();
					if(sql.param == null){
						bindParam = "[No Value]";
					}else{
						bindParam = sql.param;
					}
					bindSumData.bindText = bindParam;
					sqlSumData.bindMap.put(bindParam, bindSumData);
				}
				bindSumData.execs++;
				bindSumData.execTime += sql.elapsed;
				
				if(StepEnum.SQL3 == stepSingle.getStepType()){
			             int updatedCount = ((SqlStep3)stepSingle).updated;
			             if (updatedCount > SqlStep3.EXECUTE_RESULT_SET) {
			            	 sqlSumData.totalRows += updatedCount;
			            	 bindSumData.totalRows += updatedCount;
			             }
				}
				break;
			case  StepEnum.HASHED_MESSAGE:
				message = (HashedMessageStep)stepSingle;
				if(!"RESULT-SET-FETCH".equals(TextProxy.hashMessage.getText(message.hash))){
					continue;
				}
				if(sqlSumData != null){
					sqlSumData.fetchTime += message.time;
					sqlSumData.totalRows += message.value;
				}
				if(bindSumData != null){
					bindSumData.fetchTime += message.time;
					bindSumData.totalRows += message.value;
				}
				break;
			}
		}
	}
	
	private ArrayList<SQLSumData> getLSQLSumDataList(){
		ArrayList<SQLSumData> list = new ArrayList<SQLSumData>();
		Iterator<SQLSumData> itor = sqlMap.values().iterator();
		while(itor.hasNext()){
			list.add(itor.next());
		}
		return list;
	}

	private ArrayList<BindSumData> getLBindSumDataList(int hash){
		ArrayList<BindSumData> list = new ArrayList<BindSumData>();
		SQLSumData sqlSumData = sqlMap.get(hash);
		if(sqlSumData == null){
			return null;
		}
		HashMap<String, BindSumData> bindMap = sqlSumData.bindMap; 
		Iterator<BindSumData> itor = bindMap.values().iterator();
		while(itor.hasNext()){
			list.add(itor.next());
		}
		return list;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(new StringBuilder(100).append("SQL Statistics Summary-").append(TextProxy.service.getText(xperf.p.service)).append('(').append(Hexa32.toString32(xperf.p.txid)).append(")-").append(FormatUtil.print(xperf.p.elapsed, "#,##0")).append("ms").toString());
	}

	@Override
	protected boolean isResizable() {
		return true;
	}
	
	@Override
	protected Control createButtonBar(Composite parent){
		return null;
	}
	
	@Override
	protected  void initializeBounds(){
		int[] size = UIUtil.getScreenSize();
		this.getShell().setBounds((size[0]/2)-400, (size[1]/2)-200, 800, 400);
	}	
	
	private class SQLSumData {
		public int execs = 0;
		public int binds = 0;
		public int execTime = 0;
		public int fetchTime = 0;
		public int totalRows = 0;
		public String sqlText = null;
		public int hash = 0;
		public HashMap<String, BindSumData> bindMap = new HashMap<String, BindSumData>();
		
		public String[] toTableInfo(){
			String [] values = new String[6];
			values[0] = FormatUtil.print(execs, "#,##0");
			values[1] = FormatUtil.print(binds, "#,##0");
			values[2] = FormatUtil.print(execTime, "#,##0");
			values[3] = FormatUtil.print(fetchTime, "#,##0");
			values[4] = FormatUtil.print(totalRows, "#,##0");
			values[5] = sqlText;
			return values;
		}
	}
	
	private class BindSumData {
		public int execs = 0;
		public int execTime = 0;
		public int fetchTime = 0;
		public int totalRows = 0;
		public String bindText = null;
		
		public String[] toTableInfo(){
			String [] values = new String[6];
			values[0] = FormatUtil.print(execs, "#,##0");
			values[1] = FormatUtil.print(execTime, "#,##0");
			values[2] = FormatUtil.print(fetchTime, "#,##0");
			values[3] = FormatUtil.print(totalRows, "#,##0");
			values[4] = bindText;
			return values;
		}
		
	}
	
	private class SQLSumDataComp  implements Comparator<SQLSumData>{
		public int compare(SQLSumData o1, SQLSumData o2) {
			if(o1.execTime > o2.execTime)
				return -1;
			else if(o1.execTime < o2.execTime)
				return 1;
			return 0;
		}
	}
	
	private class BindSumDataComp  implements Comparator<BindSumData>{
		public int compare(BindSumData o1, BindSumData o2) {
			if(o1.execTime > o2.execTime)
				return -1;
			else if(o1.execTime < o2.execTime)
				return 1;
			return 0;
		}
	}
}