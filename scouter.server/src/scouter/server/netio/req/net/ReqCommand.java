package scouter.server.netio.req.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ReqCommand {
	public void process(InputStream in, OutputStream out) throws IOException;
}
