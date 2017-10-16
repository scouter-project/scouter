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

package scouter.server.netio.service.handle;

import scouter.lang.TextTypes
import scouter.lang.pack.MapPack
import scouter.lang.value.ListValue
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.net.RequestCmd
import scouter.net.TcpFlag
import scouter.server.core.app.PerfStat
import scouter.server.db.TextPermRD
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.util.ArrayUtil
import java.util.HashSet

import scouter.util.IntSet
import scouter.server.util.EnumerScala
import scouter.lang.value.DecimalValue
import scouter.server.core.app.XLogGroupPerf

class GroupService {

    @ServiceHandler(RequestCmd.REALTIME_SERVICE_GROUP)
    def realTimeServiceGroup(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objLv = param.getList("objHash");
        val objSet = new IntSet();

        EnumerScala.foreach(objLv, (objHash: DecimalValue) => {
            objSet.add(objHash.intValue());
        })

        if (objSet.size() == 0) {
            return ;
        }

        val m = new MapPack();
        val nameLv = m.newList("name");
        val countLv = m.newList("count");
        val elapsedLv = m.newList("elapsed");
        val errorLv = m.newList("error");
        val groupPerfStat = XLogGroupPerf.getGroupPerfStat(objSet);

        EnumerScala.foreach(groupPerfStat.keys(), (hash: Int) => {
            val name = TextPermRD.getString(TextTypes.GROUP, hash);
            val perf = groupPerfStat.get(hash);
            nameLv.add(if (name == null) "unknown" else name);
            countLv.add(perf.count);
            elapsedLv.add(perf.getAvgElapsed());
            errorLv.add(perf.error);
        })

        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(m);
    }
}