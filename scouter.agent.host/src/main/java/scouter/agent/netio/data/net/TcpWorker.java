package scouter.agent.netio.data.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import scouter.agent.Configure;
import scouter.agent.netio.request.ReqestHandlingProxy;
import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.lang.pack.Pack;
import scouter.net.NetCafe;
import scouter.net.TcpFlag;
import scouter.util.FileUtil;
import scouter.util.IntKeyLinkedMap;

public class TcpWorker implements Runnable {
	public static IntKeyLinkedMap<TcpWorker> LIVE = new IntKeyLinkedMap<TcpWorker>();

	public static String localAddr = null;
	public int objHash = Configure.getInstance().getObjHash();

	public void run() {
		if (socket == null)
			return;
		try {
			process(socket);
		} catch (Throwable t) {
		} finally {
			close();
		}
	}
    public void close(){
    	FileUtil.close(socket);
    	socket = null;
		LIVE.remove(this.hashCode());
    }
	protected Socket socket = null;

	public boolean prepare() {
		Configure conf = Configure.getInstance();
		String host = conf.net_collector_ip;
		int port = conf.net_collector_tcp_port;
		int so_timeout = conf.net_collector_tcp_so_timeout_ms;
		int connection_timeout = conf.net_collector_tcp_connection_timeout_ms;

		socket = new Socket();
		try {
			socket.connect(new InetSocketAddress(host, port), connection_timeout);
			socket.setSoTimeout(so_timeout);
			if (localAddr == null) {
				localAddr = socket.getLocalAddress().getHostAddress();
			}
			LIVE.put(this.hashCode(),this);
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

			while (objHash == Configure.getInstance().getObjHash()) {
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
