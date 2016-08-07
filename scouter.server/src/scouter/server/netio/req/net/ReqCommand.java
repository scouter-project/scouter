package scouter.server.netio.req.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;

public interface ReqCommand {
	public void process(DataInputStream in, DataOutputStream out);
}
