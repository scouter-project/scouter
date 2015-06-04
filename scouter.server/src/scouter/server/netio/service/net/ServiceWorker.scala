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
 *
 */

package scouter.server.netio.service.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.net.Socket;
import java.net.SocketTimeoutException;

import scouter.server.LoginManager;
import scouter.server.logs.RequestLogger;
import scouter.server.netio.service.ServiceHandlingProxy;
import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.net.RequestCmd;
import scouter.net.TcpFlag;
import scouter.util.FileUtil;

object ServiceWorker {
    var workers = 0;

    def inc() {
        this.synchronized {
            workers += 1;
        }
    }

    def desc() {
        this.synchronized {
            workers -= 1;
        }
    }

    def getActiveCount(): Int = {
        workers;
    }
}
class ServiceWorker(_socket: Socket) extends Runnable {
    var socket = _socket;

    val in = new DataInputX(new BufferedInputStream(socket.getInputStream()));
    val out = new DataOutputX(new BufferedOutputStream(socket.getOutputStream()));

    override def run() {

        try {
            ServiceWorker.inc();

            while (true) {
                val cmd = in.readText();
                if (RequestCmd.CLOSE.equals(cmd)) {
                    return
                }
                val session = in.readLong();
                val login = LoginManager.okSession(session);

                RequestLogger.getInstance().add(cmd, session);
                ServiceHandlingProxy.process(cmd, in, out, login);

                out.writeByte(TcpFlag.NoNEXT);
                out.flush();
            }
        } catch {
            case ne: NullPointerException =>
                System.out.println("Client : " + socket.toString() + " closed");
            case e: EOFException =>
                System.out.println("Client : " + socket.toString() + " closed");
            case se: SocketTimeoutException => {}
            case e: Exception =>
                System.out.println("Client : " + socket.toString() + " closed " + e + " workers=" + ServiceWorker.getActiveCount());
            case t: Throwable => t.printStackTrace();
        } finally {
            FileUtil.close(in);
            FileUtil.close(out);
            FileUtil.close(socket);
            ServiceWorker.desc();
        }
    }

}