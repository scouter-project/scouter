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
package scouter.client.views;

import java.util.ArrayList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Images;
import scouter.client.model.ServerDataProxy;
import scouter.client.util.ChartUtil;
import scouter.client.util.ColoringWord;
import scouter.client.util.ImageUtil;
import scouter.client.util.TableControlAdapter;
import scouter.client.util.CustomLineStyleListener;
import scouter.client.util.UIUtil;
import scouter.lang.pack.MapPack;
import scouter.lang.value.DecimalValue;
import scouter.lang.value.DoubleValue;
import scouter.lang.value.FloatValue;
import scouter.lang.value.TextValue;
import scouter.lang.value.Value;
import scouter.util.CastUtil;
import scouter.util.FormatUtil;


public class ServerThreadDetailView extends ViewPart {
	public static final String ID = ServerThreadDetailView.class.getName();

	private ArrayList<ColoringWord> defaultHighlightings;
	CustomLineStyleListener listener;
	
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		initializeColoring();
	}
	
	public void initializeColoring(){
		defaultHighlightings = new ArrayList<ColoringWord>();
		
		defaultHighlightings.add(new ColoringWord("javax.servlet.http.HttpServlet", SWT.COLOR_BLUE, false));
		defaultHighlightings.add(new ColoringWord("org.apache.jasper.servlet.JspServlet", SWT.COLOR_BLUE, false));
		defaultHighlightings.add(new ColoringWord("java.lang.Thread.sleep", SWT.COLOR_RED, false));
	}
	
	public void createPartControl(Composite parent) {

		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(ChartUtil.gridlayout(1));
		comp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL));

		table = build(comp);
		table.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL));
		table.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.F5) {
					reload();
				}
			}
		});

		stacktrace = new StyledText(comp, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		stacktrace.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL));

		stacktrace.setText("");
		stacktrace.setFont(new Font(null, "verdana", 10, 0));
		stacktrace.setMargins(10, 10, 10, 10);
	
		listener = new CustomLineStyleListener(false, defaultHighlightings, false);
		stacktrace.addLineStyleListener(listener);
		
		IToolBarManager man = getViewSite().getActionBars().getToolBarManager();
		man.add(new Action("reload",ImageUtil.getImageDescriptor(Images.refresh)) {
			public void run() {
				reload();
			}
		});
		
		comp.addControlListener(new TableControlAdapter(table, cols, new int[]{3, -1}));
	}

	@SuppressWarnings("unused")
	private String title = "";
	private long threadid;
	private int serverId;

	public void setInput(String title, long threadid, int serverId) {
		this.title = title;
		this.threadid = threadid;
		this.serverId = serverId;
		reload();
	}

	protected Table table;
	protected StyledText stacktrace;

	TableColumn[] cols;
	private Table build(Composite parent) {
		table = new Table(parent, SWT.BORDER | SWT.WRAP | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		cols = new TableColumn[9];
		cols[0] = UIUtil.create(table, SWT.LEFT, "Key", cols.length, 0, true, 150);
		cols[1] = UIUtil.create(table, SWT.NONE, "Value", cols.length, 1, true, 600);

		return table;
	}
	
	protected void reload() {
		table.removeAll();
		MapPack mp = ServerDataProxy.getThreadDetail(threadid, serverId);
		
		String[] names = scouter.util.SortUtil.sort_string(mp.keys(), mp.size());
		for (int i = 0, j = 0; i < names.length; i++) {
			String key = names[i];
			Value value = mp.get(key);
			if ("Stack Trace".equals(key)) {
				stacktrace.setText(CastUtil.cString(value));
//				JavaColor.setJavaCode(stacktrace, new Cast(value).cString());
				continue;
			}
			String text = null;
			TableItem ti = new TableItem(table, SWT.NONE, j++);
			if (value instanceof TextValue) {
				text = CastUtil.cString(value);
				ti.setText(0, key);
				ti.setText(1, text);
			} else {
				if (value instanceof DecimalValue) {
					text = FormatUtil.print(value, "#,##0");
				} else if (value instanceof DoubleValue || value instanceof FloatValue) {
					text =FormatUtil.print(value, "#,##0.0##");
				}
				ti.setText(new String[] { key, text });
			}
		

		}

	}

	public void setFocus() {
		
	}

	@Override
	public void dispose() {
		super.dispose();
	}

}