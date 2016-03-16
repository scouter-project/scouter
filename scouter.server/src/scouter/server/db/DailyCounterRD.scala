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
        val fileName = path + "/" + DailyCounterWR.prefix;
        val offset = getOffset(key, fileName);
        if (offset < 0)
            return null;

        val reader = DailyCounterData.open(fileName);
        try {
            return reader.getValue(offset, hhmm);
        } finally {
            FileUtil.close(reader);
        }
    }

    def getValues(date: String, key: CounterKey): Array[Value] = {
        val path = DailyCounterWR.getDBPath(date);
        if (new File(path).canRead() == false) {
            return null;
        }
        val fileName = path + "/" + DailyCounterWR.prefix;
        val offset = getOffset(key, fileName);
        if (offset < 0)
            return null;
        val reader = DailyCounterData.open(fileName);
        try {
            return reader.getValues(offset);
        } finally {
            FileUtil.close(reader);
        }
    }

    def getOffset(key: CounterKey, fileName: String): Long = {
        val idx = DailyCounterIndex.open(fileName);
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
        val fileName = path + "/" + DailyCounterWR.prefix;

        var idx: DailyCounterIndex = null;
        var reader: DailyCounterData = null;
        try {
            idx = DailyCounterIndex.open(fileName);
            reader = DailyCounterData.open(fileName);
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
        val fileName = path + "/" + DailyCounterWR.prefix;
        var idx: DailyCounterIndex = null;
        try {
            idx = DailyCounterIndex.open(fileName);
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
