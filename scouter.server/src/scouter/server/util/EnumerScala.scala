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
package scouter.server.util

import java.util.ArrayList
import scouter.lang.value.ListValue
import scouter.lang.value.Value
import scouter.util.IntKeyMap
import scouter.util.IntSet
import scouter.util.IntEnumer
import org.w3c.dom.NodeList
import scouter.util.NodeEnumer
import org.w3c.dom.Node
import scouter.util.LongEnumer
import scouter.util.StringEnumer

object EnumerScala {
    def backward[T](data: java.util.List[T], handler: T => Any) {
        if (data == null)
            return
        var t = data.size() - 1
        while (t >= 0) {
            handler(data.get(t))
            t -= 1
        }
    }

    def forward[T](data: java.util.List[T], handler: T => Any) {
        if (data == null)
            return
        val itr = data.iterator();
        while (itr.hasNext()) {
            handler(itr.next())
        }
    }
    def foreach[T](itr: java.util.Iterator[T], handler: T => Any) {
        while (itr.hasNext()) {
            handler(itr.next())
        }
    }
    def foreach[T](arr: Array[T], handler: T => Any) {
        var i = 0
        while (i < arr.length) {
            handler(arr(i))
            i += 1
        }
    }

    def foreach[T](itr: java.util.Enumeration[T], handler: T => Any) {
        if (itr == null)
            return
        while (itr.hasMoreElements()) {
            handler(itr.nextElement())
        }
    }

    def foreach(nodes: NodeList, handler: Node => Any) {

        val enumer = new NodeEnumer(nodes)
        while (enumer.hasNext()) {
            handler(enumer.next())
        }
    }
    def foreach(itr: StringEnumer, handler: String => Any) {
        if (itr == null)
            return
        while (itr.hasMoreElements()) {
            handler(itr.nextString())
        }
    }
    def foreach(itr: IntEnumer, handler: Int => Any) {
        if (itr == null)
            return
        while (itr.hasMoreElements()) {
            handler(itr.nextInt())
        }
    }
    def foreach(itr: LongEnumer, handler: Long => Any) {
        if (itr == null)
            return
        while (itr.hasMoreElements()) {
            handler(itr.nextLong())
        }
    }
    def foreach[T](data: ListValue, handler: (T) => Any) {
        if (data == null)
            return
        val itr = data.iterator();
        while (itr.hasNext()) {
            handler(itr.next().asInstanceOf[T])
        }
    }
    def foreach[A, B](a: ListValue, b: ListValue, handler: (A, B) => Any) {
        if (a == null || b == null)
            return
        val a1 = a.iterator();
        val b1 = b.iterator();

        while (a1.hasNext()) {
            handler(a1.next().asInstanceOf[A], b1.next().asInstanceOf[B])
        }
    }
    def foreach[A, B, C](a: ListValue, b: ListValue, c: ListValue, handler: (A, B, C) => Any) {
        if (a == null || b == null || c == null)
            return
        val a1 = a.iterator();
        val b1 = b.iterator();
        val c1 = c.iterator();

        while (a1.hasNext()) {
            handler(a1.next().asInstanceOf[A], b1.next().asInstanceOf[B], c1.next().asInstanceOf[C])
        }
    }
}