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
            open(conf.dataudp_host, conf.dataudp_port);
            recv();
            FileUtil.close(udpsocket)
        }
    }

    def recv() {
        try {
            val BUFFER_SIZE = conf.dataudp_buffer;
            val rbuf = new Array[Byte](BUFFER_SIZE)
            val p = new DatagramPacket(rbuf, BUFFER_SIZE);

            while (true) { // 예외가 발생하기 전에는 반복된다.
                udpsocket.receive(p);
                val data = new Array[Byte](p.getLength());
                System.arraycopy(p.getData(), 0, data, 0, p.getLength());
                NetDataProcessor.add(data, p.getAddress());
            }
        } catch {
            case t: Throwable =>
                Logger.println("S153", 10, t);
        }
    }

    def open(host: String, port: Int) {
        Logger.println("udp listen " + host + ":" + port + " for agent data");
        Logger.println("\tdataudp_host=" + host);
        Logger.println("\tdataudp_port=" + port);
        Logger.println("\tdataudp_buffer=" + conf.dataudp_buffer);
        Logger.println("\tdataudp_so_rcvbuf=" + conf.dataudp_so_rcvbuf);

        while (true) {
            try {
                udpsocket = new DatagramSocket(port, InetAddress.getByName(host));
                val buf = conf.dataudp_so_rcvbuf;
                if (buf > 0) {
                    // so_rcvbuf 값이 셋팅되지 않았을때는 100이상의 패킷이 동시에 도착할때는
                    // 유실이 발생했다. 이값을 셋팅함으로 해결
                    udpsocket.setReceiveBufferSize(buf);
                }
                //	udpsocket.setReuseAddress(true);
                //	udpsocket.setBroadcast(true);
                return ;
            } catch {
                case e: Exception =>
                    Logger.println("S159", 1, "udp data server port=" + port, e);
            }
            ThreadUtil.sleep(3000);
        }
    }
}
