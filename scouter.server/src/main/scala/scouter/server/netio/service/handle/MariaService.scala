/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package scouter.server.netio.service.handle;

import java.util.HashMap
import java.util.HashSet
import scouter.lang.CounterKey
import scouter.lang.DigestKey
import scouter.lang.TimeTypeEnum
import scouter.lang.constants.StatusConstants
import scouter.lang.pack.MapPack
import scouter.lang.pack.StatusPack
import scouter.lang.value.DoubleValue
import scouter.lang.value.FloatValue
import scouter.lang.value.MapValue
import scouter.io.DataInputX
import scouter.io.DataOutputX
import scouter.net.TcpFlag
import scouter.server.core.cache.CounterCache
import scouter.server.core.cache.StatusCache
import scouter.server.db.DailyCounterRD
import scouter.server.db.StatusRD
import scouter.server.netio.service.anotation.ServiceHandler
import scouter.util.ArrayUtil
import scouter.util.CastUtil
import scouter.util.DateUtil
import scouter.net.RequestCmd

class MariaService {
  
    @ServiceHandler(RequestCmd.DB_REALTIME_CONNECTIONS)
  def realtimeConnections(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
    val param = din.readMapPack();
    val objHashLv = param.getList("objHash");
    if (objHashLv.size() == 0) {
      return;
    }
    var total = 0L;
    var active = 0L;
    for (i <- 0 to objHashLv.size()-1) {
      val objHash = objHashLv.getInt(i);
      val key1 = new CounterKey(objHash, "THREADS_TOTAL", TimeTypeEnum.REALTIME);
      val v1 = CounterCache.get(key1);
      if (v1 != null) {
        total += CastUtil.clong(v1);
      }
      val key2 = new CounterKey(objHash, "THREADS_RUNNING", TimeTypeEnum.REALTIME);
      val v2 = CounterCache.get(key2);
      if (v2 != null) {
        active += CastUtil.clong(v2);
      }
    }
    val value = new MapValue();
    value.put("total", total);
    value.put("active", active);
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writeValue(value);
  }
    
  @ServiceHandler(RequestCmd.DB_REALTIME_ACTIVITY)
  def realtimeActivity(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
    val param = din.readMapPack();
    val objHashLv = param.getList("objHash");
    if (objHashLv.size() == 0) {
      return;
    }
    var call = 0L;
    var select = 0L;
    var update = 0L;
    var insert = 0L;
    var delete = 0L;
    for (i <- 0 to objHashLv.size()-1) {
      val objHash = objHashLv.getInt(i);
      val key1 = new CounterKey(objHash, "COM_CALL_PROCEDURE", TimeTypeEnum.REALTIME);
      val v1 = CounterCache.get(key1);
      if (v1 != null) {
        call += CastUtil.clong(v1);
      }
      val key2 = new CounterKey(objHash, "COM_SELECT", TimeTypeEnum.REALTIME);
      val v2 = CounterCache.get(key2);
      if (v2 != null) {
        select += CastUtil.clong(v2);
      }
      val key3 = new CounterKey(objHash, "COM_UPDATE", TimeTypeEnum.REALTIME);
      val v3 = CounterCache.get(key3);
      if (v3 != null) {
        update += CastUtil.clong(v3);
      }
      val key4 = new CounterKey(objHash, "COM_INSERT", TimeTypeEnum.REALTIME);
      val v4 = CounterCache.get(key4);
      if (v4 != null) {
        insert += CastUtil.clong(v4);
      }
      val key5 = new CounterKey(objHash, "COM_DELETE", TimeTypeEnum.REALTIME);
      val v5 = CounterCache.get(key5);
      if (v5 != null) {
        delete += CastUtil.clong(v5);
      }
    }
    val value = new MapValue();
    value.put("call", call);
    value.put("select", select);
    value.put("update", update);
    value.put("insert", insert);
    value.put("delete", delete);
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writeValue(value);
  }
  
    @ServiceHandler(RequestCmd.DB_REALTIME_RESPONSE_TIME)
  def realtimeResponseTime(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
    val param = din.readMapPack();
    val objHashLv = param.getList("objHash");
    val time = System.currentTimeMillis() - DateUtil.MILLIS_PER_FIVE_MINUTE;
    for (i <- 0 to objHashLv.size()-1) {
       val objHash = objHashLv.getInt(i);
       val status = StatusCache.get(objHash, StatusConstants.EVENTS_STATEMENTS_SUMMARY_BY_DIGEST);
       if(status != null) {
         val data = status.data;
         val timeLv = data.getList("AVG_TIMER_WAIT");
         val lastSeenLv = data.getList("LAST_SEEN");
         val countLv = data.getList("COUNT_STAR");
         var timeSum = 0.0;
         var totalCnt = 0;
         for (j <- 0 to timeLv.size() - 1) {
           val lastTime = lastSeenLv.getLong(j);
           if (time < lastTime) {
             timeSum += timeLv.getDouble(j) * countLv.getInt(j);
             totalCnt += countLv.getInt(j);
           }
         }
         
         var value = 0.0;
         if (totalCnt > 0) {
            value = timeSum / totalCnt;
         }
         val pack = new MapPack();
         pack.put("objHash", objHash);
         pack.put("value", new DoubleValue(value));
         dout.writeByte(TcpFlag.HasNEXT);
         dout.writePack(pack);
       }
    }
  }
    
  @ServiceHandler(RequestCmd.DB_REALTIME_HIT_RATIO)
  def realtimeHitRatio(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
    val param = din.readMapPack();
    val objHashLv = param.getList("objHash");
    if (objHashLv.size() == 0) {
      return;
    }
    var innodb_buffer = 0.0f;
    var key_cache = 0.0f;
    var query_cache = 0.0f;
    var thread_cache = 0.0f;
    
    var innoCnt = 0;
    var keyCnt = 0;
    var qcacheCnt = 0;
    var tcacheCnt = 0;
    for (i <- 0 to objHashLv.size()-1) {
      val objHash = objHashLv.getInt(i);
      val key1 = new CounterKey(objHash, "INNO_BUF_HIT", TimeTypeEnum.REALTIME);
      val v1 = CounterCache.get(key1);
      if (v1 != null) {
        innodb_buffer += CastUtil.cfloat(v1);
        innoCnt = innoCnt + 1;
      }
      val key2 = new CounterKey(objHash, "KEY_RATIO", TimeTypeEnum.REALTIME);
      val v2 = CounterCache.get(key2);
      if (v2 != null) {
        key_cache += CastUtil.cfloat(v2);
        keyCnt = keyCnt + 1;
      }
      val key3 = new CounterKey(objHash, "QCACHE_RATIO", TimeTypeEnum.REALTIME);
      val v3 = CounterCache.get(key3);
      if (v3 != null) {
        query_cache += CastUtil.cfloat(v3);
        qcacheCnt = qcacheCnt + 1;
      }
      val key4 = new CounterKey(objHash, "TCACHE_RATIO", TimeTypeEnum.REALTIME);
      val v4 = CounterCache.get(key4);
      if (v4 != null) {
        thread_cache += CastUtil.cfloat(v4);
        tcacheCnt = tcacheCnt + 1;
      }
    }
    val pack = new MapPack();
    if (innoCnt > 0) { pack.put("innodb_buffer", new FloatValue(innodb_buffer / innoCnt)) };
    if (keyCnt > 0) {pack.put("key_cache", new FloatValue(key_cache / keyCnt)) };
    if (qcacheCnt > 0) {pack.put("query_cache", new FloatValue(query_cache / qcacheCnt)) };
    if (tcacheCnt > 0) {pack.put("thread_cache", new FloatValue(thread_cache / tcacheCnt))};
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writePack(pack);
  }
    
     @ServiceHandler(RequestCmd.DB_DAILY_CONNECTIONS)
  def dailyConnections(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
    val param = din.readMapPack();
    val objHashLv = param.getList("objHash");
    val sdate = param.getText("sdate");
    val edate = param.getText("edate");
    
    val stime = DateUtil.yyyymmdd(sdate);
    val etime = DateUtil.yyyymmdd(edate) + DateUtil.MILLIS_PER_DAY;

    val period = TimeTypeEnum.FIVE_MIN;
    val delta = TimeTypeEnum.getTime(period);
    val timeLength = ((etime - stime) / delta).asInstanceOf[Int];
    val totalValues = new Array[Double](timeLength);
    val activeValues = new Array[Double](timeLength);
    
    var date = stime;
    var dayPointer = 0;
    val cntPerDay = (TimeTypeEnum.getTime(TimeTypeEnum.DAY) / TimeTypeEnum.getTime(TimeTypeEnum.FIVE_MIN)).asInstanceOf[Int];
    
    while (date <= (etime - DateUtil.MILLIS_PER_DAY)) {
      val d = DateUtil.yyyymmdd(date);
      for (i <- 0 to objHashLv.size()-1) {
	      val objHash = objHashLv.getInt(i);
	      val key1 = new CounterKey(objHash, "THREADS_TOTAL", period);
	      val total = DailyCounterRD.getValues(d, key1);
	      for (j <- 0 to ArrayUtil.len(total) - 1) {
	          val value = total(j);
	          var doubleValue = CastUtil.cdouble(value);
              totalValues(dayPointer + j) += doubleValue;
	      }
	      val key2 = new CounterKey(objHash, "THREADS_RUNNING", period);
	      val active = DailyCounterRD.getValues(d, key2);
	      for (j <- 0 to ArrayUtil.len(active) - 1) {
	          val value = active(j);
	          var doubleValue = CastUtil.cdouble(value);
              activeValues(dayPointer + j) += doubleValue;
	      }
      }
      date += DateUtil.MILLIS_PER_DAY;
      dayPointer += cntPerDay;
    }
    val pack = new MapPack();
    val timeLv = pack.newList("time");
    val totalLv = pack.newList("total");
    val activeLv = pack.newList("active");
    for (i <- 0 to timeLength - 1) {
      timeLv.add(stime + (delta * i));
      totalLv.add(totalValues(i));
      activeLv.add(activeValues(i));
    }
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writePack(pack);
  }
     
      @ServiceHandler(RequestCmd.DB_DAILY_ACTIVITY)
  def dailyActivity(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
    val param = din.readMapPack();
    val objHashLv = param.getList("objHash");
    val sdate = param.getText("sdate");
    val edate = param.getText("edate");
    
    val stime = DateUtil.yyyymmdd(sdate);
    val etime = DateUtil.yyyymmdd(edate) + DateUtil.MILLIS_PER_DAY;

    val period = TimeTypeEnum.FIVE_MIN;
    val delta = TimeTypeEnum.getTime(period);
    val timeLength = ((etime - stime) / delta).asInstanceOf[Int];
    val callValues = new Array[Double](timeLength);
    val selectValues = new Array[Double](timeLength);
    val insertValues = new Array[Double](timeLength);
    val updateValues = new Array[Double](timeLength);
    val deleteValues = new Array[Double](timeLength);
    
    var date = stime;
    var dayPointer = 0;
    val cntPerDay = (TimeTypeEnum.getTime(TimeTypeEnum.DAY) / TimeTypeEnum.getTime(TimeTypeEnum.FIVE_MIN)).asInstanceOf[Int];
    
    while (date <= (etime - DateUtil.MILLIS_PER_DAY)) {
      val d = DateUtil.yyyymmdd(date);
      for (i <- 0 to objHashLv.size()-1) {
	      val objHash = objHashLv.getInt(i);
	      val key1 = new CounterKey(objHash, "COM_CALL_PROCEDURE", period);
	      val call = DailyCounterRD.getValues(d, key1);
	      for (j <- 0 to ArrayUtil.len(call) - 1) {
	          val value = call(j);
	          var doubleValue = CastUtil.cdouble(value);
              callValues(dayPointer + j) += doubleValue;
	      }
	      val key2 = new CounterKey(objHash, "COM_SELECT", period);
	      val select = DailyCounterRD.getValues(d, key2);
	      for (j <- 0 to ArrayUtil.len(select) - 1) {
	          val value = select(j);
	          var doubleValue = CastUtil.cdouble(value);
              selectValues(dayPointer + j) += doubleValue;
	      }
	      val key3 = new CounterKey(objHash, "COM_UPDATE", period);
	      val update = DailyCounterRD.getValues(d, key3);
	      for (j <- 0 to ArrayUtil.len(update) - 1) {
	          val value = update(j);
	          var doubleValue = CastUtil.cdouble(value);
              updateValues(dayPointer + j) += doubleValue;
	      }
	      val key4 = new CounterKey(objHash, "COM_INSERT", period);
	      val insert = DailyCounterRD.getValues(d, key4);
	      for (j <- 0 to ArrayUtil.len(insert) - 1) {
	          val value = insert(j);
	          var doubleValue = CastUtil.cdouble(value);
              insertValues(dayPointer + j) += doubleValue;
	      }
	      val key5 = new CounterKey(objHash, "COM_DELETE", period);
	      val delete = DailyCounterRD.getValues(d, key5);
	      for (j <- 0 to ArrayUtil.len(delete) - 1) {
	          val value = delete(j);
	          var doubleValue = CastUtil.cdouble(value);
              deleteValues(dayPointer + j) += doubleValue;
	      }
      }
      date += DateUtil.MILLIS_PER_DAY;
      dayPointer += cntPerDay;
    }
    val pack = new MapPack();
    val timeLv = pack.newList("time");
    val callLv = pack.newList("call");
    val selectLv = pack.newList("select");
    val insertLv = pack.newList("insert");
    val updateLv = pack.newList("update");
    val deleteLv = pack.newList("delete");
    
    for (i <- 0 to timeLength - 1) {
      timeLv.add(stime + (delta * i));
      callLv.add(callValues(i));
      selectLv.add(selectValues(i));
      insertLv.add(insertValues(i));
      updateLv.add(updateValues(i));
      deleteLv.add(deleteValues(i));
    }
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writePack(pack);
  }
     
     @ServiceHandler(RequestCmd.DB_DIGEST_TABLE)
   def digestTable(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
    val param = din.readMapPack();
    val objHashLv = param.getList("objHash");
    val date = param.getText("date");
    val stime = param.getLong("stime");
    val etime = param.getLong("etime");
    val objSet = new HashSet[Integer]();
    for (i <- 0 to objHashLv.size() - 1) {
      objSet.add(objHashLv.getInt(i));
    }
    
    val summaryMap = new HashMap[DigestKey, MapPack]();
    val lastPackMap = new HashMap[Integer, StatusPack]();
    
    val percentPack = new MapPack();
    percentPack.put("percent", 0);
    
    val handler = (time: Long, data: Array[Byte]) => {
        val pk = new DataInputX(data).readPack().asInstanceOf[StatusPack];
        if (StatusConstants.EVENTS_STATEMENTS_SUMMARY_BY_DIGEST == pk.key) {
          if (objSet.contains(pk.objHash)) {
            val dataMap = pk.data;
            val digestLv = dataMap.getList("DIGEST_TEXT");
            val minTimerWaitLv = dataMap.getList("MIN_TIMER_WAIT");
            val maxTimerWaitLv = dataMap.getList("MAX_TIMER_WAIT");
            val avgTimerWaitLv = dataMap.getList("AVG_TIMER_WAIT");
            val lastSeenLv = dataMap.getList("LAST_SEEN");
            for (i <- 0 to digestLv.size() - 1) {
              val lastSeen = lastSeenLv.getLong(i);
              if (lastSeen >= stime && lastSeen <= etime) {
            	  val digestHash = digestLv.getInt(i);
	              val key = new DigestKey(pk.objHash, digestHash)
	              var summaryValue = summaryMap.get(key);
	              if (summaryValue == null) {
	                summaryValue = new MapPack();
	                summaryValue.put("objHash", pk.objHash);
	                summaryValue.put("digestHash", digestHash);
	                summaryValue.put("MIN_TIMER_WAIT", Long.MaxValue);
	                summaryValue.put("MAX_TIMER_WAIT", Long.MinValue);
	                summaryValue.put("AVG_TIMER_WAIT", 0L);
	                summaryValue.put("count", 0);
	                summaryMap.put(key, summaryValue);
	              }
	              val current_min = minTimerWaitLv.getLong(i);
	              val before_min = summaryValue.getLong("MIN_TIMER_WAIT");
	              if (current_min < before_min) {
	                summaryValue.put("MIN_TIMER_WAIT", current_min);
	              }
	              val current_max = maxTimerWaitLv.getLong(i);
	              val before_max = summaryValue.getLong("MAX_TIMER_WAIT");
	              if (current_max > before_max) {
	                summaryValue.put("MAX_TIMER_WAIT", current_max);
	              }
	              val current_avg = avgTimerWaitLv.getLong(i);
	              val before_avg = summaryValue.getLong("AVG_TIMER_WAIT");
	              summaryValue.put("AVG_TIMER_WAIT", before_avg + current_avg);
	              val before_count = summaryValue.getInt("count");
	              summaryValue.put("count", before_count + 1);
              }
            }
            lastPackMap.put(pk.objHash, pk);
            percentPack.put("percent", ((time-stime) * 100) / (etime-stime));
            dout.writeByte(TcpFlag.HasNEXT);
            dout.writePack(percentPack);
          }
        }
    }
    StatusRD.readByTime(date, stime, etime, handler)
    val itr = summaryMap.values().iterator();
    while (itr.hasNext()) {
    	val mappack = itr.next();
    	dout.writeByte(TcpFlag.HasNEXT);
    	dout.writePack(mappack); // key : objHash + digest  value : summary
    }
    val itr2 = lastPackMap.values().iterator();
    while (itr2.hasNext()) {
      val statuspack = itr2.next();
      dout.writeByte(TcpFlag.HasNEXT);
      dout.writePack(statuspack); // key : objHash   value : last status-pack
    }
   }
     
       @ServiceHandler(RequestCmd.DB_LAST_DIGEST_TABLE)
   def lastDigestTable(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
    val param = din.readMapPack();
    val objHashLv = param.getList("objHash");
    val date = param.getText("date");
    val time = param.getLong("time");
    val objSet = new HashSet[Integer]();
    for (i <- 0 to objHashLv.size() - 1) {
      objSet.add(objHashLv.getInt(i));
    }
    
    var done = false
    val handler = (time: Long, data: Array[Byte]) => {
    	if (done == false) {
		    val pk = new DataInputX(data).readPack().asInstanceOf[StatusPack];
		    if (StatusConstants.EVENTS_STATEMENTS_SUMMARY_BY_DIGEST == pk.key) {
		      if (objSet.contains(pk.objHash)) {
		        dout.writeByte(TcpFlag.HasNEXT);
		        dout.writePack(pk);
		        objSet.remove(pk.objHash);
		        if (objSet.size() == 0) {
		          done = true
		        }
		      }
		    }
    	}
	  }
    val stime = time - DateUtil.MILLIS_PER_FIVE_MINUTE;
    StatusRD.readFromEndTime(date, stime, time, handler);
   }
     
     @ServiceHandler(RequestCmd.DB_MAX_TIMER_WAIT_THREAD)
   def maxTimerWaitThread(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
    val param = din.readMapPack();
    val objHashLv = param.getList("objHash");
    val date = param.getText("date");
    val stime = param.getLong("stime");
    val etime = param.getLong("etime");
    val digest = param.getInt("digest");
    val objSet = new HashSet[Integer]();
    for (i <- 0 to objHashLv.size() - 1) {
      objSet.add(objHashLv.getInt(i));
    }
    
    var pack = new MapPack();
    var timer_wait = 0L;
    
    val handler = (time: Long, data: Array[Byte]) => {
        val pk = new DataInputX(data).readPack().asInstanceOf[StatusPack];
        if (StatusConstants.EVENTS_STATEMENTS_CURRENT == pk.key) {
          if (objSet.contains(pk.objHash)) {
            val mv = pk.data;
            val digestLv = mv.getList("DIGEST_TEXT");
            val threadIdLv = mv.getList("THREAD_ID");
            val timerWaitLv = mv.getList("TIMER_WAIT");
            val lockTimeLv = mv.getList("LOCK_TIME");
            val sqlTextLv = mv.getList("SQL_TEXT");
            val timerStartLv = mv.getList("TIMER_START");
            val timerEndLv = mv.getList("TIMER_END");
        	if (digestLv != null) {
        	  scala.util.control.Breaks.breakable {
	            for (i <- 0 to digestLv.size() - 1) {
	              if (digest == digestLv.getInt(i)) {
	                val timer = timerWaitLv.getLong(i);
	                if (timer > timer_wait) {
	                  timer_wait = timer;
	                  pack.put("time", time);
	                  pack.put("thread_id", threadIdLv.getLong(i));
	                  pack.put("timer", timer);
	                  pack.put("lock_time", lockTimeLv.getLong(i));
	                  pack.put("sql_text", sqlTextLv.getInt(i));
	                }
	                scala.util.control.Breaks.break
	              }
	            }
        	  }
        	}
          }
        }
    }
    StatusRD.readByTime(date, stime, etime, handler)
    dout.writeByte(TcpFlag.HasNEXT);
    dout.writePack(pack);
   }
     
    @ServiceHandler(RequestCmd.DB_LOAD_DIGEST_COUNTER)
   def loadDigestCounter(din: DataInputX, dout: DataOutputX, login: Boolean): Unit = {
    val param = din.readMapPack();
    val objHashLv = param.getList("objHash");
    val date = param.getText("date");
    val stime = param.getLong("stime");
    val etime = param.getLong("etime");
    val digest = param.getInt("digest");
    val column = param.getText("column");
    val valueMap = new HashMap[Integer, MapPack]();
    for (i <- 0 to objHashLv.size() - 1) {
      val p = new MapPack();
      val objHash = objHashLv.getInt(i);
      p.put("objHash", objHash);
      p.newList("time");
      p.newList("value");
      valueMap.put(objHash, p);
    }
    
    val handler = (time: Long, data: Array[Byte]) => {
	    val pk = new DataInputX(data).readPack().asInstanceOf[StatusPack];
	    if (StatusConstants.EVENTS_STATEMENTS_SUMMARY_BY_DIGEST == pk.key) {
	      if (valueMap.containsKey(pk.objHash)) {
			val valuePack = valueMap.get(pk.objHash);
			val timeLv = valuePack.getList("time");
			val valueLv = valuePack.getList("value");
			val mv = pk.data;
			val digestLv = mv.getList("DIGEST_TEXT");
			val targetLv = mv.getList(column);
			scala.util.control.Breaks.breakable {
			  for (i <- 0 to digestLv.size() - 1) {
			    if (digest == digestLv.getInt(i)) {
			      timeLv.add(time);
			      valueLv.add(targetLv.get(i));
			      scala.util.control.Breaks.break
			    }
			  }
			}
	      }
	    }
    }
    StatusRD.readByTime(date, stime, etime, handler)
    
    val itr = valueMap.values().iterator();
    while (itr.hasNext()) {
       dout.writeByte(TcpFlag.HasNEXT);
       dout.writePack(itr.next());
    }
   }
}