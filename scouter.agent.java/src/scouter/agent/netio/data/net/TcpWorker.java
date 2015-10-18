package scouter.agent.netio.data.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

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
	public int objHash = Configure.getInstance().objHash;

	public void run() {
		if (socket == null)
			return;
		try {
			processV2(socket);
		} catch (Throwable t) {
		} finally {
			close();
		}
	}

	public void close() {
		FileUtil.close(socket);
		socket = null;
		LIVE.remove(this.hashCode());
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
				localAddr = socket.getLocalAddress().getHostAddress();
			}
			LIVE.put(this.hashCode(), this);
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

	private void processV2(Socket socket) throws IOException {

		DataInputX in = null;
		DataOutputX out = null;
		Configure conf = Configure.getInstance();

		try {
			in = new DataInputX(new BufferedInputStream(socket.getInputStream()));
			out = new DataOutputX(new BufferedOutputStream(socket.getOutputStream()));

			String server_addr = conf.server_addr;
			int port = conf.server_tcp_port;

			out.writeInt(NetCafe.TCP_AGENT_V2);
			out.writeInt(objHash);
			out.flush();

			//에이전트 이름, 서버 주소포트가 같은 동안만 세션을 유지하라.
			while (objHash == Configure.getInstance().objHash && server_addr.equals(conf.server_addr)
					&& port == conf.server_tcp_port) {
				
				byte[] buff = in.readIntBytes();

				DataInputX in2 = new DataInputX(buff);
				String cmd = in2.readText();
				Pack parameter = (Pack) in2.readPack();

				Pack res = ReqestHandlingProxy.process(cmd, parameter, in, out);
				if (res != null) {
					out.writeByte(TcpFlag.HasNEXT);

					byte[] obuff = new DataOutputX().writePack(res).toByteArray();
					out.writeIntBytes(obuff);
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
