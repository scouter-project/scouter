package scouter.agent.netio.data.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

import scouter.agent.Configure;
import scouter.agent.netio.request.ReqestHandlingProxy;
import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.lang.pack.Pack;
import scouter.net.NetCafe;
import scouter.net.TcpFlag;
import scouter.util.FileUtil;

public class TcpWorker implements Runnable {
	public static AtomicInteger LIVE = new AtomicInteger();

	public static String localAddr = null;
	public int objHash = Configure.getInstance().objHash;

	public void run() {
		if (socket == null)
			return;
		try {
			process(socket);
		} catch (Throwable t) {
		} finally {
			FileUtil.close(socket);
			socket = null;
			LIVE.decrementAndGet();
		}
	}

	protected Socket socket = null;

	public boolean prepare() {
		Configure conf = Configure.getInstance();
		String host = conf.server_addr;
		int port = conf.server_tcp_port;
		int so_timeout = conf.server_tcp_so_timeout;
		int connection_timeout = conf.server_tcp_connection_timeout;

		socket = new Socket();
		try {
			socket.connect(new InetSocketAddress(host, port), connection_timeout);
			socket.setSoTimeout(so_timeout);
			if (localAddr == null) {
				localAddr = socket.getLocalAddress().getHostAddress() + ":0";
			}
			LIVE.incrementAndGet();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private void process(Socket socket) throws IOException {
		DataInputX in = null;
		DataOutputX out = null;
		try {
			in = new DataInputX(new BufferedInputStream(socket.getInputStream()));
			out = new DataOutputX(new BufferedOutputStream(socket.getOutputStream()));

			out.writeInt(NetCafe.TCP_AGENT);
			out.writeInt(objHash);
			out.flush();

			while (objHash == Configure.getInstance().objHash) {
				String cmd = in.readText();
				Pack parameter = (Pack) in.readPack();
				Pack res = ReqestHandlingProxy.process(cmd, parameter, in, out);
				if (res != null) {
					out.writeByte(TcpFlag.HasNEXT);
					out.writePack(res);
				}
				out.writeByte(TcpFlag.NoNEXT);
				out.flush();
			}
		} finally {
			FileUtil.close(in);
			FileUtil.close(out);
		}
	}

}
