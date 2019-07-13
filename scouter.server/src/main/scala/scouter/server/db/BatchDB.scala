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

package scouter.server.db;

import scouter.server.Configure
import scouter.server.Logger
import scouter.server.util.ThreadScala
import scouter.util.RequestQueue
import scouter.util.StringKeyLinkedMap
import scouter.server.util.cardinality.HyperLogLog
import scouter.util.DateUtil
import scouter.util.ThreadUtil
import scouter.util.IntKeyLinkedMap
import java.io.File
import scouter.util.FileUtil
import scouter.util.HashUtil
import scouter.util.Hexa32
import scouter.server.util.EnumerScala
import scouter.server.core.CoreRun
import scouter.lang.pack.BatchPack
import scouter.lang.pack.ObjectPack
import scouter.server.core.AgentManager
import scouter.util.LongKeyLinkedMap
import scouter.util.BitUtil
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import scouter.io.DataOutputX
import java.io.DataInput
import scouter.io.DataInputX
import java.io.RandomAccessFile
import scouter.util.ArrayUtil
import scouter.server.util.BinSearch
import scouter.server.util.BinSearch

object BatchDB {
    val IDX_LEN = 8 + 5

    val queue = new RequestQueue[BatchPack](DBCtr.MAX_QUE_SIZE);
    val dbinfo = new LongKeyLinkedMap[String]().setMax(1000);
    ThreadScala.startDaemon("scouter.server.db.BatchDB") {
        while (DBCtr.running) {
            val m = queue.get()
            val path = open(m)
            if (path != null) {
                val pos = append(path, m);
                appendX(path, (m.startTime + m.elapsedTime), pos);
            }
        }
    }
    def add(data: BatchPack) {
        val ok = queue.put(data);
        if (ok == false) {
            Logger.println("S000", 10, "queue exceeded!!");
        }
    }
    
    def read(objName: String, from: Long, to: Long, filter: String, response: Long, handler: (Long, BatchPack) => Any) {
        val date = DateUtil.yyyymmdd(from)
        val path = getDBPath(date, objName)
        val idxFile = new File(path + "/batch.idx")
        if (idxFile.canRead() == false)
            return

        val idxRAF = new RandomAccessFile(path + "/batch.idx", "rw");
        val dataFile = new RandomAccessFile(path + "/batch.dat", "rw");
        val len = (idxFile.length / IDX_LEN).toInt
        val bs = new BinSearch[Long](len, (a: Long) => { idxRAF.seek(a * IDX_LEN); new DataInputX(idxRAF).readLong() },
            (a: Long, b: Long) => (b - a).toInt)

        var x = bs.searchBE(from).toInt

        if (x < 0) {
            return
        }
        var isSend = false
        while (x < len) {
            idxRAF.seek(x * IDX_LEN);
            val time = new DataInputX(idxRAF).readLong()
            if (time <= to) {
                idxRAF.seek(x * IDX_LEN + 8);
                val dataPos = new DataInputX(idxRAF).readLong5()
                dataFile.seek(dataPos)
                val dataIn = new DataInputX(dataFile)
                val len = DataInputX.toInt(dataIn.read(4), 0)
                val data = dataIn.read(len)
                isSend = true
                val batchPack = new BatchPack()
                batchPack.readSimplePack(data)
                if(filter != null || response > 0){
                	if(filter != null){
                	  if(batchPack.batchJobId.indexOf(filter) < 0){
                	    isSend = false;
                	  }
                	}
                	if(response > 0){
                	  if(batchPack.elapsedTime < response){
                	    isSend = false;
                	  }
                	}
                }
                if(isSend){
                	batchPack.position = dataPos
                	handler(time, batchPack)
            	}
                x += 1
            } else {
                x = len // break
            }
        }
    }

    def read(objName: String, time: Long, position: Long) : Array[Byte] = {
        val date = DateUtil.yyyymmdd(time)
        val path = getDBPath(date, objName)
        val datFile = new File(path + "/batch.idx")
        if (datFile.canRead() == false)
            return null

        val dataFile = new RandomAccessFile(path + "/batch.dat", "rw");    
        dataFile.seek(position)
        val dataIn = new DataInputX(dataFile)
        val len = DataInputX.toInt(dataIn.read(4), 0)
        val data = dataIn.read(len)
        return data;
    }
    
    protected def append(path: String, m: BatchPack): Long = {
        val file = new File(path + "/batch.dat")
        val offset = file.length();
        val out = new FileOutputStream(file, true)
        val data = new DataOutputX().writePack(m).toByteArray();
        out.write(DataOutputX.toBytes(data.length))
        out.write(data)
        out.close();
        return offset;
    }
    protected def appendX(path: String, time: Long, pos: Long) {
        val file = new File(path + "/batch.idx")
        //val offset = file.length();
        val out = new FileOutputStream(file, true);
        out.write(new DataOutputX().writeLong(time).writeLong5(pos).toByteArray())
        out.close();
    }
    protected def open(m: BatchPack): String = {
        val key = BitUtil.setHigh(DateUtil.getDateUnit(m.startTime + m.elapsedTime), m.objHash)
        val opath = dbinfo.get(key)
        if (opath != null)
            return opath
        else {
            val objName = getObjName(m)
            if (objName != null) {
                val date = DateUtil.yyyymmdd(m.startTime + m.elapsedTime)
                val path = getDBPath(date, objName)
                val f = new File(path)
                if (f.exists() == false) {
                    f.mkdirs()
                }
                if (f.exists()) {
                    dbinfo.put(key, path);
                }
                return path;
            }
            return null
        }
    }
    protected def getDBPath(date: String, objName: String): String = {
        val sb = new StringBuffer();
        sb.append(DBCtr.getRootPath());
        sb.append("/").append(date).append(objName);
        return sb.toString();
    }
    protected def getObjName(m: BatchPack): String = {
        val objInfo = AgentManager.getAgent(m.objHash)
        if (objInfo == null) {
            val objInfoNew = new ObjectPack(); 
            objInfoNew.objHash = m.objHash;
            objInfoNew.objName = m.objName;
            objInfoNew.objType = m.objType;
            AgentManager.active(objInfoNew);
            return objInfoNew.objName;
        }
        return objInfo.objName;
    }
}
