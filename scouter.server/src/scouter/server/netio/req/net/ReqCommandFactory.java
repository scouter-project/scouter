package scouter.server.netio.req.net;

import scouter.net.NetCafe;
import scouter.server.Configure;
import scouter.server.Logger;

public class ReqCommandFactory {
	static public ReqCommand makeReqCommand(int cmd){
        if (Configure.getInstance().log_udp_batch) {
            Logger.println(new StringBuilder(100).append("Batch CMD: ").append(cmd).toString());
        }
		switch(cmd){
		case NetCafe.TCP_SEND_STACK:
			return new TcpSendStack();
		}
		return null;
	}
}
