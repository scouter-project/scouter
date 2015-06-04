package scouter.server

import scouter.util.StringKeyLinkedMap
import scouter.server.util.EnumerScala

object ConfObserver {

    private val observers = new StringKeyLinkedMap[Runnable]();

    def put(name: String)(code: => Any) {
        observers.put(name, new Runnable() {
            override def run() {
                code
            }
        })
    }
    def put(name: String,code: Runnable) {
        observers.put(name,code)
    }
    def exec() {
        val en = observers.values()
        while (en.hasMoreElements()) {
            val r = en.nextElement();
            try {
                r.run()
            } catch {
                case t: Throwable => t.printStackTrace();
            }
        }
    }
}