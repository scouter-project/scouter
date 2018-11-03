/*
 *  Copyright 2015 the original author or authors.
 *  @https://github.com/scouter-project/scouter
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package scouter.server.core

import scouter.io.DataOutputX
import scouter.lang.pack.SpanContainerPack
import scouter.server.db.ZipkinSpanWR
import scouter.server.util.ThreadScala
import scouter.server.{Configure, Logger}
import scouter.util.RequestQueue

/**
  * @author Gun Lee (gunlee01@gmail.com) on 27/10/2018
  */

object SpanCore {

    val conf = Configure.getInstance()
    val queue = new RequestQueue[SpanContainerPack](conf.span_queue_size)

    ThreadScala.startDaemon("scouter.server.core.SpanCore", {CoreRun.running}) {
        val spanContainerPack = queue.get()
        ServerStat.put("span.core.queue", queue.size());

        if (Configure.WORKABLE) {
            //TODO plugin
            //PlugInManager.xlog(spanPack)

            val spanContainerBytes = new DataOutputX().writePack(spanContainerPack).toByteArray
            ZipkinSpanWR.add(spanContainerPack.timestamp, spanContainerPack.gxid, spanContainerBytes)
        }
    }

    def add(p: SpanContainerPack): Unit = {
        if (p.timestamp == 0) {
            p.timestamp = System.currentTimeMillis()
        }

        val ok = queue.put(p)
        if (!ok) {
            Logger.println("SZ116", 10, "queue exceeded Span Core Queue!!");
        }
    }

}
