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
package scouter.server.tagcnt.first;

import java.io.IOException
import java.util.ArrayList
import java.util.Enumeration
import java.util.HashMap
import java.util.Hashtable
import java.util.Iterator
import java.util.List
import java.util.Map
import java.util.Set
import scouter.io.DataInputX
import scouter.lang.value.Value
import scouter.util.CastUtil
import scouter.util.IClose
import scouter.util.IntEnumer
import scouter.util.IntIntMap
import scouter.util.LongEnumer
import scouter.util.LongKeyMap;
import scouter.util.IntFloatMap
object TagValueEntry {
    val table = new Hashtable[String, TagValueEntry]();

    def open(tagv: IndexFile): TagValueEntry = {
        table.synchronized {
            var inx = table.get(tagv.path);
            if (inx != null) {
                inx.refrence += 1;
                return inx;
            } else {
                inx = new TagValueEntry(tagv);
                table.put(tagv.path, inx);
                return inx;
            }
        }
    }

    def close(date: String) {
        val en = TagValueEntry.table.keys();
        while (en.hasMoreElements()) {
            val key = en.nextElement();
            if (key.indexOf(date) >= 0) {
                try {
                    val db = table.get(key);
                    while (table.containsKey(key)) {
                        db.close();
                    }
                } catch {
                    case e: Exception => e.printStackTrace();
                }
            }
        }
    }

}
class TagValueEntry(countingTable: IndexFile) extends IClose {

    var refrence = 0;
    val file = countingTable.path;

    var tagmap: LongKeyMap[Map[Value, IntFloatMap]] = null
    var dirty = false

    def setDirty() {
        this.dirty = true;
    }

    def getTagList(): Array[Long] = {
        init();
        return tagmap.keyArray();
    }

    def getValueListForTag(tagName: Long): List[Value] = {
        init();
        val valueMap = tagmap.get(tagName);
        if (valueMap == null)
            return new ArrayList[Value]();
        else
            return new ArrayList[Value](valueMap.keySet());
    }

    def getTagValueSet(): LongKeyMap[Set[Value]] = {
        init();
        val out = new LongKeyMap[Set[Value]]();
        val itr = tagmap.keys();
        while (itr.hasMoreElements()) {
            val key = itr.nextLong();
            val value = tagmap.get(key).keySet();
            out.put(key, value);
        }
        return out;
    }

    private def init() {
        this.synchronized {
            if (tagmap == null) {
                tagmap = new LongKeyMap[Map[Value, IntFloatMap]]();
                this.countingTable.read((key: Array[Byte], data: Array[Int]) => {
                    val in = new DataInputX(key);
                    val tagkey = in.readLong();
                    val tagValue = in.readValue();
                    push(tagkey, tagValue);
                });
            }
        }
    }

    def reloadTagMap() {
        this.synchronized {
            save();
            tagmap = null;
            init();
        }
    }

    def save() {
        this.synchronized {
            if (tagmap == null) {
                return ;
            }
            if (this.dirty == false)
                return ;
            this.dirty = false;
            val keys = tagmap.keyArray();
            var inx = 0
            while (inx < keys.length) {
                val tagkey = keys(inx);
                val valueMap = tagmap.get(tagkey);
                if (valueMap != null) {
                    val itr = valueMap.keySet().iterator();
                    while (itr.hasNext()) {
                        val tagValue = itr.next();
                        val hmCnt = valueMap.get(tagValue);
                        val hmItr = hmCnt.keys();
                        while (hmItr.hasMoreElements()) {
                            val hhmm = hmItr.nextInt();
                            val count = hmCnt.get(hhmm);
                            this.countingTable.add(tagkey, tagValue, hhmm, count);
                        }
                        hmCnt.clear();
                    }
                }
                inx += 1
            }
        }
    }

    def add(tagName: Long, tagValue: Value, hhmm: Int, cnt: Float): Boolean = {
        this.setDirty();
        init();
        var valueCountMap = tagmap.get(tagName);
        if (valueCountMap == null) {
            valueCountMap = new HashMap[Value, IntFloatMap]();
            tagmap.put(tagName, valueCountMap);
        }
        var hmCnt = valueCountMap.get(tagValue);
        if (hmCnt == null) {
            if (valueCountMap.size() >= 100)
                return false;
            hmCnt = new IntFloatMap();
            valueCountMap.put(tagValue, hmCnt);
        }
        hmCnt.put(hhmm, CastUtil.cfloat(hmCnt.get(hhmm)) + cnt);
        return true;
    }

    def push(tagName: Long, tagValue: Value) {
        var valueCntMap = tagmap.get(tagName);
        if (valueCntMap == null) {
            valueCntMap = new HashMap[Value, IntFloatMap]();
            tagmap.put(tagName, valueCntMap);
            valueCntMap.put(tagValue, new IntFloatMap());
            return ;
        }
        if (valueCntMap.containsKey(tagValue) == false) {
            valueCntMap.put(tagValue, new IntFloatMap());
        }
    }

    def close() {
        TagValueEntry.table.synchronized {
            if (this.refrence == 0) {
                this.save();
                TagValueEntry.table.remove(this.file);
            } else {
                this.refrence -= 1;
            }
        }
    }
}
