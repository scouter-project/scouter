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
package scouter.client.heapdump.actions;


import org.eclipse.jface.action.Action;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbenchWindow;

import scouter.client.net.TcpProxy;
import scouter.client.util.ConsoleProxy;
import scouter.client.util.ExUtil;
import scouter.client.util.ImageUtil;
import scouter.lang.pack.MapPack;
import scouter.lang.value.BooleanValue;
import scouter.net.RequestCmd;
import scouter.util.CastUtil;

public class HeapDumpDeleteAction extends Action {
	public final static String ID = HeapDumpDeleteAction.class.getName();

	@SuppressWarnings("unused")
	private final IWorkbenchWindow window;
	@SuppressWarnings("unused")
	private String key;
	private int objHash;
	private String delfileName;
	private int serverId;
	
	public HeapDumpDeleteAction(IWorkbenchWindow window, String label, String key, int objHash, String delfileName, Image image, int serverId) {
		this.window = window;
		this.key = key;
		this.objHash = objHash;
		this.delfileName = delfileName;
		this.serverId = serverId;

		setText(label);
		setId(ID);
		setImageDescriptor(ImageUtil.getImageDescriptor(image));

	}

	public void run() {
		ExUtil.exec(new Runnable() {
			public void run() {
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				try {
					MapPack param = new MapPack();
					param.put("objHash", objHash);
					param.put("delfileName", delfileName);
					MapPack out = (MapPack) tcp.getSingle(RequestCmd.OBJECT_DELETE_HEAP_DUMP, param);
					if(out != null){
						if(!CastUtil.cboolean((BooleanValue)out.get("success"))){
							ConsoleProxy.infoSafe(out.getText("msg"));
						}
					}
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
			}
		});
	}
}
