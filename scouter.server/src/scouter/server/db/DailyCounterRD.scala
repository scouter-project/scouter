package scouter.server.db;

import java.io.File
import scouter.lang.CounterKey
import scouter.lang.value.Value
import scouter.server.db.counter.DailyCounterData
import scouter.util.FileUtil;
import scouter.server.db.counter.DailyCounterIndex

object DailyCounterRD {

    def getValue(date: String, key: CounterKey, hhmm: Int): Value = {
        val path = DailyCounterWR.getDBPath(date);
        if (new File(path).canRead() == false) {
            return null;
        }
        val file = path + "/" + DailyCounterWR.prefix;
        val pos = getLocation(key, file);
        if (pos < 0)
            return null;

        val reader = DailyCounterData.open(file);
        try {
            return reader.getValue(pos, hhmm);
        } finally {
            FileUtil.close(reader);
        }
    }

    def getValues(date: String, key: CounterKey): Array[Value] = {
        val path = DailyCounterWR.getDBPath(date);
        if (new File(path).canRead() == false) {
            return null;
        }
        val file = path + "/" + DailyCounterWR.prefix;
        val location = getLocation(key, file);
        if (location < 0)
            return null;
        val reader = DailyCounterData.open(file);
        try {
            return reader.getValues(location);
        } finally {
            FileUtil.close(reader);
        }
    }

    def getLocation(key: CounterKey, file: String): Long = {
        val idx = DailyCounterIndex.open(file);
        try {
            return idx.get(key.getBytesKey());
        } finally {
            FileUtil.close(idx);
        }
    }

    def read(date: String, handler: (Array[Byte], Array[Byte]) => Unit) {

        val path = DailyCounterWR.getDBPath(date);
        if (new File(path).canRead() == false) {
            return ;
        }
        val file = path + "/" + DailyCounterWR.prefix;

        var idx: DailyCounterIndex = null;
        var reader: DailyCounterData = null;
        try {
            idx = DailyCounterIndex.open(file);
            reader = DailyCounterData.open(file);
            idx.read(handler, reader.read);
        } catch {
            case e: Exception => e.printStackTrace();
        } finally {
            FileUtil.close(idx);
            FileUtil.close(reader);
        }
    }

    def readKey(date: String, clo: (Array[Byte]) => Any) {

        val path = DailyCounterWR.getDBPath(date);
        if (new File(path).canRead() == false) {
            return ;
        }
        val file = path + "/" + DailyCounterWR.prefix;
        var idx: DailyCounterIndex = null;
        try {
            idx = DailyCounterIndex.open(file);
            val handler = (key: Array[Byte], date: Array[Byte]) => {
                clo(key);
            }
            idx.read(handler);
        } catch {
            case e: Exception => e.printStackTrace();
        } finally {
            FileUtil.close(idx);
        }
    }
}
