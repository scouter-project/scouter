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
 */
package scouter.server.netio.service.handle;

import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.lang.constants.TagConstants
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
import scouter.server.util.EnumerScala
import scouter.util.DateUtil
import scouter.util.IPUtil
import scouter.util.IntSet
import scouter.util.StringUtil
import scouter.net.RequestCmd
import scouter.lang.value.ValueEnum

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
        try {
            val objType = param.getText("objType");
            val tagGroup = param.getText("tagGroup");
            val tagNameLv = param.getList("tagName");
            var date = param.getText("date");
            if (StringUtil.isEmpty(date)) {
                date = DateUtil.yyyymmdd();
            }

            for (i <- 0 to tagNameLv.size() - 1) {
                val tagName = tagNameLv.getString(i);
                val valueCountTotal = TagCountProxy.getTagValueCountWithCache(date, objType, tagGroup, tagName, 100);
                if (valueCountTotal != null) {
                    dout.writeByte(TcpFlag.HasNEXT);
                    dout.writeText(tagName);
                    dout.writeInt(valueCountTotal.howManyValues)
                    // TODO: temp
                    dout.writeFloat(valueCountTotal.totalCount)
                    dout.writeInt(valueCountTotal.values.size())
                    EnumerScala.forward(valueCountTotal.values, (vc: ValueCount) => {
                        dout.writeValue(vc.tagValue)
                        // TODO: temp
                        dout.writeFloat(vc.valueCount.toFloat)
                    })
                }
            }
        } catch {
            case e: Throwable =>
                println("TAGCNT_TAG_VALUES: " + param)
                e.printStackTrace()
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
    private def toIntArr(a: Array[Float]): Array[Int] = {
        val out = new Array[Int](a.length)
        for (i <- 0 to a.length - 1) {
            out(i) = a(i).toInt
        }
        return out
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

        if (tagGroup == TagConstants.GROUP_SERVICE) {
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
        } else if (tagGroup == TagConstants.GROUP_ALERT) {
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
            if (key == TagConstants.NAME_ERROR) {
                val errorLv = mv.getList(key);
                for (i <- 0 to errorLv.size() - 1) {
                    var error = errorLv.get(i).toJavaObject();
                    if (x.error == error) {
                        return true;
                    }
                }
            } else if (key == TagConstants.NAME_USER_AGENT) {
                val userAgentLv = mv.getList(key);
                for (i <- 0 to userAgentLv.size() - 1) {
                    var userAgent = userAgentLv.get(i).toJavaObject();
                    if (x.userAgent == userAgent) {
                        return true;
                    }
                }
            } else if (key == TagConstants.NAME_OBJECT) {
                val objHashLv = mv.getList(key);
                for (i <- 0 to objHashLv.size() - 1) {
                    var objHash = objHashLv.get(i).toJavaObject();
                    if (x.objHash == objHash) {
                        return true;
                    }
                }
            } else if (key == TagConstants.NAME_NATION) {
                val nationLv = mv.getList(key);
                for (i <- 0 to nationLv.size() - 1) {
                    var nation = nationLv.getString(i);
                    if (x.countryCode == nation) {
                        return true;
                    }
                }
                //            } else if (key == TagConstants.NAME_USERID) {
                //                val useridLv = mv.getList(key);
                //                for (i <- 0 to useridLv.size() - 1) {
                //                    var userid = useridLv.getLong(i);
                //                    if (x.userid == userid) {
                //                        return true;
                //                    }
                //                }
            } else if (key == TagConstants.NAME_CITY) {
                val cityLv = mv.getList(key);
                for (i <- 0 to cityLv.size() - 1) {
                    var city = cityLv.get(i).toJavaObject();
                    if (x.city == city) {
                        return true;
                    }
                }
                //            } else if (key == TagConstants.NAME_IP) {
                //                val ipLv = mv.getList(key);
                //                for (i <- 0 to ipLv.size() - 1) {
                //                    var ip = ipLv.get(i);
                //                    if (IPUtil.toString(x.ipaddr) == ip.toString()) {
                //                        return true;
                //                    }
                //                }
            } else if (TagConstants.serviceHashGroup.hasKey(key)) {
                val serviceLv = mv.getList(key);
                for (i <- 0 to serviceLv.size() - 1) {
                    var service = serviceLv.get(i).toJavaObject();
                    if (x.service == service) {
                        return true;
                    }
                }
            } else if (key == TagConstants.NAME_REFERER) {
                val refererLv = mv.getList(key);
                for (i <- 0 to refererLv.size() - 1) {
                    var referer = refererLv.get(i).toJavaObject();
                    if (x.referer == referer) {
                        return true;
                    }
                }
            } else if (key == TagConstants.NAME_GROUP) {
                val groupLv = mv.getList(key);
                for (i <- 0 to groupLv.size() - 1) {
                    var group = groupLv.get(i).toJavaObject();
                    if (x.group == group) {
                        return true;
                    }
                }
                //            } else if (key == TagConstants.NAME_APITIME) {
                //                if (x.apicallTime >= 1000) {
                //                    val apitimeLv = mv.getList(key);
                //                    for (i <- 0 to apitimeLv.size() - 1) {
                //                        var apitime = apitimeLv.getInt(i);
                //                        apitime match {
                //                            case 1 => if (1000 <= x.apicallTime && x.apicallTime < 3000) return true;
                //                            case 3 => if (3000 <= x.apicallTime && x.apicallTime < 8000) return true;
                //                            case 8 => if (8000 <= x.apicallTime) return true;
                //                        }
                //                    }
                //                }
                //            } else if (key == TagConstants.NAME_SQLTIME) {
                //                if (x.sqlTime >= 1000) {
                //                    val sqltimeLv = mv.getList(key);
                //                    for (i <- 0 to sqltimeLv.size() - 1) {
                //                        var sqltime = sqltimeLv.getInt(i);
                //                        sqltime match {
                //                            case 1 => if (1000 <= x.sqlTime && x.sqlTime < 3000) return true;
                //                            case 3 => if (3000 <= x.sqlTime && x.sqlTime < 8000) return true;
                //                            case 8 => if (8000 <= x.sqlTime) return true;
                //                        }
                //                    }
                //                }
                //            } else if (key == TagConstants.NAME_ELAPSED) {
                //                if (x.elapsed >= 1000) {
                //                    val elapsedLv = mv.getList(key);
                //                    for (i <- 0 to elapsedLv.size() - 1) {
                //                        var elapsed = elapsedLv.getInt(i);
                //                        elapsed match {
                //                            case 1 => if (1000 <= x.elapsed && x.elapsed < 3000) return true;
                //                            case 3 => if (3000 <= x.elapsed && x.elapsed < 8000) return true;
                //                            case 8 => if (8000 <= x.elapsed) return true;
                //                        }
                //                    }
                //                }
            }
        }
        return false;
    }

    def alertFilterOk(mv: MapValue, x: AlertPack): Boolean = {
        val itr = mv.keys();
        while (itr.hasMoreElements()) {
            val key = itr.nextElement();
            if (key == TagConstants.NAME_OBJECT) {
                val objHashLv = mv.getList(key);
                for (i <- 0 to objHashLv.size() - 1) {
                    var objHash = objHashLv.get(i).toJavaObject();
                    if (x.objHash == objHash) {
                        return true;
                    }
                }
            } else if (key == TagConstants.NAME_LEVEL) {
                val levelLv = mv.getList(key);
                for (i <- 0 to levelLv.size() - 1) {
                    var level = levelLv.getInt(i);
                    if (x.level == level) {
                        return true;
                    }
                }
            } else if (key == TagConstants.NAME_TITLE) {
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