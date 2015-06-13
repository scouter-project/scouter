package scouter.server.db;

import scouter.lang.value.MapValue
import scouter.server.db.counter.RealtimeCounterDBHelper
import scouter.util.DateUtil
import scouter.util.FileUtil
import scouter.util.HashUtil
import scouter.util.StringEnumer

object RealtimeCounterRD {
    def read(objName: String, date: String, handler: (Long, MapValue) => Any) {
        val stime = DateUtil.yyyymmdd(date);
        val etime = stime + DateUtil.MILLIS_PER_DAY;
        read(objName, date, stime, etime, handler);
    }

    def read(objName: String, date: String, stime: Long, etime: Long, handler: (Long, MapValue) => Any) {
        if (objName == null)
            return ;
        var perfdb: RealtimeCounterDBHelper = null;
        try {
            perfdb = new RealtimeCounterDBHelper().open(objName, date, true);
            if (perfdb == null)
                return ;
            perfdb.counterIndex.read(HashUtil.hash(objName), stime, etime, handler, perfdb.counterDbHeader.getTagIntStr(), perfdb.counterData.read);
        } catch {
            case e: Exception => e.printStackTrace();
        } finally {
            FileUtil.close(perfdb);
        }
    }

    def getCounterSet(objName: String, date: String): StringEnumer = {
        if (objName == null)
            return null;
        var logdb: RealtimeCounterDBHelper = null;
        try {
            logdb = new RealtimeCounterDBHelper().open(objName, date, true);
            if (logdb == null)
                return null;
            return logdb.counterDbHeader.getTagStrInt().keys();
        } catch {
            case e: Exception => e.printStackTrace();
        } finally {
            FileUtil.close(logdb);
        }
        return null;
    }

    def readFromEnd(objName: String, date: String, handler: (Long, MapValue) => Boolean) {
        val stime = DateUtil.yyyymmdd(date);
        val etime = stime + DateUtil.MILLIS_PER_DAY;
        readFromEnd(objName, date, stime, etime, handler);
    }

    def readFromEnd(objName: String, date: String, stime: Long, etime: Long, handler: (Long, MapValue) => Any) {
        var logdb: RealtimeCounterDBHelper = null;
        try {
            logdb = new RealtimeCounterDBHelper().open(objName, date, true);
            if (logdb == null)
                return ;

            logdb.counterIndex.readFromEnd(HashUtil.hash(objName), stime, etime, handler, logdb.counterDbHeader.getTagIntStr(),
                logdb.counterData.read);
        } catch {
            case e: Exception => e.printStackTrace();
        } finally {
            FileUtil.close(logdb);
        }
    }

    def getDataNames(objName: String, date: String): StringEnumer = {
        var logdb: RealtimeCounterDBHelper = null;
        try {
            logdb = new RealtimeCounterDBHelper().open(objName, date, true);
            if (logdb == null)
                return null;
            return logdb.counterDbHeader.getTagStrInt().keys();
        } catch {
            case e: Exception => e.printStackTrace();
        } finally {
            FileUtil.close(logdb);
        }
        return null;
    }
}
