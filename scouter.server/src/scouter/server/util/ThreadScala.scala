package scouter.server.util
import java.util.Timer
import java.util.TimerTask
import java.util.Date
import scouter.util.ThreadUtil
import scouter.server.Logger
object ThreadScala {
    def startDaemon(name: String)(codeBlock: => Unit): Thread = {
        val thread = new Thread(name) {
            override def run() {
                try {
                    codeBlock
                } catch {
                    case t: Throwable => t.printStackTrace()
                }
            }
        }
        thread.setDaemon(true)
        thread.start()
        return thread
    }
    def startDaemon(name: String, cond: => Boolean)(codeBlock: => Unit): Thread = {
        val thread = new Thread(name) {
            override def run() {
                while (cond) {
                    try {
                        codeBlock
                    } catch {
                        case n: NullPointerException => Logger.println("S189", 10, "@startDaemon"+codeBlock.getClass(), n)
                        case t: Throwable => Logger.println("S190", 10, "@startDaemon: "+codeBlock.getClass(),t)
                    }
                }
            }
        }
        thread.setDaemon(true)
        thread.start()
        return thread
    }
    def startDaemon(name: String, cond: => Boolean, interval: Long)(codeBlock: => Unit): Thread = {
        val thread = new Thread(name) {
            override def run() {
                while (cond) {
                    try {
                        codeBlock
                    } catch {
                        case n: NullPointerException => Logger.println("S217", 10, "@startDaemon", n)
                        case t: Throwable => Logger.println("S218", 10, "@startDaemon: " + t)
                    }
                    ThreadUtil.sleep(interval)
                }
            }
        }
        thread.setDaemon(true)
        thread.start()
        return thread
    }
    def start(name: String)(codeBlock: => Unit): Thread = {
        val thread = new Thread(name) {
            override def run() {
                try {
                    codeBlock
                } catch {
                    case n: NullPointerException => Logger.println("S193", 10, "@start", n)
                    case t: Throwable => Logger.println("S194", 10, "@start: " + t)
                }
            }
        }
        thread.start()
        return thread
    }
    def start(name: String, cond: => Boolean)(codeBlock: => Unit): Thread = {
        val thread = new Thread(name) {
            override def run() {
                while (cond) {
                    try {
                        codeBlock
                    } catch {
                        case n: NullPointerException => Logger.println("S195", 10, "@start", n)
                        case t: Throwable => Logger.println("S196", 10, "@start: " + t)
                    }
                }
            }
        }
        thread.start()
        return thread
    }
    def start(name: String, cond: => Boolean, interval: Long)(codeBlock: => Unit): Thread = {
        val thread = new Thread(name) {
            override def run() {
                while (cond) {
                    try {
                        codeBlock
                    } catch {
                        case n: NullPointerException => Logger.println("S197", 10, "@start", n)
                        case t: Throwable => Logger.println("S198", 10, "@start: " + t)
                    }
                    ThreadUtil.sleep(interval)
                }
            }
        }
        thread.start()
        return thread
    }
    def startFixedRate(TIME_INTERVAL: Long)(code: => Unit): Timer = {
        def timer = new Timer(true);
        val stime = (System.currentTimeMillis() / TIME_INTERVAL + 1) * TIME_INTERVAL;
        val timerTask = new TimerTask {
            override def run {
                code
            }
        }
        timer.scheduleAtFixedRate(timerTask, new Date(stime), TIME_INTERVAL);
        return timer
    }
    
    // main을 실행하며 동작방식을 이해
    def main(args: Array[String]) {
        val v1 = ThreadScala.startDaemon("scouter.server.util.ThreadScala") {
            while (true) {
                Thread.sleep(1000)
                println("hello " + Thread.currentThread().getName() + "  " + Thread.currentThread().isDaemon())
            }
        }
        val v2 = ThreadScala.start("scouter.server.util.ThreadScala-2") {
            while (true) {
                Thread.sleep(1000)
                println("hello2 " + Thread.currentThread().getName() + "  " + Thread.currentThread().isDaemon())
            }
        }
    }
}
