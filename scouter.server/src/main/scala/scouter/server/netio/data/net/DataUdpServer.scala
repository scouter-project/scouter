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

package scouter.server.netio.data.net;

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import scouter.server.Configure
import scouter.server.Logger
import scouter.server.netio.data.NetDataProcessor
import scouter.util.ThreadUtil
import scouter.server.util.ThreadScala
import scouter.util.FileUtil

object DataUdpServer {

    val conf = Configure.getInstance();
    var udpsocket: DatagramSocket = null;
    ThreadScala.startDaemon("scouter.server.netio.data.net.DataUdpServer") {
        while (true) {
            open(conf.net_udp_listen_ip, conf.net_udp_listen_port);
            recv();
            FileUtil.close(udpsocket)
        }
    }

    def recv() {
        try {
            val BUFFER_SIZE = conf.net_udp_packet_buffer_size;
            val rbuf = new Array[Byte](BUFFER_SIZE)
            val p = new DatagramPacket(rbuf, BUFFER_SIZE);

            // loop until any exception
            while (true) {
                udpsocket.receive(p);
                val data = new Array[Byte](p.getLength());
                System.arraycopy(p.getData(), 0, data, 0, p.getLength());
                NetDataProcessor.add(data, p.getAddress());
            }
        } catch {
            case t: Throwable =>
                Logger.println("S151", 10, t);
        }
    }

    def open(host: String, port: Int) {
        Logger.println("udp listen " + host + ":" + port);
        Logger.println("\tudp_host=" + host);
        Logger.println("\tudp_port=" + port);
        Logger.println("\tudp_buffer=" + conf.net_udp_packet_buffer_size);
        Logger.println("\tudp_so_rcvbuf=" + conf.net_udp_so_rcvbuf_size);

        while (true) {
            try {
                udpsocket = new DatagramSocket(port, InetAddress.getByName(host));
                val buf = conf.net_udp_so_rcvbuf_size;
                if (buf > 0) {
                    udpsocket.setReceiveBufferSize(buf);
                }
                return ;
            } catch {
                case e: Exception =>
                    Logger.println("S157", 1, "udp data server port=" + port, e);
            }
            ThreadUtil.sleep(3000);
        }
    }
}
