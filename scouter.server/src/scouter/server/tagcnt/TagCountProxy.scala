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

package scouter.server.tagcnt;

import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.HashMap
import java.util.Map

import scouter.lang.value.Value
import scouter.server.tagcnt.core.TagCountUtil
import scouter.server.tagcnt.core.Top100FileCache
import scouter.server.tagcnt.core.ValueCount
import scouter.server.tagcnt.core.ValueCountTotal
import scouter.server.tagcnt.first.FirstTCData
import scouter.server.tagcnt.first.FirstTagCountDB
import scouter.server.tagcnt.next.NextTagCountDB
import scouter.server.util.EnumerScala
import scouter.util.BitUtil
import scouter.util.CastUtil
import scouter.util.HashUtil
import scouter.util.LongKeyMap
object TagCountProxy {
    def getTagValueCountWithCache(date: String, objType: String, tagGroup: String, tagName: String, limit: Int): ValueCountTotal = {
        val tagKey = BitUtil.composite(HashUtil.hash(tagGroup), if(tagName!=null) HashUtil.hash(tagName) else 0);
        return getTagValueCountWithCache(date, objType, tagKey, limit);
    }

    def getTagValueCountWithCache(date: String, objType: String, tagKey: Long, limit: Int): ValueCountTotal = {
        val pack = Top100FileCache.readTop100Cache(date, objType, tagKey);
        if (pack != null) {
            return pack;
        }
        return getTagValueCount(date, objType, tagKey, limit);
    }

    def getTagValueCount(date: String, objType: String, tagGroup: String, tagName: String, limit: Int): ValueCountTotal = {
        val tagKey = BitUtil.composite(HashUtil.hash(tagGroup), HashUtil.hash(tagName));
        return getTagValueCount(date, objType, tagKey, limit);
    }

    def getTagValueCount(date: String, objType: String, tagKey: Long, limit: Int): ValueCountTotal = {

        val total = new HashMap[Value, Float]();
        var countPerValue = 0f;
        val list = new ArrayList[ValueCount]();

        val tagmap = FirstTagCountDB.getTagValues(objType, date);
        val out = tagmap.get(tagKey);
        if (out == null) {
            return new ValueCountTotal(0, 0, list);
        }

        val outList = new ArrayList[Value](out);
        EnumerScala.foreach(outList.iterator(), (v: Value) => {
            val cnt = FirstTagCountDB.getTagValueCount(objType, date, tagKey, v);
            total.put(v, TagCountUtil.sum(cnt) + CastUtil.cfloat(total.get(v)));
        })

        EnumerScala.foreach(total.entrySet().iterator(), (e: Map.Entry[Value, Float]) => {
            val cnt = e.getValue();
            list.add(new ValueCount(e.getKey(), cnt));
            countPerValue += cnt;
        })

        Collections.sort(list, new Comparator[ValueCount]() {
            override def compare(o1: ValueCount, o2: ValueCount): Int = {
                return (o2.valueCount - o1.valueCount).toInt
            }
        });
        return new ValueCountTotal(list.size(), countPerValue, if (limit <= 0) list else list.subList(0, Math.min(list.size(), limit)));
    }

    def getTagValueCountData(date: String, objType: String, tagGroup: String, tagName: String, value: Value): Array[Float] = {
        val tagKey = BitUtil.composite(HashUtil.hash(tagGroup), HashUtil.hash(tagName));
        val values = FirstTagCountDB.getTagValueCount(objType, date, tagKey, value);
        if (values != null)
            return values;
        return NextTagCountDB.getTagValueCount(date, objType, tagKey, value);
    }

    
    def getEveryTagTop100Value(objType: String, date: String, tagGroup: String, limit: Int): LongKeyMap[ValueCountTotal] = {
        val map2 = new LongKeyMap[ValueCountTotal]();

        val divHash = HashUtil.hash(tagGroup);

        val tagmap = FirstTagCountDB.getTagValues(objType, date);
        val en = tagmap.keys();
        while (en.hasMoreElements()) {
            val tagName = en.nextLong();
            if (BitUtil.getHigh(tagName) == divHash) {
                val count = tagmap.get(tagName).size();
                var wrap: ValueCountTotal = null;
                if (count >= 100) {
                    wrap = TagCountProxy.getTagValueCountWithCache(date, objType, tagName, limit);
                } else {
                    wrap = TagCountProxy.getTagValueCount(date, objType, tagName, limit);
                }
                map2.put(tagName, wrap);
            }
        }
        return map2;
    }

    def add(time: Long, objType: String, tag: TagCountConfig.Tag, tagValue: Value, cnt: Int) {
         FirstTagCountDB.add(new FirstTCData(objType, time, tag.key, tagValue, cnt.toFloat));
    }
    def add(time: Long, objType: String, tag: TagCountConfig.Tag, tagValue: Value, cnt: Float) {
        FirstTagCountDB.add(new FirstTCData(objType, time, tag.key, tagValue, cnt));
    }

    def add(time: Long, objType: String, tagKey: Long, tagValue: Value, cnt: Float) {
        FirstTagCountDB.add(new FirstTCData(objType, time, tagKey, tagValue, cnt));
    }
}
