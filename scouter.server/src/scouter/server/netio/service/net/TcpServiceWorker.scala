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

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.EOFException
import java.net.Socket
import java.net.SocketTimeoutException
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.net.NetCafe
import scouter.net.RequestCmd
import scouter.net.TcpFlag
import scouter.server.LoginManager
import scouter.server.logs.RequestLogger
import scouter.server.netio.service.ServiceHandlingProxy
import scouter.util.FileUtil
import scouter.util.Hexa32
import scouter.server.Configure

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
    val conf = Configure.getInstance()

    override def run() {

        var remoteAddr = ""
        try {
            remoteAddr = "" + socket.getRemoteSocketAddress()

            val cafe = in.readInt();
            cafe match {
                case NetCafe.TCP_AGENT =>
                    val objHash = in.readInt()
                    val num= TcpAgentManager.add(objHash, new TcpAgentWorker(socket, in, out))
                    if(conf.debug_net){
                         println("Agent : " + remoteAddr + " open [" + Hexa32.toString32(objHash) + "] #"+num);
                    }
                    return
                case NetCafe.TCP_CLIENT =>
                    if(conf.debug_net){
                        println("Client : " + remoteAddr + " open #"+(ServiceWorker.getActiveCount()+1));
                    }
                case _ =>
                   if(conf.debug_net){
                        println("Unknown : " + remoteAddr + " drop");
                   }
                   FileUtil.close(in);
                    FileUtil.close(out);
                    FileUtil.close(socket);
                    return
            }

        } catch {
            case _: Throwable =>
                FileUtil.close(in);
                FileUtil.close(out);
                FileUtil.close(socket);
                return
        }
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
                if(conf.debug_net){
                    println("Client : " + remoteAddr + " closed");
                    ne.printStackTrace();
                }
            case e: EOFException =>
                if(conf.debug_net){
                     println("Client : " + remoteAddr + " closed");
                }
            case se: SocketTimeoutException =>
                if(conf.debug_net){
                     println("Client : " + remoteAddr + " closed");
                     se.printStackTrace();
                }
            case e: Exception =>
                if(conf.debug_net){
                    println("Client : " + remoteAddr + " closed " + e + " workers=" + ServiceWorker.getActiveCount());
                }
            case t: Throwable => t.printStackTrace();
        } finally {
            FileUtil.close(in);
            FileUtil.close(out);
            FileUtil.close(socket);
            ServiceWorker.desc();
        }
    }

}