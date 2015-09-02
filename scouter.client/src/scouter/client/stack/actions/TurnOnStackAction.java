package scouter.client.stack.actions;

import org.eclipse.jface.action.Action;

import scouter.client.net.TcpProxy;
import scouter.client.util.ExUtil;
import scouter.lang.pack.MapPack;
import scouter.net.RequestCmd;
import scouter.util.DateUtil;

public class TurnOnStackAction extends Action {
	public final static String ID = TurnOnStackAction.class.getName();
	
	int serverId;
	int objHash;

	public TurnOnStackAction(int serverId, int objHash) {
		this.serverId = serverId;
		this.objHash = objHash;
		this.setText("Turn on(5min)");
	}
	
	public void run(){
		ExUtil.asyncRun(new Runnable() {
			public void run() {
				TcpProxy tcp = TcpProxy.getTcpProxy(serverId);
				try {
					MapPack param = new MapPack();
					param.put("objHash", objHash);
					param.put("time", DateUtil.MILLIS_PER_FIVE_MINUTE);
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