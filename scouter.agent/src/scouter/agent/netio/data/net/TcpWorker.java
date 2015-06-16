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
import scouter.util.ThreadUtil;

public class TcpWorker implements Runnable {
	public static AtomicInteger ACTIVE = new AtomicInteger();

	public String host;
	public int port;
	public int so_timeout = 30000;

	public TcpWorker() {

	}

	public void run() {
		if (socket == null)
			return;
		try {
			process(socket);
		} catch (Throwable t) {
		} finally {
			FileUtil.close(socket);
			ACTIVE.decrementAndGet();
		}
	}

	protected Socket socket = null;

	public boolean prepare() {
		socket = new Socket();
		try {
			socket.connect(new InetSocketAddress(host, port));
			socket.setSoTimeout(so_timeout);
			ACTIVE.incrementAndGet();
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
			out.flush();
		
			while (true) {
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
