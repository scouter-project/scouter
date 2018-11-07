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
package scouter.client.xlog.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import scouter.client.Activator;
import scouter.client.model.XLogData;
import scouter.client.xlog.ProfileText;
import scouter.client.xlog.actions.OpenXLogProfileJob;
import scouter.client.xlog.actions.OpenXLogThreadProfileJob;
import scouter.lang.step.Step;
import scouter.util.DateUtil;
import scouter.util.Hexa32;
import scouter.util.StringUtil;
import scouter.util.SystemUtil;


public class XLogThreadProfileView extends ViewPart {
	public static final String ID = XLogThreadProfileView.class.getName();
	private StyledText text;
	private String date;
	
	IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	
	Step[] steps;
	
	public void createPartControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, true));
		
		text = new StyledText(composite, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		text.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		text.setText("");
		if(SystemUtil.IS_MAC_OSX){
		    text.setFont(new Font(null, "Courier New", 12, SWT.NORMAL));		
		}else{
		    text.setFont(new Font(null, "Courier New", 10, SWT.NORMAL));
		}
		text.setBackgroundImage(Activator.getImage("icons/grid.jpg"));
	}

	public void setInput(final XLogData data, Step[] steps, long threadId, int serverId) {
		this.date = DateUtil.yyyymmdd(data.p.endTime);
		this.steps = steps;
		setPartName(Hexa32.toString32(threadId));
		text.setText("");
		ProfileText.buildThreadProfile(data, text, steps);
		text.addListener(SWT.MouseDown, new Listener(){
			public void handleEvent(Event event) {
				try {
					int offset = text.getOffsetAtLocation(new Point (event.x, event.y));
					StyleRange style = text.getStyleRangeAtOffset(offset);
					if (style != null && style.underline && style.underlineStyle == SWT.UNDERLINE_LINK) {
						int line = text.getLineAtOffset(offset);
						String fulltxt = text.getLine(line);
						if (StringUtil.isNotEmpty(fulltxt)) {
							if (fulltxt.endsWith(">") && fulltxt.contains("call:")) {
								int startIndex = fulltxt.lastIndexOf("<");
								if (startIndex > -1) {
									int endIndex = fulltxt.lastIndexOf(">");
									String txIdStr = fulltxt.substring(startIndex + 1, endIndex);
									long txid = Hexa32.toLong32(txIdStr);
									new OpenXLogProfileJob(XLogThreadProfileView.this.getViewSite().getShell().getDisplay(), XLogThreadProfileView.this.date, txid, data.p.gxid).schedule();
								}
							}else if (fulltxt.endsWith(">") && fulltxt.contains("thread:")) {
								int startIndex = fulltxt.lastIndexOf("<");
								if (startIndex > -1) {
									int endIndex = fulltxt.lastIndexOf(">");
									String txIdStr = fulltxt.substring(startIndex + 1, endIndex);
									long threadTxid = Hexa32.toLong32(txIdStr);
									new OpenXLogThreadProfileJob(data,  threadTxid).schedule();
								}
							}
						}
					}
				} catch (IllegalArgumentException e) {
					// no character under event.x, event.y
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
	
	public static void main(String[] args) {
		String fulltxt = "call: UCON:http://127.0.0.1:8080/e2end.jsp 117 ms <z2bcfcg3hfcm0h>";
		int startIndex = fulltxt.lastIndexOf("<");
		if (startIndex > -1) {
			int endIndex = fulltxt.lastIndexOf(">");
			String txIdStr = fulltxt.substring(startIndex + 1, endIndex);
			System.out.println(txIdStr);
		}
	}
}