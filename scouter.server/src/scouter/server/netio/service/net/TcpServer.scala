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
import java.net.{InetAddress, ServerSocket, Socket}
import scouter.server.ConfObserver
import scouter.server.Configure
import scouter.server.Logger
import scouter.server.util.ThreadScala
import scouter.util.FileUtil
import scouter.util.ThreadUtil
object TcpServer {
    val conf = Configure.getInstance();
    val threadPool = ThreadUtil.createExecutor("ServiceServer", conf.net_tcp_service_pool_size, 10000, true);
    ConfObserver.put("TcpServer") {
      if (conf.net_tcp_service_pool_size != threadPool.getCorePoolSize()) {
        threadPool.setCorePoolSize(conf.net_tcp_service_pool_size);
      }
    }
    ThreadScala.startDaemon("scouter.server.netio.service.net.TcpServer") {
        Logger.println("\ttcp_port=" + conf.net_tcp_listen_port);
        Logger.println("\tcp_agent_so_timeout=" + conf.net_tcp_agent_so_timeout_ms);
        Logger.println("\tcp_client_so_timeout=" + conf.net_tcp_client_so_timeout_ms);
        var server: ServerSocket = null;
        try {
            server = new ServerSocket(conf.net_tcp_listen_port, 50, InetAddress.getByName(conf.net_tcp_listen_ip));
            while (true) {
                val client = server.accept();
                client.setSoTimeout(conf.net_tcp_client_so_timeout_ms);
                client.setReuseAddress(true);
                try {
                    threadPool.execute(new ServiceWorker(client));
                } catch {
                    case e: Throwable => e.printStackTrace();
                }
            }
        } catch {
            case e: Throwable => Logger.println("S167", 1, "tcp port=" + conf.net_tcp_listen_port, e);
        } finally {
            FileUtil.close(server);
        }
    }
}
