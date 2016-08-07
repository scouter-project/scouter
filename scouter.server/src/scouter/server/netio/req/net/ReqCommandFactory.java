package scouter.server.netio.req.net;

import scouter.net.NetCafe;

public class ReqCommandFactory {
	static public ReqCommand makeReqCommand(int cmd){
		switch(cmd){
		case NetCafe.TCP_SEND_STACK:
			return new TcpSendStack();
		}
		return null;
	}
}
