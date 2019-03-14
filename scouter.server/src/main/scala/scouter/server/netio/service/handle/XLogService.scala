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

import scouter.io.{DataInputX, DataOutputX}
import scouter.lang.TextTypes
import scouter.lang.constants.ParamConstant
import scouter.lang.pack._
import scouter.lang.step.{Step, StepSingle}
import scouter.lang.value._
import scouter.net.{RequestCmd, TcpFlag}
import scouter.server.Configure
import scouter.server.core.app.SpanStepBuilder
import scouter.server.core.cache.XLogCache
import scouter.server.db.{TextRD, XLogProfileRD, XLogRD, ZipkinSpanRD}
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.server.util.EnumerScala
import scouter.util._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class XLogService {

    @ServiceHandler(RequestCmd.TRANX_PROFILE)
    def getProfile(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
        val param = din.readMapPack()

        var date = param.getText("date")
        val txid = param.getLong("txid")
        var gxid = param.getLong("gxid")
        var xlogType = param.getInt("xlogType")
        val max = param.getInt("max")
        if (StringUtil.isEmpty(date)) {
            date = DateUtil.yyyymmdd()
        }

        try {
            if (gxid == 0) {
                val din = new DataInputX(XLogRD.getByTxid(date, txid))
                val xlog = din.readPack().asInstanceOf[XLogPack];
                gxid = xlog.gxid
                xlogType = xlog.xType
            }

            if(xlogType == XLogTypes.ZIPKIN_SPAN) {
                processGetSpansAsProfile(dout, date, gxid, txid)
            } else {
                processGetProfile(dout, date, txid, max)
            }

        } catch {
            case e: Exception => e.printStackTrace()
        }
    }

    @ServiceHandler(RequestCmd.TRANX_PROFILE_FULL)
    def getFullProfile(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();

        var date = param.getText("date");
        val txid = param.getLong("txid");
        var gxid = param.getLong("gxid")
        var xlogType = param.getInt("xlogType")
        val max = -1;
        if (StringUtil.isEmpty(date)) {
            date = DateUtil.yyyymmdd();
        }

        if (gxid == 0) {
            val din = new DataInputX(XLogRD.getByTxid(date, txid))
            val xlog = din.readPack().asInstanceOf[XLogPack];
            gxid = xlog.gxid
            xlogType = xlog.xType
        }

        if(xlogType == XLogTypes.ZIPKIN_SPAN) {
            processGetSpansAsSteps(dout, date, gxid, txid)
        } else {
            processGetFullProfile(dout, date, txid, max)
        }
    }

    private def processGetSpansAsProfile(dout: DataOutputX, date: String, gxid: Long, txid: Long): Unit = {
        val (stepList, mySpanPack) = getStepsFromSpans(date, gxid, txid)

        import collection.JavaConverters._
        val profilePack = new XLogProfilePack
        profilePack.txid = txid
        profilePack.objHash = mySpanPack.objHash
        profilePack.profile = Step.toBytes(stepList.map(_.asInstanceOf[Step]).asJava)
        profilePack.service = mySpanPack.name
        profilePack.elapsed = mySpanPack.elapsed

        dout.writeByte(TcpFlag.HasNEXT)
        dout.writePack(profilePack)
    }

    private def processGetSpansAsSteps(dout: DataOutputX, date: String, gxid: Long, txid: Long): Unit = {
        val (stepList, mySpanPck) = getStepsFromSpans(date, gxid, txid)

        import collection.JavaConverters._
        dout.writeByte(TcpFlag.HasNEXT)
        dout.writeBlob(Step.toBytes(stepList.map(_.asInstanceOf[Step]).asJava))
    }

    private def getStepsFromSpans(date: String, gxid: Long, txid: Long): (ListBuffer[StepSingle], SpanPack) = {
        import collection.JavaConverters._
        val spanBuffer = new mutable.ListBuffer[SpanPack]
        val spanContainerPackList = ZipkinSpanRD.getByGxid(date, gxid)
        spanContainerPackList.foreach(bytes => {
            val din = new DataInputX(bytes)
            val container = din.readPack.asInstanceOf[SpanContainerPack]
            spanBuffer ++= SpanPack.toObjectList(container.spans).asScala
        })

        SpanStepBuilder.toSteps(gxid, txid, spanBuffer)
    }

    private def getSpansMap(date: String, gxid: Long): Map[Long, SpanPack] = {
        import collection.JavaConverters._
        val spanMap = mutable.HashMap[Long, SpanPack]()
        val spanContainerPackList = ZipkinSpanRD.getByGxid(date, gxid)
        spanContainerPackList.foreach(bytes => {
            val din = new DataInputX(bytes)
            val container = din.readPack.asInstanceOf[SpanContainerPack]
            spanMap ++= SpanPack.toObjectList(container.spans).asScala.map(p => (p.txid, p))
        })
        spanMap.toMap
    }

    private def processGetProfile(dout: DataOutputX, date: String, txid: Long, max: Int): Unit = {
        val profilePacket = XLogProfileRD.getProfile(date, txid, max)
        if (profilePacket != null) {
            dout.writeByte(TcpFlag.HasNEXT)
            val p = new XLogProfilePack()
            p.profile = profilePacket
            dout.writePack(p) // ProfilePacket
        }
    }

    private def processGetFullProfile(dout: DataOutputX, date: String, txid: Long, max: Int): Unit = {
        XLogProfileRD.getFullProfile(date, txid, max, (data: Array[Byte]) => {
            dout.writeByte(TcpFlag.HasNEXT)
            dout.writeBlob(data)
        })
    }

    /**
      * get latest XLog data
      * @param din MapPack{index, loop, objHash[]}
      * @param dout {MapPack{loop, index}, XLogPack[]}
      * @param login
      */
    @ServiceHandler(RequestCmd.TRANX_REAL_TIME_GROUP)
    def getRealtimePerfGroup(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val index = param.getInt("index");
        val loop = param.getLong("loop");
        var limit = param.getInt("limit");
        limit = Math.max(Configure.getInstance().xlog_realtime_lower_bound_ms, limit);
        val objHashLv = param.getList("objHash");

        val intSet = if(objHashLv == null || objHashLv.size() < 1)
                     null
                     else new IntSet(objHashLv.size(), 1.0f)

        EnumerScala.foreach(objHashLv, (obj: DecimalValue) => {
            intSet.add(obj.intValue());
        })

        val d = if(intSet != null)
                XLogCache.get(intSet, loop, index, limit)
                else XLogCache.get(loop, index, limit)

        if (d == null) return ;

        // 첫번째 패킷에 정보를 전송한다.
        val outparam = new MapPack();
        outparam.put("loop", new DecimalValue(d.loop));
        outparam.put("index", new DecimalValue(d.index));
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(outparam);

        EnumerScala.forward(d.data, (p: Array[Byte]) => {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.write(p);
        })

    }

    /**
      * get latest XLog data
      * @param din MapPack{index, loop, count, objHash[]}
      * @param dout {MapPack{loop, index}, XLogPack[]}
      * @param login
      */
    @ServiceHandler(RequestCmd.TRANX_REAL_TIME_GROUP_LATEST)
    def getRealtimePerfGroupLatestCount(din: DataInputX, dout: DataOutputX, login: Boolean) {

        val param = din.readMapPack();
        val index = param.getInt("index");
        val loop = param.getLong("loop");
        var count = param.getInt("count");

        val objHashLv = param.getList("objHash");

        val objHashSet = if(objHashLv == null || objHashLv.size() < 1)
            null
        else new IntSet(objHashLv.size(), 1.0f)

        EnumerScala.foreach(objHashLv, (obj: DecimalValue) => {
            objHashSet.add(obj.intValue());
        })

        val d = if(objHashSet != null)
                    XLogCache.getWithinCount(objHashSet, loop, index, count)
                else XLogCache.getWithinCount(loop, index, count)

        if (d == null)
            return ;

        // 첫번째 패킷에 정보를 전송한다.
        val outparam = new MapPack();
        outparam.put("loop", new DecimalValue(d.loop));
        outparam.put("index", new DecimalValue(d.index));
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(outparam);

        EnumerScala.forward(d.data, (p: Array[Byte]) => {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.write(p);
        })

    }

    /**
      * get past XLog data
      * @param din MapPack{date, stime, etime, max, reverse, objHash[]}
      * @param dout XLogPack[]
      * @param login
      */
    @ServiceHandler(RequestCmd.TRANX_LOAD_TIME_GROUP)
    def getHistoryPerfGroup(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val date = param.getText("date");
        val stime = param.getLong("stime");
        val etime = param.getLong("etime");
        val limitTime = param.getInt("limit");
        val limit = Math.max(Configure.getInstance().xlog_pasttime_lower_bound_ms, limitTime);
        val max = param.getInt("max");
        val rev = param.getBoolean("reverse");
        val objHashLv = param.getList("objHash");
        if (objHashLv == null || objHashLv.size() < 1) {
            return ;
        }

        val objHashSet = new IntSet();
        EnumerScala.foreach(objHashLv, (obj: DecimalValue) => {
            objHashSet.add(obj.intValue());
        })

        var cnt = 0;
        val handler = (time: Long, data: Array[Byte]) => {
            val x = new DataInputX(data).readPack().asInstanceOf[XLogPack];
            if (objHashSet.contains(x.objHash) && x.elapsed > limit) {
                dout.writeByte(TcpFlag.HasNEXT);
                dout.write(data);
                dout.flush();
                cnt += 1;
            }
            if (max > 0 && cnt >= max) {
                return ;
            }
        }

        if (rev) {
            XLogRD.readFromEndTime(date, stime, etime, handler)
        } else {
            XLogRD.readByTime(date, stime, etime, handler);
        }
    }

    /**
      * get past XLog data by time & object
      *
      * date - String: yyyymmdd
      * stime - long: scan start time (ms)
      * etime - long: scan end time (ms)
      * pageCount - xlog count to get a time
      * lastBucketTime - from previous paging result
      * txid - (last xlog txid) from previous paging result
      * objHash[] - object hashes to retrieve xlog
      * @param din MapPack{date, stime, etime, pageCount, lastBucketTime, txid, objHash[]}
      * @param dout XLogPack[]
      * @param login
      */
    @ServiceHandler(RequestCmd.TRANX_LOAD_TIME_GROUP_V2)
    def getHistoryPerfGroupV2(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack()
        val date = param.getText(ParamConstant.DATE)
        val stime = param.getLong(ParamConstant.XLOG_START_TIME)
        val etime = param.getLong(ParamConstant.XLOG_END_TIME)
        val lastBucketTime = param.getLong(ParamConstant.XLOG_LAST_BUCKET_TIME)
        val txid = param.getLong(ParamConstant.XLOG_TXID)
        val limitCount = param.getInt(ParamConstant.XLOG_PAGE_COUNT)
        val objHashLv = param.getList(ParamConstant.OBJ_HASH)

        if (objHashLv == null || objHashLv.size() < 1) {
            writeHistoryPerfGroupV2MetaPack(dout, false, 0, 0)
            return
        }
        if((txid == 0 && lastBucketTime != 0) || (txid != 0 && lastBucketTime == 0)) {
            writeHistoryPerfGroupV2MetaPack(dout, false, 0, 0)
            return
        }
        if(limitCount == 0) {
            writeHistoryPerfGroupV2MetaPack(dout, false, 0, 0)
            return
        }

        val objHashSet = new IntSet()
        EnumerScala.foreach(objHashLv, (obj: DecimalValue) => {
            objHashSet.add(obj.intValue())
        })

        var lastTime = 0L
        var lastData: XLogPack = null
        var count: Int = 0;

        var start = true
        if(txid != 0L) {
            start = false
        }

        var hasMore = false

        val handler = (time: Long, data: Array[Byte]) =>  {
            val xLog = new DataInputX(data).readPack().asInstanceOf[XLogPack]
            if(!start) {
                if(xLog.txid == txid) {
                    start = true
                }
            } else {
                if (objHashSet.contains(xLog.objHash)) {
                    if(count < limitCount) {
                        dout.writeByte(TcpFlag.HasNEXT)
                        dout.write(data)
                        dout.flush()
                        lastTime = time
                        lastData = xLog
                    } else {
                        hasMore = true
                    }
                    count += 1
                }
            }
            count
        }
        XLogRD.readByTimeLimitCount(date, stime, etime, lastBucketTime, limitCount, handler)

        if(lastTime > 0L) {
            writeHistoryPerfGroupV2MetaPack(dout, hasMore, lastTime, lastData.txid)
        } else {
            writeHistoryPerfGroupV2MetaPack(dout, false, 0, 0)
        }
    }

    def writeHistoryPerfGroupV2MetaPack(dout: DataOutputX, hasMore: Boolean, lastTime: Long, lastTxid: Long): Unit = {
        val metaPack = new MapPack();
        metaPack.put(ParamConstant.XLOG_RESULT_HAS_MORE, new BooleanValue(hasMore))
        metaPack.put(ParamConstant.XLOG_RESULT_LAST_TIME, new DecimalValue(lastTime))
        metaPack.put(ParamConstant.XLOG_RESULT_LAST_TXID, new DecimalValue(lastTxid))

        dout.writeByte(TcpFlag.HasNEXT)
        dout.writePack(metaPack)
    }

    @ServiceHandler(RequestCmd.XLOG_READ_BY_GXID)
    def readByGxId(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val date = param.getText("date");
        val gxid = param.getLong("gxid");
        try {
            val list = XLogRD.getByGxid(date, gxid);
            EnumerScala.forward(list, (xlog: Array[Byte]) => {
                dout.writeByte(TcpFlag.HasNEXT);
                dout.write(xlog);
                dout.flush();
            })

        } catch {
            case e: Exception => {}
        }
    }

    @ServiceHandler(RequestCmd.XLOG_READ_BY_TXID)
    def readByTxId(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack()
        val date = param.getText("date")
        val txid = param.getLong("txid")
        val gxid = param.getLong("gxid")
        try {
            var xbytes = XLogRD.getByTxid(date, txid)
            var xbytesChecked = false

            if (xbytes == null && gxid != 0) {
                val spanMap = getSpansMap(date, gxid)
                if (spanMap.nonEmpty) {
                    val superTxId = getXLoggableParent(txid, spanMap)
                    xbytes = XLogRD.getByTxid(date, superTxId)
                    xbytesChecked = true
                }
            }

            if (xbytes != null) {
                if (!xbytesChecked) {
                    val xlog = new DataInputX(xbytes).readPack().asInstanceOf[XLogPack]
                    if ((xlog.xType == XLogTypes.ZIPKIN_SPAN || xlog.b3Mode) && xlog.caller != 0 && xlog.caller != xlog.gxid) {
                        val spanMap = getSpansMap(date, xlog.gxid)
                        xlog.caller = getXLoggableParent(xlog.caller, spanMap)
                        xbytes = new DataOutputX().writePack(xlog).toByteArray
                    }
                }
                dout.writeByte(TcpFlag.HasNEXT)
                dout.write(xbytes)
                dout.flush()
            }
        } catch {
            case e: Exception => {}
        }
    }

    private def getXLoggableParent(caller: Long, map: Map[Long, SpanPack]): Long = {
        if (!map.contains(caller)) {
            0
        } else {
            val pack = map(caller)
            if (SpanTypes.isParentXLoggable(pack.spanType)) {
                caller
            } else {
                getXLoggableParent(pack.caller, map)
            }
        }

    }

    @ServiceHandler(RequestCmd.XLOG_LOAD_BY_TXIDS)
    def loadByTxIds(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack()
        val date = param.getText("date")
        val txidLv = param.getList("txid")

        var loadCount = 0
        try {
            EnumerScala.foreach(txidLv, (txidValue: DecimalValue) => {
                loadCount += 1;

                if (loadCount >= Configure.getInstance().req_search_xlog_max_count) {
                    return;
                }
                val xbytes = XLogRD.getByTxid(date, txidValue.longValue())
                if(xbytes != null) {
                    //TODO adjust caller in the case of Sapn
                    dout.writeByte(TcpFlag.HasNEXT)
                    dout.write(xbytes)
                    dout.flush()
                }
            })
        } catch {
            case e: Exception => {}
        }
    }

    @ServiceHandler(RequestCmd.XLOG_LOAD_BY_GXID)
    def loadByGxId(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val stime = param.getLong("stime");
        val etime = param.getLong("etime");
        val gxid = param.getLong("gxid");
        val date = DateUtil.yyyymmdd(stime);
        val date2 = DateUtil.yyyymmdd(etime);
        try {
            val list = XLogRD.getByGxid(date, gxid);

            EnumerScala.forward(list, (xlog: Array[Byte]) => {
                //TODO adjust caller in the case of Sapn
                dout.writeByte(TcpFlag.HasNEXT);
                dout.write(xlog);
                dout.flush();
            })

        } catch {
            case e: Exception => {}
        }
        if (date.equals(date2) == false) {
            try {
                val list = XLogRD.getByGxid(date2, gxid);

                EnumerScala.forward(list, (xlog: Array[Byte]) => {
                    //TODO adjust caller in the case of Sapn
                    dout.writeByte(TcpFlag.HasNEXT);
                    dout.write(xlog);
                    dout.flush();
                });

            } catch {
                case e: Exception => {}
            }
        }
    }

    @ServiceHandler(RequestCmd.QUICKSEARCH_XLOG_LIST)
    def quickSearchXlogList(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val date = param.getText("date");
        val txid = param.getLong("txid");
        val gxid = param.getLong("gxid");
        if (txid != 0) {
            try {
                val xbytes = XLogRD.getByTxid(date, txid);
                if (xbytes != null) {
                    //TODO adjust caller in the case of Sapn
                    dout.writeByte(TcpFlag.HasNEXT);
                    dout.write(xbytes);
                    dout.flush();
                }
            } catch {
                case e: Exception => {}
            }
        }
        if (gxid != 0) {
            try {
                val list = XLogRD.getByGxid(date, gxid);
                if (list == null)
                    return ;
                for (i <- 0 to list.size() - 1) {
                    val xlog = list.get(i);
                    //TODO adjust caller in the case of Sapn
                    dout.writeByte(TcpFlag.HasNEXT);
                    dout.write(xlog);
                    dout.flush();
                }
            } catch {
                case e: Exception => {}
            }
        }
    }

    @ServiceHandler(RequestCmd.SEARCH_XLOG_LIST)
    def searchXlogList(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val stime = param.getLong("stime");
        val etime = param.getLong("etime");
        val service = param.getText("service");
        val objHash = param.getInt("objHash");
        val ip = param.getText("ip");
        val login = param.getText("login");
        val desc = param.getText("desc");
        val text1 = param.getText("text1");
        val text2 = param.getText("text2");
        val text3 = param.getText("text3");
        val text4 = param.getText("text4");
        val text5 = param.getText("text5");

        val serviceMatch = if (service == null) null else new StrMatch(service);
        val ipMatch = if (ip == null) null else new StrMatch(ip);
        val loginMatch = if (login == null) null else new StrMatch(login);
        val descMatch = if (desc == null) null else new StrMatch(desc);
        val text1Match = if (text1 == null) null else new StrMatch(text1);
        val text2Match = if (text2 == null) null else new StrMatch(text2);
        val text3Match = if (text3 == null) null else new StrMatch(text3);
        val text4Match = if (text4 == null) null else new StrMatch(text4);
        val text5Match = if (text5 == null) null else new StrMatch(text5);

        val date = DateUtil.yyyymmdd(stime);
        val date2 = DateUtil.yyyymmdd(etime);
        var mtime = 0L;
        var twoDays = false;
        var loadCount = 0;
        if (date.equals(date2) == false) {
            mtime = DateUtil.yyyymmdd(date2);
            twoDays = true;
        }

        val handler = (time: Long, data: Array[Byte]) => {
            if (loadCount >= Configure.getInstance().req_search_xlog_max_count) {
                return ;
            }
            val x = new DataInputX(data).readPack().asInstanceOf[XLogPack];
            var ok = true
            if (ipMatch != null) {
                if (x.ipaddr == null) {
                    ok = false;
                }
                if (ipMatch.include(IPUtil.toString(x.ipaddr)) == false) {
                    ok = false;
                }
            }
            if (objHash != 0 && x.objHash != objHash) {
                ok = false;
            }
            if (serviceMatch != null) {
                var serviceName = TextRD.getString(DateUtil.yyyymmdd(time), TextTypes.SERVICE, x.service);
                if (serviceMatch.include(serviceName) == false) {
                    ok = false;
                }
            }
            if (loginMatch != null) {
                var loginName = TextRD.getString(DateUtil.yyyymmdd(time), TextTypes.LOGIN, x.login);
                if (loginMatch.include(loginName) == false) {
                    ok = false;
                }
            }
            if (descMatch != null) {
                var descName = TextRD.getString(DateUtil.yyyymmdd(time), TextTypes.DESC, x.desc);
                if (descMatch.include(descName) == false) {
                    ok = false;
                }
            }

            if (text1Match != null) {
                var text1Name = x.text1;
                if (text1Match.include(text1Name) == false) {
                    ok = false;
                }
            }

            if (text2Match != null) {
                var text2Name = x.text2;
                if (text2Match.include(text2Name) == false) {
                    ok = false;
                }
            }

            if (text3Match != null) {
                var text3Name = x.text3;
                if (text3Match.include(text3Name) == false) {
                    ok = false;
                }
            }

            if (text4Match != null) {
                var text4Name = x.text4;
                if (text4Match.include(text4Name) == false) {
                    ok = false;
                }
            }

            if (text5Match != null) {
                var text5Name = x.text5;
                if (text5Match.include(text5Name) == false) {
                    ok = false;
                }
            }

            if (ok) {
                //TODO adjust caller in the case of Sapn
                dout.writeByte(TcpFlag.HasNEXT);
                dout.write(data);
                dout.flush();
                loadCount += 1;
            }
        }

        if (twoDays) {
            XLogRD.readByTime(date, stime, mtime - 1, handler);
            XLogRD.readByTime(date2, mtime, etime, handler);
        } else {
            XLogRD.readByTime(date, stime, etime, handler);
        }
    }

}
