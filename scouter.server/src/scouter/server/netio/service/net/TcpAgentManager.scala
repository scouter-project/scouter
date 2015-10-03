/*
*  Copyright 2015 the original author or authors.
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
package scouter.server.netio.service.net
import scouter.server.util.ThreadScala
import scouter.util.IntKeyLinkedMap
import scouter.util.LinkedList
import scouter.util.RequestQueue
import scala.util.control.Breaks._
import scouter.server.Configure
import java.util.concurrent.Executors
object TcpAgentManager {
    val pool = Executors.newFixedThreadPool(4)
    val agentTable = new IntKeyLinkedMap[RequestQueue[TcpAgentWorker]]().setMax(5000)
    
    ThreadScala.startDaemon("scouter.server.netio.service.net.TcpAgentManager", { true }, 5000) {
        val keys = agentTable.keyArray()
        for (k <- keys) {
            val agentSessions = agentTable.get(k)
            if (agentSessions != null) {
                pool.execute(new Runnable() {
                    override def run() {
                        breakable {
                            val cnt = agentSessions.size()
                            for (k <- 0 to cnt) {
                                val item = agentSessions.getNoWait()
                                if (item == null) {
                                    break
                                }
                                if (item.isExpired()) {
                                    item.sendKeepAlive(3000)
                                }
                                if (item.isClosed() == false) {
                                    agentSessions.put(item)
                                }
                            }
                        }
                    }
                });
            }
        }
    }
    val conf = Configure.getInstance()
    def add(objHash: Int, agent: TcpAgentWorker): Int = {
        agentTable.synchronized {
            var session = agentTable.get(objHash)
            if (session == null) {
                session = new RequestQueue[TcpAgentWorker](50);
                agentTable.put(objHash, session)
            }
            session.put(agent);
            return session.size()
        }
    }
    def get(objHash: Int): TcpAgentWorker = {
        var session = agentTable.get(objHash)
        return if (session != null) session.get(conf.tcp_agent_max_wait) else null
    }
}
