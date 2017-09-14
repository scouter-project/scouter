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

import java.util.HashMap

import scala.collection.JavaConversions._
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.lang.AlertLevel
import scouter.lang.pack.AlertPack
import scouter.lang.pack.MapPack
import scouter.lang.pack.ObjectPack
import scouter.lang.value.DecimalValue
import scouter.net.TcpFlag
import scouter.server.core.cache.AlertCache
import scouter.server.core.cache.CacheOut
import scouter.server.db.AlertRD
import scouter.server.db.ObjectRD
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.server.util.EnumerScala
import scouter.net.RequestCmd
import scouter.lang.value.MapValue
import scouter.server.db.SummaryRD
import scouter.util.{DateUtil, StringUtil}
import scouter.lang.pack.SummaryPack
import scouter.lang.SummaryEnum

class AlertService {

    @ServiceHandler(RequestCmd.ALERT_REAL_TIME)
    def getRealtime(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val index = param.getInt("index");
        val loop = param.getLong("loop");
        val objType = if(StringUtil.isEmpty(param.getText("objType"))) null else param.getText("objType");
        val first = param.getBoolean("first");

        val d = AlertCache.get(objType, loop, index);
        if (d == null)
            return ;

        // 첫번째 패킷에 정보를 전송한다.
        val outparam = new MapPack();
        outparam.put("loop", new DecimalValue(d.loop));
        outparam.put("index", new DecimalValue(d.index));
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(outparam);

        if (first) {
            return ;
        }
        EnumerScala.forward(d.data, (pack: AlertPack) => {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.write(new DataOutputX().writePack(pack).toByteArray());
        })

    }

    @ServiceHandler(RequestCmd.ALERT_LOAD_TIME)
    def getAlertHistory(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        //////////
        val date = param.getText("date");
        val stime = param.getLong("stime");
        val etime = param.getLong("etime");
        val max = param.getInt("count");
        val level = param.getText("level");
        val obj = param.getText("object");
        val key = param.getText("key");
        val levelCode = AlertLevel.getValue(level);
        val ccount = if (max < 1 || max > 1000) 500 else max

        var srchCnt = 0;
        val tempObjNameMap = new HashMap[Integer, String]();

        val handler = (time: Long, data: Array[Byte]) => {
            if (srchCnt > ccount) {
                return
            }
            var ok = check(date, level, obj, key, levelCode, tempObjNameMap, data)
            if (ok) {
                srchCnt += 1
                dout.writeByte(TcpFlag.HasNEXT);
                dout.write(data);
            }
        }

        AlertRD.readByTime(date, stime, etime, handler)
    }

    private def check(date: String, level: String, obj: String, key: String, levelCode: Byte, tempObjNameMap: java.util.HashMap[Integer, String], data: Array[Byte]): Boolean = {
        val pack = new DataInputX(data).readPack().asInstanceOf[AlertPack];
        if (level != null && levelCode != pack.level) {
            return false
        }
        if (obj != null) {
            var objName = tempObjNameMap.get(pack.objHash);
            if (objName == null) {
                val objPack = ObjectRD.getObjectPack(date, pack.objHash);
                if (objPack == null) {
                    return false
                }
                objName = objPack.objName;
                tempObjNameMap.put(pack.objHash, objName);
            }
            if (objName.contains(obj) == false) {
                return false
            }
        }
        if (key != null) {
            if (pack.title.contains(key) == false && pack.message.contains(key) == false) {
                return false
            }
        }
        return true
    }

    @ServiceHandler(RequestCmd.ALERT_TITLE_COUNT)
    def titleAlertCount(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
        val param = din.readPack().asInstanceOf[MapPack];
        val date = param.getText("date");
        val stime = param.getLong("stime");
        val etime = param.getLong("etime");
        val valueMap = new HashMap[String, MapPack]();

        val handler = (time: Long, b: Array[Byte]) => {
            val data = new DataInputX(b).readPack().asInstanceOf[SummaryPack];
            if (data.stype == SummaryEnum.ALERT ) {
                val hhmm = DateUtil.hhmm(time);
                val titleLv = data.table.getList("title");
                val levelLv = data.table.getList("level");
                val countLv = data.table.getList("count");
                for (i <- 0 to titleLv.size() - 1) {
                    val title = titleLv.getString(i);
                    val level = levelLv.getLong(i).asInstanceOf[Byte];
                    val count = countLv.getInt(i)
                    var pack = valueMap.get(title);
                    if (pack == null) {
                        pack = new MapPack();
                        pack.put("title", title);
                        pack.put("level", level);
                        pack.put("count", new MapValue());
                        valueMap.put(title, pack);
                    }
                    val mv = pack.get("count").asInstanceOf[MapValue];
                    mv.put(hhmm, count);
                }
            }
        }

        SummaryRD.readByTime(SummaryEnum.ALERT, date, stime, etime, handler)
        
        val keySet = valueMap.keySet();
        for (title <- keySet) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(valueMap.get(title));
        }
    }
}
