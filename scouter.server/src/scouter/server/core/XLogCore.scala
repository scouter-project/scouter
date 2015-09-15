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

package scouter.server.core;

import scouter.io.DataOutputX
import scouter.lang.pack.XLogPack
import scouter.lang.pack.XLogTypes
import scouter.server.Configure
import scouter.server.Logger
import scouter.server.core.app.XLogGroupPerf
import scouter.server.core.app.XLogGroupPerf
import scouter.server.core.app.XLogGroupUtil
import scouter.server.core.cache.XLogCache
import scouter.server.db.XLogWR
import scouter.server.geoip.GeoIpUtil
import scouter.server.plugin.PlugXLogBuf
import scouter.server.tagcnt.XLogTagCount
import scouter.server.util.ThreadScala
import scouter.util.RequestQueue

object ServiceCore {

    val queue = new RequestQueue[XLogPack](CoreRun.MAX_QUE_SIZE);

    val conf = Configure.getInstance();
    val plugin = PlugXLogBuf.getInstance();

    def calc(m: XLogPack) = {
        XLogGroupUtil.process(m);
        if (conf.enable_geoip) {
            GeoIpUtil.setNationAndCity(m);
        }
        XLogGroupPerf.add(m);
    }
    ThreadScala.startDaemon("scouter.server.core.XLogCore", { CoreRun.running }) {
        val m = queue.get();
        
        if (Configure.WORKABLE) {
	        m.xType match {
	            case XLogTypes.WEB_SERVICE =>
	                VisitorCore.add(m)
	                calc(m)
	            case XLogTypes.APP_SERVICE =>
	                calc(m)
	            case _ => //기타 타입은 무시한다.
	        }
	
	        plugin.add(m);
	
	        val b = new DataOutputX().writePack(m).toByteArray();
	        XLogCache.put(m.objHash, m.elapsed, m.error != 0, b);
	        if (conf.tagcnt_enabled) {
	            XLogTagCount.add(m)
	        }
	        XLogWR.add(m.endTime, m.txid, m.gxid, m.elapsed, b);
        }
    }

    def add(p: XLogPack) {
       if(p.endTime==0){
           p.endTime = System.currentTimeMillis();
       }
    
        val ok = queue.put(p);
        if (ok == false) {
            Logger.println("S116", 10, "queue exceeded!!");
        }
    }

}
