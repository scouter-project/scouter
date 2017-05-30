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
package scouter.server.tagcnt.core;
import java.io.File
import java.io.IOException
import java.util.Collections
import java.util.Comparator
import java.util.HashSet
import scouter.lang.value.Value
import scouter.server.core.CoreRun
import scouter.server.tagcnt.first.FirstTagCountDB
import scouter.server.tagcnt.next.IndexFile
import scouter.server.tagcnt.next.NextTagCountDB
import scouter.server.util.EnumerScala
import scouter.server.util.ThreadScala
import scouter.util.CastUtil
import scouter.util.DateUtil
import scouter.util.FileUtil
import scouter.util.LongIntMap
import scouter.util.LongKeyMap
import scouter.util.LongSet
import scouter.util.Order
import scouter.util.OrderUtil
import scouter.util.ThreadUtil
import scouter.util.TopN
class Key(_logDate: String, _objType: String) {
    val logDate = _logDate;
    val objType = _objType;
    override def hashCode = objType.hashCode() ^ logDate.hashCode();
    override def equals(obj: Any): Boolean = {
        if (obj == null)
            return false
        val other = obj.asInstanceOf[Key]
        return this.logDate.equals(other.logDate) && this.objType.equals(other.objType);
    }
    override def toString() = "Key [logDate=" + logDate + ", objType=" + objType + "]"
}
object Top100FileCache {
    private var logSet = new HashSet[Key]();
    def add(logdate: String, objType: String) {
        logSet.add(new Key(logdate, objType));
    }
    ThreadScala.startDaemon("scouter.server.tagcnt.core.Top100FileCache") {
        while (CoreRun.running) {
            ThreadUtil.sleep(DateUtil.MILLIS_PER_FIVE_MINUTE);
            try {
                val workSet = logSet;
                logSet = new HashSet[Key]();
                EnumerScala.foreach(workSet.iterator(), (key: Key) => {
                    this.synchronized {
                        makeTop100(key.logDate, key.objType,100)
                    }
                })
            } catch {
                case e: Exception => e.printStackTrace();
            }
        }
    }
    def readTop100Cache(logDate: String, objType: String, tagName: Long): ValueCountTotal = {
        val file = getFileName(logDate, objType, tagName);
        if (file.exists() == false)
            return null;
        val b = FileUtil.readAll(file);
        try {
            return new ValueCountTotal().toObject(b);
        } catch {
            case e: IOException =>
                e.printStackTrace();
        }
        return null;
    }
    private def getFileName(logDate: String, objType: String, tagName: Long): File = {
        val fileRef = new File(CountEnv.getDBPath(logDate));
        val fileUPFolder = new File(fileRef, objType);
        if (!fileUPFolder.exists()) {
            fileUPFolder.mkdirs();
        }
        return new File(fileUPFolder, "top100." + tagName + ".dat");
    }
    def getTagNames(logDate: String, objType: String): LongIntMap = {
        val tagmap = FirstTagCountDB.getTagValues(objType, logDate);
        val tagMap = new LongIntMap();
        EnumerScala.foreach(tagmap.keys(), (tagKey: Long) => {
            tagMap.put(tagKey, tagmap.get(tagKey).size());
        })
        return tagMap;
    }
    def getLargeTagSet(logDate: String, objType: String): LongSet = {
        val tagmap = FirstTagCountDB.getTagValues(objType, logDate);
        val tagMap = new LongSet();
        EnumerScala.foreach(tagmap.keys(), (tagKey: Long) => {
            val valueSet = tagmap.get(tagKey);
            if (valueSet.size() >= 100) {
                tagMap.add(tagKey);
            }
        })
        return tagMap;
    }
    class TopItem(_tag: Long, limit: Int) {
        var kindsOfValue = 0
        var sumOfValue = 0f
        var topN = new TopN[ValueCount](limit,TopN.DIRECTION.DESC) ;
        var tag = _tag
        def toString(tagName: String) = {
            "TopItem [tag=" + tagName + " sumOfValue=" + sumOfValue.formatted("#,##0.0") + ", kindsOfValue=" + kindsOfValue + ", topN=" + topN.size() + "]";
        }
        override def toString() = {
            "TopItem [sumOfValue=" + sumOfValue.formatted("#,##0.0") + ", kindsOfValue=" + kindsOfValue + ", topN=" + topN + ", tag=" + tag + "]"
        }
    }
    def makeTop100(date: String, objType: String, limit: Int) = {
        val map = new LongKeyMap[TopItem]();
        NextTagCountDB.read(date, objType, (tag: Long, value: Value, tcnt: Float, vpos: Array[Long], table: IndexFile, pos: Long) => {
            var t = map.get(tag);
            if (t == null) {
                t = new TopItem(tag, limit);
                map.put(tag, t);
            }
            t.sumOfValue += tcnt;
            t.kindsOfValue += 1;
            t.topN.add(new ValueCount(value, tcnt));
        });
        val tagmap = FirstTagCountDB.getTagValues(objType, date);
        val map2 = new LongKeyMap[ValueCountTotal]();
        EnumerScala.foreach(map.keys(), (tagName: Long) => {
            val topItem = map.get(tagName);
            val out = tagmap.get(tagName);
            if (out != null) {
                EnumerScala.foreach(out.iterator(), (value: Value) => {
                    val cnt = FirstTagCountDB.getTagValueCount(objType, date, tagName, value);
                    val countSum = TagCountUtil.sum(cnt);
                    topItem.topN.add(new ValueCount(value, countSum));
                    topItem.kindsOfValue += 1;
                    topItem.sumOfValue += countSum;
                })
            }
            val outMap = new ValueCountTotal();
            map2.put(tagName, outMap);
            outMap.totalCount = topItem.sumOfValue;
            outMap.howManyValues = topItem.kindsOfValue;
            val sublist = topItem.topN.getList();
           
            var i = 0
            while (i < sublist.size()) {
                outMap.values.add(sublist.get(i));
                i+=1 //bugfix 
            }
             
            //save cache file
            val file = getFileName(date, objType, tagName);
            FileUtil.save(file, outMap.toByteArray());
          
        })
       
    }
    def getEveryTagTop100Value(date: String, objType: String, hhmm: String, limit: Int): LongKeyMap[ValueCountTotal] = {
        val map = new LongKeyMap[TopItem]();
        val hm = CastUtil.cint(hhmm);
        val hh = hm / 100;
        val mm = hm % 100;
        NextTagCountDB.read(date, objType, (tag: Long, value: Value, tcnt: Float, vpos: Array[Long], table: IndexFile, pos: Long) => {
            try {
                if (vpos(hh) > 0) {
                    val mcount = table.getValue(vpos(hh));
                    if (mcount(mm) > 0) {
                        var t = map.get(tag);
                        if (t == null) {
                            t = new TopItem(tag, limit);
                            map.put(tag, t);
                        }
                        t.sumOfValue += mcount(mm);
                        t.kindsOfValue += 1
                        t.topN.add(new ValueCount(value, mcount(mm)));
                    }
                }
            } catch {
                case e: Exception => e.printStackTrace();
            }
        });
        val tagmap = FirstTagCountDB.getTagValues(objType, date);
        val map2 = new LongKeyMap[ValueCountTotal]();
        EnumerScala.foreach(tagmap.keys(), (tagName: Long) => {
            var topItem = map.get(tagName);
            if (topItem == null) {
                topItem = new TopItem(tagName, limit);
            }
            val out = tagmap.get(tagName);
            if (out != null) {
                EnumerScala.foreach(out.iterator(), (value: Value) => {
                    val cnt = FirstTagCountDB.getTagValueCount(objType, date, tagName, value);
                    val countForValue = cnt(TagCountUtil.getBucketPos(hh, mm));
                    if (countForValue > 0) {
                        topItem.topN.add(new ValueCount(value, countForValue));
                        topItem.kindsOfValue += 1;
                        topItem.sumOfValue += countForValue;
                    }
                })
            }
            val sublist = topItem.topN.getList();
            Collections.sort(sublist, new Comparator[ValueCount]() {
                override def compare(o1: ValueCount, o2: ValueCount): Int = {
                    return (o2.valueCount - o1.valueCount).toInt
                }
            });
            val wr = new ValueCountTotal();
            wr.howManyValues = topItem.kindsOfValue;
            wr.totalCount = topItem.sumOfValue;
            wr.values = sublist;
            map2.put(tagName, wr);
        })
        return map2;
    }
}
