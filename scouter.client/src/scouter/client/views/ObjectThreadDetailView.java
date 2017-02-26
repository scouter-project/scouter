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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import scouter.client.Images;
import scouter.client.model.AgentDataProxy;
import scouter.client.net.TcpProxy;
import scouter.client.popup.EditableMessageDialog;
import scouter.client.server.GroupPolicyConstants;
import scouter.client.server.ServerManager;
import scouter.client.util.*;
import scouter.client.util.UIUtil.ViewWithTable;
import scouter.lang.pack.MapPack;
import scouter.lang.value.*;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;
import scouter.util.FormatUtil;

import java.util.ArrayList;


public class ObjectThreadDetailView extends ViewPart implements ViewWithTable{
	public static final String ID = ObjectThreadDetailView.class.getName();

	private int objHash;
	private long threadid;
	private long txid;
	private int serverId;
	
	private ArrayList<ColoringWord> defaultHighlightings;
	CustomLineStyleListener listener;
	
	Button interruptBtn, stopBtn, resumeBtn, suspendBtn;
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
		String secId = site.getSecondaryId();
		String[] ids = secId.split("&");
		serverId = Integer.valueOf(ids[0]);
		objHash = Integer.valueOf(ids[1]);
	}
	
	public void initializeColoring(){
		defaultHighlightings = new ArrayList<ColoringWord>();
		
		defaultHighlightings.add(new ColoringWord("javax.servlet.http.HttpServlet", SWT.COLOR_BLUE, false));
		defaultHighlightings.add(new ColoringWord("org.apache.jasper.servlet.JspServlet", SWT.COLOR_BLUE, false));
		defaultHighlightings.add(new ColoringWord("java.lang.Thread.sleep", SWT.COLOR_RED, false));
		
		defaultHighlightings.add(new ColoringWord("java.lang", SWT.COLOR_BLUE, false));
		defaultHighlightings.add(new ColoringWord("java.util", SWT.COLOR_BLUE, false));
	}
	
	public void createPartControl(final Composite parent) {
		initializeColoring();
		SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
		sashForm.SASH_WIDTH = 1;
		
		Composite composite = new Composite(sashForm, SWT.NONE);
		composite.setLayout(new GridLayout(1, true));
		
		Composite upperComp = new Composite(composite, SWT.NONE);
		upperComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		upperComp.setLayout(UIUtil.formLayout(3, 3));
		
		if (ServerManager.getInstance().getServer(serverId).isAllowAction(GroupPolicyConstants.ALLOW_KILLTRANSACTION)) {
			resumeBtn = new Button(upperComp, SWT.PUSH);
			resumeBtn.setText("&Resume");
			resumeBtn.setImage(Images.WARN);
			resumeBtn.setLayoutData(UIUtil.formData(null, -1, null, -1, 100, -5, null, -1, 100));
			resumeBtn.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (MessageDialog.openConfirm(parent.getShell(), "Resume Thread", "This thread will be resumed.(This may result in throwing a SecurityException) Continue?")) {
						controlThread("resume");
					}
				}
			});
			
			suspendBtn = new Button(upperComp, SWT.PUSH);
			suspendBtn.setText("&Suspend");
			suspendBtn.setImage(Images.WARN);
			suspendBtn.setLayoutData(UIUtil.formData(null, -1, null, -1, resumeBtn, -5, null, -1, 100));
			suspendBtn.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (MessageDialog.openConfirm(parent.getShell(), "Suspend Thread", "This thread will be suspended.(This may result in throwing a SecurityException) Continue?")) {
						controlThread("suspend");
					}
				}
			});
			
			stopBtn = new Button(upperComp, SWT.PUSH);
			stopBtn.setText("&Stop");
			stopBtn.setImage(Images.WARN);
			stopBtn.setLayoutData(UIUtil.formData(null, -1, null, -1, suspendBtn, -5, null, -1, 100));
			stopBtn.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (MessageDialog.openConfirm(parent.getShell(), "Stop Thread", "This thread will be terminated.(This feature requires a pre-test) Continue?")) {
						controlThread("stop");
					}
				}
			});
			
			interruptBtn = new Button(upperComp, SWT.PUSH);
			interruptBtn.setText("&Interrupt");
			interruptBtn.setLayoutData(UIUtil.formData(null, -1, null, -1, stopBtn, -5, null, -1, 100));
			interruptBtn.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					if (MessageDialog.openConfirm(parent.getShell(), "Interrupt Thread", "This thread will be interrupted. Continue?")) {
						controlThread("interrupt");
					}
				}
			});
		}
		
		table = build(composite);
		table.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				TableItem[] items = table.getSelection();
				if (items == null || items.length < 1) {
					return;
				}
				String key = items[0].getText(0);
				String content = items[0].getText(1);
				new EditableMessageDialog().show(key, content);
			}
		});
		table.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.F5) {
					reload();
				}
			}
		});

		stacktrace = new StyledText(sashForm, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);

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
	}
	
	public void setInput(long threadId, long txid) {
		this.threadid = threadId;
		this.txid = txid;
		if(threadId != 0L) {
			this.setPartName("Thread Detail[" + threadid + "]");
		} else {
			this.setPartName("Deferred Tx Detail[" + txid + "]");
		}
		table.removeAll();
		stacktrace.setText("");
		reload();
	}

	protected Table table;
	protected StyledText stacktrace;

	private Table build(Composite parent) {
		table = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		TableColumn[] cols = new TableColumn[9];
		cols[0] = UIUtil.create(table, SWT.LEFT, "Key", cols.length, 0, true, 150, this);
		cols[1] = UIUtil.create(table, SWT.NONE, "Value", cols.length, 1, true, 600, this);

		return table;
	}
	
	protected void reload() {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				final MapPack mp = AgentDataProxy.getThreadDetail(objHash, threadid, txid, serverId);
				setThreadDetailContens(mp);
			}
		});
	}
	
	private void setThreadDetailContens (final MapPack pack) {
		ExUtil.exec(table, new Runnable() {
			public void run() {
				table.removeAll();
				stacktrace.setText("");
				int time = CastUtil.cint(pack.get("Service Elapsed"));
				boolean serviceThread = false;
				String[] names = scouter.util.SortUtil.sort_string(pack.keys(), pack.size());
				for (int i = 0, j = 0; i < names.length; i++) {
					String key = names[i];
					Value value = pack.get(key);
					if ("Stack Trace".equals(key)) {
						stacktrace.setText(CastUtil.cString(value));
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
							text = FormatUtil.print(value, "#,##0.0##");
						}
						ti.setText(new String[] { key, text });
					}
					if (key.startsWith("Service")) {
						if (time > 8000) {
							ti.setForeground(ColorUtil.getInstance().getColor(SWT.COLOR_RED));
						} else if (time > 3000) {
							ti.setForeground(ColorUtil.getInstance().getColor(SWT.COLOR_MAGENTA));
						} else {
							ti.setForeground(ColorUtil.getInstance().getColor(SWT.COLOR_BLUE));
						}
						serviceThread = true;
					}
				}
				if (stopBtn != null && interruptBtn != null) {
					stopBtn.setEnabled(serviceThread);
					interruptBtn.setEnabled(serviceThread);
					resumeBtn.setEnabled(serviceThread);
					suspendBtn.setEnabled(serviceThread);
				}
				sortTable();
			}
		});
	}
	
	private void controlThread(final String action) {
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				MapPack p = null;
				try {
					MapPack param = new MapPack();
					param.put("objHash", objHash);
					param.put("id", threadid);
					param.put("action", action);
					p = (MapPack) tcp.getSingle(RequestCmd.OBJECT_THREAD_CONTROL, param);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				if (p != null) {
					setThreadDetailContens(p);
				}
			}
		});
	}

	public void setFocus() {
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	boolean asc;
	int col_idx;
	boolean isNum;
	
	public void setSortCriteria(boolean asc, int col_idx, boolean isNum) {
		this.asc = asc;
		this.col_idx = col_idx;
		this.isNum = isNum;
		
	}

	public void setTableItem(TableItem t) {
	}

	public void sortTable(){
		int col_count = table.getColumnCount();
		TableItem[] items = table.getItems();
		if (isNum) {
			new SortUtil(asc).sort_num(items, col_idx, col_count);
		} else {
			new SortUtil(asc).sort_str(items, col_idx, col_count);
		}
	}
}
