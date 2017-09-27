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

package scouter.server.core.cache

import java.util.{ArrayList, List}

class AlertScriptLoadMessageLoopCache(capacity: Int) {
    val queue = new Array[Any](capacity);

    var index = 0;
    var loop = 0L;

    def put(record: String) {
        queue.synchronized {
            queue(index) = record;
            index += 1;
            if (index >= queue.length) {
                loop += 1;
                index = 0;
            }
        }
    }

    def getList(start_loop: Long, start_index: Int): List[String] = {
        val end_index = this.index;
        val end_loop = this.loop;

        val buff = new ArrayList[String](queue.length);
        (end_loop - start_loop) match {
            case 0 =>
                if (start_index < end_index) {
                    copy(buff, start_index, end_index);
                }
            case 1 =>
                if (start_index <= end_index) {
                    copy(buff, end_index, queue.length);
                    copy(buff, 0, end_index);
                } else {
                    copy(buff, start_index, queue.length);
                    copy(buff, 0, end_index);
                }
            case _ =>
                copy(buff, end_index, queue.length);
                copy(buff, 0, end_index);
        }
        return buff;
    }

    def copy(buff: List[String], _from: Int, _to: Int) {
        var i = _from
        while (i < _to) {
            if (queue(i) != null) {
                buff.add(queue(i).asInstanceOf[String]);
            }
            i += 1
        }
    }

    def get(last_loop: Long, last_index: Int): CacheOut[String] = {
        val d = new CacheOut[String]();
        d.data = getList(last_loop, last_index);
        d.loop = this.loop;
        d.index = this.index;
        return d;
    }
}
