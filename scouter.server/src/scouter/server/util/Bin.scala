package scouter.server.util

import scouter.util.ArrayUtil

object Bin {

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