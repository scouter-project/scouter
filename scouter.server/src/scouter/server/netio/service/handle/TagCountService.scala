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
 */
package scouter.server.netio.service.handle;

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.asScalaSet
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.lang.pack.AlertPack
import scouter.lang.pack.MapPack
import scouter.lang.pack.XLogPack
import scouter.lang.value.MapValue
import scouter.lang.value.TextValue
import scouter.net.TcpFlag
import scouter.server.db.AlertRD
import scouter.server.db.ObjectRD
import scouter.server.db.XLogRD
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.server.tagcnt.TagCountConfig
import scouter.server.tagcnt.TagCountProxy
import scouter.server.tagcnt.core.ValueCount
import scouter.server.tagcnt.core.ValueCountTotal
import scouter.server.util.EnumerScala
import scouter.server.util.TimedSeries
import scouter.util.DateUtil
import scouter.util.IntSet
import scouter.util.StringUtil
import scouter.net.RequestCmd
import java.util.Enumeration
import scouter.util.IPUtil

class TagCountService {

    @ServiceHandler(RequestCmd.TAGCNT_DIV_NAMES)
    def getDivNames(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objType = param.getText("objType");

        val tagGroups = TagCountConfig.getTagGroups();
        while (tagGroups.hasMoreElements()) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writeValue(new TextValue(tagGroups.nextString()));
        }
    }

    @ServiceHandler(RequestCmd.TAGCNT_TAG_NAMES)
    def getTagNames(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objType = param.getText("objType");
        val tagGroup = param.getText("tagGroup");

        val tagNames = TagCountConfig.getTagNames(tagGroup);
        while (tagNames.hasMoreElements()) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writeValue(new TextValue(tagNames.nextString()));
        }
    }

    @ServiceHandler(RequestCmd.TAGCNT_TAG_VALUES)
    def getTagValues(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objType = param.getText("objType");
        val tagGroup = param.getText("tagGroup");
        val tagName = param.getText("tagName");
        var date = param.getText("date");
        if (StringUtil.isEmpty(date)) {
            date = DateUtil.yyyymmdd();
        }

        val valueCountTotal = TagCountProxy.getTagValueCountWithCache(date, objType, tagGroup, tagName, 100);
        if (valueCountTotal != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writeInt(valueCountTotal.howManyValues)
            // TODO: temp
            dout.writeInt(valueCountTotal.totalCount.toInt)
            dout.writeInt(valueCountTotal.values.size())
            EnumerScala.forward(valueCountTotal.values, (vc: ValueCount) => {
                dout.writeValue(vc.tagValue)
                // TODO: temp
                dout.writeLong(vc.valueCount.toLong)
            })
        }
    }

    @ServiceHandler(RequestCmd.TAGCNT_TAG_VALUE_DATA)
    def getTagValueCount(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objType = param.getText("objType");
        val tagGroup = param.getText("tagGroup");
        val tagName = param.getText("tagName");
        val tagValue = param.get("tagValue");
        var date = param.getText("date");
        if (StringUtil.isEmpty(date)) {
            date = DateUtil.yyyymmdd();
        }

        val valueCount = TagCountProxy.getTagValueCountData(date, objType, tagGroup, tagName, tagValue);
        if (valueCount != null) {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writeArray(valueCount)
        }
    }

    @ServiceHandler(RequestCmd.TAGCNT_TAG_ACTUAL_DATA)
    def getTagActualData(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readPack().asInstanceOf[MapPack];
        val objType = param.getText("objType");
        val stime = param.getLong("stime");
        val etime = param.getLong("etime");
        val tagGroup = param.getText("tagGroup");
        var date = param.getText("date");
        var max = param.getInt("max");
        var rev = param.getBoolean("reverse");
        val filterMv = param.get("filter");

        val mpack = ObjectRD.getDailyAgent(date);
        val objTypeLv = mpack.getList("objType");
        val objHashLv = mpack.getList("objHash");

        val objHashSet = new IntSet();
        for (i <- 0 to objHashLv.size() - 1) {
            if (objType == objTypeLv.getString(i)) {
                objHashSet.add(objHashLv.getInt(i));
            }
        }

        if (tagGroup == "service") {
            var txid = param.getLong("txid");
            var ok = if (txid == 0) true else false;
            var cnt = 0;
            val handler = (time: Long, data: Array[Byte]) => {
                val x = new DataInputX(data).readPack().asInstanceOf[XLogPack];
                if (objHashSet.contains(x.objHash)) {
                    if (ok == false) {
                        ok = x.txid == txid;
                    } else {
                        if (filterMv == null || serviceFilterOk(filterMv.asInstanceOf[MapValue], x)) {
                            dout.writeByte(TcpFlag.HasNEXT);
                            dout.write(data);
                            dout.flush();
                            cnt += 1;
                        }
                    }
                }
                if (cnt >= max) {
                    return ;
                }
            }

            if (rev) {
                XLogRD.readFromEndTime(date, stime, etime, handler);
            } else {
                XLogRD.readByTime(date, stime, etime, handler);
            }
        } else if (tagGroup == "alert") {
            var cnt = 0;
            val handler = (time: Long, data: Array[Byte]) => {
                val x = new DataInputX(data).readPack().asInstanceOf[AlertPack];
                if (objHashSet.contains(x.objHash)) {
                    if (filterMv == null || alertFilterOk(filterMv.asInstanceOf[MapValue], x)) {
                        dout.writeByte(TcpFlag.HasNEXT);
                        dout.write(data);
                        dout.flush();
                        cnt += 1;
                    }
                }
                if (cnt >= max) {
                    return ;
                }
            }
            if (rev) {
                AlertRD.readFromEndTime(date, stime, etime, handler);
            } else {
                AlertRD.readByTime(date, stime, etime, handler);
            }
        }

    }

    def serviceFilterOk(mv: MapValue, x: XLogPack): Boolean = {
        val itr = mv.keys();
        while (itr.hasMoreElements()) {
            val key = itr.nextElement();
            if (key == "error") {
                val errorLv = mv.getList(key);
                for (i <- 0 to errorLv.size() - 1) {
                    var error = errorLv.get(i).toJavaObject();
                    if (x.error == error) {
                        return true;
                    }
                }
            } else if (key == "user-agent") {
                val userAgentLv = mv.getList(key);
                for (i <- 0 to userAgentLv.size() - 1) {
                    var userAgent = userAgentLv.get(i).toJavaObject();
                    if (x.userAgent == userAgent) {
                        return true;
                    }
                }
            } else if (key == "object") {
                val objHashLv = mv.getList(key);
                for (i <- 0 to objHashLv.size() - 1) {
                    var objHash = objHashLv.get(i).toJavaObject();
                    if (x.objHash == objHash) {
                        return true;
                    }
                }
            } else if (key == "nation") {
                val nationLv = mv.getList(key);
                for (i <- 0 to nationLv.size() - 1) {
                    var nation = nationLv.getString(i);
                    if (x.countryCode == nation) {
                        return true;
                    }
                }
            } else if (key == "visitor") {
                val visitorLv = mv.getList(key);
                for (i <- 0 to visitorLv.size() - 1) {
                    var visitor = visitorLv.getLong(i);
                    if (x.visitor == visitor) {
                        return true;
                    }
                }
            } else if (key == "city") {
                val cityLv = mv.getList(key);
                for (i <- 0 to cityLv.size() - 1) {
                    var city = cityLv.get(i).toJavaObject();
                    if (x.city == city) {
                        return true;
                    }
                }
            } else if (key == "ip") {
                val ipLv = mv.getList(key);
                for (i <- 0 to ipLv.size() - 1) {
                    var ip = ipLv.get(i);
                    if (IPUtil.toString(x.ipaddr) == ip.toString()) {
                        return true;
                    }
                }
            } else if (key == "service") {
                val serviceLv = mv.getList(key);
                for (i <- 0 to serviceLv.size() - 1) {
                    var service = serviceLv.get(i).toJavaObject();
                    if (x.service == service) {
                        return true;
                    }
                }
            } else if (key == "referer") {
                val refererLv = mv.getList(key);
                for (i <- 0 to refererLv.size() - 1) {
                    var referer = refererLv.get(i).toJavaObject();
                    if (x.referer == referer) {
                        return true;
                    }
                }
            } else if (key == "group") {
                val groupLv = mv.getList(key);
                for (i <- 0 to groupLv.size() - 1) {
                    var group = groupLv.get(i).toJavaObject();
                    if (x.group == group) {
                        return true;
                    }
                }
            } else if (key == "apitime") {
                val apitimeLv = mv.getList(key);
                for (i <- 0 to apitimeLv.size() - 1) {
                    var apitime = apitimeLv.getInt(i);
                    if (x.apicallTime / 1000 == apitime) {
                        return true;
                    }
                }
            } else if (key == "sqltime") {
                val sqltimeLv = mv.getList(key);
                for (i <- 0 to sqltimeLv.size() - 1) {
                    var sqltime = sqltimeLv.getInt(i);
                    if (x.sqlTime / 1000 == sqltime) {
                        return true;
                    }
                }
            } else if (key == "elapsed") {
                val elapsedLv = mv.getList(key);
                for (i <- 0 to elapsedLv.size() - 1) {
                    var elapsed = elapsedLv.getInt(i);
                    if (x.elapsed / 1000 == elapsed) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    def alertFilterOk(mv: MapValue, x: AlertPack): Boolean = {
        val itr = mv.keys();
        while (itr.hasMoreElements()) {
            val key = itr.nextElement();
            if (key == "object") {
                val objHashLv = mv.getList(key);
                for (i <- 0 to objHashLv.size() - 1) {
                    var objHash = objHashLv.get(i).toJavaObject();
                    if (x.objHash == objHash) {
                        return true;
                    }
                }
            } else if (key == "level") {
                val levelLv = mv.getList(key);
                for (i <- 0 to levelLv.size() - 1) {
                    var level = levelLv.getInt(i);
                    if (x.level == level) {
                        return true;
                    }
                }
            } else if (key == "title") {
                val titleLv = mv.getList(key);
                for (i <- 0 to titleLv.size() - 1) {
                    var title = titleLv.getString(i);
                    if (x.title == title) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}