package scouter.client.stack.actions;

import org.eclipse.jface.action.Action;

import scouter.client.net.TcpProxy;
import scouter.client.util.ExUtil;
import scouter.lang.pack.MapPack;
import scouter.net.RequestCmd;
import scouter.util.DateUtil;

public class TurnOffStackAction extends Action {
	public final static String ID = TurnOffStackAction.class.getName();
	
	int serverId;
	int objHash;

	public TurnOffStackAction(int serverId, int objHash) {
		this.serverId = serverId;
		this.objHash = objHash;
		this.setText("Turn off");
	}
	
	public void run(){
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				try {
					MapPack param = new MapPack();
					param.put("objHash", objHash);
					tcp.getSingle(RequestCmd.PSTACK_ON, param);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					TcpProxy.putTcpProxy(tcp);
				}
				
			}
		});
	}
}