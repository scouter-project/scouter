/*
*  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
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
import scouter.server.ConfObserver
import scouter.server.Configure
import scouter.server.Logger
import scouter.server.util.ThreadScala
import scouter.util.FileUtil
import scouter.util.ThreadUtil
object TcpServer {
    val conf = Configure.getInstance();
    val threadPool = ThreadUtil.createExecutor("ServiceServer", conf.tcp_server_pool_size, 10000, true);
    ConfObserver.put("TcpServer") {
      if (conf.tcp_server_pool_size != threadPool.getCorePoolSize()) {
        threadPool.setCorePoolSize(conf.tcp_server_pool_size);
      }
    }
    ThreadScala.startDaemon("scouter.server.netio.service.net.TcpServer") {
        Logger.println("\ttcp_port=" + conf.tcp_port);
        Logger.println("\tcp_agent_so_timeout=" + conf.tcp_agent_so_timeout);
        Logger.println("\tcp_client_so_timeout=" + conf.tcp_client_so_timeout);
        var server: ServerSocket = null;
        try {
            server = new ServerSocket( conf.tcp_port);
            while (true) {
                val client = server.accept();
                client.setSoTimeout(conf.tcp_client_so_timeout);
                client.setReuseAddress(true);
                try {
                    threadPool.execute(new ServiceWorker(client));
                } catch {
                    case e: Throwable => e.printStackTrace();
                }
            }
        } catch {
            case e: Throwable => Logger.println("S167", 1, "tcp port=" + conf.tcp_port, e);
        } finally {
            FileUtil.close(server);
        }
    }
}
