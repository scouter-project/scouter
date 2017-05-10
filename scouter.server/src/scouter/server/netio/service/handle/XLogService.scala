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

import scouter.util.StringUtil
import scouter.lang.TextTypes
import scouter.lang.pack.MapPack
import scouter.lang.pack.Pack
import scouter.lang.pack.PackEnum
import scouter.lang.pack.XLogPack
import scouter.lang.pack.XLogProfilePack
import scouter.lang.value.DecimalValue
import scouter.lang.value.ListValue
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.net.RequestCmd
import scouter.net.TcpFlag
import scouter.server.Configure
import scouter.server.Logger
import scouter.server.core.cache.CacheOut
import scouter.server.core.cache.TextCache
import scouter.server.core.cache.XLogCache
import scouter.server.db.XLogProfileRD
import scouter.server.db.XLogRD
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.util.DateUtil
import scouter.util.IPUtil
import scouter.util.IntSet
import scouter.util.StrMatch
import java.io.IOException
import scouter.server.db.TextRD
import scouter.server.util.EnumerScala
import sun.security.provider.certpath.ForwardBuilder
import scouter.lang.value.Value
import scouter.util.CastUtil

class XLogService {

    @ServiceHandler(RequestCmd.TRANX_PROFILE)
    def getProfile(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();

        var date = param.getText("date");
        val txid = param.getLong("txid");
        val max = param.getInt("max");
        if (StringUtil.isEmpty(date)) {
            date = DateUtil.yyyymmdd(System.currentTimeMillis());
        }
        try {
            val profilePacket = XLogProfileRD.getProfile(date, txid, max);
            if (profilePacket != null) {
                dout.writeByte(TcpFlag.HasNEXT);
                val p = new XLogProfilePack();
                p.profile = profilePacket;
                dout.writePack(p); // ProfilePacket
            }
        } catch {
            case e: Exception => e.printStackTrace();
        }
    }

    @ServiceHandler(RequestCmd.TRANX_PROFILE_FULL)
    def getFullProfile(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();

        var date = param.getText("date");
        val txid = param.getLong("txid");
        val max = -1;
        if (StringUtil.isEmpty(date)) {
            date = DateUtil.yyyymmdd();
        }

        XLogProfileRD.getFullProfile(date, txid, max, (data: Array[Byte]) => {
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writeBlob(data);
        })
    }

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
        val param = din.readMapPack();
        val date = param.getText("date");
        val txid = param.getLong("txid");
        try {
            val xbytes = XLogRD.getByTxid(date, txid);
            if (xbytes != null) {
                dout.writeByte(TcpFlag.HasNEXT);
                dout.write(xbytes);
                dout.flush();
            }
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

        val serviceMatch = if (service == null) null else new StrMatch(service);
        val ipMatch = if (ip == null) null else new StrMatch(ip);
        val loginMatch = if (login == null) null else new StrMatch(login);
        val descMatch = if (desc == null) null else new StrMatch(desc);
        val text1Match = if (text1 == null) null else new StrMatch(text1);
        val text2Match = if (text2 == null) null else new StrMatch(text2);

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
            if (loadCount >= 500) {
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

            if (ok) {
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
