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

package scouter.server.netio.data;

import java.io.IOException
import java.net.InetAddress
import scouter.lang.TextTypes
import scouter.server.Configure
import scouter.server.Logger
import scouter.server.core.cache.TextCache
import scouter.util.LongEnumer
import scouter.util.LongKeyLinkedMap
import scouter.util.ThreadUtil;
import scouter.server.util.ThreadScala

object MultiPacketProcessor {

    val MAX_COUNT = 1000;

    val buffer = new LongKeyLinkedMap[MultiPacket]().setMax(MAX_COUNT);

    ThreadScala.startDaemon("scouter.server.netio.data.MultiPacketProcessor") {
        while (true) {
            ThreadUtil.sleep(1000);
            if (buffer.size() > 0) {
                try {
                    checkExpired();
                } catch {
                    case e: Exception => { e.printStackTrace() }
                }
            }
        }
    }

    def add(pkid: Long, total: Int, num: Int, data: Array[Byte], objHash: Int, addr: InetAddress): Array[Byte] = {
        var p: MultiPacket = null
        buffer.synchronized {
            p = buffer.get(pkid);
            if (p == null) {
                p = new MultiPacket(total, objHash, addr);
                buffer.put(pkid, p);
            }
        }

        p.set(num, data);
        if (p.isDone()) {
            buffer.remove(pkid);
            return p.toBytes();
        }
        return null;
    }

    def checkExpired() {
        val en = buffer.keys();
        while (en.hasMoreElements() == true) {
            val key = en.nextLong();
            val p = buffer.get(key);
            if (p.isExpired) {
                buffer.remove(key);
                if (Configure.getInstance().log_expired_multipacket) {
                    Logger.println("S150", 10, p.toString);
                }
            }
        }
    }
}
