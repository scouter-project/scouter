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
import scouter.lang.CounterKey
import scouter.lang.TimeTypeEnum
import scouter.lang.pack.MapPack
import scouter.lang.value.DoubleValue
import scouter.lang.value.ListValue
import scouter.lang.value.NullValue
import scouter.lang.value.Value
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.net.RequestCmd
import scouter.net.TcpFlag
import scouter.server.core.AgentManager
import scouter.server.db.DailyCounterRD
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.util.CastUtil
import scouter.util.DateUtil
import scouter.util.ArrayUtil

class DailyCounterService {

    @ServiceHandler(RequestCmd.COUNTER_TODAY)
    def getDailyCounter(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val objHash = param.getInt("objHash");
        val counter = param.getText("counter");
        val timetype = param.getInt("timetype").toByte

        val date = DateUtil.yyyymmdd();
        val stime = DateUtil.yyyymmdd(date);

        val ck = new CounterKey(objHash, counter, timetype);
        val v = DailyCounterRD.getValues(date, ck);

        val mpack = new MapPack();
        val timeLv = mpack.newList("time");
        val valueLv = mpack.newList("value");

        val delta = TimeTypeEnum.getTime(ck.timetype);
        for (i <- 0 to ArrayUtil.len(v) - 1) {

            val time = stime + delta * i;
            var value = v(i);
            if (value == null)
                value = new NullValue();

            timeLv.add(time);
            valueLv.add(value);
        }

        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(mpack);
    }

    @ServiceHandler(RequestCmd.COUNTER_TODAY_ALL)
    def getDailyCounterAll(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val counter = param.getText("counter");
        val objType = param.getText("objType");
        if (StringUtil.isEmpty(objType)) {
            System.out.println("please check.. COUNTER_TODAY_ALL objType is null");
            return ;
        }
        val date = DateUtil.yyyymmdd();
        val stime = DateUtil.yyyymmdd(date);

        val objPackMap = AgentManager.getCurrentObjects(objType);
        val objHashLv = objPackMap.getList("objHash");
        // ListValue objNameLv = agentGrp.getList("objName");
        for (i <- 0 to ArrayUtil.len(objHashLv) - 1) {
            val objHash = objHashLv.getInt(i);
          try {
            val ck = new CounterKey(objHash, counter, TimeTypeEnum.FIVE_MIN);
            val v = DailyCounterRD.getValues(date, ck);
            if (v != null) {
                val mpack = new MapPack();
                mpack.put("objHash", objHash);
                // mpack.put("objName", objNameLv.getString(i));
                val timeLv = mpack.newList("time");
                val valueLv = mpack.newList("value");

                val delta = TimeTypeEnum.getTime(ck.timetype);
                for (j <- 0 to v.length - 1) {
                    val time = stime + delta * j;
                    var value = v(j);
                    if (value == null)
                        value = new NullValue();

                    timeLv.add(time);
                    valueLv.add(value);
                }
                dout.writeByte(TcpFlag.HasNEXT);
                dout.writePack(mpack);
            }
          } catch {
            case e: Throwable =>
              val op = AgentManager.getAgent(objHash);
              println(op.objName + " invalid data : " + e.getMessage())
              e.printStackTrace()
          }
        }
    }

    @ServiceHandler(RequestCmd.COUNTER_TODAY_TOT)
    def getRealDateTotal(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val inout = din.readMapPack();
        val counter = inout.getText("counter");
        val mode = inout.getText("mode");
        val objType = inout.getText("objType");
        if (StringUtil.isEmpty(objType)) {
            System.out.println("please check.. COUNTER_TODAY_TOT objType is null");
            return ;
        }
        val date = DateUtil.yyyymmdd();

        val period = TimeTypeEnum.FIVE_MIN;
        val delta = TimeTypeEnum.getTime(period);
        val timeLength = (TimeTypeEnum.getTime(TimeTypeEnum.DAY) / delta).toInt
        val values = Array[Double](timeLength);
        val cnt = Array[Int](timeLength);

        val objPackMap = AgentManager.getCurrentObjects(objType);
        val objHashLv = objPackMap.getList("objHash");
        for (i <- 0 to ArrayUtil.len(objHashLv) - 1) {
            val objHash = objHashLv.getInt(i);
            try {
              val ck = new CounterKey(objHash, counter, period);
              val v = DailyCounterRD.getValues(date, ck);
              for (j <- 0 to ArrayUtil.len(v) - 1) {
                  val value = v(j);
                  var doubleValue = if (value == null) 0.0 else CastUtil.cdouble(value);
                  if (doubleValue > 0) {
                      cnt(j) += 1;
                      values(j) += doubleValue;
                  }
              }
            } catch {
               case e: Throwable =>
                val op = AgentManager.getAgent(objHash);
                println(op.objName + " invalid data : " + e.getMessage())
                e.printStackTrace()
           }
        }
        val stime = DateUtil.yyyymmdd(date);
        val mpack = new MapPack();
        val timeLv = mpack.newList("time");
        val valueLv = mpack.newList("value");
        val isAvg = "avg".equalsIgnoreCase(mode)

        for (i <- 0 to values.length - 1) {
            timeLv.add(stime + (delta * i));
            if (isAvg && cnt(i) > 1) {
                values(i) = values(i) / cnt(i);
            }
            valueLv.add(new DoubleValue(values(i)));
        }
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(mpack);
    }

    @ServiceHandler(RequestCmd.COUNTER_PAST_DATE_TOT)
    def getPastDateTotalCounter(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val counter = param.getText("counter");
        val date = param.getText("date");
        val mode = param.getText("mode");
        val objType = param.getText("objType");
        if (StringUtil.isEmpty(objType)) {
            System.out.println("please check.. COUNTER_PAST_DATE_TOT objType is null");
            return ;
        }

        val period = TimeTypeEnum.FIVE_MIN;
        val delta = TimeTypeEnum.getTime(period);
        val timeLength = (TimeTypeEnum.getTime(TimeTypeEnum.DAY) / delta).toInt
        val values = new Array[Double](timeLength);
        val cnt = new Array[Int](timeLength);

        val agentGrp = AgentManager.getDailyObjects(date, objType);
        val objHashLv = agentGrp.getList("objHash");
        for (i <- 0 to ArrayUtil.len(objHashLv) - 1) {
            val objHash = objHashLv.getInt(i);
            val ckey = new CounterKey(objHash, counter, period);
            val outvalue = DailyCounterRD.getValues(date, ckey);
            for (j <- 0 to ArrayUtil.len(outvalue) - 1) {
                val value = outvalue(j);
                if (value != null) {
                    cnt(j) += 1;
                    values(j) += CastUtil.cdouble(value);
                }
            }

        }

        val stime = DateUtil.yyyymmdd(date);
        val mpack = new MapPack();
        val timeLv = mpack.newList("time");
        val valueLv = mpack.newList("value");

        val isAvg = "avg".equalsIgnoreCase(mode);

        for (i <- 0 to values.length - 1) {
            timeLv.add(stime + (delta * i));
            if (isAvg && cnt(i) > 1) {
                values(i) /= cnt(i);
            }
            valueLv.add(new DoubleValue(values(i)));
        }
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(mpack);
    }

    @ServiceHandler(RequestCmd.COUNTER_PAST_DATE_ALL)
    def getPastDateAll(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val counter = param.getText("counter");
        val date = param.getText("date");
        val objType = param.getText("objType");
        if (StringUtil.isEmpty(objType)) {
            System.out.println("please check.. COUNTER_LOAD_DATE_ALL objType is null");
            return ;
        }
        val agentGrp = AgentManager.getDailyObjects(date, objType);
        val stime = DateUtil.yyyymmdd(date);
        val objHashLv = agentGrp.getList("objHash");
        for (i <- 0 to ArrayUtil.len(objHashLv) - 1) {

            val objHash = objHashLv.getInt(i);

            val mpack = new MapPack();
            mpack.put("objHash", objHash);
            val timeLv = mpack.newList("time");
            val valueLv = mpack.newList("value");

            val v = DailyCounterRD.getValues(date, new CounterKey(objHash, counter, TimeTypeEnum.FIVE_MIN));

            for (j <- 0 to ArrayUtil.len(v) - 1) {
                val time = stime + DateUtil.MILLIS_PER_MINUTE * 5 * j;
                timeLv.add(time);
                valueLv.add(v(j));
            }

            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(mpack);
            dout.flush();
        }
    }

    @ServiceHandler(RequestCmd.COUNTER_PAST_LONGDATE_ALL)
    def getPastLongDateAll(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val counter = param.getText("counter");
        val sDate = param.getText("sDate");
        val eDate = param.getText("eDate");
        val objType = param.getText("objType");
        val objHashParamLv = param.getList("objHash");

        if (StringUtil.isEmpty(objType) && (objHashParamLv == null || objHashParamLv.size() == 0)) {
            System.out.println("please check.. COUNTER_LOAD_TIME_ALL objType is null");
            return ;
        }

        var objHashLv = if(objHashParamLv != null && objHashParamLv.size() > 0)
            objHashParamLv else AgentManager.getPeriodicObjects(sDate, eDate, objType).getList("objHash")

        var stime = DateUtil.yyyymmdd(sDate);
        var etime = DateUtil.yyyymmdd(eDate) + DateUtil.MILLIS_PER_DAY;

        var date = stime;
        while (date <= (etime - DateUtil.MILLIS_PER_DAY)) {
            val d = DateUtil.yyyymmdd(date);
            for (i <- 0 to ArrayUtil.len(objHashLv) - 1) {
                val objHash = objHashLv.getInt(i);
                try {
                  val mpack = new MapPack();
                  mpack.put("objHash", objHash);
                  val timeLv = mpack.newList("time");
                  val valueLv = mpack.newList("value");
  
                  val v = DailyCounterRD.getValues(d, new CounterKey(objHash, counter, TimeTypeEnum.FIVE_MIN));
  
                  for (j <- 0 to ArrayUtil.len(v) - 1) {
                      val time = date + DateUtil.MILLIS_PER_MINUTE * 5 * j;
                      timeLv.add(time);
                      valueLv.add(v(j));
                  }
                  dout.writeByte(TcpFlag.HasNEXT);
                  dout.writePack(mpack);
                  dout.flush();
                } catch {
                   case e: Throwable =>
                    val op = AgentManager.getAgent(objHash);
                    println(op.objName + " invalid data : " + e.getMessage())
                    e.printStackTrace()
                }
            }
            date += DateUtil.MILLIS_PER_DAY;
        };

    }

    @ServiceHandler(RequestCmd.COUNTER_PAST_LONGDATE_TOT)
    def getPastLongDateTot(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val counter = param.getText("counter");
        val sDate = param.getText("sDate");
        val eDate = param.getText("eDate");
        val mode = param.getText("mode");
        val objType = param.getText("objType");
        if (StringUtil.isEmpty(objType)) {
            System.out.println("please check.. COUNTER_PAST_LONGDATE_TOT objType is null");
            return ;
        }

        val stime = DateUtil.yyyymmdd(sDate);
        val etime = DateUtil.yyyymmdd(eDate) + DateUtil.MILLIS_PER_DAY;

        val period = TimeTypeEnum.FIVE_MIN;
        val delta = TimeTypeEnum.getTime(period);
        val timeLength = ((etime - stime) / delta).toInt
        val values = new Array[Double](timeLength);
        val cnt = new Array[Int](timeLength);

        val agentGrp = AgentManager.getPeriodicObjects(sDate, eDate, objType);

        var date = stime;
        var dayPointer = 0;
        //    val cntPerDay = (TimeTypeEnum.getTime(DAY) / TimeTypeEnum.getTime(FIVE_MIN)).toInt
        val cntPerDay = 288

        while (date <= (etime - DateUtil.MILLIS_PER_DAY)) {
            val d = DateUtil.yyyymmdd(date);

            val objHashLv = agentGrp.getList("objHash");
            for (i <- 0 to ArrayUtil.len(objHashLv) - 1) {
                val objHash = objHashLv.getInt(i);
                try {
                  val ckey = new CounterKey(objHash, counter, period);
                  val outvalue = DailyCounterRD.getValues(d, ckey);
                  for (j <- 0 to ArrayUtil.len(outvalue) - 1) {
                      val value = outvalue(j);
                      var doubleValue = CastUtil.cdouble(value);
  
                      if (doubleValue > 0) {
                          cnt(dayPointer + j) += 1;
                          values(dayPointer + j) += doubleValue;
                      }
                  }
                } catch {
                   case e: Throwable =>
                    val op = AgentManager.getAgent(objHash);
                    println(op.objName + " invalid data : " + e.getMessage())
                    e.printStackTrace()
                }
            }
            date += DateUtil.MILLIS_PER_DAY;
            dayPointer += cntPerDay;
        }

        val t = DateUtil.yyyymmdd(sDate);
        val mpack = new MapPack();
        val timeLv = mpack.newList("time");
        val valueLv = mpack.newList("value");

        val isAvg = "avg".equalsIgnoreCase(mode);

        for (i <- 0 to ArrayUtil.len(values) - 1) {
            timeLv.add(t + (delta * i));
            if (isAvg && cnt(i) > 1) {
                values(i) /= cnt(i);
            }
            val value = new DoubleValue(values(i));
            valueLv.add(value);
        }
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(mpack);
    }

    @ServiceHandler(RequestCmd.COUNTER_PAST_DATE)
    def getPastDate(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();

        val date = param.getText("date");
        val objHash = param.getInt("objHash");
        val counter = param.getText("counter");

        val stime = DateUtil.yyyymmdd(date);

        val mpack = new MapPack();
        val timeLv = mpack.newList("time");
        val valueLv = mpack.newList("value");

        val ckey = new CounterKey(objHash, counter, TimeTypeEnum.FIVE_MIN);
        val outvalue = DailyCounterRD.getValues(date, ckey)
        if (outvalue == null)
            return ;

        for (i <- 0 to ArrayUtil.len(outvalue) - 1) {
            val time = stime + DateUtil.MILLIS_PER_MINUTE * 5 * i;
            val value = outvalue(i);
            timeLv.add(time);
            valueLv.add(value);
        }

        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(mpack);
        dout.flush();
    }

    @ServiceHandler(RequestCmd.GET_COUNTER_EXIST_DAYS)
    def getCounterExistDays(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val counter = param.getText("counter");
        val objType = param.getText("objType");
        val duration = param.getInt("duration");
        val etime = param.getLong("etime");
        val stime = etime - (duration * DateUtil.MILLIS_PER_DAY);

        val p = new MapPack();
        val dateLv = p.newList("date");
        val existLv = p.newList("exist");
        var time = stime;

        for (i <- 0 to duration) {
            var result = false;
            val yyyymmdd = DateUtil.yyyymmdd(time);
            val agentGrp = AgentManager.getDailyObjects(yyyymmdd, objType);
            val objHashLv = agentGrp.getList("objHash");
            for (j <- 0 to ArrayUtil.len(objHashLv) - 1) {
                val objHash = objHashLv.getInt(j);
                val v = DailyCounterRD.getValues(yyyymmdd, new CounterKey(objHash, counter, TimeTypeEnum.FIVE_MIN));
                if (ArrayUtil.len(v) > 0) {
                    result = true;
                }
            }
            dateLv.add(yyyymmdd);
            existLv.add(result);
            time += DateUtil.MILLIS_PER_DAY;
        }
        dout.writeByte(TcpFlag.HasNEXT);
        dout.writePack(p);
    }

    @ServiceHandler(RequestCmd.COUNTER_PAST_DATE_GROUP)
    def getPastDateGroup(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val counter = param.getText("counter");
        val date = param.getText("date");
        val objHashLv = param.getList("objHash");
        val stime = DateUtil.yyyymmdd(date);

        for (i <- 0 to ArrayUtil.len(objHashLv) - 1) {
            val objHash = objHashLv.getInt(i);
            val key = new CounterKey(objHash, counter, TimeTypeEnum.FIVE_MIN);
            val mpack = new MapPack();
            mpack.put("objHash", objHash);
            val timeLv = mpack.newList("time");
            val valueLv = mpack.newList("value");
            val v = DailyCounterRD.getValues(date, key);
            for (j <- 0 to ArrayUtil.len(v) - 1) {
                val time = stime + DateUtil.MILLIS_PER_MINUTE * 5 * j;
                val value = v(j);
                timeLv.add(time);
                valueLv.add(value);
            }
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(mpack);
            dout.flush();
        }
    }

    @ServiceHandler(RequestCmd.COUNTER_TODAY_GROUP)
    def getTodayGroup(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val counter = param.getText("counter");
        val objHashLv = param.getList("objHash");

        val date = DateUtil.yyyymmdd();
        val stime = DateUtil.yyyymmdd(date);

        for (i <- 0 to ArrayUtil.len(objHashLv) - 1) {
            val objHash = objHashLv.getInt(i);
            
            try {
              val ck = new CounterKey(objHash, counter, TimeTypeEnum.FIVE_MIN);
              val v = DailyCounterRD.getValues(date, ck);
  
              val mpack = new MapPack();
              mpack.put("objHash", objHash);
              val timeLv = mpack.newList("time");
              val valueLv = mpack.newList("value");
  
              val delta = TimeTypeEnum.getTime(ck.timetype);
              for (j <- 0 to ArrayUtil.len(v) - 1) {
                  val time = stime + delta * j;
                  val value = if (v(j) != null) v(j) else new NullValue()
                  timeLv.add(time);
                  valueLv.add(value);
              }
              dout.writeByte(TcpFlag.HasNEXT);
              dout.writePack(mpack);
             } catch {
                 case e: Throwable =>
                  val op = AgentManager.getAgent(objHash);
                  println(op.objName + " invalid data : " + e.getMessage())
                  e.printStackTrace()
            }
        }
    }

    @ServiceHandler(RequestCmd.COUNTER_PAST_LONGDATE_GROUP)
    def getPastLongDateGroup(din: DataInputX, dout: DataOutputX, login: Boolean) {
        val param = din.readMapPack();
        val counter = param.getText("counter");
        val stime = param.getLong("stime");
        val etime = param.getLong("etime");
        val lastDay = DateUtil.yyyymmdd(etime);
        val objHashLv = param.getList("objHash");
        for (i <- 0 to ArrayUtil.len(objHashLv) - 1) {
            val objHash = objHashLv.getInt(i);
            try {
              var time = stime;
              val key = new CounterKey(objHash, counter, TimeTypeEnum.FIVE_MIN);
              val mpack = new MapPack();
              mpack.put("objHash", objHash);
              val timeLv = mpack.newList("time");
              val valueLv = mpack.newList("value");
              var date: String = null;
              while (lastDay.equals(date) == false) {
                  date = DateUtil.yyyymmdd(time);
                  val oclock = DateUtil.yyyymmdd(date);
                  val v = DailyCounterRD.getValues(date, key);
                  if (v == null) {
                      for (j <- 0 to 287) {
                          timeLv.add(oclock + DateUtil.MILLIS_PER_FIVE_MINUTE * j);
                          valueLv.add(new NullValue());
                      }
                  } else {
                      for (j <- 0 to v.length - 1) {
                          timeLv.add(oclock + DateUtil.MILLIS_PER_FIVE_MINUTE * j);
                          valueLv.add(v(j));
                      }
                  }
                  time += DateUtil.MILLIS_PER_DAY;
              }
              dout.writeByte(TcpFlag.HasNEXT);
              dout.writePack(mpack);
              dout.flush();
            } catch {
                 case e: Throwable =>
                  val op = AgentManager.getAgent(objHash);
                  println(op.objName + " invalid data : " + e.getMessage())
                  e.printStackTrace()
            }
        }
    }
}