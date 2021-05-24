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
import java.io.IOException
import java.net.InetAddress

import scouter.io.{DataInputX, DataOutputX}
import scouter.lang.{TextTypes, TimeTypeEnum}
import scouter.lang.counters.CounterConstants
import scouter.lang.pack._
import scouter.net.NetCafe
import scouter.server.Configure
import scouter.server.Logger
import scouter.server.core._
import scouter.server.core.cache.TextCache
import scouter.server.util.ThreadScala
import scouter.util.{BytesUtil, HashUtil, RequestQueue, StringUtil}
import scouter.lang.value.DecimalValue
object NetDataProcessor {
    class NetData(_data: Array[Byte], _addr: InetAddress) {
        val addr = _addr
        val data = _data
    }
    var working = true
    val num = Configure.getInstance()._net_udp_worker_thread_count
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

    @throws[IOException]
    def add(pack: Pack, addr: InetAddress): Unit = {
        val out = new DataOutputX
        out.write(NetCafe.CAFE)
        out.write(new DataOutputX().writePack(pack).toByteArray)
        add(out.toByteArray, addr)
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
            if (conf.log_udp_multipacket) {
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
        if (conf.log_udp_packet) {
            System.out.println(p)
        }
        p.getPackType() match {
            case PackEnum.PERF_COUNTER =>
                val counterPack = p.asInstanceOf[PerfCounterPack]
                val objHash = HashUtil.hash(counterPack.objName)
                if (counterPack.time == 0) {
                    counterPack.time = System.currentTimeMillis();
                }
                if (counterPack.timetype == 0) {
                    counterPack.timetype = TimeTypeEnum.REALTIME;
                }
                counterPack.data.put(CounterConstants.COMMON_OBJHASH, new DecimalValue(objHash)) //add objHash into datafile
                counterPack.data.put(CounterConstants.COMMON_TIME, new DecimalValue(counterPack.time)) //add time into datafile

                PerfCountCore.add(counterPack)
                if (conf.log_udp_counter) {
                    System.out.println("DEBUG UDP COUNTER: " + p)
                }
            case PackEnum.PERF_INTERACTION_COUNTER =>
                val counterPack = p.asInstanceOf[InteractionPerfCounterPack]
                InteractionPerfCountCore.add(counterPack)

                if (conf.log_udp_interaction_counter) {
                    System.out.println("DEBUG UDP INTERACTION COUNTER: " + p)
                }
            case PackEnum.XLOG =>
                XLogPreCore.add(p.asInstanceOf[XLogPack])
                if (conf.log_udp_xlog) {
                    System.out.println("DEBUG UDP XLOG: " + p)
                }
            case PackEnum.XLOG_PROFILE =>
                ProfilePreCore.add(p.asInstanceOf[XLogProfilePack])
                if (conf.log_udp_profile) {
                    System.out.println("DEBUG UDP PROFILE: " + p)
                }
            case PackEnum.XLOG_PROFILE2 =>
                ProfilePreCore.add(p.asInstanceOf[XLogProfilePack2])
                if (conf.log_udp_profile) {
                    System.out.println("DEBUG UDP PROFILE: " + p)
                }
            case PackEnum.DROPPED_XLOG =>
                val droppedXLogPack = p.asInstanceOf[DroppedXLogPack]
                val xLogPack = new XLogPack();
                xLogPack.gxid = droppedXLogPack.gxid;
                xLogPack.txid = droppedXLogPack.txid;

                XLogPreCore.add(xLogPack)
                if (conf.log_udp_xlog) {
                    System.out.println("DEBUG UDP DROPPED XLOG: " + p)
                }
            case PackEnum.TEXT =>
                TextCore.add(p.asInstanceOf[TextPack])
                if (conf.log_udp_text) {
                    System.out.println("DEBUG UDP TEXT: " + p)
                }
            case PackEnum.ALERT =>
                AlertCore.add(p.asInstanceOf[AlertPack])
                if (conf.log_udp_alert) {
                    System.out.println("DEBUG UDP ALERT: " + p)
                }
            case PackEnum.OBJECT =>
                val h = p.asInstanceOf[ObjectPack]
                if (StringUtil.isEmpty(h.address)) {
                    h.address = addr.getHostAddress()
                }
                AgentManager.active(h)
                if (conf.log_udp_object) {
                    System.out.println("DEBUG UDP OBJECT: " + p)
                }
            case PackEnum.PERF_STATUS =>
                StatusCore.add(p.asInstanceOf[StatusPack])
                if (conf.log_udp_status) {
                    System.out.println("DEBUG UDP STATUS: " + p)
                }
            case PackEnum.STACK =>
                StackAnalyzerCore.add(p.asInstanceOf[StackPack])
                if (conf.log_udp_stack) {
                    System.out.println("DEBUG UDP STACK: " + p)
                }
             case PackEnum.SUMMARY =>
                SummaryCore.add(p.asInstanceOf[SummaryPack])
                if (conf.log_udp_summary) {
                    System.out.println("DEBUG UDP SUMMARY: " + p)
                }
             case PackEnum.BATCH =>
                BatchCore.add(p.asInstanceOf[BatchPack])
                if (conf.log_udp_batch) {
                    System.out.println("DEBUG UDP Batch: " + p)
                }
            case PackEnum.SPAN_CONTAINER =>
                SpanCore.add(p.asInstanceOf[SpanContainerPack])
                if (conf.log_udp_span) {
                    System.out.println("DEBUG UDP SPAN CONTAINER: " + p)
                }
            case _ =>
                PackExtProcessChain.doChain(p)
                //System.out.println(p)
        }
    }
}
