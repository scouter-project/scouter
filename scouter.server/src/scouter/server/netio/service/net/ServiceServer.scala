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

import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ExecutorService
import scouter.server.Configure
import scouter.server.Logger
import scouter.util.FileUtil
import scouter.util.ThreadUtil;
import scouter.server.util.ThreadScala

object ServiceServer {

    val conf = Configure.getInstance();
    val threadPool = ThreadUtil.createExecutor("ServiceServer", 30, 1000, 10000, true);

    ThreadScala.startDaemon("ServiceServer") {
        val listen_port = conf.service_port;
        val so_timeout = conf.service_so_timeout;

        Logger.println("tcp listen " + "0.0.0.0:" + listen_port + " for client service");
        Logger.println("\tservice_port=" + listen_port);
        Logger.println("\tservice_so_timeout=" + so_timeout);

        var server: ServerSocket = null;
        try {
            server = new ServerSocket(listen_port);
            while (true) {
                val client = server.accept();
                // TODO 주의하여 테스트해야
                client.setSoTimeout(so_timeout);
                client.setReuseAddress(true);

                try {
                    threadPool.execute(new ServiceWorker(client));
                } catch {
                    case e: Throwable => e.printStackTrace();
                }
            }
        } catch {
            case e: Throwable => Logger.println("ServiceServer", 1, "service port=" + listen_port, e);
        } finally {
            FileUtil.close(server);
        }
    }

}