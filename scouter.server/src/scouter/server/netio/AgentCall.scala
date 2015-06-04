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

package scouter.server.netio;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import scouter.lang.pack.MapPack;
import scouter.lang.pack.ObjectPack;
import scouter.lang.pack.Pack;
import scouter.lang.pack.PackEnum;
import scouter.io.DataInputX;
import scouter.io.DataOutputX;
import scouter.net.SocketAddr;
import scouter.net.TcpFlag;
import scouter.server.Logger;
import scouter.server.logs.RequestLogger;
import scouter.util.FileUtil;

object AgentCall {

    def call(o: ObjectPack, cmd: String, param: MapPack): MapPack = {
        if (o == null)
            return null;
        var socket: Socket = null;
        var in: DataInputX = null;
        var out: DataOutputX = null;

        RequestLogger.getInstance().registerCmd(cmd);
        val addr = new SocketAddr(o.address);
        if (addr.isOk() == false) {
            return null;
        }

        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(addr.getAddr(), addr.getPort()));
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(8000);
            socket.setReuseAddress(true);

            in = new DataInputX(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputX(new BufferedOutputStream(socket.getOutputStream()));

            out.writeText(cmd);
            out.writePack(if (param != null) param else new MapPack());

            out.flush();

            var p: Pack = null;
            while (in.readByte() == TcpFlag.HasNEXT) {
                p = in.readPack();
            }
            if (p == null) {
                return null;
            }
            p.getPackType() match {
                case PackEnum.MAP => return p.asInstanceOf[MapPack];
                case _ => Logger.println("AGENT CALL", "not allowed return : " + p);
            }

        } catch {
            case e: Throwable =>
                Logger.println(cmd + " " + o);
                e.printStackTrace();
        } finally {
            FileUtil.close(in);
            FileUtil.close(out);
            FileUtil.close(socket);
        }
        return null;
    }

    def call(o: ObjectPack, cmd: String, param: MapPack, handler: (DataInputX, DataOutputX) => Unit) {
        if (o == null)
            return ;
        var socket: Socket = null;
        var in: DataInputX = null;
        var out: DataOutputX = null;

        RequestLogger.getInstance().registerCmd(cmd);
        val addr = new SocketAddr(o.address);
        if (addr.isOk() == false) {
            return ;
        }

        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(addr.getAddr(), addr.getPort()));
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(8000);
            in = new DataInputX(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputX(new BufferedOutputStream(socket.getOutputStream()));

            out.writeText(cmd);
            out.writePack(if (param != null) param else new MapPack());

            out.flush();

            handler(in, out);

        } catch {
            case e: Throwable =>
                Logger.println(cmd + " " + o);
                e.printStackTrace();
        } finally {
            FileUtil.close(in);
            FileUtil.close(out);
            FileUtil.close(socket);
        }
        return ;
    }
}