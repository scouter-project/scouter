/*
*  Copyright 2015 the original author or authors. 
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License") 
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
package scouter.server.netio.data

import java.net.InetAddress
import scouter.io.DataInputX
import scouter.lang.TextTypes
import scouter.lang.pack.AlertPack
import scouter.lang.pack.ObjectPack
import scouter.lang.pack.Pack
import scouter.lang.pack.PackEnum
import scouter.lang.pack.PerfCounterPack
import scouter.lang.pack.StackPack
import scouter.lang.pack.StatusPack
import scouter.lang.pack.TextPack
import scouter.lang.pack.XLogPack
import scouter.lang.pack.XLogProfilePack
import scouter.net.NetCafe
import scouter.server.Configure
import scouter.server.Logger
import scouter.server.core.AgentManager
import scouter.server.core.AlertCore
import scouter.server.core.PerfCountCore
import scouter.server.core.ProfileCore
import scouter.server.core.ServiceCore
import scouter.server.core.StackAnalyzerCore
import scouter.server.core.StatusCore
import scouter.server.core.TextCore
import scouter.server.core.cache.TextCache
import scouter.server.util.ThreadScala
import scouter.util.BytesUtil
import scouter.util.RequestQueue
import scouter.util.StringUtil
import tuna.server.core.SummaryCore
import scouter.lang.pack.SummaryPack
import scouter.lang.pack.SummaryPack
object NetDataProcessor {
    class NetData(_data: Array[Byte], _addr: InetAddress) {
        val addr = _addr
        val data = _data
    }
    var working = true
    val num = Configure.getInstance().num_of_net_processor
    for (x <- 0 to num - 1) {
        ThreadScala.startDaemon("scouter.server.netio.data.NetDataProcessor") {
            while (working) {
                try {
                    val data = queue.get()
                    process(data)
                } catch {
                    case t: Throwable => t.printStackTrace()
                }
            }
        }
    }
    val queue = new RequestQueue[NetData](2048)
    val conf = Configure.getInstance()
    def add(data: Array[Byte], addr: InetAddress) {
        val ok = queue.putNotifySingle(new NetData(data, addr))
        if (ok == false) {
            Logger.println("S158", 10, "overflow recv queue!!")
        }
    }
    def process(p: NetData) {
        try {
            val in = new DataInputX(p.data)
            val cafe = in.readInt()
            cafe match {
                case NetCafe.UDP_CAFE => processCafe(in, p.addr)
                case NetCafe.UDP_CAFE_N => processCafeN(in, p.addr)
                case NetCafe.UDP_CAFE_MTU => processCafeMTU(in, p.addr)
                case NetCafe.UDP_JAVA => processCafe(in, p.addr)
                case NetCafe.UDP_JAVA_N => processCafeN(in, p.addr)
                case NetCafe.UDP_JAVA_MTU => processCafeMTU(in, p.addr)
                case _ =>
                    System.out.println("Receive unknown data, length=" + BytesUtil.getLength(p.data) + " from " + p.addr)
            }
        } catch {
            case e: Throwable =>
                Logger.println("S159", 10, "invalid data ", e)
                e.printStackTrace()
        }
    }
    private def processCafeMTU(in: DataInputX, addr: InetAddress) {
        val objHash = in.readInt()
        val pkid = in.readLong()
        val total = in.readShort()
        val num = in.readShort()
        val data = in.readBlob()
        val done = MultiPacketProcessor.add(pkid, total, num, data, objHash, addr)
        if (done != null) {
            val p = new DataInputX(done).readPack()
            process(p, addr)
            if (conf.debug_udp_multipacket) {
                val objName = TextCache.get(TextTypes.OBJECT, objHash)
                val sb = new StringBuffer()
                sb.append("recv ").append(p.getClass().getName())
                sb.append(" total=").append(total)
                sb.append(" object=(").append(objHash).append(")").append(objName)
                sb.append(" ").append(addr)
                Logger.println("S160", sb.toString())
            }
        }
    }
    private def processCafe(in: DataInputX, addr: InetAddress) {
        val p = in.readPack()
        process(p, addr)
    }
    private def processCafeN(in: DataInputX, addr: InetAddress) {
        val n = in.readShort()
        for (i <- 1 to n) {
            val p = in.readPack()
            process(p, addr)
        }
    }
    //    private val reserved = new HashSet[String]()
    //
    //    reserved.add("objType")
    //    reserved.add("objName")
    //    reserved.add("timeType")
    //    reserved.add("counter")
    //    reserved.add("addr")
    def process(p: Pack, addr: InetAddress) {
        if (p == null)
            return
        if (conf.debug_udp_packet) {
            System.out.println(p)
        }
        p.getPackType() match {
            case PackEnum.PERF_COUNTER =>
                PerfCountCore.add(p.asInstanceOf[PerfCounterPack])
                if (conf.debug_udp_counter) {
                    System.out.println("DEBUG UDP COUNTER: " + p)
                }
            case PackEnum.XLOG =>
                ServiceCore.add(p.asInstanceOf[XLogPack])
                if (conf.debug_udp_xlog) {
                    System.out.println("DEBUG UDP XLOG: " + p)
                }
            case PackEnum.XLOG_PROFILE =>
                ProfileCore.add(p.asInstanceOf[XLogProfilePack])
                if (conf.debug_udp_profile) {
                    System.out.println("DEBUG UDP PROFILE: " + p)
                }
            case PackEnum.TEXT =>
                TextCore.add(p.asInstanceOf[TextPack])
                if (conf.debug_udp_text) {
                    System.out.println("DEBUG UDP TEXT: " + p)
                }
            case PackEnum.ALERT =>
                AlertCore.add(p.asInstanceOf[AlertPack])
                if (conf.debug_udp_alert) {
                    System.out.println("DEBUG UDP ALERT: " + p)
                }
            case PackEnum.OBJECT =>
                val h = p.asInstanceOf[ObjectPack]
                if (StringUtil.isNotEmpty(h.address)) {
                    h.address = addr.getHostAddress()
                }
                AgentManager.active(h)
                if (conf.debug_udp_object) {
                    System.out.println("DEBUG UDP OBJECT: " + p)
                }
            case PackEnum.PERF_STATUS =>
                StatusCore.add(p.asInstanceOf[StatusPack])
                if (conf.debug_udp_status) {
                    System.out.println("DEBUG UDP STATUS: " + p)
                }
            case PackEnum.STACK =>
                StackAnalyzerCore.add(p.asInstanceOf[StackPack])
                if (conf.debug_udp_stack) {
                    System.out.println("DEBUG UDP STACK: " + p)
                }
             case PackEnum.SUMMARY =>
                SummaryCore.add(p.asInstanceOf[SummaryPack])
                if (conf.debug_udp_summary) {
                    System.out.println("DEBUG UDP SUMMARY: " + p)
                }
            case _ =>
                System.out.println(p)
        }
    }
}
