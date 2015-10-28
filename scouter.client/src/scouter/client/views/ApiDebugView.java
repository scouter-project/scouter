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

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import scouter.util.StringUtil;
import scouter.client.net.TcpProxy;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.UIUtil;
import scouter.lang.pack.MapPack;
import scouter.lang.pack.Pack;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;

public class ApiDebugView extends ViewPart  {
	
	public final static String ID = ApiDebugView.class.getName();

	private Text params, command, results;
	private int serverId;
	
	String COMMAND = "";
	
	public void init(IViewSite site) throws PartInitException {
		super.init(site);
	}

	public void createPartControl(Composite parent) {
		parent.setLayout(UIUtil.formLayout(5, 5));
		
		Button callBtn = new Button(parent, SWT.NONE);
		callBtn.setText("Call");
		callBtn.setLayoutData(UIUtil.formData(null, 0, 0, 5, 100, -5, null, -1));
		callBtn.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				makeParamAndCall();
			}
			
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		params = new Text(parent, SWT.BORDER);
		params.setLayoutData(UIUtil.formData(0, 200, 0, 7, callBtn, -5, null, -1));
		
		command = new Text(parent, SWT.BORDER);
		command.setLayoutData(UIUtil.formData(0, 5, 0, 7, params, -5, null, -1));
		
		results = new Text(parent, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		results.setLayoutData(UIUtil.formData(0, 5, command, 5, 100, -5, 100, -5));
		
	}
	
	/*
	 *  Parameter example
	 *  I:objHash=1232242,S:objType=tomcat
	 */
	
	protected void makeParamAndCall() {
		String input = params.getText();
		MapPack param = null;
		try{
			if(StringUtil.isNotEmpty(input)){
				param = new MapPack();
				String[] inputs = input.split(",");
				for(int i = 0 ; i < inputs.length ; i++){
					if(inputs[i].indexOf("=") == -1 || inputs[i].indexOf(":") == -1){
						continue;
					}
					String[] keyValue = inputs[i].split("=");
					String type = StringUtil.removeWhitespace(keyValue[0].split(":")[0]);
					String key = StringUtil.removeWhitespace(keyValue[0].split(":")[1]);
					String value = StringUtil.removeWhitespace(keyValue[1]);
					
					if(type.equals("I")){
						param.put(key, CastUtil.clong(value));	
					}else if(type.equals("S")){
						param.put(key, value);		
					}
				}
			}
			load(param);
		}catch(Exception e){
			ConsoleProxy.errorSafe(e.toString());
		}
	}

	public void setInput(int serverId, int objHash) {
		this.serverId = serverId;
		
		if(objHash != 0){
			params.setText("I:objHash="+objHash+", ");
			command.setText(RequestCmd.DEBUG_AGENT);
		}else{
			command.setText(RequestCmd.DEBUG_SERVER);
		}
		
	}
	
	public void load(final MapPack param) {
		ExUtil.exec(new Runnable() {
			public void run() {
				List<Pack> packList = null;
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				try {
					packList = tcp.process(command.getText(), param);
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				StringBuilder sb = new StringBuilder();
				if (packList != null && packList.size() > 0) {
					for (Pack pack : packList) {
						sb.append(pack.toString());
						sb.append("\n");
					}
				}
				results.setText(sb.toString());
			}
		});
	}

	public void setFocus() {
		
	}

}