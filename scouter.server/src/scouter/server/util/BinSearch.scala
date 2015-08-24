/*
*  Copyright 2015 Scouter Project.
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

class BinSearch[T](length: Long, data: (Long) => T, comp: (T, T) => Int) {

    private val NONE = -1
    private val LE = -1
    private val EQ = 0
    private val BE = 1
    def search(target: T): Long = {
        search(target, EQ)
    }
    def searchLE(target: T): Long = {
        search(target, LE)
    }
    def searchBE(target: T): Long = {
        search(target, BE)
    }
    private def search(target: T, mode: Int): Long = {
        var low = 0L
        var high = length - 1L

        while (high >= low) {
            val mid = (low + high) / 2
            val midData = data(mid)
            if (comp(midData, target) < 0) {
                high = mid - 1
            } else if (comp(midData, target) > 0) {
                low = mid + 1
            } else {
                return mid
            }
        }
        mode match {
            case -1 => if (high >= 0) return high
            case 1 => if (low < length) return low
            case _ => return NONE
        }
        return NONE
    }

}

/**
 * test code
 */
object BinSearch {

    def main(args: Array[String]): Unit = {
        val data = Array(10, 20, 30, 40, 50, 60, 70, 80, 90, 100)
        println(java.util.Arrays.toString(data));
        val b = new BinSearch[Int](data.length, (a: Long) => data(a.toInt), (a: Int, b: Int) => b - a)
        prt(b, 9)
        prt(b, 10)
        prt(b, 22)
        prt(b, 22)
        prt(b, 100)
        prt(b, 101)

    }

    private def prt(b: BinSearch[Int], a: Int): Unit = {
        println(b.searchLE(a) + " " + b.search(a) + " " + b.searchBE(a))
    }

}