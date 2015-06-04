/*
 *  Copyright 2015 LG CNS.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License. 
 */

package scouter.agent.netio.request.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.Socket;
import java.net.SocketException;

import scouter.agent.netio.request.ReqestHandlingProxy;
import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.lang.pack.Pack;
import scouter.net.TcpFlag;
import scouter.util.FileUtil;

public class RequestWorker implements Runnable {
	protected Socket socket;

	public RequestWorker(Socket socket) {
		this.socket = socket;
		try {
			this.socket.setReuseAddress(true);
		} catch (SocketException e) {
		}
	}

	public void run() {
		DataInputX in = null;
		DataOutputX out = null;
		try {
			inc();
			in = new DataInputX(new BufferedInputStream(socket.getInputStream()));
			out = new DataOutputX(new BufferedOutputStream(socket.getOutputStream()));
			process(in, out);
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			FileUtil.close(in);
			FileUtil.close(out);
			FileUtil.close(socket);
			desc();
		}
	}

	private void process(DataInputX in, DataOutputX out) throws Exception {
		// 암호 처리를 이곳에서 해야 한다.
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

	private static int workers = 0;

	private synchronized static void inc() {
		workers++;
	}

	private synchronized static void desc() {
		workers--;
	}

	public static int getActiveCount() {
		return workers;
	}

}