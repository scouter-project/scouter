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
package scouter.server.db.io.zip;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import scouter.server.ConfObserver;
import scouter.server.Configure;
import scouter.server.Logger;
import scouter.util.CacheTable;
import scouter.util.CompressUtil;
import scouter.util.FileUtil;
import scouter.util.IShutdown;
import scouter.util.LinkedMap;
import scouter.util.StopWatch;
object IOChannel {
    ConfObserver.put("IOChannel", new Runnable() {
        override def run() {
            readCache.setMaxRow(conf.gzip_read_cache_block);
        }
    });
    val conf = Configure.getInstance();
    val headers = new LinkedMap[String, CountBoard]();
    def getLastWriteBlock(date: String): Block = {
        var uc = headers.get(date);
        if (uc == null) {
            check();
            uc = new CountBoard(date);
            headers.put(date, uc);
        }
        val n = uc.getCount();
        val start = (n % GZipCtr.BLOCK_MAX_SIZE).toInt
        val bk = new Block(date, new Array[Byte](128), start, start, GZipCtr.BLOCK_MAX_SIZE);
        bk.blockNum = (n / GZipCtr.BLOCK_MAX_SIZE).toInt
        return bk;
    }
    private def check() {
        while (headers.size() >= conf.gzip_unitcount_header_cache - 1) {
            try {
                headers.removeFirst().close();
            } catch {
                case e: Exception =>
            }
        }
    }
    def getCountBoard(date: String): CountBoard = {
        var uc = headers.get(date);
        if (uc == null) {
            check();
            try {
                uc = new CountBoard(date);
            } catch {
                case e: Exception => e.printStackTrace();
            }
            headers.put(date, uc);
        }
        return uc;
    }
    def store(_bk: Block) {
        var bk = _bk
        this.synchronized {
            if (bk.dirty == false)
                return ;
            // Logger.println("S129","Store " + bk);
            bk.dirty = false;
            var mgtime = 0;
            val w = new StopWatch();
            if (bk.START > 0) {
                val w2 = new StopWatch();
                val old = getReadBlock(bk.date, bk.blockNum);
                if (old != null) {
                    bk = bk.merge(old);
                    readCache.put(new BKey(bk.date, bk.blockNum), bk, conf.gzip_read_cache_time);
                }
                mgtime = w2.getTime().toInt
            }
            getCountBoard(bk.date).set(bk.getOffset());
            try {
                val org = bk.getBlockBytes();
                val date = bk.date;
                val blockNum = bk.blockNum;
                val out = CompressUtil.doZip(org);
                FileUtil.save(getFile(date, blockNum), out);
            } catch {
                case e: Exception => e.printStackTrace();
            }
            val tm = w.getTime();
            if (tm > 1000) {
                Logger.println("S130", "Store " + tm + " ms " + (if (mgtime > 0) " old-load=" + mgtime + "ms" else ""));
            }
        }
    }
    private def getFile(date: String, blockNum: Int): File = {
        val filename = (GZipCtr.createPath(date) + "/xlog." + blockNum);
        return new File(filename);
    }
    private val readCache = new CacheTable[BKey, Block]().setMaxRow(conf.gzip_read_cache_block);
    def getReadBlock(date: String, blockNum: Int): Block = {
        val b = readCache.get(new BKey(date, blockNum));
        if (b != null)
            return b;
        val f = getFile(date, blockNum);
        if (f.exists() == false)
            return null;
        try {
            var gz = FileUtil.readAll(f);
            gz = CompressUtil.unZip(gz);
            val bk = new Block(date, gz, 0, gz.length, GZipCtr.BLOCK_MAX_SIZE);
            bk.blockNum = blockNum;
            readCache.put(new BKey(date, blockNum), bk, conf.gzip_read_cache_time);
            return bk;
        } catch {
            case e: Throwable => e.printStackTrace();
        }
        return null;
    }
    def close(date: String) {
        try {
            val en = readCache.keys();
            while (en.hasMoreElements()) {
                val k = en.nextElement();
                if (date.equals(k.date)) {
                    readCache.remove(k);
                }
            }
        } catch {
            case e: Throwable => e.printStackTrace();
        }
    }
}
